package dev.dfonline.flint.feature.impl;

import dev.dfonline.flint.Flint;
import dev.dfonline.flint.FlintAPI;
import dev.dfonline.flint.User;
import dev.dfonline.flint.feature.trait.RenderedFeature;
import dev.dfonline.flint.hypercube.Mode;
import dev.dfonline.flint.hypercube.Node;
import dev.dfonline.flint.hypercube.PlayerProfile;
import dev.dfonline.flint.hypercube.PlayerRanks;
import dev.dfonline.flint.hypercube.Plot;
import dev.dfonline.flint.hypercube.PlotSize;
import dev.dfonline.flint.hypercube.ServerPatches;
import dev.dfonline.flint.util.ObjectUtil;
import dev.dfonline.flint.util.PaletteColor;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.minecraft.text.Text.literal;

public class StateDebugDisplayFeature implements RenderedFeature {

    private static final int STARTING_Y = 5;
    private static final int STARTING_X = 5;
    private static final int END_PADDING = 5;
    private static final String TRUNCATION_SUFFIX = "...";

    @Override
    public void render(DrawContext draw, RenderTickCounter renderTickCounter) {
        User user = Flint.getUser();

        ArrayList<Text> texts = new ArrayList<>();
        texts.add(literal("General State:").withColor(PaletteColor.PURPLE.value()));
        texts.add(formatValue("Mode", ObjectUtil.toString(user.getMode(), Mode::getName)));
        texts.add(formatValue("Location Confirmed", user.isLocationConfirmed() + ""));
        texts.add(formatValue("In Plot", user.isInPlot() + ""));
        texts.add(formatValue("Is Editor", user.isEditor() + ""));
        texts.add(formatValue("Node", ObjectUtil.toString(user.getNode(), Node::getName)));
        texts.add(formatValue("Node Id", ObjectUtil.toString(user.getNode(), Node::getId)));
        texts.add(formatValue("Patch", ObjectUtil.toString(ServerPatches.current().getForNode(user.getNode()), Object::toString)));
        texts.add(formatValue("Command Queue", CommandSenderFeature.queueSize() + ""));
        texts.add(formatValue("Command Rate Limited", CommandSenderFeature.isRateLimited() + ""));
        texts.add(formatValue("Command Rate Limit", CommandSenderFeature.rateLimitCount() + "/" + CommandSenderFeature.rateLimitThreshold()));
        texts.add(formatValue("Command Rate Increment", CommandSenderFeature.rateLimitIncrementStep() + ""));

        this.addPlayerState(texts, user);
        this.addPlotState(texts, user);
        this.addOwnerState(texts, user.getPlot());

        this.renderTexts(texts, draw);
    }

    private void addPlayerState(List<Text> texts, User user) {
        ClientPlayerEntity player = Flint.getClient().player;
        PlayerProfile profile = user.getProfile();

        texts.add(formatHeader("Player:"));
        texts.add(formatValue("Name", ObjectUtil.toString(player, playerEntity -> playerEntity.getGameProfile().name())));
        texts.add(formatValue("Position", ObjectUtil.toString(player, playerEntity -> formatPosition(playerEntity.getEntityPos()))));
        texts.add(formatValue("Plot Position", ObjectUtil.toString(user.getPlotPosition(), StateDebugDisplayFeature::formatPosition, "unknown")));
        addProfileState(texts, profile, user.getRanks());
    }

    private void addPlotState(List<Text> texts, User user) {
        Plot plot = user.getPlot();
        ClientPlayerEntity player = Flint.getClient().player;

        texts.add(formatHeader("Plot:"));
        texts.add(formatValue("Plot Loaded", (plot != null) + ""));
        texts.add(formatValue("Id", ObjectUtil.toString(plot, plotData -> plotData.getId() + "")));
        texts.add(formatValue("Name", ObjectUtil.toString(plot, plotData -> plotData.getName().getString())));
        texts.add(formatValue("Handle", ObjectUtil.toString(plot, plotData -> readable(plotData.getHandle()))));
        texts.add(formatValue("Whitelisted", ObjectUtil.toString(plot, plotData -> plotData.isWhitelisted() + "")));
        texts.add(formatValue("Size Known", ObjectUtil.toString(plot, plotData -> plotData.isSizeKnown() + "")));
        texts.add(formatValue("Detected Size", ObjectUtil.toString(plot, plotData -> ObjectUtil.toString(plotData.getDetectedSize(), PlotSize::name, "unknown"))));
        texts.add(formatValue("Assumed Size", ObjectUtil.toString(plot, plotData -> plotData.getSize().name())));
        texts.add(formatValue("Has Underground", ObjectUtil.toString(plot, plotData -> plotData.hasUnderground() + "")));
        texts.add(formatValue("Dev Origin", ObjectUtil.toString(plot, plotData -> ObjectUtil.toString(plotData.getDevOrigin(), StateDebugDisplayFeature::formatPosition))));
        texts.add(formatValue("Code Bounds", ObjectUtil.toString(plot, plotData -> ObjectUtil.toString(plotData.getCodeBoundsString(), Object::toString, "unknown"))));
        texts.add(formatValue("In Code Space", inCodeSpace(plot, player)));
    }

    private void addOwnerState(List<Text> texts, Plot plot) {
        PlayerProfile ownerProfile = plot == null ? null : plot.getOwnerProfile();
        PlayerRanks ownerRanks = plot == null ? PlayerRanks.EMPTY : plot.getOwnerRanks();

        texts.add(formatHeader("Owner:"));
        texts.add(formatValue("Name", ObjectUtil.toString(plot, Plot::getOwner)));
        addProfileState(texts, ownerProfile, ownerRanks);
    }

    private static void addProfileState(List<Text> texts, PlayerProfile profile, PlayerRanks ranks) {
        texts.add(formatValue("Profile Loaded", (profile != null) + ""));
        texts.add(formatValue("Ranks", ranks.toReadableString()));
        texts.add(formatValue("Rank Levels", ranks.toLevelString()));
    }

    private void renderTexts(ArrayList<Text> texts, DrawContext draw) {
        int y = STARTING_Y;
        for (int i = 0; i < texts.size(); i++) {
            Text text = texts.get(i);
            if (i == 0) {
                draw.drawTextWithShadow(Flint.getClient().textRenderer, text, STARTING_X, y, 0xFF_FFFFFF);
                y += 1;
            } else if (text.getString().endsWith(":")) {
                y += 1;
                draw.drawTextWithShadow(Flint.getClient().textRenderer, text, STARTING_X, y, 0xFF_FFFFFF);
            } else {
                draw.drawTextWithShadow(Flint.getClient().textRenderer, text, STARTING_X + STARTING_Y, y, 0xFF_FFFFFF);
            }
            y += Flint.getClient().textRenderer.fontHeight + 1;
        }
    }

    private static Text formatHeader(String header) {
        return literal(header).withColor(PaletteColor.PURPLE.value());
    }

    private static Text formatValue(String key, String value) {
        return literal(key).withColor(PaletteColor.PURPLE.value())
                .append(literal(" = ").withColor(PaletteColor.GRAY_DARK.value()))
                .append(literal(truncateValue(key, value)).withColor(PaletteColor.PURPLE_LIGHT.value()));
    }

    private static String inCodeSpace(Plot plot, ClientPlayerEntity player) {
        if (plot == null || player == null || plot.getDevOrigin() == null) {
            return "unknown";
        }

        return plot.isPosInCodeSpace(player.getBlockPos()) + "";
    }

    private static String readable(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private static String formatPosition(Vec3d position) {
        return formatPosition(position.x, position.y, position.z);
    }

    private static String formatPosition(Vec3i position) {
        return formatPosition(position.getX(), position.getY(), position.getZ());
    }

    private static String formatPosition(double x, double y, double z) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", x, y, z);
    }

    private static String truncateValue(String key, String value) {
        TextRenderer textRenderer = Flint.getClient().textRenderer;
        int valueStartX = STARTING_X + STARTING_Y + textRenderer.getWidth(key + " = ");
        int maxValueWidth = Flint.getClient().getWindow().getScaledWidth() - valueStartX - END_PADDING;

        if (maxValueWidth <= 0 || textRenderer.getWidth(value) <= maxValueWidth) {
            return value;
        }

        int suffixWidth = textRenderer.getWidth(TRUNCATION_SUFFIX);
        if (maxValueWidth <= suffixWidth) {
            return TRUNCATION_SUFFIX;
        }

        return textRenderer.trimToWidth(value, maxValueWidth - suffixWidth) + TRUNCATION_SUFFIX;
    }

    @Override
    public boolean isEnabled() {
        return FlintAPI.isDebugging();
    }

}
