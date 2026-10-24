package naturalexceptions.minecraft.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
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
        QUARRY,
        /** A dead household's chest: a pantry, a wardrobe, a workshop or a study, now and then a better find. */
        HOUSEHOLD
    }

    /** Chance that a household chest holds something better than the household's things. */
    public static final double HOUSEHOLD_GOODIE_CHANCE = 0.06;
    /** Of those, the chance it is a treasure (so about 1 household chest in 80). */
    public static final double HOUSEHOLD_TREASURE_SHARE = 0.2;

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
            case HOUSEHOLD:
                household(chest, random);
                break;
            case VAULT_TREASURE:
                vanilla(chest, random, ChestGenHooks.STRONGHOLD_CROSSING);
                vaultTreasure(chest, random); // last, so nothing lands on top of it
                break;
            case VAULT_RICH:
            default:
                vanilla(chest, random, ChestGenHooks.PYRAMID_DESERT_CHEST);
                vanilla(chest, random, ChestGenHooks.STRONGHOLD_CROSSING);
                break;
        }
    }

    /**
     * What a household left: the things of one of its rooms, mostly Minecraft's own, a few bandages sometimes, and
     * rarely a better find. Never a Sky Key: a citadel's key room stays its way in.
     */
    private static void household(IInventory chest, Random random) {
        switch (random.nextInt(4)) {
            case 0: // the pantry
                put(chest, random, Items.bread, 1 + random.nextInt(4));
                put(chest, random, Items.apple, random.nextInt(4));
                put(chest, random, Items.potato, random.nextInt(5));
                put(chest, random, Items.carrot, random.nextInt(5));
                put(chest, random, Items.wheat, random.nextInt(6));
                put(chest, random, Items.sugar, random.nextInt(3));
                put(chest, random, Items.bowl, random.nextInt(3));
                if (random.nextInt(3) == 0) put(chest, random, Items.cookie, 2 + random.nextInt(6));
                break;
            case 1: // the wardrobe
                putStack(chest, random, new ItemStack(Blocks.wool, 1 + random.nextInt(5), random.nextInt(16)));
                putStack(chest, random, new ItemStack(Blocks.carpet, 1 + random.nextInt(4), random.nextInt(16)));
                put(chest, random, Items.leather, random.nextInt(4));
                put(chest, random, Items.string, random.nextInt(6));
                if (random.nextInt(3) == 0) {
                    Item[] worn = { Items.leather_helmet, Items.leather_chestplate, Items.leather_leggings,
                        Items.leather_boots };
                    ItemStack piece = new ItemStack(worn[random.nextInt(worn.length)]);
                    piece.setItemDamage(random.nextInt(Math.max(1, piece.getMaxDamage() / 2)));
                    putStack(chest, random, piece);
                }
                if (NEItems.cloudsilk != null && random.nextInt(4) == 0)
                    put(chest, random, NEItems.cloudsilk, 1 + random.nextInt(3));
                break;
            case 2: // the workshop
                put(chest, random, Items.stick, 2 + random.nextInt(8));
                put(chest, random, Item.getItemFromBlock(Blocks.planks), random.nextInt(8));
                put(chest, random, Items.coal, random.nextInt(5));
                put(chest, random, Items.flint, random.nextInt(3));
                put(chest, random, Item.getItemFromBlock(Blocks.torch), random.nextInt(8));
                if (random.nextInt(2) == 0) {
                    Item[] tools = { Items.stone_pickaxe, Items.stone_axe, Items.stone_shovel, Items.iron_shovel,
                        Items.shears };
                    ItemStack tool = new ItemStack(tools[random.nextInt(tools.length)]);
                    tool.setItemDamage(tool.getMaxDamage() / 3 + random.nextInt(Math.max(1, tool.getMaxDamage() / 2)));
                    putStack(chest, random, tool);
                }
                if (random.nextInt(3) == 0) put(chest, random, Items.iron_ingot, 1 + random.nextInt(2));
                break;
            default: // the study
                put(chest, random, Items.paper, 1 + random.nextInt(6));
                put(chest, random, Items.book, random.nextInt(3));
                put(chest, random, Items.feather, random.nextInt(4));
                if (random.nextInt(2) == 0) putStack(chest, random, new ItemStack(Items.dye, 1 + random.nextInt(3), 0));
                if (random.nextInt(4) == 0) put(chest, random, random.nextBoolean() ? Items.clock : Items.compass, 1);
                if (random.nextInt(3) == 0) put(chest, random, Items.glass_bottle, 1 + random.nextInt(3));
                break;
        }
        if (NEItems.bandage != null && random.nextInt(4) == 0)
            put(chest, random, NEItems.bandage, 1 + random.nextInt(2));
        if (random.nextDouble() < HOUSEHOLD_GOODIE_CHANCE) {
            if (random.nextDouble() < HOUSEHOLD_TREASURE_SHARE) treasure(chest, random);
            else switch (random.nextInt(4)) {
                case 0:
                    // A book enchanted at random (1.7.10 turns the book into an enchanted book as it does so).
                    putStack(
                        chest,
                        random,
                        net.minecraft.enchantment.EnchantmentHelper
                            .addRandomEnchantment(random, new ItemStack(Items.book), 10 + random.nextInt(15)));
                    break;
                case 1:
                    put(chest, random, Items.gold_ingot, 2 + random.nextInt(3));
                    break;
                case 2:
                    put(chest, random, random.nextBoolean() ? Items.diamond : Items.emerald, 1);
                    break;
                default:
                    put(chest, random, random.nextBoolean() ? Items.record_cat : Items.record_13, 1);
                    break;
            }
        }
    }

    private static void putStack(IInventory chest, Random random, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return;
        for (int tries = 0; tries < 8; tries++) {
            int slot = random.nextInt(chest.getSizeInventory());
            if (chest.getStackInSlot(slot) == null) {
                chest.setInventorySlotContents(slot, stack);
                return;
            }
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
            if (!treasure.crafted()) continue; // relics never, vault finds only in a vault (vaultTreasures)
            Item item = NEItems.TREASURES.get(treasure);
            if (item != null) pool.add(item);
        }
        if (NEItems.skyfall != null) pool.add(NEItems.skyfall);
        if (NEItems.shardcaster != null) pool.add(NEItems.shardcaster);
        return pool;
    }

    /** A vault's pool: the chest treasures, and the treasures found only in vaults. */
    public static List<Item> vaultTreasures() {
        List<Item> pool = treasures();
        for (Treasure treasure : Treasure.values()) {
            Item item = NEItems.TREASURES.get(treasure);
            if (treasure.source() == Treasure.Source.VAULT && item != null) pool.add(item);
        }
        return pool;
    }

    private static void vaultTreasure(IInventory chest, Random random) {
        List<Item> pool = vaultTreasures();
        if (pool.isEmpty()) return;
        chest.setInventorySlotContents(TREASURE_SLOT, new ItemStack(pool.get(random.nextInt(pool.size()))));
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
