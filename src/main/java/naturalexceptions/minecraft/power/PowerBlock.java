package naturalexceptions.minecraft.power;

import net.minecraft.world.IBlockAccess;

/** A block that carries current: cables, a closed switch, a relay, a machine. */
public interface PowerBlock {

    /** Whether the block at (x, y, z) is part of a network just now (an open switch is not). */
    boolean conducts(IBlockAccess world, int x, int y, int z);

    /** The resistance of a metre through it (a cable's copper), or a contact's for anything else. */
    default double ohms(IBlockAccess world, int x, int y, int z) {
        return PowerNetworks.CONTACT_OHMS;
    }

    /** Whether it reaches other relays through the air. */
    default boolean relay() {
        return false;
    }
}
