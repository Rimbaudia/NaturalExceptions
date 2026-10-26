package naturalexceptions.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;

import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * An edge inlaid along a steel tool's head, past what any metal does alone: three shards of Lumenite, or three pieces
 * of a fallen star's Fused Impact Crust. Only a head hard enough to hold it (steel's) takes one. A Lumenite edge glows,
 * and leaves faint light in the dark where it cuts; a star-crust edge, every few blows, shatters the stone round
 * what it strikes.
 */
public enum ToolEdge {

    LUMENITE("lumenite", "Lumenite-edged", "Glows; leaves faint light where it cuts in the dark."),
    CRUST("crust", "Star-edged", "Every fourth blow on stone shatters the face around it.");

    static final String TAG = "Edge", STRIKES = "EdgeStrikes";
    /** Edged, it cuts this much faster. */
    public static final float SPEED = 1.3F;
    /** A star-crust edge shatters on every so many blows on stone. */
    public static final int SHATTER_EVERY = 4;
    /** A Lumenite edge leaves light in one cut of so many, where it is darker than this. */
    public static final int TRAIL_ONE_IN = 3, TRAIL_DARKER_THAN = 8;

    public final String id, prefix, effect;

    ToolEdge(String id, String prefix, String effect) {
        this.id = id;
        this.prefix = prefix;
        this.effect = effect;
    }

    /** The edge on a tool, or null. */
    public static ToolEdge of(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return null;
        String id = stack.getTagCompound()
            .getString(TAG);
        for (ToolEdge edge : values()) if (edge.id.equals(id)) return edge;
        return null;
    }

    public void setOn(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound()
            .setString(TAG, id);
    }

    /** Whether a player holds a Lumenite-edged tool (it glows in the hand). */
    public static boolean glowsIn(EntityPlayer player) {
        return player != null && of(player.getCurrentEquippedItem()) == LUMENITE;
    }

    /**
     * What the edge does as the block at (x, y, z) breaks under it. Returns how many more blocks it broke (a
     * shatter), for the tool's wear.
     */
    int cut(ItemStack stack, World world, Block block, int x, int y, int z, EntityPlayer player) {
        if (world == null || world.isRemote || player == null) return 0;
        if (this == LUMENITE) {
            if (NEBlocks.lamplight != null && world.isAirBlock(x, y, z)
                && world.getBlockLightValue(x, y, z) < TRAIL_DARKER_THAN
                && world.rand.nextInt(TRAIL_ONE_IN) == 0)
                world.setBlock(x, y, z, NEBlocks.lamplight, naturalexceptions.minecraft.block.BlockLamplight.TRAIL, 3);
            return 0;
        }
        if (block == null || block.getMaterial() != Material.rock) return 0;
        NBTTagCompound tag = stack.getTagCompound();
        int strikes = tag.getInteger(STRIKES) + 1;
        if (strikes < SHATTER_EVERY) {
            tag.setInteger(STRIKES, strikes);
            return 0;
        }
        tag.setInteger(STRIKES, 0);
        return shatter(world, x, y, z, player);
    }

    /** Breaks the stone of the 3x3 face round (x, y, z), square to where the player looks. */
    private static int shatter(World world, int x, int y, int z, EntityPlayer player) {
        Vec3 look = player.getLookVec();
        double ax = Math.abs(look.xCoord), ay = Math.abs(look.yCoord), az = Math.abs(look.zCoord);
        int broken = 0;
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
            if (a == 0 && b == 0) continue;
            int bx = x, by = y, bz = z;
            if (ay >= ax && ay >= az) {
                bx += a;
                bz += b;
            } else if (ax >= az) {
                by += a;
                bz += b;
            } else {
                bx += a;
                by += b;
            }
            if (breaks(world, bx, by, bz, player)) broken++;
        }
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "naturalexceptions:weapon.star_impact", 0.6F, 1.4F);
        if (world instanceof WorldServer) ((WorldServer) world)
            .func_147487_a("largesmoke", x + 0.5, y + 0.5, z + 0.5, 12, 0.6, 0.6, 0.6, 0.02);
        return broken;
    }

    private static boolean breaks(World world, int x, int y, int z, EntityPlayer player) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        if (block.getMaterial() != Material.rock || block == Blocks.bedrock || block.hasTileEntity(meta)) return false;
        float hardness = block.getBlockHardness(world, x, y, z);
        if (hardness < 0.0F || hardness > 10.0F || !ForgeHooks.canHarvestBlock(block, player, meta)) return false;
        return world.func_147480_a(x, y, z, true);
    }
}
