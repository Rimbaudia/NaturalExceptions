package naturalexceptions.minecraft.worldgen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.entity.living.LivingEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.core.worldgen.Landmarks;

/**
 * The great stone arches are Wind Gates: walk beneath one's span and the wind through it carries you on, a Tailwind
 * (quicker feet and a higher jump) for a few minutes.
 */
public final class WindGates {

    /** How long a Tailwind lasts, in ticks. */
    public static final int TAILWIND_TICKS = 3 * 60 * 20;
    /** A gate does not speak to you again for this long after it has. */
    static final long QUIET_TICKS = 60 * 20;
    /** How far around a player to look for arches (an arch reaches less than this from its centre). */
    static final int LOOK = 96;

    private static Landmarks field;
    private final Map<UUID, Long> lastSpoke = new HashMap<>();
    private final Map<UUID, Long> lastChecked = new HashMap<>();
    static final long CHECK_TICKS = 10;

    private static synchronized Landmarks field(long seed) {
        if (field == null || field.seed() != seed) field = new Landmarks(seed);
        return field;
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null || !(event.entityLiving instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;
        if (player.worldObj.provider.dimensionId != 0) return;
        // A player is updated once per movement packet, not once a tick: pace by the clock.
        long now = player.worldObj.getTotalWorldTime();
        Long checked = lastChecked.get(player.getUniqueID());
        if (checked != null && now - checked < CHECK_TICKS && now >= checked) return;
        lastChecked.put(player.getUniqueID(), now);
        Landmarks landmarks = field(NEOverworldGenerator.landmarkSeed(player.worldObj));
        int x = (int) Math.floor(player.posX), z = (int) Math.floor(player.posZ);
        for (Landmarks.Landmark arch : landmarks.near(x - LOOK, z - LOOK, x + LOOK, z + LOOK)) {
            if (arch.kind() != Landmarks.Kind.ARCH
                || !Landmarks.underSpan(arch, player.posX, player.boundingBox.minY, player.posZ)) continue;
            tailwind(player);
            return;
        }
    }

    private void tailwind(EntityPlayerMP player) {
        PotionEffect speed = player.getActivePotionEffect(Potion.moveSpeed);
        if (speed != null && speed.getDuration() > TAILWIND_TICKS - 200) return; // just given
        player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, TAILWIND_TICKS, 0));
        player.addPotionEffect(new PotionEffect(Potion.jump.id, TAILWIND_TICKS, 0));
        long now = player.worldObj.getTotalWorldTime();
        Long last = lastSpoke.get(player.getUniqueID());
        if (last != null && now - last < QUIET_TICKS) return;
        lastSpoke.put(player.getUniqueID(), now);
        player.worldObj.playSoundAtEntity(player, "naturalexceptions:trap.gale", 0.6F, 1.3F);
        player.addChatMessage(new ChatComponentText("§bThe wind through the gate catches you: a Tailwind."));
    }
}
