package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.ConditioningHistory;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.minecraft.persistence.MaterialCarrierNbtCodec;
import naturalexceptions.minecraft.persistence.PersistenceValidationException;
import naturalexceptions.minecraft.registry.NEItems;

public final class ItemMaterialSample extends Item {

    public ItemMaterialSample() {
        setUnlocalizedName("materialSample");
        setTextureName("naturalexceptions:material_sample");
        setMaxStackSize(64);
    }

    public static ItemStack createStack(MaterialRef ref) {
        return createStack(ref, (CanonicalMaterialKey) null);
    }

    public static ItemStack createStack(MaterialRef ref, CanonicalComposition comp) {
        return createStack(ref, comp != null ? CanonicalMaterialKey.of(comp) : null);
    }

    public static ItemStack createStack(MaterialRef ref, CanonicalMaterialKey stateKey) {
        Item item = NEItems.materialSample != null ? NEItems.materialSample : new ItemMaterialSample();
        ItemStack stack = new ItemStack(item);
        NBTTagCompound nbt = new NBTTagCompound();
        MaterialCarrierNbtCodec.writeToNbt(ref, stateKey, nbt);
        stack.setTagCompound(nbt);
        return stack;
    }

    public static MaterialRef getMaterialRef(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        try {
            return MaterialCarrierNbtCodec.readRefFromNbt(stack.getTagCompound());
        } catch (PersistenceValidationException e) {
            return null;
        }
    }

    public static CanonicalMaterialKey getStateKey(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        try {
            return MaterialCarrierNbtCodec.readStateKeyFromNbt(stack.getTagCompound());
        } catch (PersistenceValidationException e) {
            return null;
        }
    }

    public static CanonicalComposition getComposition(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        try {
            return MaterialCarrierNbtCodec.readCompositionFromNbt(stack.getTagCompound());
        } catch (PersistenceValidationException e) {
            return null;
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return "Material Sample (Empty)";
        }
        try {
            MaterialRef ref = MaterialCarrierNbtCodec.readRefFromNbt(stack.getTagCompound());
            if (ref.isAuthored()) {
                if (BuiltinContent.materials()
                    .getAuthored(ref) == null) {
                    return "\u00a7cMaterial Sample (Unresolved Authored: " + ref.authoredId()
                        .path() + ")";
                }
                return "Material Sample (" + capitalize(
                    ref.authoredId()
                        .path())
                    + ")";
            } else {
                return "Material Sample (Generated " + ref.generatedId()
                    .hex()
                    .substring(0, 8) + ")";
            }
        } catch (PersistenceValidationException e) {
            return "\u00a7cMaterial Sample (Corrupted)";
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        if (stack == null || !stack.hasTagCompound()) {
            list.add("\u00a77Empty carrier");
            return;
        }
        try {
            MaterialRef ref = MaterialCarrierNbtCodec.readRefFromNbt(stack.getTagCompound());
            list.add("\u00a78" + ref.asString());

            if (ref.isAuthored()) {
                if (BuiltinContent.materials()
                    .getAuthored(ref) == null) {
                    list.add("\u00a7cUnresolved authored material: " + ref.authoredId());
                }
            } else {
                CanonicalMaterialKey key = MaterialCarrierNbtCodec.readStateKeyFromNbt(stack.getTagCompound());
                if (key != null) {
                    list.add(
                        "\u00a77Phase: " + key.phase()
                            .id());
                    list.add(
                        "\u00a77Microstructure: " + key.microstructure()
                            .id());
                    if (key.conditioning() != ConditioningHistory.NONE) {
                        list.add(
                            "\u00a77Conditioning: " + key.conditioning()
                                .id());
                    }
                    list.add("\u00a77Composition:");
                    for (ResourceId constituent : key.composition()
                        .constituents()) {
                        list.add(
                            String.format(
                                "  \u00a77%s: %.2f%%",
                                constituent.path(),
                                key.composition()
                                    .fraction(constituent) * 100.0));
                    }
                }
            }
        } catch (PersistenceValidationException e) {
            list.add("\u00a7cCorrupted data: " + e.getMessage());
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
