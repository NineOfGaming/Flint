package dev.dfonline.flint.hypercube;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

public record ServerPatchSet(
        @Nullable String main,
        @Nullable String beta,
        @Nullable String unknown
) {

    public static final ServerPatchSet UNKNOWN = new ServerPatchSet(null, null, null);

    public ServerPatchSet {
        main = normalizeStoredPatch(main);
        beta = normalizeStoredPatch(beta);
        unknown = normalizeStoredPatch(unknown);
    }

    public @Nullable String get(ServerPatchTarget target) {
        return switch (target) {
            case MAIN -> this.main;
            case BETA -> this.beta;
        };
    }

    public @Nullable ServerPatch patch(ServerPatchTarget target) {
        return ServerPatch.tryParse(this.get(target));
    }

    public @Nullable String getForNode(@Nullable Node node) {
        ServerPatchTarget target = ServerPatchTarget.fromNode(node);
        if (target == null) {
            return this.unknown;
        }

        String patch = this.get(target);
        return patch == null ? this.unknown : patch;
    }

    public @Nullable ServerPatch patchForNode(@Nullable Node node) {
        return ServerPatch.tryParse(this.getForNode(node));
    }

    public ServerPatchSet with(ServerPatchTarget target, String patch) {
        String normalizedPatch = ServerPatch.parse(patch).value();
        String nextUnknown = normalizedPatch.equals(this.unknown) ? null : this.unknown;

        return switch (target) {
            case MAIN -> new ServerPatchSet(normalizedPatch, this.beta, nextUnknown);
            case BETA -> new ServerPatchSet(this.main, normalizedPatch, nextUnknown);
        };
    }

    public ServerPatchSet withUnknown(String patch) {
        String normalizedPatch = ServerPatch.parse(patch).value();

        if (normalizedPatch.equals(this.main) || normalizedPatch.equals(this.beta)) {
            return this;
        }

        return new ServerPatchSet(this.main, this.beta, normalizedPatch);
    }

    public ServerPatchSet withUnknownAssignedTo(@Nullable Node node) {
        ServerPatchTarget target = ServerPatchTarget.fromNode(node);
        if (target == null || this.unknown == null) {
            return this;
        }

        return this.with(target, this.unknown);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        addNullableString(json, "main", this.main);
        addNullableString(json, "beta", this.beta);
        return json;
    }

    private static void addNullableString(JsonObject json, String key, @Nullable String value) {
        json.add(key, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    private static @Nullable String normalizeStoredPatch(@Nullable String patch) {
        if (patch == null) {
            return null;
        }

        String normalizedPatch = patch.trim();
        return normalizedPatch.isEmpty() ? null : normalizedPatch;
    }

}
