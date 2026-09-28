package naturalexceptions.minecraft.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;

/**
 * Represents a single transformation or processing step (e.g. smelting ore into molten metal,
 * pouring into a mold, or assembling a tool).
 */
public final class ProcessStep {

    private final String operation;
    private final ItemStack machineStack;
    private final String machineName;
    private final List<ItemStack> inputs;
    private final List<String> inputDescriptions;
    private final List<ItemStack> outputs;
    private final List<String> outputDescriptions;
    private final List<String> notes;

    public ProcessStep(String operation, ItemStack machineStack, String machineName, List<ItemStack> inputs,
        List<String> inputDescriptions, List<ItemStack> outputs, List<String> outputDescriptions, List<String> notes) {
        this.operation = operation != null ? operation : "Process";
        this.machineStack = machineStack != null ? machineStack.copy() : null;
        this.machineName = machineName != null ? machineName
            : (machineStack != null ? machineStack.getDisplayName() : "Manual");
        this.inputs = inputs != null ? new ArrayList<>(inputs) : new ArrayList<>();
        this.inputDescriptions = inputDescriptions != null ? new ArrayList<>(inputDescriptions) : new ArrayList<>();
        this.outputs = outputs != null ? new ArrayList<>(outputs) : new ArrayList<>();
        this.outputDescriptions = outputDescriptions != null ? new ArrayList<>(outputDescriptions) : new ArrayList<>();
        this.notes = notes != null ? new ArrayList<>(notes) : new ArrayList<>();
    }

    public String operation() {
        return operation;
    }

    public ItemStack machineStack() {
        return machineStack != null ? machineStack.copy() : null;
    }

    public String machineName() {
        return machineName;
    }

    public List<ItemStack> inputs() {
        return Collections.unmodifiableList(inputs);
    }

    public List<String> inputDescriptions() {
        return Collections.unmodifiableList(inputDescriptions);
    }

    public List<ItemStack> outputs() {
        return Collections.unmodifiableList(outputs);
    }

    public List<String> outputDescriptions() {
        return Collections.unmodifiableList(outputDescriptions);
    }

    public List<String> notes() {
        return Collections.unmodifiableList(notes);
    }

    /**
     * Checks if this process step consumes the given stack as an input.
     */
    public boolean matchesInput(ItemStack query) {
        if (query == null) return false;
        for (ItemStack in : inputs) {
            if (isMatching(in, query)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this process step produces the given stack as an output.
     */
    public boolean matchesOutput(ItemStack query) {
        if (query == null) return false;
        for (ItemStack out : outputs) {
            if (isMatching(out, query)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Intelligent matching for vanilla items, generic components, molds, and tools.
     */
    public static boolean isMatching(ItemStack template, ItemStack query) {
        if (template == null || query == null) return false;
        if (template.getItem() == null || query.getItem() == null) return false;
        if (template.getItem() != query.getItem()) return false;

        // If generic component (e.g. pick head, ingot, handle)
        if (template.getItem() instanceof ItemGenericComponent) {
            naturalexceptions.core.fabrication.GeometryId templateGeom = ItemGenericComponent.getGeometry(template);
            naturalexceptions.core.fabrication.GeometryId queryGeom = ItemGenericComponent.getGeometry(query);
            if (templateGeom != null && queryGeom != null && templateGeom != queryGeom) {
                return false;
            }
            return true;
        }

        // If casting mold (e.g. pick head mold, blank mold)
        if (template.getItem() instanceof ItemCastingMold) {
            naturalexceptions.core.fabrication.GeometryId templateGeom = ItemCastingMold.getGeometry(template);
            naturalexceptions.core.fabrication.GeometryId queryGeom = ItemCastingMold.getGeometry(query);
            if (templateGeom != null && queryGeom != null && templateGeom != queryGeom) {
                return false;
            }
            return true;
        }

        // If generic assembly / tool
        if (template.getItem() instanceof ItemGenericAssembly) {
            return true;
        }

        // For regular items, check metadata/damage if not wildcard
        if (template.getItemDamage() != 32767 && template.getItemDamage() != query.getItemDamage()) {
            return false;
        }

        if (template.hasTagCompound() && !ItemStack.areItemStackTagsEqual(template, query)) return false;

        return true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String operation = "Process";
        private ItemStack machineStack;
        private String machineName;
        private final List<ItemStack> inputs = new ArrayList<>();
        private final List<String> inputDescriptions = new ArrayList<>();
        private final List<ItemStack> outputs = new ArrayList<>();
        private final List<String> outputDescriptions = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();

        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder machine(ItemStack machineStack, String machineName) {
            this.machineStack = machineStack;
            this.machineName = machineName;
            return this;
        }

        public Builder addInput(ItemStack stack) {
            if (stack != null) inputs.add(stack);
            return this;
        }

        public Builder addInput(ItemStack stack, String description) {
            if (stack != null) inputs.add(stack);
            if (description != null) inputDescriptions.add(description);
            return this;
        }

        public Builder addInputDescription(String description) {
            if (description != null) inputDescriptions.add(description);
            return this;
        }

        public Builder addOutput(ItemStack stack) {
            if (stack != null) outputs.add(stack);
            return this;
        }

        public Builder addOutput(ItemStack stack, String description) {
            if (stack != null) outputs.add(stack);
            if (description != null) outputDescriptions.add(description);
            return this;
        }

        public Builder addOutputDescription(String description) {
            if (description != null) outputDescriptions.add(description);
            return this;
        }

        public Builder addNote(String note) {
            if (note != null) notes.add(note);
            return this;
        }

        public ProcessStep build() {
            return new ProcessStep(
                operation,
                machineStack,
                machineName,
                inputs,
                inputDescriptions,
                outputs,
                outputDescriptions,
                notes);
        }
    }
}
