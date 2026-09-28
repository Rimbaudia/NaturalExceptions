package naturalexceptions.minecraft.biology;

import net.minecraft.util.DamageSource;

import naturalexceptions.core.biology.PhysicalInteraction;
import naturalexceptions.core.id.ResourceId;

/**
 * Coarse translation of a Minecraft damage source into what physically happened. Only sources with a clear
 * mechanical or thermal meaning become P10 interactions; the rest (drowning, starvation, magic, the void,
 * suffocation, lightning, unknown) stay ordinary host damage rather than being disguised as a blow to the torso.
 */
public final class HostDamageMapping {

    public enum Category {
        /** A blow, bite, projectile, blast or falling object. */
        PHYSICAL_HIT,
        /** Landing hard: the legs take it. */
        FALL,
        /** Fire, lava, hot surfaces. */
        THERMAL,
        /** No physical interaction is modelled; the host's own damage applies unchanged. */
        UNMODELLED
    }

    public static final ResourceId TORSO = ResourceId.of("naturalexceptions", "torso");
    public static final ResourceId LEFT_LEG = ResourceId.of("naturalexceptions", "left_leg");

    private HostDamageMapping() {}

    public static Category categorize(DamageSource source) {
        if (source == null) return Category.UNMODELLED;
        String type = source.getDamageType();
        if (source.isFireDamage()) return Category.THERMAL;
        if ("fall".equals(type)) return Category.FALL;
        if (source.isMagicDamage() || source == DamageSource.outOfWorld
            || source == DamageSource.drown
            || source == DamageSource.starve
            || source == DamageSource.inWall
            || source == DamageSource.wither
            || source == DamageSource.generic
            || "lightningBolt".equals(type)) return Category.UNMODELLED;
        if (source.getEntity() != null || source.getSourceOfDamage() != null
            || source.isExplosion()
            || source.isProjectile()
            || source == DamageSource.cactus
            || source == DamageSource.anvil
            || source == DamageSource.fallingBlock) return Category.PHYSICAL_HIT;
        return Category.UNMODELLED;
    }

    /** The interaction type for a modelled category (a blade hit may still upgrade PHYSICAL_HIT to CUT). */
    public static PhysicalInteraction.Type interactionType(Category category) {
        return category == Category.THERMAL ? PhysicalInteraction.Type.THERMAL : PhysicalInteraction.Type.IMPACT;
    }

    public static ResourceId region(Category category) {
        return category == Category.FALL ? LEFT_LEG : TORSO;
    }
}
