package dev.dfonline.flint.hypercube;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a visible badge on a DiamondFire player profile.
 *
 * @param text        The visible badge text or symbol.
 * @param name        The badge name from the hover text.
 * @param description The extra hover text below the badge name, if present.
 */
public record PlayerBadge(String text, String name, @Nullable String description) {

    public PlayerBadge {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(name, "name");
    }

    public String getText() {
        return this.text;
    }

    public String getName() {
        return this.name;
    }

    @SuppressWarnings("unused")
    public @Nullable String getDescription() {
        return this.description;
    }

    public String toReadableString() {
        if (this.description == null || this.description.isBlank()) {
            return this.text + " " + this.name;
        }

        return this.text + " " + this.name + " - " + this.description;
    }

}
