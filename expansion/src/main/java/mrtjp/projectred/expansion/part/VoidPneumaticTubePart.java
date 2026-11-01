package mrtjp.projectred.expansion.part;

import mrtjp.projectred.expansion.TubeType;
import mrtjp.projectred.expansion.pneumatics.PneumaticTransport;
import mrtjp.projectred.expansion.pneumatics.PneumaticTransportMode;

import java.util.Collection;
import java.util.List;

import static mrtjp.projectred.expansion.part.PneumaticTubePayload.MAX_PROGRESS;

/**
 * Terminal pneumatic tube that destroys incoming items at its centre.
 * Exposes a valid route endpoint without providing any outgoing routes.
 */
public class VoidPneumaticTubePart extends PneumaticTubePart {

    public VoidPneumaticTubePart(TubeType type) {
        super(type);
    }

    @Override
    public boolean requiresActiveNode() {
        // Stop graph links at this endpoint even with multiple connected inputs.
        return true;
    }

    @Override
    public boolean canItemExitTube(PneumaticTubePayload payload, int side, PneumaticTransportMode mode) {
        // The route finder recognises this tube as an available destination.
        return maskConnects(side);
    }

    @Override
    public boolean canItemEnterTube(PneumaticTubePayload payload, int side) {
        // No downstream inventory is required, but entry policies still apply.
        return maskConnects(side) && isEntryAllowedByPolicies(payload, side);
    }

    @Override
    public void setOutputDirection(PneumaticTubePayload payload) {
        // No outgoing side: the item will be destroyed at the midpoint.
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        Collection<Integer> removed = discardArrivedPayloads(getPneumaticTransport());
        if (!removed.isEmpty()) {
            sendPayloadRemoved(removed);
            tile().setChanged();
        }
    }

    static Collection<Integer> discardArrivedPayloads(PneumaticTransport transport) {
        return transport.removePayloadsConditionally(
                payload -> payload.getProgress() >= MAX_PROGRESS / 2);
    }

    @Override
    public boolean onPayloadReachedOutput(int id, PneumaticTubePayload payload) {
        // Defensive fallback for a malformed or extremely fast payload.
        if (level().isClientSide) return false;
        sendPayloadRemoved(List.of(id));
        tile().setChanged();
        return true;
    }
}
