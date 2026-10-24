package naturalexceptions.minecraft.power;

/** A machine that can tell a Multimeter what it is doing. */
public interface PowerReadout {

    /** One line: what it sees and does (volts, amps, watts, its state). */
    String readout();
}
