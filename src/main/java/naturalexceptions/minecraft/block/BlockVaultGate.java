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
 * The sealed gates of a sky dungeon. Unbreakable. Meta 0 seals the Sky Warden's hall: a Sky Key used on it opens the
 * whole gate (every gate block joined to this one) and is used up. Meta 1 seals the vault and is bound to the Warden:
 * no key moves it, and it breaks when the Warden falls ({@link #breakWardenSeals}).
 */
public final class BlockVaultGate extends Block {

    /** A gate is never larger than this; stops a runaway search. */
    static final int MAX_GATE_BLOCKS = 64;
    public static final int KEY_GATE = 0;
    public static final int WARDEN_BOUND = 1;

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
        if (world.getBlockMetadata(x, y, z) == WARDEN_BOUND) {
            if (!world.isRemote)
                player.addChatMessage(new ChatComponentText("\u00a77This seal answers only to the Castellan's fall."));
            return true;
        }
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

    /** Removes every gate block of the same kind joined to (x, y, z); returns how many. */
    public static int open(World world, int x, int y, int z) {
        Block gate = world.getBlock(x, y, z);
        if (!(gate instanceof BlockVaultGate)) return 0;
        int kind = world.getBlockMetadata(x, y, z);
        Deque<int[]> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        queue.add(new int[] { x, y, z });
        int opened = 0;
        while (!queue.isEmpty() && opened < MAX_GATE_BLOCKS) {
            int[] p = queue.poll();
            long key = ((long) p[0] << 38) ^ ((long) p[1] << 26) ^ (p[2] & 0x3FFFFFFL);
            if (!seen.add(key) || world.getBlock(p[0], p[1], p[2]) != gate
                || world.getBlockMetadata(p[0], p[1], p[2]) != kind) continue;
            world.setBlockToAir(p[0], p[1], p[2]);
            opened++;
            int[][] steps = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            for (int[] s : steps) queue.add(new int[] { p[0] + s[0], p[1] + s[1], p[2] + s[2] });
        }
        return opened;
    }

    /** Breaks every Warden-bound seal within {@code reach} of (x, y, z); returns how many gate blocks opened. */
    public static int breakWardenSeals(World world, int x, int y, int z, int reach) {
        if (world == null) return 0;
        int opened = 0;
        int minX = x - reach, maxX = x + reach;
        int minY = Math.max(1, y - reach / 2), maxY = Math.min(254, y + reach / 2);
        int minZ = z - reach, maxZ = z + reach;
        int minCX = minX >> 4, maxCX = maxX >> 4;
        int minCZ = minZ >> 4, maxCZ = maxZ >> 4;
        int minSection = minY >> 4, maxSection = maxY >> 4;
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                if (world.getChunkProvider() != null && !world.getChunkProvider()
                    .chunkExists(cx, cz)) continue;
                net.minecraft.world.chunk.Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                if (chunk == null) continue;
                if (chunk.chunkTileEntityMap != null) {
                    for (Object obj : new java.util.ArrayList<>(chunk.chunkTileEntityMap.values())) {
                        if (obj instanceof TileEntityWardenSeal) {
                            TileEntityWardenSeal seal = (TileEntityWardenSeal) obj;
                            if (!seal.isInvalid() && Math.abs(seal.xCoord - x) <= reach
                                && Math.abs(seal.yCoord - y) <= reach / 2
                                && Math.abs(seal.zCoord - z) <= reach
                                && naturalexceptions.minecraft.registry.NEBlocks.skystoneBricks != null) {
                                world.setBlock(
                                    seal.xCoord,
                                    seal.yCoord,
                                    seal.zCoord,
                                    naturalexceptions.minecraft.registry.NEBlocks.skystoneBricks,
                                    0,
                                    3);
                            }
                        }
                    }
                }
                net.minecraft.world.chunk.storage.ExtendedBlockStorage[] storages = chunk.getBlockStorageArray();
                if (storages == null) continue;
                int startX = Math.max(minX, cx << 4), endX = Math.min(maxX, (cx << 4) + 15);
                int startZ = Math.max(minZ, cz << 4), endZ = Math.min(maxZ, (cz << 4) + 15);
                for (int sec = minSection; sec <= maxSection && sec < storages.length; sec++) {
                    net.minecraft.world.chunk.storage.ExtendedBlockStorage storage = storages[sec];
                    if (storage == null || storage.isEmpty()) continue;
                    int startY = Math.max(minY, sec << 4), endY = Math.min(maxY, (sec << 4) + 15);
                    for (int bx = startX; bx <= endX; bx++) {
                        int lx = bx & 15;
                        for (int bz = startZ; bz <= endZ; bz++) {
                            int lz = bz & 15;
                            for (int by = startY; by <= endY; by++) {
                                int ly = by & 15;
                                Block blk = storage.getBlockByExtId(lx, ly, lz);
                                if (blk == naturalexceptions.minecraft.registry.NEBlocks.wardenSeal
                                    && naturalexceptions.minecraft.registry.NEBlocks.skystoneBricks != null) {
                                    world.setBlock(
                                        bx,
                                        by,
                                        bz,
                                        naturalexceptions.minecraft.registry.NEBlocks.skystoneBricks,
                                        0,
                                        3);
                                    continue;
                                }
                                if (!(blk instanceof BlockVaultGate)
                                    || storage.getExtBlockMetadata(lx, ly, lz) != WARDEN_BOUND) continue;
                                int n = open(world, bx, by, bz);
                                if (n > 0) {
                                    opened += n;
                                    world.playSoundEffect(bx + 0.5, by + 0.5, bz + 0.5, "random.anvil_land", 1.0F, 0.4F);
                                    world.playSoundEffect(
                                        bx + 0.5,
                                        by + 0.5,
                                        bz + 0.5,
                                        "naturalexceptions:warden.phase",
                                        1.5F,
                                        1.4F);
                                    if (world instanceof net.minecraft.world.WorldServer)
                                        ((net.minecraft.world.WorldServer) world).func_147487_a(
                                            "fireworksSpark",
                                            bx + 0.5,
                                            by + 1.0,
                                            bz + 0.5,
                                            40,
                                            1.0,
                                            1.0,
                                            1.0,
                                            0.1);
                                }
                            }
                        }
                    }
                }
            }
        }
        return opened;
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public boolean canEntityDestroy(net.minecraft.world.IBlockAccess world, int x, int y, int z, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, net.minecraft.world.Explosion explosion) {}
}
