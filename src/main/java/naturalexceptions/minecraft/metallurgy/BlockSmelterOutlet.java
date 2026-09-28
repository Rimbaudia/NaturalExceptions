package naturalexceptions.minecraft.metallurgy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Smelter Outlet spout block that physically pours molten metal from an adjacent
 * Smelter vessel down into a Casting Table below.
 */
public final class BlockSmelterOutlet extends BlockContainer implements naturalexceptions.minecraft.block.PartModel {

    public BlockSmelterOutlet() {
        super(Material.rock);
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(Block.soundTypeStone);
        setBlockName("smelterOutlet");
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntitySmelterOutlet();
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return naturalexceptions.minecraft.block.PartModel.RenderType.id;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        setGeometryBounds(SmelterOutletGeometry.bounds(world.getBlockMetadata(x, y, z)));
    }

    private void setGeometryBounds(double[] b) {
        setBlockBounds((float) b[0], (float) b[1], (float) b[2], (float) b[3], (float) b[4], (float) b[5]);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setGeometryBounds(SmelterOutletGeometry.bounds(2));
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, java.util.List boxes,
        net.minecraft.entity.Entity entity) {
        for (double[] b : SmelterOutletGeometry.parts(world.getBlockMetadata(x, y, z))) {
            AxisAlignedBB box = AxisAlignedBB
                .getBoundingBox(x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5]);
            if (mask.intersectsWith(box)) boxes.add(box);
        }
    }

    private boolean attached(World world, int x, int y, int z, int facing) {
        return SmelterPortLookup.findOutletController(world, x, y, z, facing) != null;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        if (!super.canPlaceBlockAt(world, x, y, z)) return false;
        for (int facing = 2; facing <= 5; facing++) {
            if (attached(world, x, y, z, facing)) return true;
        }
        return false;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
        int metadata) {
        if (side >= 2 && attached(world, x, y, z, side)) return side;
        for (int facing = 2; facing <= 5; facing++) {
            if (attached(world, x, y, z, facing)) return facing;
        }
        return 2;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!world.isRemote && !attached(world, x, y, z, world.getBlockMetadata(x, y, z))) {
            dropBlockAsItem(world, x, y, z, 0, 0);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
        int facing = world.getBlockMetadata(x, y, z);
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntitySmelterOutlet) {
            ((TileEntitySmelterOutlet) te).setFacing(facing);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntitySmelterOutlet)) {
            return false;
        }
        TileEntitySmelterOutlet outlet = (TileEntitySmelterOutlet) te;
        if (world.isRemote) return true;
        ItemStack held = player.getHeldItem();

        // 0. Holding Foundry Ladle: scoop molten metal from outlet spout or pour back
        if (held != null && held.getItem() instanceof naturalexceptions.minecraft.item.ItemFoundryLadle) {
            TileEntityStarterSmelter smelter = outlet.findConnectedSmelter();
            if (smelter == null) {
                if (!world.isRemote) {
                    player.addChatMessage(
                        new ChatComponentText(
                            "\u00a7c[Outlet] This outlet is not set into the wall of a Starter Smelter."));
                }
                return true;
            }
            if (player.isSneaking() && naturalexceptions.minecraft.item.ItemFoundryLadle.hasMetal(held)) {
                // Return molten metal from ladle back through spout into crucible
                naturalexceptions.core.metallurgy.MaterialQuantity mass = naturalexceptions.minecraft.item.ItemFoundryLadle
                    .getFilledMass(held);
                naturalexceptions.core.metallurgy.AggregateLayer drained = naturalexceptions.minecraft.item.ItemFoundryLadle
                    .drainMetal(held, mass);
                if (drained != null) {
                    smelter.vesselState()
                        .remelt(drained);
                    smelter.markDirty();
                    world.markBlockForUpdate(smelter.xCoord, smelter.yCoord, smelter.zCoord);
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "liquid.lavapop", 1.0F, 1.0F);
                    if (!world.isRemote) {
                        player.addChatMessage(
                            new ChatComponentText("\u00a7a[Outlet] Poured the ladle back into the smelter."));
                    }
                    return true;
                }
            } else {
                // Scoop / draw molten metal from smelter via outlet spout
                if (smelter.pourIntoLadle(held)) {
                    player.inventoryContainer.detectAndSendChanges();
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "liquid.lavapop", 1.0F, 1.0F);
                    if (!world.isRemote) {
                        naturalexceptions.core.metallurgy.MaterialQuantity mass = naturalexceptions.minecraft.item.ItemFoundryLadle
                            .getFilledMass(held);
                        player
                            .addChatMessage(new ChatComponentText("\u00a7a[Outlet] Drew molten metal into the ladle."));
                    }
                    return true;
                } else {
                    if (!world.isRemote) {
                        player.addChatMessage(
                            new ChatComponentText("\u00a7c[Outlet] There is no molten metal the ladle can reach yet."));
                    }
                    return true;
                }
            }
        }

        // Pouring into a mold is done by aligning a Casting Table beneath this spout.
        return outlet.startPouring(player);
    }

    @SideOnly(Side.CLIENT)
    private IIcon clayIcon;
    @SideOnly(Side.CLIENT)
    private IIcon channelIcon;

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        clayIcon = reg.registerIcon("naturalexceptions:smelter_outlet_clay");
        channelIcon = reg.registerIcon("naturalexceptions:smelter_outlet_channel");
        blockIcon = clayIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return clayIcon;
    }

    @Override
    public double[][] parts(int metadata) {
        return SmelterOutletGeometry.parts(metadata);
    }

    /** Flange and rims are fired clay; the channel floor and lip are sooted from the metal. */
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        return index == 1 || index == 4 ? channelIcon : clayIcon;
    }

    @Override
    public int inventoryMetadata() {
        return 2;
    }
}
