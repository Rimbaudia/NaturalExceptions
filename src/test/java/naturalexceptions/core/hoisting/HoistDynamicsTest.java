package naturalexceptions.core.hoisting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.hoisting.HoistDynamics.Crank;

class HoistDynamicsTest {

    private static final double RIDER_IN_BASKET = 110.0;

    private static HoistDynamics run(HoistDynamics rope, int ticks, double load, double counter, Crank crank,
        int winding, boolean brake) {
        for (int i = 0; i < ticks; i++) rope.step(load, counter, crank, winding, brake, 1.0, 30.0);
        return rope;
    }

    @Test
    void theBrakeHoldsWhateverHangsOnIt() {
        HoistDynamics rope = run(new HoistDynamics(10.0), 200, 400.0, 0.0, Crank.NONE, 0, true);
        assertEquals(10.0, rope.paidOut(), 1e-9);
        assertEquals(0.0, rope.speed(), 1e-9);
    }

    @Test
    void withTheBrakeSetACrankStillWindsUpPastThePawl() {
        HoistDynamics rope = run(new HoistDynamics(20.0), 100, RIDER_IN_BASKET, 0.0, Crank.WINDLASS, -1, true);
        assertTrue(rope.paidOut() < 17.0, "wound up: " + rope.paidOut());
        double before = rope.paidOut();
        run(rope, 100, RIDER_IN_BASKET, 0.0, Crank.NONE, 0, true);
        assertEquals(before, rope.paidOut(), 1e-9, "and the pawl holds it there");
    }

    @Test
    void easingTheBrakeLetsItDownNoFasterThanTheCrank() {
        HoistDynamics rope = new HoistDynamics(5.0);
        for (int i = 0; i < 100; i++) {
            rope.step(400.0, 0.0, Crank.WINDLASS, 1, true, 1.0, 30.0);
            assertTrue(rope.speed() <= Crank.WINDLASS.speed + 1e-12, "eased: " + rope.speed());
        }
        assertTrue(rope.paidOut() > 7.0, "it went down: " + rope.paidOut());
    }

    @Test
    void theHeavierSideGoesDown() {
        assertTrue(run(new HoistDynamics(10.0), 40, RIDER_IN_BASKET, 60.0, Crank.NONE, 0, false).paidOut() > 10.5);
        assertTrue(run(new HoistDynamics(10.0), 40, RIDER_IN_BASKET, 200.0, Crank.NONE, 0, false).paidOut() < 9.5);
    }

    @Test
    void balancedItStaysWhereItIs() {
        assertEquals(10.0, run(new HoistDynamics(10.0), 100, 100.0, 100.0, Crank.NONE, 0, false).paidOut(), 1e-9);
    }

    @Test
    void aCrankWindsARiderUpSteadilyAgainstTheirWeight() {
        HoistDynamics rope = run(new HoistDynamics(20.0), 100, RIDER_IN_BASKET, 0.0, Crank.WINDLASS, -1, false);
        assertTrue(rope.paidOut() < 20.0 - 3.0, "wound up: " + rope.paidOut());
        assertEquals(-Crank.WINDLASS.speed, rope.speed(), 0.002, "at the crank's own speed");
    }

    @Test
    void aMuchHeavierLoadOverpowersTheCrank() {
        HoistDynamics rope = run(new HoistDynamics(10.0), 60, 600.0, 0.0, Crank.WINDLASS, -1, false);
        assertTrue(rope.paidOut() > 10.0, "it went down regardless: " + rope.paidOut());
    }

    @Test
    void theHandLineIsSlowerThanTheWindlass() {
        double windlass = 20.0
            - run(new HoistDynamics(20.0), 100, RIDER_IN_BASKET, 0.0, Crank.WINDLASS, -1, false).paidOut();
        double hand = 20.0
            - run(new HoistDynamics(20.0), 100, RIDER_IN_BASKET, 0.0, Crank.HAND_LINE, -1, false).paidOut();
        assertTrue(hand > 0 && hand < windlass, "hand " + hand + " windlass " + windlass);
    }

    @Test
    void theGovernorCapsItsSpeedAndItStopsAtTheFloor() {
        HoistDynamics rope = new HoistDynamics(1.0);
        double fastest = 0;
        for (int i = 0; i < 1200; i++) {
            rope.step(400.0, 0.0, Crank.NONE, 0, false, 1.0, 30.0);
            fastest = Math.max(fastest, rope.speed());
        }
        assertTrue(fastest <= HoistDynamics.MAX_SPEED + 1e-12);
        assertEquals(30.0, rope.paidOut(), 1e-9);
        assertEquals(0.0, rope.speed(), 1e-9);
    }

    @Test
    void itSlowsComingInToTheFloor() {
        HoistDynamics rope = new HoistDynamics(1.0);
        while (rope.paidOut() < 29.7) rope.step(400.0, 0.0, Crank.NONE, 0, false, 1.0, 30.0);
        // Well under the governor's speed, which alone would bring it in at full tilt.
        assertTrue(rope.speed() <= HoistDynamics.MAX_SPEED * 0.75, "coming in at " + rope.speed());
    }

    @Test
    void itStopsUpUnderTheSheave() {
        HoistDynamics rope = run(new HoistDynamics(5.0), 400, 50.0, 300.0, Crank.NONE, 0, false);
        assertEquals(1.0, rope.paidOut(), 1e-9);
    }

    @Test
    void itGathersSpeedRatherThanJumpingToIt() {
        HoistDynamics rope = new HoistDynamics(10.0);
        rope.step(RIDER_IN_BASKET, 60.0, Crank.NONE, 0, false, 1.0, 30.0);
        assertTrue(rope.speed() > 0 && rope.speed() < HoistDynamics.MAX_SPEED / 4, "first tick: " + rope.speed());
    }
}
