package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.assembly.DerivedCapability;
import naturalexceptions.core.assembly.TargetMaterialProfile;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.minecraft.persistence.AssemblyNbtCodec;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Generic Minecraft item carrier representing an arbitrary physical tool assembly.
 */
public final class ItemGenericAssembly extends Item {

    public ItemGenericAssembly() {
        setUnlocalizedName("genericAssembly");
        setTextureName("naturalexceptions:blacksmith_hammer");
        setMaxStackSize(1);
    }

    public static ItemStack createStack(AssemblyGraph assembly) {
        Item item = NEItems.genericAssembly != null ? NEItems.genericAssembly : new ItemGenericAssembly();
        ItemStack stack = new ItemStack(item);
        NBTTagCompound nbt = new NBTTagCompound();
        AssemblyNbtCodec.writeToNbt(assembly, nbt);
        stack.setTagCompound(nbt);
        return stack;
    }

    public static AssemblyGraph getAssembly(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        try {
            return AssemblyNbtCodec.readFromNbt(stack.getTagCompound());
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveAssembly(ItemStack stack, AssemblyGraph assembly) {
        if (stack == null) return;
        NBTTagCompound nbt = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        AssemblyNbtCodec.writeToNbt(assembly, nbt);
        stack.setTagCompound(nbt);
    }

    @Override
    public float getDigSpeed(ItemStack stack, Block block, int meta) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null || assembly.isBroken()) {
            return 1.0F;
        }

        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        MaterialProperties headProps = resolver.resolve(
            assembly.head()
                .materialRef(),
            assembly.head()
                .stateKey()
                .composition())
            .properties();
        MaterialProperties handleProps = BuiltinMaterials.WOOD_PROPERTIES;

        DerivedCapability capability = new DerivedCapability(assembly, headProps, handleProps);
        TargetMaterialProfile target = mapBlockToTarget(block);

        return (float) capability.miningSpeed(target);
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z,
        EntityLivingBase player) {
        if (world.isRemote) {
            return true;
        }

        AssemblyGraph assembly = getAssembly(stack);
        if (assembly != null && !assembly.isBroken()) {
            MaterialPropertyResolver resolver = new MaterialPropertyResolver();
            MaterialProperties headProps = resolver.resolve(
                assembly.head()
                    .materialRef(),
                assembly.head()
                    .stateKey()
                    .composition())
                .properties();
            MaterialProperties handleProps = BuiltinMaterials.WOOD_PROPERTIES;

            DerivedCapability capability = new DerivedCapability(assembly, headProps, handleProps);
            TargetMaterialProfile target = mapBlockToTarget(block);

            capability.interactWith(target);
            saveAssembly(stack, assembly);
        }

        return true;
    }

    private static TargetMaterialProfile mapBlockToTarget(Block block) {
        if (block == NEBlocks.copperOre) {
            return TargetMaterialProfile.copperOre();
        }
        if (block.getMaterial() == net.minecraft.block.material.Material.rock) {
            return TargetMaterialProfile.stone();
        }
        if (block.getMaterial() == net.minecraft.block.material.Material.wood) {
            return TargetMaterialProfile.wood();
        }
        return TargetMaterialProfile.stone();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null) {
            list.add("\u00a77Empty assembly");
            return;
        }

        list.add(
            "\u00a76Tool Assembly: " + assembly.head()
                .geometryId()
                .id()
                + " + "
                + assembly.handle()
                    .geometryId()
                    .id());
        list.add(
            "  \u00a77Head Material: " + assembly.head()
                .materialRef()
                .asString());
        list.add(
            "  \u00a77Handle Material: " + assembly.handle()
                .materialRef()
                .asString());
        list.add("  \u00a77Total Mass: " + assembly.totalMass());

        if (assembly.isBroken()) {
            list.add("\u00a7cStatus: BROKEN (Component fractured)");
        } else {
            double wear = assembly.head()
                .damage()
                .wear() * 100.0;
            double deform = assembly.head()
                .damage()
                .deformation() * 100.0;
            double cracks = assembly.head()
                .damage()
                .cracking() * 100.0;
            list.add(
                String.format("  \u00a7eDamage: Wear %.1f%% | Deform %.1f%% | Cracks %.1f%%", wear, deform, cracks));
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null) {
            return "Tool Assembly";
        }
        try {
            String headMatName = formatMaterialName(
                assembly.head()
                    .materialRef());
            String handleMatName = formatMaterialName(
                assembly.handle()
                    .materialRef());
            String geomName = formatGeometryName(
                assembly.head()
                    .geometryId());

            if (assembly.isBroken()) {
                return "Broken " + headMatName + " " + geomName;
            }
            return headMatName + " + " + handleMatName + " " + geomName;
        } catch (Exception e) {
            return "Tool Assembly";
        }
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, net.minecraft.creativetab.CreativeTabs tab, List list) {
        list.add(new ItemStack(item));

        // Create sample Copper Pick
        naturalexceptions.core.fabrication.ComponentInstance cuHead = new naturalexceptions.core.fabrication.ComponentInstance(
            naturalexceptions.core.fabrication.GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey
                .of(CanonicalComposition.of(naturalexceptions.core.id.ResourceId.of("naturalexceptions", "copper"))),
            naturalexceptions.core.fabrication.ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            naturalexceptions.core.fabrication.ManufacturingState.FINISHED);

        naturalexceptions.core.fabrication.ComponentInstance woodHandle = new naturalexceptions.core.fabrication.ComponentInstance(
            naturalexceptions.core.fabrication.GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey
                .of(CanonicalComposition.of(naturalexceptions.core.id.ResourceId.of("naturalexceptions", "wood"))),
            naturalexceptions.core.metallurgy.MaterialQuantity.ofGrams(100.0),
            naturalexceptions.core.fabrication.ManufacturingState.FINISHED);

        AssemblyGraph cuPick = AssemblyGraph.ofTool(cuHead, woodHandle);
        list.add(createStack(cuPick));
    }

    private static String formatMaterialName(MaterialRef ref) {
        if (ref == null) return "Unknown";
        if (ref.isAuthored()) {
            String path = ref.authoredId()
                .path();
            return Character.toUpperCase(path.charAt(0)) + path.substring(1);
        }
        return "Generated Alloy";
    }

    private static String formatGeometryName(naturalexceptions.core.fabrication.GeometryId id) {
        if (id == null) return "Tool";
        switch (id) {
            case PICK_HEAD:
                return "Pick";
            case BLADE:
                return "Blade";
            case PLATE:
                return "Shield";
            default:
                return "Tool";
        }
    }
}
