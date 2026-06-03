package dev.dfonline.flint.hypercube;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a visible badge on a DiamondFire player profile.
 *
 * @param text        The visible badge text component or symbol.
 * @param name        The badge name component from the hover text.
 * @param description The extra hover text component below the badge name, if present.
 */
public record PlayerBadge(Text text, Text name, @Nullable Text description) {

    public PlayerBadge {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(name, "name");
    }

    public Text getText() {
        return this.text;
    }

    public Text getName() {
        return this.name;
    }

    public @Nullable Text getDescription() {
        return this.description;
    }

    public String toReadableString() {
        if (this.description == null || this.description.getString().isBlank()) {
            return this.text.getString() + " " + this.name.getString();
        }

        return this.text.getString() + " " + this.name.getString() + " - " + this.description.getString();
    }

}
