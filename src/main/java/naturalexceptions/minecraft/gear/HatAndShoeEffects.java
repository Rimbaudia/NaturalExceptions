package naturalexceptions.minecraft.gear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.core.anomaly.RegionKey;
import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.event.EventPhase;
import naturalexceptions.core.event.EventRecord;
import naturalexceptions.core.event.EventRegion;
import naturalexceptions.core.event.P8WorldState;
import naturalexceptions.minecraft.item.ItemTreasureGear;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * What the hats and shoes do on the server (their movement on the client is in {@code GearClientEffects}, the
 * Diver's Bell Helm's clear water in {@code GearClientSight}). A pair of shoes is worn one in each shoe slot.
 */
public final class HatAndShoeEffects {

    /** Gale Striders: speed per shoe, as a share of the base (a pair, twice that). */
    public static final double STRIDE_SPEED_PER_SHOE = 0.1;
    /** With a pair of Gale Striders a full block is a step, not a jump. */
    public static final float PAIR_STEP_HEIGHT = 1.0F;
    public static final float NORMAL_STEP_HEIGHT = 0.5F;
    /** Skystone Sabatons: knockback resistance per shoe (a pair, none at all). */
    public static final double SABATON_FOOTING_PER_SHOE = 0.5;
    /** Diver's Bell Helm: air lasts this many times as long. */
    public static final int DIVER_AIR_FACTOR = 4;
    /** Featherstep Slippers: how near a monster must be to notice you, with one slipper and with a pair. */
    public static final double NOTICE_ONE = 10.0;
    public static final double NOTICE_PAIR = 6.0;
    /** The Weatherwise Hood feels a Star Rain or a Blood Night this far ahead. */
    public static final long FORESIGHT_TICKS = NaturalCalendar.TICKS_PER_DAY;
    /** The Lamplighter's Cap lights only where the sky does not. */
    static final int LAMP_SKY_LIGHT_BELOW = 10;

    private static final UUID STRIDE = UUID.fromString("6f1c2a8e-3b7d-4e51-9a0c-5d2e8b4f7a13");
    private static final UUID FOOTING = UUID.fromString("a2d94c17-58e3-4b6f-8c21-e7f05b9d3c48");

    private final Map<UUID, int[]> lamps = new HashMap<>();
    private final Map<UUID, Set<String>> forewarned = new HashMap<>();
    private final Set<UUID> raisedStep = new HashSet<>();
    /** Server players only: a client player in the same game shares its entity id, and so its key. */
    private final Map<EntityPlayer, int[]> breaths = new java.util.WeakHashMap<>();
    private final Map<UUID, long[]> paced = new HashMap<>();

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null || event.entityLiving == null) return;
        EntityLivingBase living = event.entityLiving;
        if (living instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) living;
            if (!(player instanceof EntityPlayerMP)) return;
            holdBreath(player);
            // A player is updated once per movement packet, not once a tick: pace by the clock.
            long now = player.worldObj.getTotalWorldTime();
            long[] last = paced.computeIfAbsent(player.getUniqueID(), k -> new long[] { -1_000_000L, -1_000_000L });
            if (now - last[0] >= 5 || now < last[0]) {
                last[0] = now;
                lamp(player);
            }
            if (now - last[1] >= 20 || now < last[1]) {
                last[1] = now;
                stride(player);
                foretell(player);
            }
            return;
        }
        // Monsters that still choose their prey the old way (spiders and the like) are told no here.
        if (!living.worldObj.isRemote && living instanceof EntityCreature) {
            EntityCreature creature = (EntityCreature) living;
            Entity prey = creature.getEntityToAttack();
            if (prey instanceof EntityPlayer && !notices(creature, (EntityPlayer) prey)) creature.setTarget(null);
        }
    }

    /** Featherstep Slippers: a monster too far off to have heard you lets you be. */
    @SubscribeEvent
    public void onSetTarget(LivingSetAttackTargetEvent event) {
        if (event == null || !(event.target instanceof EntityPlayer) || !(event.entityLiving instanceof EntityLiving))
            return;
        EntityLiving mob = (EntityLiving) event.entityLiving;
        if (!mob.worldObj.isRemote && !notices(mob, (EntityPlayer) event.target)) mob.setAttackTarget(null);
    }

    /** Weatherwise Hood: lightning never strikes its wearer, nor does the fire it lights catch them. */
    @SubscribeEvent
    public void onLightning(EntityStruckByLightningEvent event) {
        if (event == null || !(event.entity instanceof EntityPlayer)
            || ItemTreasureGear.worn((EntityPlayer) event.entity, Treasure.WEATHERWISE_HOOD) == 0) return;
        event.setCanceled(true);
        EntityPlayer player = (EntityPlayer) event.entity;
        douse(player.worldObj, event.lightning.posX, event.lightning.posY, event.lightning.posZ);
        douse(player.worldObj, player.posX, player.boundingBox.minY, player.posZ);
        player.extinguish();
    }

    private static void douse(World world, double x, double y, double z) {
        int bx = MathHelper.floor_double(x), by = MathHelper.floor_double(y), bz = MathHelper.floor_double(z);
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++)
            if (world.getBlock(bx + dx, by + dy, bz + dz) == net.minecraft.init.Blocks.fire)
                world.setBlockToAir(bx + dx, by + dy, bz + dz);
    }

    static boolean notices(EntityLivingBase mob, EntityPlayer player) {
        if (mob instanceof naturalexceptions.minecraft.entity.CitadelMachine
            && naturalexceptions.minecraft.entity.CitadelMachine.passes(player, mob)) return false; // Sentry's Crest
        if (mournedBy(mob, player)) return false; // Mourner's Veil
        return notices(
            mob.getDistanceToEntity(player),
            ItemTreasureGear.worn(player, Treasure.FEATHERSTEP_SLIPPERS),
            mob.getAITarget() == player);
    }

    /** Mourner's Veil: the undead (not the great ones) pass its wearer by, until struck. */
    public static boolean mournedBy(EntityLivingBase mob, EntityPlayer player) {
        return mob.getCreatureAttribute() == net.minecraft.entity.EnumCreatureAttribute.UNDEAD
            && !(mob instanceof net.minecraft.entity.boss.IBossDisplayData)
            && mob.getAITarget() != player
            && ItemTreasureGear.worn(player, Treasure.MOURNERS_VEIL) > 0;
    }

    /** How long the Gathered Heart must rest after holding its wearer back. */
    public static final long HEART_REST = NaturalCalendar.TICKS_PER_DAY;

    /** Gathered Heart: once a day, a blow that would kill its wearer does not; the dead hold them back. */
    @SubscribeEvent(priority = cpw.mods.fml.common.eventhandler.EventPriority.LOWEST)
    public void onHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event == null || event.isCanceled() || !(event.entityLiving instanceof EntityPlayerMP)) return;
        EntityPlayer player = (EntityPlayer) event.entityLiving;
        if (event.ammount < player.getHealth() || event.source == net.minecraft.util.DamageSource.outOfWorld) return;
        net.minecraft.item.ItemStack heart = null;
        PlayerGear gear = PlayerGear.get(player);
        for (GearSlot slot : GearSlot.values()) {
            net.minecraft.item.ItemStack stack = gear.get(slot);
            if (stack != null && stack.getItem()
                == naturalexceptions.minecraft.registry.NEItems.TREASURES.get(Treasure.GATHERED_HEART)) heart = stack;
        }
        long now = player.worldObj.getTotalWorldTime();
        if (heart == null || !heartReady(heart, now)) return;
        if (!heart.hasTagCompound()) heart.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        heart.getTagCompound()
            .setLong("Held", now);
        event.setCanceled(true);
        player.setHealth(6.0F);
        player.addPotionEffect(
            new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.regeneration.id, 100, 1));
        player.addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.resistance.id, 60, 2));
        player.worldObj.playSoundAtEntity(player, "mob.ghast.moan", 1.0F, 1.6F);
        player.addChatMessage(new ChatComponentText("\u00a7e\u00a7oThe dead hold you back."));
    }

    public static boolean heartReady(net.minecraft.item.ItemStack heart, long now) {
        if (!heart.hasTagCompound() || !heart.getTagCompound()
            .hasKey("Held")) return true;
        long held = heart.getTagCompound()
            .getLong("Held");
        return now - held >= HEART_REST || now < held;
    }

    /** Whether a monster this far away notices a wearer of so many slippers; one struck by you always does. */
    public static boolean notices(double distance, int slippers, boolean provoked) {
        if (slippers <= 0 || provoked) return true;
        return distance <= (slippers >= 2 ? NOTICE_PAIR : NOTICE_ONE);
    }

    /**
     * Diver's Bell Helm: give back all but one breath in every few the water takes. Counted per update, not per
     * tick: a player on a server is updated once for each movement packet, and those come unevenly.
     */
    private void holdBreath(EntityPlayer player) {
        if (player.getAir() >= 300 || !player.isInsideOfMaterial(Material.water)
            || ItemTreasureGear.worn(player, Treasure.DIVERS_BELL_HELM) == 0) return;
        int[] breaths = this.breaths.computeIfAbsent(player, k -> new int[1]);
        if (++breaths[0] % DIVER_AIR_FACTOR != 0) player.setAir(player.getAir() + 1);
    }

    /** Gale Striders and Skystone Sabatons: speed, footing, and with Striders on both feet a higher step. */
    private void stride(EntityPlayer player) {
        int striders = ItemTreasureGear.worn(player, Treasure.GALE_STRIDERS);
        int sabatons = ItemTreasureGear.worn(player, Treasure.SKYSTONE_SABATONS);
        modify(
            player,
            SharedMonsterAttributes.movementSpeed,
            STRIDE,
            "Gale Striders",
            striders * STRIDE_SPEED_PER_SHOE,
            1);
        modify(
            player,
            SharedMonsterAttributes.knockbackResistance,
            FOOTING,
            "Skystone Sabatons",
            Math.min(1.0, sabatons * SABATON_FOOTING_PER_SHOE),
            0);
        UUID id = player.getUniqueID();
        if (striders >= 2) {
            player.stepHeight = PAIR_STEP_HEIGHT;
            raisedStep.add(id);
        } else if (raisedStep.remove(id)) player.stepHeight = NORMAL_STEP_HEIGHT;
    }

    private static void modify(EntityLivingBase living, IAttribute attribute, UUID id, String name, double amount,
        int operation) {
        IAttributeInstance instance = living.getEntityAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (current != null && current.getAmount() == amount) return;
        if (current != null) instance.removeModifier(current);
        if (amount != 0.0) instance.applyModifier(new AttributeModifier(id, name, amount, operation).setSaved(false));
    }

    /** Lamplighter's Cap: keep a Lamplight at the wearer's head (or feet) wherever the sky leaves it dark. */
    private void lamp(EntityPlayer player) {
        UUID id = player.getUniqueID();
        int[] last = lamps.get(id);
        World world = player.worldObj;
        int[] spot = null;
        boolean lantern = player.getCurrentEquippedItem() != null && player.getCurrentEquippedItem()
            .getItem() == naturalexceptions.minecraft.registry.NEItems.reliquaryLantern
            || naturalexceptions.minecraft.item.ToolEdge.glowsIn(player);
        if ((ItemTreasureGear.worn(player, Treasure.LAMPLIGHTERS_CAP) > 0 || lantern) && NEBlocks.lamplight != null) {
            int x = MathHelper.floor_double(player.posX), z = MathHelper.floor_double(player.posZ);
            int feet = MathHelper.floor_double(player.boundingBox.minY);
            int y = open(world, x, feet + 1, z) ? feet + 1 : open(world, x, feet, z) ? feet : Integer.MIN_VALUE;
            if (y != Integer.MIN_VALUE && world.getSavedLightValue(EnumSkyBlock.Sky, x, y, z) - world.skylightSubtracted
                < LAMP_SKY_LIGHT_BELOW) spot = new int[] { world.provider.dimensionId, x, y, z };
        }
        if (last != null && spot != null && java.util.Arrays.equals(last, spot)) return;
        if (last != null && last[0] == world.provider.dimensionId
            && world.getBlock(last[1], last[2], last[3]) == NEBlocks.lamplight)
            world.setBlockToAir(last[1], last[2], last[3]);
        if (spot == null) {
            lamps.remove(id);
            return;
        }
        world.setBlock(spot[1], spot[2], spot[3], NEBlocks.lamplight, 0, 3);
        lamps.put(id, spot);
    }

    private static boolean open(World world, int x, int y, int z) {
        if (y <= 0 || y >= 255) return false;
        net.minecraft.block.Block block = world.getBlock(x, y, z);
        return block == net.minecraft.init.Blocks.air || block == NEBlocks.lamplight;
    }

    /** Weatherwise Hood: a word, once, the day before a Star Rain or a Blood Night over where you stand. */
    private void foretell(EntityPlayer player) {
        if (ItemTreasureGear.worn(player, Treasure.WEATHERWISE_HOOD) == 0) return;
        NEWorldSavedData data = NEWorldSavedData.get(player.worldObj);
        P8WorldState p8 = data == null ? null : data.p8();
        if (p8 == null) return;
        List<EventRecord> coming = coming(
            p8.events()
                .snapshot()
                .events(),
            player.worldObj.getTotalWorldTime(),
            MathHelper.floor_double(player.posX),
            MathHelper.floor_double(player.posZ));
        if (coming.isEmpty()) return;
        Set<String> told = forewarned.computeIfAbsent(player.getUniqueID(), k -> new HashSet<>());
        for (EventRecord event : coming) {
            if (!told.add(
                event.id()
                    .toString()))
                continue;
            player.addChatMessage(
                new ChatComponentText(
                    EventRecord.PROTECTED_STAR_RAIN.equals(event.type())
                        ? "\u00a7bYour hood prickles: the stars will fall within a day."
                        : "\u00a7cYour hood prickles: a Blood Night will fall here within a day."));
        }
    }

    /** The Star Rains, and the Blood Nights over this block, that begin within the next day. */
    public static List<EventRecord> coming(List<EventRecord> events, long now, int blockX, int blockZ) {
        RegionKey key = RegionKey.fromBlock(blockX, blockZ);
        EventRegion here = EventRegion.of((int) key.x(), (int) key.z());
        List<EventRecord> coming = new ArrayList<>();
        for (EventRecord event : events) {
            long until = event.scheduledAtTick() - now;
            if (event.phase() != EventPhase.SCHEDULED || until <= 0 || until > FORESIGHT_TICKS) continue;
            boolean overHere = event.targetRegion()
                .isGlobal()
                || event.targetRegion()
                    .equals(here);
            if (EventRecord.PROTECTED_STAR_RAIN.equals(event.type())
                || P8WorldState.BLOOD_NIGHT.equals(event.type()) && overHere) coming.add(event);
        }
        return coming;
    }
}
