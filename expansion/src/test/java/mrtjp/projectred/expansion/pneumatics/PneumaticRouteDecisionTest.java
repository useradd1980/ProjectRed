package mrtjp.projectred.expansion.pneumatics;

import mrtjp.projectred.api.pneumatics.PneumaticRouteDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PneumaticRouteDecisionTest {

    @Test
    void negativeCostsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> PneumaticRouteDecision.cost(-1));
    }

    @Test
    void constantsExpressExpectedSemantics() {
        assertTrue(PneumaticRouteDecision.PASS.allowed());
        assertEquals(0, PneumaticRouteDecision.PASS.additionalCost());
        assertFalse(PneumaticRouteDecision.BLOCK.allowed());
        assertEquals(0, PneumaticRouteDecision.BLOCK.additionalCost());
    }
}