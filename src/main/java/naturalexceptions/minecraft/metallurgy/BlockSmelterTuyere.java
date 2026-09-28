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
public final class BlockSmelterTuyere extends Block implements naturalexceptions.minecraft.block.PartModel {

    /** Boxes with the vessel wall on the -Z side: collar, blast pipe, two bands, bellows port. */
    private static final double[][] PARTS = { { 0.25, 0.25, 0.0, 0.75, 0.75, 0.3125 },
        { 0.375, 0.375, 0.3125, 0.625, 0.625, 0.875 }, { 0.34375, 0.34375, 0.5, 0.65625, 0.65625, 0.5625 },
        { 0.34375, 0.34375, 0.75, 0.65625, 0.65625, 0.8125 }, { 0.3125, 0.3125, 0.875, 0.6875, 0.6875, 1.0 } };

    @SideOnly(Side.CLIENT)
    private IIcon collarIcon;
    @SideOnly(Side.CLIENT)
    private IIcon pipeIcon;
    @SideOnly(Side.CLIENT)
    private IIcon bandIcon;
    @SideOnly(Side.CLIENT)
    private IIcon portIcon;

    public BlockSmelterTuyere() {
        super(Material.rock);
        setHardness(2.5F);
        setResistance(10.0F);
        setStepSound(Block.soundTypeStone);
        setBlockName("smelterTuyere");
        setBlockTextureName("naturalexceptions:tuyere_pipe");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        collarIcon = register.registerIcon("naturalexceptions:tuyere_collar");
        pipeIcon = register.registerIcon("naturalexceptions:tuyere_pipe");
        bandIcon = register.registerIcon("naturalexceptions:tuyere_band");
        portIcon = register.registerIcon("naturalexceptions:tuyere_port");
        blockIcon = collarIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return pipeIcon;
    }

    @Override
    public double[][] parts(int metadata) {
        int facing = metadata >= 2 && metadata <= 5 ? metadata : 2;
        double[][] rotated = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++)
            rotated[i] = naturalexceptions.minecraft.block.PartModel.rotate(PARTS[i], facing);
        return rotated;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        switch (index) {
            case 0:
                return collarIcon;
            case 1:
                return pipeIcon;
            case 4:
                return portIcon;
            default:
                return bandIcon;
        }
    }

    @Override
    public int inventoryMetadata() {
        return 5;
    }

    @Override
    public int getRenderType() {
        return naturalexceptions.minecraft.block.PartModel.RenderType.id;
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    /** While the smelter it feeds is under forced draft, the port breathes a little flame and smoke. */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, java.util.Random random) {
        TileEntityStarterSmelter smelter = SmelterPortLookup.findTuyereController(world, x, y, z);
        if (smelter == null || smelter.getForcedDraftTicks() <= 0) return;
        double[] port = parts(world.getBlockMetadata(x, y, z))[4];
        double px = x + (port[0] + port[3]) / 2.0, py = y + (port[1] + port[4]) / 2.0,
            pz = z + (port[2] + port[5]) / 2.0;
        world.spawnParticle("smoke", px, py, pz, 0.0, 0.03, 0.0);
        if (random.nextInt(3) == 0) world.spawnParticle("flame", px, py, pz, 0.0, 0.01, 0.0);
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
        double[] b = naturalexceptions.minecraft.block.PartModel.bounds(parts(world.getBlockMetadata(x, y, z)));
        setBlockBounds((float) b[0], (float) b[1], (float) b[2], (float) b[3], (float) b[4], (float) b[5]);
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
