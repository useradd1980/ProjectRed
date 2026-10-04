package mrtjp.projectred.expansion.part;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.api.part.TickablePart;
import codechicken.multipart.block.TileMultipart;
import mrtjp.projectred.api.IConnectable;
import mrtjp.projectred.api.pneumatics.PneumaticRouteContext;
import mrtjp.projectred.api.pneumatics.PneumaticRouteNodeContext;
import mrtjp.projectred.api.pneumatics.PneumaticTube;
import mrtjp.projectred.api.pneumatics.PneumaticTubeData;
import mrtjp.projectred.core.CenterLookup;
import mrtjp.projectred.core.power.ILowLoadMachine;
import mrtjp.projectred.core.power.ILowLoadPowerLine;
import mrtjp.projectred.core.power.IPowerConnectable;
import mrtjp.projectred.core.power.IPowerConductorSource;
import mrtjp.projectred.core.power.PowerConductor;
import mrtjp.projectred.expansion.PneumaticRouteRegistry;
import mrtjp.projectred.expansion.PneumaticTubeConnectionRegistry;
import mrtjp.projectred.expansion.PneumaticTubePowerRegistry;
import mrtjp.projectred.expansion.TubeType;
import mrtjp.projectred.expansion.client.PneumaticSmokeParticle;
import mrtjp.projectred.expansion.graphs.ClientSideLinkCache;
import mrtjp.projectred.expansion.graphs.GraphContainer;
import mrtjp.projectred.expansion.init.ExpansionSounds;
import mrtjp.projectred.expansion.pneumatics.*;
import mrtjp.projectred.lib.InventoryLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static mrtjp.projectred.core.client.particle.ParticleAction.*;
import static mrtjp.projectred.expansion.ProjectRedExpansion.LOGGER;

public class PneumaticTubePart extends GraphContainerTubePart implements PneumaticTransportContainer, GraphContainer,
        TickablePart, PneumaticTube, ILowLoadPowerLine, IPowerConnectable, IPowerConductorSource {

    private static final int KEY_NEW_PAYLOAD = 0x10;
    private static final int KEY_PAYLOAD_UPDATE = 0x11;
    private static final int KEY_PAYLOAD_HANDOFF = 0x12;
    private static final int KEY_PAYLOAD_REMOVE = 0x13;
    private static final int KEY_NODE_LINKS_UPDATE = 0x14;
    private static final int KEY_NODE_STATE_UPDATE = 0x15;
    private static final int KEY_TUBE_DATA_UPDATE = 0x16;

    private static final String TUBE_DATA_KEY = "projectred_tube_data";

    public final ClientSideLinkCache linkCache = new ClientSideLinkCache();
    private final PneumaticTransport transport = new PneumaticTransport(this);
    private final PneumaticTubeData customData = new PneumaticTubeData();
    private final PowerConductor lowLoadConductor = new PowerConductor(this, 0.01, 16);

    private int lastRoundRobinDir = -1;

    public PneumaticTubePart(TubeType pipeType) {
        super(pipeType);
        if (FMLEnvironment.dist.isClient()) {
            linkCache.setRemovedCallback(this::onLinksRemoved);
            linkCache.setAddedCallback(this::onLinksAdded);
        }
    }

    @Override
    public PneumaticTransport getPneumaticTransport() {
        return transport;
    }

    //region Optional low-load power conductor
    private boolean isLowLoadPowerEnabled() {
        return PneumaticTubePowerRegistry.isLowLoadPowerEnabled(this);
    }

    @Override
    public long getTime() {
        return level().getGameTime();
    }

    @Override
    public PowerConductor getConductor(int dir) {
        return lowLoadConductor;
    }

    @Override
    public List<PowerConductor> getConnectedConductors() {
        if (!isLowLoadPowerEnabled()) {
            return Collections.emptyList();
        }

        List<PowerConductor> conductors = new ArrayList<>();

        for (int s = 0; s < 6; s++) {
            CenterLookup lookup;

            if (maskConnectsIn(s)) {
                lookup = CenterLookup.lookupInsideFace(level(), pos(), s);
            } else if (maskConnectsOut(s)) {
                lookup = CenterLookup.lookupStraightCenter(level(), pos(), s);
            } else {
                continue;
            }

            if (lookup.part instanceof PneumaticTubePart otherTube
                    && !otherTube.isLowLoadPowerEnabled()) {
                continue;
            }

            if (lookup.part instanceof IPowerConnectable powerPart) {
                conductors.add(powerPart.getConductor(lookup.otherDirection));
            } else if (lookup.tile instanceof IPowerConnectable powerTile) {
                conductors.add(powerTile.getConductor(lookup.otherDirection));
            }
        }

        return conductors;
    }
    //endregion

    //region Save/load
    @Override
    public void save(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.save(tag, lookupProvider);
        tag.putByte("last_dir", (byte) lastRoundRobinDir);
        transport.save(tag, lookupProvider);
        if (customData.isEmpty()) {
            tag.remove(TUBE_DATA_KEY);
        } else {
            tag.put(TUBE_DATA_KEY, customData.save());
        }
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.load(tag, lookupProvider);
        lastRoundRobinDir = tag.getByte("last_dir");
        transport.load(tag, lookupProvider);
        customData.load(tag.contains(TUBE_DATA_KEY, Tag.TAG_COMPOUND)
                ? tag.getCompound(TUBE_DATA_KEY)
                : new CompoundTag());
    }

    @Override
    public void writeDesc(MCDataOutput packet) {
        super.writeDesc(packet);
        transport.writeDesc(packet);
        linkCache.writeDesc(packet);
        packet.writeCompoundNBT(customData.save());
    }

    @Override
    public void readDesc(MCDataInput packet) {
        super.readDesc(packet);
        transport.readDesc(packet);
        linkCache.readDesc(packet);
        customData.load(packet.readCompoundNBT());
    }
    //endregion

    //region Network
    @Override
    protected void read(MCDataInput packet, int key) {
        switch (key) {
            case KEY_NEW_PAYLOAD -> {
                int id = packet.readInt();
                PneumaticTubePayload payload = new PneumaticTubePayload();
                payload.readDesc(packet);
                transport.addPayload(id, payload);
            }
            case KEY_PAYLOAD_UPDATE -> {
                int id = packet.readInt();
                PneumaticTubePayload payload = transport.getPayload(id);
                if (payload == null) {
                    LOGGER.warn("Pneumatic tube got update for non-existent payload");
                    payload = new PneumaticTubePayload(); // For read-through
                }
                payload.readDesc(packet);
            }
            case KEY_PAYLOAD_HANDOFF -> readPayloadHandoff(packet);
            case KEY_PAYLOAD_REMOVE -> readPayloadRemoved(packet);
            case KEY_NODE_LINKS_UPDATE -> linkCache.readLinkUpdate(packet);
            case KEY_NODE_STATE_UPDATE -> linkCache.readStateUpdate(packet);
            case KEY_TUBE_DATA_UPDATE -> {
                customData.load(packet.readCompoundNBT());
                tile().markRender();
            }
            default -> super.read(packet, key);
        }
    }

    private void sendNewPayload(int id, PneumaticTubePayload payload) {
        sendUpdate(KEY_NEW_PAYLOAD, out -> {
            out.writeInt(id);
            payload.writeDesc(out);
        });
    }

    private void sendPayloadUpdate(int id, PneumaticTubePayload payload) {
        sendUpdate(KEY_PAYLOAD_UPDATE, out -> {
            out.writeInt(id);
            payload.writeDesc(out);
        });
    }

    private void sendPayloadHandoff(int id, PneumaticTubePayload payload, int newId, int dir) {
        sendUpdate(KEY_PAYLOAD_HANDOFF, out -> {
            out.writeInt(id);
            payload.writeDesc(out);
            out.writeInt(newId);
            out.writeByte(dir);
        });
    }

    private void sendPayloadRemoved(Collection<Integer> ids) {
        sendUpdate(KEY_PAYLOAD_REMOVE, out -> {
            out.writeInt(ids.size());
            for (int id : ids) {
                out.writeInt(id);
            }
        });
    }

    private void sendNodeLinkUpdate() {
        sendUpdate(KEY_NODE_LINKS_UPDATE, linkCache::writeLinkUpdate);
    }

    private void sendNodeStateUpdate() {
        sendUpdate(KEY_NODE_STATE_UPDATE, linkCache::writeStateUpdate);
    }

    private void sendTubeDataUpdate() {
        sendUpdate(KEY_TUBE_DATA_UPDATE, out -> out.writeCompoundNBT(customData.save()));
    }

    private void onTubeDataChanged() {
        if (level().isClientSide) return;

        tile().setChanged();
        updateInsideAndOutside();

        // Connection policy may depend on metadata. Neighbouring multipart
        // tubes must also rebuild their masks when this tube's metadata changes.
        for (int s = 0; s < 6; s++) {
            var neighbour = posOfStraight(s);
            level().neighborChanged(
                    neighbour, tile().getBlockState().getBlock(), pos());
        }

        node.markLinksChanged();
        sendTubeDataUpdate();
    }

    private void readPayloadHandoff(MCDataInput packet) {
        int id = packet.readInt();
        PneumaticTubePayload payload = transport.removePayload(id);
        if (payload == null) {
            LOGGER.warn("Pneumatic tube got handoff for non-existent payload");
            payload = new PneumaticTubePayload(); // For read-through
        }
        payload.readDesc(packet);

        int newId = packet.readInt();
        int side = packet.readUByte();

        CenterLookup lookup = CenterLookup.lookupStraightCenter(level(), pos(), side);
        if (lookup.part instanceof PneumaticTransportContainer ptc) {
            ptc.getPneumaticTransport().addPayload(newId, payload);
        }
    }

    private void readPayloadRemoved(MCDataInput packet) {
        int count = packet.readInt();
        for (int i = 0; i < count; i++) {
            transport.removePayload(packet.readInt());
        }
    }
    //endregion

    //region PneumaticTube API
    @Override
    public net.minecraft.world.level.Level getLevel() {
        return level();
    }

    @Override
    public net.minecraft.core.BlockPos getPos() {
        return pos();
    }

    @Override
    public boolean hasData(ResourceLocation key) {
        return customData.has(key);
    }

    @Override
    public CompoundTag getData(ResourceLocation key) {
        return customData.get(key);
    }

    @Override
    public void setData(ResourceLocation key, CompoundTag value) {
        customData.put(key, value);
        onTubeDataChanged();
    }

    @Override
    public void removeData(ResourceLocation key) {
        if (!customData.has(key)) return;

        customData.remove(key);
        onTubeDataChanged();
    }
    //endregion

    //region MultiPart events
    @Override
    public void onRemoved() {
        super.onRemoved();
        if (!level().isClientSide) {
            for (var i : transport.getPayloads()) {
                TileMultipart.dropItem(i.getItemStack(), level(), Vector3.fromTileCenter(tile()));
            }
        }
    }

    @Override
    public void maskChangeEvent(boolean internalChange, boolean externalChange) {
        super.maskChangeEvent(internalChange, externalChange);
        if (internalChange || externalChange) {
            // Drop items that were previously in closed off connections
            var removed = transport.removePayloadsConditionally(p -> {
                if (!maskConnects(p.getCurrentSide())) {
                    TileMultipart.dropItem(p.getItemStack(), level(), Vector3.fromTileCenter(tile()));
                    return true;
                } else {
                    return false;
                }
            });
            sendPayloadRemoved(removed);
        }
    }

    @Override
    public void tick() {
        super.tick();
        transport.tick();

        if (!level().isClientSide && isLowLoadPowerEnabled()) {
            lowLoadConductor.tick();
        }
    }
    //endregion

    //region Links changed callbacks

    //endregion

    @Override
    protected void onRemovalSeveredLink() {
        super.onRemovalSeveredLink();
        // On removal, we must've severed an active link. Play pressure sound
        level().playSound(null, pos(), ExpansionSounds.DEPRESSURIZE.get(), SoundSource.BLOCKS,
                0.7F + level().getRandom().nextFloat() * 0.3F, //Volume
                1.0F);
    }

    @Override
    public boolean canConnectPart(IConnectable part, int s) {
        if (part instanceof PneumaticTube otherTube
                && !PneumaticTubeConnectionRegistry.canConnect(
                        this,
                        otherTube,
                        Direction.values()[s])) {
            return false;
        }

        if (isLowLoadPowerEnabled()
                && (part instanceof ILowLoadPowerLine
                || part instanceof ILowLoadMachine)) {
            return true;
        }

        if (part instanceof PneumaticTransportContainer) return true;

        return super.canConnectPart(part, s);
    }

    @Override
    public boolean discoverStraightOverride(int s) {
        if (isLowLoadPowerEnabled()) {
            CenterLookup lookup =
                    CenterLookup.lookupStraightCenter(level(), pos(), s);

            if (lookup.tile instanceof IConnectable connectable
                    && (lookup.tile instanceof ILowLoadPowerLine
                    || lookup.tile instanceof ILowLoadMachine)
                    && connectable.connectStraight(
                            this,
                            lookup.otherDirection,
                            -1)) {
                return true;
            }
        }

        return hasEndpointOnSide(s);
    }

    private boolean hasEndpointOnSide(int s) {
        CenterLookup lookup = CenterLookup.lookupStraightCenter(level(), pos(), s);
        if (lookup.tile == null) return false;

        if (lookup.tile instanceof PneumaticTransportDevice p) {
            return p.canConnectTube(lookup.otherDirection);
        }

        if (lookup.tile instanceof WorldlyContainer wc) {
            return wc.getSlotsForFace(Direction.values()[lookup.otherDirection]).length > 0;
        }

        var itemCap = level().getCapability(Capabilities.ItemHandler.BLOCK, lookup.tile.getBlockPos(), Direction.values()[lookup.otherDirection]);
        if (itemCap != null) {
            return itemCap.getSlots() > 0;
        }

        return false;
    }

    //region Particles
    @OnlyIn(Dist.CLIENT)
    private void onLinksRemoved(List<ClientSideLinkCache.ClientLink> removed) {
        for (var link : removed) {
            var points = link.getPointListFor(pos());
            var p = new PneumaticSmokeParticle((ClientLevel) level(), points);
            p.setAlpha(0.0F);
            p.addAction(
                    sequence(
                            changeAlphaTo(0.7, 1),
                            changeAlphaTo(0.0, 5),
                            remove()
                    )
            );
            Minecraft.getInstance().particleEngine.add(p);
            level().playLocalSound(pos(), ExpansionSounds.DEPRESSURIZE.get(), SoundSource.BLOCKS,
                    0.7F + level().getRandom().nextFloat() * 0.3F, //Volume
                    1.0F,
                    true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void onLinksAdded(List<ClientSideLinkCache.ClientLink> added) {
        for (var link : added) {
            var points = link.getPointListFor(pos());
            var p = new PneumaticSmokeParticle((ClientLevel) level(), points);
            p.setAlpha(0.0F);
            p.addAction(
                    sequence(
                            changeAlphaTo(0.7, 1),
                            changeAlphaTo(0.0, 10),
                            remove()
                    )
            );
            Minecraft.getInstance().particleEngine.add(p);
            level().playLocalSound(pos(), ExpansionSounds.PRESSURIZE.get(), SoundSource.BLOCKS,
                    0.7F + level().getRandom().nextFloat() * 0.3F, //Volume
                    1.0F,
                    true);
        }
    }
    //endregion

    //region IGraphNodeContainer overrides
    @Override
    public void onNodeChanged(boolean linksChanged, boolean stateChange) {
        if (!level().isClientSide) {
            if (linksChanged) {
                linkCache.setLinks(node.getLinks());
                linkCache.setActive(node.isActive());
                sendNodeLinkUpdate();
            } else if (stateChange) {
                linkCache.setActive(node.isActive());
                sendNodeStateUpdate();
            }
        }
    }

    @Override
    public boolean requiresActiveNode() {
        // Addons may mark an otherwise-redundant tube location as routing
        // significant. This decision is payload-independent so the topology
        // remains globally cacheable.
        if (PneumaticRouteRegistry.requiresRoutingNode(
                new PneumaticRouteNodeContext(level(), pos()))) {
            return true;
        }

        int connectedTubes = 0;
        for (int s = 0; s < 6; s++) {
            if (maskConnects(s)) {
//                CenterLookup lookup = CenterLookup.lookupStraightCenter(level(), pos(), s);
//                if (lookup.part instanceof PneumaticTransportContainer) {
//                    connectedTubes++;
//                    if (connectedTubes > 2) {
//                        return false;
//                    }
//                }

                //TODO Temporarily treat all connections as tubes to avoid routing into
                //     redstone. See comment below in {@link setOutputDirection}.
                connectedTubes++; // Assume all conns are tubes
                if (connectedTubes > 2) {
                    return true;
                }

                // Location is significant to routing logic so requires active node
                if (hasEndpointOnSide(s)) {
                    return true;
                }
            }
        }

        // Payload can't branch to multiple outputs, so no need for active node
        return false;
    }
    //endregion

    //region PneumaticTransportContainer
    @Override
    public void setOutputDirection(PneumaticTubePayload payload) {

        if (level().isClientSide) return; // Clients will get dir from packet

        //TODO This strategy of using the connMask breaks down when pipe is connected to redstone blocks.
        //     We should have a separate portion of the mask just for route-able connections. Otherwise,
        //     the dumb non-node pipes will choose the redstone side. For temporary fix, {@link checkRedundant}
        //     will mark all tubes with conns > 2 as non-redundant.
        int connMask = getConnMap() & 0x3F;
        int connCount = Integer.bitCount(getConnMap() & 0x3F);
        // Error case. For whatever reason, we have no connections
        if (connCount == 0) {
            return; // TODO do something here?
        }
        // If there's only 1 possible output, we have to take it. Even if its bounced back
        if (connCount == 1) {
            payload.setOutputSide(Integer.numberOfTrailingZeros(connMask));
            return;
        }

        // There are multiple sides connected. Try and find an exit
        int exitMask = connMask & ~(1 << payload.getInputSide()); // Conn mask excluding input
        int exitCount = Integer.bitCount(exitMask);

        // connCount is >= 1, so exitCount is at least 1
        assert exitCount > 0;

        // If there's only 1 possible output, we have to take it.
        if (exitCount == 1) {
            int s = Integer.numberOfTrailingZeros(exitMask);
            payload.setOutputSide(s);
            return;
        }

        // Multiple exits possible. Check route table.
        PneumaticExitPathfinder exitFinder = new PneumaticExitPathfinder(this, getNode().getRouteTable(), payload, exitMask);
        var result = exitFinder.result();
        int routeMask = result.exitDirMask();
        int routeCount = Integer.bitCount(routeMask);

        // Multiple exits but no routes. Choose one of them round-robin style
        if (routeCount == 0) {
            setOutputRoundRobin(payload, exitMask);
            return;
        }

        // Only one possible route. Take it
        if (routeCount == 1) {
            int s = Integer.numberOfTrailingZeros(routeMask);
            payload.setOutputSide(s);
            return;
        }

        // Multiple possible routes. Choose one round-robin style
        setOutputRoundRobin(payload, routeMask);
    }

    private void setOutputRoundRobin(PneumaticTubePayload payload, int dirMask) {
        assert (dirMask & 0x3F) != 0;

        // Find first possible direction, but start looking after last one
        int s = lastRoundRobinDir;
        do {
            s = (s + 1) % 6;
        } while ((dirMask & (1 << s)) == 0);

        lastRoundRobinDir = s; //TODO maybe this should track *every* routed direction?
        payload.setOutputSide(s);
    }

    @Override
    public void onPayloadChanged(int id, PneumaticTubePayload payload) {
        if (!level().isClientSide) {
            sendPayloadUpdate(id, payload);
        }
    }

    @Override
    public boolean onPayloadReachedOutput(int id, PneumaticTubePayload payload) {
        if (level().isClientSide) return false; //Client will keep item until handoff packet

        CenterLookup lookup = CenterLookup.lookupStraightCenter(level(), pos(), payload.getOutputSide());

        // Pass to next tube
        if (lookup.part instanceof PneumaticTransportContainer ptc) {
            // Set new directions
            int side = payload.getOutputSide();
            payload.setInputSide(payload.getOutputSide() ^ 1);
            payload.resetOutput();
            payload.resetProgress();

            int newId = ptc.getPneumaticTransport().addPayload(payload);
            sendPayloadHandoff(id, payload, newId, side);
            return true;
        }

        if (lookup.tile == null) return false;

        // Pass to device
        if (lookup.tile instanceof PneumaticTransportDevice p) {
            if (p.insertPayload(lookup.otherDirection, payload)) {
                sendPayloadRemoved(Collections.singletonList(id));
                return true;
            }
        }

        // Pass to worldly container or item handler
        if (lookup.tile instanceof WorldlyContainer wc) {
            if (InventoryLib.injectWorldly(wc, payload.getItemStack(), lookup.otherDirection, false)) {
                sendPayloadRemoved(Collections.singletonList(id));
                return true;
            }
        }

        // Pass to IItemHandler capability
        var itemCap = level().getCapability(Capabilities.ItemHandler.BLOCK, lookup.tile.getBlockPos(), Direction.values()[lookup.otherDirection]);
        if (itemCap != null) {
            if (InventoryLib.injectItemHandler(itemCap, payload.getItemStack(), false)) {
                sendPayloadRemoved(Collections.singletonList(id));
                return true;
            }
        }

        // Either nothing was passed, or only partial pass (payload would've been modified). Bounce it back
        int tmp = payload.getInputSide();
        payload.setInputSide(payload.getOutputSide());
        payload.setOutputSide(tmp);
        payload.resetProgress();
        sendPayloadUpdate(id, payload);
        return false;
    }

    @Override
    public boolean canItemExitTube(PneumaticTubePayload payload, int side, PneumaticTransportMode mode) {
        if (!maskConnects(side)) {
            return false;
        }

        CenterLookup lookup = CenterLookup.lookupStraightCenter(level(), pos(), side);
        if (lookup.tile == null) return false;

        // Check pneumatic device
        if (lookup.tile instanceof PneumaticTransportDevice p) {
            return p.canAcceptPayload(lookup.otherDirection, payload, mode);
        }

        // Check worldly container
        if (lookup.tile instanceof WorldlyContainer wc) {
            ItemStack copy = payload.getItemStack().copy();
            InventoryLib.injectWorldly(wc, copy, lookup.otherDirection, true);
            if (copy.getCount() < payload.getItemStack().getCount()) { // If anything was inserted
                return true;
            }
        }

        // Check IItemHandler cap
        var itemCap = level().getCapability(Capabilities.ItemHandler.BLOCK, lookup.tile.getBlockPos(), Direction.values()[lookup.otherDirection]);
        if (itemCap != null) {
            ItemStack copy = payload.getItemStack().copy();
            InventoryLib.injectItemHandler(itemCap, copy, true);
            if (copy.getCount() < payload.getItemStack().getCount()) { // If anything was inserted
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canItemEnterTube(PneumaticTubePayload payload, int side) {
        if (!maskConnects(side)) {
            return false;
        }

        // A route policy must also be able to reject the first physical tube
        // a payload is being inserted into. Pathfinding normally evaluates
        // transitions after the start container, which is too late for
        // per-tube admission rules such as painted tube colours.
        if (PneumaticRouteRegistry.hasPolicies()) {
            Direction incomingSide = Direction.values()[side];
            var decision = PneumaticRouteRegistry.evaluate(
                    payload,
                    new PneumaticRouteContext(
                            level(),
                            pos().relative(incomingSide),
                            pos(),
                            incomingSide.getOpposite(),
                            0));
            if (!decision.allowed()) return false;
        }

        PneumaticExitPathfinder exitFinder = new PneumaticExitPathfinder(this, getNode().getRouteTable(), payload, ~(1 << side), List.of(PneumaticTransportMode.PASSIVE_NORMAL));
        return exitFinder.result().exitDirMask() != 0;
    }

    @Override
    public boolean insertPayload(int s, PneumaticTubePayload payload) {
        if (!canItemEnterTube(payload, s)) {
            return false;
        }

        payload.resetProgress();
        payload.resetOutput();
        payload.setSpeed(12);
        payload.setInputSide(s);
        int id = transport.addPayload(payload);
        sendNewPayload(id, Objects.requireNonNull(transport.getPayload(id)));
        return true;
    }
    //endregion
}
