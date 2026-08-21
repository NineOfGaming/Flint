package dev.dfonline.flint.hypercube;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A player's current DiamondFire location as reported by /locate.
 *
 * @param player The located player's username.
 * @param mode   The player's current mode.
 * @param plot   The plot the player is on, if available.
 * @param node   The node the player is on, if known.
 * @param privateNodeId The specific private node ID, or {@code null} when the
 *                      player is not on a numbered private node.
 */
public record PlayerLocation(
        String player,
        Mode mode,
        @Nullable Plot plot,
        @Nullable Node node,
        @Nullable String privateNodeId
) {

    public PlayerLocation {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(mode, "mode");

        if (node != Node.PRIVATE) {
            privateNodeId = null;
        }
    }

    public PlayerLocation(String player, Mode mode, @Nullable Plot plot, @Nullable Node node) {
        this(player, mode, plot, node, null);
    }

    public String getPlayer() {
        return this.player;
    }

    public Mode getMode() {
        return this.mode;
    }

    public @Nullable Plot getPlot() {
        return this.plot;
    }

    public @Nullable Node getNode() {
        return this.node;
    }

    public @Nullable String getPrivateNodeId() {
        return this.privateNodeId;
    }

}
