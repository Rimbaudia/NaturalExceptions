package naturalexceptions.minecraft.knowledge.provider;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.knowledge.IKnowledgeProvider;
import naturalexceptions.minecraft.knowledge.KnowledgeCategory;
import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.knowledge.ProcessStep;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

public final class CraftingKnowledgeProvider implements IKnowledgeProvider {

    @Override
    public void registerEntries(KnowledgeRegistry registry) {
        ItemStack brickStack = new ItemStack(
            NEBlocks.refractoryBrick != null ? NEBlocks.refractoryBrick : Blocks.brick_block);
        ItemStack outletStack = new ItemStack(NEBlocks.smelterOutlet != null ? NEBlocks.smelterOutlet : Blocks.stone);
        ItemStack tuyereStack = new ItemStack(NEBlocks.smelterTuyere != null ? NEBlocks.smelterTuyere : Blocks.stone);
        ItemStack bellowsStack = new ItemStack(NEItems.bellows != null ? NEItems.bellows : Items.stick);
        ItemStack ladleStack = new ItemStack(NEItems.foundryLadle != null ? NEItems.foundryLadle : Items.bowl);
        ItemStack tableStack = new ItemStack(
            NEBlocks.castingTable != null ? NEBlocks.castingTable : Blocks.crafting_table);
        ItemStack assemblyStack = new ItemStack(
            NEBlocks.assemblyTable != null ? NEBlocks.assemblyTable : Blocks.crafting_table);
        ItemStack handleStack = ItemGenericComponent.createWoodenHandle();
        ItemStack copperIngotStack = new ItemStack(
            NEItems.copperIngot != null ? NEItems.copperIngot : Items.iron_ingot);

        // 1. Refractory Brick Block
        registry.registerEntry(
            KnowledgeEntry.builder("refractory_brick", "Refractory Brick Block", KnowledgeCategory.CRAFTING)
                .icon(brickStack)
                .summary("Heat-resistant refractory masonry engineered to contain extreme thermal crucibles.")
                .description(
                    "Refractory bricks are dense, alumina-clay based masonry designed to withstand "
                        + "molten metal temperatures exceeding 1600 \u00b0C without melting or cracking.\n\n"
                        + "Combine Clay Balls and Sand to make Refractory Mix, fire the mix in a Furnace, then craft "
                        + "four fired Refractory Bricks into this block.\n\n"
                        + "They are the fundamental structural block required to construct the Starter Smelter "
                        + "crucible, as well as the Casting Table, Smelter Outlet, Tuyere, and Assembly Table.")
                .addProperty("Max Safe Temp", "1800 \u00b0C (2073 K)")
                .addProperty("Smelter Base Required", "9 blocks")
                .addProperty("Smelter Walls Required", "15 blocks")
                .addProperty("Total Required for Smelter", "24 blocks")
                .addUsage(
                    ProcessStep.builder()
                        .operation("Multiblock Construction")
                        .addInput(new ItemStack(brickStack.getItem(), 24))
                        .addOutput(
                            new ItemStack(NEBlocks.starterSmelter != null ? NEBlocks.starterSmelter : Blocks.stone))
                        .addNote(
                            "Provides the controller for the minimum Starter Smelter; larger bounded vessels can be built around it.")
                        .build())
                .addRelated("starter_smelter")
                .addRelated("casting_table")
                .addRelated("smelter_outlet")
                .addRelated("smelter_tuyere")
                .build());

        // 2. Smelter Outlet
        registry.registerEntry(
            KnowledgeEntry.builder("smelter_outlet", "Smelter Outlet", KnowledgeCategory.CRAFTING)
                .icon(outletStack)
                .summary("Pouring spout mounted onto the crucible face to channel clean molten metal.")
                .description(
                    "The Smelter Outlet attaches directly to the exterior front face of the Starter Smelter "
                        + "at the crucible hearth level (dy = 0, dz = -1 relative to controller).\n\n"
                        + "Right-clicking the outlet opens the stopper valve to pour molten metal into a Casting "
                        + "Table positioned immediately below it. Molten slag sits above the metal layer and is "
                        + "automatically excluded from pouring.")
                .addProperty("Mounting Position", "Front exterior face (facing front)")
                .addProperty("Pour Rate", "Continuous gravity stream")
                .addProperty("Slag Filtering", "Automatic (taps lower clean metal layer)")
                .addAcquisition(
                    ProcessStep.builder()
                        .operation("Crafting Table")
                        .machine(new ItemStack(Blocks.crafting_table), "Crafting Grid")
                        .addInput(new ItemStack(brickStack.getItem(), 3), "3x Refractory Brick Blocks (V-shape)")
                        .addOutput(outletStack, "Smelter Outlet")
                        .addNote("Arranged as 'B B' and ' B ' in crafting grid.")
                        .build())
                .addUsage(
                    ProcessStep.builder()
                        .operation("Pouring Molten Metal")
                        .addInput(outletStack)
                        .addOutput(copperIngotStack, "Cast Component")
                        .addNote("Channels molten liquid from smelter crucible directly into mold on Casting Table.")
                        .build())
                .addRelated("starter_smelter")
                .addRelated("casting_table")
                .build());

        // 3. Smelter Tuyere
        registry.registerEntry(
            KnowledgeEntry.builder("smelter_tuyere", "Smelter Tuyere", KnowledgeCategory.CRAFTING)
                .icon(tuyereStack)
                .summary("Air-blast injection nozzle enabling forced air draft to reach iron-smelting temperatures.")
                .description(
                    "The Tuyere is a specialized refractory nozzle lined with copper to direct high-velocity "
                        + "air into the combustion zone. It mounts onto the exterior chamber walls (dx = \u00b12, dy = 0, dz = 1).\n\n"
                        + "The Tuyere is optional. Strong fuel can melt iron under natural draft; pumping a Hand Bellows "
                        + "through a Tuyere increases airflow and speeds heating.")
                .addProperty("Role", "Optional forced draft; increases heating rate")
                .addProperty("Attachment Ports", "Left & Right exterior chamber walls")
                .addAcquisition(
                    ProcessStep.builder()
                        .operation("Crafting Table")
                        .machine(new ItemStack(Blocks.crafting_table), "Crafting Grid")
                        .addInput(brickStack, "2x Refractory Brick Blocks")
                        .addInput(copperIngotStack, "1x Copper Ingot")
                        .addOutput(tuyereStack, "Smelter Tuyere")
                        .addNote("Arranged as 'BCB' horizontally in crafting grid.")
                        .build())
                .addUsage(
                    ProcessStep.builder()
                        .operation("Forced Draft Air Blast")
                        .addInput(tuyereStack)
                        .addInput(bellowsStack, "Hand Bellows")
                        .addOutput(new ItemStack(Items.iron_ingot), "Molten Iron")
                        .addNote(
                            "Pumping increases airflow and accelerates heating; the tuyere is an optional upgrade.")
                        .build())
                .addRelated("starter_smelter")
                .addRelated("bellows")
                .build());

        // 4. Hand Bellows
        registry.registerEntry(
            KnowledgeEntry.builder("bellows", "Hand Bellows", KnowledgeCategory.CRAFTING)
                .icon(bellowsStack)
                .summary(
                    "Manual air pump providing continuous draft pulses to accelerate combustion and raise temperature.")
                .description(
                    "The Hand Bellows allows the smith to force oxygen into the furnace hearth. "
                        + "Hold the bellows in hand and right-click against a mounted Tuyere on the Starter Smelter.\n\n"
                        + "Each pump injects a burst of combustion air, rapidly raising hearth temperature. "
                        + "Pumping consumes fuel more quickly but is essential to melt higher-melting metals like iron.")
                .addProperty("Blast Boost", "+350 to +500 \u00b0C over passive draft")
                .addProperty("Operation", "Right-click on mounted Tuyere")
                .addAcquisition(
                    ProcessStep.builder()
                        .operation("Crafting Table")
                        .machine(new ItemStack(Blocks.crafting_table), "Crafting Grid")
                        .addInput(new ItemStack(Blocks.planks, 4), "Wood Planks")
                        .addInput(new ItemStack(Items.leather, 1), "Leather")
                        .addInput(new ItemStack(Items.stick, 1), "Stick")
                        .addOutput(bellowsStack, "Hand Bellows")
                        .addNote("Crafted in crafting grid: P / PLP / S.")
                        .build())
                .addRelated("smelter_tuyere")
                .addRelated("starter_smelter")
                .build());

        // 5. Foundry Ladle
        registry.registerEntry(
            KnowledgeEntry.builder("foundry_ladle", "Foundry Ladle", KnowledgeCategory.CRAFTING)
                .icon(ladleStack)
                .summary("Refractory hand vessel used to skim molten slag and transport molten metal.")
                .description(
                    "The Foundry Ladle is a heat-shielded hand tool essential for crucible metallurgy.\n\n"
                        + "1. Skimming Slag: Shift-right-click the Starter Smelter controller while holding an empty ladle. "
                        + "This scoops accumulated slag lumps off the top layer, leaving clean liquid metal below.\n\n"
                        + "2. Metal Transport: Right-click molten metal to scoop up to 4.0 kg into the ladle. "
                        + "Carry it to a Casting Table and right-click to pour into an open mold.")
                .addProperty("Capacity", "4.0 kg liquid metal")
                .addProperty("Primary Action", "Shift-RClick: Skim Slag | RClick: Scoop/Pour")
                .addAcquisition(
                    ProcessStep.builder()
                        .operation("Crafting Table")
                        .machine(new ItemStack(Blocks.crafting_table), "Crafting Grid")
                        .addInput(new ItemStack(brickStack.getItem(), 3), "Refractory Bricks / Clay (Bowl shape)")
                        .addInput(new ItemStack(Items.stick, 1), "Stick Handle")
                        .addOutput(ladleStack, "Foundry Ladle")
                        .build())
                .addUsage(
                    ProcessStep.builder()
                        .operation("Slag Skimming")
                        .addInput(ladleStack)
                        .addOutput(
                            new ItemStack(NEItems.slagLump != null ? NEItems.slagLump : Items.clay_ball),
                            "Molten Slag Lump")
                        .addNote("Shift-right-click smelter to remove slag before tapping metal.")
                        .build())
                .addRelated("starter_smelter")
                .addRelated("slag_lump")
                .build());

        // 6. Casting Table
        registry.registerEntry(
            KnowledgeEntry.builder("casting_table", "Casting Table", KnowledgeCategory.CRAFTING)
                .icon(tableStack)
                .summary("Refractory workstation positioned beneath a smelter outlet to hold molds and cool castings.")
                .description(
                    "The Casting Table holds casting molds securely while liquid metal is channeled from the "
                        + "Smelter Outlet or poured from a ladle.\n\n"
                        + "Place a casting mold into the table with Right-Click. When filled with liquid metal, the table "
                        + "conducts heat into the surrounding ambient air. Pouring water nearby or into a trough cools "
                        + "castings significantly faster. Once solidified, Right-Click with an empty hand to extract the component.")
                .addProperty("Placement", "Directly beneath Smelter Outlet (dy = -1, dz = -1)")
                .addProperty("Supported Molds", "Blank, Ingot, Pick Head, Blade, Axe Head, Shovel, Hoe, Plate, Rod")
                .addProperty("Cooling", "Thermodynamic heat dissipation; accelerated by water")
                .addAcquisition(
                    ProcessStep.builder()
                        .operation("Crafting Table")
                        .machine(new ItemStack(Blocks.crafting_table), "Crafting Grid")
                        .addInput(new ItemStack(brickStack.getItem(), 5), "5x Refractory Brick Blocks (U-shape)")
                        .addOutput(tableStack, "Casting Table")
                        .addNote("Arranged as 'BBB' and 'B B' in crafting grid.")
                        .build())
                .addRelated("smelter_outlet")
                .addRelated("unshaped_blank_mold")
                .build());

        // 7. Assembly Table
        registry.registerEntry(
            KnowledgeEntry.builder("assembly_table", "Assembly Table", KnowledgeCategory.ASSEMBLY)
                .icon(assemblyStack)
                .summary(
                    "Heavy timber and refractory workbench for fitting tool heads onto handles and recovering components.")
                .description(
                    "The Assembly Table provides a dedicated workstation for tool fabrication and lossless disassembly.\n\n"
                        + "Place a Tool Head (Pick, Axe, Shovel, Hoe, Blade) in the head slot and a Handle in the handle slot "
                        + "to assemble a finished tool. Place any assembled tool inside to cleanly disassemble it, "
                        + "recovering both the head component and the handle without damage or loss.")
                .addProperty("Operation", "Tool Assembly & Disassembly (100% component recovery)")
                .addProperty("Inputs", "1x Tool Head + 1x Handle")
                .addAcquisition(
                    ProcessStep.builder()
                        .operation("Crafting Table")
                        .machine(new ItemStack(Blocks.crafting_table), "Crafting Grid")
                        .addInput(new ItemStack(Blocks.planks, 3), "3x Wood Planks (Top row)")
                        .addInput(new ItemStack(brickStack.getItem(), 4), "4x Refractory Bricks (Legs)")
                        .addOutput(assemblyStack, "Assembly Table")
                        .build())
                .addRelated("tool_assembly")
                .addRelated("wooden_handle")
                .build());

        // 8. Wooden Handle
        registry.registerEntry(
            KnowledgeEntry.builder("wooden_handle", "Wooden Tool Handle", KnowledgeCategory.ASSEMBLY)
                .icon(handleStack)
                .summary("Standard ergonomic wooden handle shaped for picks, axes, shovels, hoes, and blades.")
                .description(
                    "Carefully planed wooden tool handle crafted from standard sticks. "
                        + "Combines with any metal or alloy tool head in the Assembly Table or crafting grid "
                        + "to produce a finished tool.")
                .addProperty("Material", "Wood")
                .addProperty("Mass", "100 grams")
                .addAcquisition(
                    ProcessStep.builder()
                        .operation("Crafting Grid")
                        .machine(new ItemStack(Blocks.crafting_table), "Crafting Grid")
                        .addInput(new ItemStack(Items.stick, 2), "2x Sticks (Vertical column)")
                        .addOutput(handleStack, "Wooden Tool Handle")
                        .addNote("Arranged vertically: Stick over Stick.")
                        .build())
                .addUsage(
                    ProcessStep.builder()
                        .operation("Tool Assembly")
                        .machine(assemblyStack, "Assembly Table")
                        .addInput(handleStack)
                        .addInput(
                            new ItemStack(
                                NEItems.genericComponent != null ? NEItems.genericComponent : Items.iron_pickaxe),
                            "Tool Head")
                        .addOutput(
                            new ItemStack(
                                NEItems.genericAssembly != null ? NEItems.genericAssembly : Items.iron_pickaxe),
                            "Finished Tool")
                        .build())
                .addRelated("assembly_table")
                .addRelated("tool_assembly")
                .build());
    }
}
