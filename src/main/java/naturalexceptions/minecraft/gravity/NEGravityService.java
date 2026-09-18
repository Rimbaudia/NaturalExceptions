package naturalexceptions.minecraft.gravity;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.projectile.EntityArrow;

import naturalexceptions.core.gravity.GravityField;
import naturalexceptions.core.gravity.GravitySample;
import naturalexceptions.core.gravity.LocalEntityFrame;
import naturalexceptions.core.gravity.UniformGravityField;
import naturalexceptions.core.gravity.Vector3d;

/**
 * Service managing the active gravitational field in Minecraft.
 *
 * <p>
 * Decouples gravity evaluation from hardcoded Y-axis physics. Computes the necessary
 * velocity delta required to counteract vanilla's hardcoded downward acceleration
 * and applies arbitrary 3D local gravity vectors to living entities, items, and projectiles.
 */
public final class NEGravityService {

    public static final double DEFAULT_VANILLA_GRAVITY = 0.08;
    public static final double DEFAULT_ITEM_GRAVITY = 0.04;
    public static final double DEFAULT_ARROW_GRAVITY = 0.05;

    private static final NEGravityService GLOBAL = new NEGravityService();

    private final Map<Long, LocalEntityFrame> entityFrames = new ConcurrentHashMap<>();
    private final Map<Long, Float> entityLastYaw = new ConcurrentHashMap<>();

    private GravityField activeField;
    private double vanillaDownwardAcceleration;
    private boolean enabled;

    public NEGravityService() {
        this.activeField = UniformGravityField.STANDARD_MINECRAFT;
        this.vanillaDownwardAcceleration = DEFAULT_VANILLA_GRAVITY;
        this.enabled = true;
    }

    public static NEGravityService global() {
        return GLOBAL;
    }

    public synchronized GravityField activeField() {
        return activeField;
    }

    public synchronized void setActiveField(GravityField field) {
        this.activeField = field != null ? field : UniformGravityField.STANDARD_MINECRAFT;
    }

    public synchronized void resetToDefault() {
        this.activeField = UniformGravityField.STANDARD_MINECRAFT;
        this.vanillaDownwardAcceleration = DEFAULT_VANILLA_GRAVITY;
        this.enabled = true;
        this.entityFrames.clear();
        this.entityLastYaw.clear();
        NEPlayerPresentationService.instance()
            .reset();
    }

    public synchronized double vanillaDownwardAcceleration() {
        return vanillaDownwardAcceleration;
    }

    public synchronized void setVanillaDownwardAcceleration(double g) {
        this.vanillaDownwardAcceleration = Math.max(0.0, g);
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private boolean debugLogging = false;

    public synchronized boolean isDebugLogging() {
        return debugLogging;
    }

    public synchronized void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    public synchronized boolean isNonstandardGravityActive() {
        if (!enabled) {
            return false;
        }
        return activeField != UniformGravityField.STANDARD_MINECRAFT;
    }

    public GravitySample sampleAt(double x, double y, double z) {
        GravityField field;
        synchronized (this) {
            field = activeField;
        }
        return field.sampleAt(x, y, z);
    }

    public GravitySample sampleFor(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return sampleAt(entity.posX, entity.posY, entity.posZ);
    }

    public static long entityKey(Entity entity) {
        if (entity == null) {
            return 0L;
        }
        long id = entity.getEntityId();
        if (entity.worldObj != null && entity.worldObj.isRemote) {
            return id | (1L << 32);
        }
        return id;
    }

    /**
     * Clears cached frame and yaw state for the given entity.
     */
    public void clearEntityFrame(Entity entity) {
        if (entity != null) {
            long key = entityKey(entity);
            entityFrames.remove(key);
            entityLastYaw.remove(key);
        }
    }

    /**
     * Gets or updates the persistent parallel-transported local frame for an entity.
     *
     * <p>
     * Ensures continuous heading transport across changing localUp surfaces without
     * coordinate singularities at the poles. The frame is parallel-transported along the geodesic
     * from the previous localUp to the new localUp, and updated with any mouse yaw change.
     */
    public LocalEntityFrame getOrUpdateLocalFrame(EntityLivingBase entity) {
        Objects.requireNonNull(entity, "entity");
        GravitySample sample = sampleFor(entity);
        long key = entityKey(entity);

        float currentYaw = entity.rotationYaw;
        Float lastYaw = entityLastYaw.get(key);
        float yawDelta = 0.0F;
        if (lastYaw != null) {
            yawDelta = currentYaw - lastYaw;
            while (yawDelta < -180.0F) yawDelta += 360.0F;
            while (yawDelta >= 180.0F) yawDelta -= 360.0F;
        }
        entityLastYaw.put(key, currentYaw);

        LocalEntityFrame frame = entityFrames.get(key);
        if (frame == null) {
            frame = LocalEntityFrame.deriveFrom(sample, currentYaw);
        } else {
            frame = frame.transportTo(sample.localUp(), yawDelta, sample.sourceId());
        }
        entityFrames.put(key, frame);
        return frame;
    }

    /**
     * Derives the local entity reference frame based on current position and heading.
     */
    public LocalEntityFrame getLocalFrame(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        if (entity instanceof EntityLivingBase) {
            return getOrUpdateLocalFrame((EntityLivingBase) entity);
        }
        GravitySample sample = sampleFor(entity);
        return LocalEntityFrame.deriveFrom(sample, entity.rotationYaw);
    }

    /**
     * Generic acceleration delta application to any entity, counteracting its
     * specific vanilla downward acceleration constant.
     */
    public void applyGravity(Entity entity, double vanillaDownwardG) {
        if (!enabled || entity == null) {
            return;
        }

        GravitySample sample = sampleFor(entity);
        Vector3d localAcc = sample.acceleration();

        // Delta needed: localAcc - (0, -vanillaDownwardG, 0)
        double deltaX = localAcc.x;
        double deltaY = localAcc.y + vanillaDownwardG;
        double deltaZ = localAcc.z;

        if (Math.abs(deltaX) > 1e-6 || Math.abs(deltaY) > 1e-6 || Math.abs(deltaZ) > 1e-6) {
            entity.motionX += deltaX;
            entity.motionY += deltaY;
            entity.motionZ += deltaZ;
        }
    }

    /**
     * Applies gravitational acceleration delta to the given living entity.
     */
    public void applyGravity(EntityLivingBase entity) {
        applyGravity(entity, vanillaDownwardAcceleration);
    }

    /**
     * Applies gravitational acceleration delta to a dropped item entity.
     */
    public void applyGravity(EntityItem item) {
        applyGravity(item, DEFAULT_ITEM_GRAVITY);
    }

    /**
     * Applies gravitational acceleration delta to an arrow / projectile.
     */
    public void applyGravity(EntityArrow arrow) {
        applyGravity(arrow, DEFAULT_ARROW_GRAVITY);
    }
}
