package naturalexceptions.minecraft.recipe;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Crafting for the sky-cache treasures, so a player who has not found a cache can still make them. Each recipe
 * uses what the item is made of: a bottled cloud needs feathers and Lumenite's light, a sword that calls stars
 * needs Lumenite and glowstone.
 */
public final class TreasureRecipes {

    private TreasureRecipes() {}

    public static void register() {
        Item shard = NEItems.lumeniteShard;
        Item copper = NEItems.copperIngot;
        if (shard == null || copper == null) return;
        ItemStack anyWool = new ItemStack(Blocks.wool, 1, OreDictionary.WILDCARD_VALUE);
        ItemStack anyIronSword = new ItemStack(Items.iron_sword, 1, OreDictionary.WILDCARD_VALUE);

        add(Treasure.CLOUD_FLASK, " F ", "FBF", " L ", 'F', Items.feather, 'B', Items.glass_bottle, 'L', shard);
        add(Treasure.GLIDER_CLOAK, "WLW", "WWW", "W W", 'W', anyWool, 'L', Items.leather);
        add(Treasure.HORSESHOE_PENDANT, "S S", "I I", "III", 'S', Items.string, 'I', Items.iron_ingot);
        add(Treasure.CLIMBING_CLAWS, "I I", "ILI", "LLL", 'I', Items.iron_ingot, 'L', Items.leather);
        add(Treasure.BAND_OF_MENDING, " G ", "GMG", " G ", 'G', Items.gold_ingot, 'M', Items.speckled_melon);
        if (NEItems.skyfall != null) GameRegistry.addRecipe(
            new ItemStack(NEItems.skyfall),
            "GLG",
            "LSL",
            "GLG",
            'G',
            Items.glowstone_dust,
            'L',
            shard,
            'S',
            anyIronSword);
        if (NEItems.shardcaster != null)
            GameRegistry.addRecipe(new ItemStack(NEItems.shardcaster), " LL", " CL", "C  ", 'L', shard, 'C', copper);
    }

    private static void add(Treasure treasure, Object... pattern) {
        Item item = NEItems.TREASURES.get(treasure);
        if (item != null) GameRegistry.addRecipe(new ItemStack(item), pattern);
    }
}
