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
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
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
    private IIcon iconIngotFilled;
    @SideOnly(Side.CLIENT)
    private IIcon iconIngotSolid;
    @SideOnly(Side.CLIENT)
    private IIcon iconPickHeadFilled;
    @SideOnly(Side.CLIENT)
    private IIcon iconPickHeadSolid;
    @SideOnly(Side.CLIENT)
    private IIcon iconPlateFilled;
    @SideOnly(Side.CLIENT)
    private IIcon iconPlateSolid;
    @SideOnly(Side.CLIENT)
    private IIcon iconRodFilled;
    @SideOnly(Side.CLIENT)
    private IIcon iconRodSolid;
    @SideOnly(Side.CLIENT)
    private IIcon iconBladeFilled;
    @SideOnly(Side.CLIENT)
    private IIcon iconBladeSolid;
    @SideOnly(Side.CLIENT)
    private IIcon iconHandleFilled;
    @SideOnly(Side.CLIENT)
    private IIcon iconHandleSolid;

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

    public static boolean isFilled(ItemStack stack) {
        return stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .getBoolean(KEY_FILLED);
    }

    public static MaterialQuantity getFilledMass(ItemStack stack) {
        if (!isFilled(stack)) {
            return MaterialQuantity.ZERO;
        }
        return MaterialQuantity.ofMilligrams(
            stack.getTagCompound()
                .getLong(KEY_MASS_MG));
    }

    public static MaterialRef getContainedMaterialRef(ItemStack stack) {
        if (!isFilled(stack)) {
            return null;
        }
        return MaterialRef.parse(
            stack.getTagCompound()
                .getString(KEY_MATERIAL_REF));
    }

    public static ActiveThermalState getThermalState(ItemStack stack) {
        if (!isFilled(stack)) {
            return null;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        double energy = nbt.getDouble(KEY_ENERGY);
        double massKg = MaterialQuantity.ofMilligrams(nbt.getLong(KEY_MASS_MG))
            .toKilograms();
        double density = nbt.hasKey(KEY_DENSITY) ? nbt.getDouble(KEY_DENSITY) : 8.96;
        double spHeat = nbt.hasKey(KEY_SPECIFIC_HEAT) ? nbt.getDouble(KEY_SPECIFIC_HEAT) : 385.0;
        double spHeatLiq = nbt.hasKey(KEY_SPECIFIC_HEAT_LIQUID) ? nbt.getDouble(KEY_SPECIFIC_HEAT_LIQUID) : 410.0;
        double meltPoint = nbt.hasKey(KEY_MELT_POINT) ? nbt.getDouble(KEY_MELT_POINT) : 1357.77;
        double latentHeat = nbt.hasKey(KEY_LATENT_HEAT) ? nbt.getDouble(KEY_LATENT_HEAT) : 205000.0;

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
        nbt.setBoolean(KEY_FILLED, true);
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
        nbt.setLong(
            KEY_MASS_MG,
            pouredLayer.mass()
                .milligrams());
        nbt.setDouble(
            KEY_ENERGY,
            pouredLayer.thermalState()
                .energyJoules());

        MaterialProperties props = pouredLayer.thermalState()
            .properties();
        nbt.setDouble(KEY_DENSITY, props.density());
        nbt.setDouble(KEY_SPECIFIC_HEAT, props.specificHeatSolid());
        nbt.setDouble(KEY_SPECIFIC_HEAT_LIQUID, props.specificHeatLiquid());
        nbt.setDouble(KEY_MELT_POINT, props.meltingPointKelvin());
        nbt.setDouble(KEY_LATENT_HEAT, props.latentHeatOfFusion());

        stack.setTagCompound(nbt);
        // Filled molds do not stack
        stack.stackSize = 1;
        return true;
    }

    public static boolean isSolidified(ItemStack stack) {
        if (!isFilled(stack)) {
            return false;
        }
        ActiveThermalState thermal = getThermalState(stack);
        if (thermal == null) {
            return true;
        }
        return thermal.phase() == MaterialPhase.SOLID && thermal.meltFraction() <= 0.0;
    }

    public static ItemStack extractComponent(ItemStack stack) {
        if (!isFilled(stack) || !isSolidified(stack)) {
            return null;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        GeometryId geom = getGeometry(stack);
        if (geom == null) {
            geom = GeometryId.BLANK;
        }
        MaterialRef ref = MaterialRef.parse(nbt.getString(KEY_MATERIAL_REF));
        CanonicalMaterialKey key = CanonicalMaterialKey.parse(nbt.getString(KEY_STATE_KEY))
            .asSolidified();
        MaterialQuantity mass = MaterialQuantity.ofMilligrams(nbt.getLong(KEY_MASS_MG));

        ComponentInstance instance = new ComponentInstance(geom, ref, key, mass, ManufacturingState.FINISHED);
        ItemStack componentStack = ItemGenericComponent.createStack(instance);

        // Empty the mold
        nbt.removeTag(KEY_FILLED);
        nbt.removeTag(KEY_MATERIAL_REF);
        nbt.removeTag(KEY_STATE_KEY);
        nbt.removeTag(KEY_COMPOSITION);
        nbt.removeTag(KEY_MASS_MG);
        nbt.removeTag(KEY_ENERGY);

        return componentStack;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isCurrentItem) {
        if (isFilled(stack)) {
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
                thermal.removeEnergyJoules(Math.max(0.0, qRad + qConv));
                saveThermalState(stack, thermal);
            }
        }
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        net.minecraft.block.Block block = world.getBlock(x, y, z);
        if (block instanceof naturalexceptions.minecraft.fabrication.BlockCastingTable
            || block instanceof naturalexceptions.minecraft.metallurgy.BlockStarterSmelter) {
            return false;
        }
        if (isFilled(stack)) {
            if (isSolidified(stack)) {
                if (!world.isRemote) {
                    ItemStack comp = extractComponent(stack);
                    if (comp != null) {
                        if (!player.inventory.addItemStackToInventory(comp)) {
                            player.dropPlayerItemWithRandomChoice(comp, false);
                        }
                        player.inventoryContainer.detectAndSendChanges();
                        world.playSoundEffect(player.posX, player.posY, player.posZ, "random.pop", 1.0F, 1.2F);
                        player.addChatMessage(
                            new ChatComponentText("\u00a7aExtracted " + comp.getDisplayName() + " from mold."));
                    }
                }
                return true;
            } else {
                if (!world.isRemote) {
                    ActiveThermalState thermal = getThermalState(stack);
                    double temp = thermal != null ? thermal.temperatureKelvin() : 1358.0;
                    player.addChatMessage(
                        new ChatComponentText(
                            String.format(
                                "\u00a7cThe cast metal is still molten/hot (%.1f K)! Wait for it to solidify.",
                                temp)));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (isFilled(stack)) {
            if (isSolidified(stack)) {
                if (!world.isRemote) {
                    ItemStack comp = extractComponent(stack);
                    if (comp != null) {
                        if (!player.inventory.addItemStackToInventory(comp)) {
                            player.dropPlayerItemWithRandomChoice(comp, false);
                        }
                        player.inventoryContainer.detectAndSendChanges();
                        world.playSoundEffect(player.posX, player.posY, player.posZ, "random.pop", 1.0F, 1.2F);
                        player.addChatMessage(
                            new ChatComponentText("\u00a7aExtracted " + comp.getDisplayName() + " from mold."));
                    }
                }
            } else {
                if (!world.isRemote) {
                    ActiveThermalState thermal = getThermalState(stack);
                    double temp = thermal != null ? thermal.temperatureKelvin() : 1358.0;
                    player.addChatMessage(
                        new ChatComponentText(
                            String.format(
                                "\u00a7cThe cast metal is still molten/hot (%.1f K)! Wait for it to solidify.",
                                temp)));
                }
            }
        }
        return stack;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        GeometryId geom = getGeometry(stack);
        String geomName = formatGeometryName(geom);
        if (isFilled(stack)) {
            MaterialRef ref = getContainedMaterialRef(stack);
            String matName = formatMaterialName(ref);
            if (isSolidified(stack)) {
                return "Solidified " + matName + " Mold (" + geomName + ")";
            } else {
                return "Molten " + matName + " Mold (" + geomName + ")";
            }
        }
        if (geom == null) {
            return "Casting Mold";
        }
        return "Casting Mold (" + geomName + ")";
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        GeometryId geom = getGeometry(stack);
        String geomName = formatGeometryName(geom);
        list.add("\u00a76Cavity: " + geomName);

        if (isFilled(stack)) {
            MaterialRef ref = getContainedMaterialRef(stack);
            MaterialQuantity mass = getFilledMass(stack);
            ActiveThermalState thermal = getThermalState(stack);
            double temp = thermal != null ? thermal.temperatureKelvin() : 293.15;
            double celsius = temp - 273.15;

            list.add("  \u00a77Material: " + formatMaterialName(ref));
            list.add("  \u00a77Mass: " + mass);
            list.add(String.format("  \u00a77Temp: %.1f K (%.1f \u00b0C)", temp, celsius));

            if (isSolidified(stack)) {
                list.add("  \u00a7aStatus: Solidified! (Right-click to extract)");
            } else {
                double meltPct = thermal != null ? thermal.meltFraction() * 100.0 : 100.0;
                list.add(String.format("  \u00a7eStatus: Hot & Molten (%.0f%% molten)", meltPct));
            }
            return;
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

        this.iconIngotFilled = reg.registerIcon("naturalexceptions:mold_ingot_filled");
        this.iconIngotSolid = reg.registerIcon("naturalexceptions:mold_ingot_solid");
        this.iconPickHeadFilled = reg.registerIcon("naturalexceptions:mold_pick_head_filled");
        this.iconPickHeadSolid = reg.registerIcon("naturalexceptions:mold_pick_head_solid");
        this.iconPlateFilled = reg.registerIcon("naturalexceptions:mold_plate_filled");
        this.iconPlateSolid = reg.registerIcon("naturalexceptions:mold_plate_solid");
        this.iconRodFilled = reg.registerIcon("naturalexceptions:mold_rod_filled");
        this.iconRodSolid = reg.registerIcon("naturalexceptions:mold_rod_solid");
        this.iconBladeFilled = reg.registerIcon("naturalexceptions:mold_blade_filled");
        this.iconBladeSolid = reg.registerIcon("naturalexceptions:mold_blade_solid");
        this.iconHandleFilled = reg.registerIcon("naturalexceptions:mold_handle_filled");
        this.iconHandleSolid = reg.registerIcon("naturalexceptions:mold_handle_solid");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) {
        boolean filled = isFilled(stack);
        boolean solid = isSolidified(stack);
        GeometryId geom = getGeometry(stack);

        if (filled) {
            if (solid) {
                if (geom != null) {
                    switch (geom) {
                        case PICK_HEAD:
                            return iconPickHeadSolid != null ? iconPickHeadSolid : iconPickHead;
                        case PLATE:
                            return iconPlateSolid != null ? iconPlateSolid : iconPlate;
                        case ROD:
                            return iconRodSolid != null ? iconRodSolid : iconRod;
                        case BLADE:
                            return iconBladeSolid != null ? iconBladeSolid : iconBlade;
                        case HANDLE:
                            return iconHandleSolid != null ? iconHandleSolid : iconHandle;
                        case BLANK:
                        default:
                            return iconIngotSolid != null ? iconIngotSolid : itemIcon;
                    }
                }
                return iconIngotSolid != null ? iconIngotSolid : itemIcon;
            } else {
                if (geom != null) {
                    switch (geom) {
                        case PICK_HEAD:
                            return iconPickHeadFilled != null ? iconPickHeadFilled : iconPickHead;
                        case PLATE:
                            return iconPlateFilled != null ? iconPlateFilled : iconPlate;
                        case ROD:
                            return iconRodFilled != null ? iconRodFilled : iconRod;
                        case BLADE:
                            return iconBladeFilled != null ? iconBladeFilled : iconBlade;
                        case HANDLE:
                            return iconHandleFilled != null ? iconHandleFilled : iconHandle;
                        case BLANK:
                        default:
                            return iconIngotFilled != null ? iconIngotFilled : itemIcon;
                    }
                }
                return iconIngotFilled != null ? iconIngotFilled : itemIcon;
            }
        }

        if (geom != null) {
            switch (geom) {
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
        list.add(createStack(GeometryId.BLANK));
        list.add(createStack(GeometryId.PICK_HEAD));
        list.add(createStack(GeometryId.BLADE));
        list.add(createStack(GeometryId.PLATE));
        list.add(createStack(GeometryId.ROD));
        list.add(createStack(GeometryId.HANDLE));
    }

    private static double getVolume(GeometryId geom) {
        if (geom == null) return ComponentGeometry.blank()
            .volumeCm3();
        switch (geom) {
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
        if (id == null) return "Unknown";
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
                return "Ingot / Blank";
            default:
                return id.id();
        }
    }
}
