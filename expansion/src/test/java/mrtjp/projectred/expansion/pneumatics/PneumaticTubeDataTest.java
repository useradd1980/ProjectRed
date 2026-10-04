package mrtjp.projectred.expansion.pneumatics;

import mrtjp.projectred.api.pneumatics.PneumaticTubeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PneumaticTubeDataTest {

    private static final ResourceLocation COLOUR =
            ResourceLocation.fromNamespaceAndPath("example_addon", "route_colour");

    @Test
    void dataRoundTripsAndCopiesMutableTags() {
        var data = new PneumaticTubeData();
        var value = new CompoundTag();
        value.putInt("colour", 14);
        data.put(COLOUR, value);

        value.putInt("colour", 1);
        assertEquals(14, data.get(COLOUR).getInt("colour"));

        var loaded = new PneumaticTubeData();
        loaded.load(data.save());
        assertEquals(14, loaded.get(COLOUR).getInt("colour"));
    }

    @Test
    void removalAndMissingDataArePredictable() {
        var data = new PneumaticTubeData();
        assertFalse(data.has(COLOUR));
        assertNull(data.get(COLOUR));

        data.put(COLOUR, new CompoundTag());
        assertTrue(data.has(COLOUR));

        data.remove(COLOUR);
        assertFalse(data.has(COLOUR));
    }
}