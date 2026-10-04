package mrtjp.projectred.api;

import mrtjp.projectred.api.pneumatics.PneumaticRoutePolicy;
import mrtjp.projectred.api.pneumatics.PneumaticTube;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.BlockCapability;

import java.util.Set;
import javax.annotation.Nullable;

public interface IExpansionAPI {

    /**
     * The capability instance for {@link Frame}
     */
    BlockCapability<Frame, Void> FRAME_CAPABILITY = BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath(ProjectRedAPI.EXPANSION_MOD_ID, "frame"), Frame.class);

    /**
     * The capability instance for {@link MovementController}
     */
    BlockCapability<MovementController, Void> MOVEMENT_CONTROLLER_CAPABILITY = BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath(ProjectRedAPI.EXPANSION_MOD_ID, "movement_controller"), MovementController.class);

    /**
     * Used to register a {@link BlockMover} to a specific block. This BlockMover will be engaged every time
     * the given block is moved.
     *
     * @param block The block to bind the mover to
     * @param mover BlockMover instance to register.
     */
    void registerBlockMover(Block block, BlockMover mover);

    /**
     * Used to register a {@link FrameInteraction}, which is a class that
     * can be used to add frame-like properties to any block.
     *
     * @param interaction The interaction to register.
     */
    void registerFrameInteraction(FrameInteraction interaction);

    /**
     * Registers a policy that may influence pneumatic payload routing.
     * Policies are evaluated only for pneumatic routing and must not mutate
     * the topology graph from inside routing callbacks.
     *
     * @param policy Policy instance to register
     */
    void registerPneumaticRoutePolicy(PneumaticRoutePolicy policy);

    /**
     * Returns the pneumatic tube occupying the center multipart slot at the
     * supplied position, or null when no pneumatic tube is present.
     */
    @Nullable
    PneumaticTube getPneumaticTube(Level level, BlockPos pos);

    /**
     * Registers a {@link MovingBlockEntityRenderCallback} object to receive callbacks for block entities
     * that render while moving
     * @param callback The callback to register
     */
    void registerBlockEntityRenderCallback(MovingBlockEntityRenderCallback callback);

    /**
     * Attempts to execute a block movement defined by the given parameters and returns an object
     * containing the status of the movement attempt and, if started successfully, methods to track
     * its progress.
     *
     * @param level  Level
     * @param dir    Direction of movement
     * @param speed  Speed of movement, in blocks per tick (1.0 would immediately move the blocks)
     * @param blocks Set of block positions to move
     * @return An {@link MovementDescriptor} object that can be used to track the movement. Discard once
     * movement is complete.
     */
    MovementDescriptor beginMove(Level level, int dir, double speed, Set<BlockPos> blocks);

    /**
     * Used to resolve a single connected framed structure from any single block in the structure. Typically,
     * a block capable of executing a move would use this to obtain a set of blocks, then pass it into
     * {@link #beginMove(Level, int, double, Set)}.
     *
     * @param level      Level
     * @param pos        The position of any block in the structure.
     * @param exclusions All coordinates to not include in the structure.
     *                   generally, one of these is the motor block that moved the structure.
     * @return A set of all block coordinates that are part of the structure that the input block was in.
     */
    Set<BlockPos> getStructure(Level level, BlockPos pos, BlockPos... exclusions);

    /**
     * Used to check if the given block is currently moving. This method is
     * client and server safe.
     *
     * @param level Level
     * @param pos   Position of block to query
     * @return True if the block is currently moving
     */
    boolean isMoving(Level level, BlockPos pos);
}