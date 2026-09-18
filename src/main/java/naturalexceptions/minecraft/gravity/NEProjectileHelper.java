package naturalexceptions.minecraft.gravity;

import java.util.Objects;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import naturalexceptions.core.gravity.GravitySample;
import naturalexceptions.core.gravity.Vector3d;

/**
 * Helper for launching projectiles (such as arrows) consistently under arbitrary local gravity.
 *
 * <p>
 * Invariants:
 * <ul>
 * <li>Bypass under standard downward gravity: vanilla projectile launch is 100% untouched.</li>
 * <li>Spawn point is anchored to the canonical presentation eye point, offset slightly along the 3D look vector.</li>
 * <li>Spawn point does not penetrate the support surface or adjacent solid geometry.</li>
 * <li>Initial velocity vector points directly along the canonical 3D look vector: normalized(v) . L == 1.0.</li>
 * <li>In-flight gravity acceleration is governed by {@link NEGravityService} with zero double-gravity.</li>
 * </ul>
 */
public final class NEProjectileHelper {

    public static final double ARROW_FORWARD_OFFSET = 0.20;

    private NEProjectileHelper() {}

    /**
     * Determines whether nonstandard projectile launch orientation should override vanilla spherical coords.
     */
    public static boolean shouldApplyCustomLaunch(EntityLivingBase shooter) {
        if (shooter == null) {
            return false;
        }
        NEGravityService service = NEGravityService.global();
        if (!service.isNonstandardGravityActive()) {
            return false;
        }
        GravitySample sample = service.sampleFor(shooter);
        return Vector3d.UP.dot(sample.localUp()) <= 0.99999;
    }

    /**
     * Computes the canonical 3D look vector for an entity under local gravity.
     */
    public static Vector3d computeLookVector(EntityLivingBase shooter) {
        Objects.requireNonNull(shooter, "shooter");
        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(shooter, 1.0F);
        return frame.computeLookVector();
    }

    /**
     * Computes the safe physical eye position for an entity under local gravity.
     */
    public static Vector3d computeEyePosition(EntityLivingBase shooter) {
        Objects.requireNonNull(shooter, "shooter");
        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(shooter, 1.0F);
        return frame.eyePoint();
    }

    /**
     * Computes the safe spawn position offset along the look vector, raytracing against obstacles if available.
     */
    public static Vector3d computeLaunchPosition(EntityLivingBase shooter, Vector3d lookVector) {
        Objects.requireNonNull(shooter, "shooter");
        Objects.requireNonNull(lookVector, "lookVector");

        Vector3d eye = computeEyePosition(shooter);
        Vector3d targetSpawn = eye.add(lookVector.multiply(ARROW_FORWARD_OFFSET));

        if (shooter.worldObj != null) {
            Vec3 start = Vec3.createVectorHelper(eye.x, eye.y, eye.z);
            Vec3 end = Vec3.createVectorHelper(targetSpawn.x, targetSpawn.y, targetSpawn.z);
            MovingObjectPosition mop = shooter.worldObj.rayTraceBlocks(start, end);
            if (mop != null) {
                // Obstacle directly in front of eyes: fallback to safe eye point
                return eye;
            }
        }
        return targetSpawn;
    }

    /**
     * Aligns an EntityArrow's initial position and velocity with the shooter's canonical local look frame.
     */
    public static void alignArrowLaunch(EntityArrow arrow, EntityLivingBase shooter, float speedMultiplier) {
        if (arrow == null || shooter == null) {
            return;
        }
        if (!shouldApplyCustomLaunch(shooter)) {
            return;
        }

        Vector3d look = computeLookVector(shooter);
        Vector3d spawnPos = computeLaunchPosition(shooter, look);

        // 1. Position arrow safely at computed launch point
        arrow.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
        arrow.prevPosX = spawnPos.x;
        arrow.prevPosY = spawnPos.y;
        arrow.prevPosZ = spawnPos.z;
        arrow.lastTickPosX = spawnPos.x;
        arrow.lastTickPosY = spawnPos.y;
        arrow.lastTickPosZ = spawnPos.z;

        // 2. Compute speed magnitude
        double speed = (double) speedMultiplier;
        if (speed <= 1e-4) {
            speed = Math
                .sqrt(arrow.motionX * arrow.motionX + arrow.motionY * arrow.motionY + arrow.motionZ * arrow.motionZ);
            if (speed <= 1e-4) {
                speed = 1.5;
            }
        }

        // 3. Set velocity directly along canonical look vector
        arrow.motionX = look.x * speed;
        arrow.motionY = look.y * speed;
        arrow.motionZ = look.z * speed;

        // 4. Update arrow heading rotation angles for coherent rendering and hit detection
        float horizontalSpeed = MathHelper.sqrt_double(arrow.motionX * arrow.motionX + arrow.motionZ * arrow.motionZ);
        arrow.prevRotationYaw = arrow.rotationYaw = (float) (Math.atan2(arrow.motionX, arrow.motionZ) * 180.0
            / Math.PI);
        arrow.prevRotationPitch = arrow.rotationPitch = (float) (Math.atan2(arrow.motionY, (double) horizontalSpeed)
            * 180.0
            / Math.PI);
    }
}
