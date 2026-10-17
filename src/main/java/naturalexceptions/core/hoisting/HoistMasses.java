package naturalexceptions.core.hoisting;

/**
 * What things weigh on a hoist's rope, in kilograms: game weights, not real ones (a real block of stone is two and a
 * half tonnes), set so that a crank lifts a rider with a few blocks of stone and a counterweight of two or three
 * blocks balances a rider.
 */
public final class HoistMasses {

    public static final double BASKET = 30.0;
    public static final double COUNTERWEIGHT_BOX = 20.0;
    public static final double RIDER = 80.0;
    /** A block carried as cargo, or loaded in a counterweight box. */
    public static final double BLOCK = 40.0;
    /** Any other item. */
    public static final double ITEM = 1.0;

    private HoistMasses() {}

    /** A basket with {@code riders} in it and cargo of {@code blocks} blocks and {@code items} other items. */
    public static double basket(int riders, int blocks, int items) {
        return BASKET + riders * RIDER + blocks * BLOCK + items * ITEM;
    }

    /** A counterweight box holding {@code blocks} blocks. */
    public static double counterweight(int blocks) {
        return COUNTERWEIGHT_BOX + blocks * BLOCK;
    }
}
