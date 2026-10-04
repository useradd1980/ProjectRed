package mrtjp.projectred.expansion.pneumatics;

import mrtjp.projectred.expansion.PneumaticRouteRegistry;
import mrtjp.projectred.expansion.graphs.GraphContainer;
import mrtjp.projectred.expansion.graphs.GraphLink;
import mrtjp.projectred.expansion.graphs.GraphLinkSegment;
import mrtjp.projectred.expansion.graphs.GraphNode;
import mrtjp.projectred.expansion.graphs.GraphRoute;
import mrtjp.projectred.expansion.graphs.GraphRouteTable;
import mrtjp.projectred.expansion.part.GraphContainerTubePart;
import mrtjp.projectred.expansion.part.PneumaticTubePayload;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class PneumaticRoutingPolicyIntegrationTest {

    @BeforeEach
    @AfterEach
    void clearRegisteredPolicies() throws ReflectiveOperationException {
        Field policies = PneumaticRouteRegistry.class.getDeclaredField("POLICIES");
        policies.setAccessible(true);
        ((List<?>) policies.get(null)).clear();
    }

    @Test
    void noPoliciesPreserveOriginalRouteTableSelection() {
        PneumaticTransportContainer start = mock(PneumaticTransportContainer.class);
        PneumaticTubePayload payload = new PneumaticTubePayload();

        Endpoint shortEndpoint = endpoint(true);
        Endpoint longEndpoint = endpoint(true);

        GraphNode routeStart = new GraphNode(mock(GraphContainer.class));
        GraphRoute shortRoute = new GraphRoute(
                routeStart,
                shortEndpoint.node(),
                2,
                List.of());
        GraphRoute longRoute = new GraphRoute(
                routeStart,
                longEndpoint.node(),
                5,
                List.of());

        int shortDirection = Direction.NORTH.ordinal();
        int longDirection = Direction.SOUTH.ordinal();

        HashMap<Integer, List<GraphRoute>> routesByDirection = new HashMap<>();
        routesByDirection.put(shortDirection, List.of(shortRoute));
        routesByDirection.put(longDirection, List.of(longRoute));

        GraphRouteTable routeTable = new GraphRouteTable(
                new HashMap<>(),
                routesByDirection,
                List.of(shortEndpoint.node(), longEndpoint.node()),
                List.of(shortRoute, longRoute));

        var result = new PneumaticExitPathfinder(
                start,
                routeTable,
                payload,
                (1 << shortDirection) | (1 << longDirection),
                List.of(PneumaticTransportMode.PASSIVE_NORMAL))
                .result();

        assertEquals(1 << shortDirection, result.exitDirMask());
        assertEquals(2, result.weight());
        assertSame(PneumaticTransportMode.PASSIVE_NORMAL, result.mode());
    }

    @Test
    void longerAllowedRouteBeatsShorterBlockedRoute() {
        PneumaticTubePayload payload = new PneumaticTubePayload();

        TubeNode start = tube(false);
        TubeNode shortMid = tube(false);
        TubeNode longMid = tube(false);
        TubeNode destination = tube(true);

        GraphLink shortFirst = link(start, shortMid, 1, Direction.EAST);
        GraphLink shortSecond = link(shortMid, destination, 1, Direction.NORTH);

        GraphLink longFirst = link(start, longMid, 2, Direction.WEST);
        GraphLink longSecond = link(longMid, destination, 3, Direction.NORTH);

        setLinks(start, shortFirst, longFirst);
        setLinks(shortMid, shortSecond);
        setLinks(longMid, longSecond);
        setLinks(destination);

        PneumaticRouteRegistry.register((ignoredPayload, context) ->
                context.direction() == Direction.EAST
                        ? mrtjp.projectred.api.pneumatics.PneumaticRouteDecision.BLOCK
                        : mrtjp.projectred.api.pneumatics.PneumaticRouteDecision.PASS);

        GraphRouteTable unusedRouteTable = new GraphRouteTable(
                new HashMap<>(),
                new HashMap<>(),
                List.of(),
                List.of());

        var result = new PneumaticExitPathfinder(
                start.transport(),
                unusedRouteTable,
                payload,
                (1 << Direction.EAST.ordinal()) | (1 << Direction.WEST.ordinal()),
                List.of(PneumaticTransportMode.PASSIVE_NORMAL))
                .result();

        assertEquals(1 << Direction.WEST.ordinal(), result.exitDirMask());
        assertEquals(5, result.weight());
        assertSame(PneumaticTransportMode.PASSIVE_NORMAL, result.mode());
    }

    private static Endpoint endpoint(boolean acceptsItems) {
        GraphContainer container = mock(
                GraphContainer.class,
                withSettings().extraInterfaces(PneumaticTransportContainer.class));

        PneumaticTransportContainer transport = (PneumaticTransportContainer) container;
        when(transport.canItemExitTube(
                any(PneumaticTubePayload.class),
                anyInt(),
                any(PneumaticTransportMode.class)))
                .thenReturn(acceptsItems);

        GraphNode node = new GraphNode(container);
        when(container.getNode()).thenReturn(node);

        return new Endpoint(container, transport, node);
    }

    private static TubeNode tube(boolean acceptsItems) {
        GraphContainerTubePart tube = mock(
                GraphContainerTubePart.class,
                withSettings().extraInterfaces(PneumaticTransportContainer.class));

        PneumaticTransportContainer transport = (PneumaticTransportContainer) tube;
        when(transport.canItemExitTube(
                any(PneumaticTubePayload.class),
                anyInt(),
                any(PneumaticTransportMode.class)))
                .thenReturn(acceptsItems);

        GraphNode node = spy(new GraphNode(tube));
        when(tube.getNode()).thenReturn(node);
        doReturn(List.of()).when(node).getLinks();

        return new TubeNode(tube, transport, node);
    }

    private static GraphLink link(TubeNode from, TubeNode to, int weight, Direction direction) {
        return new GraphLink(
                from.tube(),
                to.tube(),
                weight,
                List.of(new GraphLinkSegment(direction.ordinal(), 1)));
    }

    private static void setLinks(TubeNode node, GraphLink... links) {
        doReturn(List.of(links)).when(node.node()).getLinks();
    }

    private record Endpoint(
            GraphContainer container,
            PneumaticTransportContainer transport,
            GraphNode node) {
    }

    private record TubeNode(
            GraphContainerTubePart tube,
            PneumaticTransportContainer transport,
            GraphNode node) {
    }
}
