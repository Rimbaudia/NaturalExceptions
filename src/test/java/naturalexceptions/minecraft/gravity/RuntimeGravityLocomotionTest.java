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
}
