package naturalexceptions.minecraft.gravity;

import java.util.Objects;

import net.minecraftforge.event.entity.living.LivingEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Event subscriber that listens for entity living update ticks and applies
 * nonstandard local gravity physics via {@link NEGravityService}.
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
        service.applyGravity(event.entityLiving);
    }
}
