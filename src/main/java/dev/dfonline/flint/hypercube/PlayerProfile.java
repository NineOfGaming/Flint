package dev.dfonline.flint.hypercube;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * General information about a DiamondFire player.
 */
public record PlayerProfile(String userName, @Nullable String userUuid, PlayerRanks ranks, List<PlayerBadge> badges,
                            @Nullable String pronouns, @Nullable String joined, @Nullable String about) {

    public PlayerProfile(String userName, @Nullable String userUuid, PlayerRanks ranks, List<PlayerBadge> badges, @Nullable String pronouns, @Nullable String joined, @Nullable String about) {
        this.userName = Objects.requireNonNull(userName, "userName");
        this.userUuid = userUuid;
        this.ranks = Objects.requireNonNull(ranks, "ranks");
        this.badges = List.copyOf(Objects.requireNonNull(badges, "badges"));
        this.pronouns = pronouns;
        this.joined = joined;
        this.about = about;
    }

    @SuppressWarnings("unused")
    public List<String> getBadgeTexts() {
        ArrayList<String> badgeTexts = new ArrayList<>();
        for (PlayerBadge badge : this.badges) {
            badgeTexts.add(badge.text());
        }
        return badgeTexts;
    }

    @SuppressWarnings("unused")
    public String getBadgesReadableString() {
        if (this.badges.isEmpty()) {
            return "none";
        }

        return this.badges.stream()
                .map(PlayerBadge::toReadableString)
                .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("unused")
    public String toReadableString() {
        String joinedString = this.joined == null ? "unknown" : this.joined;
        return "name " + this.userName + ", ranks " + this.ranks.toReadableString() + ", joined " + joinedString;
    }

}
