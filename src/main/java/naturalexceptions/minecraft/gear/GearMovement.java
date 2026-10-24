package naturalexceptions.minecraft.gear;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.minecraft.item.ItemTreasureGear;
import naturalexceptions.minecraft.network.PacketGearAction;

/**
 * Server side of movement gear: checks the gear is worn, forgives falls it softens, and plays its effect for
 * everyone nearby. The owner's client moves the player (see {@code GearClientEffects}).
 */
public final class GearMovement {

    /** A landing this soon after a glide, climb or cloud jump is forgiven. */
    static final long ASSIST_GRACE_TICKS = 12L;
    private static final long SOUND_GAP_TICKS = 16L;

    private static final Map<EntityPlayer, Long> LAST_ASSIST = new WeakHashMap<>();
    private static final Map<EntityPlayer, Long> LAST_SOUND = new WeakHashMap<>();
    private static final Map<EntityPlayer, Long> LAST_DASH = new WeakHashMap<>();
    private static final Map<EntityPlayer, Long> LAST_AEGIS = new WeakHashMap<>();
    /** When each player's last glide, climb or double jump was accepted. */
    private static final Map<EntityPlayer, Long> LAST_GLIDE = new WeakHashMap<>(), LAST_CLIMB = new WeakHashMap<>(),
        LAST_JUMP = new WeakHashMap<>();
    /** The client reports a glide or climb every 5 ticks; a little slack for lag, none for spam. */
    static final long CONTINUOUS_GAP_TICKS = 3L;
    /** One double jump an airborne spell; the client can't jump again within half a second. */
    static final long JUMP_GAP_TICKS = 10L;

    public static void onAction(EntityPlayerMP player, PacketGearAction.Action action) {
        Treasure needed = action == PacketGearAction.Action.DOUBLE_JUMP ? Treasure.CLOUD_FLASK
            : action == PacketGearAction.Action.GLIDE ? Treasure.GLIDER_CLOAK
                : action == PacketGearAction.Action.DASH ? Treasure.GALEHEART_BELT : Treasure.CLIMBING_CLAWS;
        if (ItemTreasureGear.worn(player, needed) == 0) return;
        long now = player.worldObj.getTotalWorldTime();
        if (!plausible(player, action, now)) return;
        if (action == PacketGearAction.Action.DASH) {
            synchronized (LAST_DASH) {
                Long last = LAST_DASH.get(player);
                // The client keeps the same cooldown; a little slack for lag, none for spam.
                if (last != null && now - last < RelicRules.DASH_COOLDOWN_TICKS - 10) return;
                LAST_DASH.put(player, now);
            }
        }
        player.fallDistance = 0.0F;
        synchronized (LAST_ASSIST) {
            LAST_ASSIST.put(player, now);
        }
        switch (action) {
            case DOUBLE_JUMP:
                burst(player, "cloud", 14, 0.35, 0.02);
                player.worldObj.playSoundAtEntity(player, "naturalexceptions:gear.cloud_jump", 0.8F, 1.0F);
                break;
            case GLIDE:
                if (soundDue(player, now)) player.worldObj.playSoundAtEntity(
                    player,
                    "naturalexceptions:gear.glide",
                    0.4F,
                    0.9F + player.getRNG()
                        .nextFloat() * 0.2F);
                break;
            case DASH:
                burst(player, "cloud", 24, 0.5, 0.08);
                player.worldObj.playSoundAtEntity(player, "naturalexceptions:gear.dash", 0.9F, 1.0F);
                break;
            case CLIMB:
            default:
                if (soundDue(player, now)) player.worldObj.playSoundAtEntity(
                    player,
                    "naturalexceptions:gear.claw_scrape",
                    0.5F,
                    0.9F + player.getRNG()
                        .nextFloat() * 0.3F);
                break;
        }
    }

    /**
     * Whether the action fits what the server sees of the player, so a tampered client can't use the gear's fall reset
     * where the gear does nothing: a glide in the air, a climb at a wall, a double jump off the ground, none faster
     * than
     * the real client sends them. (Movement itself is the client's; this only guards what the server grants.)
     */
    static boolean plausible(EntityPlayer player, PacketGearAction.Action action, long now) {
        switch (action) {
            case GLIDE:
                return !player.onGround && due(LAST_GLIDE, player, now, CONTINUOUS_GAP_TICKS);
            case CLIMB:
                return nearWall(player) && due(LAST_CLIMB, player, now, CONTINUOUS_GAP_TICKS);
            case DOUBLE_JUMP:
                return !player.onGround && due(LAST_JUMP, player, now, JUMP_GAP_TICKS);
            default:
                return true;
        }
    }

    private static boolean due(Map<EntityPlayer, Long> last, EntityPlayer player, long now, long gap) {
        synchronized (last) {
            Long then = last.get(player);
            if (then != null && now - then < gap) return false;
            last.put(player, now);
            return true;
        }
    }

    /** A solid block beside the player's body, within a little of it (the server's view of a client's wall). */
    private static boolean nearWall(EntityPlayer player) {
        net.minecraft.util.AxisAlignedBB box = player.boundingBox.expand(0.35, 0.0, 0.35);
        int x0 = (int) Math.floor(box.minX), x1 = (int) Math.floor(box.maxX);
        int y0 = (int) Math.floor(box.minY), y1 = (int) Math.floor(box.maxY);
        int z0 = (int) Math.floor(box.minZ), z1 = (int) Math.floor(box.maxZ);
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) if (player.worldObj.getBlock(x, y, z)
                .getMaterial()
                .blocksMovement()) return true;
        return false;
    }

    private static boolean soundDue(EntityPlayer player, long now) {
        synchronized (LAST_SOUND) {
            Long last = LAST_SOUND.get(player);
            if (last != null && now - last < SOUND_GAP_TICKS) return false;
            LAST_SOUND.put(player, now);
            return true;
        }
    }

    private static void burst(EntityPlayer player, String particle, int count, double spread, double speed) {
        if (player.worldObj instanceof WorldServer) ((WorldServer) player.worldObj)
            .func_147487_a(particle, player.posX, player.posY - 0.2, player.posZ, count, spread, 0.05, spread, speed);
    }

    /** Landing on Cloud, or with Windbloom Tea still working. */
    static boolean softLanding(net.minecraft.entity.EntityLivingBase living) {
        if (naturalexceptions.minecraft.item.ItemWindbloomTea.active(living, living.worldObj.getTotalWorldTime()))
            return true;
        int x = net.minecraft.util.MathHelper.floor_double(living.posX);
        int y = net.minecraft.util.MathHelper.floor_double(living.boundingBox.minY - 0.2);
        int z = net.minecraft.util.MathHelper.floor_double(living.posZ);
        return naturalexceptions.minecraft.registry.NEBlocks.cloud != null
            && living.worldObj.getBlock(x, y, z) == naturalexceptions.minecraft.registry.NEBlocks.cloud;
    }

    /** Healing multiplier from Bands of Mending: half again per band, up to two. */
    public static double mendingMultiplier(int bands) {
        return 1.0 + 0.5 * Math.max(0, Math.min(2, bands));
    }

    /**
     * Soft landings: Cloud underfoot or Windbloom Tea take the whole fall for anyone; for players the Horseshoe
     * Pendant cancels every fall and other gear forgives a landing right after it was used.
     */
    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj.isRemote) return;
        net.minecraft.entity.EntityLivingBase living = event.entityLiving;
        if (softLanding(living)) {
            event.setCanceled(true);
            return;
        }
        if (!(living instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) living;
        if (ItemTreasureGear.worn(player, Treasure.HORSESHOE_PENDANT) > 0
            || ItemTreasureGear.worn(player, Treasure.CLOUD_TREADS) > 0) {
            event.setCanceled(true);
            return;
        }
        Long last;
        synchronized (LAST_ASSIST) {
            last = LAST_ASSIST.get(player);
        }
        if (last != null && player.worldObj.getTotalWorldTime() - last <= ASSIST_GRACE_TICKS) event.setCanceled(true);
    }

    /** Aegis Sigil: a blow on its wearer answers with a gale that throws nearby foes back and staggers them. */
    @SubscribeEvent
    public void onHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event == null || !(event.entityLiving instanceof EntityPlayer) || event.entityLiving.worldObj.isRemote)
            return;
        EntityPlayer player = (EntityPlayer) event.entityLiving;
        if (event.source.getEntity() == null || ItemTreasureGear.worn(player, Treasure.AEGIS_SIGIL) == 0) return;
        long now = player.worldObj.getTotalWorldTime();
        synchronized (LAST_AEGIS) {
            Long last = LAST_AEGIS.get(player);
            if (!RelicRules.ready(last == null ? Long.MIN_VALUE : last, now, RelicRules.AEGIS_COOLDOWN_TICKS)) return;
            LAST_AEGIS.put(player, now);
        }
        double r = RelicRules.AEGIS_RADIUS;
        @SuppressWarnings("unchecked")
        java.util.List<net.minecraft.entity.EntityLivingBase> near = player.worldObj
            .getEntitiesWithinAABB(net.minecraft.entity.EntityLivingBase.class, player.boundingBox.expand(r, 2.0, r));
        for (net.minecraft.entity.EntityLivingBase foe : near) {
            if (foe == player || !(foe instanceof net.minecraft.entity.monster.IMob || foe == event.source.getEntity()))
                continue;
            double dx = foe.posX - player.posX, dz = foe.posZ - player.posZ;
            double d = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            if (d > r) continue;
            foe.motionX = dx / d * RelicRules.AEGIS_PUSH;
            foe.motionZ = dz / d * RelicRules.AEGIS_PUSH;
            foe.motionY = 0.45;
            foe.velocityChanged = true;
            // Staggered: slowed almost to a stop and made to find its way back (old-style mobs also skip a swing).
            foe.attackTime = RelicRules.AEGIS_STAGGER_TICKS;
            foe.addPotionEffect(
                new net.minecraft.potion.PotionEffect(
                    net.minecraft.potion.Potion.moveSlowdown.id,
                    RelicRules.AEGIS_STAGGER_TICKS,
                    3));
            if (foe instanceof net.minecraft.entity.EntityLiving)
                ((net.minecraft.entity.EntityLiving) foe).getNavigator()
                    .clearPathEntity();
        }
        if (player.worldObj instanceof WorldServer) {
            WorldServer server = (WorldServer) player.worldObj;
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI * 2.0 / 24.0;
                server.func_147487_a(
                    "cloud",
                    player.posX + Math.cos(a) * 0.6,
                    player.posY - 0.6,
                    player.posZ + Math.sin(a) * 0.6,
                    0,
                    Math.cos(a),
                    0.1,
                    Math.sin(a),
                    0.45);
            }
        }
        player.worldObj.playSoundAtEntity(player, "naturalexceptions:gear.aegis", 1.0F, 1.0F);
    }
}
