package dev.dfonline.flint.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dfonline.flint.Flint;
import dev.dfonline.flint.util.message.impl.prefix.InfoMessage;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public final class FlintUpdate {

    private static final Logger LOGGER = Logger.of(FlintUpdate.class);
    private static final String MODRINTH_PROJECT = "dBv9so2c";
    private static final String MOD_LOADER = "fabric";
    private static final String MODRINTH_URL = "https://modrinth.com/mod/flint/versions";
    private static final String MOD_VERSION = getCurrentVersion();
    private static final String UNKNOWN_VERSION = "unknown";
    private static @Nullable String latestVersion = null;

    private FlintUpdate() {
    }

    public static void fetchLatestRelease() {
        String minecraftVersion = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse(UNKNOWN_VERSION);

        if (minecraftVersion.equals(UNKNOWN_VERSION)) {
            LOGGER.error("Failed to get the current Minecraft version");
            return;
        }

        String url = String.format(
                "https://api.modrinth.com/v2/project/%s/version?game_versions=%s&loaders=%s&include_changelog=false",
                MODRINTH_PROJECT,
                encodeFilter(minecraftVersion),
                encodeFilter(MOD_LOADER)
        );

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "DFOnline/Flint/" + MOD_VERSION)
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            throw new RuntimeException("Expected a successful response, instead got: " + response.statusCode());
                        }
                        return response.body();
                    })
                    .thenAccept(responseBody -> {
                        JsonArray versions = JsonParser.parseString(responseBody).getAsJsonArray();
                        if (versions.isEmpty()) {
                            LOGGER.info("No Flint releases found for Minecraft {}", minecraftVersion);
                            return;
                        }

                        JsonObject latestRelease = versions.get(0).getAsJsonObject();
                        if (!latestRelease.has("version_number")) {
                            throw new RuntimeException("Expected a response with version_number, instead got: " + responseBody);
                        }

                        latestVersion = latestRelease.get("version_number").getAsString().replaceFirst("^v", "");
                        LOGGER.info("Latest version for Minecraft {}: v{}", minecraftVersion, latestVersion);
                    })
                    .exceptionally(e -> {
                        LOGGER.error("Error while fetching version", e);
                        return null;
                    });
        }
    }

    private static String encodeFilter(String value) {
        JsonArray filter = new JsonArray();
        filter.add(value);
        return URLEncoder.encode(filter.toString(), StandardCharsets.UTF_8);
    }

    private static String getCurrentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(Flint.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse(UNKNOWN_VERSION);
    }


    public static void sendUpdateMessage() {
        if (latestVersion == null || MOD_VERSION.equals(latestVersion) || MOD_VERSION.equals(UNKNOWN_VERSION)) {
            return;
        }

        try {
            // We are outdated, inform the user.
            if (Flint.getClient().player != null) {
                Flint.getUser().sendMessage(new InfoMessage("flint.update",
                        Component.text("v" + MOD_VERSION),
                        Component.text("v" + latestVersion),
                        Component.translatable("flint.update.link", PaletteColor.SKY_LIGHT_2)
                                .clickEvent(ClickEvent.openUrl(MODRINTH_URL))
                                .hoverEvent(HoverEvent.showText(Component.text(MODRINTH_URL, PaletteColor.GRAY_LIGHT)))
                ));
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
