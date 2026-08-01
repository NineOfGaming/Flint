package dev.dfonline.flint.hypercube;

public final class ServerPatches {

    private static ServerPatchSet current = ServerPatchSet.UNKNOWN;

    private ServerPatches() {
    }

    public static ServerPatchSet current() {
        return current;
    }

    public static void set(ServerPatchTarget target, String patch) {
        current = current.with(target, patch);
    }

    public static void setUnknown(String patch) {
        current = current.withUnknown(patch);
    }

    public static void clear() {
        current = ServerPatchSet.UNKNOWN;
    }

}
