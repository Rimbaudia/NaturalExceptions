package naturalexceptions.minecraft.fabrication;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.persistence.AssemblyNbtCodec;
import naturalexceptions.minecraft.persistence.MaterialCarrierNbtCodec;
import naturalexceptions.minecraft.recipe.RecipeToolAssembly;

/**
 * The single, reversible mapping between a head + handle pair of component stacks and an assembled tool stack.
 * Component identity, material, mass, manufacturing state and damage live in the assembly graph; any other data
 * a component stack carried (provenance, names, history) is kept alongside and restored on disassembly. Transient
 * thermal state is not carried: an assembled tool equilibrates with its surroundings.
 */
public final class ToolAssemblyCarrier {

    /** Extra, non-graph stack data of the original parts, keyed {@code Head}/{@code Handle}. */
    public static final String KEY_PART_EXTRAS = "NEPartExtras";
    private static final Set<String> GRAPH_KEYS = new HashSet<>();

    static {
        GRAPH_KEYS.add(AssemblyNbtCodec.KEY_INSTANCE_ID);
        GRAPH_KEYS.add(AssemblyNbtCodec.KEY_GEOMETRY);
        GRAPH_KEYS.add(AssemblyNbtCodec.KEY_MATERIAL_REF);
        GRAPH_KEYS.add(AssemblyNbtCodec.KEY_STATE_KEY);
        GRAPH_KEYS.add(AssemblyNbtCodec.KEY_MASS_MG);
        GRAPH_KEYS.add(AssemblyNbtCodec.KEY_MANUFACTURING);
        GRAPH_KEYS.add(AssemblyNbtCodec.KEY_WEAR);
        GRAPH_KEYS.add(AssemblyNbtCodec.KEY_DEFORMATION);
        GRAPH_KEYS.add(AssemblyNbtCodec.KEY_CRACKING);
        GRAPH_KEYS.add(AssemblyNbtCodec.KEY_FRACTURED);
        GRAPH_KEYS.add(MaterialCarrierNbtCodec.KEY_THERMAL);
    }

    private ToolAssemblyCarrier() {}

    /** Builds the tool for one head and one handle stack, or null if they do not form a tool. */
    public static ItemStack assemble(ItemStack headStack, ItemStack handleStack) {
        ComponentInstance head = RecipeToolAssembly.extractHead(headStack);
        ComponentInstance handle = RecipeToolAssembly.extractHandle(handleStack);
        if (head == null || handle == null) return null;
        AssemblyGraph assembly;
        try {
            assembly = AssemblyGraph.ofTool(head, handle);
        } catch (RuntimeException invalid) {
            return null;
        }
        ItemStack tool = ItemGenericAssembly.createStack(assembly);
        NBTTagCompound extras = new NBTTagCompound();
        NBTTagCompound headExtras = extrasOf(headStack);
        NBTTagCompound handleExtras = extrasOf(handleStack);
        if (headExtras != null) extras.setTag("Head", headExtras);
        if (handleExtras != null) extras.setTag("Handle", handleExtras);
        if (!extras.hasNoTags()) tool.getTagCompound()
            .setTag(KEY_PART_EXTRAS, extras);
        return tool;
    }

    /** True if the stack is an assembled tool that can be taken apart. */
    public static boolean isDisassemblable(ItemStack tool) {
        return tool != null && tool.getItem() instanceof ItemGenericAssembly
            && ItemGenericAssembly.getAssembly(tool) != null;
    }

    /**
     * Splits one assembled tool into {head, handle} component stacks carrying the tool's current (worn) component
     * state and the parts' original extra data. Returns null if the stack is not an assembled tool.
     */
    public static ItemStack[] disassemble(ItemStack tool) {
        if (!isDisassemblable(tool)) return null;
        AssemblyGraph assembly = ItemGenericAssembly.getAssembly(tool);
        ItemStack head = ItemGenericComponent.createStack(assembly.head());
        ItemStack handle = ItemGenericComponent.createStack(assembly.handle());
        NBTTagCompound extras = tool.getTagCompound()
            .getCompoundTag(KEY_PART_EXTRAS);
        restore(head, extras.getCompoundTag("Head"));
        restore(handle, extras.getCompoundTag("Handle"));
        return new ItemStack[] { head, handle };
    }

    private static NBTTagCompound extrasOf(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemGenericComponent) || !stack.hasTagCompound()) return null;
        NBTTagCompound extras = new NBTTagCompound();
        for (Object key : stack.getTagCompound()
            .func_150296_c()) {
            String name = (String) key;
            if (!GRAPH_KEYS.contains(name)) extras.setTag(
                name,
                stack.getTagCompound()
                    .getTag(name)
                    .copy());
        }
        return extras.hasNoTags() ? null : extras;
    }

    private static void restore(ItemStack part, NBTTagCompound extras) {
        if (part == null || extras == null || extras.hasNoTags()) return;
        for (Object key : extras.func_150296_c()) {
            String name = (String) key;
            if (!part.getTagCompound()
                .hasKey(name))
                part.getTagCompound()
                    .setTag(
                        name,
                        extras.getTag(name)
                            .copy());
        }
    }
}
