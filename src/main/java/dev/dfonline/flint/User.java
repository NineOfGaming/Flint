package dev.dfonline.flint;

import dev.dfonline.flint.feature.core.FeatureTraitType;
import dev.dfonline.flint.feature.trait.ModeSwitchListeningFeature;
import dev.dfonline.flint.feature.trait.PlotSwitchListeningFeature;
import dev.dfonline.flint.hypercube.Mode;
import dev.dfonline.flint.hypercube.Node;
import dev.dfonline.flint.hypercube.PlayerProfile;
import dev.dfonline.flint.hypercube.PlayerRanks;
import dev.dfonline.flint.hypercube.Plot;
import dev.dfonline.flint.util.message.Message;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores additional information about the client player.
 */
public final class User {

    private @NotNull Mode mode = Mode.NONE;
    private @Nullable Plot plot;
    private @Nullable Node node;
    private @Nullable String privateNodeId;
    private @Nullable PlayerProfile profile;
    private boolean locationConfirmed = false;

    public ClientPlayerEntity getPlayer() {
        ClientPlayerEntity player = Flint.getClient().player;

        if (player == null) {
            throw new NullPointerException("Player is null, User#getPlayer should only be used as a shorthand when it is known that the player is not null.");
        }
        return player;
    }

    public @NotNull Mode getMode() {
        return this.mode;
    }

    /**
     * @return Whether the current location state has been confirmed by /locate.
     */
    public boolean isLocationConfirmed() {
        return this.locationConfirmed;
    }

    /**
     * @return Whether the client is currently connected to DiamondFire.
     */
    public boolean isOnDiamondFire() {
        return !this.mode.isNone();
    }

    public boolean isAtSpawn() {
        return this.mode.isAtSpawn();
    }

    public boolean isConfirmedAtSpawn() {
        return this.locationConfirmed && this.isAtSpawn();
    }

    public boolean isInPlay() {
        return this.mode.isInPlay();
    }

    public boolean isConfirmedInPlay() {
        return this.locationConfirmed && this.isInPlay();
    }

    public boolean isInDev() {
        return this.mode.isInDev();
    }

    public boolean isConfirmedInDev() {
        return this.locationConfirmed && this.isInDev();
    }

    public boolean isInBuild() {
        return this.mode.isInBuild();
    }

    public boolean isConfirmedInBuild() {
        return this.locationConfirmed && this.isInBuild();
    }

    public boolean isCodeSpectating() {
        return this.mode.isCodeSpectating();
    }

    public boolean isConfirmedCodeSpectating() {
        return this.locationConfirmed && this.isCodeSpectating();
    }

    public boolean isInPlot() {
        return this.mode.isInPlot();
    }

    public boolean isConfirmedInPlot() {
        return this.locationConfirmed && this.isInPlot();
    }

    public boolean isEditor() {
        return this.mode.isEditor();
    }

    public boolean isConfirmedEditor() {
        return this.locationConfirmed && this.isEditor();
    }

    @ApiStatus.Internal
    public void setMode(@NotNull Mode mode) {
        Flint.FEATURE_MANAGER.getByTrait(FeatureTraitType.MODE_SWITCH_LISTENING).forEach(feature ->
                ((ModeSwitchListeningFeature) feature).onSwitchMode(this.mode, mode)
        );
        this.mode = mode;
    }

    public @Nullable Plot getPlot() {
        return this.plot;
    }

    /**
     * Gets the client's current position in the current plot's coordinate space.
     *
     * @return The plot-relative position, or {@code null} if the player, plot, or development origin is unavailable.
     */
    public @Nullable Vec3d getPlotPosition() {
        ClientPlayerEntity player = Flint.getClient().player;
        if (player == null || this.plot == null) {
            return null;
        }

        return this.plot.toRelativePosition(player.getEntityPos());
    }

    @ApiStatus.Internal
    public void setPlot(@Nullable Plot plot) {
        Flint.FEATURE_MANAGER.getByTrait(FeatureTraitType.PLOT_SWITCH_LISTENING).forEach(feature ->
                ((PlotSwitchListeningFeature) feature).onSwitchPlot(this.plot, plot)
        );
        this.plot = plot;
    }

    public @Nullable PlayerProfile getProfile() {
        return this.profile;
    }

    public PlayerRanks getRanks() {
        if (this.profile == null) {
            return PlayerRanks.EMPTY;
        }

        return this.profile.ranks();
    }

    @ApiStatus.Internal
    public void setProfile(@Nullable PlayerProfile profile) {
        this.profile = profile;
    }

    public @Nullable Node getNode() {
        return this.node;
    }

    /**
     * Gets the specific ID of the private node the client is currently on.
     *
     * @return An ID such as {@code private14}, or {@code null} when the client
     * is not on a numbered private node or the location is not known.
     */
    public @Nullable String getPrivateNodeId() {
        return this.privateNodeId;
    }

    @ApiStatus.Internal
    public void setNode(@Nullable Node node) {
        this.setNode(node, null);
    }

    @ApiStatus.Internal
    public void setNode(@Nullable Node node, @Nullable String privateNodeId) {
        this.node = node;
        this.privateNodeId = node == Node.PRIVATE ? privateNodeId : null;
    }

    @ApiStatus.Internal
    public void setLocationConfirmed(boolean locationConfirmed) {
        this.locationConfirmed = locationConfirmed;
    }

    public void sendMessage(Message message) {
        message.send();
    }

}
