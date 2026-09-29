package naturalexceptions.minecraft.item;

import naturalexceptions.core.fabrication.GeometryId;

/** Test access to the mold sprite table from other packages. */
public final class ItemCastingMoldAccess {

    private ItemCastingMoldAccess() {}

    public static String[] spriteNames(GeometryId shape) {
        return ItemCastingMold.spriteNames(shape);
    }
}
