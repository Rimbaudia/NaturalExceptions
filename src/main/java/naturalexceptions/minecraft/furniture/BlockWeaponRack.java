package naturalexceptions.minecraft.furniture;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** A weapon rack: two uprights and two pegged rails against a wall, holding three weapons or tools upright. */
public final class BlockWeaponRack extends BlockWallFurniture {

    private static final double[][] PARTS = { { 0.0625, 0.0, 0.0, 0.1875, 1.0, 0.125 },
        { 0.8125, 0.0, 0.0, 0.9375, 1.0, 0.125 }, { 0.0, 0.0625, 0.0, 1.0, 0.1875, 0.25 },
        { 0.0, 0.75, 0.0, 1.0, 0.8125, 0.1875 } };

    public BlockWeaponRack() {
        super("weaponRack");
    }

    @Override
    double[][] authored() {
        return PARTS;
    }

    @Override
    int setsDown(ItemStack held) {
        return 1;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityWeaponRack();
    }
}
