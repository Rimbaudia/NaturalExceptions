package naturalexceptions.minecraft.power;

import naturalexceptions.core.power.Grid;

/** A machine's tile: what it puts into its network's solve, and what it does with the result. */
public interface PowerDevice {

    /** Adds itself to the solve: a source, a load, a battery, at its terminal {@code node}. */
    void stamp(Grid grid, int node);

    /** After the solve: read its voltage, its current, its power; {@code seconds} since the last. */
    void powered(Grid grid, int node, double seconds);

    /**
     * After a solve, before {@link #powered}: whether it must take itself out and have the network solved again (a
     * flat battery the solve had giving current). Asked once a solve.
     */
    default boolean withdraws(Grid grid) {
        return false;
    }
}
