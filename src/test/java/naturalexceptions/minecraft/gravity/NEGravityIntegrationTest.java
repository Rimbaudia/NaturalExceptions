package naturalexceptions.minecraft.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import naturalexceptions.core.gravity.GravityCameraFrame;
import naturalexceptions.core.gravity.GravitySample;
import naturalexceptions.core.gravity.GravitySurfaceContact;
import naturalexceptions.core.gravity.RadialGravitySource;
import naturalexceptions.core.gravity.UniformGravityField;
import naturalexceptions.core.gravity.Vector3d;
import naturalexceptions.minecraft.registry.NEItems;

final class NEGravityIntegrationTest {

    @BeforeAll
    static void init() {
        NEItems.ensureBootstrapped();
    }

    private static final double EPSILON = 1e-6;

    private static class TestLivingEntity extends EntityLivingBase {

        public TestLivingEntity() {
            super(null);
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
    private NEGravityTickHandler handler;

    @BeforeEach
    void setUp() {
        service = new NEGravityService();
        handler = new NEGravityTickHandler(service);
    }

    @AfterEach
    void tearDown() {
        service.resetToDefault();
    }

    @Test
    void standardDownwardGravityLeavesMotionUnmodified() {
        TestLivingEntity entity = new TestLivingEntity();
        entity.posX = 0.0;
        entity.posY = 64.0;
        entity.posZ = 0.0;
        entity.motionX = 0.1;
        entity.motionY = -0.08;
        entity.motionZ = 0.2;

        service.applyGravity(entity);

        assertEquals(0.1, entity.motionX, EPSILON);
        assertEquals(-0.08, entity.motionY, EPSILON);
        assertEquals(0.2, entity.motionZ, EPSILON);
    }

    @Test
    void sidewaysGravityAcceleratesAlongXAndCancelsDownward() {
        service.setActiveField(UniformGravityField.of(0.08, 0.0, 0.0));

        TestLivingEntity entity = new TestLivingEntity();
        entity.posX = 0.0;
        entity.posY = 64.0;
        entity.posZ = 0.0;
        entity.motionX = 0.0;
        entity.motionY = 0.0;
        entity.motionZ = 0.0;

        service.applyGravity(entity);

        // Motion X receives +0.08 sideways acceleration
        assertEquals(0.08, entity.motionX, EPSILON);
        // Motion Y receives +0.08 counteracting vanilla's hardcoded -0.08 fall
        assertEquals(0.08, entity.motionY, EPSILON);
        assertEquals(0.0, entity.motionZ, EPSILON);
    }

    @Test
    void radialGravityPullsEntityTowardsCenter() {
        Vector3d center = new Vector3d(100.0, 50.0, 100.0);
        service.setActiveField(new RadialGravitySource(center, 0.10, 10.0, 50.0));

        TestLivingEntity entity = new TestLivingEntity();
        entity.posX = 120.0;
        entity.posY = 50.0;
        entity.posZ = 100.0;
        entity.motionX = 0.0;
        entity.motionY = 0.0;
        entity.motionZ = 0.0;

        service.applyGravity(entity);

        assertTrue(entity.motionX < -0.02, "Entity should be pulled towards center (negative X)");
        assertEquals(-0.025, entity.motionX, EPSILON);
        assertEquals(0.08, entity.motionY, EPSILON);
        assertEquals(0.0, entity.motionZ, EPSILON);
    }

    @Test
    void droppedItemAcceleratesAlongSidewaysGravityVector() {
        // Test dropped item (EntityItem) physics under sideways gravity
        service.setActiveField(UniformGravityField.of(0.04, 0.0, 0.0));

        EntityItem item = new EntityItem(null, 10.0, 64.0, 10.0);
        item.motionX = 0.0;
        item.motionY = 0.0;
        item.motionZ = 0.0;

        service.applyGravity(item);

        // Motion X receives +0.04 sideways acceleration
        assertEquals(0.04, item.motionX, EPSILON);
        // Motion Y receives +0.04 counteracting vanilla item's hardcoded -0.04 fall
        assertEquals(0.04, item.motionY, EPSILON);
        assertEquals(0.0, item.motionZ, EPSILON);
    }

    @Test
    void projectileArrowReceivesCoherentGravity() {
        // Test projectile (EntityArrow) physics under Z-directed gravity
        service.setActiveField(UniformGravityField.of(0.0, 0.0, 0.05));

        EntityArrow arrow = new EntityArrow(null, 0.0, 64.0, 0.0);
        arrow.motionX = 0.0;
        arrow.motionY = 0.0;
        arrow.motionZ = 0.0;

        service.applyGravity(arrow);

        assertEquals(0.0, arrow.motionX, EPSILON);
        // Motion Y receives +0.05 counteracting vanilla arrow's hardcoded -0.05 fall
        assertEquals(0.05, arrow.motionY, EPSILON);
        // Motion Z receives +0.05 local gravity acceleration
        assertEquals(0.05, arrow.motionZ, EPSILON);
    }

    @Test
    void eventHandlerAppliesGravityOnLivingUpdateEvent() {
        service.setActiveField(UniformGravityField.of(0.0, 0.0, 0.05));

        TestLivingEntity entity = new TestLivingEntity();
        entity.posX = 10.0;
        entity.posY = 20.0;
        entity.posZ = 30.0;
        entity.motionX = 0.0;
        entity.motionY = 0.0;
        entity.motionZ = 0.0;

        LivingEvent.LivingUpdateEvent event = new LivingEvent.LivingUpdateEvent(entity);
        handler.onLivingUpdate(event);

        assertEquals(0.0, entity.motionX, EPSILON);
        assertEquals(0.08, entity.motionY, EPSILON);
        assertEquals(0.05, entity.motionZ, EPSILON);
    }

    @Test
    void disabledServiceDoesNotModifyMotion() {
        service.setActiveField(UniformGravityField.of(1.0, 1.0, 1.0));
        service.setEnabled(false);

        TestLivingEntity entity = new TestLivingEntity();
        entity.motionX = 0.0;
        entity.motionY = 0.0;
        entity.motionZ = 0.0;

        service.applyGravity(entity);

        assertEquals(0.0, entity.motionX, EPSILON);
        assertEquals(0.0, entity.motionY, EPSILON);
        assertEquals(0.0, entity.motionZ, EPSILON);
    }

    @Test
    void circumnavigationKinematicsLoopAroundRadialBody() {
        // Acceptance scene proof: Entity circumnavigates a closed spherical radial body
        // and returns physically to start without coordinate teleportation or world rotation.
        Vector3d center = new Vector3d(0.0, 100.0, 0.0);
        double radius = 10.0;
        double surfaceG = 0.08;
        RadialGravitySource body = new RadialGravitySource(center, surfaceG, radius, radius + 20.0);
        service.setActiveField(body);

        GravityCameraFrame camera = new GravityCameraFrame(Vector3d.UP, 0.25);

        // Step 1: Top (+Y face, global floor)
        Vector3d posTop = new Vector3d(0.0, 110.1, 0.0);
        GravitySample sampleTop = service.sampleAt(posTop.x, posTop.y, posTop.z);
        assertEquals(new Vector3d(0, -1, 0), sampleTop.localDown());
        assertEquals(new Vector3d(0, 1, 0), sampleTop.localUp());
        GravitySurfaceContact contactTop = GravitySurfaceContact.grounded(Vector3d.UP, 0.1);
        assertTrue(contactTop.isGrounded());
        camera.update(sampleTop.localUp());

        // Step 2: Transition onto side wall (+X face, local down is -X)
        Vector3d posWall = new Vector3d(10.1, 100.0, 0.0);
        GravitySample sampleWall = service.sampleAt(posWall.x, posWall.y, posWall.z);
        assertEquals(new Vector3d(-1, 0, 0), sampleWall.localDown());
        assertEquals(new Vector3d(1, 0, 0), sampleWall.localUp());
        GravitySurfaceContact contactWall = GravitySurfaceContact.grounded(new Vector3d(1, 0, 0), 0.1);
        assertTrue(contactWall.isGrounded());
        camera.update(sampleWall.localUp());
        // Verify camera up has rotated toward wall's localUp without NaN or gimbal lock
        assertTrue(camera.currentUp().x > 0.1);
        assertFalse(Double.isNaN(camera.computeLookDirection(0, 0).y));

        // Step 3: Transition onto bottom ceiling (-Y face, local down is +Y)
        Vector3d posCeiling = new Vector3d(0.0, 89.9, 0.0);
        GravitySample sampleCeiling = service.sampleAt(posCeiling.x, posCeiling.y, posCeiling.z);
        assertEquals(new Vector3d(0, 1, 0), sampleCeiling.localDown());
        assertEquals(new Vector3d(0, -1, 0), sampleCeiling.localUp());
        GravitySurfaceContact contactCeiling = GravitySurfaceContact.grounded(new Vector3d(0, -1, 0), 0.1);
        assertTrue(contactCeiling.isGrounded());
        camera.update(sampleCeiling.localUp());
        assertTrue(camera.currentUp().y < 0.9);

        // Step 4: Transition onto opposite wall (-X face, local down is +X)
        Vector3d posOppWall = new Vector3d(-10.1, 100.0, 0.0);
        GravitySample sampleOppWall = service.sampleAt(posOppWall.x, posOppWall.y, posOppWall.z);
        assertEquals(new Vector3d(1, 0, 0), sampleOppWall.localDown());
        assertEquals(new Vector3d(-1, 0, 0), sampleOppWall.localUp());
        GravitySurfaceContact contactOppWall = GravitySurfaceContact.grounded(new Vector3d(-1, 0, 0), 0.1);
        assertTrue(contactOppWall.isGrounded());
        camera.update(sampleOppWall.localUp());

        // Step 5: Complete circle back to top (+Y face)
        GravitySample sampleReturn = service.sampleAt(posTop.x, posTop.y, posTop.z);
        assertEquals(new Vector3d(0, -1, 0), sampleReturn.localDown());
        assertEquals(new Vector3d(0, 1, 0), sampleReturn.localUp());
        // Verify closed physical circuit with zero world rotation
        assertEquals(0.0, posTop.distance(new Vector3d(0.0, 110.1, 0.0)), EPSILON);
    }
}
