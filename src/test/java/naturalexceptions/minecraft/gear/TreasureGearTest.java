package naturalexceptions.minecraft.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.biology.BiologicalInjury;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BiologicalTreatment;
import naturalexceptions.core.biology.BuiltinBodyPlans;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

final class TreasureGearTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void eachTreasureIsItsOwnItemWornOnlyInItsSlotWithASprite() {
        for (Treasure treasure : Treasure.values()) {
            assertNotNull(NEItems.TREASURES.get(treasure), treasure + " registered");
            ItemStack stack = new ItemStack(NEItems.TREASURES.get(treasure));
            for (GearSlot slot : GearSlot.values())
                assertEquals(slot.kind() == treasure.kind(), IWearable.fits(slot, stack), treasure + " in " + slot);
            assertTrue(
                new File(
                    "src/main/resources/assets/naturalexceptions/textures/items/" + treasure.registryName() + ".png")
                        .isFile(),
                treasure + " sprite");
        }
    }

    @Test
    void clawsGoOnBothHandsAndBandsOnManyFingers() {
        PlayerGear gear = new PlayerGear();
        ItemStack claws = new ItemStack(NEItems.TREASURES.get(Treasure.CLIMBING_CLAWS));
        assertTrue(gear.wear(claws));
        assertTrue(gear.wear(claws), "second hand");
        assertFalse(gear.wear(claws), "only two hands");
        ItemStack band = new ItemStack(NEItems.TREASURES.get(Treasure.BAND_OF_MENDING));
        for (int i = 0; i < 10; i++) assertTrue(gear.wear(band), "finger " + i);
        assertFalse(gear.wear(band), "ten fingers");
    }

    @Test
    void bandsOfMendingSpeedHealingUpToTwo() {
        assertEquals(1.0, GearMovement.mendingMultiplier(0), 1e-9);
        assertEquals(1.5, GearMovement.mendingMultiplier(1), 1e-9);
        assertEquals(2.0, GearMovement.mendingMultiplier(2), 1e-9);
        assertEquals(2.0, GearMovement.mendingMultiplier(10), 1e-9, "no stacking beyond two");

        BiologicalState body = new BiologicalState(BuiltinBodyPlans.humanLike(), 0L);
        BiologicalInjury burn = new BiologicalInjury(
            DeterministicId.derive("mend", "burn"),
            BiologicalInjury.Kind.BURN,
            ResourceId.of("naturalexceptions", "torso"),
            0.6,
            0.0,
            0L);
        body.addInjury(burn);
        body.treatInjury(burn.id(), BiologicalTreatment.DRESS);
        double plain = body.secondsToHeal(burn);
        body.setRecoveryMultiplier(GearMovement.mendingMultiplier(1));
        assertEquals(plain / 1.5, body.secondsToHeal(burn), plain * 1e-6, "estimate follows the band");
        double before = burn.severity();
        body.advanceTo(20L * 60L);
        BiologicalState reference = new BiologicalState(BuiltinBodyPlans.humanLike(), 0L);
        BiologicalInjury same = new BiologicalInjury(
            DeterministicId.derive("mend", "burn"),
            BiologicalInjury.Kind.BURN,
            ResourceId.of("naturalexceptions", "torso"),
            0.6,
            0.0,
            0L);
        reference.addInjury(same);
        reference.treatInjury(same.id(), BiologicalTreatment.DRESS);
        reference.advanceTo(20L * 60L);
        assertTrue(before - burn.severity() > (before - same.severity()) * 1.4, "heals faster with a band");
    }
}
