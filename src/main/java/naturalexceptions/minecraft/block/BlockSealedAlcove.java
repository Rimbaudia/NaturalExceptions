package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * A walled-up niche in a sky dungeon: it looks like the bricks around it until a trap (or the Sky Warden) breaks it
 * open and whatever waited inside steps out. Meta: the low two bits pick the creature (zombie, skeleton, spider,
 * cave spider); bit 4 arms it with iron.
 */
public final class BlockSealedAlcove extends Block {

    public static final int ZOMBIE = 0;
    public static final int SKELETON = 1;
    public static final int SPIDER = 2;
    public static final int CAVE_SPIDER = 3;
    public static final int ARMED = 4;
    /** The sky dungeons' own creatures: kinds 4 and 5, with the high bit set. */
    public static final int STORMWISP = 4, SENTINEL = 5;
    static final int HIGH_KIND = 8;

    /** The creature kind in an alcove's meta: the low two bits, and a third from the high bit. */
    public static int kind(int meta) {
        return (meta & 3) | ((meta & HIGH_KIND) != 0 ? 4 : 0);
    }

    public BlockSealedAlcove() {
        super(Material.rock);
        setBlockName("sealedAlcove");
        setBlockTextureName("naturalexceptions:skystone_bricks_cracked");
        setHardness(1.5F);
        setResistance(10.0F);
        setStepSound(soundTypeStone);
    }

    /** Breaks the alcove open and lets its occupant out; returns the creature, or null if nothing was there. */
    public static EntityLiving release(World world, int x, int y, int z) {
        if (world.isRemote || !(world.getBlock(x, y, z) instanceof BlockSealedAlcove)) return null;
        int meta = world.getBlockMetadata(x, y, z);
        world.playAuxSFX(2001, x, y, z, Block.getIdFromBlock(world.getBlock(x, y, z)));
        world.setBlockToAir(x, y, z);
        EntityLiving mob;
        switch (kind(meta)) {
            case STORMWISP:
                mob = new naturalexceptions.minecraft.entity.EntityStormwisp(world);
                break;
            case SENTINEL:
                mob = new naturalexceptions.minecraft.entity.EntitySkystoneSentinel(world);
                break;
            case SKELETON:
                mob = new naturalexceptions.minecraft.entity.EntityStormboneArcher(world);
                break;
            case SPIDER:
            case CAVE_SPIDER:
                mob = new naturalexceptions.minecraft.entity.EntityCloudweaver(world);
                break;
            case ZOMBIE:
            default:
                mob = new naturalexceptions.minecraft.entity.EntityGaleHusk(world);
                break;
        }
        boolean husk = mob instanceof naturalexceptions.minecraft.entity.EntityGaleHusk;
        if ((meta & ARMED) != 0 && (husk || mob instanceof naturalexceptions.minecraft.entity.EntityStormboneArcher)) {
            mob.setCurrentItemOrArmor(4, new ItemStack(Items.iron_helmet));
            mob.setCurrentItemOrArmor(3, new ItemStack(Items.iron_chestplate));
            if (husk) mob.setCurrentItemOrArmor(0, new ItemStack(Items.iron_sword));
        }
        // It steps out of its niche into the open (a niche is only one block high).
        int[] spot = { x, z };
        for (int[] s : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
            if (world.isAirBlock(x + s[0], y, z + s[1]) && world.isAirBlock(x + s[0], y + 1, z + s[1])) {
                spot = new int[] { x + s[0], z + s[1] };
                break;
            }
        }
        mob.setLocationAndAngles(spot[0] + 0.5, y, spot[1] + 0.5, world.rand.nextFloat() * 360.0F, 0.0F);
        mob.func_110163_bv(); // stays: it lives in the dungeon
        world.spawnEntityInWorld(mob);
        return mob;
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    @Override
    public net.minecraft.item.Item getItemDropped(int meta, java.util.Random random, int fortune) {
        return Item.getItemFromBlock(naturalexceptions.minecraft.registry.NEBlocks.skystoneBricks);
    }
}
