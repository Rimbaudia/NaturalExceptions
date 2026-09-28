package naturalexceptions.minecraft.biology;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import naturalexceptions.core.biology.BiologicalInteractionResolver;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BodyPlanRegistry;
import naturalexceptions.core.biology.BuiltinBodyPlans;
import naturalexceptions.core.biology.PhysicalInteraction;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.id.ResourceId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class VanillaArmorPhysicalAdapterTest {
    @BeforeAll static void bootstrap() { try { Bootstrap.func_151354_b(); } catch (Throwable ignored) { } }

    @Test void ordinaryIronChestplateInterceptsStressAndPersistsP5ComponentDamage() {
        ItemStack chestplate = new ItemStack(Items.iron_chestplate);
        VanillaArmorAdapter.EquippedArmor armor = VanillaArmorAdapter.captureSlot(chestplate, 3, "wearer-1");
        assertNotNull(armor);
        BiologicalState body = new BiologicalState(BodyPlanRegistry.standard().require(BuiltinBodyPlans.HUMAN_ID), 0L);
        ResourceId torso = ResourceId.of("naturalexceptions", "torso");
        PhysicalInteraction cut = new PhysicalInteraction(PhysicalInteraction.Type.CUT, torso, 40.0, 0.4, 0.9, 20.0);

        BiologicalInteractionResolver.apply(body, cut, 1L, java.util.Collections.singletonList(armor.layer()));
        armor.commit(null);

        ComponentInstance restored = VanillaArmorAdapter.captureSlot(chestplate, 3, "wearer-1").layer().component();
        assertTrue(restored.damage().isDamaged(), "the ordinary armor stack must retain its P5 component stress");
        assertTrue(body.injuries().isEmpty(), "a weak cut should be stopped by an intact plate");

        BiologicalState unarmored = new BiologicalState(BodyPlanRegistry.standard().require(BuiltinBodyPlans.HUMAN_ID), 0L);
        BiologicalInteractionResolver.apply(unarmored, cut, 1L);
        assertTrue(!unarmored.injuries().isEmpty(), "the same cut should wound unarmored tissue");

        ItemStack damagedChestplate = new ItemStack(Items.iron_chestplate);
        VanillaArmorAdapter.EquippedArmor weakPlate = VanillaArmorAdapter.captureSlot(damagedChestplate, 3, "wearer-2");
        BiologicalState strongHit = new BiologicalState(BodyPlanRegistry.standard().require(BuiltinBodyPlans.HUMAN_ID), 0L);
        BiologicalInteractionResolver.apply(strongHit,
            new PhysicalInteraction(PhysicalInteraction.Type.CUT, torso, 600.0, 0.4, 0.9, 20.0), 1L,
            java.util.Collections.singletonList(weakPlate.layer()));
        assertTrue(weakPlate.layer().component().damage().isFractured());
        assertTrue(!strongHit.injuries().isEmpty(), "a penetrating attack can pass a fractured plate into tissue");
    }
}
