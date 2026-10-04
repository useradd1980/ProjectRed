package mrtjp.projectred.expansion;

import mrtjp.projectred.api.*;
import codechicken.multipart.block.TileMultipart;
import mrtjp.projectred.api.pneumatics.PneumaticRoutePolicy;
import mrtjp.projectred.api.pneumatics.PneumaticTubeConnectionPolicy;
import mrtjp.projectred.api.pneumatics.PneumaticTube;
import mrtjp.projectred.expansion.client.MovementClientRegistry;
import mrtjp.projectred.expansion.part.PneumaticTubePart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class ExpansionAPI implements IExpansionAPI {

    public static final IExpansionAPI INSTANCE = new ExpansionAPI();

    private ExpansionAPI() { }

    @Override
    public void registerPneumaticRoutePolicy(PneumaticRoutePolicy policy) {
        PneumaticRouteRegistry.register(policy);
    }

    @Override
    public void registerPneumaticTubeConnectionPolicy(PneumaticTubeConnectionPolicy policy) {
        PneumaticTubeConnectionRegistry.register(policy);
    }

    @Override
    public void registerPneumaticLowLoadPowerPredicate(Predicate<PneumaticTube> predicate) {
        PneumaticTubePowerRegistry.register(predicate);
    }

    @Override
    public PneumaticTube getPneumaticTube(Level level, BlockPos pos) {
        var tile = level.getBlockEntity(pos);
        if (!(tile instanceof TileMultipart multipart)) return null;

        var part = multipart.getSlottedPart(6);
        return part instanceof PneumaticTubePart tube ? tube : null;
    }

    @Override
    public void registerBlockMover(Block block, BlockMover mover) {
        MovementRegistry.registerBlockMover(block, mover);
    }

    @Override
    public void registerFrameInteraction(FrameInteraction interaction) {
        MovementRegistry.registerFrameInteraction(interaction);
    }

    @Override
    public void registerBlockEntityRenderCallback(MovingBlockEntityRenderCallback callback) {
        MovementClientRegistry.registerBlockEntityRendererCallback(callback);
    }

    @Override
    public MovementDescriptor beginMove(Level level, int dir, double speed, Set<BlockPos> blocks) {
        return MovementManager.getInstance(level).beginMove(level, blocks, dir, speed);
    }

    @Override
    public Set<BlockPos> getStructure(Level level, BlockPos pos, BlockPos... exclusions) {
        FrameStickResolver resolver = new FrameStickResolver(level, pos, new HashSet<>(Set.of(exclusions)));
        return resolver.resolve();
    }

    @Override
    public boolean isMoving(Level level, BlockPos pos) {
        return MovementManager.getInstance(level).getMovementInfo(pos).isMoving();
    }
}
