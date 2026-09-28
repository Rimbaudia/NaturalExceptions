package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.persistence.AssemblyNbtCodec;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Generic Minecraft item carrier representing a physical component instance.
 */
public final class ItemGenericComponent extends Item {

    @SideOnly(Side.CLIENT)
    private IIcon iconBlank;
    @SideOnly(Side.CLIENT)
    private IIcon iconPlate;
    @SideOnly(Side.CLIENT)
    private IIcon iconRod;
    @SideOnly(Side.CLIENT)
    private IIcon iconHandle;
    @SideOnly(Side.CLIENT)
    private IIcon iconPickHead;
    @SideOnly(Side.CLIENT)
    private IIcon iconAxeHead;
    @SideOnly(Side.CLIENT)
    private IIcon iconShovelHead;
    @SideOnly(Side.CLIENT)
    private IIcon iconHoeHead;
    @SideOnly(Side.CLIENT)
    private IIcon iconBlade;

    public ItemGenericComponent() {
        setUnlocalizedName("genericComponent");
        setTextureName("naturalexceptions:component_ingot");
        setMaxStackSize(16);
    }

    public static ItemStack createWoodenHandle() {
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "wood"))),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);
        return createStack(woodHandle);
    }

    public static ItemStack createStack(ComponentInstance comp) {
        Item item = NEItems.genericComponent != null ? NEItems.genericComponent : new ItemGenericComponent();
        ItemStack stack = new ItemStack(item);
        if (comp != null) {
            NBTTagCompound nbt = new NBTTagCompound();
            AssemblyNbtCodec.writeComponentToNbt(comp, nbt);
            stack.setTagCompound(nbt);
        }
        return stack;
    }

    public static ComponentInstance getComponent(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        try {
            return AssemblyNbtCodec.readComponentFromNbt(stack.getTagCompound());
        } catch (Exception e) {
            return null;
        }
    }

    public static GeometryId getGeometry(ItemStack stack) {
        ComponentInstance comp = getComponent(stack);
        return comp != null ? comp.geometryId() : null;
    }

    public static ActiveThermalState getThermalState(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        if (!nbt.hasKey(naturalexceptions.minecraft.persistence.MaterialCarrierNbtCodec.KEY_THERMAL, 10)) {
            return null;
        }
        ComponentInstance comp = getComponent(stack);
        MaterialProperties props = resolveProperties(comp);
        return naturalexceptions.minecraft.persistence.MaterialCarrierNbtCodec.readThermalStateFromNbt(nbt, props);
    }

    public enum ThermalCondition {

        COLD("\u00a77Cold (Ambient)", false, false),
        WARM("\u00a7eWarm", false, false),
        STIFF_COLD_WORK("\u00a7cStiff (Cold Working - Risk of Hardening)", true, false),
        WORKABLE("\u00a7eWorkable (Cooling Down)", true, true),
        IDEAL_FORGE("\u00a76Ideal Forge Heat (Malleable)", true, true),
        NEAR_MELTING("\u00a74Near Melting (Soft)", true, true),
        MOLTEN("\u00a74Molten Liquid", false, false);

        private final String label;
        private final boolean isHot;
        private final boolean isWorkableHot;

        ThermalCondition(String label, boolean isHot, boolean isWorkableHot) {
            this.label = label;
            this.isHot = isHot;
            this.isWorkableHot = isWorkableHot;
        }

        public String label() {
            return label;
        }

        public boolean isHot() {
            return isHot;
        }

        public boolean isWorkableHot() {
            return isWorkableHot;
        }
    }

    public static MaterialProperties resolveProperties(ComponentInstance comp) {
        if (comp != null) {
            try {
                return new naturalexceptions.core.property.MaterialPropertyResolver()
                    .resolve(
                        comp.materialRef(),
                        comp.stateKey()
                            .composition(),
                        comp.stateKey()
                            .phase())
                    .properties();
            } catch (Exception ignored) {}
        }
        return BuiltinMaterials.COPPER_PROPERTIES;
    }

    public static ThermalCondition getThermalCondition(double tempK, MaterialProperties props) {
        if (props == null) props = BuiltinMaterials.COPPER_PROPERTIES;
        double meltK = props.meltingPointKelvin();
        double tRx = props.recrystallizationTemperatureKelvin();

        if (tempK >= meltK) {
            return ThermalCondition.MOLTEN;
        }
        if (tempK >= meltK - 80.0) {
            return ThermalCondition.NEAR_MELTING;
        }
        if (tempK >= tRx + 100.0) {
            return ThermalCondition.IDEAL_FORGE;
        }
        if (tempK >= tRx) {
            return ThermalCondition.WORKABLE;
        }
        if (tempK > 373.15) {
            return ThermalCondition.STIFF_COLD_WORK;
        }
        if (tempK > 315.0) {
            return ThermalCondition.WARM;
        }
        return ThermalCondition.COLD;
    }

    public static void saveThermalState(ItemStack stack, ActiveThermalState thermal) {
        if (stack == null) return;
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        naturalexceptions.minecraft.persistence.MaterialCarrierNbtCodec
            .writeThermalStateToNbt(thermal, stack.getTagCompound());
    }

    public static double getTemperatureKelvin(ItemStack stack) {
        ActiveThermalState thermal = getThermalState(stack);
        return thermal != null ? thermal.temperatureKelvin() : 293.15;
    }

    public static boolean checkAnneal(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return false;
        ComponentInstance comp = getComponent(stack);
        if (comp == null || comp.damage()
            .isFractured()) return false;
        ActiveThermalState thermal = getThermalState(stack);
        if (thermal == null) return false;

        MaterialProperties props = thermal.properties();
        if (props == null) return false;

        double tempK = thermal.temperatureKelvin();
        double meltK = props.meltingPointKelvin();
        double tRx = props.recrystallizationTemperatureKelvin();

        if (tempK >= tRx && tempK < meltK) {
            if (comp.manufacturingState() == ManufacturingState.COLD_WORKED || comp.stateKey()
                .microstructure() == naturalexceptions.core.material.Microstructure.WORK_HARDENED
                || comp.stateKey()
                    .conditioning() == naturalexceptions.core.material.ConditioningHistory.COLD_WORKED) {
                comp.setStateKey(
                    comp.stateKey()
                        .withMicrostructure(naturalexceptions.core.material.Microstructure.ANNEALED)
                        .withConditioning(naturalexceptions.core.material.ConditioningHistory.NONE));
                comp.setManufacturingState(ManufacturingState.ANNEALED);
                comp.damage()
                    .repairDeformation(0.5);
                comp.damage()
                    .repairCracking(0.5);
                AssemblyNbtCodec.writeComponentToNbt(comp, stack.getTagCompound());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onUpdate(ItemStack stack, net.minecraft.world.World world, net.minecraft.entity.Entity entity, int slot,
        boolean isCurrentItem) {
        ActiveThermalState thermal = getThermalState(stack);
        if (thermal != null && thermal.temperatureKelvin() > 293.15) {
            double deltaSeconds = 0.05; // 1 tick
            double temp = thermal.temperatureKelvin();
            double surfaceArea = 0.02; // Component surface area
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
            checkAnneal(stack);
            saveThermalState(stack, thermal);

            if (isCurrentItem && entity instanceof net.minecraft.entity.player.EntityPlayer
                && temp > 400.0
                && world != null
                && world.getTotalWorldTime() % 20L == 0L) {
                entity.attackEntityFrom(net.minecraft.util.DamageSource.onFire, 1.0F);
            }
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        ComponentInstance comp = getComponent(stack);
        if (comp == null) {
            GeometryId blank = BlankGeometry.get(stack);
            return blank == null ? "Component" : "Blank " + formatGeometryName(blank);
        }
        String prefix = comp.manufacturingState() == ManufacturingState.COLD_WORKED ? "Cold-Worked "
            : comp.manufacturingState() == ManufacturingState.ANNEALED ? "Annealed " : "";
        return prefix + formatGeometryName(comp.geometryId());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        ComponentInstance comp = getComponent(stack);
        if (comp == null) {
            list.add("\u00a77Empty component carrier");
            return;
        }

        ActiveThermalState thermal = getThermalState(stack);
        MaterialProperties props = resolveProperties(comp);
        double tempK = thermal != null ? thermal.temperatureKelvin() : 293.15;
        ThermalCondition cond = getThermalCondition(tempK, props);

        list.add("\u00a76Geometry: \u00a7f" + formatGeometryName(comp.geometryId()));
        list.add("  \u00a77Material: \u00a7f" + naturalexceptions.minecraft.knowledge.KnowledgePresentation
            .materialName(player, comp.materialRef(), comp.stateKey()));
        if (MaterialNamingTarget.canOpenNaming(stack)) {
            list.add("\u00a78Sneak-right-click to name generated material");
        }
        list.add("  \u00a77Heat: " + cond.label());

        // Player-facing behavior remains qualitative; exact microstructure is simulation truth.
        if (comp.damage()
            .isFractured()) {
            list.add("  \u00a74Condition: Fractured; remelting is needed");
        } else if (comp.manufacturingState() == ManufacturingState.COLD_WORKED || comp.stateKey()
            .microstructure() == naturalexceptions.core.material.Microstructure.WORK_HARDENED) {
                list.add("  \u00a7cCondition: Feels stiff and resists further shaping");
            } else if (comp.manufacturingState() == ManufacturingState.ANNEALED || comp.stateKey()
                .microstructure() == naturalexceptions.core.material.Microstructure.ANNEALED) {
                    list.add("  \u00a7aCondition: Feels softened and easier to shape");
                } else if (comp.stateKey()
                    .microstructure() == naturalexceptions.core.material.Microstructure.QUENCHED) {
                        list.add("  \u00a7bCondition: Feels hard after rapid cooling");
                    }

        // Finish state & Assembly readiness
        boolean assemblyReady = isAssemblyReady(comp);
        if (comp.manufacturingState() == ManufacturingState.FINISHED) {
            list.add("  \u00a7aFinish: Planished" + (assemblyReady ? " (Ready for assembly)" : ""));
        } else if (comp.manufacturingState() == ManufacturingState.AS_CAST) {
            list.add("  \u00a77State: As-Cast" + (assemblyReady ? " (Ready for assembly)" : ""));
        } else if (comp.manufacturingState() == ManufacturingState.WORKED) {
            list.add("  \u00a7eFinish: Rough-Forged (Needs Planishing)");
        }

        // Damage summary if not pristine
        if (comp.damage()
            .isDamaged()
            && !comp.damage()
                .isFractured()) {
            list.add("  \u00a7cCondition: Shows cracking / damage");
        }

        // Progressive disclosure check: advanced (F3+H) or Shift key held
        boolean showAdvanced = advanced;
        try {
            if (org.lwjgl.input.Keyboard.isCreated()) {
                showAdvanced |= org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                    || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
            }
        } catch (Throwable ignored) {}

        if (advanced) {
            list.add("\u00a78--- Developer diagnostics ---");
            list.add(String.format("  \u00a78Mass: %s", comp.mass()));
            if (thermal != null) {
                list.add(String.format("  \u00a78Temperature: %.1f K (%.1f \u00b0C)", tempK, tempK - 273.15));
                list.add(
                    String.format(
                        "  \u00a78Thresholds: T_rx = %.0f K, T_melt = %.0f K",
                        props.recrystallizationTemperatureKelvin(),
                        props.meltingPointKelvin()));
            }
            list.add(
                "  \u00a78State: " + comp.manufacturingState()
                    .id());
            list.add(
                "  \u00a78Microstructure: " + comp.stateKey()
                    .microstructure()
                    .id());
            list.add(
                "  \u00a78Conditioning: " + comp.stateKey()
                    .conditioning()
                    .id());
            if (comp.damage()
                .isDamaged()) {
                list.add(
                    String.format(
                        "  \u00a78Strain: Deform %.2f | Wear %.2f | Cracks %.2f",
                        comp.damage()
                            .deformation(),
                        comp.damage()
                            .wear(),
                        comp.damage()
                            .cracking()));
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        this.itemIcon = reg.registerIcon("naturalexceptions:component_ingot");
        this.iconBlank = this.itemIcon;
        this.iconPlate = reg.registerIcon("naturalexceptions:hammered_copper_plate");
        this.iconRod = reg.registerIcon("naturalexceptions:copper_rod");
        this.iconHandle = reg.registerIcon("naturalexceptions:component_handle");
        this.iconPickHead = reg.registerIcon("naturalexceptions:component_pick_head");
        this.iconAxeHead = reg.registerIcon("naturalexceptions:component_axe_head");
        this.iconShovelHead = reg.registerIcon("naturalexceptions:component_shovel_head");
        this.iconHoeHead = reg.registerIcon("naturalexceptions:component_hoe_head");
        this.iconBlade = reg.registerIcon("naturalexceptions:component_blade");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) {
        ComponentInstance comp = getComponent(stack);
        GeometryId geometry = comp != null ? comp.geometryId() : BlankGeometry.get(stack);
        if (geometry != null) {
            switch (geometry) {
                case PICK_HEAD:
                    return iconPickHead != null ? iconPickHead : itemIcon;
                case AXE_HEAD:
                    return iconAxeHead != null ? iconAxeHead : itemIcon;
                case SHOVEL_HEAD:
                    return iconShovelHead != null ? iconShovelHead : itemIcon;
                case HOE_HEAD:
                    return iconHoeHead != null ? iconHoeHead : itemIcon;
                case PLATE:
                    return iconPlate != null ? iconPlate : itemIcon;
                case ROD:
                    return iconRod != null ? iconRod : itemIcon;
                case HANDLE:
                    return iconHandle != null ? iconHandle : itemIcon;
                case BLADE:
                    return iconBlade != null ? iconBlade : itemIcon;
                case INGOT:
                    return itemIcon;
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
    public int getColorFromItemStack(ItemStack stack, int pass) {
        ComponentInstance comp = getComponent(stack);
        if (comp != null) {
            MaterialRef ref = comp.materialRef();
            if (comp.geometryId() != GeometryId.HANDLE && ref != null
                && ("iron".equalsIgnoreCase(
                    ref.authoredId() != null ? ref.authoredId()
                        .path() : "")
                    || "fe".equalsIgnoreCase(
                        ref.authoredId() != null ? ref.authoredId()
                            .path() : ""))) {
                return 0xFFFFFF;
            }
            return naturalexceptions.core.material.MaterialAppearance
                .getSolidColor(comp.materialRef(), comp.stateKey());
        }
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (GeometryId geometry : GeometryId.values()) {
            list.add(BlankGeometry.create(item, geometry));
        }
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
        if (MaterialNamingTarget.openIfSneaking(stack, world, player)) return stack;
        return super.onItemRightClick(stack, world, player);
    }

    public static boolean isAssemblyReady(ComponentInstance comp) {
        if (comp == null || comp.geometryId() == null) return false;
        GeometryId geom = comp.geometryId();
        return geom == GeometryId.PICK_HEAD || geom == GeometryId.AXE_HEAD
            || geom == GeometryId.SHOVEL_HEAD
            || geom == GeometryId.HOE_HEAD
            || geom == GeometryId.BLADE
            || geom == GeometryId.HANDLE
            || geom == GeometryId.ROD;
    }

    public static String formatMaterialName(MaterialRef ref) {
        return formatMaterialName(ref, null);
    }

    public static String formatMaterialName(MaterialRef ref, CanonicalMaterialKey key) {
        return naturalexceptions.core.material.MaterialNameResolver.resolveMaterialName(ref, key);
    }

    public static String formatGeometryName(GeometryId id) {
        return naturalexceptions.core.material.MaterialNameResolver.formatComponentGeometry(id);
    }
}
