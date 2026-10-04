package mrtjp.projectred.expansion.pneumatics;

import mrtjp.projectred.api.pneumatics.PneumaticPayloadData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PneumaticPayloadDataTest {

    private static final ResourceLocation FIRST = ResourceLocation.fromNamespaceAndPath("first_mod", "request");
    private static final ResourceLocation SECOND = ResourceLocation.fromNamespaceAndPath("second_mod", "request");

    @Test
    void keysFromDifferentModsDoNotCollide() {
        PneumaticPayloadData data = new PneumaticPayloadData();
        CompoundTag first = new CompoundTag();
        first.putInt("count", 16);
        CompoundTag second = new CompoundTag();
        second.putInt("count", 64);

        data.put(FIRST, first);
        data.put(SECOND, second);

        assertEquals(16, data.get(FIRST).getInt("count"));
        assertEquals(64, data.get(SECOND).getInt("count"));
        data.remove(FIRST);
        assertFalse(data.has(FIRST));
        assertTrue(data.has(SECOND));
    }

    @Test
    void inputAndOutputTagsCannotMutateStoredData() {
        PneumaticPayloadData data = new PneumaticPayloadData();
        CompoundTag original = new CompoundTag();
        original.putInt("count", 7);
        data.put(FIRST, original);

        original.putInt("count", 99);
        assertEquals(7, data.get(FIRST).getInt("count"));

        CompoundTag received = data.get(FIRST);
        received.putInt("count", 23);
        assertEquals(7, data.get(FIRST).getInt("count"));

        CompoundTag snapshot = data.save();
        snapshot.getCompound(FIRST.toString()).putInt("count", 100);
        assertEquals(7, data.get(FIRST).getInt("count"));
    }

    @Test
    void metadataRoundTripsThroughNbtAndOldEmptyPayloadsClearPreviousData() {
        PneumaticPayloadData original = new PneumaticPayloadData();
        CompoundTag request = new CompoundTag();
        request.putString("target", "warehouse");
        original.put(FIRST, request);

        PneumaticPayloadData loaded = new PneumaticPayloadData();
        loaded.load(original.save());
        assertEquals("warehouse", loaded.get(FIRST).getString("target"));

        loaded.load(new CompoundTag()); // Legacy ProjectRed payload with no metadata
        assertTrue(loaded.isEmpty());
        assertNull(loaded.get(FIRST));
    }

    @Test
    void missingKeysAndNullValuesHavePredictableBehaviour() {
        PneumaticPayloadData data = new PneumaticPayloadData();
        assertFalse(data.has(FIRST));
        assertNull(data.get(FIRST));
        assertThrows(NullPointerException.class, () -> data.put(FIRST, null));
    }
}