package naturalexceptions.minecraft.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialTransparent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.item.ItemTreasureGear;

/**
 * The glow of a Lamplighter's Cap: an invisible, empty block of light that follows its wearer (see
 * {@code HatAndShoeEffects}). It is not air to the game's scheduled ticks, so it can check on itself and go out when no
 * wearer stands by, as after a log-out or a crash.
 */
public final class BlockLamplight extends Block {

    /** How often it checks for a wearer, in ticks. */
    static final int CHECK_TICKS = 40;
    /** How near a wearer must stand for it to stay lit. */
    static final double WEARER_REACH = 2.5;

    private static final Material GLOW = new MaterialTransparent(MapColor.airColor);

    public BlockLamplight() {
        super(GLOW);
        setBlockName("lamplight");
        setBlockTextureName("naturalexceptions:lumenite_lamp");
        setLightLevel(14.0F / 15.0F);
        setLightOpacity(0);
        setHardness(0.0F);
    }

    @Override
    public boolean isAir(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public int getRenderType() {
        return -1;
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
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean canCollideCheck(int meta, boolean hitIfLiquid) {
        return false;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return null;
    }

    /** A piston breaks it rather than pushing light about. */
    @Override
    public int getMobilityFlag() {
        return 1;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (!world.isRemote) world.scheduleBlockUpdate(x, y, z, this, CHECK_TICKS);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (wearerNear(world, x, y, z)) world.scheduleBlockUpdate(x, y, z, this, CHECK_TICKS);
        else world.setBlockToAir(x, y, z);
    }

    private static boolean wearerNear(World world, int x, int y, int z) {
        AxisAlignedBB around = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1)
            .expand(WEARER_REACH, WEARER_REACH, WEARER_REACH);
        for (Object entity : (List<?>) world.getEntitiesWithinAABB(EntityPlayer.class, around)) {
            EntityPlayer player = (EntityPlayer) entity;
            if (ItemTreasureGear.worn(player, Treasure.LAMPLIGHTERS_CAP) > 0) return true;
            if (player.getCurrentEquippedItem() != null && player.getCurrentEquippedItem()
                .getItem() == naturalexceptions.minecraft.registry.NEItems.reliquaryLantern) return true;
        }
        return false;
    }
}
