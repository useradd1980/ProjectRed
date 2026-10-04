package mrtjp.projectred.expansion;

import mrtjp.projectred.api.pneumatics.PneumaticPayload;
import mrtjp.projectred.api.pneumatics.PneumaticRouteContext;
import mrtjp.projectred.api.pneumatics.PneumaticRouteDecision;
import mrtjp.projectred.api.pneumatics.PneumaticRouteNodeContext;
import mrtjp.projectred.api.pneumatics.PneumaticRoutePolicy;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PneumaticRouteRegistry {

    private static final List<PneumaticRoutePolicy> POLICIES = new CopyOnWriteArrayList<>();

    private PneumaticRouteRegistry() { }

    public static void register(PneumaticRoutePolicy policy) {
        POLICIES.add(Objects.requireNonNull(policy, "policy"));
    }

    public static boolean hasPolicies() {
        return !POLICIES.isEmpty();
    }

    public static boolean requiresRoutingNode(PneumaticRouteNodeContext context) {
        for (var policy : POLICIES) {
            if (policy.requiresRoutingNode(context)) return true;
        }
        return false;
    }

    public static PneumaticRouteDecision evaluate(PneumaticPayload payload, PneumaticRouteContext context) {
        long addedCost = 0;

        for (var policy : POLICIES) {
            var result = Objects.requireNonNull(
                    policy.evaluate(payload, context),
                    "Pneumatic route policy returned null");

            if (!result.allowed()) return PneumaticRouteDecision.BLOCK;

            addedCost += result.additionalCost();
            if (addedCost >= Integer.MAX_VALUE) {
                addedCost = Integer.MAX_VALUE;
                break;
            }
        }

        return addedCost == 0
                ? PneumaticRouteDecision.PASS
                : PneumaticRouteDecision.cost((int) addedCost);
    }
}