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
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.item.ItemCastingMold;

/**
 * Smelter Outlet spout block that physically pours molten metal from an adjacent
 * Smelter vessel down into a Casting Table below.
 */
public final class BlockSmelterOutlet extends BlockContainer {

    @SideOnly(Side.CLIENT)
    private IIcon blockIconRefractory;

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
        return -1; // TileEntitySpecialRenderer
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        switch (meta) {
            case 2: // North (spout points North into z - 1)
                setBlockBounds(0.25F, 0.2F, 0.0F, 0.75F, 0.65F, 0.7F);
                break;
            case 3: // South (spout points South into z + 1)
                setBlockBounds(0.25F, 0.2F, 0.3F, 0.75F, 0.65F, 1.0F);
                break;
            case 4: // West (spout points West into x - 1)
                setBlockBounds(0.0F, 0.2F, 0.25F, 0.7F, 0.65F, 0.75F);
                break;
            case 5: // East (spout points East into x + 1)
                setBlockBounds(0.3F, 0.2F, 0.25F, 1.0F, 0.65F, 0.75F);
                break;
            default:
                setBlockBounds(0.25F, 0.2F, 0.25F, 0.75F, 0.65F, 0.75F);
                break;
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
        // Set facing away from the block the player clicked or toward player look
        int l = MathHelper.floor_double((double) (player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int facing = 2;
        if (l == 0) facing = 2; // North
        if (l == 1) facing = 5; // East
        if (l == 2) facing = 3; // South
        if (l == 3) facing = 4; // West

        world.setBlockMetadataWithNotify(x, y, z, facing, 2);
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
        ItemStack held = player.getHeldItem();

        // 1. Holding empty mold: pour directly into held mold from connected smelter
        if (held != null && held.getItem() instanceof ItemCastingMold) {
            if (!ItemCastingMold.isFilled(held)) {
                TileEntityStarterSmelter smelter = outlet.findConnectedSmelter();
                if (smelter == null) {
                    player.addChatMessage(
                        new ChatComponentText("\u00a7c[Outlet] No connected Starter Smelter found behind outlet."));
                    return true;
                }
                ItemStack singleMold = held.stackSize == 1 ? held : held.splitStack(1);
                if (smelter.pourIntoMold(singleMold)) {
                    if (singleMold != held) {
                        if (!player.inventory.addItemStackToInventory(singleMold)) {
                            player.dropPlayerItemWithRandomChoice(singleMold, false);
                        }
                    }
                    player.inventoryContainer.detectAndSendChanges();
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "liquid.lavapop", 1.0F, 1.0F);
                    player.addChatMessage(
                        new ChatComponentText(
                            "\u00a7a[Outlet] Poured molten metal directly into " + singleMold.getDisplayName()));
                    return true;
                } else {
                    if (singleMold != held) {
                        held.stackSize++;
                    }
                    player.addChatMessage(
                        new ChatComponentText("\u00a7c[Outlet] Smelter crucible has no molten liquid metal ready."));
                    return true;
                }
            }
        }

        // 2. Normal activation: Trigger physical pouring into casting table below
        return outlet.startPouring(player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        this.blockIconRefractory = reg.registerIcon("naturalexceptions:refractory_brick_block");
        this.blockIcon = this.blockIconRefractory;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return this.blockIconRefractory;
    }
}
