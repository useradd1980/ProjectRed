package mrtjp.projectred.api.pneumatics;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Public view of a ProjectRed pneumatic tube.
 *
 * Namespaced custom data belongs to the tube part itself and is persisted and
 * synchronized by ProjectRed. Mutations should be performed on the logical
 * server.
 */
public interface PneumaticTube {

    Level getLevel();

    BlockPos getPos();

    boolean hasData(ResourceLocation key);

    /** Returns a defensive copy, or null when absent. */
    @Nullable
    CompoundTag getData(ResourceLocation key);

    /** Stores a defensive copy and invalidates routing topology as needed. */
    void setData(ResourceLocation key, CompoundTag value);

    /** Removes data and invalidates routing topology as needed. */
    void removeData(ResourceLocation key);
}