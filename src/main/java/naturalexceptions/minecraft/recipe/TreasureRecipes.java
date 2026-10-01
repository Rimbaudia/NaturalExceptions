package naturalexceptions.minecraft.recipe;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Crafting for the sky-cache treasures, so a player who has not found a cache can still make them. Each recipe
 * uses what the item is made of: a bottled cloud needs feathers and Stormglass, and a sword that calls
 * stars is forged from a fallen star's crust and heart ({@link RecipeSkyfall}).
 */
public final class TreasureRecipes {

    private TreasureRecipes() {}

    public static void register() {
        Item shard = NEItems.stormglassShard;
        Item copper = NEItems.copperIngot;
        if (shard == null || copper == null) return;
        ItemStack anyWool = new ItemStack(Blocks.wool, 1, OreDictionary.WILDCARD_VALUE);

        // Cloudsilk Rope: three silk twisted into six lengths.
        if (NEItems.cloudsilk != null && NEBlocks.cloudsilkRope != null) GameRegistry
            .addRecipe(new ItemStack(NEBlocks.cloudsilkRope, 6), "S", "S", "S", 'S', NEItems.cloudsilk);
        // A bottled cloud; a cloak that catches the wind on Windbloom petals and a Cloudsilk sail.
        if (NEBlocks.cloud != null)
            add(Treasure.CLOUD_FLASK, " C ", "CBC", " L ", 'C', NEBlocks.cloud, 'B', Items.glass_bottle, 'L', shard);
        Item cloakItem = NEItems.TREASURES.get(Treasure.GLIDER_CLOAK);
        if (NEBlocks.windbloom != null && cloakItem != null && NEItems.cloudsilk != null) GameRegistry
            .addRecipe(new RecipeGliderCloak(cloakItem, Item.getItemFromBlock(NEBlocks.windbloom), NEItems.cloudsilk));
        if (NEItems.stypticSalve != null && NEBlocks.windbloom != null) GameRegistry.addShapelessRecipe(
            new ItemStack(NEItems.stypticSalve, 2),
            Items.bowl,
            NEBlocks.windbloom,
            Items.slime_ball,
            Items.paper);
        if (NEItems.windbloomTea != null && NEBlocks.windbloom != null) GameRegistry.addShapelessRecipe(
            new ItemStack(NEItems.windbloomTea),
            Items.potionitem,
            NEBlocks.windbloom,
            NEBlocks.windbloom);
        if (NEBlocks.skystoneBricks != null && NEBlocks.cloud != null) GameRegistry.addRecipe(
            new ItemStack(NEBlocks.skystoneBricks, 4),
            " S ",
            "SCS",
            " S ",
            'S',
            Blocks.stonebrick,
            'C',
            NEBlocks.cloud);
        add(Treasure.HORSESHOE_PENDANT, "S S", "I I", "III", 'S', Items.string, 'I', Items.iron_ingot);
        add(Treasure.CLIMBING_CLAWS, "I I", "ILI", "LLL", 'I', Items.iron_ingot, 'L', Items.leather);
        add(Treasure.BAND_OF_MENDING, " G ", "GMG", " G ", 'G', Items.gold_ingot, 'M', Items.speckled_melon);
        // A Warden's Gale Core: two Gale Vents; or the wind for a Cloud Flask or a Glider Cloak without the search.
        Item core = NEItems.galeCore;
        if (core != null && NEBlocks.galeVent != null && NEBlocks.skystoneBricks != null && NEBlocks.windbloom != null)
            GameRegistry.addRecipe(
                new ItemStack(NEBlocks.galeVent, 2),
                " W ",
                "IGI",
                "SSS",
                'W',
                NEBlocks.windbloom,
                'I',
                Items.iron_ingot,
                'G',
                core,
                'S',
                new ItemStack(NEBlocks.skystoneBricks, 1, OreDictionary.WILDCARD_VALUE));
        if (core != null) {
            Item flask = NEItems.TREASURES.get(Treasure.CLOUD_FLASK),
                cloak = NEItems.TREASURES.get(Treasure.GLIDER_CLOAK);
            if (flask != null) GameRegistry.addShapelessRecipe(new ItemStack(flask), core, Items.glass_bottle);
            if (cloak != null && NEItems.cloudsilk != null)
                GameRegistry.addRecipe(new RecipeGliderCloak.FromCore(cloak, core, NEItems.cloudsilk));
        }
        // Skyfall is forged from a fallen star, not from Stormglass.
        if (NEItems.skyfall != null && NEItems.materialSample != null) GameRegistry.addRecipe(new RecipeSkyfall());
        if (NEItems.shardcaster != null)
            GameRegistry.addRecipe(new ItemStack(NEItems.shardcaster), " LL", " CL", "C  ", 'L', shard, 'C', copper);
    }

    private static void add(Treasure treasure, Object... pattern) {
        Item item = NEItems.TREASURES.get(treasure);
        if (item != null) GameRegistry.addRecipe(new ItemStack(item), pattern);
    }
}
