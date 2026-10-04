package mrtjp.projectred.expansion;

import mrtjp.projectred.api.pneumatics.PneumaticTube;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Generic opt-in registry for low-load power conduction through pneumatic
 * tubes. The registry deliberately knows nothing about electrotine or addon
 * metadata; those semantics remain in the registering addon.
 */
public final class PneumaticTubePowerRegistry {

    private static final List<Predicate<PneumaticTube>> PREDICATES =
            new CopyOnWriteArrayList<>();

    private PneumaticTubePowerRegistry() { }

    public static void register(Predicate<PneumaticTube> predicate) {
        PREDICATES.add(Objects.requireNonNull(predicate, "predicate"));
    }

    public static boolean isLowLoadPowerEnabled(PneumaticTube tube) {
        for (var predicate : PREDICATES) {
            if (predicate.test(tube)) return true;
        }
        return false;
    }
}