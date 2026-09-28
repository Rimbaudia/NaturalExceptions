package naturalexceptions.minecraft.biology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.biology.ArmorLayer;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.minecraft.persistence.AssemblyNbtCodec;

/** Adapts ordinary equipped armor stacks into temporary P5 component-backed P10 interception layers. */
public final class VanillaArmorAdapter {
    private static final String KEY_COMPONENT = "NEPhysicalArmorComponent";
    private static final ResourceId HEAD = ResourceId.of("naturalexceptions", "head");
    private static final ResourceId TORSO = ResourceId.of("naturalexceptions", "torso");
    private static final ResourceId LEFT_ARM = ResourceId.of("naturalexceptions", "left_arm");
    private static final ResourceId RIGHT_ARM = ResourceId.of("naturalexceptions", "right_arm");
    private static final ResourceId LEFT_LEG = ResourceId.of("naturalexceptions", "left_leg");
    private static final ResourceId RIGHT_LEG = ResourceId.of("naturalexceptions", "right_leg");
    private static final MaterialProperties LEATHER = MaterialProperties.builder().density(0.9).hardness(0.7)
        .thermalConductivity(0.2).specificHeatSolid(1500.0).build();
    private static final MaterialProperties DIAMOND = MaterialProperties.builder().density(3.5).hardness(10.0)
        .thermalConductivity(1000.0).build();

    private VanillaArmorAdapter() { }

    public static final class EquippedArmor {
        private final ItemStack stack;
        private final ArmorLayer layer;
        private final double initialDamage;
        private EquippedArmor(ItemStack stack, ArmorLayer layer) {
            this.stack = stack;
            this.layer = layer;
            this.initialDamage = damageMeasure(layer.component());
        }
        public ArmorLayer layer() { return layer; }
        public void commit(EntityLivingBase wearer) {
            NBTTagCompound root = stack.getTagCompound() == null ? new NBTTagCompound() : stack.getTagCompound();
            NBTTagCompound payload = new NBTTagCompound();
            AssemblyNbtCodec.writeComponentToNbt(layer.component(), payload);
            root.setTag(KEY_COMPONENT, payload);
            stack.setTagCompound(root);
            if (wearer != null && damageMeasure(layer.component()) > initialDamage + 0.005
                && stack.getItemDamage() < stack.getMaxDamage()) {
                stack.damageItem(1, wearer);
            }
        }
    }

    public static List<EquippedArmor> capture(EntityLivingBase wearer) {
        if (wearer == null) return Collections.emptyList();
        List<EquippedArmor> captured = new ArrayList<>();
        for (int slot = 4; slot >= 1; slot--) {
            ItemStack stack = wearer.getEquipmentInSlot(slot);
            EquippedArmor armor = captureSlot(stack, slot, wearer.getUniqueID().toString());
            if (armor != null) captured.add(armor);
        }
        return captured;
    }

    /** Equipment slots follow 1=boots, 2=leggings, 3=chestplate, 4=helmet in Forge 1.7.10. */
    public static EquippedArmor captureSlot(ItemStack stack, int equipmentSlot, String wearerIdentity) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor) || equipmentSlot < 1 || equipmentSlot > 4)
            return null;
        ItemArmor item = (ItemArmor) stack.getItem();
        ArmorMaterial material = material(item.getArmorMaterial());
        CanonicalComposition composition = CanonicalComposition.of(material.resource);
        MaterialRef ref = material.reference != null ? material.reference : MaterialRef.authored(material.resource);
        ComponentInstance component = readComponent(stack);
        if (component == null) {
            component = new ComponentInstance(DeterministicId.derive("vanilla-armor", wearerIdentity,
                Integer.toString(equipmentSlot), material.resource.toString()), GeometryId.PLATE, ref,
                CanonicalMaterialKey.of(composition), mass(equipmentSlot), ManufacturingState.FINISHED,
                new naturalexceptions.core.fabrication.ComponentDamage());
        }
        Set<ResourceId> regions = coverage(equipmentSlot);
        double thickness = material.thickness;
        return new EquippedArmor(stack, new ArmorLayer(component, material.properties, thickness, regions));
    }

    private static ComponentInstance readComponent(ItemStack stack) {
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(KEY_COMPONENT, 10)) return null;
        try { return AssemblyNbtCodec.readComponentFromNbt(stack.getTagCompound().getCompoundTag(KEY_COMPONENT)); }
        catch (RuntimeException malformed) { return null; }
    }

    private static double damageMeasure(ComponentInstance component) {
        return component.damage().wear() + component.damage().cracking() + component.damage().deformation();
    }

    private static Set<ResourceId> coverage(int equipmentSlot) {
        Set<ResourceId> regions = new LinkedHashSet<>();
        if (equipmentSlot == 4) regions.add(HEAD);
        else if (equipmentSlot == 3) {
            regions.add(TORSO); regions.add(LEFT_ARM); regions.add(RIGHT_ARM);
        } else if (equipmentSlot == 2 || equipmentSlot == 1) {
            regions.add(LEFT_LEG); regions.add(RIGHT_LEG);
        }
        return regions;
    }

    private static MaterialQuantity mass(int equipmentSlot) {
        switch (equipmentSlot) {
            case 4: return MaterialQuantity.ofGrams(300.0);
            case 3: return MaterialQuantity.ofGrams(650.0);
            case 2: return MaterialQuantity.ofGrams(500.0);
            default: return MaterialQuantity.ofGrams(250.0);
        }
    }

    private static final class ArmorMaterial {
        final ResourceId resource;
        final MaterialRef reference;
        final MaterialProperties properties;
        final double thickness;
        ArmorMaterial(ResourceId resource, MaterialRef reference, MaterialProperties properties, double thickness) {
            this.resource = resource; this.reference = reference; this.properties = properties; this.thickness = thickness;
        }
    }

    private static ArmorMaterial material(ItemArmor.ArmorMaterial material) {
        switch (material) {
            case CLOTH:
                return new ArmorMaterial(ResourceId.of("minecraft", "leather"), null, LEATHER, 0.003);
            case CHAIN:
            case IRON:
                return new ArmorMaterial(BuiltinMaterials.IRON_RESOURCE_ID, BuiltinMaterials.IRON.ref(),
                    BuiltinMaterials.IRON_PROPERTIES, 0.006);
            case GOLD:
                return new ArmorMaterial(BuiltinMaterials.GOLD_RESOURCE_ID, BuiltinMaterials.GOLD.ref(),
                    BuiltinMaterials.GOLD_PROPERTIES, 0.004);
            case DIAMOND:
            default:
                return new ArmorMaterial(ResourceId.of("minecraft", "diamond"), null, DIAMOND, 0.007);
        }
    }
}
