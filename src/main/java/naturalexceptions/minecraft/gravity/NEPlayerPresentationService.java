package naturalexceptions.minecraft.gravity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import naturalexceptions.core.gravity.LocalBodyStance;
import naturalexceptions.core.gravity.LocalEntityFrame;
import naturalexceptions.core.gravity.Vector3d;

/**
 * Service providing the canonical presentation frame for player rendering and camera positioning.
 *
 * <p>
 * Architectural invariants:
 * <ul>
 * <li><b>Camera orientation may be smoothed:</b> {@code renderedUp} and {@code renderedForward}
 * are geodesically interpolated across render frames to eliminate visual disorientation on ledge transitions.</li>
 * <li><b>Camera position may not interpolate through solid geometry:</b> {@code eyePoint} is anchored
 * strictly to the physically valid local body stance, derived from authoritative {@code physicalLocalUp}
 * and collision-checked against nearby block geometry.</li>
 * <li>Zero entity physics interference: adjustments apply solely to presentation viewpoints.</li>
 * </ul>
 */
public final class NEPlayerPresentationService {

    private static final int MAX_CACHED_ENTITIES = 512;
    private static final NEPlayerPresentationService INSTANCE = new NEPlayerPresentationService();

    private static final class CachedFrame {

        final int ticksExisted;
        final float partialTicks;
        final double posX;
        final double posY;
        final double posZ;
        final float yaw;
        final float pitch;
        final Vector3d targetUp;
        final boolean cameraEyeResolved;
        final NEPlayerPresentationFrame frame;

        CachedFrame(int ticksExisted, float partialTicks, double posX, double posY, double posZ, float yaw, float pitch,
            Vector3d targetUp, boolean cameraEyeResolved, NEPlayerPresentationFrame frame) {
            this.ticksExisted = ticksExisted;
            this.partialTicks = partialTicks;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.yaw = yaw;
            this.pitch = pitch;
            this.targetUp = targetUp;
            this.cameraEyeResolved = cameraEyeResolved;
            this.frame = frame;
        }

        boolean matches(EntityLivingBase entity, float pt, Vector3d up, boolean requireCameraEye) {
            return this.ticksExisted == entity.ticksExisted && Float.compare(this.partialTicks, pt) == 0
                && Double.compare(this.posX, entity.posX) == 0
                && Double.compare(this.posY, entity.posY) == 0
                && Double.compare(this.posZ, entity.posZ) == 0
                && Float.compare(this.yaw, entity.rotationYaw) == 0
                && Float.compare(this.pitch, entity.rotationPitch) == 0
                && this.targetUp.equals(up)
                && (!requireCameraEye || this.cameraEyeResolved);
        }
    }

    private final Map<Long, Vector3d> smoothedUps = new ConcurrentHashMap<>();
    private final Map<Long, Vector3d> smoothedForwards = new ConcurrentHashMap<>();
    private final Map<Long, Vector3d> prevSafeEyePoints = new ConcurrentHashMap<>();
    private final Map<Long, CachedFrame> cachedFrames = new ConcurrentHashMap<>();
    private double smoothingFactor = 0.25;

    private NEPlayerPresentationService() {}

    public static NEPlayerPresentationService instance() {
        return INSTANCE;
    }

    public synchronized void reset() {
        smoothedUps.clear();
        smoothedForwards.clear();
        prevSafeEyePoints.clear();
        cachedFrames.clear();
    }

    public boolean hasActiveSmoothing(EntityLivingBase entity) {
        if (entity == null || smoothedUps.isEmpty()) {
            return false;
        }
        Vector3d up = smoothedUps.get(entityKey(entity));
        return up != null && up.dot(Vector3d.UP) <= 0.999999;
    }

    public synchronized double smoothingFactor() {
        return smoothingFactor;
    }

    public synchronized void setSmoothingFactor(double factor) {
        this.smoothingFactor = Math.max(0.01, Math.min(1.0, factor));
    }

    /**
     * Gets or updates the canonical rendered presentation frame for an entity using the live World.
     */
    public NEPlayerPresentationFrame getPresentationFrame(EntityLivingBase entity, float partialTicks) {
        return getPresentationFrame(entity, partialTicks, true);
    }

    /**
     * Gets or updates the canonical rendered presentation frame for an entity, optionally resolving
     * camera eye collision against world geometry. Entity body and model passes should pass
     * {@code resolveCameraEye = false} to skip world raytraces and AABB collision queries.
     */
    public NEPlayerPresentationFrame getPresentationFrame(EntityLivingBase entity, float partialTicks,
        boolean resolveCameraEye) {
        if (!resolveCameraEye || entity == null || entity.worldObj == null) {
            return getPresentationFrame(entity, partialTicks, null, null);
        }
        return getPresentationFrame(entity, partialTicks, entity.worldObj::rayTraceBlocks, (e, bb) -> {
            @SuppressWarnings("unchecked")
            List<AxisAlignedBB> list = (List<AxisAlignedBB>) entity.worldObj.getCollidingBoundingBoxes(e, bb);
            return list != null ? list : Collections.emptyList();
        });
    }

    /**
     * Core presentation frame derivation, accepting raytrace and collision providers for deterministic unit testing.
     */
    public NEPlayerPresentationFrame getPresentationFrame(EntityLivingBase entity, float partialTicks,
        NECameraCollisionHelper.RayTraceProvider rayTracer, NECameraCollisionHelper.CollisionBoxProvider boxProvider) {
        Objects.requireNonNull(entity, "entity");
        NEGravityService service = NEGravityService.global();
        boolean nonstandardActive = service.isNonstandardGravityActive();
        boolean hasCollisionProviders = rayTracer != null || boxProvider != null;
        long key = entityKey(entity);

        LocalEntityFrame physFrame = service.getOrUpdateLocalFrame(entity);
        Vector3d targetUp = physFrame.localUp();
        Vector3d targetFwd = physFrame.tangentForward();

        CachedFrame cached = cachedFrames.get(key);
        if (cached != null && cached.matches(entity, partialTicks, targetUp, hasCollisionProviders)) {
            return cached.frame;
        }

        Vector3d currUp = smoothedUps.get(key);
        Vector3d currFwd = smoothedForwards.get(key);

        if (currUp == null || currFwd == null || !nonstandardActive) {
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

        if (nonstandardActive) {
            if (smoothedUps.size() >= MAX_CACHED_ENTITIES) {
                smoothedUps.clear();
                smoothedForwards.clear();
                prevSafeEyePoints.clear();
                cachedFrames.clear();
            }
            smoothedUps.put(key, currUp);
            smoothedForwards.put(key, currFwd);
        } else if (!smoothedUps.isEmpty()) {
            smoothedUps.remove(key);
            smoothedForwards.remove(key);
            prevSafeEyePoints.remove(key);
            cachedFrames.remove(key);
        }

        // 1. Interpolated entity coordinates
        double interpX = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
        double interpY = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks;
        double interpZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;

        double hw = entity.width * 0.5;
        double hh = entity.height * 0.5;

        // 2. Physical eye height in Minecraft 1.7.10:
        // In 1.7.10, EntityPlayer.yOffset is 1.62F (or 1.54F sneaking) and entity.posY is eye-level.
        // EntityPlayer.getEyeHeight() returns 0.12F (sneak offset), NOT 1.62F.
        double physicalEyeHeight;
        double yOffset;
        if (entity instanceof EntityPlayer || entity.yOffset > 0.0F) {
            yOffset = entity.yOffset > 0.0F ? (double) entity.yOffset : 1.62;
            physicalEyeHeight = yOffset;
        } else {
            float eh = entity.getEyeHeight();
            physicalEyeHeight = eh > 0.0F ? (double) eh : entity.height * 0.85;
            yOffset = 0.0;
        }

        // True physical AABB center: entity.posY in 1.7.10 is feet + yOffset
        double bottomY = interpY - yOffset;
        Vector3d physicalBodyCenter = new Vector3d(interpX, bottomY + hh, interpZ);

        // 3. PHYSICAL stance: authoritative physical local frame dictates eye position
        Vector3d physUp = physFrame.localUp();
        Vector3d physDown = physUp.negate();
        LocalBodyStance physicalStance = LocalBodyStance
            .fromAabb(physicalBodyCenter, physDown, hw, hh, physicalEyeHeight);

        Vector3d rawEyePoint = physicalStance.eyePoint();
        Vector3d supportPoint = physicalStance.supportPoint();

        // 4. Collision-aware first-person camera placement and depenetration
        Vector3d safeEyePoint;
        if (nonstandardActive && hasCollisionProviders) {
            Vector3d prevSafeEye = prevSafeEyePoints.get(key);
            safeEyePoint = NECameraCollisionHelper
                .resolveCameraPosition(rayTracer, boxProvider, entity, physicalBodyCenter, rawEyePoint, prevSafeEye);
            prevSafeEyePoints.put(key, safeEyePoint);
        } else {
            safeEyePoint = rawEyePoint;
        }

        float mcPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        double mathPitch = -mcPitch;

        NEPlayerPresentationFrame result = new NEPlayerPresentationFrame(
            physFrame,
            currUp,
            currFwd,
            safeEyePoint,
            supportPoint,
            mathPitch);
        if (nonstandardActive) {
            cachedFrames.put(
                key,
                new CachedFrame(
                    entity.ticksExisted,
                    partialTicks,
                    entity.posX,
                    entity.posY,
                    entity.posZ,
                    entity.rotationYaw,
                    entity.rotationPitch,
                    targetUp,
                    hasCollisionProviders,
                    result));
        }
        return result;
    }

    private static long entityKey(EntityLivingBase entity) {
        long id = entity.getEntityId();
        if (entity.worldObj != null && entity.worldObj.isRemote) {
            return id | (1L << 32);
        }
        return id;
    }
}
