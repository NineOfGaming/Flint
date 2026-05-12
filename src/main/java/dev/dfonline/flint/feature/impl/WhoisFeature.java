package dev.dfonline.flint.feature.impl;

import dev.dfonline.flint.feature.trait.PacketListeningFeature;
import dev.dfonline.flint.hypercube.PlayerBadge;
import dev.dfonline.flint.hypercube.PlayerProfile;
import dev.dfonline.flint.hypercube.PlayerRank;
import dev.dfonline.flint.hypercube.PlayerRanks;
import dev.dfonline.flint.util.Toaster;
import dev.dfonline.flint.util.result.EventResult;
import net.kyori.adventure.text.Component;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles requesting whois commands and serving their responses in a structured manner.
 */
public class WhoisFeature implements PacketListeningFeature {

    private static final Pattern PROFILE_PATTERN = Pattern.compile("Profile of\\s+(?<username>[A-Za-z0-9_]+)(?:\\s+\\((?<pronouns>[^\\n)]+)\\))?");
    private static final Pattern RANK_PATTERN = Pattern.compile("\\[(?<rank>[^]]+)]");
    private static final Queue<WhoisRequest> whoisRequests = new LinkedList<>();
    private static final Map<String, CompletableFuture<PlayerProfile>> pendingRequests = new HashMap<>();
    private static final Map<String, CachedProfile> profileCache = new HashMap<>();
    private static final int WHOIS_TIMEOUT_SECONDS = 5;
    private static final long CACHE_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private static boolean awaitingResponse = false;

    @Override
    public boolean alwaysOn() {
        return true;
    }

    public static CompletableFuture<PlayerProfile> requestWhois(String playerName) {
        String normalizedPlayerName = playerName.trim();
        String cacheKey = cacheKey(normalizedPlayerName);
        CachedProfile cachedProfile = profileCache.get(cacheKey);
        if (cachedProfile != null && !cachedProfile.isExpired()) {
            return CompletableFuture.completedFuture(cachedProfile.profile());
        }

        CompletableFuture<PlayerProfile> pendingRequest = pendingRequests.get(cacheKey);
        if (pendingRequest != null) {
            return pendingRequest;
        }

        CompletableFuture<PlayerProfile> profileResult = new CompletableFuture<>();
        WhoisRequest request = new WhoisRequest(normalizedPlayerName, cacheKey, profileResult);

        pendingRequests.put(cacheKey, profileResult);
        whoisRequests.add(request);

        CompletableFuture.delayedExecutor(WHOIS_TIMEOUT_SECONDS, TimeUnit.SECONDS).execute(() -> {
            if (!profileResult.isDone()) {
                profileResult.completeExceptionally(new TimeoutException("Whois request timed out after " + WHOIS_TIMEOUT_SECONDS + " second(s)"));
                pendingRequests.remove(cacheKey);
                Toaster.toast(Component.translatable("flint.whois.timeout.title"), Component.translatable("flint.whois.timeout.description", Component.text(WHOIS_TIMEOUT_SECONDS)));

                if (awaitingResponse && !whoisRequests.isEmpty() && whoisRequests.peek().result() == profileResult) {
                    whoisRequests.poll();
                    awaitingResponse = false;
                    processNextRequestIfReady();
                } else {
                    whoisRequests.removeIf(pair -> pair.result() == profileResult);
                }
            }
        });

        processNextRequestIfReady();

        return profileResult;
    }

    public static void clearCache() {
        profileCache.clear();
    }

    private static void processNextRequestIfReady() {
        if (!awaitingResponse && !whoisRequests.isEmpty()) {
            awaitingResponse = true;
            WhoisRequest currentRequest = whoisRequests.peek();

            CommandSenderFeature.queue("whois " + currentRequest.playerName());
        }
    }

    @Override
    public EventResult onReceivePacket(Packet<?> packet) {
        if (!(packet instanceof GameMessageS2CPacket message)) {
            return EventResult.PASS;
        }

        if (whoisRequests.isEmpty() || !awaitingResponse) {
            return EventResult.PASS;
        }

        Text content = message.content();
        String text = Formatting.strip(content.getString());
        PlayerProfile profile = parseWhoisResponse(content, text);

        if (profile == null) {
            return EventResult.PASS;
        }

        WhoisRequest currentRequest = whoisRequests.poll();
        if (currentRequest == null) {
            return EventResult.PASS;
        }
        cacheProfile(currentRequest.cacheKey(), profile);
        cacheProfile(cacheKey(profile.userName()), profile);
        pendingRequests.remove(currentRequest.cacheKey());

        currentRequest.result().complete(profile);
        awaitingResponse = false;

        processNextRequestIfReady();

        return EventResult.CANCEL;
    }

    private static PlayerProfile parseWhoisResponse(Text content, String text) {
        if (text == null) {
            return null;
        }

        String normalizedText = normalizeText(text);
        Matcher profileMatcher = PROFILE_PATTERN.matcher(normalizedText);

        if (!profileMatcher.find()) {
            return null;
        }

        String userName = profileMatcher.group("username");
        String pronouns = normalizeBlank(profileMatcher.group("pronouns"));
        PlayerRanks ranks = parseRanks(content, readLineValue(normalizedText, "Ranks"));
        List<PlayerBadge> badges = parseBadges(readLineValue(normalizedText, "Badges"), content);
        String joined = normalizeBlank(readLineValue(normalizedText, "Joined"));
        String about = normalizeBlank(readLineValue(normalizedText, "About"));

        return new PlayerProfile(userName, null, ranks, badges, pronouns, joined, about);
    }

    private static PlayerRanks parseRanks(Text content, String rawRanks) {
        List<RankToken> rankTokens = parseStyledRanks(content);
        if (!rankTokens.isEmpty()) {
            ArrayList<PlayerRank> ranks = new ArrayList<>();
            for (RankToken rankToken : rankTokens) {
                ranks.add(PlayerRank.fromName(rankToken.name(), rankToken.textColor(), rankToken.bracketColor()));
            }
            return new PlayerRanks(ranks);
        }

        return parseRanks(rawRanks);
    }

    private static PlayerRanks parseRanks(String rawRanks) {
        if (rawRanks == null || rawRanks.isBlank()) {
            return PlayerRanks.EMPTY;
        }

        ArrayList<PlayerRank> ranks = new ArrayList<>();
        Matcher rankMatcher = RANK_PATTERN.matcher(rawRanks);

        while (rankMatcher.find()) {
            String rankName = normalizeBlank(rankMatcher.group("rank"));
            if (rankName != null) {
                ranks.add(PlayerRank.fromName(rankName));
            }
        }

        return new PlayerRanks(ranks);
    }

    private static List<RankToken> parseStyledRanks(Text content) {
        ArrayList<StyledCharacter> characters = flattenStyledCharacters(content);
        StringBuilder plainText = new StringBuilder();
        for (StyledCharacter character : characters) {
            plainText.append(character.value());
        }

        int rankLineStart = plainText.indexOf("Ranks:");
        if (rankLineStart == -1) {
            return List.of();
        }

        rankLineStart += "Ranks:".length();
        int rankLineEnd = plainText.indexOf("\n", rankLineStart);
        if (rankLineEnd == -1) {
            rankLineEnd = plainText.length();
        }

        ArrayList<RankToken> ranks = new ArrayList<>();
        StringBuilder rankName = new StringBuilder();
        Integer rankTextColor = null;
        Integer rankBracketColor = null;
        boolean readingRank = false;

        for (int i = rankLineStart; i < rankLineEnd; i++) {
            StyledCharacter character = characters.get(i);
            char value = character.value();

            if (value == '[') {
                readingRank = true;
                rankName.setLength(0);
                rankTextColor = null;
                rankBracketColor = character.textColor();
                continue;
            }

            if (value == ']' && readingRank) {
                String normalizedRankName = normalizeBlank(rankName.toString());
                if (normalizedRankName != null) {
                    ranks.add(new RankToken(normalizedRankName, rankTextColor, rankBracketColor));
                }
                readingRank = false;
                continue;
            }

            if (!readingRank) {
                continue;
            }

            rankName.append(value);
            if (rankTextColor == null && isRankNameCharacter(value)) {
                rankTextColor = character.textColor();
            }
        }

        return ranks;
    }

    private static ArrayList<StyledCharacter> flattenStyledCharacters(Text content) {
        ArrayList<StyledCharacter> characters = new ArrayList<>();
        content.visit((style, text) -> {
            TextColor textColor = style.getColor();
            Integer color = textColor == null ? null : textColor.getRgb();
            for (int i = 0; i < text.length(); i++) {
                characters.add(new StyledCharacter(text.charAt(i), color));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return characters;
    }

    private static boolean isRankNameCharacter(char character) {
        return Character.isLetterOrDigit(character);
    }

    private static List<PlayerBadge> parseBadges(String rawBadges, Text content) {
        ArrayList<PlayerBadge> badges = new ArrayList<>();
        String badgeLine = rawBadges == null ? "" : normalizeBadgeText(rawBadges);
        collectBadges(content, badges, badgeLine);
        if (!badges.isEmpty()) {
            return badges;
        }

        if (rawBadges == null || rawBadges.isBlank()) {
            return List.of();
        }

        for (String badgeText : rawBadges.trim().split("\\s+")) {
            if (!badgeText.isBlank()) {
                badges.add(new PlayerBadge(badgeText, badgeText, null));
            }
        }
        return badges;
    }

    private static void collectBadges(Text text, List<PlayerBadge> badges, String badgeLine) {
        HoverEvent hoverEvent = text.getStyle().getHoverEvent();
        if (hoverEvent instanceof HoverEvent.ShowText(Text value)) {
            String visibleText = normalizeBadgeText(text.getString());
            String hoverText = normalizeText(value.getString());

            if (!visibleText.isBlank() && !hoverText.isBlank() && (badgeLine.isBlank() || badgeLine.contains(visibleText))) {
                String[] hoverLines = hoverText.split("\\n", 2);
                String name = normalizeBlank(hoverLines[0]);
                String description = hoverLines.length > 1 ? normalizeBlank(hoverLines[1]) : null;

                if (name != null && badges.stream().noneMatch(badge -> badge.text().equals(visibleText) && badge.name().equals(name))) {
                    badges.add(new PlayerBadge(visibleText, name, description));
                }
            }
        }

        for (Text sibling : text.getSiblings()) {
            collectBadges(sibling, badges, badgeLine);
        }
    }

    private static String normalizeBadgeText(String text) {
        String normalizedText = normalizeText(text).replaceAll("\\s+", " ");
        return normalizedText.trim();
    }

    private static String readLineValue(String text, String label) {
        Pattern pattern = Pattern.compile("(?m)(?:^|\\n)[^\\n]*" + Pattern.quote(label) + ":\\s*(?<value>[^\\n]*)");
        Matcher matcher = pattern.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group("value");
    }

    private static String normalizeText(String text) {
        return text.replace('\r', '\n')
                .replaceAll("[\\x{00A0}\\x{200B}\\x{200C}\\x{200D}\\x{FEFF}]", "")
                .trim();
    }

    private static String normalizeBlank(String text) {
        if (text == null) {
            return null;
        }

        String normalizedText = normalizeText(text).replaceAll("\\s+", " ");
        return normalizedText.isBlank() ? null : normalizedText;
    }

    private static void cacheProfile(String cacheKey, PlayerProfile profile) {
        profileCache.put(cacheKey, new CachedProfile(profile, System.currentTimeMillis()));
    }

    private static String cacheKey(String playerName) {
        return playerName.trim().toLowerCase(Locale.ROOT);
    }

    private record WhoisRequest(String playerName, String cacheKey, CompletableFuture<PlayerProfile> result) {
    }

    private record StyledCharacter(char value, Integer textColor) {
    }

    private record RankToken(String name, Integer textColor, Integer bracketColor) {
    }

    private record CachedProfile(PlayerProfile profile, long timestamp) {

        private boolean isExpired() {
            return System.currentTimeMillis() - this.timestamp > CACHE_DURATION_MILLIS;
        }

    }

}
