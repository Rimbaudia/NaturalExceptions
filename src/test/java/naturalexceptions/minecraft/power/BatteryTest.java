package naturalexceptions.minecraft.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.power.Grid;

/** A Storm Cell runs a lamp down; a dynamo charges it part way; its voltage follows its charge. */
final class BatteryTest {

    @Test
    void aCellRunsDownIntoALoad() {
        TileStormCell cell = new TileStormCell();
        cell.addCharge(TileStormCell.CAPACITY / 2);
        double before = cell.fraction();
        Grid grid = new Grid(1);
        cell.stamp(grid, 0);
        grid.load(0, TileArcLamp.OHMS);
        grid.solve();
        cell.powered(grid, 0, 10.0);
        assertTrue(cell.fraction() < before, "it gave its charge to the lamp");
        assertTrue(grid.voltage(0) > 20.0, "a stiff cell holds its voltage under a lamp: " + grid.voltage(0));
    }

    @Test
    void aDynamoChargesAnEmptyCellPartWay() {
        TileStormCell cell = new TileStormCell();
        assertEquals(0.0, cell.fraction(), 1e-9);
        assertTrue(cell.emf() < TileCrankDynamo.EMF, "an empty cell sits below a dynamo's 24 V");
        Grid grid = new Grid(1);
        grid.source(0, TileCrankDynamo.EMF, TileCrankDynamo.INTERNAL_OHMS);
        cell.stamp(grid, 0);
        grid.solve();
        cell.powered(grid, 0, 60.0);
        assertTrue(cell.fraction() > 0.0, "the dynamo charged it");
        // Full, its EMF is above what a dynamo makes, so a dynamo alone never fills it.
        cell.addCharge(TileStormCell.CAPACITY);
        assertTrue(cell.emf() > TileCrankDynamo.EMF);
    }

    @Test
    void aFlatCellGivesNothingButStillTakesCharge() {
        TileStormCell cell = new TileStormCell();
        Grid grid = new Grid(1);
        cell.stamp(grid, 0);
        grid.load(0, TileArcLamp.OHMS);
        grid.solve();
        assertTrue(cell.withdraws(grid), "flat, and the solve had it lighting a lamp: it withdraws");
        Grid again = new Grid(1);
        cell.stamp(again, 0);
        assertTrue(!again.hasSources(), "and the second solve runs without it");
        // With a dynamo to charge it, it stays in.
        TileStormCell other = new TileStormCell();
        Grid charging = new Grid(1);
        charging.source(0, TileCrankDynamo.EMF, TileCrankDynamo.INTERNAL_OHMS);
        other.stamp(charging, 0);
        charging.solve();
        assertTrue(!other.withdraws(charging), "being charged, it stays");
    }

    @Test
    void anEmptySocketGivesNothing() {
        TileHeartSocket socket = new TileHeartSocket();
        Grid grid = new Grid(1);
        socket.stamp(grid, 0);
        assertTrue(!grid.hasSources(), "no heart, no source");
    }
}
