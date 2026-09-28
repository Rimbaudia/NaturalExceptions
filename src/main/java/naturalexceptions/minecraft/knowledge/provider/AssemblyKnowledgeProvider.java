package naturalexceptions.minecraft.knowledge.provider;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.knowledge.IKnowledgeProvider;
import naturalexceptions.minecraft.knowledge.KnowledgeCategory;
import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.knowledge.ProcessStep;
import naturalexceptions.minecraft.registry.NEBlocks;

public final class AssemblyKnowledgeProvider implements IKnowledgeProvider {

    @Override
    public void registerEntries(KnowledgeRegistry registry) {
        ItemStack tableStack = new ItemStack(
            NEBlocks.assemblyTable != null ? NEBlocks.assemblyTable : Blocks.crafting_table);
        ItemStack handleStack = ItemGenericComponent.createWoodenHandle();

        // Sample Copper Pick Head
        ComponentInstance copperPickHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            MaterialQuantity.ofGrams(30.0 * 8.96),
            ManufacturingState.FINISHED);
        ItemStack pickHeadStack = ItemGenericComponent.createStack(copperPickHead);

        // Sample Copper Pickaxe
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "wood"))),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);
        AssemblyGraph sampleAssembly = AssemblyGraph.ofTool(copperPickHead, woodHandle);
        ItemStack pickaxeStack = ItemGenericAssembly.createStack(sampleAssembly);

        registry.registerEntry(
            KnowledgeEntry.builder("tool_assembly", "Tool Assembly & Disassembly", KnowledgeCategory.ASSEMBLY)
                .icon(pickaxeStack != null ? pickaxeStack : new ItemStack(Items.iron_pickaxe))
                .summary("Combining cast tool heads with handles into functional tools with reversible recovery.")
                .description(
                    "Tools in Natural Exceptions are physical assemblies composed of a distinct Tool Head and Handle.\n\n"
                        + "\u00a7lAssembly Process:\u00a7r\n"
                        + "Combine 1 Tool Head (Pick Head, Axe Head, Shovel Head, Hoe Head, or Blade) with 1 Handle "
                        + "(Wooden Handle or metal Rod/Handle) in an \u00a7eAssembly Table\u00a7r or crafting grid.\n\n"
                        + "\u00a7lPhysical Tool Properties:\u00a7r\n"
                        + "\u2022 \u00a7eMining Speed & Hardness:\u00a7r Determined by the head material's physical density and hardness.\n"
                        + "\u2022 \u00a7eDurability:\u00a7r Derived from tensile strength, yield stress, and component mass.\n"
                        + "\u2022 \u00a7eHarvest Capability:\u00a7r Copper harvests stone/coal/copper (Level 1). Iron harvests redstone/diamond/gold (Level 2).\n\n"
                        + "\u00a7lTaking a tool apart:\u00a7r\n"
                        + "Lay an assembled tool in the head slot of an empty Assembly Table (or right-click the table "
                        + "holding it). It separates into the same head and handle it was made from, carrying any wear "
                        + "they picked up in use. The crafting grid only assembles; it never takes tools apart.\n\n"
                        + "A tool that breaks in use falls apart: its fractured head and its handle drop at your feet.")
                .addProperty("Assembly Grid", "Shapeless (Head + Handle)")
                .addProperty("Disassembly", "Assembly Table returns the same head and handle")
                .addProperty("Component Loss", "None; wear from use is kept")
                .addProperty("Harvest Level: Copper", "Level 1 (Stone, Coal, Copper)")
                .addProperty("Harvest Level: Iron", "Level 2 (Iron, Redstone, Diamond)")
                .addAcquisition(
                    ProcessStep.builder()
                        .operation("Tool Assembly")
                        .machine(tableStack, "Assembly Table")
                        .addInput(pickHeadStack, "Tool Head (e.g. Copper Pick Head)")
                        .addInput(handleStack, "Wooden Tool Handle")
                        .addOutput(pickaxeStack, "Assembled Tool (e.g. Copper Pickaxe)")
                        .addNote("Combines head and handle into complete working tool.")
                        .build())
                .addUsage(
                    ProcessStep.builder()
                        .operation("Tool Disassembly")
                        .machine(tableStack, "Assembly Table")
                        .addInput(pickaxeStack, "Assembled Tool")
                        .addOutput(pickHeadStack, "Recovered Tool Head")
                        .addOutput(handleStack, "Recovered Tool Handle")
                        .addNote("Put the tool in the head slot of an empty table.")
                        .build())
                .addRelated("assembly_table")
                .addRelated("wooden_handle")
                .addRelated("mold_pick_head")
                .build());
    }
}
