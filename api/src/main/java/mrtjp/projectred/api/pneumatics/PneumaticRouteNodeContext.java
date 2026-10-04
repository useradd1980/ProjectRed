package mrtjp.projectred.api.pneumatics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Context used to decide whether a physical tube location is significant to
 * payload routing and therefore must remain represented as a graph node.
 */
public record PneumaticRouteNodeContext(Level level, BlockPos pos) {
}