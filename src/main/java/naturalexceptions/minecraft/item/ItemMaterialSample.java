package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.ConditioningHistory;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.minecraft.persistence.MaterialCarrierNbtCodec;
import naturalexceptions.minecraft.persistence.PersistenceValidationException;
import naturalexceptions.minecraft.registry.NEItems;
import naturalexceptions.core.knowledge.KnowledgeEvidence;
import naturalexceptions.core.knowledge.KnowledgePrecision;
import naturalexceptions.core.knowledge.KnowledgeProvenance;
import naturalexceptions.core.knowledge.KnowledgeSubjectRef;
import naturalexceptions.core.knowledge.PlayerKnowledge;
import naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketKnowledgeSnapshot;

public final class ItemMaterialSample extends Item {
    private static final String KEY_MASS_MILLIGRAMS = "MatterMassMg";
    private static final String KEY_PROVENANCE = "MatterProvenance";

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
        return createStack(ref, stateKey, null);
    }

    public static ItemStack createStack(MaterialRef ref, CanonicalMaterialKey stateKey,
        naturalexceptions.core.thermal.ActiveThermalState thermalState) {
        MaterialQuantity mass = thermalState != null ? MaterialQuantity.ofKilograms(thermalState.massKg())
            : MaterialQuantity.ofGrams(100.0);
        return createStack(ref, stateKey, mass, thermalState);
    }

    public static ItemStack createStack(MaterialRef ref, CanonicalComposition composition, MaterialQuantity mass,
        naturalexceptions.core.thermal.ActiveThermalState thermalState) {
        return createStack(ref, composition != null ? CanonicalMaterialKey.of(composition) : null, mass, thermalState);
    }

    public static ItemStack createStack(MaterialRef ref, CanonicalMaterialKey stateKey, MaterialQuantity mass,
        naturalexceptions.core.thermal.ActiveThermalState thermalState) {
        if (mass == null || mass.isZero()) throw new IllegalArgumentException("Material samples need positive mass");
        if (thermalState != null && MaterialQuantity.ofKilograms(thermalState.massKg()).milligrams() != mass.milligrams())
            throw new IllegalArgumentException("Sample mass and thermal mass disagree");
        Item item = NEItems.materialSample != null ? NEItems.materialSample : new ItemMaterialSample();
        ItemStack stack = new ItemStack(item);
        NBTTagCompound nbt = new NBTTagCompound();
        MaterialCarrierNbtCodec.writeToNbt(ref, stateKey, thermalState, nbt);
        nbt.setLong(KEY_MASS_MILLIGRAMS, mass.milligrams());
        stack.setTagCompound(nbt);
        return stack;
    }

    /** Exact carrier mass; old carriers retain their historical 100 g fallback. */
    public static MaterialQuantity getMass(ItemStack stack) {
        if (stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey(KEY_MASS_MILLIGRAMS, 99)) {
            long milligrams = stack.getTagCompound().getLong(KEY_MASS_MILLIGRAMS);
            if (milligrams > 0L) return MaterialQuantity.ofMilligrams(milligrams);
            return MaterialQuantity.ZERO;
        }
        if (stack != null && stack.hasTagCompound()
            && stack.getTagCompound().hasKey(MaterialCarrierNbtCodec.KEY_THERMAL, 10)) {
            NBTTagCompound thermal = stack.getTagCompound().getCompoundTag(MaterialCarrierNbtCodec.KEY_THERMAL);
            double kilograms = thermal.getDouble(MaterialCarrierNbtCodec.KEY_THERMAL_MASS);
            if (Double.isFinite(kilograms) && kilograms > 0.0) return MaterialQuantity.ofKilograms(kilograms);
        }
        return MaterialQuantity.ofGrams(100.0);
    }

    public static void setProvenance(ItemStack stack, naturalexceptions.core.id.DeterministicId provenance) {
        if (stack == null) throw new IllegalArgumentException("Material carrier required");
        if (stack.getTagCompound() == null) stack.setTagCompound(new NBTTagCompound());
        if (provenance == null) stack.getTagCompound().removeTag(KEY_PROVENANCE);
        else stack.getTagCompound().setString(KEY_PROVENANCE, provenance.hex());
    }

    public static naturalexceptions.core.id.DeterministicId getProvenance(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound() || !stack.getTagCompound().hasKey(KEY_PROVENANCE, 8)) return null;
        try { return naturalexceptions.core.id.DeterministicId.parse(stack.getTagCompound().getString(KEY_PROVENANCE)); }
        catch (IllegalArgumentException invalid) { return null; }
    }

    public static naturalexceptions.core.thermal.ActiveThermalState getThermalState(ItemStack stack,
        naturalexceptions.core.property.MaterialProperties props) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        try {
            return MaterialCarrierNbtCodec.readThermalStateFromNbt(stack.getTagCompound(), props);
        } catch (PersistenceValidationException e) {
            return null;
        }
    }

    private static naturalexceptions.core.property.MaterialProperties propertiesFor(ItemStack stack) {
        MaterialRef ref = getMaterialRef(stack);
        if (ref != null && ref.isAuthored()) {
            naturalexceptions.core.material.MaterialDefinition definition = BuiltinContent.materials()
                .getAuthored(ref);
            if (definition != null) return definition.properties();
        }
        CanonicalMaterialKey key = getStateKey(stack);
        if (key != null) {
            return new naturalexceptions.core.property.MaterialPropertyResolver().resolve(key)
                .properties();
        }
        return naturalexceptions.content.material.BuiltinMaterials.COPPER_PROPERTIES;
    }

    public static naturalexceptions.core.property.MaterialProperties propertiesForKnowledge(ItemStack stack) {
        MaterialRef ref = getMaterialRef(stack);
        if (ref == null) return null;
        if (ref.isAuthored()) {
            naturalexceptions.core.material.MaterialDefinition definition = BuiltinContent.materials()
                .getAuthored(ref);
            return definition != null ? definition.properties() : null;
        }
        CanonicalMaterialKey key = getStateKey(stack);
        return key != null ? new naturalexceptions.core.property.MaterialPropertyResolver().resolve(key).properties()
            : null;
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
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        MaterialRef ref = getMaterialRef(stack);
        if (ref == null) {
            return 0xFFFFFF;
        }
        if (stack != null && stack.hasTagCompound()
            && MaterialCarrierNbtCodec.hasActiveThermalState(stack.getTagCompound())) {
            try {
                naturalexceptions.core.thermal.ActiveThermalState thermal = MaterialCarrierNbtCodec
                    .readThermalStateFromNbt(stack.getTagCompound(), propertiesFor(stack));
                if (thermal != null && thermal.phase() == naturalexceptions.core.material.MaterialPhase.LIQUID) {
                    return naturalexceptions.core.material.MaterialAppearance
                        .getMoltenColor(ref, getStateKey(stack), thermal.temperatureKelvin());
                }
            } catch (Exception ignored) {}
        }
        return naturalexceptions.core.material.MaterialAppearance.getSolidColor(ref, getStateKey(stack));
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return "Material Sample (Empty)";
        }
        try {
            MaterialRef ref = MaterialCarrierNbtCodec.readRefFromNbt(stack.getTagCompound());
            if (ref == null) {
                return "Material Sample (Empty)";
            }
            CanonicalMaterialKey stateKey = getStateKey(stack);
            boolean isLiquid = false;
            if (MaterialCarrierNbtCodec.hasActiveThermalState(stack.getTagCompound())) {
                naturalexceptions.core.thermal.ActiveThermalState thermal = MaterialCarrierNbtCodec
                    .readThermalStateFromNbt(stack.getTagCompound(), propertiesFor(stack));
                if (thermal != null && thermal.phase() == naturalexceptions.core.material.MaterialPhase.LIQUID) {
                    isLiquid = true;
                }
            }

            if (ref.isAuthored() && BuiltinContent.materials().getAuthored(ref) == null) {
                // Matter the game cannot name is described by what the eye sees, not as an error.
                return isLiquid ? "Unfamiliar Molten Matter" : "Unfamiliar Fragment";
            }

            return isLiquid ? "Molten Material Sample" : "Material Sample";
        } catch (PersistenceValidationException e) {
            return "Crumbled Sample";
        }
    }

    @Override
    public void onUpdate(ItemStack stack, net.minecraft.world.World world, net.minecraft.entity.Entity entity, int slot,
        boolean isSelected) {
        // Player aliases live only in PlayerKnowledge and are never inferred from ItemStack display names.
        if (world == null || world.isRemote || !isSelected || !(entity instanceof net.minecraft.entity.EntityLivingBase)
            || Math.floorMod(world.getTotalWorldTime(), 20L) != 0L
            || !MaterialCarrierNbtCodec.hasActiveThermalState(stack.getTagCompound())) return;
        try {
            naturalexceptions.core.thermal.ActiveThermalState thermal = MaterialCarrierNbtCodec
                .readThermalStateFromNbt(stack.getTagCompound(), propertiesFor(stack));
            if (thermal != null && thermal.temperatureKelvin() >= 373.15) {
                naturalexceptions.minecraft.biology.BiologicalEvents.applyThermalContact(
                    (net.minecraft.entity.EntityLivingBase) entity,
                    naturalexceptions.core.id.ResourceId.of("naturalexceptions", "right_arm"),
                    thermal.temperatureKelvin(), 12.0);
            }
        } catch (RuntimeException invalid) { /* Malformed carriers do not become a damage source. */ }
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, net.minecraft.world.World world, int x, int y,
        int z, int side, float hitX, float hitY, float hitZ) {
        if (MaterialNamingTarget.openIfSneaking(stack, world, player)) {
            return true;
        }
        return super.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, net.minecraft.world.World world, int x, int y, int z,
        int side, float hitX, float hitY, float hitZ) {
        if (MaterialNamingTarget.openIfSneaking(stack, world, player)) {
            return true;
        }
        return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, net.minecraft.world.World world, EntityPlayer player) {
        if (MaterialNamingTarget.openIfSneaking(stack, world, player)) {
            return stack;
        }
        observeSample(stack, world, player);
        return stack;
    }

    private static void observeSample(ItemStack stack, net.minecraft.world.World world, EntityPlayer player) {
        if (world == null || world.isRemote || player == null) return;
        MaterialRef ref = getMaterialRef(stack);
        if (ref == null) return;
        CanonicalMaterialKey key = getStateKey(stack);
        KnowledgeSubjectRef subject = KnowledgeSubjectRef.material(ref);
        PlayerKnowledgeProperties props = PlayerKnowledgeProperties.get(player);
        PlayerKnowledge knowledge = props.knowledge();
        String appearance = appearanceClass(MaterialAppearanceColor.color(ref, key));
        boolean changed = knowledge.addEvidence(subject, new KnowledgeEvidence("appearance",
            capitalize(appearance) + " metallic appearance", "inspection:appearance:" + appearance,
            KnowledgeProvenance.EXPERIMENT, KnowledgePrecision.QUALITATIVE));
        boolean liquid = isLiquidSample(stack);
        changed |= knowledge.addEvidence(subject, new KnowledgeEvidence("phase", "Observed as " + (liquid ? "liquid" : "solid"),
            "inspection:phase:" + liquid, KnowledgeProvenance.EXPERIMENT, KnowledgePrecision.QUALITATIVE));
        if (changed && player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            NENetwork.CHANNEL.sendTo(new PacketKnowledgeSnapshot(props), (net.minecraft.entity.player.EntityPlayerMP) player);
        }
        player.addChatMessage(new net.minecraft.util.ChatComponentText("\u00a77Observed a " + appearance
            + " metallic material, currently " + (liquid ? "liquid" : "solid") + "."));
    }

    private static boolean isLiquidSample(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound() || !MaterialCarrierNbtCodec.hasActiveThermalState(stack.getTagCompound())) return false;
        try {
            naturalexceptions.core.thermal.ActiveThermalState thermal = MaterialCarrierNbtCodec
                .readThermalStateFromNbt(stack.getTagCompound(), propertiesFor(stack));
            return thermal != null && thermal.phase() == naturalexceptions.core.material.MaterialPhase.LIQUID;
        } catch (Exception ignored) { return false; }
    }

    private static int appearanceColor(MaterialRef ref, CanonicalMaterialKey key) {
        return naturalexceptions.core.material.MaterialAppearance.getSolidColor(ref, key);
    }

    private static String appearanceClass(int color) {
        return naturalexceptions.minecraft.knowledge.KnowledgePresentation.appearance(color);
    }

    private static final class MaterialAppearanceColor {
        static int color(MaterialRef ref, CanonicalMaterialKey key) { return appearanceColor(ref, key); }
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, net.minecraft.creativetab.CreativeTabs tab, List list) {
        list.add(new ItemStack(item));
        list.add(createStack(naturalexceptions.content.material.BuiltinMaterials.COPPER.ref()));
        naturalexceptions.core.thermal.ActiveThermalState hotCu = naturalexceptions.core.thermal.ActiveThermalState
            .atTemperature(1400.0, 1.0, 0.1, naturalexceptions.content.material.BuiltinMaterials.COPPER_PROPERTIES);
        list.add(createStack(naturalexceptions.content.material.BuiltinMaterials.COPPER.ref(), null, hotCu));
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        if (stack == null || !stack.hasTagCompound()) {
            list.add("\u00a77Empty carrier");
            list.add("\u00a78Sneak-right-click a filled Foundry Ladle in air to take a sample.");
            return;
        }
        try {
            MaterialRef ref = MaterialCarrierNbtCodec.readRefFromNbt(stack.getTagCompound());
            CanonicalMaterialKey key = null;
            if (!ref.isAuthored()) {
                key = MaterialCarrierNbtCodec.readStateKeyFromNbt(stack.getTagCompound());
            }
            if (ref.isAuthored()) {
                if (BuiltinContent.materials()
                    .getAuthored(ref) == null) {
                    list.add("\u00a77Material definition is missing in this installation.");
                }
            }
            String appearance = appearanceClass(appearanceColor(ref, key));
            KnowledgeSubjectRef subject = KnowledgeSubjectRef.material(ref);
            PlayerKnowledge knowledge = player != null ? PlayerKnowledgeProperties.get(player).knowledge() : null;
            String matName = naturalexceptions.core.knowledge.KnowledgeDisplay.resolve(subject, knowledge,
                appearance, isLiquidSample(stack) ? "liquid" : "solid");
            list.add("  \u00a77Material: \u00a7f" + matName);
            MaterialQuantity mass = getMass(stack);
            list.add("  Weight: " + (mass.compareTo(MaterialQuantity.ofGrams(500.0)) >= 0
                ? "heavy fragment" : "small sample"));
            if (knowledge != null) {
                int shown = 0;
                for (KnowledgeEvidence evidence : knowledge.evidenceFor(subject)) {
                    if (shown++ >= 3) break;
                    list.add("  \u00a78" + evidence.statement() + " [" + evidence.provenance().name().toLowerCase() + "]");
                }
            }
            if (MaterialNamingTarget.canOpenNaming(stack)) {
                list.add("\u00a78Sneak-right-click to name generated material");
            }
            list.add("\u00a78Return to a Starter Smelter to remelt.");

            boolean showShift = false;
            try {
                if (org.lwjgl.input.Keyboard.isCreated()) {
                    showShift = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                        || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
                }
            } catch (Throwable ignored) {}

            if (MaterialCarrierNbtCodec.hasActiveThermalState(stack.getTagCompound())) {
                naturalexceptions.core.thermal.ActiveThermalState thermal = MaterialCarrierNbtCodec
                    .readThermalStateFromNbt(stack.getTagCompound(), propertiesFor(stack));
                if (thermal != null) {
                    list.add("  \u00a7eObserved state: " + (thermal.phase() == naturalexceptions.core.material.MaterialPhase.LIQUID
                        ? "Molten liquid" : "Solid fragment"));
                    if (thermal.temperatureKelvin() >= 1273.15) list.add("  \u00a7cGlowing hot; unsafe to handle directly");
                    else if (thermal.temperatureKelvin() >= 373.15) list.add("  \u00a7cDangerously hot to touch");
                    else if (thermal.temperatureKelvin() >= 310.15) list.add("  \u00a7eWarm to the touch");
                    else if (thermal.temperatureKelvin() <= 273.15) list.add("  \u00a79Cold to the touch");
                }
            }

            if (advanced) {
                list.add("\u00a78--- Developer diagnostics ---");
                if (key != null) {
                    list.add("  \u00a78A field assay can estimate density.");
                }
                if (advanced) {
                    list.add("  \u00a78ID: " + ref.asString());
                }
            } else {
                list.add("\u00a78Right-click to record an observation");
            }
        } catch (PersistenceValidationException e) {
            list.add("\u00a77Too crumbled to tell what it is.");
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
