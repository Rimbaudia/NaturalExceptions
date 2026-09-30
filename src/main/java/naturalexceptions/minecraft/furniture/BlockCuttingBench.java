package naturalexceptions.minecraft.furniture;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.NaturalExceptions;
import naturalexceptions.minecraft.gui.NEGuiHandler;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A bench that works one thing into many, one piece at a time, with none of the crafting grid's waste
 * ({@link CuttingRecipes}): the Mason's Bench, a stone workbench with a block on it waiting for the chisel, dresses
 * Skystone; the Sawmill, a trestle with its blade standing up through the bed, cuts logs; the Loom, its warp strung
 * between two posts, weaves wool; the Potter's Wheel throws clay; the Glassblower's Furnace, a brick furnace with its
 * blowpipe, blows sand into glass as long as it has fuel.
 */
public final class BlockCuttingBench extends BlockParts {

    private static final double[][] MASON = { { 0.0, 0.75, 0.0, 1.0, 0.9375, 1.0 },
        { 0.0625, 0.0, 0.0625, 0.9375, 0.75, 0.9375 }, { 0.2, 0.9375, 0.25, 0.6, 1.25, 0.65 },
        { 0.7, 0.9375, 0.3, 0.78, 1.0, 0.8 } };
    private static final double[][] SAW = { { 0.0, 0.625, 0.125, 1.0, 0.8125, 0.875 },
        { 0.0625, 0.0, 0.1875, 0.25, 0.625, 0.375 }, { 0.75, 0.0, 0.1875, 0.9375, 0.625, 0.375 },
        { 0.0625, 0.0, 0.625, 0.25, 0.625, 0.8125 }, { 0.75, 0.0, 0.625, 0.9375, 0.625, 0.8125 },
        { 0.47, 0.5, 0.15, 0.53, 1.2, 0.85 }, { 0.0, 0.8125, 0.55, 0.4, 1.0625, 0.8 } };

    private static final double[][] LOOM = { { 0.0, 0.0, 0.2, 0.1, 1.1, 0.8 }, { 0.9, 0.0, 0.2, 1.0, 1.1, 0.8 },
        { 0.0, 1.0, 0.35, 1.0, 1.1, 0.65 }, { 0.1, 0.25, 0.45, 0.9, 1.0, 0.55 }, { 0.1, 0.5, 0.1, 0.9, 0.6, 0.25 } };
    private static final double[][] POTTER = { { 0.1, 0.0, 0.1, 0.9, 0.5, 0.9 }, { 0.45, 0.5, 0.45, 0.55, 0.75, 0.55 },
        { 0.2, 0.75, 0.2, 0.8, 0.8125, 0.8 }, { 0.35, 0.8125, 0.35, 0.65, 1.1, 0.65 } };
    private static final double[][] GLASS = { { 0.0, 0.0, 0.0, 1.0, 0.875, 1.0 }, { 0.3, 0.875, 0.3, 0.7, 1.3, 0.7 },
        { 0.8, 0.875, 0.45, 1.1, 0.95, 0.55 } };
    private static final double[][][] MODELS = { MASON, SAW, LOOM, POTTER, GLASS };

    /** Each kind's name, as a block and on its screen. */
    static final String[] BLOCK_NAMES = { "masonsBench", "sawmill", "loom", "pottersWheel", "glassFurnace" };
    static final String[] NAMES = new String[BLOCK_NAMES.length];
    /** The sound of a piece being worked at each kind. */
    static final String[] SOUNDS = { "dig.stone", "step.wood", "step.cloth", "dig.gravel", "fire.fire" };

    static {
        for (int i = 0; i < BLOCK_NAMES.length; i++) NAMES[i] = "tile." + BLOCK_NAMES[i] + ".name";
    }

    final CuttingRecipes.Kind kind;

    public BlockCuttingBench(CuttingRecipes.Kind kind) {
        super(stone(kind) ? Material.rock : Material.wood);
        this.kind = kind;
        setBlockName(BLOCK_NAMES[kind.ordinal()]);
        setBlockTextureName(stone(kind) ? "stonebrick" : "planks_spruce");
        setHardness(2.5F);
        setResistance(8.0F);
        setStepSound(stone(kind) ? soundTypePiston : soundTypeWood);
        setHarvestLevel(stone(kind) ? "pickaxe" : "axe", 0);
        if (kind.fired()) setLightLevel(0.6F);
    }

    private static boolean stone(CuttingRecipes.Kind kind) {
        return kind == CuttingRecipes.Kind.MASON || kind == CuttingRecipes.Kind.GLASS;
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] src = MODELS[kind.ordinal()];
        double[][] out = new double[src.length][];
        for (int i = 0; i < src.length; i++) out[i] = src[i].clone();
        return out;
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (kind == CuttingRecipes.Kind.GLASS) return Blocks.brick_block.getIcon(side, 0);
        if (kind != CuttingRecipes.Kind.MASON) return Blocks.planks.getIcon(side, 1);
        return NEBlocks.skystoneDecor != null ? NEBlocks.skystoneDecor.getIcon(side, 2)
            : Blocks.stonebrick.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        if (kind == CuttingRecipes.Kind.LOOM) return index == 3 ? Blocks.wool.getIcon(2, 0) : getIcon(1, 0);
        if (kind == CuttingRecipes.Kind.POTTER) {
            if (index == 1) return Blocks.iron_block.getIcon(2, 0);
            if (index == 2) return Blocks.stone.getIcon(1, 0);
            if (index == 3) return Blocks.clay.getIcon(2, 0);
            return getIcon(1, 0);
        }
        if (kind == CuttingRecipes.Kind.GLASS) {
            if (index == 2) return Blocks.iron_block.getIcon(2, 0);
            return getIcon(1, 0);
        }
        if (kind == CuttingRecipes.Kind.SAW) {
            if (index == 0) return Blocks.planks.getIcon(1, 1);
            if (index == 5) return Blocks.iron_block.getIcon(2, 0);
            if (index == 6) return Blocks.log.getIcon(2, 0);
            return Blocks.log.getIcon(2, 1);
        }
        switch (index) {
            case 0:
                return getIcon(1, 0);
            case 1:
                return NEBlocks.skystoneBricks != null ? NEBlocks.skystoneBricks.getIcon(2, 0)
                    : Blocks.stonebrick.getIcon(2, 0);
            case 2:
                return NEBlocks.skystoneRock != null ? NEBlocks.skystoneRock.getIcon(2, 0) : Blocks.stone.getIcon(2, 0);
            default:
                return Blocks.iron_block.getIcon(2, 0);
        }
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityCuttingBench(kind);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!world.isRemote) player.openGui(NaturalExceptions.instance, NEGuiHandler.GUI_CUTTING, world, x, y, z);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityCuttingBench) || ((TileEntityCuttingBench) tile).getStackInSlot(0) == null)
            return;
        // Dust from the work waiting on it: stone chips, or sawdust.
        if (kind.fired()) {
            world.spawnParticle("smoke", x + 0.5, y + 1.35, z + 0.5, 0.0, 0.05, 0.0);
            world.spawnParticle("flame", x + 0.5, y + 0.4, z + 0.02, 0.0, 0.0, 0.0);
            return;
        }
        world.spawnParticle(
            kind == CuttingRecipes.Kind.SAW ? "townaura" : "snowshovel",
            x + 0.3 + random.nextDouble() * 0.4,
            y + 1.05,
            z + 0.3 + random.nextDouble() * 0.4,
            0.0,
            0.02,
            0.0);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityStorage && !world.isRemote)
            ((TileEntityStorage) tile).dropContents(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }
}
