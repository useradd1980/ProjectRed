package mrtjp.projectred.api.pneumatics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Namespaced metadata attached to a physical pneumatic tube part.
 */
public final class PneumaticTubeData {

    private CompoundTag data = new CompoundTag();

    public boolean has(ResourceLocation key) {
        return data.contains(
                Objects.requireNonNull(key, "key").toString(),
                Tag.TAG_COMPOUND);
    }

    public @Nullable CompoundTag get(ResourceLocation key) {
        return has(key) ? data.getCompound(key.toString()).copy() : null;
    }

    public void put(ResourceLocation key, CompoundTag value) {
        data.put(
                Objects.requireNonNull(key, "key").toString(),
                Objects.requireNonNull(value, "value").copy());
    }

    public void remove(ResourceLocation key) {
        data.remove(Objects.requireNonNull(key, "key").toString());
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public CompoundTag save() {
        return data.copy();
    }

    public void load(CompoundTag saved) {
        data = Objects.requireNonNull(saved, "saved").copy();
    }
}