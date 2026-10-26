package naturalexceptions.minecraft.fabrication;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Castings forged up into a full ingot at the anvil. An ingot mould's casting is a fifth of an ingot, about 0.22 kg;
 * laid one on another on the anvil they pile up, and once the pile holds an ingot's worth (1 kg) the hammer folds an
 * ingot off it: one blow while it is hot enough to work, three cold. What is left over stays on the anvil. Copper's
 * castings make NE's copper ingot; iron's and gold's, Minecraft's. Mass is kept throughout.
 */
public final class CastingFold {

    public static final MaterialQuantity INGOT = MaterialQuantity.ofKilograms(1.0);
    /** Blows to fold an ingot off a pile: hot, one; cold, this many. */
    public static final int COLD_BLOWS = 3;

    private CastingFold() {}

    /** The ingot a pure metal's castings fold into, or null if they fold into none. */
    public static Item ingotFor(CanonicalComposition comp) {
        if (comp == null) return null;
        if (BuiltinMaterials.COPPER_COMPOSITION.equals(comp)) return NEItems.copperIngot;
        if (CanonicalComposition.of(BuiltinMaterials.IRON_RESOURCE_ID)
            .equals(comp)) return Items.iron_ingot;
        if (CanonicalComposition.of(BuiltinMaterials.GOLD_RESOURCE_ID)
            .equals(comp)) return Items.gold_ingot;
        return null;
    }

    private static ComponentInstance casting(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemGenericComponent)) return null;
        ComponentInstance part = ItemGenericComponent.getComponent(stack);
        if (part == null || part.geometryId() != GeometryId.INGOT
            || part.damage()
                .isFractured()
            || part.stateKey() == null) return null;
        return ingotFor(
            part.stateKey()
                .composition()) == null ? null : part;
    }

    /** Whether {@code stack} is a casting that folds into an ingot. */
    public static boolean isCasting(ItemStack stack) {
        return casting(stack) != null;
    }

    /** Whether {@code added} can be laid on {@code pile}: castings of the same metal, neither molten. */
    public static boolean joins(ItemStack pile, ItemStack added) {
        ComponentInstance a = casting(pile), b = casting(added);
        if (a == null || b == null || !a.stateKey()
            .composition()
            .equals(
                b.stateKey()
                    .composition()))
            return false;
        return !molten(pile) && !molten(added);
    }

    private static boolean molten(ItemStack stack) {
        MaterialProperties props = ItemGenericComponent.resolveProperties(ItemGenericComponent.getComponent(stack));
        return ItemGenericComponent.getTemperatureKelvin(stack) >= props.meltingPointKelvin();
    }

    /** The pile with {@code added} laid on it: one casting of both their mass and heat. */
    public static ItemStack join(ItemStack pile, ItemStack added) {
        ComponentInstance a = casting(pile), b = casting(added);
        return piece(a, a.mass().add(b.mass()), joules(pile) + joules(added));
    }

    private static double joules(ItemStack stack) {
        ActiveThermalState thermal = ItemGenericComponent.getThermalState(stack);
        if (thermal != null) return thermal.energyJoules();
        ComponentInstance part = ItemGenericComponent.getComponent(stack);
        return naturalexceptions.core.thermal.ThermodynamicModel.energyForTemperature(
            293.15,
            0.0,
            part.mass()
                .toKilograms(),
            ItemGenericComponent.resolveProperties(part));
    }

    /** A casting like {@code like}, of {@code mass}, holding {@code joules}. */
    private static ItemStack piece(ComponentInstance like, MaterialQuantity mass, double joules) {
        ComponentInstance part = new ComponentInstance(
            GeometryId.INGOT,
            like.materialRef(),
            like.stateKey(),
            mass,
            ManufacturingState.AS_CAST);
        ItemStack stack = ItemGenericComponent.createStack(part);
        ItemGenericComponent.saveThermalState(
            stack,
            new ActiveThermalState(joules, mass.toKilograms(), ItemGenericComponent.resolveProperties(part)));
        return stack;
    }

    /** Whether a pile holds an ingot's worth. */
    public static boolean ready(ItemStack pile) {
        ComponentInstance part = casting(pile);
        return part != null && part.mass()
            .compareTo(INGOT) >= 0;
    }

    /** Blows it takes to fold an ingot off the pile as it is now: one hot (past recrystallization), more cold. */
    public static int blows(ItemStack pile) {
        ComponentInstance part = casting(pile);
        MaterialProperties props = ItemGenericComponent.resolveProperties(part);
        return ItemGenericComponent.getTemperatureKelvin(pile) >= props.recrystallizationTemperatureKelvin() ? 1
            : COLD_BLOWS;
    }

    /** Folds an ingot off the pile: {ingot, what is left on the anvil (null if nothing)}. */
    public static ItemStack[] fold(ItemStack pile) {
        ComponentInstance part = casting(pile);
        MaterialQuantity left = part.mass()
            .subtract(INGOT);
        double joules = joules(pile);
        ItemStack ingot = new ItemStack(
            ingotFor(
                part.stateKey()
                    .composition()));
        if (left.isZero()) return new ItemStack[] { ingot, null };
        double share = (double) left.milligrams() / (double) part.mass()
            .milligrams();
        return new ItemStack[] { ingot, piece(part, left, joules * share) };
    }
}
