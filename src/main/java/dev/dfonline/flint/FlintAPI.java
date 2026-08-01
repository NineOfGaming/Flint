package dev.dfonline.flint;

import dev.dfonline.flint.feature.core.FeatureTrait;
import dev.dfonline.flint.feature.impl.CommandSenderFeature;
import dev.dfonline.flint.feature.impl.LocateFeature;
import dev.dfonline.flint.feature.impl.WhoisFeature;
import dev.dfonline.flint.hypercube.PlayerLocation;
import dev.dfonline.flint.hypercube.PlayerProfile;
import dev.dfonline.flint.hypercube.ServerPatch;
import dev.dfonline.flint.hypercube.ServerPatchSet;
import dev.dfonline.flint.hypercube.ServerPatches;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused") // API, we don't use all methods
public final class FlintAPI {

    private static boolean confirmLocationWithLocateRequired = false;
    private static boolean userConfirmLocationWithLocate = false;
    private static boolean fetchUserProfileWithWhoisRequired = false;
    private static boolean userFetchUserProfileWithWhois = false;
    private static boolean fetchPlotOwnerProfileWithWhoisRequired = false;
    private static boolean userFetchPlotOwnerProfileWithWhois = false;
    private static boolean debugging = false;

    private FlintAPI() {
    }

    /**
     * Lets Flint know that it should run /locate after a suspected mode change.
     *
     * <p>
     * This is useful when you want to be 100% sure about the player's mode, such as when done by outside forces.
     * </p>
     *
     * <p>
     * Enabling this also gives access to the player's plot, something that is not available in the default mode change detection.
     * </p>
     *
     * @implNote This is intended to be called from your mod's initialization method.
     */
    public static void confirmLocationWithLocate() {
        confirmLocationWithLocateRequired = true;
    }

    /**
     * Lets Flint know that it should run /whois to get the current user's profile data.
     *
     * <p>
     * This makes the user's ranks available through {@link User#getProfile()} and {@link User#getRanks()}.
     * </p>
     *
     * @implNote This is intended to be called from your mod's initialization method.
     */
    public static void fetchUserProfileWithWhois() {
        fetchUserProfileWithWhoisRequired = true;
    }

    /**
     * Lets Flint know that it should run /whois to keep the current plot owner's profile data updated.
     *
     * <p>
     * Plot owner profile data is available through {@link dev.dfonline.flint.hypercube.Plot#getOwnerProfile()}
     * and {@link dev.dfonline.flint.hypercube.Plot#getOwnerRanks()}.
     * </p>
     *
     * @implNote This is intended to be called from your mod's initialization method.
     */
    public static void fetchPlotOwnerProfileWithWhois() {
        fetchPlotOwnerProfileWithWhoisRequired = true;
    }

    /**
     * Requests the current user's location with /locate.
     *
     * <p>
     * The returned future completes on the client thread with the parsed location response,
     * or exceptionally if the request times out.
     * </p>
     *
     * @return A future for the current user's location.
     */
    public static CompletableFuture<PlayerLocation> requestPlayerLocation() {
        return requestPlayerLocation(null);
    }

    /**
     * Requests a player's current location with /locate.
     *
     * <p>
     * The returned future completes on the client thread with the parsed location response,
     * or exceptionally if the request times out. A null or blank player name falls back to the current client user.
     * </p>
     *
     * @param playerName The player to locate, or null/blank for the current client user.
     * @return A future for the player's location.
     */
    public static CompletableFuture<PlayerLocation> requestPlayerLocation(String playerName) {
        String resolvedPlayerName = resolvePlayerName(playerName);
        if (resolvedPlayerName == null) {
            return noClientPlayerFuture();
        }

        return completeOnClientThread(LocateFeature.requestLocate(resolvedPlayerName));
    }

    /**
     * Requests the current user's profile with /whois.
     *
     * <p>
     * The returned future completes on the client thread with the parsed profile response,
     * or exceptionally if the request times out. Recent profile results may be served from Flint's whois cache.
     * </p>
     *
     * @return A future for the current user's profile.
     */
    public static CompletableFuture<PlayerProfile> requestPlayerProfile() {
        return requestPlayerProfile(null);
    }

    /**
     * Requests a player's profile with /whois.
     *
     * <p>
     * The returned future completes on the client thread with the parsed profile response,
     * or exceptionally if the request times out. Recent profile results may be served from Flint's whois cache.
     * A null or blank player name falls back to the current client user.
     * </p>
     *
     * @param playerName The player to get profile data for, or null/blank for the current client user.
     * @return A future for the player's profile.
     */
    public static CompletableFuture<PlayerProfile> requestPlayerProfile(String playerName) {
        String resolvedPlayerName = resolvePlayerName(playerName);
        if (resolvedPlayerName == null) {
            return noClientPlayerFuture();
        }

        return completeOnClientThread(WhoisFeature.requestWhois(resolvedPlayerName));
    }

    /**
     * Clears Flint's cached /whois profile results.
     *
     * <p>
     * The next profile request for any player will run /whois instead of using a cached profile.
     * </p>
     */
    public static void clearPlayerProfileCache() {
        WhoisFeature.clearCache();
    }

    private static String resolvePlayerName(String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            return playerName.trim();
        }

        if (Flint.getClient().player == null) {
            return null;
        }

        return Flint.getClient().player.getGameProfile().name();
    }

    private static <T> CompletableFuture<T> noClientPlayerFuture() {
        return CompletableFuture.failedFuture(new IllegalStateException("No client player is available"));
    }

    private static <T> CompletableFuture<T> completeOnClientThread(CompletableFuture<T> future) {
        CompletableFuture<T> clientThreadFuture = new CompletableFuture<>();
        future.whenComplete((result, throwable) -> Flint.getClient().execute(() -> {
            if (throwable != null) {
                clientThreadFuture.completeExceptionally(throwable);
                return;
            }

            clientThreadFuture.complete(result);
        }));
        return clientThreadFuture;
    }

    /**
     * Sets whether the user has enabled /locate confirmation.
     *
     * @param userConfirmLocationWithLocate Whether the user has enabled /locate confirmation.
     */
    @ApiStatus.Internal
    public static void setUserConfirmLocationWithLocate(boolean userConfirmLocationWithLocate) {
        FlintAPI.userConfirmLocationWithLocate = userConfirmLocationWithLocate;
    }

    /**
     * @return Whether the user has enabled /locate confirmation.
     */
    @ApiStatus.Internal
    public static boolean isUserConfirmLocationWithLocate() {
        return userConfirmLocationWithLocate;
    }

    /**
     * @return Whether a mod has requested /locate confirmation.
     */
    @ApiStatus.Internal
    public static boolean isConfirmLocationWithLocateRequired() {
        return confirmLocationWithLocateRequired;
    }

    /**
     * Sets whether the user has enabled /whois fetching for their own profile.
     *
     * @param userFetchUserProfileWithWhois Whether the user has enabled /whois fetching for their own profile.
     */
    @ApiStatus.Internal
    public static void setUserFetchUserProfileWithWhois(boolean userFetchUserProfileWithWhois) {
        FlintAPI.userFetchUserProfileWithWhois = userFetchUserProfileWithWhois;
    }

    /**
     * @return Whether the user has enabled /whois fetching for their own profile.
     */
    @ApiStatus.Internal
    public static boolean isUserFetchUserProfileWithWhois() {
        return userFetchUserProfileWithWhois;
    }

    /**
     * @return Whether a mod has requested /whois fetching for the current user's profile.
     */
    @ApiStatus.Internal
    public static boolean isFetchUserProfileWithWhoisRequired() {
        return fetchUserProfileWithWhoisRequired;
    }

    /**
     * Sets whether the user has enabled /whois fetching for the current plot owner's profile.
     *
     * @param userFetchPlotOwnerProfileWithWhois Whether the user has enabled /whois fetching for the current plot owner's profile.
     */
    @ApiStatus.Internal
    public static void setUserFetchPlotOwnerProfileWithWhois(boolean userFetchPlotOwnerProfileWithWhois) {
        FlintAPI.userFetchPlotOwnerProfileWithWhois = userFetchPlotOwnerProfileWithWhois;
    }

    /**
     * @return Whether the user has enabled /whois fetching for the current plot owner's profile.
     */
    @ApiStatus.Internal
    public static boolean isUserFetchPlotOwnerProfileWithWhois() {
        return userFetchPlotOwnerProfileWithWhois;
    }

    /**
     * @return Whether a mod has requested /whois fetching for the current plot owner's profile.
     */
    @ApiStatus.Internal
    public static boolean isFetchPlotOwnerProfileWithWhoisRequired() {
        return fetchPlotOwnerProfileWithWhoisRequired;
    }

    /**
     * @return Whether Flint should confirm the player's location with /locate after a suspected mode change.
     */
    public static boolean shouldConfirmLocationWithLocate() {
        return confirmLocationWithLocateRequired || userConfirmLocationWithLocate || shouldFetchPlotOwnerProfileWithWhois();
    }

    /**
     * @return Whether Flint should fetch the current user's profile with /whois.
     */
    public static boolean shouldFetchUserProfileWithWhois() {
        return fetchUserProfileWithWhoisRequired || userFetchUserProfileWithWhois;
    }

    /**
     * @return Whether Flint should fetch the current plot owner's profile with /whois.
     */
    public static boolean shouldFetchPlotOwnerProfileWithWhois() {
        return fetchPlotOwnerProfileWithWhoisRequired || userFetchPlotOwnerProfileWithWhois;
    }

    /**
     * Set whether Flint should print debug messages.
     *
     * @param debugging Whether Flint should print debug messages.
     */
    public static void setDebugging(boolean debugging) {
        FlintAPI.debugging = debugging;
    }

    /**
     * @return Whether Flint should print debug messages.
     */
    public static boolean isDebugging() {
        return debugging;
    }

    /**
     * @return The known server patches, split by server target.
     */
    public static ServerPatchSet getServerPatches() {
        return ServerPatches.current();
    }

    /**
     * @return The known patch for the node the client is currently on, or {@code null} if it is not known yet.
     */
    public static @Nullable ServerPatch getServerPatch() {
        return getServerPatches().patchForNode(Flint.getUser().getNode());
    }

    /**
     * Register a feature with Flint, call this on your mod's initialization method.
     *
     * @param feature The feature to register.
     */
    public static void registerFeature(FeatureTrait feature) {
        Flint.FEATURE_MANAGER.register(feature);
    }

    /**
     * Register multiple features with Flint, call this on your mod's initialization method.
     *
     * @param features The features to register.
     */
    public static void registerFeatures(FeatureTrait... features) {
        Flint.FEATURE_MANAGER.registerAll(features);
    }

    /**
     * Queues a command to be sent without triggering chat spam limits.
     *
     * @param command The command to send, with or without a leading slash.
     */
    public static void queueCommand(String command) {
        CommandSenderFeature.queueCommand(command);
    }

}
