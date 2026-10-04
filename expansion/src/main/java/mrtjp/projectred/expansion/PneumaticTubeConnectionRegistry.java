package mrtjp.projectred.expansion;

import mrtjp.projectred.api.pneumatics.PneumaticTube;
import mrtjp.projectred.api.pneumatics.PneumaticTubeConnectionPolicy;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PneumaticTubeConnectionRegistry {

    private static final List<PneumaticTubeConnectionPolicy> POLICIES =
            new CopyOnWriteArrayList<>();

    private PneumaticTubeConnectionRegistry() { }

    public static void register(PneumaticTubeConnectionPolicy policy) {
        POLICIES.add(Objects.requireNonNull(policy, "policy"));
    }

    public static boolean canConnect(
            PneumaticTube from,
            PneumaticTube to,
            Direction direction) {

        for (var policy : POLICIES) {
            if (!policy.canConnect(from, to, direction)) return false;
        }
        return true;
    }
}