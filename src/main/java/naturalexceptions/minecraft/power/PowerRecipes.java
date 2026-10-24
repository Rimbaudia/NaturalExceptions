package naturalexceptions.minecraft.power;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.core.power.Cable;
import naturalexceptions.minecraft.recipe.RecipeCastCopper;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Copper drawn into wire, wire twisted into cable and cable into heavy cable, each wrapped in cloth if wanted; and the
 * first machines, from wire and the things to hand.
 */
public final class PowerRecipes {

    private PowerRecipes() {}

    public static ItemStack cable(Cable kind, int count) {
        return new ItemStack(NEBlocks.cable, count, kind.ordinal());
    }

    public static void register() {
        if (NEBlocks.cable == null || NEItems.copperIngot == null) return;
        // Two ingots drawn into eight lengths of wire (a cast ingot does as well).
        GameRegistry.addRecipe(new RecipeCastCopper(cable(Cable.LIGHT_BARE, 8), "C", "C", 'C', NEItems.copperIngot));
        // Three wires twisted into a cable; six cables laid up into a heavy one.
        GameRegistry.addRecipe(cable(Cable.STANDARD_BARE, 1), "WWW", 'W', cable(Cable.LIGHT_BARE, 1));
        GameRegistry.addRecipe(cable(Cable.HEAVY_BARE, 1), "CCC", "CCC", 'C', cable(Cable.STANDARD_BARE, 1));
        // Eight lengths round a block of wool: wrapped in cloth.
        ItemStack wool = new ItemStack(Blocks.wool, 1, OreDictionary.WILDCARD_VALUE);
        for (Cable bare : Cable.values()) {
            if (bare.insulated) continue;
            Cable wrapped = Cable.values()[bare.ordinal() + 1];
            GameRegistry.addRecipe(cable(wrapped, 8), "XXX", "XWX", "XXX", 'X', cable(bare, 1), 'W', wool);
        }
        ItemStack wire = cable(Cable.LIGHT_BARE, 1);
        if (NEBlocks.knifeSwitch != null) GameRegistry.addRecipe(
            new RecipeCastCopper(
                new ItemStack(NEBlocks.knifeSwitch),
                "C",
                "S",
                'C',
                NEItems.copperIngot,
                'S',
                Blocks.stone_slab));
        if (NEBlocks.stormglassRelay != null && NEItems.stormglassShard != null) GameRegistry.addRecipe(
            new ItemStack(NEBlocks.stormglassRelay, 2),
            "G",
            "W",
            "I",
            'G',
            NEItems.stormglassShard,
            'W',
            wire,
            'I',
            Items.iron_ingot);
        if (NEBlocks.crankDynamo != null) GameRegistry.addRecipe(
            new ItemStack(NEBlocks.crankDynamo),
            "IWI",
            "WSW",
            "III",
            'I',
            Items.iron_ingot,
            'W',
            wire,
            'S',
            Items.stick);
        if (NEBlocks.arcLamp != null) GameRegistry.addRecipe(
            new ItemStack(NEBlocks.arcLamp),
            " G ",
            "WCW",
            " I ",
            'G',
            Blocks.glass,
            'W',
            wire,
            'C',
            new ItemStack(Items.coal, 1, OreDictionary.WILDCARD_VALUE),
            'I',
            Items.gold_ingot);
        ItemStack anyWool = new ItemStack(Blocks.wool, 1, OreDictionary.WILDCARD_VALUE);
        if (NEBlocks.windmill != null && NEBlocks.crankDynamo != null) GameRegistry.addRecipe(
            new ItemStack(NEBlocks.windmill),
            "WSW",
            "SDS",
            "WSW",
            'W',
            anyWool,
            'S',
            Items.stick,
            'D',
            NEBlocks.crankDynamo);
        if (NEBlocks.heartSocket != null) GameRegistry.addRecipe(
            new RecipeCastCopper(
                new ItemStack(NEBlocks.heartSocket),
                "ICI",
                "WGW",
                "III",
                'I',
                Items.iron_ingot,
                'C',
                NEItems.copperIngot,
                'W',
                wire,
                'G',
                Blocks.glass));
        if (NEBlocks.stormCell != null && NEItems.stormglassShard != null) GameRegistry.addRecipe(
            new ItemStack(NEBlocks.stormCell),
            "IGI",
            "SSS",
            "IWI",
            'I',
            Items.iron_ingot,
            'G',
            Blocks.glass,
            'S',
            NEItems.stormglassShard,
            'W',
            wire);
        if (NEBlocks.lightningSpire != null && NEItems.stormglassShard != null) GameRegistry.addRecipe(
            new ItemStack(NEBlocks.lightningSpire),
            " S ",
            "SBS",
            " B ",
            'S',
            NEItems.stormglassShard,
            'B',
            Blocks.iron_bars);
        if (NEItems.multimeter != null) GameRegistry.addRecipe(
            new ItemStack(NEItems.multimeter),
            "IGI",
            "WRW",
            "III",
            'I',
            Items.iron_ingot,
            'G',
            Blocks.glass_pane,
            'W',
            wire,
            'R',
            Items.redstone);
    }
}
