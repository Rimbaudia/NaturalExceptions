package naturalexceptions.minecraft.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import naturalexceptions.core.gravity.GravityCameraFrame;
import naturalexceptions.core.gravity.GravitySample;
import naturalexceptions.core.gravity.LocalBodyStance;
import naturalexceptions.core.gravity.LocalEntityFrame;
import naturalexceptions.core.gravity.RadialGravitySource;
import naturalexceptions.core.gravity.UniformGravityField;
import naturalexceptions.core.gravity.Vector3d;
import naturalexceptions.minecraft.mixin.EntityLivingBaseAccessor;

final class RuntimeGravityLocomotionTest {

    private static final double EPSILON = 1e-6;

    private static class LocomotionTestEntity extends EntityLivingBase implements EntityLivingBaseAccessor {

        private boolean jumping;
        private int jumpTicks;
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
            this.prevPosX = x;
            this.prevPosY = y;
            this.prevPosZ = z;
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
        public int getJumpTicks() {
            return this.jumpTicks;
        }

        @Override
        public void setJumpTicks(int ticks) {
            this.jumpTicks = ticks;
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

    private static class LocomotionTestPlayer extends net.minecraft.entity.player.EntityPlayer
        implements EntityLivingBaseAccessor {

        private boolean jumping;
        private int jumpTicks;
        private NEPlayerMovementHandler.CollisionProvider collisionProvider;

        public LocomotionTestPlayer() {
            super(null, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "TestPlayer"));
            this.boundingBox.setBounds(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3);
            this.posX = 0.0;
            this.posY = 0.0;
            this.posZ = 0.0;
        }

        public static LocomotionTestPlayer create() {
            try {
                java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
                LocomotionTestPlayer player = (LocomotionTestPlayer) unsafe
                    .allocateInstance(LocomotionTestPlayer.class);
                java.lang.reflect.Field bbField = net.minecraft.entity.Entity.class.getDeclaredField("boundingBox");
                bbField.setAccessible(true);
                bbField.set(player, AxisAlignedBB.getBoundingBox(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3));
                player.capabilities = new net.minecraft.entity.player.PlayerCapabilities();
                player.jumpMovementFactor = 0.02F;
                player.stepHeight = 0.6F;
                player.setPosition(0.0, 0.0, 0.0);
                return player;
            } catch (Exception e) {
                throw new RuntimeException("Failed to allocate LocomotionTestPlayer", e);
            }
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
        public int getJumpTicks() {
            return this.jumpTicks;
        }

        @Override
        public void setJumpTicks(int ticks) {
            this.jumpTicks = ticks;
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
        public void addChatMessage(net.minecraft.util.IChatComponent p_145747_1_) {}

        @Override
        public boolean canCommandSenderUseCommand(int p_70003_1_, String p_70003_2_) {
            return false;
        }

        @Override
        public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() {
            return new net.minecraft.util.ChunkCoordinates((int) posX, (int) posY, (int) posZ);
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
        entity.setJumpTicks(0);
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
        entity.setJumpTicks(0);
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

    @Test
    void playerGravityIsAppliedExactlyOncePerTick() {
        // Sideways gravity along +X. Local down is +X, local up is -X.
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));

        LocomotionTestPlayer player = LocomotionTestPlayer.create();
        player.setPosition(9.7, 64.0, 0.0);
        player.rotationYaw = 0.0F;
        player.motionX = 0.0;
        player.motionY = 0.0;
        player.motionZ = 0.0;

        AxisAlignedBB wallBox = AxisAlignedBB.getBoundingBox(10.0, 50.0, -10.0, 11.0, 80.0, 10.0);
        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            if (bb.intersectsWith(wallBox)) {
                return Collections.singletonList(wallBox);
            }
            return Collections.emptyList();
        };
        player.setCollisionProvider(provider);

        NEGravityTickHandler tickHandler = new NEGravityTickHandler(service);

        // Simulate 20 ticks: both NEGravityTickHandler.onLivingUpdate AND
        // NEPlayerMovementHandler.handleMovement run in the tick sequence.
        // NEGravityTickHandler must skip EntityPlayer completely, ensuring
        // player gravity is applied exactly once per tick exclusively by NEPlayerMovementHandler.
        // There must be no phantom +0.08 upward velocity injection into motionY.
        for (int tick = 0; tick < 20; tick++) {
            tickHandler
                .onLivingUpdate(new net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent(player));
            NEPlayerMovementHandler.handleMovement(player, 0.0F, 0.0F, provider);

            assertEquals(
                0.0,
                player.motionY,
                EPSILON,
                "Player motionY must remain 0.0 (no upward launch injection at tick " + tick + ")");
            assertEquals(
                64.0,
                player.posY,
                EPSILON,
                "Player posY must remain 64.0 (no upward drift at tick " + tick + ")");
        }
    }

    @Test
    void localStepCannotAccumulateUpwardImpulseAcrossTicks() {
        // Wall walking (+X gravity, local down is +X, local up is -X)
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));

        LocomotionTestPlayer player = LocomotionTestPlayer.create();
        player.setPosition(9.7, 64.0, 0.0);
        player.rotationYaw = 0.0F; // facing along Z
        player.motionX = 0.0;
        player.motionY = 0.0;
        player.motionZ = 0.0;

        // Base wall at X=10.0 to 11.0
        AxisAlignedBB wallBox = AxisAlignedBB.getBoundingBox(10.0, 50.0, -10.0, 11.0, 80.0, 20.0);
        // Step obstacle sticking out 0.5 along localUp (-X) from X=9.5 to 10.0, starting at Z=0.5
        AxisAlignedBB stepObstacle = AxisAlignedBB.getBoundingBox(9.5, 50.0, 0.5, 10.0, 80.0, 20.0);

        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            List<AxisAlignedBB> boxes = new java.util.ArrayList<>();
            if (bb.intersectsWith(wallBox)) {
                boxes.add(wallBox);
            }
            if (bb.intersectsWith(stepObstacle)) {
                boxes.add(stepObstacle);
            }
            return boxes;
        };
        player.setCollisionProvider(provider);

        // Run 20 ticks of forward movement against and over the obstacle
        double initialX = player.posX; // 9.7
        for (int tick = 0; tick < 20; tick++) {
            NEPlayerMovementHandler.handleMovement(player, 0.0F, 1.0F, provider);

            // Elevation along localUp (-X) relative to initial position
            double elevationAlongLocalUp = initialX - player.posX;
            assertTrue(
                elevationAlongLocalUp <= NEPlayerMovementHandler.STEP_HEIGHT + EPSILON,
                "Elevation along localUp must never exceed STEP_HEIGHT (0.6), was: " + elevationAlongLocalUp
                    + " at tick "
                    + tick);

            // Velocity along localUp (-X) must not accumulate upward impulse (should be <= 0.05)
            double velocityAlongLocalUp = -player.motionX;
            assertTrue(
                velocityAlongLocalUp <= 0.05,
                "Velocity along localUp must remain bounded to 0 (no upward launch), was: " + velocityAlongLocalUp
                    + " at tick "
                    + tick);

            // Distance from active surface: either base wall (X=10.0) or step surface (X=9.5)
            double distFromWall = Math.abs(player.boundingBox.maxX - 10.0);
            double distFromStep = Math.abs(player.boundingBox.maxX - 9.5);
            double distFromActiveSurface = Math.min(distFromWall, distFromStep);
            assertTrue(
                distFromActiveSurface < 0.35,
                "Distance from surface must remain stable, was: " + distFromActiveSurface + " at tick " + tick);
        }
    }

    @Test
    void walkingAcrossRadialSurfaceDoesNotInjectEscapeVelocity() {
        Vector3d center = new Vector3d(0, 100, 0);
        RadialGravitySource body = new RadialGravitySource(center, 0.08, 10.0, 40.0);
        service.setActiveField(body);

        LocomotionTestPlayer player = LocomotionTestPlayer.create();
        player.setPosition(0.0, 110.0, 0.0);
        player.rotationYaw = 0.0F;

        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            List<AxisAlignedBB> boxes = new java.util.ArrayList<>();
            int minX = (int) Math.floor(bb.minX);
            int maxX = (int) Math.ceil(bb.maxX);
            int minY = (int) Math.floor(bb.minY);
            int maxY = (int) Math.ceil(bb.maxY);
            int minZ = (int) Math.floor(bb.minZ);
            int maxZ = (int) Math.ceil(bb.maxZ);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        double cx = x + 0.5 - center.x;
                        double cy = y + 0.5 - center.y;
                        double cz = z + 0.5 - center.z;
                        if (cx * cx + cy * cy + cz * cz <= 10.0 * 10.0) {
                            boxes.add(AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1));
                        }
                    }
                }
            }
            return boxes;
        };
        player.setCollisionProvider(provider);

        // Walk across top-to-side curvature over 40 ticks without jumping
        for (int tick = 0; tick < 40; tick++) {
            NEPlayerMovementHandler.handleMovement(player, 0.0F, 1.0F, provider);

            Vector3d rVec = new Vector3d(player.posX - center.x, player.posY - center.y, player.posZ - center.z);
            double r = rVec.length();
            Vector3d rHat = rVec.normalize();

            // Outward radial velocity
            double outwardRadialVelocity = player.motionX * rHat.x + player.motionY * rHat.y + player.motionZ * rHat.z;
            assertTrue(
                outwardRadialVelocity < 0.2,
                "Outward radial velocity must remain bounded (< 0.2), was: " + outwardRadialVelocity
                    + " at tick "
                    + tick);

            // Distance from surface: voxel shell surface is at nominal R=10.0
            assertTrue(
                Math.abs(r - 10.0) < 0.8,
                "Distance from surface must remain within contact tolerance, was: " + Math.abs(r - 10.0)
                    + " at tick "
                    + tick);

            // Radial distance must never continuously gain altitude into space
            assertTrue(r < 10.8, "Radial distance must not escape into space, r was: " + r + " at tick " + tick);
        }
    }

    @Test
    void standardGravityUsesVanillaMovementPath() {
        standardGravityPreservesVanillaMovementPath();
    }

    @Test
    void forwardInputMovesAlongProjectedViewForward() {
        // Floor (-Y gravity, local up is +Y)
        GravitySample floorSample = GravitySample.of(new Vector3d(0, -0.08, 0));
        Vector3d viewLookFloor = new Vector3d(0.5, -0.2, 0.8).normalize();
        LocalEntityFrame floorFrame = LocalEntityFrame.deriveFromView(floorSample, viewLookFloor, null);

        Vector3d fwdInputFloor = floorFrame.mapInput(1.0, 0.0);
        Vector3d projLookFloor = viewLookFloor.subtract(
            floorSample.localUp()
                .multiply(viewLookFloor.dot(floorSample.localUp())))
            .normalize();
        assertTrue(
            fwdInputFloor.dot(projLookFloor) > 0.99,
            "Forward input must move strictly along projected view forward on floor");

        Vector3d backInputFloor = floorFrame.mapInput(-1.0, 0.0);
        assertTrue(
            backInputFloor.dot(projLookFloor) < -0.99,
            "Backward input must move opposite to projected view forward on floor");

        // Wall (+X gravity, local up is -X)
        GravitySample wallSample = GravitySample.of(new Vector3d(0.08, 0, 0));
        Vector3d viewLookWall = new Vector3d(0.2, 0.7, 0.4).normalize();
        LocalEntityFrame wallFrame = LocalEntityFrame.deriveFromView(wallSample, viewLookWall, null);

        Vector3d fwdInputWall = wallFrame.mapInput(1.0, 0.0);
        Vector3d projLookWall = viewLookWall.subtract(
            wallSample.localUp()
                .multiply(viewLookWall.dot(wallSample.localUp())))
            .normalize();
        assertTrue(
            fwdInputWall.dot(projLookWall) > 0.99,
            "Forward input must move strictly along projected view forward on wall");

        // Ceiling (+Y gravity, local up is -Y)
        GravitySample ceilingSample = GravitySample.of(new Vector3d(0, 0.08, 0));
        Vector3d viewLookCeiling = new Vector3d(-0.3, 0.5, -0.6).normalize();
        LocalEntityFrame ceilingFrame = LocalEntityFrame.deriveFromView(ceilingSample, viewLookCeiling, null);

        Vector3d fwdInputCeiling = ceilingFrame.mapInput(1.0, 0.0);
        Vector3d projLookCeiling = viewLookCeiling.subtract(
            ceilingSample.localUp()
                .multiply(viewLookCeiling.dot(ceilingSample.localUp())))
            .normalize();
        assertTrue(
            fwdInputCeiling.dot(projLookCeiling) > 0.99,
            "Forward input must move strictly along projected view forward on ceiling");
    }

    @Test
    void localFrameMaintainsConsistentHandedness() {
        Vector3d[] testUps = { Vector3d.UP, Vector3d.DOWN, new Vector3d(1, 0, 0), new Vector3d(-1, 0, 0),
            new Vector3d(0, 0, 1), new Vector3d(0, 0, -1), new Vector3d(1, 1, 0).normalize(),
            new Vector3d(0, 1, 1).normalize(), new Vector3d(1, 1, 1).normalize() };

        for (Vector3d up : testUps) {
            GravitySample sample = GravitySample.of(
                up.negate()
                    .multiply(0.08));
            for (double yaw = 0; yaw < 360; yaw += 45) {
                LocalEntityFrame frame = LocalEntityFrame.deriveFrom(sample, yaw);
                assertEquals(
                    1.0,
                    frame.basisDeterminant(),
                    1e-5,
                    "Basis determinant must be +1.0 for up=" + up + " yaw=" + yaw);
            }
        }
    }

    @Test
    void localFrameDoesNotFlipForwardAcrossWallTransition() {
        // Continuous walk from top pole (0, 1, 0) to side wall (0, 0, 1)
        int steps = 50;
        Vector3d prevFwd = null;

        for (int i = 0; i <= steps; i++) {
            double angle = (Math.PI * 0.5 * i) / steps;
            Vector3d up = new Vector3d(0, Math.cos(angle), Math.sin(angle)).normalize();
            GravitySample sample = GravitySample.of(
                up.negate()
                    .multiply(0.08));
            LocalEntityFrame frame = LocalEntityFrame.deriveFrom(sample, 0.0);

            if (prevFwd != null) {
                double dot = frame.tangentForward()
                    .dot(prevFwd);
                assertTrue(
                    dot > 0.95,
                    "Tangent forward must change smoothly without 180-degree flip: step " + i + " dot=" + dot);
            }
            prevFwd = frame.tangentForward();
        }
    }

    @Test
    void cameraPreservesStableRollAcrossTopSideBottom() {
        GravityCameraFrame camera = new GravityCameraFrame(Vector3d.UP, 0.1);

        // Circumnavigate from top (+Y) through side (+X) to bottom (-Y)
        int steps = 72;
        for (int i = 0; i <= steps; i++) {
            double angle = (Math.PI * i) / steps;
            Vector3d targetUp = new Vector3d(Math.sin(angle), Math.cos(angle), 0.0).normalize();
            camera.update(targetUp);

            assertFalse(Double.isNaN(camera.currentUp().x));
            assertFalse(Double.isNaN(camera.currentUp().y));
            assertFalse(Double.isNaN(camera.currentUp().z));
            assertEquals(
                1.0,
                camera.currentUp()
                    .length(),
                EPSILON);

            GravityCameraFrame.CameraBasis basis = camera.computeCameraBasis(0.0, 0.0);
            assertEquals(1.0, basis.determinant(), 1e-5, "Camera basis determinant must remain strictly +1.0");
        }
    }

    @Test
    void cameraDoesNotChooseOpposite180DegreeRollSolution() {
        Vector3d[] surfaceUps = { Vector3d.UP, new Vector3d(1, 0, 0), new Vector3d(-1, 0, 0), new Vector3d(0, 0, 1),
            new Vector3d(0, 0, -1), Vector3d.DOWN };

        for (Vector3d up : surfaceUps) {
            GravityCameraFrame camera = new GravityCameraFrame(up, 1.0);
            camera.update(up);

            for (double yaw = 0; yaw < 360; yaw += 90) {
                GravityCameraFrame.CameraBasis basis = camera.computeCameraBasis(yaw, 0.0);

                // At pitch 0, screen up must strictly align with currentUp (dot == 1.0, never -1.0)
                double upAlignment = basis.up.dot(camera.currentUp());
                assertEquals(
                    1.0,
                    upAlignment,
                    1e-4,
                    "Screen up must point along localUp (not upside down) for up=" + up + " yaw=" + yaw);

                // Determinant must be +1.0 (strictly right-handed, no mirror inversion)
                assertEquals(
                    1.0,
                    basis.determinant(),
                    1e-5,
                    "Basis must have determinant +1.0 for up=" + up + " yaw=" + yaw);
            }
        }
    }

    @Test
    void jumpInputProducesSingleLocalUpImpulse() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0)); // local up is -X

        LocomotionTestPlayer player = LocomotionTestPlayer.create();
        player.setPosition(9.7, 64.0, 0.0);
        player.motionX = 0.0;
        player.motionY = 0.0;
        player.motionZ = 0.0;

        NEPlayerMovementHandler.applyJump(player);

        assertEquals(
            -NEPlayerMovementHandler.JUMP_IMPULSE,
            player.motionX,
            EPSILON,
            "Jump impulse must be along localUp (-X)");
        assertEquals(0.0, player.motionY, EPSILON);
        assertEquals(0.0, player.motionZ, EPSILON);
        assertTrue(player.isAirBorne, "Player must be set to airborne");
        assertFalse(player.onGround, "Player must not be on ground immediately after jump");
        assertEquals(10, player.getJumpTicks(), "Jump ticks cooldown must be set to 10");
    }

    @Test
    void airbornePlayerDoesNotReceiveRepeatedJumpImpulse() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0)); // local up is -X

        LocomotionTestPlayer player = LocomotionTestPlayer.create();
        player.setPosition(9.7, 64.0, 0.0);
        player.setJumping(true);

        AxisAlignedBB wallBox = AxisAlignedBB.getBoundingBox(10.0, 50.0, -10.0, 11.0, 80.0, 10.0);
        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            if (bb.intersectsWith(wallBox)) {
                return Collections.singletonList(wallBox);
            }
            return Collections.emptyList();
        };
        player.setCollisionProvider(provider);

        // Tick 1: Grounded, jumps
        NEPlayerMovementHandler.handleMovement(player, 0.0F, 0.0F, provider);
        double velAfterJump = player.motionX;
        assertTrue(velAfterJump < -0.3, "First tick must jump along -X");

        // Tick 2: Airborne (jumpTicks > 0 or not grounded). Next handleMovement call must NOT add another impulse
        NEPlayerMovementHandler.handleMovement(player, 0.0F, 0.0F, provider);
        assertTrue(player.motionX >= velAfterJump - 0.05, "Airborne player must not receive repeated jump impulse");
    }

    @Test
    void localStepCannotAccumulateRadialOutwardVelocity() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0)); // local up is -X

        LocomotionTestPlayer player = LocomotionTestPlayer.create();
        player.setPosition(9.7, 64.0, 0.0);
        Vector3d localUp = new Vector3d(-1, 0, 0);

        AxisAlignedBB wallBox = AxisAlignedBB.getBoundingBox(10.0, 50.0, -10.0, 11.0, 80.0, 20.0);
        AxisAlignedBB obstacle = AxisAlignedBB.getBoundingBox(9.6, 50.0, 0.5, 10.0, 80.0, 20.0);
        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            List<AxisAlignedBB> boxes = new java.util.ArrayList<>();
            if (bb.intersectsWith(wallBox)) boxes.add(wallBox);
            if (bb.intersectsWith(obstacle)) boxes.add(obstacle);
            return boxes;
        };
        player.setCollisionProvider(provider);

        // Perform step over obstacle
        boolean stepped = NEPlayerMovementHandler.attemptLocalStep(player, 0.0, 0.0, 0.3, localUp, 0.6, provider);
        assertTrue(stepped, "Obstacle must be stepped over");
        assertEquals(0.0, player.motionX, EPSILON, "Step must zero out motion along localUp");
        assertEquals(0.0, player.motionY, EPSILON);
        assertEquals(0.0, player.motionZ, EPSILON);
    }

    @Test
    void supportTransitionsWithoutGroundingThrash() {
        // Radial body with center at (0, 100, 0), radius 10
        Vector3d center = new Vector3d(0, 100, 0);
        RadialGravitySource body = new RadialGravitySource(center, 0.08, 10.0, 40.0);
        service.setActiveField(body);

        LocomotionTestEntity entity = new LocomotionTestEntity();

        // Curved spherical shell at R=10
        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            List<AxisAlignedBB> boxes = new java.util.ArrayList<>();
            int minX = (int) Math.floor(bb.minX);
            int maxX = (int) Math.ceil(bb.maxX);
            int minY = (int) Math.floor(bb.minY);
            int maxY = (int) Math.ceil(bb.maxY);
            int minZ = (int) Math.floor(bb.minZ);
            int maxZ = (int) Math.ceil(bb.maxZ);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        double cx = x + 0.5 - center.x;
                        double cy = y + 0.5 - center.y;
                        double cz = z + 0.5 - center.z;
                        if (cx * cx + cy * cy + cz * cz <= 10.0 * 10.0) {
                            boxes.add(AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1));
                        }
                    }
                }
            }
            return boxes;
        };
        LocomotionTestPlayer player = LocomotionTestPlayer.create();
        player.setPosition(0.0, 110.0, 0.0);
        player.setCollisionProvider(provider);

        // Walk across the curvature transition over 30 consecutive ticks
        int groundedTicks = 0;
        for (int tick = 0; tick < 30; tick++) {
            NEPlayerMovementHandler.handleMovement(player, 0.0F, 1.0F, provider);
            if (player.onGround) {
                groundedTicks++;
            }
        }
        assertTrue(
            groundedTicks >= 28,
            "Player must remain grounded without thrash across transition (grounded " + groundedTicks
                + " of 30 ticks)");
    }

    @Test
    void bodyUpMatchesLocalUp() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(10.0, 64.0, 0.0);

        GravitySample sample = service.sampleFor(entity);
        Vector3d expectedUp = sample.localUp();

        LocalBodyStance stance = new LocalBodyStance(
            new Vector3d(entity.posX, entity.posY + 0.9, entity.posZ),
            sample.localDown(),
            0.3,
            0.9);

        assertEquals(expectedUp.x, stance.localUp().x, EPSILON);
        assertEquals(expectedUp.y, stance.localUp().y, EPSILON);
        assertEquals(expectedUp.z, stance.localUp().z, EPSILON);
    }

    @Test
    void bodySupportPointLiesAlongLocalDown() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(10.0, 64.0, 0.0);

        GravitySample sample = service.sampleFor(entity);
        Vector3d center = new Vector3d(entity.posX, entity.posY + 0.9, entity.posZ);
        LocalBodyStance stance = new LocalBodyStance(center, sample.localDown(), 0.3, 0.9);

        Vector3d supDir = stance.supportPoint()
            .subtract(center)
            .normalize();
        assertEquals(sample.localDown().x, supDir.x, EPSILON);
        assertEquals(sample.localDown().y, supDir.y, EPSILON);
        assertEquals(sample.localDown().z, supDir.z, EPSILON);
    }

    @Test
    void bodyForwardMatchesMovementForward() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(10.0, 64.0, 0.0);
        entity.rotationYaw = 0.0F;

        GravitySample sample = service.sampleFor(entity);
        LocalEntityFrame frame = LocalEntityFrame.deriveFrom(sample, entity.rotationYaw);
        Vector3d moveFwd = frame.mapInput(1.0, 0.0);

        // Movement forward on wall (+X gravity) must be along tangent plane orthogonal to localUp
        assertEquals(0.0, moveFwd.dot(sample.localUp()), EPSILON);
        assertEquals(0.0, moveFwd.dot(sample.localDown()), EPSILON);
        assertTrue(moveFwd.length() > 0.99, "Movement forward must be unit magnitude");
    }

    @Test
    void cameraAndBodyShareTargetUp() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(10.0, 64.0, 0.0);

        GravitySample sample = service.sampleFor(entity);
        LocalBodyStance stance = new LocalBodyStance(
            new Vector3d(entity.posX, entity.posY + 0.9, entity.posZ),
            sample.localDown(),
            0.3,
            0.9);

        GravityCameraFrame cameraFrame = new GravityCameraFrame(Vector3d.UP, 0.15);
        cameraFrame.update(sample.localUp());

        // Stepping camera frame toward targetUp converges smoothly to stance.localUp()
        for (int i = 0; i < 50; i++) {
            cameraFrame.update(sample.localUp());
        }
        assertEquals(stance.localUp().x, cameraFrame.currentUp().x, 2e-3);
        assertEquals(stance.localUp().y, cameraFrame.currentUp().y, 2e-3);
        assertEquals(stance.localUp().z, cameraFrame.currentUp().z, 2e-3);
        assertTrue(
            cameraFrame.currentUp()
                .dot(stance.localUp()) > 0.99999,
            "Camera up must align with stance localUp within 0.99999");
    }

    @Test
    void sideWallStanceRotatesBodyNinetyDegreesFromWorldUp() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(10.0, 64.0, 0.0);

        GravitySample sample = service.sampleFor(entity);
        LocalBodyStance stance = new LocalBodyStance(
            new Vector3d(entity.posX, entity.posY + 0.9, entity.posZ),
            sample.localDown(),
            0.3,
            0.9);

        assertEquals(90.0, stance.rotationFromWorldUpDegrees(), EPSILON);
    }

    @Test
    void undersideStanceRotatesBodyOneHundredEightyDegreesFromWorldUp() {
        service.setActiveField(UniformGravityField.of(0.0, 0.08, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(0.0, 98.2, 0.0);

        GravitySample sample = service.sampleFor(entity);
        LocalBodyStance stance = new LocalBodyStance(
            new Vector3d(entity.posX, entity.posY + 0.9, entity.posZ),
            sample.localDown(),
            0.3,
            0.9);

        assertEquals(180.0, stance.rotationFromWorldUpDegrees(), EPSILON);
    }

    @Test
    void orientationBasisHasPositiveDeterminant() {
        Vector3d[] testDowns = { new Vector3d(0, -1, 0), new Vector3d(1, 0, 0), new Vector3d(-1, 0, 0),
            new Vector3d(0, 0, 1), new Vector3d(0, 0, -1), new Vector3d(0, 1, 0), new Vector3d(1, -1, 0).normalize(),
            new Vector3d(0, 1, 1).normalize() };

        for (Vector3d down : testDowns) {
            LocalBodyStance stance = new LocalBodyStance(Vector3d.ZERO, down, 0.3, 0.9);
            LocalEntityFrame frame = stance.toEntityFrame(45.0, null);
            assertEquals(1.0, frame.basisDeterminant(), EPSILON, "Frame basis determinant must be +1 for down=" + down);
        }
    }

    @Test
    void orientationRemainsContinuousAcrossFullCircumnavigation() {
        int steps = 120;
        double maxDeltaDeg = 0.0;
        double prevAngle = 0.0;

        for (int i = 0; i <= steps; i++) {
            double theta = 2.0 * Math.PI * i / steps;
            Vector3d down = new Vector3d(Math.sin(theta), -Math.cos(theta), 0.0).normalize();
            LocalBodyStance stance = new LocalBodyStance(Vector3d.ZERO, down, 0.3, 0.9);
            double angleDeg = stance.rotationFromWorldUpDegrees();

            if (i > 0) {
                double delta = Math.abs(angleDeg - prevAngle);
                if (delta > maxDeltaDeg) {
                    maxDeltaDeg = delta;
                }
            }
            prevAngle = angleDeg;
        }

        assertTrue(
            maxDeltaDeg < 5.0,
            "Stance orientation must evolve continuously (max delta was " + maxDeltaDeg + " deg)");
    }

    @Test
    void wallJumpImpulsePointsAwayFromSupport() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestPlayer player = LocomotionTestPlayer.create();
        player.setPosition(9.7, 64.0, 0.0);
        player.motionX = 0.0;
        player.motionY = 0.0;
        player.motionZ = 0.0;

        AxisAlignedBB wallBox = AxisAlignedBB.getBoundingBox(10.0, 60.0, -10.0, 11.0, 70.0, 10.0);
        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            if (bb.intersectsWith(wallBox)) {
                return Collections.singletonList(wallBox);
            }
            return Collections.emptyList();
        };
        player.setCollisionProvider(provider);

        player.setJumping(true);
        NEPlayerMovementHandler.applyJump(player);

        assertTrue(
            player.motionX < -0.4,
            "Jump motion must point away from support wall along localUp (motionX was " + player.motionX + ")");
        assertEquals(0.0, player.motionY, EPSILON, "Wall jump should have zero Y impulse in pure X gravity");
        assertEquals(0.0, player.motionZ, EPSILON, "Wall jump should have zero Z impulse in pure X gravity");
        assertTrue(player.isAirBorne, "Player must become airborne after jump");
    }

    @Test
    void shouldApplyCustomOrientationOnlyWhenNonstandard() {
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(0.0, 64.0, 0.0);

        // Under standard downward gravity
        service.setActiveField(UniformGravityField.of(0.0, -0.08, 0.0));
        assertFalse(
            NEEntityRenderHandler.shouldApplyCustomOrientation(entity),
            "Custom orientation must NOT apply under standard downward gravity");

        // Under side-wall gravity
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        assertTrue(
            NEEntityRenderHandler.shouldApplyCustomOrientation(entity),
            "Custom orientation must apply under side-wall gravity");

        // Under ceiling gravity
        service.setActiveField(UniformGravityField.of(0.0, 0.08, 0.0));
        assertTrue(
            NEEntityRenderHandler.shouldApplyCustomOrientation(entity),
            "Custom orientation must apply under ceiling gravity");
    }

    @Test
    void eyePointUsesLocalUpInsteadOfGlobalY() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0)); // +X gravity, wall at +X
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);

        GravitySample sample = service.sampleFor(entity);
        Vector3d localDown = sample.localDown(); // (1, 0, 0)

        LocalBodyStance stance = LocalBodyStance.fromAabb(new Vector3d(9.7, 64.9, 0.0), localDown, 0.3, 0.9, 1.62);

        Vector3d eye = stance.eyePoint();
        // Eye extends along localUp (-X), so eye.x is displaced into the room, while Y is body center level
        assertTrue(eye.x < 9.7, "Eye point must extend along localUp (-X) into room");
        assertEquals(64.9, eye.y, EPSILON, "Eye point along global Y must not be shifted by global Y eye height");
    }

    @Test
    void sideWallEyePointLiesTowardLocalHeadNotSupport() {
        // Wall at X=10.0, player standing on wall with localDown = (+1, 0, 0)
        Vector3d center = new Vector3d(9.7, 64.9, 0.0);
        Vector3d localDown = new Vector3d(1.0, 0.0, 0.0);
        LocalBodyStance stance = new LocalBodyStance(center, localDown, 0.3, 0.9);

        Vector3d sup = stance.supportPoint(); // feet at X = 10.0
        Vector3d head = stance.headPoint(); // head at X = 9.4
        Vector3d eye = stance.eyePoint(); // eye at X = 8.38

        // Eye is on the head side of the support surface along localUp (-X)
        assertTrue(eye.x < sup.x, "Eye point must lie along localUp away from support");
        assertTrue(Math.abs(eye.x - head.x) < Math.abs(eye.x - sup.x), "Eye point must be closer to head than feet");
    }

    @Test
    void bottomEyePointLiesOnLocalHeadSide() {
        // Bottom pole: localDown = (0, 1, 0), localUp = (0, -1, 0)
        Vector3d center = new Vector3d(0.0, 10.0, 0.0);
        Vector3d localDown = new Vector3d(0.0, 1.0, 0.0);
        LocalBodyStance stance = new LocalBodyStance(center, localDown, 0.3, 0.9);

        Vector3d sup = stance.supportPoint(); // feet at Y = 10.9
        Vector3d eye = stance.eyePoint(); // eye at Y = 10.9 - 1.62 = 9.28

        assertTrue(eye.y < sup.y, "At bottom pole, eye point must lie below feet along localUp");
        assertEquals(center.x, eye.x, EPSILON);
        assertEquals(center.z, eye.z, EPSILON);
    }

    @Test
    void groundedStabilizationDoesNotPreventJumpDetachment() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestPlayer player = LocomotionTestPlayer.create();
        player.setPosition(9.7, 64.0, 0.0);
        player.motionX = 0.0;
        player.motionY = 0.0;
        player.motionZ = 0.0;

        AxisAlignedBB wallBox = AxisAlignedBB.getBoundingBox(10.0, 60.0, -10.0, 11.0, 70.0, 10.0);
        NEPlayerMovementHandler.CollisionProvider provider = (e, bb) -> {
            if (bb.intersectsWith(wallBox)) {
                return Collections.singletonList(wallBox);
            }
            return Collections.emptyList();
        };
        player.setCollisionProvider(provider);

        // First establish grounding
        NEPlayerMovementHandler.handleMovement(player, 0.0F, 0.0F, provider);
        assertTrue(player.onGround, "Player should be grounded initially");

        // Jump!
        player.setJumping(true);
        NEPlayerMovementHandler.applyJump(player);

        // Outward velocity along localUp (-X)
        assertTrue(player.motionX < -0.4, "Jump must give outward velocity along localUp (-X)");
        assertTrue(player.isAirBorne, "Player must be airborne after jump");
        assertFalse(player.onGround, "Player must not be onGround immediately after jump");
    }

    @Test
    void airbornePlayerReceivesNoSurfaceAdhesion() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        // Position entity in open space far from any collision boxes
        entity.setPosition(0.0, 64.0, 0.0);
        entity.motionX = 0.0;
        entity.motionY = 0.0;
        entity.motionZ = 0.0;
        entity.isAirBorne = true;

        NEPlayerMovementHandler.CollisionProvider emptyProvider = (e, bb) -> Collections.emptyList();
        NEPlayerMovementHandler.handleMovement(entity, 0.0F, 0.0F, emptyProvider);

        // Acceleration should be strictly equal to the gravity field acceleration (0.08, 0, 0)
        // damped by airborne drag (0.91), without any additional adhesion force
        assertEquals(0.08 * 0.91, entity.motionX, 1e-4, "Airborne entity must only receive physical gravity with drag");
        assertFalse(entity.onGround, "Airborne entity far from walls must not be marked onGround");
    }

    @Test
    void headingTransportPreservesForwardAcrossChangingUp() {
        LocalEntityFrame frame = LocalEntityFrame.standardMinecraft(); // up = (0, 1, 0), fwd = (0, 0, 1)

        // Transport along a curved trajectory from up=(0,1,0) toward up=(0,0,1)
        int steps = 10;
        for (int i = 1; i <= steps; i++) {
            double angle = (Math.PI * 0.5) * (i / (double) steps);
            Vector3d newUp = new Vector3d(0.0, Math.cos(angle), Math.sin(angle)).normalize();
            frame = frame.transportTo(newUp, 0.0);

            // Forward must remain orthogonal to localUp
            assertEquals(
                0.0,
                frame.tangentForward()
                    .dot(frame.localUp()),
                EPSILON);
            // Right must remain orthogonal to both
            assertEquals(
                0.0,
                frame.tangentRight()
                    .dot(frame.localUp()),
                EPSILON);
            assertEquals(
                0.0,
                frame.tangentRight()
                    .dot(frame.tangentForward()),
                EPSILON);
            // Determinant must be +1
            assertEquals(1.0, frame.basisDeterminant(), EPSILON);
        }
    }

    @Test
    void headingTransportDoesNotSpinAtBottomPole() {
        // Start near bottom pole at theta = 170 degrees in Y-Z plane
        double thetaStart = Math.toRadians(170.0);
        Vector3d upStart = new Vector3d(0.0, Math.cos(thetaStart), Math.sin(thetaStart)).normalize();
        LocalEntityFrame frame = LocalEntityFrame.standardMinecraft()
            .transportTo(upStart, 0.0);

        // Step across bottom pole (180 degrees) to 190 degrees
        Vector3d rightBefore = frame.tangentRight();
        for (double deg = 171.0; deg <= 190.0; deg += 1.0) {
            double rad = Math.toRadians(deg);
            Vector3d newUp = new Vector3d(0.0, Math.cos(rad), Math.sin(rad)).normalize();
            frame = frame.transportTo(newUp, 0.0);
        }
        Vector3d rightAfter = frame.tangentRight();

        // The tangentRight vector should not have flipped or spun around
        assertEquals(rightBefore.x, rightAfter.x, 1e-3, "tangentRight X must not flip at bottom pole");
        assertEquals(0.0, rightAfter.y, 1e-3, "tangentRight Y should remain near 0 in Y-Z plane transport");
        assertEquals(0.0, rightAfter.z, 1e-3, "tangentRight Z should remain near 0 in Y-Z plane transport");
    }

    @Test
    void holdingForwardTraversesThroughPoleInsteadOfOrbiting() {
        // Entity at equator of radial body: up = (0, 0, 1), fwd = (0, -1, 0)
        Vector3d initUp = new Vector3d(0.0, 0.0, 1.0);
        LocalEntityFrame frame = LocalEntityFrame.standardMinecraft()
            .transportTo(initUp, 0.0);

        // Transport towards bottom pole up = (0, -1, 0) through angle theta from 0 to PI
        int steps = 50;
        for (int i = 0; i <= steps; i++) {
            double angle = Math.PI * (i / (double) steps);
            Vector3d newUp = new Vector3d(0.0, -Math.sin(angle), Math.cos(angle)).normalize();
            frame = frame.transportTo(newUp, 0.0);

            // Throughout the traversal, motion must stay strictly in the Y-Z plane (X component == 0)
            // proving it never deflects or orbits azimuthally
            assertEquals(0.0, frame.tangentForward().x, EPSILON, "tangentForward must not develop an X component");
            assertEquals(0.0, frame.localUp().x, EPSILON, "localUp must not develop an X component");
        }
    }

    @Test
    void transportedFrameRemainsOrthonormal() {
        LocalEntityFrame frame = LocalEntityFrame.standardMinecraft();
        for (int i = 0; i < 100; i++) {
            double phi = Math.toRadians(i * 3.6);
            Vector3d newUp = new Vector3d(Math.sin(phi) * Math.cos(phi), Math.cos(phi), Math.sin(phi)).normalize();
            frame = frame.transportTo(newUp, 5.0);

            assertEquals(
                1.0,
                frame.localUp()
                    .length(),
                EPSILON);
            assertEquals(
                1.0,
                frame.tangentForward()
                    .length(),
                EPSILON);
            assertEquals(
                1.0,
                frame.tangentRight()
                    .length(),
                EPSILON);

            assertEquals(
                0.0,
                frame.localUp()
                    .dot(frame.tangentForward()),
                EPSILON);
            assertEquals(
                0.0,
                frame.localUp()
                    .dot(frame.tangentRight()),
                EPSILON);
            assertEquals(
                0.0,
                frame.tangentForward()
                    .dot(frame.tangentRight()),
                EPSILON);
        }
    }

    @Test
    void transportedFrameMaintainsPositiveDeterminant() {
        LocalEntityFrame frame = LocalEntityFrame.standardMinecraft();
        for (int i = 0; i < 50; i++) {
            double angle = 2.0 * Math.PI * (i / 50.0);
            Vector3d newUp = new Vector3d(Math.sin(angle), -Math.cos(angle), 0.0).normalize();
            frame = frame.transportTo(newUp, 7.5);
            assertEquals(1.0, frame.basisDeterminant(), EPSILON, "Determinant must remain +1 for step " + i);
        }
    }

    @Test
    void cameraTargetUsesTransportedForward() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);
        entity.rotationYaw = 0.0F;

        LocalEntityFrame frame = service.getOrUpdateLocalFrame(entity);
        Vector3d fwd = frame.tangentForward();

        // Forward vector must be orthogonal to localUp
        assertEquals(0.0, fwd.dot(frame.localUp()), EPSILON);
        // Canonical frame is shared by service
        assertEquals(frame, service.getOrUpdateLocalFrame(entity));
    }

    @Test
    void fullCircumnavigationReturnsWithContinuousHeading() {
        LocalEntityFrame initial = LocalEntityFrame.standardMinecraft();
        LocalEntityFrame frame = initial;
        int steps = 120;
        for (int i = 1; i <= steps; i++) {
            double angle = 2.0 * Math.PI * (i / (double) steps);
            Vector3d newUp = new Vector3d(Math.sin(angle), Math.cos(angle), 0.0).normalize();
            frame = frame.transportTo(newUp, 0.0);
        }

        // Full 360 degree loop with zero yaw delta returns to initial frame
        assertEquals(initial.localUp().x, frame.localUp().x, 1e-4);
        assertEquals(initial.localUp().y, frame.localUp().y, 1e-4);
        assertEquals(initial.localUp().z, frame.localUp().z, 1e-4);

        assertEquals(initial.tangentForward().x, frame.tangentForward().x, 1e-4);
        assertEquals(initial.tangentForward().y, frame.tangentForward().y, 1e-4);
        assertEquals(initial.tangentForward().z, frame.tangentForward().z, 1e-4);
    }

    @Test
    void cameraBasisMatchesCanonicalRenderedFrame() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);
        entity.rotationYaw = 0.0F;

        NEPlayerPresentationService.instance()
            .reset();
        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);

        Vector3d up = frame.renderedUp();
        Vector3d fwd = frame.renderedForward();
        Vector3d right = frame.renderedRight();

        // Must match physical frame target when settled
        assertEquals(
            frame.physicalFrame()
                .localUp().x,
            up.x,
            EPSILON);
        assertEquals(
            frame.physicalFrame()
                .localUp().y,
            up.y,
            EPSILON);
        assertEquals(
            frame.physicalFrame()
                .localUp().z,
            up.z,
            EPSILON);

        // Orthonormal basis checks
        assertEquals(0.0, up.dot(fwd), EPSILON);
        assertEquals(0.0, up.dot(right), EPSILON);
        assertEquals(0.0, fwd.dot(right), EPSILON);
        assertEquals(1.0, up.length(), EPSILON);
        assertEquals(1.0, fwd.length(), EPSILON);
        assertEquals(1.0, right.length(), EPSILON);

        // Determinant must be +1.0
        double det = right.dot(up.cross(fwd));
        assertEquals(1.0, det, EPSILON);
    }

    @Test
    void cameraWorldToViewMatrixHasPositiveOrientation() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);
        entity.rotationPitch = -15.0F; // tilted upward 15 degrees

        NEPlayerPresentationService.instance()
            .reset();
        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);

        float[] m = frame.cameraMatrix();
        // Column-major: column 0 = (m[0], m[1], m[2]), column 1 = (m[4], m[5], m[6]), column 2 = (m[8], m[9], m[10])
        double det = m[0] * (m[5] * m[10] - m[6] * m[9]) - m[4] * (m[1] * m[10] - m[2] * m[9])
            + m[8] * (m[1] * m[6] - m[2] * m[5]);
        assertEquals(1.0, det, 1e-4, "Camera view matrix rotation submatrix must have determinant +1.0");
    }

    @Test
    void cameraMatrixDoesNotDoubleApplyVanillaYawPitch() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);
        entity.rotationPitch = 0.0F;

        NEPlayerPresentationService.instance()
            .reset();
        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);

        float[] m = frame.cameraMatrix();
        Vector3d look = frame.computeLookVector();

        // M * look must transform directly to (0, 0, -1) in eye space
        double eyeX = m[0] * look.x + m[4] * look.y + m[8] * look.z;
        double eyeY = m[1] * look.x + m[5] * look.y + m[9] * look.z;
        double eyeZ = m[2] * look.x + m[6] * look.y + m[10] * look.z;

        assertEquals(0.0, eyeX, 1e-4);
        assertEquals(0.0, eyeY, 1e-4);
        assertEquals(-1.0, eyeZ, 1e-4);
    }

    @Test
    void cameraOriginAndOrientationAreIndependent() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);
        entity.prevPosX = 9.7;
        entity.prevPosY = 64.0;
        entity.prevPosZ = 0.0;

        NEPlayerPresentationService.instance()
            .reset();
        NEPlayerPresentationFrame frame1 = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 1.0F);

        // Move entity
        entity.setPosition(9.7, 80.0, 15.0);
        entity.prevPosX = 9.7;
        entity.prevPosY = 80.0;
        entity.prevPosZ = 15.0;
        NEPlayerPresentationFrame frame2 = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 1.0F);

        // Orientation matrices must be identical
        float[] m1 = frame1.cameraMatrix();
        float[] m2 = frame2.cameraMatrix();
        for (int i = 0; i < 16; i++) {
            assertEquals(m1[i], m2[i], 1e-4F);
        }

        // But eyePoint positions must reflect the position change
        assertEquals(frame1.eyePoint().x, frame2.eyePoint().x, 1e-4);
        assertEquals(frame1.eyePoint().y + 16.0, frame2.eyePoint().y, 1e-4);
        assertEquals(frame1.eyePoint().z + 15.0, frame2.eyePoint().z, 1e-4);
    }

    @Test
    void firstPersonHandUsesLocalCameraFrame() {
        // Under nonstandard gravity, presentation frame guarantees camera view matrix
        // aligns screen with local gravity rather than world Y
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);

        NEPlayerPresentationService.instance()
            .reset();
        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);

        assertTrue(service.isNonstandardGravityActive());
        // Screen up matches renderedUp when pitch is 0
        Vector3d look = frame.computeLookVector();
        Vector3d camRight = frame.renderedForward()
            .cross(frame.renderedUp())
            .normalize();
        Vector3d screenUp = camRight.cross(look)
            .normalize();
        assertEquals(frame.renderedUp().x, screenUp.x, 1e-4);
        assertEquals(frame.renderedUp().y, screenUp.y, 1e-4);
        assertEquals(frame.renderedUp().z, screenUp.z, 1e-4);
    }

    @Test
    void thirdPersonBodyUpMatchesLocalUp() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);

        NEPlayerPresentationService.instance()
            .reset();
        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);

        float[] modelM = frame.modelMatrix();
        // Column 1 is model UP
        Vector3d modelUp = new Vector3d(modelM[4], modelM[5], modelM[6]);
        assertEquals(frame.renderedUp().x, modelUp.x, 1e-4);
        assertEquals(frame.renderedUp().y, modelUp.y, 1e-4);
        assertEquals(frame.renderedUp().z, modelUp.z, 1e-4);

        // Model matrix determinant is +1.0
        double det = modelM[0] * (modelM[5] * modelM[10] - modelM[6] * modelM[9])
            - modelM[4] * (modelM[1] * modelM[10] - modelM[2] * modelM[9])
            + modelM[8] * (modelM[1] * modelM[6] - modelM[2] * modelM[5]);
        assertEquals(1.0, det, 1e-4);
    }

    @Test
    void bodyForwardMatchesTransportedForward() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(9.7, 64.0, 0.0);

        NEPlayerPresentationService.instance()
            .reset();
        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);

        float[] modelM = frame.modelMatrix();
        // Model forward in model space is -Z, which transforms to -Column 2
        Vector3d bodyForward = new Vector3d(-modelM[8], -modelM[9], -modelM[10]);
        assertEquals(frame.renderedForward().x, bodyForward.x, 1e-4);
        assertEquals(frame.renderedForward().y, bodyForward.y, 1e-4);
        assertEquals(frame.renderedForward().z, bodyForward.z, 1e-4);
    }

    @Test
    void locomotionDistanceUsesLocalTangentDisplacement() {
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(10.0, 64.0, 0.0);
        Vector3d localUp = new Vector3d(1.0, 0.0, 0.0); // side wall

        // Pure vertical world-Y movement along the wall
        entity.prevPosX = 10.0;
        entity.posX = 10.0;
        entity.prevPosY = 64.0;
        entity.posY = 64.25;
        entity.prevPosZ = 0.0;
        entity.posZ = 0.0;

        NEPlayerMovementHandler.updateLocomotionAnimation(entity, localUp, true);

        assertTrue(entity.limbSwingAmount > 0.0, "Vertical movement along wall must advance limbSwingAmount");
        assertEquals(
            0.25 * 0.6,
            entity.distanceWalkedModified,
            1e-4,
            "distanceWalkedModified must accumulate tangential distance");
    }

    @Test
    void verticalWorldMovementStillAdvancesWalkCycle() {
        LocomotionTestEntity entity = new LocomotionTestEntity();
        Vector3d localUp = new Vector3d(1.0, 0.0, 0.0); // side wall

        float initialSwing = entity.limbSwing;
        for (int tick = 0; tick < 10; tick++) {
            entity.prevPosY = 64.0 + tick * 0.1;
            entity.posY = 64.0 + (tick + 1) * 0.1;
            entity.prevPosX = 10.0;
            entity.posX = 10.0;
            entity.prevPosZ = 0.0;
            entity.posZ = 0.0;

            NEPlayerMovementHandler.updateLocomotionAnimation(entity, localUp, true);
        }

        assertTrue(
            entity.limbSwing > initialSwing + 1.0F,
            "Walk cycle must advance continuously across vertical movement ticks");
        assertTrue(entity.distanceWalkedModified > 0.5F, "distanceWalkedModified must accumulate across ticks");
    }

    @Test
    void standingStillDoesNotAdvanceWalkCycle() {
        LocomotionTestEntity entity = new LocomotionTestEntity();
        Vector3d localUp = new Vector3d(1.0, 0.0, 0.0);
        entity.limbSwingAmount = 0.5F;
        entity.limbSwing = 10.0F;
        entity.prevPosX = 10.0;
        entity.posX = 10.0;
        entity.prevPosY = 64.0;
        entity.posY = 64.0;
        entity.prevPosZ = 0.0;
        entity.posZ = 0.0;

        for (int tick = 0; tick < 15; tick++) {
            NEPlayerMovementHandler.updateLocomotionAnimation(entity, localUp, true);
        }

        assertTrue(entity.limbSwingAmount < 0.01F, "limbSwingAmount must decay to zero when standing still");
        float settledSwing = entity.limbSwing;
        NEPlayerMovementHandler.updateLocomotionAnimation(entity, localUp, true);
        assertEquals(settledSwing, entity.limbSwing, 0.005F, "Limb swing must stop advancing when standing still");
    }

    @Test
    void fullCircumnavigationKeepsAnimationContinuous() {
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.limbSwing = 0.0F;
        int steps = 120;
        for (int i = 0; i < steps; i++) {
            double angle = 2.0 * Math.PI * (i / (double) steps);
            Vector3d localUp = new Vector3d(Math.sin(angle), Math.cos(angle), 0.0).normalize();
            Vector3d tangent = new Vector3d(Math.cos(angle), -Math.sin(angle), 0.0).normalize();

            entity.prevPosX = Math.sin(angle) * 10.0;
            entity.prevPosY = Math.cos(angle) * 10.0;
            entity.prevPosZ = 0.0;

            entity.posX = entity.prevPosX + tangent.x * 0.15;
            entity.posY = entity.prevPosY + tangent.y * 0.15;
            entity.posZ = 0.0;

            NEPlayerMovementHandler.updateLocomotionAnimation(entity, localUp, true);
            assertFalse(Float.isNaN(entity.limbSwing), "Limb swing must remain a valid number");
            assertTrue(entity.limbSwingAmount > 0.0F, "Limb swing amount must remain active while walking around body");
        }
        assertTrue(entity.limbSwing > 10.0F, "Full circumnavigation must advance limb swing continuously");
    }

    @Test
    void renderFrameDoesNotFlipAtBottomPole() {
        LocomotionTestEntity entity = new LocomotionTestEntity();
        NEPlayerPresentationService.instance()
            .reset();

        // Transition through bottom pole: localUp passes from (-0.1, -0.99, 0) through (0, -1, 0) to (0.1, -0.99, 0)
        Vector3d prevFwd = null;
        for (int i = -5; i <= 5; i++) {
            double x = i * 0.05;
            double y = -Math.sqrt(1.0 - x * x);
            service.setActiveField(UniformGravityField.of(-x * 0.08, -y * 0.08, 0.0));
            entity.setPosition(x * 10.0, y * 10.0, 0.0);

            NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
                .getPresentationFrame(entity, 0.0F);

            if (prevFwd != null) {
                double dot = prevFwd.dot(frame.renderedForward());
                assertTrue(dot > 0.9, "Forward vector must not flip 180 degrees at bottom pole");
            }
            prevFwd = frame.renderedForward();
        }
    }

    @Test
    void standardGravityUsesVanillaPresentationPath() {
        service.resetToDefault();
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(0.0, 64.0, 0.0);

        assertFalse(service.isNonstandardGravityActive(), "Standard gravity must not report nonstandard active");
        assertFalse(
            NEEntityRenderHandler.shouldApplyCustomOrientation(entity),
            "Custom orientation must not apply under standard gravity");
    }

    @Test
    void firstPersonEyeUsesPhysicallyValidLocalStance() {
        // Wall at X = 5.0, localDown = (+1, 0, 0), localUp = (-1, 0, 0)
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;
        // Player standing on the wall: feet at X = 5.0, bounding box [4.4, 5.0] along X, posY at 65.62
        entity.setPosition(4.7, 64.0, 0.0);
        entity.boundingBox.setBounds(4.4, 64.0, -0.3, 5.0, 65.8, 0.3);

        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);

        // Support point must touch the wall surface at X = 5.0
        assertEquals(5.0, frame.supportPoint().x, 1e-3, "Support point must lie on wall surface at X=5.0");

        // Eye point must extend outward along localUp (-1, 0, 0) by physicalEyeHeight (1.62)
        // 5.0 - 1.62 = 3.38
        assertEquals(3.38, frame.eyePoint().x, 1e-3, "Eye point must extend outward from wall along localUp by 1.62");
        assertFalse(
            Math.abs(frame.eyePoint().x - 4.88) < 0.1,
            "Eye point must not be placed at 0.12 from wall (ankles)");
    }

    @Test
    void renderedOrientationSmoothingDoesNotMoveEyeThroughSolid() {
        NEPlayerPresentationService.instance()
            .reset();
        // Frame 1: on top surface under downward gravity (localUp = (0, 1, 0))
        service.setActiveField(UniformGravityField.of(0.0, -0.08, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;
        entity.setPosition(5.3, 64.0, 0.0);
        entity.boundingBox.setBounds(5.0, 64.0, -0.3, 5.6, 65.8, 0.3);

        // Render frame 1: currUp starts at (0, 1, 0)
        NEPlayerPresentationFrame frame1 = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);
        assertEquals(1.0, frame1.renderedUp().y, 1e-3, "Initial rendered up must be (0, 1, 0)");

        // Step onto side wall: gravity pulls along -X, physical localUp switches to (+1, 0, 0)
        service.setActiveField(UniformGravityField.of(-0.08, 0.0, 0.0));

        // Render frame 2: currUp is slerping toward (1, 0, 0), so renderedUp has x > 0 and y > 0
        NEPlayerPresentationFrame frame2 = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);

        // Visual orientation is smoothed
        assertTrue(frame2.renderedUp().x > 0.1 && frame2.renderedUp().y > 0.1, "Rendered up must be smoothed");

        // BUT eyePoint must remain strictly anchored to the physical stance (along physical localUp +X),
        // NOT diagonally rotated along renderedUp which would penetrate the corner block
        assertEquals(5.0 + 1.62, frame2.eyePoint().x, 1e-3, "Eye point X must remain on physical stance");
        assertTrue(frame2.eyePoint().x > 6.0, "Eye point must remain extended outward from wall");
    }

    @Test
    void desiredCameraOriginInsideSolidIsDepenetrated() {
        // Block obstacle at [0, 0, 0] to [1, 1, 1]
        AxisAlignedBB blockBox = AxisAlignedBB.getBoundingBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        List<AxisAlignedBB> blockList = Collections.singletonList(blockBox);
        NECameraCollisionHelper.CollisionBoxProvider boxProvider = (e, bb) -> blockList;
        NECameraCollisionHelper.RayTraceProvider rayTracer = (start, end) -> {
            // Ray from start (outside, y=1.5) to end (inside, y=0.5) hits top face at y=1.0
            if (start.yCoord >= 1.0 && end.yCoord < 1.0) {
                return new MovingObjectPosition(0, 0, 0, 1, Vec3.createVectorHelper(end.xCoord, 1.0, end.zCoord));
            }
            return null;
        };

        LocomotionTestEntity entity = new LocomotionTestEntity();
        Vector3d bodyCenter = new Vector3d(0.5, 1.5, 0.5);
        Vector3d desiredEye = new Vector3d(0.5, 0.5, 0.5); // inside the block!

        Vector3d resolved = NECameraCollisionHelper
            .resolveCameraPosition(rayTracer, boxProvider, entity, bodyCenter, desiredEye, null);

        // Resolved eye must not be inside the block
        assertFalse(
            NECameraCollisionHelper.isPointInsideSolid(boxProvider, entity, resolved),
            "Resolved camera position must not be inside solid block");
        assertTrue(
            resolved.y >= 1.0 + NECameraCollisionHelper.SURFACE_BACKOFF_MARGIN - 1e-3,
            "Resolved camera position must be pushed outside top face of block");
    }

    @Test
    void cameraDepenetrationDoesNotMovePlayer() {
        AxisAlignedBB blockBox = AxisAlignedBB.getBoundingBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        NECameraCollisionHelper.CollisionBoxProvider boxProvider = (e, bb) -> Collections.singletonList(blockBox);

        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.setPosition(0.5, 1.5, 0.5);

        double origX = entity.posX;
        double origY = entity.posY;
        double origZ = entity.posZ;
        AxisAlignedBB origBb = entity.boundingBox.copy();

        Vector3d bodyCenter = new Vector3d(origX, origY, origZ);
        Vector3d desiredEye = new Vector3d(0.5, 0.5, 0.5);

        NECameraCollisionHelper.resolveCameraPosition(null, boxProvider, entity, bodyCenter, desiredEye, null);

        assertEquals(origX, entity.posX, 1e-9, "Entity posX must not be modified by camera depenetration");
        assertEquals(origY, entity.posY, 1e-9, "Entity posY must not be modified by camera depenetration");
        assertEquals(origZ, entity.posZ, 1e-9, "Entity posZ must not be modified by camera depenetration");
        assertEquals(origBb.minX, entity.boundingBox.minX, 1e-9, "Entity bounding box must not change");
        assertEquals(origBb.minY, entity.boundingBox.minY, 1e-9, "Entity bounding box must not change");
    }

    @Test
    void cameraDepenetrationDoesNotModifyVelocity() {
        AxisAlignedBB blockBox = AxisAlignedBB.getBoundingBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        NECameraCollisionHelper.CollisionBoxProvider boxProvider = (e, bb) -> Collections.singletonList(blockBox);

        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.motionX = 0.05;
        entity.motionY = -0.08;
        entity.motionZ = 0.12;

        Vector3d bodyCenter = new Vector3d(0.5, 1.5, 0.5);
        Vector3d desiredEye = new Vector3d(0.5, 0.5, 0.5);

        NECameraCollisionHelper.resolveCameraPosition(null, boxProvider, entity, bodyCenter, desiredEye, null);

        assertEquals(0.05, entity.motionX, 1e-9, "MotionX must remain untouched");
        assertEquals(-0.08, entity.motionY, 1e-9, "MotionY must remain untouched");
        assertEquals(0.12, entity.motionZ, 1e-9, "MotionZ must remain untouched");
    }

    @Test
    void cameraSweepStopsBeforeVoxelCollision() {
        // Corner block at [1.0, 0.0, 0.0] to [2.0, 1.0, 1.0]
        AxisAlignedBB blockBox = AxisAlignedBB.getBoundingBox(1.0, 0.0, 0.0, 2.0, 1.0, 1.0);
        NECameraCollisionHelper.CollisionBoxProvider boxProvider = (e, bb) -> Collections.singletonList(blockBox);
        NECameraCollisionHelper.RayTraceProvider rayTracer = (start, end) -> {
            if (start.xCoord < 1.0 && end.xCoord >= 1.0) {
                return new MovingObjectPosition(0, 0, 0, 4, Vec3.createVectorHelper(1.0, end.yCoord, end.zCoord));
            }
            return null;
        };

        LocomotionTestEntity entity = new LocomotionTestEntity();
        Vector3d bodyCenter = new Vector3d(0.5, 1.5, 0.5);
        Vector3d prevSafeEye = new Vector3d(0.5, 0.5, 0.5);
        Vector3d desiredEye = new Vector3d(1.5, 0.5, 0.5); // attempting to sweep across x = 1.0 into block

        Vector3d resolved = NECameraCollisionHelper
            .resolveCameraPosition(rayTracer, boxProvider, entity, bodyCenter, desiredEye, prevSafeEye);

        assertTrue(resolved.x < 1.0, "Sweep must stop before entering the voxel surface at X=1.0");
        assertFalse(
            NECameraCollisionHelper.isPointInsideSolid(boxProvider, entity, resolved),
            "Camera sweep destination must not be inside solid");
    }

    @Test
    void cameraPositionRemainsValidAcrossNinetyDegreeTransition() {
        // L-shaped step: ground at Y <= 0, and wall at X >= 5 for Y in [0, 5]
        AxisAlignedBB ground = AxisAlignedBB.getBoundingBox(-10.0, -10.0, -10.0, 10.0, 0.0, 10.0);
        AxisAlignedBB wall = AxisAlignedBB.getBoundingBox(5.0, 0.0, -10.0, 10.0, 10.0, 10.0);
        List<AxisAlignedBB> terrain = Arrays.asList(ground, wall);
        NECameraCollisionHelper.CollisionBoxProvider boxProvider = (e, bb) -> {
            List<AxisAlignedBB> list = new java.util.ArrayList<>();
            for (AxisAlignedBB b : terrain) {
                if (bb.intersectsWith(b)) {
                    list.add(b);
                }
            }
            return list;
        };

        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;

        // Step through 10 points traversing from floor approaching wall to climbing wall
        for (int i = 0; i <= 10; i++) {
            double frac = i / 10.0;
            double posX = 4.0 + frac * 0.7; // 4.0 to 4.7
            double posY = frac * 1.5;

            // When approaching wall, localUp transitions toward (-1, 0, 0)
            double ux = -frac;
            double uy = 1.0 - frac;
            Vector3d up = new Vector3d(ux, uy, 0.0).normalize();
            service.setActiveField(UniformGravityField.of(-up.x * 0.08, -up.y * 0.08, 0.0));

            entity.setPosition(posX, posY + 1.62, 0.0);
            NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
                .getPresentationFrame(entity, 0.0F, null, boxProvider);

            Vector3d eye = frame.eyePoint();
            assertFalse(
                NECameraCollisionHelper.isPointInsideSolid(boxProvider, entity, eye),
                "Camera eye point must never enter solid geometry across transition at step " + i);
        }
    }

    @Test
    void cameraPositionRemainsValidAtBottomPole() {
        // Underside of a block platform at Y = 10.0 (blocks exist at Y >= 10.0)
        AxisAlignedBB ceiling = AxisAlignedBB.getBoundingBox(-10.0, 10.0, -10.0, 10.0, 20.0, 10.0);
        NECameraCollisionHelper.CollisionBoxProvider boxProvider = (e, bb) -> Collections.singletonList(ceiling);

        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;
        // Gravity pulls UP into the ceiling (+Y), localUp points DOWN (-Y)
        service.setActiveField(UniformGravityField.of(0.0, 0.08, 0.0));
        // Feet touching ceiling at Y = 10.0; bounding box [8.2, 10.0], entity.posY = 8.2 + 1.62 = 9.82
        entity.setPosition(0.0, 9.82, 0.0);
        entity.boundingBox.setBounds(-0.3, 8.2, -0.3, 0.3, 10.0, 0.3);

        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F, null, boxProvider);

        Vector3d eye = frame.eyePoint();
        assertEquals(10.0 - 1.62, eye.y, 1e-3, "Eye point must hang 1.62 blocks below ceiling");
        assertFalse(
            NECameraCollisionHelper.isPointInsideSolid(boxProvider, entity, eye),
            "Camera eye must not penetrate ceiling blocks");
    }

    @Test
    void cameraCollisionCorrectionDoesNotAccumulate() {
        AxisAlignedBB blockBox = AxisAlignedBB.getBoundingBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        NECameraCollisionHelper.CollisionBoxProvider boxProvider = (e, bb) -> Collections.singletonList(blockBox);

        LocomotionTestEntity entity = new LocomotionTestEntity();
        Vector3d bodyCenter = new Vector3d(0.5, 1.5, 0.5);
        Vector3d desiredEye = new Vector3d(0.5, 0.95, 0.5); // near top face

        Vector3d firstResolved = NECameraCollisionHelper
            .resolveCameraPosition(null, boxProvider, entity, bodyCenter, desiredEye, null);

        Vector3d current = firstResolved;
        for (int frame = 0; frame < 100; frame++) {
            current = NECameraCollisionHelper
                .resolveCameraPosition(null, boxProvider, entity, bodyCenter, desiredEye, current);
        }

        assertEquals(firstResolved.x, current.x, 1e-9, "X position must not accumulate or drift over 100 frames");
        assertEquals(firstResolved.y, current.y, 1e-9, "Y position must not accumulate or drift over 100 frames");
        assertEquals(firstResolved.z, current.z, 1e-9, "Z position must not accumulate or drift over 100 frames");
    }

    @Test
    void standardGravityUsesVanillaEyePath() {
        service.resetToDefault();
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;
        entity.setPosition(10.0, 65.62, -20.0);

        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);

        Vector3d entityPos = new Vector3d(10.0, 65.62, -20.0);
        Vector3d eyeDelta = entityPos.subtract(frame.eyePoint());

        assertEquals(0.0, eyeDelta.x, 1e-6, "Eye delta X must be 0 in standard gravity");
        assertEquals(0.0, eyeDelta.y, 1e-6, "Eye delta Y must be 0 in standard gravity");
        assertEquals(0.0, eyeDelta.z, 1e-6, "Eye delta Z must be 0 in standard gravity");
    }

    @Test
    void runtimeInvariantCameraNeverInsideSolidOnCircumnavigation() {
        // Spherical shell test body represented by 6 bounding boxes enclosing radius 10
        AxisAlignedBB topBlock = AxisAlignedBB.getBoundingBox(-2.0, 9.0, -2.0, 2.0, 10.0, 2.0);
        AxisAlignedBB sideBlock = AxisAlignedBB.getBoundingBox(9.0, -2.0, -2.0, 10.0, 2.0, 2.0);
        AxisAlignedBB bottomBlock = AxisAlignedBB.getBoundingBox(-2.0, -10.0, -2.0, 2.0, -9.0, 2.0);
        AxisAlignedBB oppBlock = AxisAlignedBB.getBoundingBox(-10.0, -2.0, -2.0, -9.0, 2.0, 2.0);
        List<AxisAlignedBB> blocks = Arrays.asList(topBlock, sideBlock, bottomBlock, oppBlock);
        NECameraCollisionHelper.CollisionBoxProvider boxProvider = (e, bb) -> blocks;

        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;

        // Loop 72 steps (5 degrees each) around the body
        for (int step = 0; step < 72; step++) {
            double angle = step * (2.0 * Math.PI / 72.0);
            double rx = Math.sin(angle);
            double ry = Math.cos(angle);
            Vector3d localUp = new Vector3d(rx, ry, 0.0);
            service.setActiveField(UniformGravityField.of(-rx * 0.08, -ry * 0.08, 0.0));

            // Place entity on the surface of the radius-10 body with feet touching radius 10
            double offsetDist = 10.0 + 0.3 * Math.abs(rx) + 0.9 * Math.abs(ry);
            double posX = rx * offsetDist;
            double posY = ry * offsetDist + 0.72;
            entity.setPosition(posX, posY, 0.0);

            NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
                .getPresentationFrame(entity, 0.0F, null, boxProvider);

            Vector3d eye = frame.eyePoint();
            assertFalse(
                NECameraCollisionHelper.isPointInsideSolid(boxProvider, entity, eye),
                "Runtime invariant violated: camera must never be inside solid geometry at angle "
                    + Math.toDegrees(angle));
        }
    }

    @Test
    void localHeadForwardMatchesCanonicalLook() {
        // On a vertical wall (e.g. East wall, localUp = (-1, 0, 0))
        Vector3d localUp = new Vector3d(-1.0, 0.0, 0.0);
        Vector3d tangentForward = new Vector3d(0.0, 1.0, 0.0); // Walking up the wall
        Vector3d tangentRight = localUp.cross(tangentForward)
            .normalize(); // (0, 0, -1) South

        // Looking straight ahead along tangentForward
        Vector3d lookVector = tangentForward;

        float[] angles = NEEntityRenderHandler
            .computeLocalHeadAngles(lookVector, localUp, tangentForward, tangentRight);
        assertEquals(
            0.0F,
            angles[0],
            1e-4,
            "Head yaw must be 0 when looking straight forward along local body heading");
        assertEquals(
            0.0F,
            angles[1],
            1e-4,
            "Head pitch must be 0 when looking straight forward along local body heading");
    }

    @Test
    void wallHeadYawIsRelativeToLocalBodyFrame() {
        Vector3d localUp = new Vector3d(-1.0, 0.0, 0.0);
        Vector3d tangentForward = new Vector3d(0.0, 1.0, 0.0);
        Vector3d tangentRight = localUp.cross(tangentForward)
            .normalize(); // (0, 0, -1)

        // Look 30 degrees right: cos(30)*fwd + sin(30)*right
        double rad30 = Math.toRadians(30.0);
        Vector3d lookRight = tangentForward.multiply(Math.cos(rad30))
            .add(tangentRight.multiply(Math.sin(rad30)))
            .normalize();
        float[] anglesRight = NEEntityRenderHandler
            .computeLocalHeadAngles(lookRight, localUp, tangentForward, tangentRight);
        assertEquals(
            30.0F,
            anglesRight[0],
            1e-3,
            "Head yaw must turn +30 degrees when looking 30 deg right relative to body");
        assertEquals(0.0F, anglesRight[1], 1e-3, "Head pitch must remain 0 when looking horizontally");

        // Look 30 degrees left: cos(30)*fwd - sin(30)*right
        Vector3d lookLeft = tangentForward.multiply(Math.cos(rad30))
            .subtract(tangentRight.multiply(Math.sin(rad30)))
            .normalize();
        float[] anglesLeft = NEEntityRenderHandler
            .computeLocalHeadAngles(lookLeft, localUp, tangentForward, tangentRight);
        assertEquals(
            -30.0F,
            anglesLeft[0],
            1e-3,
            "Head yaw must turn -30 degrees when looking 30 deg left relative to body");
        assertEquals(0.0F, anglesLeft[1], 1e-3, "Head pitch must remain 0 when looking horizontally");

        // Look 45 degrees up (toward localUp)
        double rad45 = Math.toRadians(45.0);
        Vector3d lookUp = tangentForward.multiply(Math.cos(rad45))
            .add(localUp.multiply(Math.sin(rad45)))
            .normalize();
        float[] anglesUp = NEEntityRenderHandler.computeLocalHeadAngles(lookUp, localUp, tangentForward, tangentRight);
        assertEquals(0.0F, anglesUp[0], 1e-3, "Head yaw must be 0 when looking up");
        assertEquals(-45.0F, anglesUp[1], 1e-3, "Head pitch must be -45 degrees when looking up along localUp");

        // Look 45 degrees down (toward localDown / support surface)
        Vector3d lookDown = tangentForward.multiply(Math.cos(rad45))
            .subtract(localUp.multiply(Math.sin(rad45)))
            .normalize();
        float[] anglesDown = NEEntityRenderHandler
            .computeLocalHeadAngles(lookDown, localUp, tangentForward, tangentRight);
        assertEquals(0.0F, anglesDown[0], 1e-3, "Head yaw must be 0 when looking down");
        assertEquals(
            45.0F,
            anglesDown[1],
            1e-3,
            "Head pitch must be +45 degrees when looking down toward support surface");
    }

    @Test
    void wallHeadDoesNotLockNinetyDegreesRight() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0)); // Gravity towards +X (East wall, localUp is -X)
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;
        entity.setPosition(10.0, 64.0, 0.0);
        entity.rotationYaw = 0.0F; // Global yaw facing South
        entity.rotationPitch = 0.0F;

        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);
        float[] angles = NEEntityRenderHandler.computeLocalHeadAngles(
            frame.computeLookVector(),
            frame.renderedUp(),
            frame.renderedForward(),
            frame.renderedRight());

        // Head yaw must be 0 when looking straight forward along local body
        assertTrue(
            Math.abs(angles[0]) < 1e-4,
            "Head yaw must NOT be locked 90 or 75 degrees right on a side wall; got: " + angles[0]);
        assertTrue(Math.abs(angles[1]) < 1e-4, "Head pitch must be 0 when looking forward; got: " + angles[1]);
    }

    @Test
    void bottomHeadOrientationRemainsContinuous() {
        // Traversal across the bottom pole (ceiling, localUp = (0, -1, 0))
        service.resetToDefault();
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;

        double[] angles = { -15.0, -10.0, -5.0, -1.0, 0.0, 1.0, 5.0, 10.0, 15.0 };
        float prevYaw = 0.0F;
        for (int i = 0; i < angles.length; i++) {
            double rad = Math.toRadians(angles[i]);
            double rx = Math.sin(rad);
            double ry = -Math.cos(rad);
            service.setActiveField(UniformGravityField.of(-rx * 0.08, -ry * 0.08, 0.0));
            entity.setPosition(rx * 10.0, ry * 10.0, 0.0);

            NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
                .getPresentationFrame(entity, 0.0F);
            float[] headAngles = NEEntityRenderHandler.computeLocalHeadAngles(
                frame.computeLookVector(),
                frame.renderedUp(),
                frame.renderedForward(),
                frame.renderedRight());

            assertTrue(Math.abs(headAngles[0]) < 75.0F, "Head yaw must remain within natural limits at bottom pole");
            if (i > 0) {
                float deltaYaw = Math.abs(headAngles[0] - prevYaw);
                assertTrue(deltaYaw < 30.0F, "Head yaw must change continuously across bottom pole traversal");
            }
            prevYaw = headAngles[0];
        }
    }

    @Test
    void arrowInitialVelocityMatchesCameraLook() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0)); // Side wall (East)
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;
        entity.setPosition(10.0, 64.0, 0.0);
        entity.rotationPitch = -20.0F; // Aiming 20 degrees up along local frame
        entity.prevRotationPitch = -20.0F;

        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 1.0F);
        Vector3d look = frame.computeLookVector();

        EntityArrow arrow = new EntityArrow(null, 0.0, 0.0, 0.0);
        NEProjectileHelper.alignArrowLaunch(arrow, entity, 2.0F);

        Vector3d arrowVel = new Vector3d(arrow.motionX, arrow.motionY, arrow.motionZ);
        assertTrue(arrowVel.length() > 1.0, "Arrow initial speed must be non-zero");

        Vector3d normVel = arrowVel.normalize();
        double dot = normVel.dot(look);
        assertEquals(
            1.0,
            dot,
            1e-5,
            "Initial arrow velocity must be directly aligned with canonical 3D look vector (dot product = 1.0)");
    }

    @Test
    void arrowLaunchDirectionWorksOnTopWallBottomAndOppositeWall() {
        Vector3d[] ups = { new Vector3d(0.0, 1.0, 0.0), // Top
            new Vector3d(1.0, 0.0, 0.0), // West wall
            new Vector3d(0.0, -1.0, 0.0), // Bottom / Ceiling
            new Vector3d(-1.0, 0.0, 0.0) // East wall
        };

        for (Vector3d up : ups) {
            Vector3d g = up.negate()
                .multiply(0.08);
            service.setActiveField(UniformGravityField.of(g.x, g.y, g.z));

            LocomotionTestEntity entity = new LocomotionTestEntity();
            entity.yOffset = 1.62F;
            Vector3d pos = up.multiply(10.0);
            entity.setPosition(pos.x, pos.y, pos.z);
            entity.rotationPitch = -15.0F; // Looking 15 degrees up
            entity.prevRotationPitch = -15.0F;

            if (up.dot(Vector3d.UP) > 0.99999) {
                // Standard top surface: shouldApplyCustomLaunch is false (vanilla bypass)
                assertFalse(NEProjectileHelper.shouldApplyCustomLaunch(entity));
            } else {
                assertTrue(NEProjectileHelper.shouldApplyCustomLaunch(entity));
                NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
                    .getPresentationFrame(entity, 1.0F);
                Vector3d look = frame.computeLookVector();

                EntityArrow arrow = new EntityArrow(null, 0.0, 0.0, 0.0);
                NEProjectileHelper.alignArrowLaunch(arrow, entity, 3.0F);

                Vector3d normVel = new Vector3d(arrow.motionX, arrow.motionY, arrow.motionZ).normalize();
                assertEquals(
                    1.0,
                    normVel.dot(look),
                    1e-5,
                    "Arrow launch velocity must match canonical 3D look on up " + up);
            }
        }
    }

    @Test
    void arrowSpawnPointUsesLocalEyeFrame() {
        // On East wall: localUp is (-1, 0, 0)
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;
        entity.setPosition(10.0, 64.0, 0.0);

        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);
        Vector3d eye = frame.eyePoint();
        Vector3d look = frame.computeLookVector();

        Vector3d spawn = NEProjectileHelper.computeLaunchPosition(entity, look);

        assertEquals(eye.x + look.x * NEProjectileHelper.ARROW_FORWARD_OFFSET, spawn.x, 1e-5);
        assertEquals(eye.y + look.y * NEProjectileHelper.ARROW_FORWARD_OFFSET, spawn.y, 1e-5);
        assertEquals(eye.z + look.z * NEProjectileHelper.ARROW_FORWARD_OFFSET, spawn.z, 1e-5);

        // On a wall where localUp is (-1, 0, 0), the eye is positioned along X:
        // entity AABB center is 10.0, support surface at boundary is 10.3, eye is 10.3 - 1.62 = 8.68
        assertEquals(8.68, eye.x, 1e-4, "Physical eye X on vertical wall must extend outward along localUp");
    }

    @Test
    void arrowDoesNotSpawnInsideSupportSurface() {
        // Standing on East wall (support surface at X = 10.0, standing on X = 10.0 face)
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;
        entity.setPosition(10.0, 64.0, 0.0);

        // Look straight down at the support wall: localPitch = -90 (looking down toward nadir)
        entity.rotationPitch = 89.0F; // Looking down at feet/wall

        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);
        Vector3d look = frame.computeLookVector();
        Vector3d spawn = NEProjectileHelper.computeLaunchPosition(entity, look);

        // Support surface is at X >= 10.0. The arrow must spawn strictly in free space (X < 10.0)
        assertTrue(spawn.x < 10.0, "Arrow spawn point must NOT penetrate the support surface; spawn.x = " + spawn.x);
        assertTrue(spawn.x < 9.0, "Arrow must remain well clear of the wall support surface");
    }

    @Test
    void projectileLaunchDoesNotUseGlobalYawAsCanonicalTruth() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;
        entity.setPosition(10.0, 64.0, 0.0);
        entity.rotationYaw = 0.0F;
        entity.rotationPitch = 0.0F;

        NEPlayerPresentationFrame frame1 = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, 0.0F);
        Vector3d look1 = frame1.computeLookVector();

        EntityArrow arrow1 = new EntityArrow(null, 0.0, 0.0, 0.0);
        NEProjectileHelper.alignArrowLaunch(arrow1, entity, 2.0F);

        Vector3d v1 = new Vector3d(arrow1.motionX, arrow1.motionY, arrow1.motionZ).normalize();
        assertEquals(1.0, v1.dot(look1), 1e-5);
    }

    @Test
    void projectileReceivesGravityExactlyOnceAfterLaunch() {
        service.setActiveField(UniformGravityField.of(0.05, 0.0, 0.0));

        EntityArrow arrow = new EntityArrow(null, 0.0, 64.0, 0.0);
        arrow.motionX = 0.0;
        arrow.motionY = 0.0;
        arrow.motionZ = 1.0;

        // 1. NEGravityService applies gravity delta for 1 tick: localAcc - (0, -0.05, 0)
        service.applyGravity(arrow);

        // 2. Simulated vanilla arrow onUpdate applies hardcoded downward acceleration: motionY -= 0.05
        arrow.motionY -= 0.05;

        // Net acceleration over 1 tick must be exactly (+0.05, 0.0, 0.0)
        assertEquals(0.05, arrow.motionX, 1e-6, "Arrow motionX must receive exactly 1 tick of local gravity (+0.05)");
        assertEquals(
            0.0,
            arrow.motionY,
            1e-6,
            "Arrow motionY must have vanilla gravity completely cancelled (net 0.0)");
        assertEquals(1.0, arrow.motionZ, 1e-6, "Arrow motionZ must remain unaffected by perpendicular gravity");
    }

    @Test
    void standardGravityUsesVanillaProjectileLaunchPath() {
        service.resetToDefault();
        LocomotionTestEntity entity = new LocomotionTestEntity();
        entity.yOffset = 1.62F;
        entity.setPosition(0.0, 64.0, 0.0);
        entity.rotationYaw = 45.0F;
        entity.rotationPitch = -30.0F;

        assertFalse(
            NEProjectileHelper.shouldApplyCustomLaunch(entity),
            "shouldApplyCustomLaunch must return false under standard downward gravity");

        EntityArrow arrow = new EntityArrow(null, 0.0, 0.0, 0.0);
        arrow.motionX = 123.456;
        arrow.motionY = 78.9;
        arrow.motionZ = -42.0;

        NEProjectileHelper.alignArrowLaunch(arrow, entity, 2.0F);

        assertEquals(123.456, arrow.motionX, 1e-6, "Arrow motionX must be untouched under standard gravity bypass");
        assertEquals(78.9, arrow.motionY, 1e-6, "Arrow motionY must be untouched under standard gravity bypass");
        assertEquals(-42.0, arrow.motionZ, 1e-6, "Arrow motionZ must be untouched under standard gravity bypass");
    }
}
