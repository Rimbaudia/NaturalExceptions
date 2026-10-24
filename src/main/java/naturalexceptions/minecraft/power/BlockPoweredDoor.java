package naturalexceptions.minecraft.power;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.power.Grid;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A Powered Door: an iron door whose bolts are held back by a solenoid. It stands open while its network gives it
 * power and swings shut, locked, when the power stops (a knife switch is its key). Hands and redstone do not move it.
 */
public final class BlockPoweredDoor extends BlockDoor implements ITileEntityProvider, PowerBlock {

    public BlockPoweredDoor() {
        super(Material.iron);
        setBlockName("poweredDoor");
        setBlockTextureName("naturalexceptions:powered_door");
        setHardness(5.0F);
        setStepSound(soundTypeMetal);
        disableStats();
    }

    static boolean lower(int meta) {
        return (meta & 8) == 0;
    }

    @Override
    public boolean conducts(IBlockAccess world, int x, int y, int z) {
        return lower(world.getBlockMetadata(x, y, z));
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return lower(meta);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return lower(meta) ? new Tile() : null;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy,
        float hz) {
        return false;
    }

    /** Only its own halves and footing matter to it: not redstone. */
    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        int meta = world.getBlockMetadata(x, y, z);
        if (lower(meta)) {
            boolean broken = world.getBlock(x, y + 1, z) != this
                || !World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
            if (broken) {
                world.setBlockToAir(x, y, z);
                if (world.getBlock(x, y + 1, z) == this) world.setBlockToAir(x, y + 1, z);
                if (!world.isRemote) dropBlockAsItem(world, x, y, z, meta, 0);
            }
        } else if (world.getBlock(x, y - 1, z) != this) world.setBlockToAir(x, y, z);
        PowerNetworks.dirty(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        PowerNetworks.dirty(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
        world.removeTileEntity(x, y, z);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return lower(meta) ? NEItems.poweredDoor : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return NEItems.poweredDoor;
    }

    /** Its solenoid: a load that holds the bolts back while it draws enough. */
    public static final class Tile extends TileSource {

        public static final double OHMS = 12.0, HOLD_WATTS = 25.0;
        private int load = -1;
        private double watts;

        @Override
        public void stamp(Grid grid, int node) {
            load = grid.load(node, OHMS);
        }

        @Override
        public void powered(Grid grid, int node, double seconds) {
            watts = load >= 0 ? grid.power(load) : 0.0;
            boolean open = watts >= HOLD_WATTS;
            Block block = worldObj.getBlock(xCoord, yCoord, zCoord);
            if (!(block instanceof BlockPoweredDoor)) return;
            BlockPoweredDoor door = (BlockPoweredDoor) block;
            if (door.func_150015_f(worldObj, xCoord, yCoord, zCoord) != open)
                door.func_150014_a(worldObj, xCoord, yCoord, zCoord, open);
        }

        @Override
        public String readout() {
            return String.format(
                "Powered Door: %s (%.0f W; it needs %.0f to open)",
                watts >= HOLD_WATTS ? "open" : "shut and locked",
                watts,
                HOLD_WATTS);
        }
    }

    /** The door as an item: set down like an iron door. */
    public static final class ItemPoweredDoorPlacer extends Item {

        public ItemPoweredDoorPlacer() {
            setUnlocalizedName("poweredDoor");
            setTextureName("naturalexceptions:powered_door_item");
        }

        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hx, float hy, float hz) {
            if (side != 1) return false;
            y++;
            Block block = naturalexceptions.minecraft.registry.NEBlocks.poweredDoor;
            if (block == null || !player.canPlayerEdit(x, y, z, side, stack)
                || !player.canPlayerEdit(x, y + 1, z, side, stack)) return false;
            if (!block.canPlaceBlockAt(world, x, y, z)) return false;
            int facing = MathHelper.floor_double((player.rotationYaw + 180.0F) * 4.0F / 360.0F - 0.5D) & 3;
            ItemDoor.placeDoorBlock(world, x, y, z, facing, block);
            stack.stackSize--;
            return true;
        }
    }
}
