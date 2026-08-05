package dev.dfonline.flint.hypercube;

import org.jetbrains.annotations.Nullable;

public final class ServerPatches {

    private static @Nullable ServerPatch current;

    private ServerPatches() {
    }

    public static @Nullable ServerPatch current() {
        return current;
    }

    public static @Nullable ServerPatch currentForNode(@Nullable Node node) {
        return current == null ? null : current.forNode(node);
    }

    public static void set(String patch) {
        current = ServerPatch.parse(patch);
    }

    public static void clear() {
        current = null;
    }

}
