package naturalexceptions.minecraft.knowledge.provider;

import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.knowledge.IKnowledgeProvider;
import naturalexceptions.minecraft.knowledge.KnowledgeCategory;
import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.knowledge.ProcessStep;
import naturalexceptions.minecraft.multiblock.MultiblockPattern;
import naturalexceptions.minecraft.multiblock.StarterSmelterDefinition;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

public final class MetallurgyKnowledgeProvider implements IKnowledgeProvider {

    @Override
    public void registerEntries(KnowledgeRegistry registry) {
        ItemStack smelterStack = new ItemStack(
            NEBlocks.starterSmelter != null ? NEBlocks.starterSmelter : Blocks.furnace);
        ItemStack copperOreStack = new ItemStack(NEBlocks.copperOre != null ? NEBlocks.copperOre : Blocks.iron_ore);
        ItemStack ironOreStack = new ItemStack(Blocks.iron_ore);
        // What a player gets from molten copper: an ingot mold's casting.
        ItemStack copperIngotStack = naturalexceptions.minecraft.item.ItemGenericComponent.createCast(
            naturalexceptions.core.fabrication.GeometryId.INGOT,
            naturalexceptions.content.material.BuiltinMaterials.COPPER.ref(),
            naturalexceptions.content.material.BuiltinMaterials.COPPER_COMPOSITION,
            naturalexceptions.content.material.BuiltinMaterials.COPPER_PROPERTIES);
        ItemStack slagStack = new ItemStack(NEItems.slagLump != null ? NEItems.slagLump : Items.clay_ball);
        ItemStack tuyereStack = new ItemStack(NEBlocks.smelterTuyere != null ? NEBlocks.smelterTuyere : Blocks.stone);
        ItemStack bellowsStack = new ItemStack(NEItems.bellows != null ? NEItems.bellows : Items.stick);
        ItemStack ladleStack = new ItemStack(NEItems.foundryLadle != null ? NEItems.foundryLadle : Items.bowl);
        StarterSmelterDefinition smelterDefinition = StarterSmelterDefinition.INSTANCE;
        int requiredBricks = 0;
        for (Map.Entry<String, Integer> required : smelterDefinition.getRequiredCountsByName()
            .entrySet()) {
            if (required.getKey()
                .equals("Refractory Brick Block")) requiredBricks += required.getValue();
        }
        int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
        for (MultiblockPattern pattern : smelterDefinition.patterns()) {
            if (!pattern.role()
                .isRequired()) continue;
            minX = Math.min(minX, pattern.dx());
            maxX = Math.max(maxX, pattern.dx());
            minY = Math.min(minY, pattern.dy());
            maxY = Math.max(maxY, pattern.dy());
            minZ = Math.min(minZ, pattern.dz());
            maxZ = Math.max(maxZ, pattern.dz());
        }

        // 1. Starter Smelter Multiblock Entry
        registry.registerEntry(
            KnowledgeEntry.builder("starter_smelter", "Starter Smelter", KnowledgeCategory.MULTIBLOCKS)
                .icon(smelterStack)
                .summary("Refractory crucible furnace for thermal smelting and casting.")
                .description(
                    "The Starter Smelter is an experimental variable-size refractory vessel for melting feedstock and separating slag. "
                        + "Use an odd 3-7 block outer width and depth with 1-3 clear cavity layers. Open Structure for the minimum layout; "
                        + "the same definition validates the live shell. Each clear cavity block provides 25 L. The floor and perimeter "
                        + "are refractory and the top stays open for exhaust. Tuyeres enter through horizontal walls; the lower side-wall "
                        + "outlet points outward. Strong fuel can melt iron under natural draft; bellows at a connected tuyere increase airflow.")
                .multiblock(StarterSmelterDefinition.INSTANCE)
                .addProperty("Structure Size", "Variable: 3-7 outer width/depth; 1-3 cavity layers")
                .addProperty("Minimum Refractory Bricks", requiredBricks + "x Refractory Brick Block")
                .addProperty("Controller", "1x Starter Smelter Controller")
                .addProperty("Forced Draft", "Optional; increases heating rate")
                .addProperty("Fuels Accepted", "Wood, Charcoal, Coal, Blaze Rods")
                .addProperty(
                    "Vessel Capacity",
                    "25 L per clear cell; occupied volume derives from layer mass and phase density")
                .addUsage(
                    ProcessStep.builder()
                        .operation("Ore Smelting")
                        .machine(smelterStack, "Starter Smelter")
                        .addInput(copperOreStack, "Copper Ore")
                        .addInput(new ItemStack(Items.coal), "Fuel (Coal/Charcoal)")
                        .addOutput(copperIngotStack, "Molten Copper (1.0 kg)")
                        .addOutput(slagStack, "Molten Slag")
                        .addNote("Heats to 1085 \u00b0C. Copper separates below slag; pour it into an Ingot mold.")
                        .build())
                .addUsage(
                    ProcessStep.builder()
                        .operation("Iron Ore Blast Smelting")
                        .machine(smelterStack, "Starter Smelter")
                        .addInput(ironOreStack, "Iron Ore")
                        .addInput(tuyereStack, "Smelter Tuyere")
                        .addInput(bellowsStack, "Hand Bellows")
                        .addOutput(new ItemStack(Items.iron_ingot), "Molten Iron")
                        .addNote("Requires forced draft to exceed 1538 \u00b0C.")
                        .build())
                .addUsage(
                    ProcessStep.builder()
                        .operation("Remelting")
                        .machine(smelterStack, "Starter Smelter")
                        .addInput(
                            new ItemStack(NEItems.copperIngot != null ? NEItems.copperIngot : Items.iron_ingot),
                            "Copper Ingot (found)")
                        .addOutput(copperIngotStack, "Molten Copper (1.0 kg)")
                        .addNote("Ingots found in caches and drops melt down like ore, ready to cast.")
                        .build())
                .addRelated("refractory_brick")
                .addRelated("smelter_outlet")
                .addRelated("smelter_tuyere")
                .addRelated("casting_table")
                .addRelated("copper_ore")
                .build());

        // 2. Copper Ore
        registry.registerEntry(
            KnowledgeEntry.builder("copper_ore", "Copper Ore", KnowledgeCategory.METALLURGY)
                .icon(copperOreStack)
                .summary("Primary raw mineral containing extractable copper.")
                .description(
                    "Copper Ore occurs naturally in stone strata. Unlike vanilla furnaces which convert ores into "
                        + "instant ingots, Natural Exceptions requires thermal smelting in the Starter Smelter.\n\n"
                        + "Place the ore into the crucible feed slot. Burn wood, charcoal, or coal to raise crucible "
                        + "temperature above 1085 \u00b0C (1358 K). The ore will liquefy into molten copper, producing "
                        + "a top layer of molten mineral slag.\n\n"
                        + "Use a Smelter Outlet to cast into ingots or tool components, or skim the slag using a Foundry Ladle.")
                .addProperty("Melting Point", "1085 \u00b0C (1358 K)")
                .addProperty("Yield per Ore", "1.0 kg Molten Metal + Slag")
                .addProperty("Draft Required", "Passive Draft sufficient (Tier 1)")
                .addAcquisition(
                    ProcessStep.builder()
                        .operation("Mining")
                        .addInputDescription("Natural rock veins underground (Pickaxe)")
                        .addOutput(copperOreStack, "Raw Copper Ore")
                        .build())
                .addUsage(
                    ProcessStep.builder()
                        .operation("Crucible Smelting")
                        .machine(smelterStack, "Starter Smelter")
                        .addInput(copperOreStack)
                        .addOutput(copperIngotStack, "1.0 kg Molten Copper")
                        .addOutput(slagStack, "Molten Slag")
                        .addNote("Melted in Starter Smelter at >= 1085 \u00b0C; cast in an Ingot mold.")
                        .build())
                .addRelated("starter_smelter")
                .addRelated("copper_ingot")
                .addRelated("slag_lump")
                .build());

        // 3. Iron Ore
        registry.registerEntry(
            KnowledgeEntry.builder("iron_ore", "Iron Ore", KnowledgeCategory.METALLURGY)
                .icon(ironOreStack)
                .summary("Iron-bearing mineral that requires sustained high heat to melt.")
                .description(
                    "Iron is a refractory transition metal with a high melting temperature of 1538 \u00b0C (1811 K).\n\n"
                        + "Use strong fuel such as charcoal or coal and allow the smelter to heat over time. A Tuyere "
                        + "and Hand Bellows are optional and increase heating rate; forced draft is not required for iron.")
                .addProperty("Melting Point", "1538 \u00b0C (1811 K)")
                .addProperty("Smelter Tier", "Strong fuel")
                .addProperty("Tuyere Required", "No; optional heating-rate upgrade")
                .addProperty("Bellows Required", "No; optional forced draft")
                .addAcquisition(
                    ProcessStep.builder()
                        .operation("Mining")
                        .addInputDescription("Underground veins (Stone or Copper Pickaxe)")
                        .addOutput(ironOreStack, "Iron Ore")
                        .build())
                .addUsage(
                    ProcessStep.builder()
                        .operation("Smelting")
                        .machine(smelterStack, "Starter Smelter")
                        .addInput(ironOreStack)
                        .addInput(new ItemStack(Items.coal), "Strong fuel (charcoal or coal)")
                        .addOutput(new ItemStack(Items.iron_ingot), "Molten Iron")
                        .addNote("Tuyere and Bellows can accelerate heating but are not required.")
                        .build())
                .addRelated("starter_smelter")
                .addRelated("smelter_tuyere")
                .addRelated("bellows")
                .build());

        // 4. Molten Slag Lump
        registry.registerEntry(
            KnowledgeEntry.builder("slag_lump", "Molten Slag Lump", KnowledgeCategory.METALLURGY)
                .icon(slagStack)
                .summary("Vitreous mineral byproduct separated during ore smelting.")
                .description(
                    "Slag consists of fluxed silica and rock matrix separated from raw ores during smelting. "
                        + "Because slag has lower density than liquid metal, it floats as a separate aggregate layer.\n\n"
                        + "If excessive slag accumulates in the crucible, it reduces available capacity for metal. "
                        + "Hold an empty Foundry Ladle and Shift-Right-Click the Starter Smelter controller to skim off a scoop of slag.\n\n"
                        + "The Smelter Outlet automatically taps from the bottom metal layer, ensuring that poured components "
                        + "are pure metal rather than contaminated by slag.\n\n"
                        + "Cold slag is not waste: ground up, it is basic slag, a phosphate fertilizer, used on fields "
                        + "much as bone meal is.")
                .addProperty("Phase", "Liquid / Glassy Solid")
                .addProperty("Density", "Lower than metal (floats on top)")
                .addProperty("Removal Method", "Shift-Right-Click with an empty Foundry Ladle")
                .addAcquisition(
                    ProcessStep.builder()
                        .operation("Slag Skimming")
                        .machine(smelterStack, "Starter Smelter")
                        .addInput(ladleStack, "Foundry Ladle")
                        .addOutput(slagStack, "Molten Slag Lump")
                        .addNote("Skimmed from top layer of crucible.")
                        .build())
                .addUsage(
                    ProcessStep.builder()
                        .operation("Grinding")
                        .machine(new ItemStack(net.minecraft.init.Blocks.crafting_table), "Crafting Grid")
                        .addInput(slagStack, "Slag Lump")
                        .addOutput(new ItemStack(Items.dye, 1, 15), "Bone Meal (basic slag fertilizer)")
                        .build())
                .addRelated("foundry_ladle")
                .addRelated("starter_smelter")
                .build());
    }
}
