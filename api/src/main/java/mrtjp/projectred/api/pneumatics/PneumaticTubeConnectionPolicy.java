package mrtjp.projectred.api.pneumatics;

import net.minecraft.core.Direction;

/**
 * Extension point for physical pneumatic tube compatibility.
 *
 * This is intentionally payload-independent: it controls whether two adjacent
 * tubes are part of the same physical topology at all. Implementations should
 * be deterministic from tube/world state and should normally be symmetric.
 */
@FunctionalInterface
public interface PneumaticTubeConnectionPolicy {

    /**
     * @param from tube whose connection mask is being rebuilt
     * @param to adjacent pneumatic tube
     * @param direction direction from {@code from} toward {@code to}
     * @return true when the two tubes may physically connect
     */
    boolean canConnect(
            PneumaticTube from,
            PneumaticTube to,
            Direction direction);
}