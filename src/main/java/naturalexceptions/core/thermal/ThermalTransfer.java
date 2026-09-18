package naturalexceptions.core.thermal;

import java.util.Objects;

/**
 * Machine-independent thermal transfer primitive.
 *
 * <p>
 * Truth invariants:
 * <ul>
 * <li>Transfer direction is strictly determined by temperature difference (hotter to colder).</li>
 * <li>Conservative: Energy removed from source identically matches energy added to destination.</li>
 * <li>Zero energy creation: Floating-point error is bounded; no energy is invented.</li>
 * <li>Equilibrium clamping: Elapsed time transfers never cause temperatures to overshoot past equilibrium.</li>
 * </ul>
 */
public final class ThermalTransfer {

    private ThermalTransfer() {}

    /**
     * Executes an exact, conservative transfer of thermal energy from one reservoir to another.
     *
     * @return the actual amount of energy transferred in Joules.
     */
    public static double transferConservative(ThermalReservoir from, ThermalReservoir to, double requestedJoules) {
        Objects.requireNonNull(from, "from reservoir cannot be null");
        Objects.requireNonNull(to, "to reservoir cannot be null");
        if (requestedJoules <= 0.0) {
            return 0.0;
        }

        double available = from.isInfinite() ? requestedJoules : from.currentEnergyJoules();
        double transfer = Math.min(requestedJoules, Math.max(0.0, available));

        if (transfer > 0.0) {
            from.removeEnergyJoules(transfer);
            to.addEnergyJoules(transfer);
        }
        return transfer;
    }

    /**
     * Computes and executes natural heat conduction between two reservoirs over an elapsed time delta.
     *
     * @param bodyA                     first reservoir
     * @param bodyB                     second reservoir
     * @param conductanceWattsPerKelvin thermal conductance G = k * A / L (W / K)
     * @param deltaSeconds              elapsed time in seconds
     * @return energy transferred in Joules
     */
    public static double exchangeElapsed(ThermalReservoir bodyA, ThermalReservoir bodyB,
        double conductanceWattsPerKelvin, double deltaSeconds) {
        Objects.requireNonNull(bodyA, "bodyA cannot be null");
        Objects.requireNonNull(bodyB, "bodyB cannot be null");
        if (conductanceWattsPerKelvin <= 0.0 || deltaSeconds <= 0.0) {
            return 0.0;
        }

        double tempA = bodyA.temperatureKelvin();
        double tempB = bodyB.temperatureKelvin();
        double deltaT = Math.abs(tempA - tempB);
        if (deltaT < 1.0e-9) {
            return 0.0;
        }

        ThermalReservoir hotter = tempA > tempB ? bodyA : bodyB;
        ThermalReservoir colder = tempA > tempB ? bodyB : bodyA;

        double heatFlowWatts = conductanceWattsPerKelvin * deltaT;
        double requestedJoules = heatFlowWatts * deltaSeconds;

        // Calculate maximum equilibrating energy to avoid numerical temperature overshoot
        double maxEquilibratingJoules = computeMaxEquilibratingJoules(hotter, colder);
        double boundedJoules = Math.min(requestedJoules, maxEquilibratingJoules);

        return transferConservative(hotter, colder, boundedJoules);
    }

    private static double computeMaxEquilibratingJoules(ThermalReservoir hotter, ThermalReservoir colder) {
        double tHot = hotter.temperatureKelvin();
        double tCold = colder.temperatureKelvin();
        double deltaT = tHot - tCold;

        if (hotter.isInfinite() && colder.isInfinite()) {
            return Double.MAX_VALUE;
        }

        if (hotter.isInfinite() && colder instanceof FiniteThermalBody) {
            FiniteThermalBody finiteCold = (FiniteThermalBody) colder;
            double energyAtHotTemp = ThermodynamicModel.energyForTemperature(
                tHot,
                finiteCold.phase() == naturalexceptions.core.material.MaterialPhase.LIQUID ? 1.0 : 0.0,
                finiteCold.massKg(),
                finiteCold.properties());
            return Math.max(0.0, energyAtHotTemp - finiteCold.currentEnergyJoules());
        }

        if (colder.isInfinite() && hotter instanceof FiniteThermalBody) {
            FiniteThermalBody finiteHot = (FiniteThermalBody) hotter;
            double energyAtColdTemp = ThermodynamicModel
                .energyForTemperature(tCold, 0.0, finiteHot.massKg(), finiteHot.properties());
            return Math.max(0.0, finiteHot.currentEnergyJoules() - energyAtColdTemp);
        }

        if (hotter instanceof FiniteThermalBody && colder instanceof FiniteThermalBody) {
            FiniteThermalBody h = (FiniteThermalBody) hotter;
            FiniteThermalBody c = (FiniteThermalBody) colder;
            double cHot = h.massKg() * h.properties()
                .specificHeatSolid();
            double cCold = c.massKg() * c.properties()
                .specificHeatSolid();
            double cTotal = cHot + cCold;
            if (cTotal <= 0.0) return 0.0;
            double tEq = (cHot * tHot + cCold * tCold) / cTotal;
            return Math.max(0.0, cHot * (tHot - tEq));
        }

        return deltaT * 1000.0; // Fallback bound
    }
}
