package naturalexceptions.minecraft.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.DamageSource;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.biology.PhysicalInteraction;

final class HostDamagePathTest {

    @Test
    void hitsLandWhereTheyStrikeAndReachEveryPart() {
        assertEquals(HostDamageMapping.HEAD, HostDamageMapping.region(0.9, 0.0, 0.5), "a high arrow: the head");
        assertEquals(HostDamageMapping.LEFT_LEG, HostDamageMapping.region(0.2, -0.8, 0.5), "low from the left");
        assertEquals(HostDamageMapping.RIGHT_LEG, HostDamageMapping.region(0.2, 0.8, 0.5), "low from the right");
        assertEquals(HostDamageMapping.LEFT_ARM, HostDamageMapping.region(0.6, -0.9, 0.1), "the middle, from the left");
        assertEquals(
            HostDamageMapping.RIGHT_ARM,
            HostDamageMapping.region(0.6, 0.9, 0.1),
            "the middle, from the right");
        assertEquals(HostDamageMapping.TORSO, HostDamageMapping.region(0.6, 0.0, 0.9), "the middle, head on");
        java.util.Set<naturalexceptions.core.id.ResourceId> reached = new java.util.HashSet<>();
        java.util.Random random = new java.util.Random(7);
        for (int i = 0; i < 400; i++) {
            reached.add(HostDamageMapping.region(Double.NaN, Double.NaN, random.nextDouble()));
            reached
                .add(HostDamageMapping.region(random.nextDouble(), random.nextDouble() * 2 - 1, random.nextDouble()));
        }
        assertEquals(6, reached.size(), "over many hits every part is reached: " + reached);
        java.util.List<naturalexceptions.core.id.ResourceId> legs = java.util.Arrays
            .asList(HostDamageMapping.fallRegions());
        assertTrue(legs.contains(HostDamageMapping.LEFT_LEG) && legs.contains(HostDamageMapping.RIGHT_LEG));
    }

    @Test
    void damageSourcesMapToCoarsePhysicalMeaning() {
        assertEquals(HostDamageMapping.Category.FALL, HostDamageMapping.categorize(DamageSource.fall));
        assertEquals(2, HostDamageMapping.fallRegions().length, "a fall lands on both legs");
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
        assertEquals(0.0, open, 0.0, "open sky carries the smoke away");
        assertEquals(0.0, SmelterSmoke.gramsPerSecond(0.0, 1.0, false), 0.0, "no fire, no smoke");
        assertEquals(0.0, SmelterSmoke.gramsPerSecond(200_000.0, 20.0, false), 0.0);
    }
}
