package naturalexceptions.minecraft.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialDefinition;
import naturalexceptions.core.material.MaterialRef;

/** Selects the material being inspected: the working head of a tool, or the held material itself. */
public final class MaterialNamingTarget {

    private MaterialNamingTarget() {}

    private static ComponentInstance component(ItemStack stack) {
        if (stack == null) return null;
        if (stack.getItem() instanceof ItemGenericAssembly) {
            AssemblyGraph assembly = ItemGenericAssembly.getAssembly(stack);
            return assembly != null ? assembly.head() : null;
        }
        return stack.getItem() instanceof ItemGenericComponent ? ItemGenericComponent.getComponent(stack) : null;
    }

    public static MaterialRef getMaterialRef(ItemStack stack) {
        ComponentInstance component = component(stack);
        if (component != null) return component.materialRef();
        return stack != null && stack.getItem() instanceof ItemMaterialSample ? ItemMaterialSample.getMaterialRef(stack)
            : null;
    }

    public static CanonicalMaterialKey getStateKey(ItemStack stack) {
        ComponentInstance component = component(stack);
        if (component != null) return component.stateKey();
        CanonicalMaterialKey key = stack != null && stack.getItem() instanceof ItemMaterialSample
            ? ItemMaterialSample.getStateKey(stack)
            : null;
        if (key != null) return key;
        MaterialRef ref = getMaterialRef(stack);
        if (ref != null && ref.isAuthored()) {
            MaterialDefinition def = BuiltinContent.materials()
                .getAuthored(ref);
            if (def != null && def.standardComposition() != null) {
                return CanonicalMaterialKey.of(def.standardComposition());
            }
        }
        return null;
    }

    public static boolean canOpenNaming(ItemStack stack) {
        if (stack == null) return false;
        MaterialRef ref = getMaterialRef(stack);
        if (ref == null) return false;
        if (ref.isGenerated()) return true;
        CanonicalMaterialKey key = getStateKey(stack);
        if (key != null && key.composition() != null
            && key.composition()
                .constituentCount() > 1) {
            return true;
        }
        return false;
    }

    public static boolean openIfSneaking(ItemStack stack, World world, EntityPlayer player) {
        if (player == null || !player.isSneaking() || !canOpenNaming(stack)) {
            return false;
        }
        if (world != null && !world.isRemote) {
            player.openGui(
                naturalexceptions.NaturalExceptions.instance,
                naturalexceptions.minecraft.gui.NEGuiHandler.GUI_ALLOY_NAMING,
                world,
                (int) player.posX,
                (int) player.posY,
                (int) player.posZ);
        }
        return true;
    }
}
