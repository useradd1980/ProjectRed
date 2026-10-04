package mrtjp.projectred.api.pneumatics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Public view of an item travelling through ProjectRed's pneumatic network.
 * Custom data belongs to the payload, not its contained ItemStack.
 */
public interface PneumaticPayload {

    ItemStack getItemStack();

    /** Whether this payload carries a compound for the specified namespaced key. */
    boolean hasData(ResourceLocation key);

    /** Returns a defensive copy, or null when absent. */
    @Nullable CompoundTag getData(ResourceLocation key);

    /** Stores a defensive copy. Does not mutate the ItemStack. */
    void setData(ResourceLocation key, CompoundTag value);

    void removeData(ResourceLocation key);
}