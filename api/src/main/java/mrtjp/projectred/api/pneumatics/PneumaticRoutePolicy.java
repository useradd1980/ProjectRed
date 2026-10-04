package mrtjp.projectred.api.pneumatics;

/**
 * Extension point for payload-aware pneumatic routing.
 *
 * Implementations should be deterministic for a given payload and world state
 * and must not mutate ProjectRed's routing graph from inside callbacks.
 */
public interface PneumaticRoutePolicy {

    PneumaticRouteDecision evaluate(PneumaticPayload payload, PneumaticRouteContext context);

    /**
     * Return true when this physical tube location carries state that can alter
     * routing. Such a location is retained as a graph node instead of being
     * compressed into a longer topology link.
     *
     * This callback is payload-independent because the topology is shared by
     * every payload.
     */
    default boolean requiresRoutingNode(PneumaticRouteNodeContext context) {
        return false;
    }
}