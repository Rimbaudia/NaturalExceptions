package naturalexceptions.core.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The nodal solve against circuits worked by hand. */
final class GridTest {

    @Test
    void aSourceAndALoadObeyOhm() {
        Grid grid = new Grid(1);
        int s = grid.source(0, 12.0, 1.0);
        int load = grid.load(0, 5.0);
        grid.solve();
        assertEquals(10.0, grid.voltage(0), 1e-6, "12 V across 1 + 5 ohms: 10 V at the terminal");
        assertEquals(2.0, grid.current(load), 1e-6);
        assertEquals(20.0, grid.power(load), 1e-6);
        assertEquals(2.0, grid.sourceCurrent(s), 1e-6);
        assertEquals(20.0, grid.sourcePower(s), 1e-6, "it delivers what the load burns");
    }

    @Test
    void aLongRunOfWireDropsItsShare() {
        int segments = 100;
        Grid grid = new Grid(segments + 1);
        grid.source(0, 24.0, 0.5);
        for (int i = 0; i < segments; i++) grid.resistor(i, i + 1, 0.05);
        int lamp = grid.load(segments, 4.5);
        grid.solve();
        double current = 24.0 / (0.5 + segments * 0.05 + 4.5);
        assertEquals(current, grid.current(lamp), 1e-6);
        assertEquals(current * 4.5, grid.voltage(segments), 1e-6, "the far end sees less than the near");
        assertTrue(grid.voltage(0) > grid.voltage(segments));
    }

    @Test
    void parallelLoadsShareTheSource() {
        Grid grid = new Grid(1);
        grid.source(0, 10.0, 0.0001);
        int a = grid.load(0, 10.0), b = grid.load(0, 10.0);
        grid.solve();
        assertEquals(1.0, grid.current(a), 1e-3);
        assertEquals(1.0, grid.current(b), 1e-3);
    }

    @Test
    void aNodeWithNoPathSitsAtNothingAndAnUnpoweredNetworkIsDead() {
        Grid grid = new Grid(3);
        grid.resistor(0, 1, 1.0);
        grid.load(2, 3.0);
        grid.solve();
        for (int i = 0; i < 3; i++) assertEquals(0.0, grid.voltage(i), 1e-9);
    }

    @Test
    void aStrongerSourceChargesAWeakerOne() {
        Grid grid = new Grid(2);
        int dynamo = grid.source(0, 24.0, 0.5);
        int battery = grid.source(1, 20.0, 0.5);
        grid.resistor(0, 1, 1.0);
        grid.solve();
        assertEquals(2.0, grid.sourceCurrent(dynamo), 1e-6, "4 V over 2 ohms");
        assertEquals(-2.0, grid.sourceCurrent(battery), 1e-6, "the battery is being charged");
        assertTrue(grid.sourcePower(battery) < 0.0);
    }

    @Test
    void aWarmStartSettlesFaster() {
        int n = 400;
        Grid cold = chain(n);
        int coldIterations = cold.solve();
        Grid warm = chain(n);
        warm.warmStart(cold.voltages());
        int warmIterations = warm.solve();
        assertTrue(warmIterations < coldIterations / 4, warmIterations + " vs " + coldIterations);
    }

    private static Grid chain(int n) {
        Grid grid = new Grid(n);
        grid.source(0, 24.0, 0.5);
        for (int i = 0; i < n - 1; i++) grid.resistor(i, i + 1, 0.0168);
        grid.load(n - 1, 4.8);
        return grid;
    }
}
