package dev.dfonline.flint.hypercube;

import org.jetbrains.annotations.Nullable;

public enum ServerPatchTarget {
    MAIN,
    BETA;

    public static @Nullable ServerPatchTarget fromNode(@Nullable Node node) {
        if (node == null) {
            return null;
        }

        return node == Node.BETA ? BETA : MAIN;
    }
}
