package dev.dfonline.flint.hypercube;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A player's visible ranks plus permission-style category levels.
 */
public final class PlayerRanks {

    public static final PlayerRanks EMPTY = new PlayerRanks(List.of());

    private final List<PlayerRank> ranks;
    private final EnumMap<PlayerRankCategory, Integer> levels = new EnumMap<>(PlayerRankCategory.class);

    public PlayerRanks(List<PlayerRank> ranks) {
        this.ranks = List.copyOf(Objects.requireNonNull(ranks, "ranks"));

        for (PlayerRank rank : ranks) {
            this.levels.merge(rank.category(), rank.level(), Math::max);
        }
    }

    @SuppressWarnings("unused")
    public List<PlayerRank> getRanks() {
        return this.ranks;
    }

    public boolean isEmpty() {
        return this.ranks.isEmpty();
    }

    public int getLevel(PlayerRankCategory category) {
        return this.levels.getOrDefault(category, 0);
    }

    public int getDonor() {
        return this.getLevel(PlayerRankCategory.DONOR);
    }

    public int getVip() {
        return this.getLevel(PlayerRankCategory.VIP);
    }

    public int getQa() {
        return this.getLevel(PlayerRankCategory.QA);
    }

    public int getYoutuber() {
        return this.getLevel(PlayerRankCategory.YOUTUBER);
    }

    public int getSupport() {
        return this.getLevel(PlayerRankCategory.SUPPORT);
    }

    public int getModeration() {
        return this.getLevel(PlayerRankCategory.MODERATION);
    }

    public int getAdmin() {
        return this.getLevel(PlayerRankCategory.ADMIN);
    }

    @SuppressWarnings("unused")
    public @Nullable PlayerRank getHighestRank(PlayerRankCategory category) {
        PlayerRank highestRank = null;
        for (PlayerRank rank : this.ranks) {
            if (rank.category() != category) {
                continue;
            }
            if (highestRank == null || rank.level() > highestRank.level()) {
                highestRank = rank;
            }
        }
        return highestRank;
    }

    public @Nullable PlayerRank getPrimaryRank() {
        PlayerRank primaryRank = null;
        for (PlayerRank rank : this.ranks) {
            if (primaryRank == null || priority(rank) > priority(primaryRank)) {
                primaryRank = rank;
            }
        }
        return primaryRank;
    }

    public String getPrimaryRankName() {
        PlayerRank primaryRank = this.getPrimaryRank();
        return primaryRank == null ? "none" : primaryRank.name();
    }

    public String toLevelString() {
        return "donor " + this.getDonor()
                + ", vip " + this.getVip()
                + ", qa " + this.getQa()
                + ", yt " + this.getYoutuber()
                + ", support " + this.getSupport()
                + ", mod " + this.getModeration()
                + ", admin " + this.getAdmin();
    }

    public String toReadableString() {
        if (this.ranks.isEmpty()) {
            return "none";
        }

        return this.ranks.stream()
                .map(PlayerRank::name)
                .collect(Collectors.joining(", "));
    }

    private static int priority(PlayerRank rank) {
        int categoryPriority = switch (rank.category()) {
            case ADMIN -> 800;
            case MODERATION -> 700;
            case SUPPORT -> 600;
            case QA -> 500;
            case YOUTUBER -> 400;
            case DONOR -> 300;
            case VIP -> 200;
            case SPECIAL -> 100;
            case UNKNOWN -> 0;
        };

        return categoryPriority + rank.level();
    }

    @Override
    public String toString() {
        return this.toReadableString();
    }

}
