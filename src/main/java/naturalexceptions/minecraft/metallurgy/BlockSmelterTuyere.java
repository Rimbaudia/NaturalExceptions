package naturalexceptions.minecraft.metallurgy;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.item.ItemBellows;

/** Directional air nozzle. Place against a vessel and pump its exposed end with bellows. */
public final class BlockSmelterTuyere extends Block {

    @SideOnly(Side.CLIENT)
    private IIcon nozzleIcon;

    public BlockSmelterTuyere() {
        super(Material.rock);
        setHardness(2.5F);
        setResistance(10.0F);
        setStepSound(Block.soundTypeStone);
        setBlockName("smelterTuyere");
        setBlockTextureName("naturalexceptions:smelter_tuyere_side");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon("naturalexceptions:smelter_tuyere_side");
        nozzleIcon = register.registerIcon("naturalexceptions:smelter_tuyere_nozzle");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return side == metadata ? nozzleIcon : blockIcon;
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.3F, 0.3F, 0.0F, 0.7F, 0.7F, 1.0F);
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
        int metadata) {
        return side >= 2 && side <= 5 ? side ^ 1 : 2;
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
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        int direction = world.getBlockMetadata(x, y, z);
        if (direction == 4 || direction == 5) setBlockBounds(0.0F, 0.3F, 0.3F, 1.0F, 0.7F, 0.7F);
        else setBlockBounds(0.3F, 0.3F, 0.0F, 0.7F, 0.7F, 1.0F);
    }

    public TileEntityStarterSmelter findAdjacentSmelter(World world, int x, int y, int z) {
        return SmelterPortLookup.findTuyereController(world, x, y, z);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (player.getHeldItem() != null && player.getHeldItem()
            .getItem() instanceof ItemBellows) {
            return ((ItemBellows) player.getHeldItem()
                .getItem()).onItemUse(player.getHeldItem(), player, world, x, y, z, side, hitX, hitY, hitZ);
        }
        if (!world.isRemote) player.addChatMessage(
            new ChatComponentText(
                findAdjacentSmelter(world, x, y, z) == null
                    ? "Place the tuyere against a horizontal vessel face so its nozzle points into the vessel."
                    : "Use bellows on the outer end for five seconds of forced draft. Requires burning fuel."));
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, net.minecraft.entity.EntityLivingBase placer,
        net.minecraft.item.ItemStack stack) {
        if (!world.isRemote && findAdjacentSmelter(world, x, y, z) == null && placer instanceof EntityPlayer) {
            ((EntityPlayer) placer).addChatMessage(
                new ChatComponentText(
                    "\u00a7cTuyere rejected: nozzle must point into a valid horizontal vessel wall."));
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }
}
