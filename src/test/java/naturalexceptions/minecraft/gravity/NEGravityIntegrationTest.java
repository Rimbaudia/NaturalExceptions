package naturalexceptions.minecraft.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import naturalexceptions.core.gravity.RadialGravitySource;
import naturalexceptions.core.gravity.UniformGravityField;
import naturalexceptions.core.gravity.Vector3d;

final class NEGravityIntegrationTest {

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
        // Place entity directly to the east: (120, 50, 100) -> distance 20
        entity.posX = 120.0;
        entity.posY = 50.0;
        entity.posZ = 100.0;
        entity.motionX = 0.0;
        entity.motionY = 0.0;
        entity.motionZ = 0.0;

        service.applyGravity(entity);

        // Pull toward center (100) means negative X motion
        assertTrue(entity.motionX < -0.02, "Entity should be pulled towards center (negative X)");
        // Falloff at dist 20: 0.10 * (10/20)^2 = 0.025 in -X direction
        assertEquals(-0.025, entity.motionX, EPSILON);
        // Y component counteracts vanilla downward gravity
        assertEquals(0.08, entity.motionY, EPSILON);
        assertEquals(0.0, entity.motionZ, EPSILON);
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
}
