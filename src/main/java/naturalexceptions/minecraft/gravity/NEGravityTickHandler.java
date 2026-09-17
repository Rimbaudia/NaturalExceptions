package naturalexceptions.minecraft.gravity;

import java.util.List;
import java.util.Objects;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraftforge.event.entity.living.LivingEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Event subscriber that listens for entity living update ticks and world ticks,
 * applying nonstandard local gravity physics to living entities, items, and projectiles
 * via {@link NEGravityService}.
 */
public final class NEGravityTickHandler {

    private final NEGravityService service;

    public NEGravityTickHandler(NEGravityService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public NEGravityService service() {
        return service;
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null || event.entityLiving == null) {
            return;
        }
        // Players have their gravity, locomotion, grounding, and contact handled
        // exclusively by NEPlayerMovementHandler under nonstandard gravity.
        // NEGravityTickHandler must never touch players to avoid duplicate gravity
        // and un-cancelled vanilla-compensation velocity injection.
        if (event.entityLiving instanceof EntityPlayer) {
            return;
        }
        service.applyGravity(event.entityLiving);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.START || event.world == null) {
            return;
        }
        if (!service.isNonstandardGravityActive()) {
            return;
        }
        List<?> loaded = event.world.loadedEntityList;
        for (int i = 0; i < loaded.size(); i++) {
            Object obj = loaded.get(i);
            if (obj instanceof EntityItem) {
                service.applyGravity((EntityItem) obj);
            } else if (obj instanceof EntityArrow) {
                service.applyGravity((EntityArrow) obj);
            }
        }
    }
}
