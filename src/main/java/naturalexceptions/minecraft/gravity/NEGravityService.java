package naturalexceptions.minecraft.gravity;

import java.util.Objects;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import naturalexceptions.core.gravity.GravityField;
import naturalexceptions.core.gravity.GravitySample;
import naturalexceptions.core.gravity.UniformGravityField;
import naturalexceptions.core.gravity.Vector3d;

/**
 * Service managing the active gravitational field in Minecraft.
 *
 * <p>
 * Decouples gravity evaluation from hardcoded Y-axis physics. Computes the necessary
 * velocity delta required to counteract vanilla's hardcoded (0, -0.08, 0) downward
 * acceleration and apply arbitrary 3D local gravity vectors.
 */
public final class NEGravityService {

    public static final double DEFAULT_VANILLA_GRAVITY = 0.08;
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
     * Applies gravitational acceleration delta to the given living entity.
     * Computes the difference between the local gravity field sample and vanilla's
     * hardcoded downward acceleration (0, -vanillaDownwardAcceleration, 0).
     */
    public void applyGravity(EntityLivingBase entity) {
        if (!enabled || entity == null) {
            return;
        }

        GravitySample sample = sampleFor(entity);
        Vector3d localAcc = sample.acceleration();

        // Vanilla hardcodes (0, -vanillaDownwardAcceleration, 0)
        // Delta needed: localAcc - (0, -vanillaDownwardAcceleration, 0)
        double deltaX = localAcc.x;
        double deltaY = localAcc.y + vanillaDownwardAcceleration;
        double deltaZ = localAcc.z;

        // Only modify motion if there is an active divergence from standard vanilla gravity
        if (Math.abs(deltaX) > 1e-6 || Math.abs(deltaY) > 1e-6 || Math.abs(deltaZ) > 1e-6) {
            entity.motionX += deltaX;
            entity.motionY += deltaY;
            entity.motionZ += deltaZ;
        }
    }
}
