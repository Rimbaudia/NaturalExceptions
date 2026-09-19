package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.minecraft.persistence.AssemblyNbtCodec;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Generic Minecraft item carrier representing a physical component instance.
 */
public final class ItemGenericComponent extends Item {

    @SideOnly(Side.CLIENT)
    private IIcon iconBlank;
    @SideOnly(Side.CLIENT)
    private IIcon iconPlate;
    @SideOnly(Side.CLIENT)
    private IIcon iconRod;
    @SideOnly(Side.CLIENT)
    private IIcon iconHandle;
    @SideOnly(Side.CLIENT)
    private IIcon iconPickHead;
    @SideOnly(Side.CLIENT)
    private IIcon iconBlade;

    public ItemGenericComponent() {
        setUnlocalizedName("genericComponent");
        setTextureName("naturalexceptions:copper_ingot");
        setMaxStackSize(16);
    }

    public static ItemStack createStack(ComponentInstance comp) {
        Item item = NEItems.genericComponent != null ? NEItems.genericComponent : new ItemGenericComponent();
        ItemStack stack = new ItemStack(item);
        if (comp != null) {
            NBTTagCompound nbt = new NBTTagCompound();
            AssemblyNbtCodec.writeComponentToNbt(comp, nbt);
            stack.setTagCompound(nbt);
        }
        return stack;
    }

    public static ComponentInstance getComponent(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        try {
            return AssemblyNbtCodec.readComponentFromNbt(stack.getTagCompound());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        ComponentInstance comp = getComponent(stack);
        if (comp == null) {
            return "Component";
        }
        try {
            String matName = formatMaterialName(comp.materialRef());
            String geomName = formatGeometryName(comp.geometryId());
            if (comp.damage()
                .isFractured()) {
                return "Fractured " + matName + " " + geomName;
            }
            return matName + " " + geomName;
        } catch (Exception e) {
            return "Component";
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        ComponentInstance comp = getComponent(stack);
        if (comp == null) {
            list.add("\u00a77Empty component carrier");
            return;
        }

        list.add("\u00a76Geometry: " + formatGeometryName(comp.geometryId()));
        list.add("  \u00a77Material: " + formatMaterialName(comp.materialRef()));
        list.add("  \u00a77Mass: " + comp.mass());
        list.add(
            "  \u00a77State: " + comp.manufacturingState()
                .id());

        if (comp.damage()
            .isFractured()) {
            list.add("\u00a7cStatus: FRACTURED");
        } else if (comp.damage()
            .isDamaged()) {
                double wear = comp.damage()
                    .wear() * 100.0;
                double def = comp.damage()
                    .deformation() * 100.0;
                double crk = comp.damage()
                    .cracking() * 100.0;
                list.add(String.format("  \u00a7eDamage: Wear %.1f%% | Deform %.1f%% | Cracks %.1f%%", wear, def, crk));
            } else {
                list.add("  \u00a7aCondition: Pristine");
            }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        this.itemIcon = reg.registerIcon("naturalexceptions:copper_ingot");
        this.iconBlank = this.itemIcon;
        this.iconPlate = reg.registerIcon("naturalexceptions:hammered_copper_plate");
        this.iconRod = reg.registerIcon("naturalexceptions:copper_rod");
        this.iconHandle = reg.registerIcon("naturalexceptions:component_handle");
        this.iconPickHead = reg.registerIcon("naturalexceptions:component_pick_head");
        this.iconBlade = reg.registerIcon("naturalexceptions:component_blade");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) {
        ComponentInstance comp = getComponent(stack);
        if (comp != null && comp.geometryId() != null) {
            switch (comp.geometryId()) {
                case PICK_HEAD:
                    return iconPickHead != null ? iconPickHead : itemIcon;
                case PLATE:
                    return iconPlate != null ? iconPlate : itemIcon;
                case ROD:
                    return iconRod != null ? iconRod : itemIcon;
                case HANDLE:
                    return iconHandle != null ? iconHandle : itemIcon;
                case BLADE:
                    return iconBlade != null ? iconBlade : itemIcon;
                case BLANK:
                default:
                    return iconBlank != null ? iconBlank : itemIcon;
            }
        }
        return this.itemIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ItemStack stack, int pass) {
        return getIconIndex(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        // Base / empty
        list.add(new ItemStack(item));

        // Copper Ingot / Blank
        ComponentInstance cuBlank = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.blank()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        list.add(createStack(cuBlank));

        // Copper Pick Head
        ComponentInstance cuHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        list.add(createStack(cuHead));

        // Copper Blade
        ComponentInstance cuBlade = new ComponentInstance(
            GeometryId.BLADE,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.blade()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        list.add(createStack(cuBlade));

        // Copper Plate
        ComponentInstance cuPlate = new ComponentInstance(
            GeometryId.PLATE,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.plate()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        list.add(createStack(cuPlate));

        // Copper Rod
        ComponentInstance cuRod = new ComponentInstance(
            GeometryId.ROD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.rod()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        list.add(createStack(cuRod));

        // Wood Handle
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "wood"))),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);
        list.add(createStack(woodHandle));
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

    private static String formatGeometryName(GeometryId id) {
        if (id == null) return "Part";
        switch (id) {
            case PICK_HEAD:
                return "Pick Head";
            case HANDLE:
                return "Handle";
            case BLADE:
                return "Blade";
            case PLATE:
                return "Plate";
            case ROD:
                return "Rod";
            case BLANK:
                return "Blank";
            default:
                return id.id();
        }
    }
}
