package naturalexceptions.minecraft.biology;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraft.item.ItemFood;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import java.util.UUID;
import naturalexceptions.core.biology.ExposureRoute;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.core.biology.BiologicalInteractionResolver;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.PhysicalInteraction;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;

/** Converts the host's coarse damage event into a shared interaction, then maps condition loss to host health once. */
public final class BiologicalEvents {
    private static final ResourceId TORSO = ResourceId.of("naturalexceptions", "torso");
    private static final ResourceId COMBUSTION_SMOKE = ResourceId.of("naturalexceptions", "combustion_smoke");
    private static final String KEY_SMOKE_WARNING = "naturalexceptions.smoke_warning";
    private static final naturalexceptions.core.property.MaterialProperties SMOKE_PROPERTIES =
        naturalexceptions.core.property.MaterialProperties.builder().density(0.001).biologicalHazard(0.25)
            .addTrait("respiratory_hazard").build();
    private static final UUID BIOLOGY_MOVEMENT = UUID.fromString("6b4196ec-33fd-48f9-aac7-f8910be12831");
    @SubscribeEvent public void onConstruct(EntityEvent.EntityConstructing event) {
        if (event != null && event.entity instanceof EntityPlayer) BiologicalProperties.get((EntityPlayer) event.entity);
    }
    @SubscribeEvent public void onClone(PlayerEvent.Clone event) {
        if (event == null || event.original == null || event.entityPlayer == null) return;
        BiologicalProperties old = BiologicalProperties.get(event.original);
        BiologicalProperties.get(event.entityPlayer).copyFrom(old, !event.wasDeath);
    }
    @SubscribeEvent public void onLivingHurt(LivingHurtEvent event) {
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj.isRemote || event.ammount <= 0.0F) return;
        EntityLivingBase target = event.entityLiving;
        HostDamageMapping.Category category = HostDamageMapping.categorize(event.source);
        // Drowning, starvation, magic, the void and similar are not physical interactions; the host's
        // own damage stands and is not disguised as a blow.
        if (category == HostDamageMapping.Category.UNMODELLED) return;
        BiologicalState state = BiologicalProperties.get(target).state();
        if (state == null) return;
        float raw = event.ammount;
        PhysicalInteraction.Type type = HostDamageMapping.interactionType(category);
        double energy = Math.max(1.0, raw * 55.0);
        double sharpness = 0.0, penetration = 0.0;
        Entity sourceEntity = event.source.getEntity();
        if (category == HostDamageMapping.Category.PHYSICAL_HIT && sourceEntity instanceof EntityPlayer) {
            net.minecraft.item.ItemStack stack = ((EntityPlayer) sourceEntity).getHeldItem();
            if (stack != null && stack.getItem() instanceof ItemGenericAssembly) {
                naturalexceptions.core.assembly.AssemblyGraph graph = ItemGenericAssembly.getAssembly(stack);
                naturalexceptions.core.assembly.DerivedCapability capability = ItemGenericAssembly.getCapability(graph);
                if (graph != null && graph.head() != null && capability != null) {
                    if (graph.head().geometryId() == naturalexceptions.core.fabrication.GeometryId.BLADE) {
                        type = PhysicalInteraction.Type.CUT;
                        penetration = Math.min(1.0, capability.penetration() / 20.0);
                        sharpness = 0.85;
                    }
                    energy = Math.max(1.0, raw * 45.0 + capability.impact() * 30.0 + capability.penetration() * 5.0);
                }
            }
        }
        double before = state.condition();
        PhysicalInteraction interaction = new PhysicalInteraction(type, HostDamageMapping.region(category), energy,
            penetration, sharpness, type == PhysicalInteraction.Type.THERMAL ? 500.0 : 20.0);
        java.util.List<VanillaArmorAdapter.EquippedArmor> equippedArmor = VanillaArmorAdapter.capture(target);
        java.util.List<naturalexceptions.core.biology.ArmorLayer> armorLayers = new java.util.ArrayList<>();
        for (VanillaArmorAdapter.EquippedArmor armor : equippedArmor) armorLayers.add(armor.layer());
        long now = target.worldObj.getTotalWorldTime();
        BiologicalInteractionResolver.apply(state, interaction, now, armorLayers);
        for (VanillaArmorAdapter.EquippedArmor armor : equippedArmor) armor.commit(target);
        // Armor has absorbed and been worn by this hit here; the host armor step must not do it again.
        if (!equippedArmor.isEmpty()) ArmorPathGate.markResolved(target, now);
        double lost = Math.max(0.0, before - state.condition());
        // Vanilla hearts are a derived compatibility summary; avoid applying both raw vanilla damage and injury damage.
        event.ammount = (float) Math.min(raw, target.getMaxHealth() * lost);
        rememberDerivedHealth(target, state);
        applyMovement(target, state);
    }

    private static final String KEY_DERIVED_HEALTH = "naturalexceptions.derived_health";

    /**
     * Records the health the biological condition implies after this hit, so the periodic sync moves hearts only
     * by later biological change and never undoes host damage that biology does not model.
     */
    private static void rememberDerivedHealth(EntityLivingBase target, BiologicalState state) {
        if (target instanceof EntityPlayer) {
            target.getEntityData()
                .setFloat(KEY_DERIVED_HEALTH, (float) (target.getMaxHealth() * state.condition()));
        }
    }

    /**
     * Health after a periodic sync: follows biological change as a delta, never rises above what the body's
     * condition allows, and keeps any additional unmodelled host damage until ordinary regeneration heals it.
     */
    static float syncedHealth(float currentHealth, float previousDerived, float derived) {
        if (derived <= 0.0F) return 0.0F;
        float next = currentHealth + (derived - previousDerived);
        return Math.max(0.0F, Math.min(derived, next));
    }

    /** Routes direct environmental heat contact through the same shared P10 interaction resolver. */
    public static void applyThermalContact(EntityLivingBase target, ResourceId region, double temperatureKelvin,
        double energyJoules) {
        if (target == null || region == null || !Double.isFinite(temperatureKelvin)
            || !Double.isFinite(energyJoules) || energyJoules <= 0.0
            || (target.worldObj != null && target.worldObj.isRemote)) return;
        BiologicalState state = BiologicalProperties.get(target).state();
        long now = Math.max(state.lastResolvedTick(), target.worldObj != null
            ? target.worldObj.getTotalWorldTime() : Math.max(0, target.ticksExisted));
        PhysicalInteraction interaction = new PhysicalInteraction(PhysicalInteraction.Type.THERMAL, region,
            energyJoules, 0.0, 0.0, temperatureKelvin - 273.15);
        java.util.List<VanillaArmorAdapter.EquippedArmor> equippedArmor = VanillaArmorAdapter.capture(target);
        java.util.List<naturalexceptions.core.biology.ArmorLayer> layers = new java.util.ArrayList<>();
        for (VanillaArmorAdapter.EquippedArmor armor : equippedArmor) layers.add(armor.layer());
        BiologicalInteractionResolver.apply(state, interaction, now, layers);
        for (VanillaArmorAdapter.EquippedArmor armor : equippedArmor) armor.commit(target);
        if (state.condition() <= 0.0) target.setHealth(0.0F);
        else target.setHealth((float) (target.getMaxHealth() * state.condition()));
        applyMovement(target, state);
    }

    private static final ThermalContactLimiter CONTACT_LIMITER = new ThermalContactLimiter();

    /**
     * Sustained contact with hot matter (standing in or on it). Resolved at most once per second per body with
     * energy proportional to the time in contact, through the same P10 thermal interaction as any other contact.
     */
    public static void applySustainedThermalContact(EntityLivingBase target, ResourceId region,
        double temperatureKelvin, double joulesPerSecond) {
        if (target == null || target.worldObj == null || target.worldObj.isRemote) return;
        double joules = CONTACT_LIMITER.admit(target, target.worldObj.getTotalWorldTime(), joulesPerSecond);
        if (joules > 0.0) applyThermalContact(target, region, temperatureKelvin, joules);
    }

    /** Inhales {@code grams} of combustion smoke (one second's worth from {@link SmelterSmoke}). */
    public static double applyCombustionSmoke(BiologicalState state, double grams, long worldTick) {
        if (state == null) throw new NullPointerException("state");
        return state.expose(COMBUSTION_SMOKE, SMOKE_PROPERTIES, ExposureRoute.INHALATION, grams, worldTick);
    }

    @SubscribeEvent public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null || !(event.entityLiving instanceof EntityPlayer) || event.entityLiving.worldObj.isRemote) return;
        EntityPlayer player = (EntityPlayer) event.entityLiving;
        if (player.ticksExisted % 20 != 0) return;
        BiologicalState state = BiologicalProperties.get(player).state();
        long now = player.worldObj.getTotalWorldTime();
        state.advanceTo(Math.max(state.lastResolvedTick(), now));
        state.clearExposureProtection();
        for (net.minecraft.item.ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && stack.getItem() instanceof naturalexceptions.minecraft.item.ItemExposureMask
                && naturalexceptions.minecraft.item.ItemExposureMask.isWorn(stack))
                state.addExposureProtection(new naturalexceptions.core.biology.ExposureProtection(ExposureRoute.INHALATION, 0.75));
        }
        double smokeGrams = SmelterSmoke.gramsPerSecondAt(player);
        boolean smoke = smokeGrams > 0.0;
        if (smoke) {
            applyCombustionSmoke(state, smokeGrams, now);
            if (!player.getEntityData().getBoolean(KEY_SMOKE_WARNING)) {
                player.addChatMessage(new net.minecraft.util.ChatComponentText(
                    "\u00a77Smoke from the smelter fire stings your breathing. Open air or a filter mask helps."));
            }
        }
        player.getEntityData().setBoolean(KEY_SMOKE_WARNING, smoke);
        float derivedHealth = (float) (player.getMaxHealth() * state.condition());
        net.minecraft.nbt.NBTTagCompound data = player.getEntityData();
        float previousDerived = data.hasKey(KEY_DERIVED_HEALTH) ? data.getFloat(KEY_DERIVED_HEALTH) : player.getHealth();
        float synced = syncedHealth(player.getHealth(), previousDerived, derivedHealth);
        data.setFloat(KEY_DERIVED_HEALTH, derivedHealth);
        if (synced <= 0.0F) player.setHealth(0.0F);
        else if (Math.abs(player.getHealth() - synced) > 0.05F) player.setHealth(synced);
        int derivedFood = (int) Math.round(20.0 * state.energyReserveJoules()
            / Math.max(1.0, state.physiology().energyCapacityJoules()));
        player.getFoodStats().setFoodLevel(Math.max(0, Math.min(20, derivedFood)));
        applyMovement(player, state);
    }

    @SubscribeEvent public void onFood(PlayerUseItemEvent.Finish event) {
        if (event == null || event.entityPlayer == null || event.entityPlayer.worldObj.isRemote
            || event.item == null || !(event.item.getItem() instanceof ItemFood)) return;
        BiologicalState state = BiologicalProperties.get(event.entityPlayer).state();
        int nourishment = ((ItemFood) event.item.getItem()).func_150905_g(event.item);
        state.replenishEnergyJoules(Math.max(1, nourishment) * 200_000.0);
        state.replenishHydrationMillilitres(Math.max(1, nourishment) * 35.0);
    }

    @SubscribeEvent public void onFluidContact(PlayerInteractEvent event) {
        if (event == null || event.world == null || event.world.isRemote || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
            || event.world.getBlock(event.x, event.y, event.z) != naturalexceptions.minecraft.registry.NEBlocks.fluidCell) return;
        NEWorldSavedData saved = NEWorldSavedData.get(event.world);
        if (saved == null) return;
        FluidParcel parcel = saved.fluids().parcelAt(new FluidCellKey(event.world.provider.dimensionId, event.x, event.y, event.z));
        if (parcel == null) return;
        ResourceId material = parcel.composition().asMap().keySet().iterator().next();
        BiologicalProperties.get(event.entityPlayer).state().expose(material, parcel.thermalState().properties(),
            ExposureRoute.CONTACT, 1.0, event.world.getTotalWorldTime());
    }

    private static void applyMovement(EntityLivingBase entity, BiologicalState state) {
        net.minecraft.entity.ai.attributes.IAttributeInstance attribute = entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(BIOLOGY_MOVEMENT);
        if (old != null) attribute.removeModifier(old);
        double amount = Math.max(-0.75, Math.min(0.0, state.movementCapability() - 1.0));
        if (amount < -0.001) attribute.applyModifier(new AttributeModifier(BIOLOGY_MOVEMENT,
            "Natural Exceptions biological impairment", amount, 2));
    }
}
