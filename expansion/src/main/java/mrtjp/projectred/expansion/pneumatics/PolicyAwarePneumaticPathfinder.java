package mrtjp.projectred.expansion.pneumatics;

import mrtjp.projectred.api.pneumatics.PneumaticRouteContext;
import mrtjp.projectred.expansion.PneumaticRouteRegistry;
import mrtjp.projectred.expansion.graphs.GraphContainer;
import mrtjp.projectred.expansion.graphs.GraphLink;
import mrtjp.projectred.expansion.graphs.GraphNode;
import mrtjp.projectred.expansion.part.GraphContainerTubePart;
import mrtjp.projectred.expansion.part.PneumaticTubePayload;
import net.minecraft.core.Direction;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Payload-aware Dijkstra search over ProjectRed's already-cached topology
 * links. The physical tube network is not rediscovered per payload.
 */
final class PolicyAwarePneumaticPathfinder {

    private final PneumaticTransportContainer startContainer;
    private final PneumaticTubePayload payload;
    private final int dirMask;
    private final PneumaticTransportMode mode;

    PolicyAwarePneumaticPathfinder(PneumaticTransportContainer startContainer,
                                   PneumaticTubePayload payload,
                                   int dirMask,
                                   PneumaticTransportMode mode) {
        this.startContainer = startContainer;
        this.payload = payload;
        this.dirMask = dirMask;
        this.mode = mode;
    }

    PneumaticExitPathfinder.PneumaticExits result() {
        if (!(startContainer instanceof GraphContainer startGraph)) {
            return new PneumaticExitPathfinder.PneumaticExits(
                    0, Integer.MAX_VALUE, null);
        }

        var queue = new PriorityQueue<State>(Comparator.comparingInt(State::cost));
        Map<StateKey, Integer> best = new HashMap<>();

        for (GraphLink link : startGraph.getNode().getLinks()) {
            int firstDir = link.direction();
            if ((dirMask & (1 << firstDir)) == 0) continue;
            enqueue(queue, best, link, firstDir, 0);
        }

        int bestExitCost = Integer.MAX_VALUE;
        int exitMask = 0;

        while (!queue.isEmpty()) {
            State state = queue.poll();
            StateKey stateKey = new StateKey(state.node(), state.firstDir());

            if (state.cost() != best.getOrDefault(stateKey, Integer.MAX_VALUE)) {
                continue;
            }
            if (state.cost() > bestExitCost) break;

            if (state.node().container instanceof PneumaticTransportContainer ptc) {
                for (int side = 0; side < 6; side++) {
                    if (ptc.canItemExitTube(payload, side, mode)) {
                        if (state.cost() < bestExitCost) {
                            bestExitCost = state.cost();
                            exitMask = 0;
                        }
                        exitMask |= 1 << state.firstDir();
                        break;
                    }
                }
            }

            if (state.cost() >= bestExitCost) continue;

            for (GraphLink link : state.node().getLinks()) {
                enqueue(queue, best, link, state.firstDir(), state.cost());
            }
        }

        return new PneumaticExitPathfinder.PneumaticExits(
                exitMask,
                bestExitCost,
                exitMask == 0 ? null : mode);
    }

    private void enqueue(PriorityQueue<State> queue,
                         Map<StateKey, Integer> best,
                         GraphLink link,
                         int firstDir,
                         int previousCost) {
        PneumaticRouteContext context = makeContext(link);
        if (context == null) return;

        var decision = PneumaticRouteRegistry.evaluate(payload, context);
        if (!decision.allowed()) return;

        long total = (long) previousCost + link.weight() + decision.additionalCost();
        int cost = total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;

        GraphNode node = link.to().getNode();
        StateKey key = new StateKey(node, firstDir);

        if (cost >= best.getOrDefault(key, Integer.MAX_VALUE)) return;

        best.put(key, cost);
        queue.add(new State(node, firstDir, cost));
    }

    private static PneumaticRouteContext makeContext(GraphLink link) {
        if (!(link.from() instanceof GraphContainerTubePart from)) return null;
        if (!(link.to() instanceof GraphContainerTubePart to)) return null;

        return new PneumaticRouteContext(
                from.level(),
                from.pos(),
                to.pos(),
                Direction.values()[link.direction()],
                link.weight());
    }

    private record State(GraphNode node, int firstDir, int cost) { }

    private record StateKey(GraphNode node, int firstDir) { }
}