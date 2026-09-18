package naturalexceptions.minecraft.gravity;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;

import naturalexceptions.core.gravity.GravitySample;
import naturalexceptions.core.gravity.LocalBodyStance;
import naturalexceptions.core.gravity.LocalEntityFrame;
import naturalexceptions.core.gravity.Vector3d;
import naturalexceptions.minecraft.mixin.EntityLivingBaseAccessor;

/**
 * Handles arbitrary-orientation player movement, grounding, jumping, stepping,
 * and contact stabilization under Natural Exceptions local gravity.
 */
public final class NEPlayerMovementHandler {

    public static final double PROBE_DISTANCE = 0.10;
    public static final double STEP_HEIGHT = 0.6;
    public static final double JUMP_IMPULSE = 0.42;
    public static final double ADHESION_ACCELERATION = 0.0;
    public static final double GROUND_FRICTION = 0.546; // standard 0.6 slipperiness * 0.91

    @FunctionalInterface
    public interface CollisionProvider {

        List<AxisAlignedBB> getCollidingBoundingBoxes(EntityLivingBase entity, AxisAlignedBB aabb);
    }

    private NEPlayerMovementHandler() {}

    /**
     * Applies a single gravity-relative jump impulse along localUp.
     * Sets jump cooldown ticks and airborne state to guarantee exactly one impulse per jump.
     */
    public static void applyJump(EntityLivingBase entity) {
        if (entity == null) {
            return;
        }

        NEGravityService service = NEGravityService.global();
        if (!service.isNonstandardGravityActive()) {
            return;
        }

        GravitySample sample = service.sampleFor(entity);
        Vector3d localUp = sample.localUp();

        Vector3d jumpVec = localUp.multiply(JUMP_IMPULSE);
        entity.motionX += jumpVec.x;
        entity.motionY += jumpVec.y;
        entity.motionZ += jumpVec.z;

        boolean isSprinting = false;
        try {
            isSprinting = entity.isSprinting();
        } catch (Throwable ignored) {
            // Uninitialized test entity or mock
        }

        if (isSprinting) {
            LocalEntityFrame frame = service.getOrUpdateLocalFrame(entity);
            Vector3d sprintBoost = frame.tangentForward()
                .multiply(0.2);
            entity.motionX += sprintBoost.x;
            entity.motionY += sprintBoost.y;
            entity.motionZ += sprintBoost.z;
        }

        if (entity instanceof EntityLivingBaseAccessor) {
            ((EntityLivingBaseAccessor) entity).setJumpTicks(10);
        }

        entity.isAirBorne = true;
        entity.onGround = false;
        try {
            net.minecraftforge.common.ForgeHooks.onLivingJump(entity);
        } catch (Throwable ignored) {
            // Forge event bus is not initialized during unit tests
        }
    }

    public static void handleMovement(EntityLivingBase entity, float strafe, float forward) {
        handleMovement(entity, strafe, forward, defaultCollisionProvider(entity));
    }

    public static void handleMovement(EntityLivingBase entity, float strafe, float forward,
        CollisionProvider collisionProvider) {
        if (entity == null) {
            return;
        }

        NEGravityService service = NEGravityService.global();
        if (!service.isNonstandardGravityActive()) {
            return;
        }

        GravitySample sample = service.sampleFor(entity);
        Vector3d localDown = sample.localDown();
        Vector3d localUp = sample.localUp();

        // 1. Evaluate gravity-relative grounding
        boolean isGrounded = isGrounded(entity, localDown, PROBE_DISTANCE, collisionProvider);

        // 2. Obtain canonical persistent parallel-transported movement frame
        LocalEntityFrame frame = service.getOrUpdateLocalFrame(entity);

        // 3. Handle jump if requested
        boolean isJumping = false;
        int jumpTicks = 0;
        EntityLivingBaseAccessor accessor = null;
        if (entity instanceof EntityLivingBaseAccessor) {
            accessor = (EntityLivingBaseAccessor) entity;
            isJumping = accessor.isJumping();
            jumpTicks = accessor.getJumpTicks();
        }
        if (isJumping && isGrounded && jumpTicks == 0) {
            applyJump(entity);
            isGrounded = false;
        }

        // 4. Calculate movement acceleration from WASD inputs mapped onto local tangent plane
        float speed = isGrounded ? entity.getAIMoveSpeed() : entity.jumpMovementFactor;
        Vector3d inputDir = frame.mapInput(forward, strafe);
        if (inputDir.lengthSquared() > 1e-4) {
            Vector3d moveAccel = inputDir.normalize()
                .multiply(speed);
            entity.motionX += moveAccel.x;
            entity.motionY += moveAccel.y;
            entity.motionZ += moveAccel.z;
        }

        // 5. Apply physical gravity acceleration always
        Vector3d gravity = sample.acceleration();
        entity.motionX += gravity.x;
        entity.motionY += gravity.y;
        entity.motionZ += gravity.z;

        // 6. Gravity-relative step handling before movement
        double plannedX = entity.motionX;
        double plannedY = entity.motionY;
        double plannedZ = entity.motionZ;

        boolean stepped = false;
        if (isGrounded && inputDir.lengthSquared() > 1e-4) {
            stepped = attemptLocalStep(entity, plannedX, plannedY, plannedZ, localUp, STEP_HEIGHT, collisionProvider);
        }

        // 7. Perform movement if not already positioned by successful discrete step
        if (!stepped) {
            entity.moveEntity(entity.motionX, entity.motionY, entity.motionZ);
        }

        // 8. Post-move grounding re-evaluation and surface contact resolution
        Vector3d postVel = new Vector3d(entity.motionX, entity.motionY, entity.motionZ);
        double normalVel = postVel.dot(localDown);
        if (normalVel >= -1e-4) {
            isGrounded = isGrounded(entity, localDown, PROBE_DISTANCE, collisionProvider);
        } else {
            isGrounded = false;
        }

        if (isGrounded) {
            entity.onGround = true;
            entity.isAirBorne = false;
            entity.fallDistance = 0.0F;

            // Cancel motion into the surface to provide normal support
            if (normalVel > 0.0) {
                Vector3d cancel = localDown.multiply(normalVel);
                entity.motionX -= cancel.x;
                entity.motionY -= cancel.y;
                entity.motionZ -= cancel.z;
            }

            // Apply ground friction strictly to tangential velocity
            Vector3d currentVel = new Vector3d(entity.motionX, entity.motionY, entity.motionZ);
            double currentNormal = currentVel.dot(localDown);
            Vector3d normalComponent = localDown.multiply(currentNormal);
            Vector3d tangentComponent = currentVel.subtract(normalComponent);
            Vector3d newVel = normalComponent.add(tangentComponent.multiply(GROUND_FRICTION));

            entity.motionX = newVel.x;
            entity.motionY = newVel.y;
            entity.motionZ = newVel.z;
        } else {
            entity.onGround = false;
            // Airborne drag
            entity.motionX *= 0.91;
            entity.motionY *= 0.91;
            entity.motionZ *= 0.91;
        }

        // 9. Update locomotion animation state using gravity-relative tangential displacement
        updateLocomotionAnimation(entity, localUp, isGrounded);

        if (service.isDebugLogging()) {
            System.out.printf(
                "[NE-GRAV-DEBUG] entity=%s pos=(%.2f, %.2f, %.2f) vel=(%.4f, %.4f, %.4f) g=(%.3f, %.3f, %.3f) down=%s up=%s grounded=%b onGround=%b jump=%b step=%b%n",
                entity.getCommandSenderName(),
                entity.posX,
                entity.posY,
                entity.posZ,
                entity.motionX,
                entity.motionY,
                entity.motionZ,
                sample.acceleration().x,
                sample.acceleration().y,
                sample.acceleration().z,
                localDown,
                localUp,
                isGrounded,
                entity.onGround,
                isJumping,
                stepped);
        }
    }

    public static boolean isGrounded(EntityLivingBase entity, Vector3d localDown, double probeDistance,
        CollisionProvider collisionProvider) {
        if (entity == null || entity.boundingBox == null || collisionProvider == null) {
            return false;
        }
        AxisAlignedBB bb = entity.boundingBox;
        double cx = (bb.minX + bb.maxX) * 0.5;
        double cy = (bb.minY + bb.maxY) * 0.5;
        double cz = (bb.minZ + bb.maxZ) * 0.5;
        double hw = (bb.maxX - bb.minX) * 0.5;
        double hh = (bb.maxY - bb.minY) * 0.5;

        LocalBodyStance stance = new LocalBodyStance(new Vector3d(cx, cy, cz), localDown, hw, hh);
        double[] b = stance.feetProbeBounds(probeDistance, 0.1);
        AxisAlignedBB probe = AxisAlignedBB.getBoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
        List<?> collisions = collisionProvider.getCollidingBoundingBoxes(entity, probe);
        return collisions != null && !collisions.isEmpty();
    }

    public static boolean attemptLocalStep(EntityLivingBase entity, double mx, double my, double mz, Vector3d localUp,
        double stepHeight, CollisionProvider collisionProvider) {
        if (entity == null || entity.boundingBox == null || collisionProvider == null) {
            return false;
        }

        Vector3d vel = new Vector3d(mx, my, mz);
        // Project onto tangent plane orthogonal to localUp
        Vector3d normalVel = localUp.multiply(vel.dot(localUp));
        Vector3d tangentVel = vel.subtract(normalVel);
        if (tangentVel.lengthSquared() < 1e-6) {
            return false;
        }

        // 1. Test if direct planned movement along tangent plane collides
        AxisAlignedBB directMove = entity.boundingBox.getOffsetBoundingBox(tangentVel.x, tangentVel.y, tangentVel.z);
        List<?> directCollisions = collisionProvider.getCollidingBoundingBoxes(entity, directMove);
        if (directCollisions == null || directCollisions.isEmpty()) {
            return false; // Path is clear, no step needed
        }

        // 2. Test elevating along localUp
        AxisAlignedBB elevated = entity.boundingBox
            .getOffsetBoundingBox(localUp.x * stepHeight, localUp.y * stepHeight, localUp.z * stepHeight);
        List<?> elevCollisions = collisionProvider.getCollidingBoundingBoxes(entity, elevated);
        if (elevCollisions != null && !elevCollisions.isEmpty()) {
            return false; // Cannot step up, obstacle overhead
        }

        // 3. Test moving forward while elevated
        AxisAlignedBB elevatedMove = elevated.getOffsetBoundingBox(tangentVel.x, tangentVel.y, tangentVel.z);
        List<?> moveCollisions = collisionProvider.getCollidingBoundingBoxes(entity, elevatedMove);
        if (moveCollisions != null && !moveCollisions.isEmpty()) {
            return false; // Obstacle too tall
        }

        // 4. Step back DOWN along localDown until feet contact the step surface
        Vector3d localDown = localUp.negate();
        double low = 0.0;
        double high = stepHeight;
        for (int i = 0; i < 8; i++) {
            double mid = (low + high) * 0.5;
            AxisAlignedBB testBox = elevatedMove
                .getOffsetBoundingBox(localDown.x * mid, localDown.y * mid, localDown.z * mid);
            List<?> testCollisions = collisionProvider.getCollidingBoundingBoxes(entity, testBox);
            if (testCollisions == null || testCollisions.isEmpty()) {
                low = mid; // Still clear, can step further down toward surface
            } else {
                high = mid; // Hit obstacle surface, back off
            }
        }
        AxisAlignedBB landed = elevatedMove
            .getOffsetBoundingBox(localDown.x * low, localDown.y * low, localDown.z * low);

        // 5. Apply stepped position to bounding box and coordinates
        double origMinX = entity.boundingBox.minX;
        double origMinY = entity.boundingBox.minY;
        double origMinZ = entity.boundingBox.minZ;

        entity.boundingBox.setBB(landed);
        entity.posX += landed.minX - origMinX;
        entity.posY += landed.minY - origMinY;
        entity.posZ += landed.minZ - origMinZ;

        // Zero out velocity into the step obstacle; no upward kinetic energy injected
        entity.motionX = 0.0;
        entity.motionY = 0.0;
        entity.motionZ = 0.0;
        return true;
    }

    /**
     * Updates walking animation, limb swing, and distance walked using gravity-relative
     * tangential displacement along the local support surface.
     */
    public static void updateLocomotionAnimation(EntityLivingBase entity, Vector3d localUp, boolean isGrounded) {
        if (entity == null || localUp == null) {
            return;
        }

        Vector3d deltaPos = new Vector3d(
            entity.posX - entity.prevPosX,
            entity.posY - entity.prevPosY,
            entity.posZ - entity.prevPosZ);

        double normalDisp = deltaPos.dot(localUp);
        Vector3d tangentDelta = deltaPos.subtract(localUp.multiply(normalDisp));
        double tangentDistance = tangentDelta.length();

        entity.prevLimbSwingAmount = entity.limbSwingAmount;
        float f6 = (float) (tangentDistance * 4.0);
        if (f6 > 1.0F) {
            f6 = 1.0F;
        }

        entity.limbSwingAmount += (f6 - entity.limbSwingAmount) * 0.4F;
        entity.limbSwing += entity.limbSwingAmount;

        if (isGrounded) {
            entity.distanceWalkedModified += (float) (tangentDistance * 0.6);
        }
    }

    private static CollisionProvider defaultCollisionProvider(EntityLivingBase entity) {
        return (e, bb) -> {
            if (e != null && e.worldObj != null) {
                @SuppressWarnings("unchecked")
                List<AxisAlignedBB> list = (List<AxisAlignedBB>) e.worldObj.getCollidingBoundingBoxes(e, bb);
                return list != null ? list : Collections.emptyList();
            }
            return Collections.emptyList();
        };
    }
}
