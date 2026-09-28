package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialAppearance;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Minecraft item carrier representing a material-agnostic casting mold.
 *
 * <p>
 * Supports full physical casting lifecycle:
 * <ul>
 * <li>Receives measured mass of molten metal directly from vessel outlet.</li>
 * <li>Carries thermal energy and temperature of poured metal.</li>
 * <li>Cools naturally via Stefan-Boltzmann radiation and natural convection.</li>
 * <li>Solidifies through the latent heat of fusion plateau.</li>
 * <li>Extracts completed {@link ItemGenericComponent} upon right-click once solidified.</li>
 * </ul>
 */
public final class ItemCastingMold extends Item {

    public static final String KEY_GEOMETRY = "TargetGeometry";
    public static final String KEY_FILLED = "Filled";
    public static final String KEY_MATERIAL_REF = "MaterialRef";
    public static final String KEY_STATE_KEY = "StateKey";
    public static final String KEY_COMPOSITION = "Composition";
    public static final String KEY_MASS_MG = "MassMg";
    public static final String KEY_ENERGY = "EnergyJoules";
    public static final String KEY_DENSITY = "Density";
    public static final String KEY_SPECIFIC_HEAT = "SpecificHeat";
    public static final String KEY_SPECIFIC_HEAT_LIQUID = "SpecificHeatLiquid";
    public static final String KEY_MELT_POINT = "MeltPoint";
    public static final String KEY_LATENT_HEAT = "LatentHeat";

    @SideOnly(Side.CLIENT)
    private IIcon iconBlank;
    @SideOnly(Side.CLIENT)
    private IIcon iconPickHead;
    @SideOnly(Side.CLIENT)
    private IIcon iconPlate;
    @SideOnly(Side.CLIENT)
    private IIcon iconRod;
    @SideOnly(Side.CLIENT)
    private IIcon iconBlade;
    @SideOnly(Side.CLIENT)
    private IIcon iconHandle;

    @SideOnly(Side.CLIENT)
    private IIcon iconEmptyMask;
    @SideOnly(Side.CLIENT)
    private IIcon iconIngotFill;
    @SideOnly(Side.CLIENT)
    private IIcon iconBlankFill;
    @SideOnly(Side.CLIENT)
    private IIcon iconPickHeadFill;
    @SideOnly(Side.CLIENT)
    private IIcon iconPlateFill;
    @SideOnly(Side.CLIENT)
    private IIcon iconRodFill;
    @SideOnly(Side.CLIENT)
    private IIcon iconBladeFill;
    @SideOnly(Side.CLIENT)
    private IIcon iconHandleFill;


    public ItemCastingMold() {
        setUnlocalizedName("castingMold");
        setTextureName("naturalexceptions:ingot_casting_mold");
        setMaxStackSize(16);
    }

    public static ItemStack createStack(GeometryId geometry) {
        Item item = NEItems.castingMold != null ? NEItems.castingMold : new ItemCastingMold();
        ItemStack stack = new ItemStack(item);
        if (geometry != null) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setString(KEY_GEOMETRY, geometry.id());
            stack.setTagCompound(nbt);
        }
        return stack;
    }

    public static GeometryId getGeometry(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(KEY_GEOMETRY, 8)) {
            return null;
        }
        return GeometryId.parse(
            stack.getTagCompound()
                .getString(KEY_GEOMETRY));
    }

    public static boolean hasMetal(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return false;
        }
        return stack.getTagCompound()
            .getLong(KEY_MASS_MG) > 0L || isFilled(stack);
    }

    public static boolean isFilled(ItemStack stack) {
        return stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .getBoolean(KEY_FILLED);
    }

    public static MaterialQuantity getFilledMass(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return MaterialQuantity.ZERO;
        }
        long massMg = stack.getTagCompound()
            .getLong(KEY_MASS_MG);
        if (massMg <= 0L) {
            return MaterialQuantity.ZERO;
        }
        return MaterialQuantity.ofMilligrams(massMg);
    }

    public static MaterialQuantity getRequiredMass(ItemStack stack) {
        return getRequiredMass(stack, resolveContainedProperties(stack));
    }

    public static MaterialQuantity getRequiredMass(ItemStack stack, MaterialProperties fallbackProps) {
        GeometryId geom = getGeometry(stack);
        if (geom == null) {
            geom = GeometryId.BLANK;
        }
        ComponentGeometry geomDef = ComponentGeometry.forId(geom);
        double density = fallbackProps != null ? fallbackProps.density() : 5.0;
        return geomDef.requiredMass(density);
    }

    public static MaterialRef getContainedMaterialRef(ItemStack stack) {
        if (!hasMetal(stack)) {
            return null;
        }
        try {
            return MaterialRef.parse(
                stack.getTagCompound()
                    .getString(KEY_MATERIAL_REF));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static CanonicalMaterialKey getContainedStateKey(ItemStack stack) {
        if (!hasMetal(stack) || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        String stateKey = nbt.getString(KEY_STATE_KEY);
        if (stateKey != null && !stateKey.isEmpty()) {
            try {
                return CanonicalMaterialKey.parse(stateKey);
            } catch (RuntimeException ignored) {
                // Fall through to legacy reconstruction
            }
        }
        if (nbt.hasKey(KEY_COMPOSITION)) {
            try {
                CanonicalComposition comp = CanonicalComposition.parse(nbt.getString(KEY_COMPOSITION));
                return CanonicalMaterialKey.of(comp);
            } catch (RuntimeException ignored) {}
        }
        MaterialRef ref = getContainedMaterialRef(stack);
        if (ref != null && ref.isAuthored()) {
            try {
                return CanonicalMaterialKey.of(CanonicalComposition.of(ref.authoredId()));
            } catch (RuntimeException ignored) {}
        }
        return null;
    }

    public static ActiveThermalState getThermalState(ItemStack stack) {
        if (!hasMetal(stack)) {
            return null;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        double energy = nbt.getDouble(KEY_ENERGY);
        long massMg = nbt.getLong(KEY_MASS_MG);
        if (massMg <= 0L) {
            return null;
        }
        double massKg = (double) massMg / MaterialQuantity.MILLIGRAMS_PER_KILOGRAM;
        MaterialProperties defaults = resolveContainedProperties(stack);
        double density = nbt.hasKey(KEY_DENSITY) ? nbt.getDouble(KEY_DENSITY) : defaults.density();
        double spHeat = nbt.hasKey(KEY_SPECIFIC_HEAT) ? nbt.getDouble(KEY_SPECIFIC_HEAT) : defaults.specificHeatSolid();
        double spHeatLiq = nbt.hasKey(KEY_SPECIFIC_HEAT_LIQUID) ? nbt.getDouble(KEY_SPECIFIC_HEAT_LIQUID)
            : defaults.specificHeatLiquid();
        double meltPoint = nbt.hasKey(KEY_MELT_POINT) ? nbt.getDouble(KEY_MELT_POINT) : defaults.meltingPointKelvin();
        double latentHeat = nbt.hasKey(KEY_LATENT_HEAT) ? nbt.getDouble(KEY_LATENT_HEAT)
            : defaults.latentHeatOfFusion();

        java.util.Map<String, Double> map = new java.util.HashMap<>();
        map.put(MaterialProperties.KEY_DENSITY, density);
        map.put(MaterialProperties.KEY_HARDNESS, 3.0);
        map.put(MaterialProperties.KEY_SPECIFIC_HEAT_SOLID, spHeat);
        map.put(MaterialProperties.KEY_SPECIFIC_HEAT_LIQUID, spHeatLiq);
        map.put(MaterialProperties.KEY_MELTING_POINT, meltPoint);
        map.put(MaterialProperties.KEY_LATENT_HEAT_FUSION, latentHeat);
        MaterialProperties props = new MaterialProperties(map);
        return new ActiveThermalState(energy, massKg, props);
    }

    private static MaterialProperties resolveContainedProperties(ItemStack stack) {
        MaterialRef ref = getContainedMaterialRef(stack);
        if (ref != null && stack.hasTagCompound()) {
            String stateKeyString = stack.getTagCompound()
                .getString(KEY_STATE_KEY);
            if (stateKeyString != null && !stateKeyString.isEmpty()) {
                try {
                    CanonicalMaterialKey key = CanonicalMaterialKey.parse(stateKeyString);
                    MaterialRef effectiveRef = ref != null ? ref : MaterialRef.generatedFrom(key);
                    return new MaterialPropertyResolver().resolve(effectiveRef, key.composition(), key.phase())
                        .properties();
                } catch (RuntimeException ignored) {
                    // Fall through to a neutral physical fallback for malformed legacy payloads.
                }
            }
            if (ref.isAuthored()) {
                try {
                    CanonicalComposition composition = CanonicalComposition.of(ref.authoredId());
                    return new MaterialPropertyResolver().resolve(ref, composition, MaterialPhase.SOLID)
                        .properties();
                } catch (RuntimeException ignored) {
                    // Fall through to a neutral physical fallback for unknown authored materials.
                }
            }
        }
        return MaterialProperties.of(5.0, 2.0, 10.0);
    }

    public static void saveThermalState(ItemStack stack, ActiveThermalState thermal) {
        if (stack == null || !stack.hasTagCompound() || thermal == null) {
            return;
        }
        stack.getTagCompound()
            .setDouble(KEY_ENERGY, thermal.energyJoules());
    }

    public static boolean fillMold(ItemStack stack, AggregateLayer pouredLayer) {
        if (stack == null || pouredLayer == null || isFilled(stack)) {
            return false;
        }
        NBTTagCompound nbt = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();

        MaterialProperties props = pouredLayer.thermalState()
            .properties();
        MaterialQuantity required = getRequiredMass(stack, props);
        MaterialQuantity currentMass = getFilledMass(stack);
        MaterialQuantity incomingMass = pouredLayer.mass();

        if (currentMass.isPositive()) {
            MaterialRef existingRef = getContainedMaterialRef(stack);
            if (existingRef != null && !existingRef.equals(pouredLayer.materialRef())) {
                return false;
            }
            MaterialQuantity newMass = currentMass.add(incomingMass);
            double currentEnergy = nbt.getDouble(KEY_ENERGY);
            double newEnergy = currentEnergy + pouredLayer.thermalState()
                .energyJoules();

            nbt.setLong(KEY_MASS_MG, newMass.milligrams());
            nbt.setDouble(KEY_ENERGY, newEnergy);

            nbt.setDouble(KEY_DENSITY, props.density());
            nbt.setDouble(KEY_SPECIFIC_HEAT, props.specificHeatSolid());
            nbt.setDouble(KEY_SPECIFIC_HEAT_LIQUID, props.specificHeatLiquid());
            nbt.setDouble(KEY_MELT_POINT, props.meltingPointKelvin());
            nbt.setDouble(KEY_LATENT_HEAT, props.latentHeatOfFusion());

            if (newMass.compareTo(required) >= 0) {
                nbt.setBoolean(KEY_FILLED, true);
            } else {
                nbt.setBoolean(KEY_FILLED, false);
            }
        } else {
            MaterialQuantity newMass = incomingMass;
            nbt.setString(
                KEY_MATERIAL_REF,
                pouredLayer.materialRef()
                    .asString());
            nbt.setString(
                KEY_STATE_KEY,
                pouredLayer.stateKey()
                    .canonicalString());
            nbt.setString(
                KEY_COMPOSITION,
                pouredLayer.composition()
                    .canonicalString());
            nbt.setLong(KEY_MASS_MG, newMass.milligrams());
            nbt.setDouble(
                KEY_ENERGY,
                pouredLayer.thermalState()
                    .energyJoules());

            nbt.setDouble(KEY_DENSITY, props.density());
            nbt.setDouble(KEY_SPECIFIC_HEAT, props.specificHeatSolid());
            nbt.setDouble(KEY_SPECIFIC_HEAT_LIQUID, props.specificHeatLiquid());
            nbt.setDouble(KEY_MELT_POINT, props.meltingPointKelvin());
            nbt.setDouble(KEY_LATENT_HEAT, props.latentHeatOfFusion());

            if (newMass.compareTo(required) >= 0) {
                nbt.setBoolean(KEY_FILLED, true);
            } else {
                nbt.setBoolean(KEY_FILLED, false);
            }
        }

        stack.setTagCompound(nbt);
        // Filled or filling molds do not stack
        stack.stackSize = 1;
        return true;
    }

    public static boolean isSolidified(ItemStack stack) {
        if (!hasMetal(stack)) {
            return false;
        }
        ActiveThermalState thermal = getThermalState(stack);
        if (thermal == null) {
            return true;
        }
        return thermal.phase() == MaterialPhase.SOLID && thermal.meltFraction() <= 0.0;
    }

    /** Returns a concise player-facing explanation for stale, incomplete, or recovered payload data. */
    public static String getPayloadDiagnostic(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        if (!isFilled(stack)) {
            if (hasMetal(stack)) {
                MaterialRef ref = getContainedMaterialRef(stack);
                CanonicalMaterialKey key = getContainedStateKey(stack);
                if (ref == null || key == null) {
                    return "Mold material data is corrupted; extraction is blocked.";
                }
                return "Incomplete casting; needs more metal.";
            }
            if (hasContainedPayloadTag(nbt)) {
                return "Stale cast residues present on empty mold.";
            }
            return null;
        }

        if (nbt.hasKey(KEY_STATE_KEY)) {
            String stateKeyStr = nbt.getString(KEY_STATE_KEY);
            if (stateKeyStr != null && !stateKeyStr.isEmpty()) {
                try {
                    CanonicalMaterialKey.parse(stateKeyStr);
                } catch (RuntimeException e) {
                    return "Mold data is incomplete; extraction is blocked.";
                }
            }
        }

        MaterialRef ref = getContainedMaterialRef(stack);
        CanonicalMaterialKey key = getContainedStateKey(stack);
        if (ref == null && key == null) {
            return "Mold payload is corrupted; extraction is blocked.";
        }
        if (ref == null || key == null) {
            return "Mold material data incomplete; extraction is blocked.";
        }

        boolean hasExplicitState = nbt.hasKey(KEY_STATE_KEY) && !nbt.getString(KEY_STATE_KEY)
            .isEmpty();
        if (!hasExplicitState) {
            return "Recovered mold contents from legacy data.";
        }

        if (!hasThermalPayload(nbt)) {
            return "Mold data incomplete; thermal properties reconstructed.";
        }
        return null;
    }

    public static boolean isRecovered(ItemStack stack) {
        if (!isFilled(stack) || !stack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        return !nbt.hasKey(KEY_STATE_KEY) || !hasThermalPayload(nbt);
    }

    public static boolean isScrapable(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return false;
        }
        String diag = getPayloadDiagnostic(stack);
        if (diag != null) {
            String lower = diag.toLowerCase();
            if (lower.contains("corrupt") || lower.contains("stale")
                || lower.contains("invalid")
                || (isFilled(stack) && lower.contains("extraction is blocked"))) {
                return true;
            }
        }
        if (hasMetal(stack)) {
            return getContainedMaterialRef(stack) == null || getContainedStateKey(stack) == null;
        }
        return hasContainedPayloadTag(stack.getTagCompound());
    }

    private static boolean hasContainedPayloadTag(NBTTagCompound nbt) {
        return nbt.hasKey(KEY_MATERIAL_REF) || nbt.hasKey(KEY_STATE_KEY)
            || nbt.hasKey(KEY_COMPOSITION)
            || nbt.hasKey(KEY_MASS_MG)
            || nbt.hasKey(KEY_ENERGY)
            || hasThermalPayload(nbt);
    }

    private static boolean hasThermalPayload(NBTTagCompound nbt) {
        return nbt.hasKey(KEY_DENSITY) && nbt.hasKey(KEY_SPECIFIC_HEAT)
            && nbt.hasKey(KEY_SPECIFIC_HEAT_LIQUID)
            && nbt.hasKey(KEY_MELT_POINT)
            && nbt.hasKey(KEY_LATENT_HEAT);
    }

    /** Clears every persisted field that belongs to the current contained cast, preserving the cavity identity. */
    public static void clearContainedPayload(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.removeTag(KEY_FILLED);
        nbt.removeTag(KEY_MATERIAL_REF);
        nbt.removeTag(KEY_STATE_KEY);
        nbt.removeTag(KEY_COMPOSITION);
        nbt.removeTag(KEY_MASS_MG);
        nbt.removeTag(KEY_ENERGY);
        nbt.removeTag(KEY_DENSITY);
        nbt.removeTag(KEY_SPECIFIC_HEAT);
        nbt.removeTag(KEY_SPECIFIC_HEAT_LIQUID);
        nbt.removeTag(KEY_MELT_POINT);
        nbt.removeTag(KEY_LATENT_HEAT);
    }

    public static ItemStack extractComponent(ItemStack stack) {
        if (!isFilled(stack) || !isSolidified(stack)) {
            return null;
        }
        String diag = getPayloadDiagnostic(stack);
        if (diag != null && diag.contains("extraction is blocked")) {
            return null;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        GeometryId geom = getGeometry(stack);
        if (geom == null) {
            geom = GeometryId.BLANK;
        }
        MaterialRef ref = getContainedMaterialRef(stack);
        CanonicalMaterialKey key = getContainedStateKey(stack);
        if (ref == null || key == null) {
            return null;
        }
        key = key.asSolidified();
        MaterialQuantity mass = MaterialQuantity.ofMilligrams(nbt.getLong(KEY_MASS_MG));

        ComponentInstance instance = new ComponentInstance(geom, ref, key, mass, ManufacturingState.AS_CAST);
        ItemStack componentStack = ItemGenericComponent.createStack(instance);

        ActiveThermalState thermal = getThermalState(stack);
        if (thermal != null) {
            ItemGenericComponent.saveThermalState(componentStack, thermal);
        }

        // Empty the mold while preserving its selected cavity geometry.
        clearContainedPayload(stack);

        return componentStack;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isCurrentItem) {
        if (hasMetal(stack)) {
            ActiveThermalState thermal = getThermalState(stack);
            if (thermal != null && thermal.temperatureKelvin() > 293.15) {
                double deltaSeconds = 0.05; // 1 tick
                double temp = thermal.temperatureKelvin();
                double surfaceArea = 0.04; // Hand mold cooling area
                double qRad = 0.80 * 5.670374419e-8
                    * surfaceArea
                    * (Math.pow(temp, 4.0) - Math.pow(293.15, 4.0))
                    * deltaSeconds;
                double qConv = 15.0 * surfaceArea * (temp - 293.15) * deltaSeconds;
                double totalLoss = Math.max(0.0, qRad + qConv);
                double ambientEnergy = naturalexceptions.core.thermal.ThermodynamicModel
                    .energyForTemperature(293.15, 0.0, thermal.massKg(), thermal.properties());
                if (thermal.energyJoules() - totalLoss < ambientEnergy) {
                    thermal.setEnergyJoules(ambientEnergy);
                } else {
                    thermal.removeEnergyJoules(totalLoss);
                }
                saveThermalState(stack, thermal);
            }
        }
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (hasMetal(stack)) {
            if (player.isSneaking() && isScrapable(stack)) {
                if (!world.isRemote) {
                    clearContainedPayload(stack);
                    player.inventoryContainer.detectAndSendChanges();
                    world.playSoundEffect(player.posX, player.posY, player.posZ, "step.gravel", 1.0F, 0.8F);
                    player.addChatMessage(
                        new ChatComponentText("\u00a7eScraped corrupted residues from mold cavity. Mold reclaimed."));
                }
                return true;
            }
            net.minecraft.block.Block block = world.getBlock(x, y, z);
            if (block instanceof naturalexceptions.minecraft.fabrication.BlockCastingTable
                || block instanceof naturalexceptions.minecraft.metallurgy.BlockStarterSmelter) {
                return false;
            }
            if (isSolidified(stack) && isFilled(stack)) {
                if (!world.isRemote) {
                    boolean recovered = isRecovered(stack);
                    ItemStack comp = extractComponent(stack);
                    if (comp != null) {
                        if (!player.inventory.addItemStackToInventory(comp)) {
                            player.dropPlayerItemWithRandomChoice(comp, false);
                        }
                        player.inventoryContainer.detectAndSendChanges();
                        world.playSoundEffect(player.posX, player.posY, player.posZ, "random.pop", 1.0F, 1.2F);
                        String msg = "\u00a7aExtracted " + comp.getDisplayName() + " from mold.";
                        if (recovered) {
                            msg += " \u00a77(Recovered from legacy data)";
                        }
                        player.addChatMessage(new ChatComponentText(msg));
                    } else {
                        player.addChatMessage(new ChatComponentText("\u00a7c" + extractionFailureMessage(stack)));
                    }
                }
                return true;
            } else if (!isFilled(stack)) {
                if (!world.isRemote) {
                    player.addChatMessage(new ChatComponentText("\u00a7c" + getPayloadDiagnostic(stack)));
                }
                return true;
            } else {
                if (!world.isRemote) {
                    player.addChatMessage(new ChatComponentText("\u00a7c" + hotStateMessage(getThermalState(stack))));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (hasMetal(stack)) {
            if (player.isSneaking() && isScrapable(stack)) {
                if (world == null || !world.isRemote) {
                    clearContainedPayload(stack);
                    player.inventoryContainer.detectAndSendChanges();
                    if (world != null) {
                        world.playSoundEffect(player.posX, player.posY, player.posZ, "step.gravel", 1.0F, 0.8F);
                    }
                    player.addChatMessage(
                        new ChatComponentText("\u00a7eScraped corrupted residues from mold cavity. Mold reclaimed."));
                }
                return stack;
            }
            if (isSolidified(stack) && isFilled(stack)) {
                if (world == null || !world.isRemote) {
                    boolean recovered = isRecovered(stack);
                    ItemStack comp = extractComponent(stack);
                    if (comp != null) {
                        ActiveThermalState thermal = ItemGenericComponent.getThermalState(comp);
                        if (thermal != null && thermal.temperatureKelvin() > 373.15) {
                            player.attackEntityFrom(net.minecraft.util.DamageSource.onFire, 1.0F);
                            player.addChatMessage(
                                new ChatComponentText(
                                    "\u00a7cOuch! The cast component is scalding hot! Let it cool or use blacksmith tongs."));
                        }
                        if (!player.inventory.addItemStackToInventory(comp)) {
                            player.dropPlayerItemWithRandomChoice(comp, false);
                        }
                        player.inventoryContainer.detectAndSendChanges();
                        if (world != null) {
                            world.playSoundEffect(player.posX, player.posY, player.posZ, "random.pop", 1.0F, 1.2F);
                        }
                        String msg = "\u00a7aExtracted " + comp.getDisplayName() + " from mold.";
                        if (recovered) {
                            msg += " \u00a77(Recovered from legacy data)";
                        }
                        player.addChatMessage(new ChatComponentText(msg));
                    } else {
                        player.addChatMessage(new ChatComponentText("\u00a7c" + extractionFailureMessage(stack)));
                    }
                }
            } else if (!isFilled(stack)) {
                if (world == null || !world.isRemote) {
                    player.addChatMessage(new ChatComponentText("\u00a7c" + getPayloadDiagnostic(stack)));
                }
            } else {
                if (world == null || !world.isRemote) {
                    player.addChatMessage(new ChatComponentText("\u00a7c" + hotStateMessage(getThermalState(stack))));
                }
            }
        }
        return stack;
    }

    private static String hotStateMessage(ActiveThermalState thermal) {
        if (thermal == null) {
            return "The cast metal's thermal state is unavailable; extraction is blocked.";
        }
        return "The metal in the mold is " + HeatWords.describe(thermal.temperatureKelvin())
            + (thermal.phase() == naturalexceptions.core.material.MaterialPhase.LIQUID ? " and still liquid" : "")
            + ". Let it cool and set first.";
    }

    private static String extractionFailureMessage(ItemStack stack) {
        String diagnostic = getPayloadDiagnostic(stack);
        if (diagnostic != null) {
            return diagnostic + " (Sneak right-click on solid block to scrape clean)";
        }
        return "Mold contents could not be reconstructed; extraction is blocked.";
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        GeometryId geom = getGeometry(stack);
        String geomName = formatGeometryName(geom);
        if (isFilled(stack)) {
            if (isSolidified(stack)) {
                return "Solidified Casting Mold (" + geomName + ")";
            } else {
                return "Filled Casting Mold (" + geomName + ")";
            }
        }
        if (geom == null) {
            return "Unshaped Blank Mold";
        }
        return "Casting Mold (" + geomName + ")";
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        GeometryId geom = getGeometry(stack);
        String geomName = formatGeometryName(geom);
        list.add("\u00a76Cavity: " + geomName);

        if (hasMetal(stack)) {
            MaterialRef ref = getContainedMaterialRef(stack);
            MaterialQuantity mass = getFilledMass(stack);
            MaterialQuantity req = getRequiredMass(stack);
            ActiveThermalState thermal = getThermalState(stack);
            double temp = thermal != null ? thermal.temperatureKelvin() : 293.15;
            double celsius = temp - 273.15;
            MaterialProperties moldProps = thermal != null ? thermal.properties() : resolveContainedProperties(stack);
            ItemGenericComponent.ThermalCondition cond = ItemGenericComponent.getThermalCondition(temp, moldProps);

            list.add(
                "  \u00a77Material: " + naturalexceptions.minecraft.knowledge.KnowledgePresentation
                    .materialName(player, ref, getContainedStateKey(stack)));
            list.add("  \u00a77Fill: " + (isFilled(stack) ? "Full" : mass.isPositive() ? "Partially filled" : "Empty"));
            list.add("  \u00a77Heat: " + cond.label());

            if (!isFilled(stack)) {
                double fillPct = req.isPositive() ? (double) mass.milligrams() / (double) req.milligrams() * 100.0
                    : 0.0;
                list.add("  \u00a7eStatus: Partially filled; needs more metal");
            } else if (isSolidified(stack)) {
                list.add("  \u00a7aStatus: Solidified! (Right-click to extract)");
            } else {
                double meltPct = thermal != null ? thermal.meltFraction() * 100.0 : 100.0;
                list.add("  \u00a7eStatus: Hot and molten");
            }

            boolean showAdvanced = advanced;
            try {
                if (org.lwjgl.input.Keyboard.isCreated()) {
                    showAdvanced |= org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                        || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
                }
            } catch (Throwable ignored) {}

            if (advanced) {
                list.add("\u00a78--- Developer diagnostics ---");
                list.add(String.format("  \u00a78Temperature: %.1f K (%.1f \u00b0C)", temp, celsius));
            }
            String diagnostic = getPayloadDiagnostic(stack);
            if (diagnostic != null) {
                if (isScrapable(stack)) {
                    list.add("  \u00a7cNotice: " + diagnostic);
                    list.add("  \u00a77(Sneak right-click on block to scrape clean)");
                } else {
                    list.add("  \u00a7eNotice: " + diagnostic);
                }
            }
            return;
        }

        String emptyDiag = getPayloadDiagnostic(stack);
        if (emptyDiag != null) {
            list.add("  \u00a7eNotice: " + emptyDiag);
        }
        double volume = getVolume(geom);
        list.add(String.format("  \u00a77Volume: %.1f cm\u00b3", volume));
        list.add("  \u00a77Status: Empty");
        list.add("  \u00a77Accepts: Any compatible molten metal");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        this.itemIcon = reg.registerIcon("naturalexceptions:ingot_casting_mold");
        this.iconBlank = reg.registerIcon("naturalexceptions:casting_mold");
        this.iconPickHead = reg.registerIcon("naturalexceptions:mold_pick_head");
        this.iconPlate = reg.registerIcon("naturalexceptions:mold_plate");
        this.iconRod = reg.registerIcon("naturalexceptions:mold_rod");
        this.iconBlade = reg.registerIcon("naturalexceptions:mold_blade");
        this.iconHandle = reg.registerIcon("naturalexceptions:mold_handle");

        this.iconEmptyMask = reg.registerIcon("naturalexceptions:empty_mask");
        this.iconIngotFill = reg.registerIcon("naturalexceptions:mold_ingot_fill");
        this.iconBlankFill = this.iconIngotFill;
        this.iconPickHeadFill = reg.registerIcon("naturalexceptions:mold_pick_head_fill");
        this.iconPlateFill = reg.registerIcon("naturalexceptions:mold_plate_fill");
        this.iconRodFill = reg.registerIcon("naturalexceptions:mold_rod_fill");
        this.iconBladeFill = reg.registerIcon("naturalexceptions:mold_blade_fill");
        this.iconHandleFill = reg.registerIcon("naturalexceptions:mold_handle_fill");

    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderPasses(int metadata) {
        return 2;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ItemStack stack, int pass) {
        if (pass == 0) {
            return getBaseMoldIcon(getGeometry(stack));
        }
        if (hasMetal(stack)) {
            return getCavityFillIcon(getGeometry(stack));
        }
        return iconEmptyMask != null ? iconEmptyMask : getBaseMoldIcon(getGeometry(stack));
    }

    public static int getTintColor(ItemStack stack, int pass) {
        if (pass == 0 || !hasMetal(stack)) {
            return 0xFFFFFF;
        }
        MaterialRef ref = getContainedMaterialRef(stack);
        CanonicalMaterialKey key = getContainedStateKey(stack);
        ActiveThermalState thermal = getThermalState(stack);
        double temp = thermal != null ? thermal.temperatureKelvin() : 300.0;
        if (isSolidified(stack)) {
            return MaterialAppearance.getSolidColor(ref, key);
        } else {
            return MaterialAppearance.getMoltenColor(ref, key, temp);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return getTintColor(stack, pass);
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBaseMoldIcon(GeometryId geom) {
        if (geom == null) {
            return iconBlank != null ? iconBlank : itemIcon;
        }
        switch (geom) {
            case INGOT:
                return itemIcon;
            case PICK_HEAD:
                return iconPickHead != null ? iconPickHead : itemIcon;
            case PLATE:
                return iconPlate != null ? iconPlate : itemIcon;
            case ROD:
                return iconRod != null ? iconRod : itemIcon;
            case BLADE:
                return iconBlade != null ? iconBlade : itemIcon;
            case HANDLE:
                return iconHandle != null ? iconHandle : itemIcon;
            case BLANK:
            default:
                return iconBlank != null ? iconBlank : itemIcon;
        }
    }

    @SideOnly(Side.CLIENT)
    public IIcon getCavityFillIcon(GeometryId geom) {
        if (geom == null) {
            return iconIngotFill != null ? iconIngotFill : iconBlank;
        }
        switch (geom) {
            case INGOT:
                return iconIngotFill != null ? iconIngotFill : itemIcon;
            case PICK_HEAD:
                return iconPickHeadFill != null ? iconPickHeadFill : iconPickHead;
            case PLATE:
                return iconPlateFill != null ? iconPlateFill : iconPlate;
            case ROD:
                return iconRodFill != null ? iconRodFill : iconRod;
            case BLADE:
                return iconBladeFill != null ? iconBladeFill : iconBlade;
            case HANDLE:
                return iconHandleFill != null ? iconHandleFill : iconHandle;
            case BLANK:
            default:
                return iconIngotFill != null ? iconIngotFill : itemIcon;
        }
    }

    @SideOnly(Side.CLIENT)
    public IIcon getEmptyMaskIcon() {
        return iconEmptyMask;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) {
        if (hasMetal(stack)) {
            return getCavityFillIcon(getGeometry(stack));
        }
        return getBaseMoldIcon(getGeometry(stack));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(createStack(null));
        list.add(createStack(GeometryId.INGOT));
        list.add(createStack(GeometryId.PICK_HEAD));
        list.add(createStack(GeometryId.BLADE));
        list.add(createStack(GeometryId.PLATE));
        list.add(createStack(GeometryId.ROD));
    }

    private static double getVolume(GeometryId geom) {
        if (geom == null) return ComponentGeometry.blank()
            .volumeCm3();
        switch (geom) {
            case INGOT:
                return ComponentGeometry.ingot()
                    .volumeCm3();
            case PICK_HEAD:
                return ComponentGeometry.pickHead()
                    .volumeCm3();
            case PLATE:
                return ComponentGeometry.plate()
                    .volumeCm3();
            case ROD:
                return ComponentGeometry.rod()
                    .volumeCm3();
            case BLADE:
                return ComponentGeometry.blade()
                    .volumeCm3();
            case HANDLE:
                return ComponentGeometry.handle()
                    .volumeCm3();
            case BLANK:
            default:
                return ComponentGeometry.blank()
                    .volumeCm3();
        }
    }

    public static String formatMaterialName(MaterialRef ref) {
        return naturalexceptions.core.material.MaterialNameResolver.resolveMaterialName(ref);
    }

    public static String formatMaterialName(ItemStack stack) {
        if (stack == null) return "Unknown";
        MaterialRef ref = getContainedMaterialRef(stack);
        CanonicalMaterialKey key = getContainedStateKey(stack);
        return naturalexceptions.core.material.MaterialNameResolver.resolveMaterialName(ref, key);
    }

    private static String formatGeometryName(GeometryId id) {
        if (id == null) return "Unshaped Blank";
        switch (id) {
            case INGOT:
                return "Ingot";
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
                return "Component Blank";
            default:
                return id.id();
        }
    }
}
