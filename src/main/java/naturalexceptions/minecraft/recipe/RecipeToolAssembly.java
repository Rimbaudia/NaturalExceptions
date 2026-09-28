package naturalexceptions.minecraft.recipe;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Dynamic recipe handling tool assembly from head + handle components (or sticks).
 * Historical tool components are disassembled through the Assembly Table, which can return both parts.
 */
public final class RecipeToolAssembly implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        if (inv == null) {
            return false;
        }

        int count = 0;
        ItemStack stack1 = null;
        ItemStack stack2 = null;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s != null) {
                count++;
                if (count == 1) {
                    stack1 = s;
                } else if (count == 2) {
                    stack2 = s;
                } else {
                    return false;
                }
            }
        }

        // A crafting recipe has only one container-item remainder slot and would silently lose
        // the handle. The Assembly Table is the lossless disassembly path.
        if (count == 1) {
            return false;
        }

        // Case 2: Assembly (1 head component + 1 handle component or stick)
        if (count == 2) {
            ComponentInstance head = extractHead(stack1);
            ComponentInstance handle = extractHandle(stack2);

            if (head == null || handle == null) {
                head = extractHead(stack2);
                handle = extractHandle(stack1);
            }

            return head != null && handle != null;
        }

        return false;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        if (inv == null) {
            return null;
        }

        int count = 0;
        ItemStack stack1 = null;
        ItemStack stack2 = null;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s != null) {
                count++;
                if (count == 1) {
                    stack1 = s;
                } else if (count == 2) {
                    stack2 = s;
                }
            }
        }

        // Assembly: return combined tool assembly
        if (count == 2) {
            ComponentInstance head = extractHead(stack1);
            ComponentInstance handle = extractHandle(stack2);

            if (head == null || handle == null) {
                head = extractHead(stack2);
                handle = extractHandle(stack1);
            }

            if (head != null && handle != null) {
                AssemblyGraph assembly = AssemblyGraph.ofTool(head, handle);
                return ItemGenericAssembly.createStack(assembly);
            }
        }

        return null;
    }

    @Override
    public int getRecipeSize() {
        return 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(NEItems.genericAssembly != null ? NEItems.genericAssembly : new ItemGenericAssembly());
    }

    public static ComponentInstance extractHead(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemGenericComponent)) {
            return null;
        }
        ComponentInstance comp = ItemGenericComponent.getComponent(stack);
        if (comp == null || comp.geometryId() == null) {
            return null;
        }
        GeometryId geom = comp.geometryId();
        // Heads are working components (not handles, rods, or raw ingots)
        if (geom != GeometryId.HANDLE && geom != GeometryId.ROD && geom != GeometryId.BLANK) {
            return comp;
        }
        return null;
    }

    public static ComponentInstance extractHandle(ItemStack stack) {
        if (stack == null) {
            return null;
        }

        if (stack.getItem() instanceof ItemGenericComponent) {
            ComponentInstance comp = ItemGenericComponent.getComponent(stack);
            if (comp != null && comp.geometryId() != null) {
                GeometryId geom = comp.geometryId();
                if (geom == GeometryId.HANDLE || geom == GeometryId.ROD) {
                    return comp;
                }
            }
        }

        // Vanilla stick fallback -> Wood Handle component
        if (stack.getItem() == Items.stick) {
            return new ComponentInstance(
                GeometryId.HANDLE,
                BuiltinMaterials.WOOD.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
                MaterialQuantity.ofGrams(100.0),
                ManufacturingState.FINISHED);
        }

        return null;
    }
}
