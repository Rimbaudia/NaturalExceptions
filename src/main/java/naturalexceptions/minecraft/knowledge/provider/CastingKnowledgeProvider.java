package naturalexceptions.minecraft.knowledge.provider;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.minecraft.fabrication.TileEntityMoldAssemblyTable;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.knowledge.IKnowledgeProvider;
import naturalexceptions.minecraft.knowledge.KnowledgeCategory;
import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.knowledge.ProcessStep;
import naturalexceptions.minecraft.registry.NEBlocks;

public final class CastingKnowledgeProvider implements IKnowledgeProvider {

    @Override
    public void registerEntries(KnowledgeRegistry registry) {
        ItemStack tableStack = new ItemStack(
            NEBlocks.castingTable != null ? NEBlocks.castingTable : Blocks.crafting_table);
        ItemStack outletStack = new ItemStack(NEBlocks.smelterOutlet != null ? NEBlocks.smelterOutlet : Blocks.stone);
        ItemStack moldAssemblyStack = new ItemStack(
            NEBlocks.moldAssemblyTable != null ? NEBlocks.moldAssemblyTable : Blocks.crafting_table);
        ItemStack unshapedMoldStack = ItemCastingMold.createStack(null);

        // Molds and Components
        for (GeometryId geom : TileEntityMoldAssemblyTable.selectableGeometries()) {

            ItemStack moldStack = ItemCastingMold.createStack(geom);
            ComponentGeometry compGeom = ComponentGeometry.forId(geom);
            double volumeCm3 = compGeom.volumeCm3();

            // Create copper component sample for preview
            ComponentInstance sampleComp = new ComponentInstance(
                geom,
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
                MaterialQuantity.ofGrams(volumeCm3 * 8.96),
                ManufacturingState.AS_CAST);
            ItemStack componentStack = ItemGenericComponent.createStack(sampleComp);

            String geomName = formatGeometryName(geom);
            String moldEntryId = "mold_" + geom.id();

            registry.registerEntry(
                KnowledgeEntry.builder(moldEntryId, geomName + " Mold", KnowledgeCategory.CASTING)
                    .icon(moldStack)
                    .summary(
                        "Refractory ceramic mold cavity designed to cast molten metal into " + geomName.toLowerCase()
                            + "s.")
                    .description(
                        "The " + geomName
                            + " Mold is a precision refractory casting mold.\n\n"
                            + "\u00a7lCasting Procedure:\u00a7r\n"
                            + "1. Place the mold into a \u00a7eCasting Table\u00a7r positioned beneath a \u00a7eSmelter Outlet\u00a7r.\n"
                            + "2. Right-click the Smelter Outlet to open the valve and channel molten metal into the mold cavity.\n"
                            + "3. The liquid cools through thermodynamics into the ambient air (water nearby cools it faster).\n"
                            + "4. Once solidified, Right-Click the Casting Table with an empty hand to extract the cast component.\n\n"
                            + "\u00a7lRemelting Safety:\u00a7r\n"
                            + "If a mold is only partially filled or you wish to reclaim the metal, simply place the mold directly "
                            + "into the Starter Smelter crucible feed slot to remelt the contents back into liquid with zero loss!")
                    .addProperty("Geometry", geomName)
                    .addProperty("Cavity Volume", String.format("%.1f cm\u00b3", volumeCm3))
                    .addProperty("Copper Mass", String.format("%.2f kg", volumeCm3 * 8.96 / 1000.0))
                    .addProperty("Iron Mass", String.format("%.2f kg", volumeCm3 * 7.874 / 1000.0))
                    .addProperty("Extraction", "Right-Click table when solidified")
                    .addAcquisition(
                        ProcessStep.builder()
                            .operation("Mold Assembly Table")
                            .machine(moldAssemblyStack, "Mold Assembly Table")
                            .addInput(unshapedMoldStack, "Unshaped Blank Mold")
                            .addOutput(moldStack, geomName + " Mold")
                            .addNote("Choose this cavity shape at the Mold Assembly Table.")
                            .build())
                    .addUsage(
                        ProcessStep.builder()
                            .operation("Component Casting")
                            .machine(tableStack, "Casting Table")
                            .addInput(moldStack)
                            .addInput(outletStack, "Smelter Outlet (Molten Metal Stream)")
                            .addOutput(componentStack, "Cast " + geomName)
                            .addNote("Fill with molten metal, allow to cool, extract component.")
                            .build())
                    .addRelated("casting_table")
                    .addRelated("smelter_outlet")
                    .addRelated("starter_smelter")
                    .build());
        }

        // The unshaped body is the only mold form made in the crafting grid.
        registry.registerEntry(
            KnowledgeEntry.builder("unshaped_blank_mold", "Unshaped Blank Mold", KnowledgeCategory.CASTING)
                .icon(unshapedMoldStack)
                .summary("Unshaped ceramic mold body that is given a cavity at the Mold Assembly Table.")
                .description(
                    "This is mold stock, not a casting cavity. Insert it into the Mold Assembly Table and choose one of "
                        + "the supported cavity shapes before using it on a Casting Table.")
                .addProperty("State", "Unshaped")
                .addUsage(
                    ProcessStep.builder()
                        .operation("Mold shaping")
                        .machine(moldAssemblyStack, "Mold Assembly Table")
                        .addInput(unshapedMoldStack)
                        .addOutput(ItemCastingMold.createStack(GeometryId.INGOT), "Selected cavity mold")
                        .addNote("Select a supported cavity; the table consumes one unshaped mold body.")
                        .build())
                .addRelated("mold_ingot")
                .build());
    }

    private static String formatGeometryName(GeometryId geom) {
        switch (geom) {
            case BLANK:
                return "Ingot Blank";
            case PICK_HEAD:
                return "Pickaxe Head";
            case AXE_HEAD:
                return "Axe Head";
            case SHOVEL_HEAD:
                return "Shovel Head";
            case HOE_HEAD:
                return "Hoe Head";
            case BLADE:
                return "Tool Blade";
            case PLATE:
                return "Plate";
            case ROD:
                return "Rod";
            case HANDLE:
                return "Handle";
            default:
                return geom.name();
        }
    }
}
