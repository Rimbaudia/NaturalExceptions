package naturalexceptions.minecraft.biology;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.core.biology.BiologicalInteractionResolver;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.ExposureRoute;
import naturalexceptions.core.biology.PhysicalInteraction;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

/** Converts the host's coarse damage event into a shared interaction, then maps condition loss to host health once. */
public final class BiologicalEvents {

    private static final ResourceId TORSO = ResourceId.of("naturalexceptions", "torso");
    private static final ResourceId COMBUSTION_SMOKE = ResourceId.of("naturalexceptions", "combustion_smoke");
    private static final String KEY_SMOKE_WARNING = "naturalexceptions.smoke_warning";
    private static final naturalexceptions.core.property.MaterialProperties SMOKE_PROPERTIES = naturalexceptions.core.property.MaterialProperties
        .builder()
        .density(0.001)
        .biologicalHazard(0.25)
        .addTrait("respiratory_hazard")
        .build();
    private static final UUID BIOLOGY_MOVEMENT = UUID.fromString("6b4196ec-33fd-48f9-aac7-f8910be12831");

    @SubscribeEvent
    public void onConstruct(EntityEvent.EntityConstructing event) {
        if (event != null && event.entity instanceof EntityPlayer)
            BiologicalProperties.get((EntityPlayer) event.entity);
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (event == null || event.original == null || event.entityPlayer == null) return;
        BiologicalProperties old = BiologicalProperties.get(event.original);
        BiologicalProperties.get(event.entityPlayer)
            .copyFrom(old, !event.wasDeath);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event == null || event.entityLiving == null
            || event.entityLiving.worldObj.isRemote
            || event.ammount <= 0.0F) return;
        EntityLivingBase target = event.entityLiving;
        HostDamageMapping.Category category = HostDamageMapping.categorize(event.source);
        // Drowning, starvation, magic, the void and similar are not physical interactions; the host's
        // own damage stands and is not disguised as a blow.
        if (category == HostDamageMapping.Category.UNMODELLED) return;
        BiologicalState state = BiologicalProperties.get(target)
            .state();
        if (state == null) return;
        float raw = event.ammount;
        PhysicalInteraction.Type type = HostDamageMapping.interactionType(category);
        double energy = Math.max(1.0, raw * 25.0);
        double sharpness = 0.0, penetration = 0.0;
        Entity sourceEntity = event.source.getEntity();
        if (category == HostDamageMapping.Category.PHYSICAL_HIT) {
            // What cuts: arrows and other missiles pierce, blades cut whoever swings them, teeth and claws bite.
            double[] cut = HostDamageMapping.cutting(event.source);
            if (cut != null) {
                type = cut[0] > 0.5 ? PhysicalInteraction.Type.PENETRATION : PhysicalInteraction.Type.CUT;
                sharpness = cut[1];
                penetration = cut[2];
                energy = Math.max(1.0, raw * 35.0);
            }
        }
        if (category == HostDamageMapping.Category.PHYSICAL_HIT && sourceEntity instanceof EntityPlayer) {
            net.minecraft.item.ItemStack stack = ((EntityPlayer) sourceEntity).getHeldItem();
            if (stack != null && stack.getItem() instanceof ItemGenericAssembly) {
                naturalexceptions.core.assembly.AssemblyGraph graph = ItemGenericAssembly.getAssembly(stack);
                naturalexceptions.core.assembly.DerivedCapability capability = ItemGenericAssembly.getCapability(graph);
                if (graph != null && graph.head() != null && capability != null) {
                    if (graph.head()
                        .geometryId() == naturalexceptions.core.fabrication.GeometryId.BLADE) {
                        type = PhysicalInteraction.Type.CUT;
                        penetration = Math.min(1.0, capability.penetration() / 20.0);
                        sharpness = 0.85;
                    }
                    energy = Math.max(1.0, raw * 45.0 + capability.impact() * 30.0 + capability.penetration() * 5.0);
                }
            }
        }
        double before = state.condition();
        java.util.List<naturalexceptions.core.id.ResourceId> parts = hitParts(category, event.source, target, state);
        java.util.List<VanillaArmorAdapter.EquippedArmor> equippedArmor = VanillaArmorAdapter.capture(target);
        java.util.List<naturalexceptions.core.biology.ArmorLayer> armorLayers = new java.util.ArrayList<>();
        for (VanillaArmorAdapter.EquippedArmor armor : equippedArmor) armorLayers.add(armor.layer());
        long now = target.worldObj.getTotalWorldTime();
        for (naturalexceptions.core.id.ResourceId part : parts) {
            PhysicalInteraction interaction = new PhysicalInteraction(
                type,
                part,
                energy / parts.size(),
                penetration,
                sharpness,
                type == PhysicalInteraction.Type.THERMAL ? 500.0 : 20.0);
            BiologicalInteractionResolver.apply(state, interaction, now, armorLayers);
        }
        for (VanillaArmorAdapter.EquippedArmor armor : equippedArmor) armor.commit(target);
        // Armor has absorbed and been worn by this hit here; the host armor step must not do it again.
        if (!equippedArmor.isEmpty()) ArmorPathGate.markResolved(target, now);
        double lost = Math.max(0.0, before - state.condition());
        // A hit always costs at least what it costs in vanilla (after the armour P10 just resolved); the body model
        // only adds to it when it judges the injury worse. Hearts are never driven by condition alone, which caps
        // injury burden and would otherwise leave badly hurt bodies unkillable.
        float armourFactor = equippedArmor.isEmpty() || event.source.isUnblockable() ? 1.0F
            : vanillaArmourFactor(target.getTotalArmorValue());
        event.ammount = hitDamage(raw, armourFactor, target.getMaxHealth(), lost);
        rememberDerivedHealth(target, state);
        applyMovement(target, state);
    }

    /**
     * The parts of the body a hit reaches: a fall both legs; a blow or a projectile the part at the height and on
     * the side it struck from; anything else a part drawn by its size. Only parts the body has.
     */
    static java.util.List<naturalexceptions.core.id.ResourceId> hitParts(HostDamageMapping.Category category,
        net.minecraft.util.DamageSource source, EntityLivingBase target, BiologicalState state) {
        java.util.List<naturalexceptions.core.id.ResourceId> parts = new java.util.ArrayList<>();
        if (category == HostDamageMapping.Category.FALL) {
            for (naturalexceptions.core.id.ResourceId leg : HostDamageMapping.fallRegions())
                if (state.region(leg) != null) parts.add(leg);
        } else {
            double height = Double.NaN, side = Double.NaN;
            Entity strike = source.getSourceOfDamage();
            if (category == HostDamageMapping.Category.PHYSICAL_HIT && strike != null
                && strike != target
                && target.boundingBox != null) {
                // Traced into the body: an arrow along its flight from where it was, a blow along its striker's aim.
                boolean thrown = source.isProjectile() || strike != source.getEntity();
                double sx = strike.posX, sy, sz = strike.posZ, dx, dy, dz;
                if (thrown) {
                    sy = strike.posY;
                    dx = strike.motionX;
                    dy = strike.motionY;
                    dz = strike.motionZ;
                } else {
                    sy = strike.posY + strike.getEyeHeight();
                    net.minecraft.util.Vec3 look = strike.getLookVec();
                    dx = look != null ? look.xCoord : target.posX - sx;
                    dy = look != null ? look.yCoord : 0.0;
                    dz = look != null ? look.zCoord : target.posZ - sz;
                }
                if (dx * dx + dy * dy + dz * dz < 1.0e-6) {
                    dx = target.posX - sx;
                    dy = target.posY + target.height / 2.0 - sy;
                    dz = target.posZ - sz;
                }
                net.minecraft.util.AxisAlignedBB box = target.boundingBox;
                double[] met = HostDamageMapping.strike(
                    box.minX,
                    box.minY,
                    box.minZ,
                    box.maxX,
                    box.maxY,
                    box.maxZ,
                    sx,
                    sy,
                    sz,
                    dx,
                    dy,
                    dz,
                    target.renderYawOffset);
                height = met[0];
                side = met[1];
            }
            naturalexceptions.core.id.ResourceId part = HostDamageMapping.region(
                height,
                side,
                target.getRNG()
                    .nextDouble());
            parts.add(state.region(part) != null ? part : HostDamageMapping.TORSO);
        }
        if (parts.isEmpty()) parts.add(HostDamageMapping.TORSO);
        return parts;
    }

    /** Share of a blow vanilla armour lets through: 4% less per armour point, as in the host's own armour step. */
    static float vanillaArmourFactor(int armourValue) {
        return (25.0F - Math.max(0, Math.min(24, armourValue))) / 25.0F;
    }

    /** Damage for one hit: the vanilla blow after armour, or the body model's condition loss if that is worse. */
    static float hitDamage(float raw, float armourFactor, float maxHealth, double conditionLost) {
        float vanilla = raw * Math.max(0.0F, Math.min(1.0F, armourFactor));
        float modelled = (float) (maxHealth * Math.max(0.0, conditionLost));
        return Math.max(vanilla, modelled);
    }

    /** Health lost to the body itself (burns, bleeding, poisoning): it has a cause of its own, not an attacker. */
    public static final net.minecraft.util.DamageSource BODY_FAILURE = new BodyFailureSource();

    /** Health lost to thirst: with no water left, a little every few seconds, as starving does. */
    public static final net.minecraft.util.DamageSource THIRST = new net.minecraft.util.DamageSource(
        "naturalexceptions.thirst").setDamageBypassesArmor();
    private static final String KEY_THIRST_WARNING = "naturalexceptions.thirst_warning";
    /** Ticks between thirst's blows, and what each takes. */
    static final int THIRST_TICKS = 80;
    static final float THIRST_DAMAGE = 1.0F;

    private static final class BodyFailureSource extends net.minecraft.util.DamageSource {

        BodyFailureSource() {
            super("naturalexceptions.body");
            setDamageBypassesArmor();
            setDamageIsAbsolute();
        }
    }

    /**
     * Moves an entity's health to {@code health} for a change the body model caused. Healing and non-lethal loss
     * set health directly; a lethal loss goes through a real damage event so death, drops and the death message
     * happen as for any other death.
     */
    static void applyBodyHealth(EntityLivingBase target, float health) {
        float current = target.getHealth();
        if (health > 0.0F) {
            if (Math.abs(current - health) > 0.05F) target.setHealth(Math.min(target.getMaxHealth(), health));
            return;
        }
        if (current <= 0.0F) return;
        target.hurtResistantTime = 0;
        target.attackEntityFrom(BODY_FAILURE, current + 1.0F);
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
        if (target == null || region == null
            || !Double.isFinite(temperatureKelvin)
            || !Double.isFinite(energyJoules)
            || energyJoules <= 0.0
            || (target.worldObj != null && target.worldObj.isRemote)) return;
        BiologicalState state = BiologicalProperties.get(target)
            .state();
        long now = Math.max(
            state.lastResolvedTick(),
            target.worldObj != null ? target.worldObj.getTotalWorldTime() : Math.max(0, target.ticksExisted));
        PhysicalInteraction interaction = new PhysicalInteraction(
            PhysicalInteraction.Type.THERMAL,
            region,
            energyJoules,
            0.0,
            0.0,
            temperatureKelvin - 273.15);
        java.util.List<VanillaArmorAdapter.EquippedArmor> equippedArmor = VanillaArmorAdapter.capture(target);
        java.util.List<naturalexceptions.core.biology.ArmorLayer> layers = new java.util.ArrayList<>();
        for (VanillaArmorAdapter.EquippedArmor armor : equippedArmor) layers.add(armor.layer());
        double before = state.condition();
        BiologicalInteractionResolver.apply(state, interaction, now, layers);
        for (VanillaArmorAdapter.EquippedArmor armor : equippedArmor) armor.commit(target);
        // Lower health by what the burn cost; contact with something hot never raises health.
        double lost = Math.max(0.0, before - state.condition());
        if (lost > 0.0 || state.condition() <= 0.0) applyBodyHealth(
            target,
            state.condition() <= 0.0 ? 0.0F : (float) (target.getHealth() - target.getMaxHealth() * lost));
        rememberDerivedHealth(target, state);
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

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null || !(event.entityLiving instanceof EntityPlayer) || event.entityLiving.worldObj.isRemote)
            return;
        EntityPlayer player = (EntityPlayer) event.entityLiving;
        if (player.ticksExisted % 20 != 0) return;
        BiologicalState state = BiologicalProperties.get(player)
            .state();
        long now = player.worldObj.getTotalWorldTime();
        // Resting on a seat mends wounds twice as fast (and the seat heals hearts itself).
        boolean seated = player.ridingEntity instanceof naturalexceptions.minecraft.furniture.EntitySeat;
        state.setRecoveryMultiplier(
            naturalexceptions.minecraft.gear.GearMovement.mendingMultiplier(
                naturalexceptions.minecraft.item.ItemTreasureGear
                    .worn(player, naturalexceptions.minecraft.gear.Treasure.BAND_OF_MENDING))
                * (seated ? 2.0 : 1.0));
        state.advanceTo(Math.max(state.lastResolvedTick(), now));
        spendExhaustion(player, state);
        state.clearExposureProtection();
        if (naturalexceptions.minecraft.item.ItemExposureMask.isWornBy(player)) state.addExposureProtection(
            new naturalexceptions.core.biology.ExposureProtection(
                ExposureRoute.INHALATION,
                naturalexceptions.minecraft.item.ItemExposureMask.INHALATION_ATTENUATION));
        double smokeGrams = SmelterSmoke.gramsPerSecondAt(player);
        boolean smoke = smokeGrams > 0.0;
        if (smoke) {
            applyCombustionSmoke(state, smokeGrams, now);
            if (!player.getEntityData()
                .getBoolean(KEY_SMOKE_WARNING)) {
                player.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        "\u00a77Smoke from the smelter fire stings your breathing. Open air or a filter mask helps."));
            }
        }
        player.getEntityData()
            .setBoolean(KEY_SMOKE_WARNING, smoke);
        float derivedHealth = (float) (player.getMaxHealth() * state.condition());
        net.minecraft.nbt.NBTTagCompound data = player.getEntityData();
        float previousDerived = data.hasKey(KEY_DERIVED_HEALTH) ? data.getFloat(KEY_DERIVED_HEALTH)
            : player.getHealth();
        float synced = syncedHealth(player.getHealth(), previousDerived, derivedHealth);
        data.setFloat(KEY_DERIVED_HEALTH, derivedHealth);
        applyBodyHealth(player, synced);
        int derivedFood = (int) Math.round(
            20.0 * state.energyReserveJoules()
                / Math.max(
                    1.0,
                    state.physiology()
                        .energyCapacityJoules()));
        // Through the field: FoodStats.setFoodLevel is client-only, and a dedicated server has no such method.
        naturalexceptions.minecraft.SidedVanilla
            .setFoodLevel(player.getFoodStats(), Math.max(0, Math.min(20, derivedFood)));
        applyMovement(player, state);
        thirst(player, state);
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP)
            BodyStatusSync.maybeSend((net.minecraft.entity.player.EntityPlayerMP) player, state, now);
    }

    /** With no water left the body suffers: a warning once, then a heart's half every four seconds. */
    private static void thirst(EntityPlayer player, BiologicalState state) {
        net.minecraft.nbt.NBTTagCompound data = player.getEntityData();
        boolean parched = state.hydrationReserveMillilitres() <= 0.0;
        if (!parched) {
            data.setBoolean(KEY_THIRST_WARNING, false);
            return;
        }
        if (player.capabilities.isCreativeMode) return;
        if (!data.getBoolean(KEY_THIRST_WARNING)) {
            data.setBoolean(KEY_THIRST_WARNING, true);
            player.addChatMessage(
                new net.minecraft.util.ChatComponentText(
                    "\u00a7cYou are parched. Drink: a water bottle, a barrel, a fountain."));
        }
        if (player.ticksExisted % THIRST_TICKS == 0) player.attackEntityFrom(THIRST, THIRST_DAMAGE);
    }

    @SubscribeEvent
    public void onFood(PlayerUseItemEvent.Finish event) {
        if (event == null || event.entityPlayer == null || event.entityPlayer.worldObj.isRemote || event.item == null)
            return;
        BiologicalState state = BiologicalProperties.get(event.entityPlayer)
            .state();
        net.minecraft.item.Item item = event.item.getItem();
        if (item instanceof ItemFood) {
            // Each point of a food's nourishment is a point of the hunger bar, as in vanilla.
            int nourishment = ((ItemFood) item).func_150905_g(event.item);
            state.replenishEnergyJoules(
                Math.max(1, nourishment) * state.physiology()
                    .energyPerHungerPoint());
            state.replenishHydrationMillilitres(Math.max(1, nourishment) * 35.0);
        }
        double drink = drinkMillilitres(event.item);
        if (drink > 0.0) state.replenishHydrationMillilitres(drink);
    }

    /** Water from a drink: a water bottle 250 mL, milk 500 mL. */
    static double drinkMillilitres(net.minecraft.item.ItemStack stack) {
        if (stack == null) return 0.0;
        if (stack.getItem() == net.minecraft.init.Items.potionitem && stack.getItemDamage() == 0) return 250.0;
        if (stack.getItem() == net.minecraft.init.Items.milk_bucket) return 500.0;
        return 0.0;
    }

    /**
     * Work done since last time (vanilla's exhaustion from walking, running, jumping, fighting and healing) spent
     * from the body's stores: four exhaustion cost a hunger point of energy and 40 mL of water. The exhaustion is
     * then cleared so vanilla does not spend it again on the bar (which is drawn from the body's energy).
     */
    static void spendExhaustion(EntityPlayer player, BiologicalState state) {
        net.minecraft.nbt.NBTTagCompound food = new net.minecraft.nbt.NBTTagCompound();
        player.getFoodStats()
            .writeNBT(food);
        float exhaustion = food.getFloat("foodExhaustionLevel");
        if (exhaustion <= 0.0F) return;
        double points = exhaustion / 4.0;
        state.spend(
            points * state.physiology()
                .energyPerHungerPoint(),
            points * 40.0);
        food.setFloat("foodExhaustionLevel", 0.0F);
        food.setFloat("foodSaturationLevel", 0.0F);
        player.getFoodStats()
            .readNBT(food);
    }

    @SubscribeEvent
    public void onFluidContact(PlayerInteractEvent event) {
        if (event == null || event.world == null
            || event.world.isRemote
            || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
            || event.world.getBlock(event.x, event.y, event.z)
                != naturalexceptions.minecraft.registry.NEBlocks.fluidCell)
            return;
        NEWorldSavedData saved = NEWorldSavedData.get(event.world);
        if (saved == null) return;
        FluidParcel parcel = saved.fluids()
            .parcelAt(new FluidCellKey(event.world.provider.dimensionId, event.x, event.y, event.z));
        if (parcel == null) return;
        ResourceId material = parcel.composition()
            .asMap()
            .keySet()
            .iterator()
            .next();
        BiologicalProperties.get(event.entityPlayer)
            .state()
            .expose(
                material,
                parcel.thermalState()
                    .properties(),
                ExposureRoute.CONTACT,
                1.0,
                event.world.getTotalWorldTime());
    }

    private static void applyMovement(EntityLivingBase entity, BiologicalState state) {
        net.minecraft.entity.ai.attributes.IAttributeInstance attribute = entity
            .getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(BIOLOGY_MOVEMENT);
        if (old != null) attribute.removeModifier(old);
        double amount = Math.max(-0.75, Math.min(0.0, state.movementCapability() - 1.0));
        if (amount < -0.001) attribute.applyModifier(
            new AttributeModifier(BIOLOGY_MOVEMENT, "Natural Exceptions biological impairment", amount, 2));
    }
}
