package mrtjp.projectred.api.pneumatics;

/**
 * Result returned by a pneumatic routing policy.
 *
 * @param allowed whether this transition may be used
 * @param additionalCost non-negative cost added to ProjectRed's link weight
 */
public record PneumaticRouteDecision(boolean allowed, int additionalCost) {

    public static final PneumaticRouteDecision PASS =
            new PneumaticRouteDecision(true, 0);
    public static final PneumaticRouteDecision BLOCK =
            new PneumaticRouteDecision(false, 0);

    public PneumaticRouteDecision {
        if (additionalCost < 0) {
            throw new IllegalArgumentException("additionalCost must be non-negative");
        }
    }

    public static PneumaticRouteDecision cost(int additionalCost) {
        return new PneumaticRouteDecision(true, additionalCost);
    }
}