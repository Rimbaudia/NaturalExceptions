package naturalexceptions.minecraft.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import naturalexceptions.core.gravity.GravityCameraFrame;
import naturalexceptions.core.gravity.GravitySample;
import naturalexceptions.core.gravity.LocalEntityFrame;
import naturalexceptions.core.gravity.RadialGravitySource;
import naturalexceptions.core.gravity.UniformGravityField;
import naturalexceptions.core.gravity.Vector3d;
import naturalexceptions.minecraft.mixin.EntityLivingBaseAccessor;

final class RuntimeGravityLocomotionTest {

    private static final double EPSILON = 1e-6;

    private static class LocomotionTestEntity extends EntityLivingBase implements EntityLivingBaseAccessor {

        private boolean jumping;
        private NEPlayerMovementHandler.CollisionProvider collisionProvider;

        public LocomotionTestEntity() {
            super(null);
            this.boundingBox.setBounds(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3);
            this.posX = 0.0;
            this.posY = 0.0;
            this.posZ = 0.0;
        }

        public void setCollisionProvider(NEPlayerMovementHandler.CollisionProvider collisionProvider) {
            this.collisionProvider = collisionProvider;
        }

        public void setPosition(double x, double y, double z) {
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            this.boundingBox.setBounds(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
        }

        public void setJumping(boolean jumping) {
            this.jumping = jumping;
        }

        @Override
        public boolean isJumping() {
            return this.jumping;
        }

        @Override
        public float getAIMoveSpeed() {
            return 0.1F;
        }

        @Override
        public void moveEntity(double x, double y, double z) {
            if (collisionProvider != null) {
                double origX = x;
                double origY = y;
                double origZ = z;
                AxisAlignedBB expanded = this.boundingBox.addCoord(x, y, z);
                @SuppressWarnings("unchecked")
                List<AxisAlignedBB> boxes = collisionProvider.getCollidingBoundingBoxes(this, expanded);
                if (boxes != null) {
                    for (AxisAlignedBB box : boxes) {
                        y = box.calculateYOffset(this.boundingBox, y);
                    }
                    this.boundingBox.offset(0.0D, y, 0.0D);
                    for (AxisAlignedBB box : boxes) {
                        x = box.calculateXOffset(this.boundingBox, x);
                    }
                    this.boundingBox.offset(x, 0.0D, 0.0D);
                    for (AxisAlignedBB box : boxes) {
                        z = box.calculateZOffset(this.boundingBox, z);
                    }
                    this.boundingBox.offset(0.0D, 0.0D, z);
                    this.posX = (this.boundingBox.minX + this.boundingBox.maxX) / 2.0;
                    this.posY = this.boundingBox.minY;
                    this.posZ = (this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0;

                    if (origX != x) this.motionX = 0.0;
                    if (origY != y) this.motionY = 0.0;
                    if (origZ != z) this.motionZ = 0.0;
                    return;
                }
            }
            this.posX += x;
            this.posY += y;
            this.posZ += z;
            this.boundingBox.offset(x, y, z);
        }

        @Override
        public ItemStack getHeldItem() {
            return null;
        }

        @Override
        public ItemStack getEquipmentInSlot(int slot) {
            return null;
        }

        @Override
        public void setCurrentItemOrArmor(int slot, ItemStack stack) {}

        @Override
        public ItemStack[] getLastActiveItems() {
            return new ItemStack[0];
        }
    }

    private NEGravityService service;

    @BeforeEach
    void setUp() {
        service = NEGravityService.global();
        service.resetToDefault();
    }

    @AfterEach
    void tearDown() {
        service.resetToDefault();
    }

    @Test
    void sideWallSupportCountsAsGravityRelativeGround() {
        // Sideways gravity along +X. Local down is +X.
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));

        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0); // bounding box maxX is 10.0

        // Wall from X=10.0 to X=11.0
        AxisAlignedBB wallBox = AxisAlignedBB.getBoundingBox(10.0, 60.0, -10.0, 11.0, 70.0, 10.0);
        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            if (bb.intersectsWith(wallBox)) {
                return Collections.singletonList(wallBox);
            }
            return Collections.emptyList();
        };

        Vector3d localDown = new Vector3d(1, 0, 0);
        boolean grounded = NEPlayerMovementHandler.isGrounded(entity, localDown, 0.15, provider);
        assertTrue(grounded, "Side wall must be recognized as valid ground under sideways gravity");
    }

    @Test
    void playerCanMoveTangentiallyWhileSupportedByWorldSpaceWall() {
        // Concrete regression test for observed side-wall movement failure.
        // Under sideways +X gravity, a wall at X=10 must support the player,
        // stop penetration into the wall, and allow tangential locomotion in Y/Z.
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));

        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);
        entity.rotationYaw = 0.0F; // Heading along tangent plane
        entity.motionX = 0.0;
        entity.motionY = 0.0;
        entity.motionZ = 0.0;

        AxisAlignedBB wallBox = AxisAlignedBB.getBoundingBox(10.0, 60.0, -10.0, 11.0, 70.0, 10.0);
        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            if (bb.intersectsWith(wallBox)) {
                return Collections.singletonList(wallBox);
            }
            return Collections.emptyList();
        };
        entity.setCollisionProvider(provider);

        // Press 'W' forward (forward = 1.0F, strafe = 0.0F)
        NEPlayerMovementHandler.handleMovement(entity, 0.0F, 1.0F, provider);

        // 1. Player is recognized as grounded on the wall
        assertTrue(entity.onGround, "Player must be onGround on the wall");
        assertEquals(0.0F, entity.fallDistance, EPSILON, "Fall distance must be zeroed on wall");

        // 2. Tangent motion is non-zero (player walks along the wall)
        Vector3d tangentMotion = new Vector3d(0.0, entity.motionY, entity.motionZ);
        assertTrue(tangentMotion.length() > 0.01, "Tangent motion must be non-zero when walking on wall");

        // 3. Normal motion into wall is cancelled (no penetration drift)
        assertTrue(entity.motionX <= 0.01, "Motion into wall must be cancelled");
    }

    @Test
    void ceilingSupportCountsAsGravityRelativeGround() {
        // Inverted gravity (+Y). Local down is +Y, local up is -Y.
        service.setActiveField(UniformGravityField.of(0.0, 0.08, 0.0));

        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(0.0, 98.2, 0.0); // bounding box maxY is 100.0

        // Ceiling at Y=100.0 to 101.0
        AxisAlignedBB ceilingBox = AxisAlignedBB.getBoundingBox(-10.0, 100.0, -10.0, 10.0, 101.0, 10.0);
        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            if (bb.intersectsWith(ceilingBox)) {
                return Collections.singletonList(ceilingBox);
            }
            return Collections.emptyList();
        };
        entity.setCollisionProvider(provider);

        Vector3d localDown = new Vector3d(0, 1, 0);
        boolean grounded = NEPlayerMovementHandler.isGrounded(entity, localDown, 0.15, provider);
        assertTrue(grounded, "Ceiling must be recognized as valid ground under inverted gravity");

        // Test walking on ceiling
        NEPlayerMovementHandler.handleMovement(entity, 0.0F, 1.0F, provider);
        assertTrue(entity.onGround, "Player must remain grounded on ceiling");
        assertTrue(
            Math.abs(entity.motionZ) > 0.01 || Math.abs(entity.motionX) > 0.01,
            "Tangent motion on ceiling must be non-zero");
    }

    @Test
    void localJumpImpulseUsesLocalUp() {
        LocomotionTestEntity entity = new LocomotionTestEntity();

        // 1. Floor (-Y gravity, local up is +Y)
        service.setActiveField(UniformGravityField.of(0.0, -0.08, 0.0));
        entity.setPosition(0.0, 64.0, 0.0);
        entity.setJumping(true);
        AxisAlignedBB floorBox = AxisAlignedBB.getBoundingBox(-10, 63, -10, 10, 64, 10);
        NEPlayerMovementHandler.CollisionProvider floorProvider = (e, bb) -> {
            if (bb.intersectsWith(floorBox)) {
                return Collections.singletonList(floorBox);
            }
            return Collections.emptyList();
        };
        entity.setCollisionProvider(floorProvider);
        NEPlayerMovementHandler.handleMovement(entity, 0.0F, 0.0F, floorProvider);
        assertTrue(entity.motionY > 0.3, "Floor jump must launch along +Y");

        // 2. Wall (+X gravity, local down is +X, local up is -X)
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        entity.setPosition(9.7, 64.0, 0.0);
        entity.motionX = 0;
        entity.motionY = 0;
        entity.motionZ = 0;
        entity.setJumping(true);
        AxisAlignedBB wallBox = AxisAlignedBB.getBoundingBox(10.0, 60.0, -10.0, 11.0, 70.0, 10.0);
        NEPlayerMovementHandler.CollisionProvider wallProvider = (e, bb) -> {
            if (bb.intersectsWith(wallBox)) {
                return Collections.singletonList(wallBox);
            }
            return Collections.emptyList();
        };
        entity.setCollisionProvider(wallProvider);
        NEPlayerMovementHandler.handleMovement(entity, 0.0F, 0.0F, wallProvider);
        assertTrue(entity.motionX < -0.3, "Wall jump must launch away from wall along -X");

        // 3. Ceiling (+Y gravity, local down is +Y, local up is -Y)
        service.setActiveField(UniformGravityField.of(0.0, 0.08, 0.0));
        entity.setPosition(0.0, 98.2, 0.0);
        entity.motionX = 0;
        entity.motionY = 0;
        entity.motionZ = 0;
        entity.setJumping(true);
        AxisAlignedBB ceilingBox = AxisAlignedBB.getBoundingBox(-10.0, 100.0, -10.0, 10.0, 101.0, 10.0);
        NEPlayerMovementHandler.CollisionProvider ceilingProvider = (e, bb) -> {
            if (bb.intersectsWith(ceilingBox)) {
                return Collections.singletonList(ceilingBox);
            }
            return Collections.emptyList();
        };
        entity.setCollisionProvider(ceilingProvider);
        NEPlayerMovementHandler.handleMovement(entity, 0.0F, 0.0F, ceilingProvider);
        assertTrue(entity.motionY < -0.3, "Ceiling jump must launch away from ceiling along -Y");
    }

    @Test
    void gravityRelativeSteppingHandlesRepresentativeVoxelObstacle() {
        // Wall walking (+X gravity, walking along +Z on the wall)
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));

        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);
        Vector3d localUp = new Vector3d(-1, 0, 0);

        // 0.5-block voxel step obstacle in front from Z=0.4 to 1.4, sticking out 0.5 from wall (X=9.5 to 10.0)
        AxisAlignedBB stepObstacle = AxisAlignedBB.getBoundingBox(9.5, 60.0, 0.4, 10.0, 70.0, 1.4);
        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            if (bb.intersectsWith(stepObstacle)) {
                return Collections.singletonList(stepObstacle);
            }
            return Collections.emptyList();
        };

        // Moving forward directly (mz = 0.3) collides with obstacle
        boolean stepped = NEPlayerMovementHandler.attemptLocalStep(entity, 0.0, 0.0, 0.3, localUp, 0.6, provider);
        assertTrue(stepped, "Local stepping must successfully step over the voxel obstacle along localUp");
        assertTrue(entity.posX < 9.7, "Entity position must elevate along localUp (-X)");
    }

    @Test
    void contactStabilizationDoesNotInjectUnboundedVelocity() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));

        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);

        AxisAlignedBB wallBox = AxisAlignedBB.getBoundingBox(10.0, 60.0, -10.0, 11.0, 70.0, 10.0);
        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            if (bb.intersectsWith(wallBox)) {
                return Collections.singletonList(wallBox);
            }
            return Collections.emptyList();
        };
        entity.setCollisionProvider(provider);

        // Simulate 50 ticks of contact stabilization
        for (int tick = 0; tick < 50; tick++) {
            NEPlayerMovementHandler.handleMovement(entity, 0.0F, 0.0F, provider);
            double speed = Math.sqrt(
                entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ);
            assertTrue(
                speed < 0.2,
                "Velocity must remain bounded during resting contact (tick " + tick + "): " + speed);
            assertTrue(Math.abs(entity.posX - 9.7) < 0.3, "Entity must remain attached near wall surface");
        }
    }

    @Test
    void standardGravityPreservesVanillaMovementPath() {
        // When standard Minecraft gravity is active, handleMovement must return immediately
        service.resetToDefault();
        assertFalse(service.isNonstandardGravityActive());

        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.motionX = 0.05;
        entity.motionY = -0.08;
        entity.motionZ = 0.12;

        NEPlayerMovementHandler.handleMovement(entity, 0.5F, 1.0F);

        // Motion remains untouched by NEPlayerMovementHandler
        assertEquals(0.05, entity.motionX, EPSILON);
        assertEquals(-0.08, entity.motionY, EPSILON);
        assertEquals(0.12, entity.motionZ, EPSILON);
    }

    @Test
    void cameraTargetOrientationIsContinuousAcrossTopSideUnderside() {
        GravityCameraFrame camera = new GravityCameraFrame(Vector3d.UP, 0.2);

        // Top (+Y)
        camera.update(new Vector3d(0, 1, 0));
        assertEquals(Vector3d.UP, camera.currentUp());

        // Side wall (+X)
        camera.update(new Vector3d(1, 0, 0));
        assertTrue(camera.currentUp().x > 0.0, "Camera up must rotate smoothly toward +X");
        assertTrue(camera.currentUp().y > 0.0, "Camera up must retain partial +Y during interpolation");

        // Underside (-Y)
        camera.update(new Vector3d(0, -1, 0));
        assertFalse(Double.isNaN(camera.currentUp().x));
        assertFalse(Double.isNaN(camera.currentUp().y));
        assertEquals(
            1.0,
            camera.currentUp()
                .length(),
            EPSILON);
    }

    @Test
    void noGimbalLockDiscontinuityAtApproximatePitchExtremes() {
        GravityCameraFrame camera = new GravityCameraFrame(new Vector3d(1, 0, 0), 1.0); // standing on wall

        // Test look directions at pitch extremes
        double[] testPitches = { -90.0, -89.9, -45.0, 0.0, 45.0, 89.9, 90.0 };
        for (double pitch : testPitches) {
            Vector3d look = camera.computeLookDirection(0.0, pitch);
            assertFalse(Double.isNaN(look.x), "Look X must not be NaN at pitch " + pitch);
            assertFalse(Double.isNaN(look.y), "Look Y must not be NaN at pitch " + pitch);
            assertFalse(Double.isNaN(look.z), "Look Z must not be NaN at pitch " + pitch);
            assertEquals(1.0, look.length(), EPSILON, "Look vector must be unit length at pitch " + pitch);
        }
    }

    @Test
    void fullSyntheticCircumnavigationPathRemainsContinuous() {
        Vector3d center = new Vector3d(0, 100, 0);
        RadialGravitySource body = new RadialGravitySource(center, 0.08, 10.0, 30.0);
        service.setActiveField(body);

        LocomotionTestEntity entity = new LocomotionTestEntity();
        GravityCameraFrame camera = new GravityCameraFrame(Vector3d.UP, 0.25);

        // Walk in a circle around the body at radius 10.2 (R=10 + feet)
        double R = 10.2;
        int steps = 72; // 5 degrees per step
        for (int i = 0; i <= steps; i++) {
            double angle = (2.0 * Math.PI * i) / steps;
            double px = center.x + R * Math.sin(angle);
            double py = center.y + R * Math.cos(angle);
            entity.setPosition(px, py, center.z);

            GravitySample sample = service.sampleFor(entity);
            camera.update(sample.localUp());

            // Check frame validity
            LocalEntityFrame frame = LocalEntityFrame.deriveFrom(sample, 0.0);
            assertEquals(
                0.0,
                frame.localDown()
                    .dot(frame.localUp()) + 1.0,
                EPSILON); // down = -up
            assertEquals(
                0.0,
                frame.tangentForward()
                    .dot(frame.localUp()),
                EPSILON); // fwd perp up
            assertEquals(
                0.0,
                frame.tangentRight()
                    .dot(frame.localUp()),
                EPSILON); // right perp up

            // Check camera look direction at horizontal pitch
            Vector3d look = camera.computeLookDirection(0.0, 0.0);
            assertFalse(Double.isNaN(look.x));
            assertFalse(Double.isNaN(look.y));
            assertFalse(Double.isNaN(look.z));
        }

        // Entity successfully completed continuous 360 degree loop without any teleport or discontinuity
        assertEquals(0.0, entity.posX, EPSILON);
        assertEquals(110.2, entity.posY, EPSILON);
    }
}
