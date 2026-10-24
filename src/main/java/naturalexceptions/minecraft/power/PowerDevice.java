package naturalexceptions.minecraft.power;

import naturalexceptions.core.power.Grid;

/** A machine's tile: what it puts into its network's solve, and what it does with the result. */
public interface PowerDevice {

    /** Adds itself to the solve: a source, a load, a battery, at its terminal {@code node}. */
    void stamp(Grid grid, int node);

    /** After the solve: read its voltage, its current, its power; {@code seconds} since the last. */
    void powered(Grid grid, int node, double seconds);
}
