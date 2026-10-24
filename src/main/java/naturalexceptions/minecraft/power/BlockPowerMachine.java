package naturalexceptions.minecraft.power;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.block.PartModel;

/** The first machines on a network, each drawn as a few boxes: the Crank Dynamo and the Arc Lamp. */
public abstract class BlockPowerMachine extends BlockContainer implements PowerBlock, PartModel {

    static final double P = 1.0 / 16.0;

    protected BlockPowerMachine(Material material, String name, String texture) {
        super(material);
        setBlockName(name);
        setBlockTextureName(texture);
    }

    @Override
    public boolean conducts(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    protected static double[][] scaled(double[][] pixels) {
        double[][] out = new double[pixels.length][];
        for (int i = 0; i < pixels.length; i++) {
            double[] b = pixels[i];
            out[i] = new double[] { b[0] * P, b[1] * P, b[2] * P, b[3] * P, b[4] * P, b[5] * P };
        }
        return out;
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    public int getRenderType() {
        return PartModel.RenderType.id;
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
        double[] b = PartModel.bounds(parts(world.getBlockMetadata(x, y, z)));
        setBlockBounds((float) b[0], (float) b[1], (float) b[2], (float) b[3], (float) b[4], (float) b[5]);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List boxes,
        Entity entity) {
        double[] b = PartModel.bounds(parts(world.getBlockMetadata(x, y, z)));
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5]);
        if (mask.intersectsWith(box)) boxes.add(box);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        PowerNetworks.dirty(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    /** A crank dynamo: an iron frame and base, a drum of copper windings, a crank and its handle. */
    public static final class CrankDynamo extends BlockPowerMachine {

        private static final double[][] PARTS = scaled(
            new double[][] { { 2, 0, 3, 14, 2, 13 }, { 3, 2, 4, 5, 10, 12 }, { 11, 2, 4, 13, 10, 12 },
                { 5, 3, 5, 11, 9, 11 }, { 13, 5.5, 7, 15, 7.5, 9 }, { 14, 7.5, 7, 15.5, 12, 9 } });
        @SideOnly(Side.CLIENT)
        private IIcon windings;

        public CrankDynamo() {
            super(Material.iron, "crankDynamo", "naturalexceptions:cable_copper");
            setHardness(2.0F);
            setStepSound(soundTypeMetal);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileCrankDynamo();
        }

        /** Hold use on it to turn it. */
        @Override
        public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx,
            float hy, float hz) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!world.isRemote && tile instanceof TileCrankDynamo) ((TileCrankDynamo) tile).crank(player);
            player.swingItem();
            return true;
        }

        @Override
        public double[][] parts(int meta) {
            return PARTS;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister register) {
            super.registerBlockIcons(register);
            windings = blockIcon;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon partIcon(int index, int meta) {
            if (index == 3) return windings;
            if (index == 5) return Blocks.planks.getIcon(0, 1);
            return Blocks.iron_block.getIcon(2, 0);
        }
    }

    /** A windmill's mast and nacelle (its sails are drawn turning, by its renderer); meta: the way its sails face. */
    public static final class Windmill extends BlockPowerMachine {

        /** Base, mast, nacelle, and the hub's stub; authored with its sails to the north (-Z). */
        private static final double[][] PARTS = scaled(
            new double[][] { { 5, 0, 5, 11, 1, 11 }, { 7, 1, 7, 9, 8, 9 }, { 5, 8, 3, 11, 13, 13 },
                { 6.5, 9, 1, 9.5, 12, 3 } });

        public Windmill() {
            super(Material.wood, "windmill", "planks_spruce");
            setHardness(2.0F);
            setStepSound(soundTypeWood);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileWindmill();
        }

        @Override
        public void onBlockPlacedBy(World world, int x, int y, int z, net.minecraft.entity.EntityLivingBase placer,
            net.minecraft.item.ItemStack stack) {
            int quarter = net.minecraft.util.MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5) & 3;
            // Its sails face away from whoever set it, into the open.
            int[] facing = { 3, 4, 2, 5 };
            world.setBlockMetadataWithNotify(x, y, z, facing[quarter], 2);
        }

        @Override
        public double[][] parts(int meta) {
            int facing = meta >= 2 && meta <= 5 ? meta : 2;
            double[][] out = new double[PARTS.length][];
            for (int i = 0; i < PARTS.length; i++) out[i] = PartModel.rotate(PARTS[i], facing);
            return out;
        }

        @Override
        public int inventoryMetadata() {
            return 2;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon partIcon(int index, int meta) {
            return index == 2 ? Blocks.planks.getIcon(0, 1) : Blocks.iron_block.getIcon(2, 0);
        }
    }

    /** A socket for a Stormglass heart: an iron base, a copper ring, a glass dome with the heart in it. */
    public static final class HeartSocket extends BlockPowerMachine {

        private static final double[][] PARTS = scaled(
            new double[][] { { 2, 0, 2, 14, 4, 14 }, { 4, 4, 4, 12, 5, 12 }, { 5, 5, 5, 11, 11, 11 },
                { 6.5, 6, 6.5, 9.5, 10, 9.5 } });
        private static final double[][] EMPTY = new double[][] { PARTS[0], PARTS[1], PARTS[2], { 0, 0, 0, 0, 0, 0 } };
        @SideOnly(Side.CLIENT)
        private IIcon heart;

        public HeartSocket() {
            super(Material.iron, "heartSocket", "naturalexceptions:cable_copper");
            setHardness(1.5F);
            setStepSound(soundTypeMetal);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileHeartSocket();
        }

        @Override
        public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx,
            float hy, float hz) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileHeartSocket)) return false;
            if (world.isRemote) return true;
            return ((TileHeartSocket) tile).use(player);
        }

        @Override
        public int getLightValue(IBlockAccess world, int x, int y, int z) {
            return world.getBlock(x, y, z) == this && world.getBlockMetadata(x, y, z) == 1 ? 6 : 0;
        }

        @Override
        public double[][] parts(int meta) {
            return meta == 1 ? PARTS : EMPTY;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister register) {
            super.registerBlockIcons(register);
            heart = register.registerIcon("naturalexceptions:stormglass_lantern_core");
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon partIcon(int index, int meta) {
            if (index == 0) return Blocks.iron_block.getIcon(2, 0);
            if (index == 1) return blockIcon;
            if (index == 2) return Blocks.glass.getIcon(0, 0);
            return heart;
        }
    }

    /** A Storm Cell: a glass jar under a brass cap, its Stormglass core glowing brighter the fuller it is. */
    public static final class StormCell extends BlockPowerMachine {

        private static final double[][] PARTS = scaled(
            new double[][] { { 3, 0, 3, 13, 13, 13 }, { 2, 13, 2, 14, 15, 14 }, { 6, 1, 6, 10, 12, 10 } });
        public static final int[] LIGHT = { 0, 4, 8, 12 };
        @SideOnly(Side.CLIENT)
        private IIcon core;

        public StormCell() {
            super(Material.glass, "stormCell", "naturalexceptions:stormglass_lantern_core");
            setHardness(1.0F);
            setStepSound(soundTypeGlass);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileStormCell();
        }

        @Override
        public int getLightValue(IBlockAccess world, int x, int y, int z) {
            return world.getBlock(x, y, z) == this ? LIGHT[world.getBlockMetadata(x, y, z) & 3] : 0;
        }

        @Override
        public double[][] parts(int meta) {
            return PARTS;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister register) {
            super.registerBlockIcons(register);
            core = blockIcon;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon partIcon(int index, int meta) {
            if (index == 0) return Blocks.glass.getIcon(0, 0);
            if (index == 1) return Blocks.gold_block.getIcon(1, 0);
            return (meta & 3) > 0 ? core : Blocks.obsidian.getIcon(0, 0);
        }
    }

    /** A Lightning Spire: a Stormglass crystal on an iron rod. */
    public static final class LightningSpire extends BlockPowerMachine {

        private static final double[][] PARTS = scaled(
            new double[][] { { 5, 0, 5, 11, 1, 11 }, { 7, 1, 7, 9, 12, 9 }, { 6, 12, 6, 10, 16, 10 } });

        public LightningSpire() {
            super(Material.iron, "lightningSpire", "naturalexceptions:stormglass_lantern_core");
            setHardness(3.0F);
            setLightLevel(0.35F);
            setStepSound(soundTypeMetal);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileLightningSpire();
        }

        @Override
        public double[][] parts(int meta) {
            return PARTS;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon partIcon(int index, int meta) {
            return index == 2 ? blockIcon : Blocks.iron_block.getIcon(2, 0);
        }
    }

    /** An Electric Heater: refractory brick round a copper coil that glows with what it draws. A full block. */
    public static final class ElectricHeater extends BlockPowerMachine {

        private static final double[][] PARTS = { { 0, 0, 0, 1, 1, 1 } };
        @SideOnly(Side.CLIENT)
        private IIcon cold, warm, hot;

        public ElectricHeater() {
            super(Material.rock, "electricHeater", "naturalexceptions:electric_heater");
            setHardness(3.0F);
            setStepSound(soundTypeStone);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileElectricHeater();
        }

        @Override
        public boolean isOpaqueCube() {
            return true;
        }

        @Override
        public boolean renderAsNormalBlock() {
            return true;
        }

        @Override
        public int getRenderType() {
            return 0;
        }

        @Override
        public int getLightValue(IBlockAccess world, int x, int y, int z) {
            return world.getBlock(x, y, z) == this ? (world.getBlockMetadata(x, y, z) & 3) * 5 : 0;
        }

        @Override
        public double[][] parts(int meta) {
            return PARTS;
        }

        @Override
        public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
            setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister register) {
            cold = register.registerIcon("naturalexceptions:electric_heater");
            warm = register.registerIcon("naturalexceptions:electric_heater_warm");
            hot = register.registerIcon("naturalexceptions:electric_heater_hot");
            blockIcon = cold;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getIcon(int side, int meta) {
            int glow = meta & 3;
            return glow >= 2 ? hot : glow == 1 ? warm : cold;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon partIcon(int index, int meta) {
            return getIcon(0, meta);
        }
    }

    /** An arc lamp: a brass foot, a glass globe with the arc in it (dark, dim or blazing), a cap. */
    public static final class ArcLamp extends BlockPowerMachine {

        private static final double[][] PARTS = scaled(
            new double[][] { { 4, 0, 4, 12, 2, 12 }, { 4.5, 2, 4.5, 11.5, 10, 11.5 }, { 5, 10, 5, 11, 11, 11 },
                { 7, 11, 7, 9, 13, 9 } });
        @SideOnly(Side.CLIENT)
        private IIcon dark, lit;

        public ArcLamp() {
            super(Material.glass, "arcLamp", "naturalexceptions:arc_lamp_lit");
            setHardness(0.8F);
            setStepSound(soundTypeGlass);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileArcLamp();
        }

        @Override
        public int getLightValue(IBlockAccess world, int x, int y, int z) {
            if (world.getBlock(x, y, z) != this) return super.getLightValue(world, x, y, z);
            return TileArcLamp.LIGHT[Math.min(3, world.getBlockMetadata(x, y, z) & 3)];
        }

        @Override
        public double[][] parts(int meta) {
            return PARTS;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister register) {
            lit = register.registerIcon("naturalexceptions:arc_lamp_lit");
            dark = register.registerIcon("naturalexceptions:arc_lamp_dark");
            blockIcon = dark;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon partIcon(int index, int meta) {
            if (index == 1) return (meta & 3) > 0 ? lit : dark;
            return Blocks.gold_block.getIcon(1, 0);
        }
    }
}
