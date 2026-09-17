package naturalexceptions.minecraft.gravity;

import java.util.Objects;

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

    /**
     * Derives the local entity reference frame based on current position and heading.
     */
    public LocalEntityFrame getLocalFrame(Entity entity) {
        Objects.requireNonNull(entity, "entity");
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
