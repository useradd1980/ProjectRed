package mrtjp.projectred.expansion.part;

import mrtjp.projectred.expansion.TubeType;
import mrtjp.projectred.expansion.pneumatics.PneumaticTransport;
import mrtjp.projectred.expansion.pneumatics.PneumaticTransportContainer;
import mrtjp.projectred.expansion.pneumatics.PneumaticTransportMode;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class VoidPneumaticTubePartTest {

    @Test
    void acceptsConnectedInputsWithoutDownstreamInventory() {
        VoidPneumaticTubePart tube = new VoidPneumaticTubePart(TubeType.VOID_PNEUMATIC_TUBE);
        int north = Direction.NORTH.ordinal();
        tube.setConnMap(1 << north);
        PneumaticTubePayload payload = new PneumaticTubePayload();

        assertTrue(tube.requiresActiveNode());
        assertTrue(tube.canItemEnterTube(payload, north));
        assertTrue(tube.canItemExitTube(payload, north, PneumaticTransportMode.PASSIVE_NORMAL));
        assertFalse(tube.canItemEnterTube(payload, Direction.SOUTH.ordinal()));
        assertFalse(tube.canItemExitTube(payload, Direction.SOUTH.ordinal(), PneumaticTransportMode.PASSIVE_NORMAL));
    }

    @Test
    void removesArrivedPayloadsButPreservesOtherStacks() {
        PneumaticTransport transport = new PneumaticTransport(mock(PneumaticTransportContainer.class));
        PneumaticTubePayload travelling = new PneumaticTubePayload();
        travelling.setProgress(PneumaticTubePayload.MAX_PROGRESS / 2 - 1);
        PneumaticTubePayload arrived = new PneumaticTubePayload();
        arrived.setProgress(PneumaticTubePayload.MAX_PROGRESS / 2);
        int travellingId = transport.addPayload(travelling);
        int arrivedId = transport.addPayload(arrived);

        var removed = VoidPneumaticTubePart.discardArrivedPayloads(transport);

        assertEquals(1, removed.size());
        assertTrue(removed.contains(arrivedId));
        assertSame(travelling, transport.getPayload(travellingId));
        assertNull(transport.getPayload(arrivedId));
    }

    @Test
    void neverAssignsAnOutgoingDirection() {
        VoidPneumaticTubePart tube = new VoidPneumaticTubePart(TubeType.VOID_PNEUMATIC_TUBE);
        PneumaticTubePayload payload = new PneumaticTubePayload();
        payload.setInputSide(Direction.NORTH.ordinal());

        tube.setOutputDirection(payload);

        assertFalse(payload.hasOutputSide());
    }
}
