package naturalexceptions.minecraft.furniture;

/** A table: four places on its top, one item each, set down and taken up by hand. */
public final class TileEntityWoodenTable extends TileEntityStorage {

    public TileEntityWoodenTable() {
        super(4, "container.naturalexceptions.table");
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }
}
