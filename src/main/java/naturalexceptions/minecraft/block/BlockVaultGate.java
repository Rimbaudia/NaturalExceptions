package naturalexceptions.minecraft.block;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import naturalexceptions.minecraft.item.ItemSkyKey;

/**
 * The sealed gate of a sky dungeon's vault. Unbreakable; a Sky Key used on it opens the whole gate (every gate
 * block joined to this one) and is used up.
 */
public final class BlockVaultGate extends Block {

    /** A gate is never larger than this; stops a runaway search. */
    static final int MAX_GATE_BLOCKS = 64;

    public BlockVaultGate() {
        super(Material.rock);
        setBlockName("vaultGate");
        setBlockTextureName("naturalexceptions:vault_gate");
        setBlockUnbreakable();
        setResistance(6000000.0F);
        setLightLevel(0.3F);
        setStepSound(soundTypeMetal);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemSkyKey)) {
            if (!world.isRemote)
                player.addChatMessage(new ChatComponentText("\u00a77The gate is sealed. It needs a Sky Key."));
            return true;
        }
        if (world.isRemote) return true;
        int opened = open(world, x, y, z);
        if (opened > 0 && !player.capabilities.isCreativeMode) {
            held.stackSize--;
            if (held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        }
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.door_open", 1.0F, 0.6F);
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.anvil_land", 0.5F, 0.5F);
        return true;
    }

    /** Removes every gate block joined to (x, y, z); returns how many. */
    public static int open(World world, int x, int y, int z) {
        Block gate = world.getBlock(x, y, z);
        if (!(gate instanceof BlockVaultGate)) return 0;
        Deque<int[]> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        queue.add(new int[] { x, y, z });
        int opened = 0;
        while (!queue.isEmpty() && opened < MAX_GATE_BLOCKS) {
            int[] p = queue.poll();
            long key = ((long) p[0] << 38) ^ ((long) p[1] << 26) ^ (p[2] & 0x3FFFFFFL);
            if (!seen.add(key) || world.getBlock(p[0], p[1], p[2]) != gate) continue;
            world.setBlockToAir(p[0], p[1], p[2]);
            opened++;
            int[][] steps = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            for (int[] s : steps) queue.add(new int[] { p[0] + s[0], p[1] + s[1], p[2] + s[2] });
        }
        return opened;
    }

    @Override
    public boolean canEntityDestroy(net.minecraft.world.IBlockAccess world, int x, int y, int z, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, net.minecraft.world.Explosion explosion) {}
}
