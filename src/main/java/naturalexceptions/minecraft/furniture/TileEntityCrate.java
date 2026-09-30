package naturalexceptions.minecraft.furniture;

/** A crate: a chest's worth of room that keeps its contents when it is picked up and carried. */
public final class TileEntityCrate extends TileEntityStorage {

    /** Set when a player breaks it: its contents leave packed in the crate item, not spilled. */
    boolean harvested;

    public TileEntityCrate() {
        super(27, "container.naturalexceptions.crate");
    }
}
