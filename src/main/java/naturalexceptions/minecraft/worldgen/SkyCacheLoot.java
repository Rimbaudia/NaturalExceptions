package naturalexceptions.minecraft.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * What sky chests hold. Mostly ordinary Minecraft loot (from the vanilla chest tables, so other mods' additions
 * appear too) with a few useful supplies; the treasures (five wearables, two weapons) are rare:
 * <ul>
 * <li>Island caches: vanilla dungeon loot, sometimes bandages, copper or Stormglass. Never a treasure.</li>
 * <li>Dungeon storerooms: mineshaft or stronghold loot and supplies (Stormglass among them); one in twelve holds a
 * spare Sky Key and one in twenty a treasure.</li>
 * <li>Key room: the Sky Key and a little stronghold loot.</li>
 * <li>Entrance hall: a Surveyor's Chart of the dungeon, torches and bread.</li>
 * <li>The vault: one treasure, always; and beside it a chest of temple and stronghold riches.</li>
 * </ul>
 * Every roll comes from the chest's own seed, so a chest's contents are the same every time.
 */
public final class SkyCacheLoot {

    public enum Tier {
        ISLAND,
        STORE,
        KEY,
        VAULT_TREASURE,
        VAULT_RICH,
        /** The entrance hall's: a Surveyor's Chart, torches and bread. */
        CHART,
        /** A quarry's tool chest: a Mason's Chisel, a worn pick, string, torches and bread. */
        QUARRY
    }

    /** Chance that a dungeon storeroom chest also holds a treasure. */
    public static final double STORE_TREASURE_CHANCE = 0.05;
    /** Chance that a dungeon storeroom chest holds a spare Sky Key (any key opens any Warden's gate). */
    public static final double STORE_KEY_CHANCE = 0.08;
    /** The slot a treasure goes in: the middle of the chest. */
    static final int TREASURE_SLOT = 13;

    private SkyCacheLoot() {}

    /** Island cache contents (kept for callers that do not say which kind of chest). */
    public static void fill(IInventory chest, long seed) {
        fill(chest, seed, Tier.ISLAND);
    }

    public static void fill(IInventory chest, long seed, Tier tier) {
        Random random = new Random(seed);
        switch (tier) {
            case ISLAND:
                vanilla(chest, random, ChestGenHooks.DUNGEON_CHEST);
                if (random.nextInt(10) < 3) put(chest, random, NEItems.bandage, 1 + random.nextInt(2));
                if (random.nextInt(10) < 3) put(chest, random, NEItems.copperIngot, 2 + random.nextInt(3));
                if (random.nextInt(10) < 2) put(chest, random, NEItems.stormglassShard, 1 + random.nextInt(3));
                break;
            case STORE:
                vanilla(
                    chest,
                    random,
                    random.nextBoolean() ? ChestGenHooks.MINESHAFT_CORRIDOR : ChestGenHooks.STRONGHOLD_CORRIDOR);
                if (random.nextInt(10) < 4) put(chest, random, NEItems.bandage, 2 + random.nextInt(3));
                if (random.nextInt(10) < 3) put(chest, random, NEItems.stormglassShard, 2 + random.nextInt(4));
                if (random.nextDouble() < STORE_KEY_CHANCE && NEItems.skyKey != null)
                    put(chest, random, NEItems.skyKey, 1);
                if (random.nextDouble() < STORE_TREASURE_CHANCE) treasure(chest, random);
                break;
            case KEY:
                vanilla(chest, random, ChestGenHooks.STRONGHOLD_CORRIDOR);
                if (NEItems.skyKey != null)
                    chest.setInventorySlotContents(TREASURE_SLOT, new ItemStack(NEItems.skyKey));
                break;
            case CHART:
                put(
                    chest,
                    random,
                    net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.torch),
                    6 + random.nextInt(6));
                put(chest, random, net.minecraft.init.Items.bread, 2 + random.nextInt(3));
                if (NEItems.surveyorsChart != null)
                    chest.setInventorySlotContents(TREASURE_SLOT, new ItemStack(NEItems.surveyorsChart));
                break;
            case QUARRY:
                if (NEItems.masonsChisel != null)
                    chest.setInventorySlotContents(TREASURE_SLOT, new ItemStack(NEItems.masonsChisel));
                ItemStack pick = new ItemStack(net.minecraft.init.Items.iron_pickaxe);
                pick.setItemDamage(60 + random.nextInt(120));
                chest.setInventorySlotContents(TREASURE_SLOT - 1, pick);
                put(chest, random, net.minecraft.init.Items.string, 3 + random.nextInt(6));
                put(
                    chest,
                    random,
                    net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.torch),
                    8 + random.nextInt(8));
                put(chest, random, net.minecraft.init.Items.bread, 2 + random.nextInt(3));
                break;
            case VAULT_TREASURE:
                vanilla(chest, random, ChestGenHooks.STRONGHOLD_CROSSING);
                treasure(chest, random); // last, so nothing lands on top of it
                break;
            case VAULT_RICH:
            default:
                vanilla(chest, random, ChestGenHooks.PYRAMID_DESERT_CHEST);
                vanilla(chest, random, ChestGenHooks.STRONGHOLD_CROSSING);
                break;
        }
    }

    private static void vanilla(IInventory chest, Random random, String table) {
        try {
            ChestGenHooks info = ChestGenHooks.getInfo(table);
            WeightedRandomChestContent
                .generateChestContents(random, info.getItems(random), chest, info.getCount(random));
        } catch (RuntimeException ignored) {
            // A missing or broken table (another mod's doing) leaves the chest with its other contents.
        }
    }

    /** The treasure pool: each wearable (not the Warden Relics) and each weapon equally likely. */
    public static List<Item> treasures() {
        List<Item> pool = new ArrayList<>();
        for (Treasure treasure : Treasure.values()) {
            if (treasure.relic()) continue; // Warden Relics are never in a chest
            Item item = NEItems.TREASURES.get(treasure);
            if (item != null) pool.add(item);
        }
        if (NEItems.skyfall != null) pool.add(NEItems.skyfall);
        if (NEItems.shardcaster != null) pool.add(NEItems.shardcaster);
        return pool;
    }

    private static void treasure(IInventory chest, Random random) {
        List<Item> pool = treasures();
        if (pool.isEmpty()) return;
        chest.setInventorySlotContents(TREASURE_SLOT, new ItemStack(pool.get(random.nextInt(pool.size()))));
    }

    private static void put(IInventory chest, Random random, Item item, int count) {
        if (item == null || count <= 0) return;
        for (int tries = 0; tries < 8; tries++) {
            int slot = random.nextInt(chest.getSizeInventory());
            if (chest.getStackInSlot(slot) == null) {
                chest.setInventorySlotContents(slot, new ItemStack(item, count));
                return;
            }
        }
    }
}
