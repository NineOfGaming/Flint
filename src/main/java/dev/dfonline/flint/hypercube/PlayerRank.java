package dev.dfonline.flint.hypercube;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

/**
 * Represents a visible rank from a player's profile.
 *
 * @param name     The display name of the rank.
 * @param category The rank category.
 * @param level    The rank level inside the category.
 */
public record PlayerRank(String name, PlayerRankCategory category, int level) {

    private static final int EMERITUS_RETIRED_TEXT_COLOR = 0x2AD4D4;
    private static final int EMERITUS_RETIRED_BRACKET_COLOR = 0xFFAA00;

    public PlayerRank {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
    }

    public String getName() {
        return this.name;
    }

    @SuppressWarnings("unused")
    public PlayerRankCategory getCategory() {
        return this.category;
    }

    @SuppressWarnings("unused")
    public int getLevel() {
        return this.level;
    }

    public static PlayerRank fromName(String name) {
        return fromName(name, null);
    }

    public static PlayerRank fromName(String name, @Nullable Integer textColor) {
        return fromName(name, textColor, null);
    }

    public static PlayerRank fromName(String name, @Nullable Integer textColor, @Nullable Integer bracketColor) {
        String cleanName = normalizeName(name);
        String key = cleanName.toLowerCase(Locale.ROOT).replace(" ", "");

        return switch (key) {
            case "noble" -> new PlayerRank("Noble", PlayerRankCategory.DONOR, 1);
            case "emperor" -> new PlayerRank("Emperor", PlayerRankCategory.DONOR, 2);
            case "mythic" -> new PlayerRank("Mythic", PlayerRankCategory.DONOR, 3);
            case "overlord" -> new PlayerRank("Overlord", PlayerRankCategory.DONOR, 4);
            case "vip" -> new PlayerRank("VIP", PlayerRankCategory.VIP, 1);
            case "qa" -> new PlayerRank("QA", PlayerRankCategory.QA, 1);
            case "youtuber" -> new PlayerRank("YouTube", PlayerRankCategory.YOUTUBER, 1);
            case "jrhelper" -> new PlayerRank("JrHelper", PlayerRankCategory.SUPPORT, 1);
            case "helper" -> new PlayerRank("Helper", PlayerRankCategory.SUPPORT, 2);
            case "srhelper" -> new PlayerRank("SrHelper", PlayerRankCategory.SUPPORT, 3);
            case "jrmod" -> new PlayerRank("JrMod", PlayerRankCategory.MODERATION, 1);
            case "mod" -> new PlayerRank("Mod", PlayerRankCategory.MODERATION, 2);
            case "srmod" -> new PlayerRank("SrMod", PlayerRankCategory.MODERATION, 3);
            case "dev" -> new PlayerRank("Dev", PlayerRankCategory.ADMIN, 1);
            case "admin" -> new PlayerRank("Admin", PlayerRankCategory.ADMIN, 2);
            case "owner" -> new PlayerRank("Owner", PlayerRankCategory.ADMIN, 3);
            case "sponsor" -> new PlayerRank("Sponsor", PlayerRankCategory.SPECIAL, 1);
            case "builder" -> new PlayerRank("Builder", PlayerRankCategory.SPECIAL, 1);
            case "retired" -> new PlayerRank(isEmeritusRetired(textColor, bracketColor) ? "Emeritus" : "Retired", PlayerRankCategory.SPECIAL, 1);
            case "vip creator" -> new PlayerRank("VIP Creator", PlayerRankCategory.SPECIAL, 1);
            case "curator" -> new PlayerRank("Curator", PlayerRankCategory.SPECIAL, 1);
            default -> new PlayerRank(cleanName, PlayerRankCategory.UNKNOWN, 0);
        };
    }

    private static boolean isEmeritusRetired(@Nullable Integer textColor, @Nullable Integer bracketColor) {
        return colorMatches(textColor, EMERITUS_RETIRED_TEXT_COLOR)
                && colorMatches(bracketColor, EMERITUS_RETIRED_BRACKET_COLOR);
    }

    private static boolean colorMatches(@Nullable Integer color, int expectedColor) {
        return color != null && (color & 0xFFFFFF) == expectedColor;
    }

    private static String normalizeName(String name) {
        return name.replaceAll("[^A-Za-z0-9 _-]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

}
