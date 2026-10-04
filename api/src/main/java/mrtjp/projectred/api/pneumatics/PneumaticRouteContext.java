package mrtjp.projectred.api.pneumatics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Stable public description of one transition in the pneumatic topology.
 * Internal ProjectRed graph objects are intentionally not exposed.
 */
public record PneumaticRouteContext(
        Level level,
        BlockPos fromPos,
        BlockPos toPos,
        Direction direction,
        int baseCost
) {
}