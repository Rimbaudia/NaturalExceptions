package naturalexceptions.minecraft.gravity;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.EntityLivingBase;

import naturalexceptions.core.gravity.GravitySample;
import naturalexceptions.core.gravity.LocalBodyStance;
import naturalexceptions.core.gravity.LocalEntityFrame;
import naturalexceptions.core.gravity.Vector3d;

/**
 * Service providing the canonical smoothed rendered presentation frame for player rendering.
 *
 * <p>
 * Ensures physicalLocalFrame (authoritative for movement/contact/jump) and renderedLocalFrame
 * (smoothed for camera/model presentation) are cleanly separated, while maintaining determinism,
 * positive determinant (+1.0), and continuous heading transport.
 */
public final class NEPlayerPresentationService {

    private static final NEPlayerPresentationService INSTANCE = new NEPlayerPresentationService();

    private final Map<Long, Vector3d> smoothedUps = new ConcurrentHashMap<>();
    private final Map<Long, Vector3d> smoothedForwards = new ConcurrentHashMap<>();
    private double smoothingFactor = 0.25;

    private NEPlayerPresentationService() {}

    public static NEPlayerPresentationService instance() {
        return INSTANCE;
    }

    public synchronized void reset() {
        smoothedUps.clear();
        smoothedForwards.clear();
    }

    public synchronized double smoothingFactor() {
        return smoothingFactor;
    }

    public synchronized void setSmoothingFactor(double factor) {
        this.smoothingFactor = Math.max(0.01, Math.min(1.0, factor));
    }

    /**
     * Gets or updates the canonical rendered presentation frame for an entity.
     * Consumed by camera, entity 3D model, and viewpoint anchoring.
     */
    public NEPlayerPresentationFrame getPresentationFrame(EntityLivingBase entity, float partialTicks) {
        Objects.requireNonNull(entity, "entity");
        NEGravityService service = NEGravityService.global();
        LocalEntityFrame physFrame = service.getOrUpdateLocalFrame(entity);

        long key = entityKey(entity);
        Vector3d targetUp = physFrame.localUp();
        Vector3d targetFwd = physFrame.tangentForward();

        Vector3d currUp = smoothedUps.get(key);
        Vector3d currFwd = smoothedForwards.get(key);

        if (currUp == null || currFwd == null || !service.isNonstandardGravityActive()) {
            currUp = targetUp;
            currFwd = targetFwd;
        } else {
            // Geodesic rotation from currUp to targetUp
            double dot = currUp.dot(targetUp);
            if (dot > 0.999999) {
                currUp = targetUp;
                currFwd = targetFwd;
            } else {
                Vector3d rotAxis;
                double angle;
                if (dot < -0.999999) {
                    rotAxis = physFrame.tangentRight();
                    angle = Math.PI * smoothingFactor;
                } else {
                    rotAxis = currUp.cross(targetUp)
                        .normalize();
                    angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot))) * smoothingFactor;
                }
                currUp = LocalEntityFrame.rotateVector(currUp, rotAxis, angle)
                    .normalize();
                // Parallel transport currFwd along the same geodesic rotation
                currFwd = LocalEntityFrame.rotateVector(currFwd, rotAxis, angle);
                // Project onto new tangent plane
                currFwd = currFwd.subtract(currUp.multiply(currFwd.dot(currUp)));
                if (currFwd.lengthSquared() > 1e-4) {
                    currFwd = currFwd.normalize();
                } else {
                    currFwd = targetFwd;
                }
                // Also smoothly track targetFwd if yaw changed on the tangent plane
                Vector3d projectedTarget = targetFwd.subtract(currUp.multiply(targetFwd.dot(currUp)))
                    .normalize();
                double fwdDot = Math.max(-1.0, Math.min(1.0, currFwd.dot(projectedTarget)));
                if (fwdDot < 0.999999) {
                    Vector3d fwdAxis = currUp;
                    double fwdAngle = Math.acos(fwdDot);
                    Vector3d cross = currFwd.cross(projectedTarget);
                    if (cross.dot(currUp) < 0) {
                        fwdAngle = -fwdAngle;
                    }
                    currFwd = LocalEntityFrame.rotateVector(currFwd, fwdAxis, fwdAngle * 0.5)
                        .normalize();
                }
            }
        }

        smoothedUps.put(key, currUp);
        smoothedForwards.put(key, currFwd);

        // Interpolated entity position
        double interpX = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
        double interpY = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks;
        double interpZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;

        double hw = entity.width * 0.5;
        double hh = entity.height * 0.5;
        Vector3d aabbCenter = new Vector3d(interpX, interpY + hh, interpZ);
        GravitySample sample = service.sampleFor(entity);
        LocalBodyStance stance = LocalBodyStance.fromAabb(aabbCenter, currUp.negate(), hw, hh, entity.getEyeHeight());

        Vector3d eyePoint = stance.eyePoint();
        Vector3d supportPoint = stance.supportPoint();

        float mcPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        double mathPitch = -mcPitch;

        return new NEPlayerPresentationFrame(physFrame, currUp, currFwd, eyePoint, supportPoint, mathPitch);
    }

    private static long entityKey(EntityLivingBase entity) {
        long id = entity.getEntityId();
        if (entity.worldObj != null && entity.worldObj.isRemote) {
            return id | (1L << 32);
        }
        return id;
    }
}
