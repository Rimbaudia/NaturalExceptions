package naturalexceptions.core.material;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Authoritative classification of vanilla Minecraft resources within the Natural Exceptions material model.
 *
 * <p>
 * Distinguishes meltable metals from deliberately non-meltable crystal, mineral, and fuel resources.
 */
public final class VanillaResourceClassification {

    private VanillaResourceClassification() {}

    /**
     * Resolves the material classification for a given item stack.
     */
    public static ResourceMaterialClass getClassification(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }

        Item item = stack.getItem();
        Block block = Block.getBlockFromItem(item);

        // 1. Metals
        if (item == Items.iron_ingot || block == Blocks.iron_ore || block == Blocks.iron_block) {
            return ResourceMaterialClass.METALLIC;
        }
        if (item == Items.gold_ingot || block == Blocks.gold_ore || block == Blocks.gold_block) {
            return ResourceMaterialClass.METALLIC;
        }
        if (item == NEItems.copperIngot || block == NEBlocks.copperOre || block == NEBlocks.copperBlock) {
            return ResourceMaterialClass.METALLIC;
        }

        // 2. Diamond (Carbon Crystal)
        if (item == Items.diamond || block == Blocks.diamond_ore || block == Blocks.diamond_block) {
            return ResourceMaterialClass.CARBON_CRYSTAL;
        }

        // 3. Emerald (Beryl Crystal)
        if (item == Items.emerald || block == Blocks.emerald_ore || block == Blocks.emerald_block) {
            return ResourceMaterialClass.BERYL_CRYSTAL;
        }

        // 4. Nether Quartz (Silicate Crystal)
        if (item == Items.quartz || block == Blocks.quartz_ore || block == Blocks.quartz_block) {
            return ResourceMaterialClass.SILICATE_CRYSTAL;
        }

        // 5. Lapis Lazuli (Mineral Rock)
        if ((item == Items.dye && stack.getItemDamage() == 4) || block == Blocks.lapis_ore
            || block == Blocks.lapis_block) {
            return ResourceMaterialClass.MINERAL_ROCK;
        }

        // 6. Redstone (Reactive Mineral)
        if (item == Items.redstone || block == Blocks.redstone_ore
            || block == Blocks.lit_redstone_ore
            || block == Blocks.redstone_block) {
            return ResourceMaterialClass.REACTIVE_MINERAL;
        }

        // 7. Coal / Charcoal (Combustible Carbon Fuel)
        if (item == Items.coal || block == Blocks.coal_ore || block == Blocks.coal_block) {
            return ResourceMaterialClass.ORGANIC_FUEL;
        }

        // 8. Blaze Fuel (Magical / Nether High-Temp Fuel)
        if (item == Items.blaze_rod || item == Items.blaze_powder) {
            return ResourceMaterialClass.MAGICAL_FUEL;
        }

        return null;
    }

    /**
     * Checks if the resource can be smelted / melted in a metallurgical crucible.
     */
    public static boolean isMeltable(ItemStack stack) {
        ResourceMaterialClass classification = getClassification(stack);
        return classification != null && classification.isMeltable();
    }

    /**
     * Returns a player-facing diagnostic explaining why the resource cannot be smelted in the crucible.
     * Returns null if the resource is meltable or unclassified.
     */
    public static String getRejectionReason(ItemStack stack) {
        ResourceMaterialClass classification = getClassification(stack);
        if (classification == null || classification.isMeltable()) {
            return null;
        }

        switch (classification) {
            case CARBON_CRYSTAL:
                return "Diamond is crystalline carbon; it cannot be melted in a standard hearth.";
            case BERYL_CRYSTAL:
                return "Emerald is a gemstone crystal; it cannot be melted in a standard hearth.";
            case SILICATE_CRYSTAL:
                return "Nether quartz is a crystalline mineral; it cannot be melted into metallic broth.";
            case MINERAL_ROCK:
                return "Lapis lazuli is a sedimentary mineral rock; it cannot be cast.";
            case REACTIVE_MINERAL:
                return "Redstone dust is a reactive mineral substance; it destabilizes and combusts rather than melting.";
            case ORGANIC_FUEL:
                return "Coal is a combustible fuel; insert into the lower fuel firebox, not the crucible.";
            case MAGICAL_FUEL:
                return "Blaze fuel is a high-temperature combustible; insert into the lower fuel firebox, not the crucible.";
            default:
                return "This material cannot be melted in a crucible.";
        }
    }

    /**
     * Resolves the canonical material reference for vanilla resources.
     */
    public static MaterialRef getMaterialRef(ItemStack stack) {
        ResourceMaterialClass classification = getClassification(stack);
        if (classification == null) {
            return null;
        }
        Item item = stack.getItem();
        Block block = Block.getBlockFromItem(item);
        if (item == Items.iron_ingot || block == Blocks.iron_ore || block == Blocks.iron_block) {
            return BuiltinMaterials.IRON.ref();
        }
        if (item == Items.gold_ingot || block == Blocks.gold_ore || block == Blocks.gold_block) {
            return BuiltinMaterials.GOLD.ref();
        }
        if (item == NEItems.copperIngot || block == NEBlocks.copperOre || block == NEBlocks.copperBlock) {
            return BuiltinMaterials.COPPER.ref();
        }
        if (item == Items.diamond) {
            return MaterialRef.generatedFrom(CanonicalComposition.of(ResourceId.of("minecraft", "diamond")));
        }
        if (item == Items.emerald) {
            return MaterialRef.generatedFrom(CanonicalComposition.of(ResourceId.of("minecraft", "emerald")));
        }
        if (item == Items.quartz) {
            return MaterialRef.generatedFrom(CanonicalComposition.of(ResourceId.of("minecraft", "quartz")));
        }
        if (item == Items.dye && stack.getItemDamage() == 4) {
            return MaterialRef.generatedFrom(CanonicalComposition.of(ResourceId.of("minecraft", "lapis")));
        }
        if (item == Items.redstone) {
            return MaterialRef.generatedFrom(CanonicalComposition.of(ResourceId.of("minecraft", "redstone")));
        }
        if (item == Items.coal) {
            return MaterialRef.generatedFrom(CanonicalComposition.of(ResourceId.of("minecraft", "coal")));
        }
        return null;
    }
}
