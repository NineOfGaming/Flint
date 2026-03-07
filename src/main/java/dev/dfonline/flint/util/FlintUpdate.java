package dev.dfonline.flint.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dfonline.flint.Flint;
import dev.dfonline.flint.util.message.impl.prefix.InfoMessage;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class FlintUpdate {

    private static final Logger LOGGER = Logger.of(FlintUpdate.class);
    private static final String MOD_REPOSITORY = "DFOnline/Flint";
    private static final String MODRINTH_URL = "https://modrinth.com/mod/flint/versions";
    private static final String MOD_VERSION = getCurrentVersion();
    private static final String UNKNOWN_VERSION = "unknown";
    private static @Nullable String latestVersion = null;

    private FlintUpdate() {
    }

    public static void fetchLatestRelease() {
        String url = String.format("https://api.github.com/repos/%s/releases/latest", MOD_REPOSITORY);

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(responseBody -> {
                        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        if (!jsonResponse.has("tag_name")) {
                            throw new RuntimeException("Expected a response with tag_name, instead got: " + responseBody);
                        }
                        latestVersion = jsonResponse.get("tag_name").getAsString().substring(1);
                        LOGGER.info("Latest version: v{}", latestVersion);
                    })
                    .exceptionally(e -> {
                        LOGGER.error("Error while fetching version", e);
                        return null;
                    });
        }
    }

    private static String getCurrentVersion() {
        try (InputStream input = Flint.class.getClassLoader().getResourceAsStream("flint_version.txt")) {
            if (input == null) {
                return UNKNOWN_VERSION;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                return reader.readLine();
            }
        } catch (IOException e) {
            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                return UNKNOWN_VERSION;
            }
            LOGGER.error("Failed to get mod version", e);
            return UNKNOWN_VERSION;
        }
    }


    public static void sendUpdateMessage() {
        if (latestVersion == null || MOD_VERSION.equals(latestVersion) || MOD_VERSION.equals(UNKNOWN_VERSION)) {
            return;
        }

        try {
            int latest = Integer.parseInt(latestVersion);
            int current = Integer.parseInt(MOD_VERSION);

            // Ignore if outdated for less than 5 versions.
            if (latest - current < 5) {
                return;
            }
            // We are outdated, inform the user.
            if (Flint.getClient().player != null) {
                Flint.getUser().sendMessage(new InfoMessage("flint.update",
                        Component.text("v" + MOD_VERSION),
                        Component.text("v" + latest),
                        Component.translatable("flint.update.link", PaletteColor.SKY_LIGHT_2)
                                .clickEvent(ClickEvent.openUrl(MODRINTH_URL))
                                .hoverEvent(HoverEvent.showText(Component.text(MODRINTH_URL, PaletteColor.GRAY_LIGHT)))
                ));
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
