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
 */
public record PlayerLocation(String player, Mode mode, @Nullable Plot plot, @Nullable Node node) {

    public PlayerLocation {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(mode, "mode");
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

}
