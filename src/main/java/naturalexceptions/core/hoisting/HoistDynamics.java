package naturalexceptions.core.hoisting;

/**
 * The rope of a hoist, tick by tick: how much is paid out from the sheave down to the load, and how fast it runs.
 * Distances are in blocks (a block is a metre), speeds in blocks a tick, masses in kilograms; a tick is 1/20 s.
 * <p>
 * The load hangs on the rope under the sheave; a counterweight, if there is one, hangs on its other end. The heavier
 * side wants to go down. A crank (a player at the windlass, or a rider hauling the hand-line) is a geared drive: it
 * pulls the rope towards its own speed, with no more than its force. The drum's friction slows everything.
 * <p>
 * The brake is a windlass's pawl and band: set, nothing runs down, but a crank still winds up past the pawl (the
 * ratchet clicks); and working the crank the other way eases the brake, letting the load down no faster than the
 * crank's own speed. Released, the heavier side runs, as fast as the governor lets it. The paid-out length stays
 * between the top (the load up under the sheave) and the bottom (the load set down on the floor, or the end of the
 * rope).
 * <p>
 * It is the same arithmetic on the server and the client, so a client that knows the state can run the rope on
 * between the server's words and arrive where the server does.
 */
public final class HoistDynamics {

    /** Metres a second squared. */
    public static final double GRAVITY = 9.81;
    /** Seconds a tick. */
    public static final double TICK = 0.05;
    /** The drum's own inertia, as a mass at the rope (kg). */
    public static final double DRUM_MASS = 40.0;
    /** The drum's friction (N per m/s of rope speed). */
    public static final double FRICTION = 260.0;
    /** The fastest the rope may run (blocks a tick): a governor on the drum, about 3 m/s. */
    public static final double MAX_SPEED = 0.15;
    /** Near either end the governor slows it, to this much speed (blocks a tick) per block still to go. */
    public static final double SLOWING = 0.12;
    /** The slowest it is slowed to, so that it still arrives. */
    public static final double CREEP = 0.01;

    /** How a hand works the rope: the speed it drives the rope at, and the most force it has. */
    public enum Crank {

        /** Nobody on it. */
        NONE(0.0, 0.0),
        /** A player at the windlass's crank: a geared crank, slow and strong. */
        WINDLASS(0.04, 2600.0),
        /** A rider hauling the hand-line from the basket: slower and weaker. */
        HAND_LINE(0.025, 1300.0);

        final double speed, force;

        Crank(double speed, double force) {
            this.speed = speed;
            this.force = force;
        }
    }

    private double paidOut;
    private double speed;

    public HoistDynamics(double paidOut) {
        this.paidOut = paidOut;
    }

    /** Rope paid out from the sheave to the load, in blocks. */
    public double paidOut() {
        return paidOut;
    }

    /** The rope's speed, in blocks a tick: positive as the load goes down. */
    public double speed() {
        return speed;
    }

    /** Sets the state outright: from the server's word, or a saved world. */
    public void set(double paidOut, double speed) {
        this.paidOut = paidOut;
        this.speed = speed;
    }

    /**
     * One tick.
     *
     * @param load    the load's mass (basket, rider, cargo)
     * @param counter the counterweight's mass, 0 for none
     * @param crank   who is working the rope, if anyone
     * @param winding which way they work it: -1 winding the load up, +1 letting it down, 0 not at all
     * @param brake   whether the brake is set (a ratchet: winding up still works, letting down is eased)
     * @param top     the least rope there may be out (the load up under the sheave)
     * @param bottom  the most (the load on the floor below it, or the end of the rope)
     * @return true if the rope moved
     */
    public boolean step(double load, double counter, Crank crank, int winding, boolean brake, double top,
        double bottom) {
        boolean working = crank != Crank.NONE && winding != 0;
        if (brake && !working) {
            speed = 0.0;
            clamp(top, bottom);
            return false;
        }
        double mass = load + counter + DRUM_MASS;
        double v = speed / TICK; // m/s
        double force = (load - counter) * GRAVITY - FRICTION * v;
        if (working) {
            // The crank pulls the rope towards its own speed, with no more than its force.
            double target = winding * crank.speed / TICK;
            double wanted = (target - v) * mass / TICK - force;
            force += Math.max(-crank.force, Math.min(crank.force, wanted));
        }
        v += force / mass * TICK;
        speed = Math.max(-MAX_SPEED, Math.min(MAX_SPEED, v * TICK));
        // Coming in to the floor or the top, it slows and sets down gently rather than stopping dead.
        if (speed > 0) speed = Math.min(speed, Math.max(CREEP, (bottom - paidOut) * SLOWING));
        else if (speed < 0) speed = Math.max(speed, -Math.max(CREEP, (paidOut - top) * SLOWING));
        if (brake) {
            // The pawl stops it running down unless it is being eased down, and then no faster than the crank turns.
            speed = winding < 0 ? Math.min(0.0, speed) : Math.max(0.0, Math.min(crank.speed, speed));
        }
        double before = paidOut;
        paidOut += speed;
        clamp(top, bottom);
        return paidOut != before;
    }

    /** Keeps the rope between its ends, stopping it at either. */
    private void clamp(double top, double bottom) {
        if (paidOut < top) {
            paidOut = top;
            speed = Math.max(0.0, speed);
        } else if (paidOut > bottom) {
            paidOut = bottom;
            speed = Math.min(0.0, speed);
        }
    }
}
