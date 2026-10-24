package naturalexceptions.minecraft.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A Citadel Automaton that ran down long ago and never got up: slumped against a wall, legs out and head on its
 * shoulder, or fallen to its knees with its head hanging. Its heart is dark and cracked. Broken up with a pickaxe it
 * gives up salvage: iron from its frame, Stormglass, and now and then a heart still whole, its halberd, or a crest.
 * Meta: bits 0-1 the way it faces (its back to the wall), bit 2 the pose ({@link #KNEELING}).
 */
public final class BlockBrokenAutomaton extends Block implements PartModel {

    public static final int KNEELING = 4;
    /** Salvage chances: a whole heart, its halberd, a sentry's crest. */
    public static final double HEART_CHANCE = 0.12, HALBERD_CHANCE = 0.04, CREST_CHANCE = 0.02;

    private static final double P = 1.0 / 16.0;
    private static final int PLATE = 0, FRAME = 1, HELM = 2, HEART = 3;

    /** Slumped against the wall behind it (on -Z), facing +Z, in pixels: box, then its look. */
    private static final double[][] SITTING = { { 4, 0, 3, 12, 4, 8 }, { 4, 0, 8, 7, 3, 15 }, { 9, 0, 8, 12, 3, 15 },
        { 3.5, 0, 14, 7.5, 4, 16 }, { 8.5, 0, 14, 12.5, 4, 16 }, { 3.5, 4, 1, 12.5, 13, 6 },
        { 6.5, 7.5, 6, 9.5, 10.5, 6.5 }, { 7.5, 11.5, 2, 12.5, 16.5, 7 }, { 1, 1, 2, 3.5, 12, 5 },
        { 9.5, 4, 6, 13, 6.5, 12 }, { 0.5, 11, 1.5, 4, 13, 5.5 }, { 13.5, 0, 1, 14.5, 1, 16 } };
    private static final int[] SITTING_LOOK = { FRAME, PLATE, PLATE, FRAME, FRAME, PLATE, HEART, HELM, FRAME, FRAME,
        PLATE, FRAME };
    /** Fallen to its knees, hunched forward over them, head hanging, arms down to the ground. */
    private static final double[][] KNEELING_PARTS = { { 4, 0, 1, 7, 3, 9 }, { 9, 0, 1, 12, 3, 9 },
        { 4, 3, 6, 7, 8, 10 }, { 9, 3, 6, 12, 8, 10 }, { 3.5, 7, 7, 12.5, 15, 12 }, { 6.5, 11, 12, 9.5, 14, 12.5 },
        { 5.5, 5.5, 12, 10.5, 10.5, 16 }, { 1.5, 0, 9, 4, 12, 12 }, { 12, 0, 9, 14.5, 12, 12 },
        { 1, 12, 8, 4.5, 14.5, 12 }, { 11.5, 12, 8, 15, 14.5, 12 }, { 0.5, 0, 13, 15.5, 1, 14 } };
    private static final int[] KNEELING_LOOK = { PLATE, PLATE, FRAME, FRAME, PLATE, HEART, HELM, FRAME, FRAME, PLATE,
        PLATE, FRAME };

    @SideOnly(Side.CLIENT)
    private IIcon[] looks;

    public BlockBrokenAutomaton() {
        super(Material.iron);
        setBlockName("brokenAutomaton");
        setBlockTextureName("naturalexceptions:automaton_plate");
        setHardness(3.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
    }

    private static int facing(int meta) {
        return 2 + (meta & 3);
    }

    private static boolean kneeling(int meta) {
        return (meta & KNEELING) != 0;
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] authored = kneeling(metadata) ? KNEELING_PARTS : SITTING;
        double[][] out = new double[authored.length][];
        for (int i = 0; i < authored.length; i++) {
            double[] b = authored[i];
            out[i] = PartModel
                .rotate(new double[] { b[0] * P, b[1] * P, b[2] * P, b[3] * P, b[4] * P, b[5] * P }, facing(metadata));
        }
        return out;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        int[] look = kneeling(metadata) ? KNEELING_LOOK : SITTING_LOOK;
        return looks[look[Math.min(index, look.length - 1)]];
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        looks = new IIcon[] { blockIcon, register.registerIcon("naturalexceptions:automaton_frame"),
            register.registerIcon("naturalexceptions:automaton_helm"),
            register.registerIcon("naturalexceptions:automaton_heart") };
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
        for (double[] b : parts(world.getBlockMetadata(x, y, z))) {
            if (PartModel.empty(b)) continue;
            AxisAlignedBB box = AxisAlignedBB
                .getBoundingBox(x + b[0], y + b[1], z + b[2], x + b[3], Math.min(y + 1.0, y + b[4]), z + b[5]);
            if (mask.intersectsWith(box)) boxes.add(box);
        }
    }

    /** Set down with its back to the wall behind it, facing whoever set it there; sitting or kneeling at random. */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int quarter = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5) & 3;
        // It faces back towards the placer: yaw 0 (looking +Z) puts its back to +Z, facing -Z (2 + 1).
        int[] facingFor = { 1, 2, 0, 3 };
        world.setBlockMetadataWithNotify(x, y, z, facingFor[quarter] | (world.rand.nextBoolean() ? KNEELING : 0), 2);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        return salvage(world.rand, fortune);
    }

    /** Iron from its frame and a little Stormglass; now and then a heart still whole, its halberd or a crest. */
    public static ArrayList<ItemStack> salvage(Random random, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Items.iron_ingot, 1 + random.nextInt(3 + fortune)));
        int shards = random.nextInt(3);
        if (shards > 0 && NEItems.stormglassShard != null) drops.add(new ItemStack(NEItems.stormglassShard, shards));
        if (NEItems.stormglassHeart != null && random.nextDouble() < HEART_CHANCE)
            drops.add(new ItemStack(NEItems.stormglassHeart));
        if (NEItems.citadelHalberd != null && random.nextDouble() < HALBERD_CHANCE)
            drops.add(new ItemStack(NEItems.citadelHalberd));
        Item crest = NEItems.TREASURES.get(Treasure.SENTRYS_CREST);
        if (crest != null && random.nextDouble() < CREST_CHANCE) drops.add(new ItemStack(crest));
        return drops;
    }
}
