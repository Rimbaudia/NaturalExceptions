package naturalexceptions.minecraft.entity;

/** What an Automaton's model needs to know of it: whether it stands dormant, whether its shield is up, whose it is. */
public interface AutomatonPose {

    boolean dormant();

    boolean shieldUp();

    /** A player's own (repaired) Automaton glows green rather than the city's blue. */
    default boolean owned() {
        return false;
    }
}
