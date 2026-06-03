package dev.dfonline.flint.util;

import dev.dfonline.flint.hypercube.PlayerBadge;
import dev.dfonline.flint.hypercube.PlayerLocation;
import dev.dfonline.flint.hypercube.PlayerProfile;
import dev.dfonline.flint.hypercube.Plot;
import net.kyori.adventure.text.Component;

public final class DebugReportUtil {

    private DebugReportUtil() {
    }

    public static Component formatLocateResult(PlayerLocation locate) {
        Component report = debugReport("Locate Test");
        report = appendValue(report, "Player", locate.player());
        report = appendValue(report, "Mode", locate.mode().getName());
        report = appendValue(report, "Mode Enum", locate.mode().name());
        report = appendValue(report, "Node", locate.node() == null ? null : locate.node().getName());
        report = appendValue(report, "Node Enum", locate.node() == null ? null : locate.node().name());
        report = appendValue(report, "Node Id", locate.node() == null ? null : locate.node().getId());

        Plot plot = locate.plot();
        if (plot == null) {
            return appendValue(report, "Plot", "none");
        }

        report = appendSection(report, "Plot");
        report = appendValue(report, "Plot Id", plot.getId());
        report = appendValue(report, "Name", plot.getName().getString());
        report = appendValue(report, "Handle", plot.getHandle());
        report = appendValue(report, "Whitelisted", plot.isWhitelisted());
        report = appendValue(report, "Owner", plot.getOwner());
        report = appendValue(report, "Owner Profile Loaded", plot.getOwnerProfile() != null);
        report = appendValue(report, "Owner Ranks", plot.getOwnerRanks().toReadableString());
        report = appendValue(report, "Dev Origin", plot.getDevOrigin());
        report = appendValue(report, "Detected Size", plot.getDetectedSize());
        report = appendValue(report, "Assumed Size", plot.getSize());
        report = appendValue(report, "Has Underground", plot.hasUnderground());
        report = appendValue(report, "Code Bounds", plot.getCodeBoundsString());
        return report;
    }

    public static Component formatProfileResult(PlayerProfile profile) {
        Component report = debugReport("Whois Test");
        report = appendValue(report, "Username", profile.userName());
        report = appendValue(report, "Pronouns", profile.pronouns());
        report = appendValue(report, "Joined", profile.joined());
        report = appendValue(report, "About", profile.about());
        report = appendValue(report, "Ranks", profile.ranks().toReadableString());
        report = appendValue(report, "Primary Rank", profile.ranks().getPrimaryRankName());
        report = appendValue(report, "Rank Levels", profile.ranks().toLevelString());

        if (profile.badges().isEmpty()) {
            return appendValue(report, "Badges", "none");
        }

        report = appendSection(report, "Badges");
        for (int i = 0; i < profile.badges().size(); i++) {
            PlayerBadge badge = profile.badges().get(i);
            report = appendValue(report, "Badge " + (i + 1), "text " + readable(badge.text().getString()) + ", name " + readable(badge.name().getString()) + ", description " + readableText(badge.description()));
        }
        return report;
    }

    private static Component debugReport(String title) {
        return Component.text(title, PaletteColor.PINK_LIGHT);
    }

    private static Component appendSection(Component report, String section) {
        return report.append(Component.newline())
                .append(Component.text(section + ":", PaletteColor.PINK_LIGHT));
    }

    private static Component appendValue(Component report, String key, Object value) {
        return report.append(Component.newline())
                .append(Component.text(key, PaletteColor.PINK_LIGHT))
                .append(Component.text(": ", PaletteColor.GRAY_DARK))
                .append(Component.text(readable(value), PaletteColor.WHITE));
    }

    private static String readable(Object value) {
        if (value == null) {
            return "unknown";
        }

        if (value instanceof String string && string.isBlank()) {
            return "none";
        }

        return value.toString();
    }

    private static String readable(String value) {
        return readable((Object) value);
    }

    private static String readableText(net.minecraft.text.Text value) {
        return value == null ? readable((Object) null) : readable(value.getString());
    }

}
