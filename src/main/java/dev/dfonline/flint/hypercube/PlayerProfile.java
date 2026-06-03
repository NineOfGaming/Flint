package dev.dfonline.flint.hypercube;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * General information about a DiamondFire player.
 */
public record PlayerProfile(String userName, PlayerRanks ranks, List<PlayerBadge> badges,
                            @Nullable String pronouns, @Nullable String joined, @Nullable String about) {

    public PlayerProfile(String userName, PlayerRanks ranks, List<PlayerBadge> badges, @Nullable String pronouns, @Nullable String joined, @Nullable String about) {
        this.userName = Objects.requireNonNull(userName, "userName");
        this.ranks = Objects.requireNonNull(ranks, "ranks");
        this.badges = List.copyOf(Objects.requireNonNull(badges, "badges"));
        this.pronouns = pronouns;
        this.joined = joined;
        this.about = about;
    }

    public List<Text> getBadgeTexts() {
        ArrayList<Text> badgeTexts = new ArrayList<>();
        for (PlayerBadge badge : this.badges) {
            badgeTexts.add(badge.text());
        }
        return badgeTexts;
    }

    public String getBadgesReadableString() {
        if (this.badges.isEmpty()) {
            return "none";
        }

        return this.badges.stream()
                .map(PlayerBadge::toReadableString)
                .collect(Collectors.joining(", "));
    }

    public String toReadableString() {
        String joinedString = this.joined == null ? "unknown" : this.joined;
        return "name " + this.userName + ", ranks " + this.ranks.toReadableString() + ", joined " + joinedString;
    }

}
