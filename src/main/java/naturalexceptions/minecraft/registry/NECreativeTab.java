package naturalexceptions.minecraft.registry;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class NECreativeTab extends CreativeTabs {

    public static final NECreativeTab INSTANCE = new NECreativeTab();

    private NECreativeTab() {
        super("naturalExceptions");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {
        return NEItems.copperIngot != null ? NEItems.copperIngot : net.minecraft.init.Items.iron_ingot;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void displayAllReleventItems(java.util.List list) {
        // 1. Machines & Foundry Blocks
        if (NEBlocks.starterSmelter != null) list.add(new net.minecraft.item.ItemStack(NEBlocks.starterSmelter));
        if (NEBlocks.smelterTuyere != null) list.add(new net.minecraft.item.ItemStack(NEBlocks.smelterTuyere));
        if (NEBlocks.smelterOutlet != null) list.add(new net.minecraft.item.ItemStack(NEBlocks.smelterOutlet));
        if (NEBlocks.castingTable != null) list.add(new net.minecraft.item.ItemStack(NEBlocks.castingTable));
        if (NEBlocks.moldAssemblyTable != null)
            list.add(new net.minecraft.item.ItemStack(NEBlocks.moldAssemblyTable));
        if (NEBlocks.assemblyTable != null) list.add(new net.minecraft.item.ItemStack(NEBlocks.assemblyTable));
        if (NEBlocks.forgingAnvil != null) list.add(new net.minecraft.item.ItemStack(NEBlocks.forgingAnvil));
        if (NEBlocks.refractoryBrick != null) list.add(new net.minecraft.item.ItemStack(NEBlocks.refractoryBrick));

        // 2. Materials
        if (NEBlocks.copperOre != null) list.add(new net.minecraft.item.ItemStack(NEBlocks.copperOre));
        if (NEBlocks.copperBlock != null) list.add(new net.minecraft.item.ItemStack(NEBlocks.copperBlock));
        if (NEItems.copperIngot != null) list.add(new net.minecraft.item.ItemStack(NEItems.copperIngot));
        if (NEItems.slagLump != null) list.add(new net.minecraft.item.ItemStack(NEItems.slagLump));
        if (NEItems.refractoryMix != null) list.add(new net.minecraft.item.ItemStack(NEItems.refractoryMix));
        if (NEItems.refractoryBrick != null) list.add(new net.minecraft.item.ItemStack(NEItems.refractoryBrick));
        if (NEItems.materialSample != null) {
            NEItems.materialSample.getSubItems(NEItems.materialSample, this, list);
        }

        // 3. Molds & Components
        if (NEItems.castingMold != null) {
            NEItems.castingMold.getSubItems(NEItems.castingMold, this, list);
        }
        if (NEItems.genericComponent != null) {
            NEItems.genericComponent.getSubItems(NEItems.genericComponent, this, list);
        }

        // 4. Dynamic Tools
        if (NEItems.genericAssembly != null) {
            NEItems.genericAssembly.getSubItems(NEItems.genericAssembly, this, list);
        }

        // 5. Equipment
        if (NEItems.blacksmithHammer != null) list.add(new net.minecraft.item.ItemStack(NEItems.blacksmithHammer));
        if (NEItems.blacksmithTongs != null) list.add(new net.minecraft.item.ItemStack(NEItems.blacksmithTongs));
        if (NEItems.foundryLadle != null) list.add(new net.minecraft.item.ItemStack(NEItems.foundryLadle));
        if (NEItems.bellows != null) list.add(new net.minecraft.item.ItemStack(NEItems.bellows));
        if (NEItems.fluidVessel != null) list.add(new net.minecraft.item.ItemStack(NEItems.fluidVessel));
        if (NEItems.bandage != null) list.add(new net.minecraft.item.ItemStack(NEItems.bandage));
        if (NEItems.exposureMask != null) list.add(new net.minecraft.item.ItemStack(NEItems.exposureMask));

        // 6. Knowledge & observation
        if (NEItems.startingManual != null) list.add(new net.minecraft.item.ItemStack(NEItems.startingManual));
        if (NEItems.foundryNotes != null) list.add(new net.minecraft.item.ItemStack(NEItems.foundryNotes));
        if (NEItems.fieldAssay != null) list.add(new net.minecraft.item.ItemStack(NEItems.fieldAssay));
        if (NEItems.knowledgeRecord != null) list.add(new net.minecraft.item.ItemStack(NEItems.knowledgeRecord));
    }
}
