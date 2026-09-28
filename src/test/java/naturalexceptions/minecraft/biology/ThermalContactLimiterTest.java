package naturalexceptions.minecraft.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ThermalContactLimiterTest {

    @Test
    void sustainedContactResolvesOncePerSecondWithTimeProportionalEnergy() {
        ThermalContactLimiter limiter = new ThermalContactLimiter();
        Object body = new Object();
        double total = 0.0;
        int interactions = 0;
        // Five seconds of per-tick collision callbacks against ~1780 K matter.
        for (long tick = 1000; tick < 1100; tick++) {
            double joules = limiter.admit(body, tick, 60.0);
            if (joules > 0.0) {
                interactions++;
                total += joules;
            }
        }
        assertEquals(5, interactions, "one resolved interaction per second, not one per tick");
        assertEquals(300.0, total, 1.0e-9, "energy tracks contact time");
    }

    @Test
    void returningAfterABreakIsNotCreditedWithStoredExposure() {
        ThermalContactLimiter limiter = new ThermalContactLimiter();
        Object body = new Object();
        assertEquals(60.0, limiter.admit(body, 0L, 60.0), 1.0e-9);
        assertEquals(60.0, limiter.admit(body, 10_000L, 60.0), 1.0e-9);
        assertEquals(0.0, limiter.admit(body, 10_005L, 60.0), 1.0e-9);
    }

    @Test
    void bodiesAreLimitedIndependently() {
        ThermalContactLimiter limiter = new ThermalContactLimiter();
        Object a = new Object();
        Object b = new Object();
        assertEquals(60.0, limiter.admit(a, 5L, 60.0), 1.0e-9);
        assertEquals(60.0, limiter.admit(b, 5L, 60.0), 1.0e-9);
    }
}
