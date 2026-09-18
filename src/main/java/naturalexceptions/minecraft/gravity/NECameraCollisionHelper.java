package naturalexceptions.minecraft.gravity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import naturalexceptions.core.gravity.Vector3d;

/**
 * Robust collision detection and depenetration helper for first-person camera positioning.
 *
 * <p>
 * Ensures the first-person camera origin never enters or passes through solid block geometry
 * during arbitrary-gravity transitions, stepping, or wall contact.
 *
 * <p>
 * Key safety invariants:
 * <ul>
 * <li>Camera position may not interpolate through solid geometry.</li>
 * <li>Adjusts presentation camera coordinates ONLY; NEVER modifies entity position, velocity, or physics.</li>
 * <li>Employs raytracing from known-safe physical body center to desired eye point.</li>
 * <li>Employs sweep verification from previous safe camera position to prevent corner tunneling.</li>
 * <li>Depenetrates from nearby collidable block AABBs to preserve near-plane frustum clearance.</li>
 * <li>Zero per-frame positional accumulation; fully deterministic and frame-rate independent.</li>
 * </ul>
 */
public final class NECameraCollisionHelper {

    /**
     * Safety radius around camera origin to prevent near-clipping plane (0.05) penetration.
     */
    public static final double CAMERA_SAFETY_RADIUS = 0.12;

    /**
     * Epsilon margin backed off from hit surfaces to keep camera strictly in open air.
     */
    public static final double SURFACE_BACKOFF_MARGIN = 0.08;

    @FunctionalInterface
    public interface RayTraceProvider {

        MovingObjectPosition rayTraceBlocks(Vec3 start, Vec3 end);
    }

    @FunctionalInterface
    public interface CollisionBoxProvider {

        List<AxisAlignedBB> getCollidingBoundingBoxes(EntityLivingBase entity, AxisAlignedBB aabb);
    }

    private NECameraCollisionHelper() {}

    /**
     * Resolves and depenetrates desired camera eye position against world collision geometry.
     */
    public static Vector3d resolveCameraPosition(World world, EntityLivingBase entity, Vector3d bodyCenter,
        Vector3d desiredEye, Vector3d prevSafeEye) {
        if (world == null || entity == null || desiredEye == null) {
            return desiredEye != null ? desiredEye : (bodyCenter != null ? bodyCenter : Vector3d.ZERO);
        }
        RayTraceProvider rayTracer = world::rayTraceBlocks;
        CollisionBoxProvider boxProvider = (e, bb) -> {
            @SuppressWarnings("unchecked")
            List<AxisAlignedBB> list = (List<AxisAlignedBB>) world.getCollidingBoundingBoxes(e, bb);
            return list != null ? list : Collections.emptyList();
        };
        return resolveCameraPosition(rayTracer, boxProvider, entity, bodyCenter, desiredEye, prevSafeEye);
    }

    /**
     * Core collision-aware camera resolution algorithm, decoupled from World for deterministic unit testing.
     */
    public static Vector3d resolveCameraPosition(RayTraceProvider rayTracer, CollisionBoxProvider boxProvider,
        EntityLivingBase entity, Vector3d bodyCenter, Vector3d desiredEye, Vector3d prevSafeEye) {
        Objects.requireNonNull(desiredEye, "desiredEye");
        if (bodyCenter == null) {
            return desiredEye;
        }

        Vector3d safeEye = desiredEye;

        // 1. Raytrace from physical body center (guaranteed outside solid blocks by entity physics) to desired eye
        if (rayTracer != null) {
            Vec3 start = Vec3.createVectorHelper(bodyCenter.x, bodyCenter.y, bodyCenter.z);
            Vec3 end = Vec3.createVectorHelper(safeEye.x, safeEye.y, safeEye.z);
            MovingObjectPosition hit = rayTracer.rayTraceBlocks(start, end);
            if (hit != null && hit.hitVec != null) {
                Vector3d rayDir = safeEye.subtract(bodyCenter);
                double totalDist = rayDir.length();
                if (totalDist > 1e-4) {
                    double hitDist = hit.hitVec.distanceTo(start);
                    double safeDist = Math.max(0.02, hitDist - SURFACE_BACKOFF_MARGIN);
                    safeEye = bodyCenter.add(rayDir.multiply(safeDist / totalDist));
                }
            }
        }

        // 2. Sweep verification from previous safe camera position to prevent tunneling across sharp voxel corners
        if (rayTracer != null && prevSafeEye != null) {
            double prevDistSq = prevSafeEye.distanceSquared(safeEye);
            if (prevDistSq > 1e-4 && prevDistSq < 16.0) {
                Vec3 prevVec = Vec3.createVectorHelper(prevSafeEye.x, prevSafeEye.y, prevSafeEye.z);
                Vec3 currVec = Vec3.createVectorHelper(safeEye.x, safeEye.y, safeEye.z);
                MovingObjectPosition sweepHit = rayTracer.rayTraceBlocks(prevVec, currVec);
                if (sweepHit != null && sweepHit.hitVec != null) {
                    double sweepDist = sweepHit.hitVec.distanceTo(prevVec);
                    double safeSweep = Math.max(0.02, sweepDist - SURFACE_BACKOFF_MARGIN);
                    Vector3d sweepDir = safeEye.subtract(prevSafeEye);
                    safeEye = prevSafeEye.add(sweepDir.multiply(safeSweep / Math.sqrt(prevDistSq)));
                }
            }
        }

        // 3. Depenetration from any nearby block collision AABBs within CAMERA_SAFETY_RADIUS
        if (boxProvider != null) {
            double r = CAMERA_SAFETY_RADIUS;
            AxisAlignedBB probe = AxisAlignedBB.getBoundingBox(
                safeEye.x - r,
                safeEye.y - r,
                safeEye.z - r,
                safeEye.x + r,
                safeEye.y + r,
                safeEye.z + r);
            List<AxisAlignedBB> collisions = boxProvider.getCollidingBoundingBoxes(entity, probe);
            if (collisions != null && !collisions.isEmpty()) {
                for (AxisAlignedBB box : collisions) {
                    if (box == null) continue;
                    safeEye = depenetrateFromBox(safeEye, box, bodyCenter, r);
                }
            }
        }

        return safeEye;
    }

    /**
     * Depenetrates a camera point from a single colliding AxisAlignedBB box.
     */
    public static Vector3d depenetrateFromBox(Vector3d point, AxisAlignedBB box, Vector3d bodyCenter, double radius) {
        // Check if point + radius intersects box
        boolean intersects = point.x > box.minX - radius && point.x < box.maxX + radius
            && point.y > box.minY - radius
            && point.y < box.maxY + radius
            && point.z > box.minZ - radius
            && point.z < box.maxZ + radius;

        if (!intersects) {
            return point;
        }

        // If point is strictly inside box, prioritize pushing along ray toward body center
        boolean strictlyInside = point.x >= box.minX && point.x <= box.maxX
            && point.y >= box.minY
            && point.y <= box.maxY
            && point.z >= box.minZ
            && point.z <= box.maxZ;

        if (strictlyInside && bodyCenter != null) {
            Vector3d toCenter = bodyCenter.subtract(point);
            if (toCenter.lengthSquared() > 1e-4) {
                Vector3d dir = toCenter.normalize();
                // Push outward along direction toward body center
                double pushDist = radius + 0.05;
                return point.add(dir.multiply(pushDist));
            }
        }

        // Otherwise push outward along the axis of minimum penetration
        double penMinX = (point.x + radius) - box.minX;
        double penMaxX = box.maxX - (point.x - radius);
        double penMinY = (point.y + radius) - box.minY;
        double penMaxY = box.maxY - (point.y - radius);
        double penMinZ = (point.z + radius) - box.minZ;
        double penMaxZ = box.maxZ - (point.z - radius);

        double minPen = penMinX;
        int axis = 0; // 0: -X, 1: +X, 2: -Y, 3: +Y, 4: -Z, 5: +Z

        if (penMaxX < minPen) {
            minPen = penMaxX;
            axis = 1;
        }
        if (penMinY < minPen) {
            minPen = penMinY;
            axis = 2;
        }
        if (penMaxY < minPen) {
            minPen = penMaxY;
            axis = 3;
        }
        if (penMinZ < minPen) {
            minPen = penMinZ;
            axis = 4;
        }
        if (penMaxZ < minPen) {
            minPen = penMaxZ;
            axis = 5;
        }

        double margin = 0.02;
        switch (axis) {
            case 0:
                return new Vector3d(box.minX - radius - margin, point.y, point.z);
            case 1:
                return new Vector3d(box.maxX + radius + margin, point.y, point.z);
            case 2:
                return new Vector3d(point.x, box.minY - radius - margin, point.z);
            case 3:
                return new Vector3d(point.x, box.maxY + radius + margin, point.z);
            case 4:
                return new Vector3d(point.x, point.y, box.minZ - radius - margin);
            case 5:
                return new Vector3d(point.x, point.y, box.maxZ + radius + margin);
            default:
                return point;
        }
    }

    /**
     * Invariant check: returns true if point is strictly inside any collidable block AABB.
     */
    public static boolean isPointInsideSolid(CollisionBoxProvider boxProvider, EntityLivingBase entity,
        Vector3d point) {
        if (boxProvider == null || point == null) {
            return false;
        }
        double eps = 1e-4;
        AxisAlignedBB probe = AxisAlignedBB
            .getBoundingBox(point.x - eps, point.y - eps, point.z - eps, point.x + eps, point.y + eps, point.z + eps);
        List<AxisAlignedBB> collisions = boxProvider.getCollidingBoundingBoxes(entity, probe);
        if (collisions == null || collisions.isEmpty()) {
            return false;
        }
        for (AxisAlignedBB box : collisions) {
            if (box == null) continue;
            if (point.x >= box.minX + eps && point.x <= box.maxX - eps
                && point.y >= box.minY + eps
                && point.y <= box.maxY - eps
                && point.z >= box.minZ + eps
                && point.z <= box.maxZ - eps) {
                return true;
            }
        }
        return false;
    }
}
