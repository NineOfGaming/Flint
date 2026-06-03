package dev.dfonline.flint.feature.impl;

import dev.dfonline.flint.Flint;
import dev.dfonline.flint.FlintAPI;
import dev.dfonline.flint.feature.trait.ConnectionListeningFeature;
import dev.dfonline.flint.feature.trait.PacketListeningFeature;
import dev.dfonline.flint.feature.trait.TickedFeature;
import dev.dfonline.flint.hypercube.Mode;
import dev.dfonline.flint.hypercube.PlayerLocation;
import dev.dfonline.flint.hypercube.PlayerProfile;
import dev.dfonline.flint.hypercube.Plot;
import dev.dfonline.flint.hypercube.PlotSize;
import dev.dfonline.flint.util.FlintUpdate;
import dev.dfonline.flint.util.Logger;
import dev.dfonline.flint.util.result.EventResult;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.regex.Pattern;

/**
 * Handles tracking the player's mode and updating it accordingly.
 */
public class ModeTrackerFeature implements PacketListeningFeature, TickedFeature, ConnectionListeningFeature {

    private static final Logger LOGGER = Logger.of(ModeTrackerFeature.class);
    private static final Pattern SPAWN_ACTION_BAR_PATTERN =
            Pattern.compile("(⏵+ - )?⧈ -?\\d+ Tokens {2}ᛥ -?\\d+ Tickets {2}⚡ -?\\d+ Sparks");
    private static final String DEV_MODE_MESSAGE = "» You are now in dev mode.";
    private static final String BUILD_MODE_MESSAGE = "» You are now in build mode.";
    private static final String JOINED_GAME_PREFIX = "» Joined game: ";
    private static final int DEV_SPAWN_OFFSET_X = 11;
    private static final int DEV_SPAWN_OFFSET_Z = 10;
    private static final int GROUND_LEVEL = 49;

    private PendingModeSwitchAction pendingAction = PendingModeSwitchAction.CLEAR_TITLE;
    private static boolean hasQueuedLocate = false;
    private static Mode queuedMode = null;
    private static boolean sentUpdateMessageThisSession = false;
    private static String requestedUserProfileName = null;
    private static boolean pendingUserProfileRequest = false;

    @Override
    public boolean alwaysOn() {
        return true;
    }

    private static void setMode(Mode mode) {
        if (FlintAPI.isDebugging()) {
            LOGGER.info("Setting to mode " + mode);
        }

        if (mode == Mode.NONE) {
            Flint.getUser().setProfile(null);
        }

        if (FlintAPI.shouldConfirmLocationWithLocate() && mode != Mode.NONE) {
            hasQueuedLocate = true;
            return;
        }

        Flint.getUser().setNode(null);
        Flint.getUser().setPlot(null);
        Flint.getUser().setMode(mode);
    }

    public static void confirmCurrentLocation() {
        ClientPlayerEntity player = Flint.getClient().player;
        if (player == null) {
            return;
        }

        LocateFeature.requestLocate(player.getGameProfile().name()).thenAccept(locate ->
                Flint.getClient().execute(() -> applyLocateResult(locate))
        );
    }

    static void ensureCurrentUserProfile() {
        if (!FlintAPI.shouldFetchUserProfileWithWhois()) {
            pendingUserProfileRequest = false;
            return;
        }

        ClientPlayerEntity player = Flint.getClient().player;
        if (player == null) {
            pendingUserProfileRequest = true;
            return;
        }

        String userName = player.getGameProfile().name();
        PlayerProfile currentProfile = Flint.getUser().getProfile();
        if (currentProfile != null && currentProfile.userName().equalsIgnoreCase(userName)) {
            requestedUserProfileName = userName;
            pendingUserProfileRequest = false;
            return;
        }

        Flint.getUser().setProfile(null);
        if (requestedUserProfileName != null && requestedUserProfileName.equalsIgnoreCase(userName)) {
            pendingUserProfileRequest = false;
            return;
        }

        requestedUserProfileName = userName;
        pendingUserProfileRequest = false;
        requestUserProfile(userName);
    }

    static void requestCurrentPlotOwnerProfile() {
        Plot plot = Flint.getUser().getPlot();
        if (plot == null) {
            confirmCurrentLocation();
            return;
        }

        requestPlotOwnerProfile(plot);
    }

    @Override
    public EventResult onReceivePacket(Packet<?> packet) {
        if (!hasQueuedLocate) {
            if (packet instanceof ClearTitleS2CPacket clear && clear.shouldReset()) {
                this.pendingAction = PendingModeSwitchAction.POSITION_CHANGE;
            } else if (packet instanceof PlayerSpawnPositionS2CPacket &&
                    this.pendingAction == PendingModeSwitchAction.POSITION_CHANGE) {
                this.pendingAction = PendingModeSwitchAction.MESSAGE;
            }
        }

        boolean overlayMatches = packet instanceof OverlayMessageS2CPacket(Text text) &&
                this.pendingAction == PendingModeSwitchAction.MESSAGE &&
                SPAWN_ACTION_BAR_PATTERN.matcher(text.getString()).matches();

        if (overlayMatches) {
            queuedMode = Mode.SPAWN;
            this.pendingAction = PendingModeSwitchAction.CLEAR_TITLE;
        }

        if (!hasQueuedLocate &&
                packet instanceof GameMessageS2CPacket gameMsg &&
                this.pendingAction == PendingModeSwitchAction.MESSAGE) {
            String content = gameMsg.content().getString();

            if (content.equals(DEV_MODE_MESSAGE)) {
                setMode(Mode.DEV);
            } else if (content.equals(BUILD_MODE_MESSAGE)) {
                setMode(Mode.BUILD);
            } else if (content.startsWith(JOINED_GAME_PREFIX)) {
                setMode(Mode.PLAY);
            }
        }

        return EventResult.PASS;
    }

    @Override
    public void tick() {
        if (Flint.getClient().player != null) {
            if (pendingUserProfileRequest) {
                ensureCurrentUserProfile();
            }

            if (hasQueuedLocate) {
                hasQueuedLocate = false;
                confirmCurrentLocation();
            }

            if (queuedMode != null) {
                if (!sentUpdateMessageThisSession) {

                    FlintUpdate.sendUpdateMessage();

                    sentUpdateMessageThisSession = true;
                }
                setMode(queuedMode);
                queuedMode = null;
            }

            Plot plot = Flint.getUser().getPlot();
            if (plot != null && plot.getDevOrigin() != null) {
                if (!plot.isSizeKnown()) {
                    plot.setSize(detectPlotSize());
                }

                plot.setHasUnderground(detectPlotUnderground());
            }
        }
    }

    @Override
    public void onJoin() {
        requestedUserProfileName = null;
        pendingUserProfileRequest = FlintAPI.shouldFetchUserProfileWithWhois();
        ensureCurrentUserProfile();
    }

    @Override
    public void onDisconnect() {
        WhoisFeature.clearCache();
        requestedUserProfileName = null;
        pendingUserProfileRequest = false;
        setMode(Mode.NONE);
        sentUpdateMessageThisSession = false;
    }

    private static void applyLocateResult(PlayerLocation locate) {
        if (Flint.getClient().player == null) {
            return;
        }

        Flint.getUser().setNode(locate.node());

        Vec3i newOrigin;
        if (locate.mode() == Mode.DEV) {
            BlockPos blockpos = Flint.getUser().getPlayer().getBlockPos();
            newOrigin = new Vec3i(blockpos.getX() + DEV_SPAWN_OFFSET_X, GROUND_LEVEL, blockpos.getZ() - DEV_SPAWN_OFFSET_Z);
        } else {
            newOrigin = null;
        }

        Plot currentPlot = Flint.getUser().getPlot();

        if (locate.plot() != null) {
            if (currentPlot == null || !currentPlot.equals(locate.plot())) {
                Flint.getUser().setPlot(locate.plot());
            }
            if (Flint.getUser().getPlot().getDevOrigin() == null && newOrigin != null) {
                Flint.getUser().getPlot().setDevOrigin(newOrigin);
            }
        } else {
            Flint.getUser().setPlot(null);
        }

        requestEnabledPlotOwnerProfile(locate);
        Flint.getUser().setMode(locate.mode());
    }

    private static void requestEnabledPlotOwnerProfile(PlayerLocation locate) {
        if (!FlintAPI.shouldFetchPlotOwnerProfileWithWhois()) {
            return;
        }

        Plot plot = locate.plot();
        if (plot == null) {
            return;
        }

        if (plot.getOwner().equalsIgnoreCase(locate.player())) {
            WhoisFeature.requestWhois(locate.player())
                    .thenAccept(profile -> Flint.getClient().execute(() -> setCurrentPlotOwnerProfile(profile)));
            return;
        }

        requestPlotOwnerProfile(plot);
    }

    private static void requestUserProfile(String userName) {
        WhoisFeature.requestWhois(userName)
                .whenComplete((profile, throwable) -> Flint.getClient().execute(() -> {
                    if (throwable != null) {
                        clearRequestedUserProfile(userName);
                        return;
                    }

                    setCurrentUserProfile(profile);
                }));
    }

    private static void requestPlotOwnerProfile(Plot plot) {
        WhoisFeature.requestWhois(plot.getOwner())
                .thenAccept(profile -> Flint.getClient().execute(() -> setCurrentPlotOwnerProfile(profile)));
    }

    private static void setCurrentUserProfile(PlayerProfile profile) {
        ClientPlayerEntity player = Flint.getClient().player;
        if (player != null && player.getGameProfile().name().equalsIgnoreCase(profile.userName())) {
            requestedUserProfileName = profile.userName();
            Flint.getUser().setProfile(profile);
        }
    }

    private static void clearRequestedUserProfile(String userName) {
        if (requestedUserProfileName != null && requestedUserProfileName.equalsIgnoreCase(userName)) {
            requestedUserProfileName = null;
        }
    }

    private static void setCurrentPlotOwnerProfile(PlayerProfile profile) {
        Plot currentPlot = Flint.getUser().getPlot();
        if (currentPlot != null && currentPlot.getOwner().equalsIgnoreCase(profile.userName())) {
            currentPlot.setOwnerProfile(profile);
        }
    }

    private enum PendingModeSwitchAction {
        CLEAR_TITLE,
        POSITION_CHANGE,
        MESSAGE
    }

    private PlotSize detectPlotSize() {
        if (Flint.getUser().getMode() != Mode.DEV || Flint.getUser().getPlot() == null || Flint.getUser().getPlot().getDevOrigin() == null) {
            return null;
        }

        Plot plot = Flint.getUser().getPlot();
        Vec3i devOrigin = plot.getDevOrigin();

        BlockPos pos = new BlockPos(devOrigin.getX() - 1, 49, devOrigin.getZ());
        ClientWorld world = Flint.getClient().world;
        if (world == null) return null;

        BlockState BASIC = world.getBlockState(pos.south(50));
        BlockState BASIC_PLUS = world.getBlockState(pos.south(51));
        BlockState LARGE = world.getBlockState(pos.south(100));
        BlockState LARGE_PLUS = world.getBlockState(pos.south(101));
        BlockState MASSIVE = world.getBlockState(pos.south(300));
        BlockState MASSIVE_PLUS = world.getBlockState(pos.south(301));
        BlockState MEGA = world.getBlockState(pos.add(-18, 0, 10));
        BlockState MEGA_PLUS = world.getBlockState(pos.add(-19, 0, 10));

        if (MEGA_PLUS.isOf(Blocks.GRASS_BLOCK) && MEGA.isOf(Blocks.GRASS_BLOCK)) {
            return PlotSize.MEGA;
        } else if (!MEGA.isOf(Blocks.VOID_AIR) && !MEGA_PLUS.isOf(Blocks.VOID_AIR) && !MEGA.isOf(Blocks.GRASS_BLOCK) && !MEGA.isOf(Blocks.STONE) && !MEGA_PLUS.isOf(Blocks.GRASS_BLOCK)) {
            return PlotSize.MEGA;
        } else if (!(BASIC.isOf(Blocks.VOID_AIR) || BASIC_PLUS.isOf(Blocks.VOID_AIR)) && !BASIC.isOf(BASIC_PLUS.getBlock())) {
            return PlotSize.BASIC;
        } else if (!(LARGE.isOf(Blocks.VOID_AIR) || LARGE_PLUS.isOf(Blocks.VOID_AIR)) && !LARGE.isOf(LARGE_PLUS.getBlock())) {
            return PlotSize.LARGE;
        } else if (!(MASSIVE.isOf(Blocks.VOID_AIR) || MASSIVE_PLUS.isOf(Blocks.VOID_AIR)) && !MASSIVE.isOf(MASSIVE_PLUS.getBlock())) {
            return PlotSize.MASSIVE;
        } else {
            // unknown, maybe the world is still streaming in chunks
            return null;
        }
    }

    private boolean detectPlotUnderground() {
        Plot plot = Flint.getUser().getPlot();

        if (plot == null) return false;
        if (Flint.getClient().world == null) return false;

        PlotSize size = plot.getSize();
        BlockState groundCheck = Flint.getClient().world.getBlockState(new BlockPos(
                Math.clamp((int) Flint.getUser().getPlayer().getX(), plot.getDevOrigin().getX() - (size.getCodeWidth()), plot.getDevOrigin().getX() - 1),
                8,
                Math.clamp((int) Flint.getUser().getPlayer().getZ(), plot.getDevOrigin().getZ(), plot.getDevOrigin().getZ() + size.getCodeLength())
        ));

        if (!groundCheck.isOf(Blocks.VOID_AIR)) {
            return !groundCheck.isOf(Blocks.GRASS_BLOCK) && !groundCheck.isOf(Blocks.STONE);
        } else {
            return false;
        }
    }

}
