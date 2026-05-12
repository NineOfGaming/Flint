package dev.dfonline.flint.hypercube;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Plot {

    private final int id;
    private final Text name;
    private final String handle;
    private final boolean whitelisted;
    private Vec3i devOrigin;
    private PlotSize size;
    private boolean hasUnderground = false;
    private final String owner;
    private @Nullable PlayerProfile ownerProfile;

    public Plot(int id, Text name, String handle, boolean whitelisted, String owner) {
        this.id = id;
        this.name = name;
        this.handle = handle;
        this.whitelisted = whitelisted;
        this.owner = owner;
    }

    public int getId() {
        return this.id;
    }

    public Text getName() {
        return this.name;
    }

    public String getHandle() {
        return this.handle;
    }

    public boolean isWhitelisted() {
        return this.whitelisted;
    }

    public Vec3i getDevOrigin() {
        return this.devOrigin;
    }

    public void setDevOrigin(Vec3i origin) {
        this.devOrigin = origin;
    }

    public PlotSize getSize() {
        // since chunks are "streamed" to the client, it's hard to tell what the plot size is immediately
        // however basic, large, and mega checks should go by pretty quickly within normal render distance
        // so if we don't know the plots size it's likely it's a massive
        return Objects.requireNonNullElse(this.size, PlotSize.MASSIVE);
    }

    public @Nullable PlotSize getDetectedSize() {
        return this.size;
    }

    public boolean isSizeKnown() {
        return this.size != null;
    }

    public void setSize(PlotSize size) {
        this.size = size;
    }

    public boolean hasUnderground() {
        return this.hasUnderground;
    }

    public void setHasUnderground(boolean hasUnderground) {
        this.hasUnderground = hasUnderground;
    }

    public String getOwner() {
        return this.owner;
    }

    @SuppressWarnings("unused")
    public String getOwnerName() {
        return this.owner;
    }

    public @Nullable PlayerProfile getOwnerProfile() {
        return this.ownerProfile;
    }

    public void setOwnerProfile(@Nullable PlayerProfile ownerProfile) {
        this.ownerProfile = ownerProfile;
    }

    public PlayerRanks getOwnerRanks() {
        if (this.ownerProfile == null) {
            return PlayerRanks.EMPTY;
        }

        return this.ownerProfile.ranks();
    }

    public boolean isPosInCodeSpace(BlockPos pos) {
        if (this.devOrigin == null) {
            return false;
        }

        PlotSize size = this.getSize();
        int x = pos.getX();
        int z = pos.getZ();

        return x <= this.devOrigin.getX()
                && x >= this.devOrigin.getX() - size.getCodeWidth() + 1
                && z >= this.devOrigin.getZ()
                && z <= this.devOrigin.getZ() + size.getCodeLength();
    }

    public @Nullable String getCodeBoundsString() {
        if (this.devOrigin == null) {
            return null;
        }

        PlotSize size = this.getSize();
        int minX = this.devOrigin.getX() - size.getCodeWidth() + 1;
        int maxX = this.devOrigin.getX();
        int minZ = this.devOrigin.getZ();
        int maxZ = this.devOrigin.getZ() + size.getCodeLength();

        return "(" + minX + ", " + minZ + ") -> (" + maxX + ", " + maxZ + ")";
    }

    public String toReadableString() {
        return "ID " + this.id + ", name " + this.name.getString() + ", handle " + this.handle + ", whitelisted " + this.whitelisted + ", origin " + this.devOrigin + ", owner " + this.owner + ", owner ranks " + this.getOwnerRanks().toReadableString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Plot plot) {
            if (this.handle == null && plot.getHandle() != null || plot.getHandle() == null && this.handle != null) {
                return false;
            }

            return this.id == plot.getId() &&
                    this.name.equals(plot.getName()) &&
                    (this.handle == null && plot.getHandle() == null || this.handle != null && this.handle.equals(plot.getHandle())) &&
                    this.whitelisted == plot.isWhitelisted() &&
                    this.owner.equals(plot.getOwner());
        }
        return false;
    }

    @Override
    public int hashCode() {
        // Origin might be null, but it's still the same plot, so we don't include it in the hash code.
        return Objects.hash(this.id, this.name, this.handle, this.whitelisted, this.owner);
    }

}
