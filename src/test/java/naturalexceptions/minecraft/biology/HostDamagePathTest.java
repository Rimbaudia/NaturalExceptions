package naturalexceptions.minecraft.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.DamageSource;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.biology.PhysicalInteraction;

final class HostDamagePathTest {

    @Test
    void damageSourcesMapToCoarsePhysicalMeaning() {
        assertEquals(HostDamageMapping.Category.FALL, HostDamageMapping.categorize(DamageSource.fall));
        assertEquals(HostDamageMapping.LEFT_LEG, HostDamageMapping.region(HostDamageMapping.Category.FALL));
        assertEquals(HostDamageMapping.Category.THERMAL, HostDamageMapping.categorize(DamageSource.lava));
        assertEquals(HostDamageMapping.Category.THERMAL, HostDamageMapping.categorize(DamageSource.onFire));
        assertEquals(
            PhysicalInteraction.Type.THERMAL,
            HostDamageMapping.interactionType(HostDamageMapping.Category.THERMAL));
        assertEquals(HostDamageMapping.Category.PHYSICAL_HIT, HostDamageMapping.categorize(DamageSource.cactus));
        assertEquals(HostDamageMapping.Category.PHYSICAL_HIT, HostDamageMapping.categorize(DamageSource.anvil));
        // Never disguised as a blow to the torso:
        assertEquals(HostDamageMapping.Category.UNMODELLED, HostDamageMapping.categorize(DamageSource.drown));
        assertEquals(HostDamageMapping.Category.UNMODELLED, HostDamageMapping.categorize(DamageSource.starve));
        assertEquals(HostDamageMapping.Category.UNMODELLED, HostDamageMapping.categorize(DamageSource.magic));
        assertEquals(HostDamageMapping.Category.UNMODELLED, HostDamageMapping.categorize(DamageSource.wither));
        assertEquals(HostDamageMapping.Category.UNMODELLED, HostDamageMapping.categorize(DamageSource.outOfWorld));
        assertEquals(HostDamageMapping.Category.UNMODELLED, HostDamageMapping.categorize(DamageSource.inWall));
    }

    @Test
    void armorIsResolvedOncePerHit() {
        Object wearer = new Object();
        ArmorPathGate.markResolved(wearer, 100L);
        assertTrue(ArmorPathGate.consume(wearer, 100L), "host armor step skips the P10-resolved hit");
        assertFalse(ArmorPathGate.consume(wearer, 100L), "and only that hit");
        ArmorPathGate.markResolved(wearer, 100L);
        assertFalse(ArmorPathGate.consume(wearer, 101L), "a stale mark never suppresses a later hit's armor");
        assertFalse(ArmorPathGate.consume(new Object(), 100L), "unmarked hits use host armor normally");
    }

    @Test
    void healthSyncFollowsBiologyWithoutErasingUnmodelledDamage() {
        // Drowning took 4 hearts that biology does not model; condition unchanged: damage stays.
        assertEquals(12.0F, BiologicalEvents.syncedHealth(12.0F, 20.0F, 20.0F), 1.0e-6);
        // Bleeding lowers the condition by 2: health follows as a delta.
        assertEquals(10.0F, BiologicalEvents.syncedHealth(12.0F, 20.0F, 18.0F), 1.0e-6);
        // Health never exceeds what the body's condition allows.
        assertEquals(15.0F, BiologicalEvents.syncedHealth(19.0F, 15.0F, 15.0F), 1.0e-6);
        assertEquals(0.0F, BiologicalEvents.syncedHealth(12.0F, 20.0F, 0.0F), 1.0e-6);
    }

    @Test
    void smelterSmokeScalesWithFireDistanceAndOpenAir() {
        double near = SmelterSmoke.gramsPerSecond(200_000.0, 1.0, false);
        double far = SmelterSmoke.gramsPerSecond(200_000.0, 4.0, false);
        double open = SmelterSmoke.gramsPerSecond(200_000.0, 1.0, true);
        assertTrue(near > far && far > 0.0);
        assertTrue(open < near);
        assertEquals(0.0, SmelterSmoke.gramsPerSecond(0.0, 1.0, false), 0.0, "no fire, no smoke");
        assertEquals(0.0, SmelterSmoke.gramsPerSecond(200_000.0, 20.0, false), 0.0);
    }
}
