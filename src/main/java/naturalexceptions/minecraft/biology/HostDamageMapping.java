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

    public static final ResourceId HEAD = ResourceId.of("naturalexceptions", "head");
    public static final ResourceId TORSO = ResourceId.of("naturalexceptions", "torso");
    public static final ResourceId LEFT_ARM = ResourceId.of("naturalexceptions", "left_arm");
    public static final ResourceId RIGHT_ARM = ResourceId.of("naturalexceptions", "right_arm");
    public static final ResourceId LEFT_LEG = ResourceId.of("naturalexceptions", "left_leg");
    public static final ResourceId RIGHT_LEG = ResourceId.of("naturalexceptions", "right_leg");

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

    /**
     * Where a hit lands. {@code height} is how high on the body it struck (0 the feet, 1 the top of the head) and
     * {@code side} which side it came from (-1 the body's left, +1 its right), each NaN when not known; {@code roll}
     * is a random number in [0, 1) for what the geometry leaves open. Known hits land where they strike: high on the
     * head, low on a leg, in the middle on the torso or, from the side, an arm. Hits from nowhere in particular
     * (a blast, a burn) land on a part drawn by its size: the torso most often, the head least.
     */
    public static ResourceId region(double height, double side, double roll) {
        if (Double.isNaN(height)) return anyPart(roll);
        boolean left = Double.isNaN(side) ? roll < 0.5 : side < 0.0;
        double lateral = Double.isNaN(side) ? 0.0 : Math.abs(side);
        if (height >= 0.82) return HEAD;
        if (height < 0.42) return left ? LEFT_LEG : RIGHT_LEG;
        // The middle: an arm when it comes from the side (and sometimes even from the front), else the torso.
        boolean arm = lateral > 0.55 ? roll < 0.8 : roll < 0.25;
        return arm ? (left ? LEFT_ARM : RIGHT_ARM) : TORSO;
    }

    /**
     * Where a strike meets a body: a ray from (sx, sy, sz) along (dx, dy, dz) (an arrow's flight, a blow's aim) traced
     * into the body's box. Returns {height, side}: how high it met the body (0 its feet, 1 the top of its head) and
     * from which side of its facing ({@code yaw}, degrees) it came (-1 its left, +1 its right). A ray that misses the
     * box (a glancing hit) meets it at the height it starts from, clamped to the body, on the side it comes from.
     */
    public static double[] strike(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
        double sx, double sy, double sz, double dx, double dy, double dz, double yaw) {
        double near = -2.0, far = 16.0;
        double[] lo = { minX, minY, minZ }, hi = { maxX, maxY, maxZ }, s = { sx, sy, sz }, d = { dx, dy, dz };
        boolean hit = true;
        for (int axis = 0; axis < 3 && hit; axis++) {
            if (Math.abs(d[axis]) < 1.0e-9) {
                if (s[axis] < lo[axis] || s[axis] > hi[axis]) hit = false;
                continue;
            }
            double t1 = (lo[axis] - s[axis]) / d[axis], t2 = (hi[axis] - s[axis]) / d[axis];
            near = Math.max(near, Math.min(t1, t2));
            far = Math.min(far, Math.max(t1, t2));
            if (near > far) hit = false;
        }
        double hx, hy, hz;
        if (hit) {
            hx = sx + dx * near;
            hy = sy + dy * near;
            hz = sz + dz * near;
        } else {
            hx = sx;
            hy = Math.max(minY, Math.min(maxY, sy));
            hz = sz;
        }
        double cx = (minX + maxX) / 2.0, cz = (minZ + maxZ) / 2.0, halfWidth = Math.max(0.05, (maxX - minX) / 2.0);
        double r = Math.toRadians(yaw);
        double side = ((hx - cx) * -Math.cos(r) + (hz - cz) * -Math.sin(r)) / halfWidth; // + towards the right
        double height = (hy - minY) / Math.max(0.1, maxY - minY);
        return new double[] { Math.max(0.0, Math.min(1.0, height)), Math.max(-1.0, Math.min(1.0, side)) };
    }

    /** A part drawn by its share of the body. */
    public static ResourceId anyPart(double roll) {
        if (roll < 0.40) return TORSO;
        if (roll < 0.54) return LEFT_ARM;
        if (roll < 0.68) return RIGHT_ARM;
        if (roll < 0.81) return LEFT_LEG;
        if (roll < 0.94) return RIGHT_LEG;
        return HEAD;
    }

    /** A fall lands on both legs: the parts that share its energy. */
    public static ResourceId[] fallRegions() {
        return new ResourceId[] { LEFT_LEG, RIGHT_LEG };
    }
}
