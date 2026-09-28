package naturalexceptions.minecraft.tick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.core.event.StarRainNavigation;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.metallurgy.MaterialQuantity;

final class CommandP11Test {

    @Test
    void developerCommandAdvertisesImpactNowAndDistinguishesFutureFromPhysicalSites() {
        CommandP11 command = new CommandP11();
        assertEquals(2, command.getRequiredPermissionLevel());
        assertTrue(
            command.getCommandUsage(null)
                .contains("impact-now [impact-id]"));
        assertTrue(
            CommandP11.stateHint(StarRainNavigation.State.PENDING)
                .contains("no Fallen Star exists"));
        assertTrue(
            CommandP11.stateHint(StarRainNavigation.State.IN_FLIGHT)
                .contains("no visible descent"));
        assertTrue(
            CommandP11.stateHint(StarRainNavigation.State.IMPACTED)
                .contains("awaits materialization"));
        assertTrue(
            CommandP11.stateHint(StarRainNavigation.State.RECOVERED)
                .contains("left the impact site"));
    }

    @Test
    void pendingFeedbackUsesEffectiveDueTimeWithoutRewritingOriginalDueTime() {
        StarRainImpact impact = new StarRainImpact(
            DeterministicId.derive("parent"),
            DeterministicId.derive("impact"),
            0,
            500L,
            12,
            -8,
            2,
            MaterialQuantity.ofGrams(800),
            false,
            false,
            100L);
        StarRainNavigation.Target target = StarRainNavigation.describe(impact, 90L, 0.0, 0.0, 0L);

        String feedback = CommandP11.formatTarget(target, 90L);

        assertTrue(feedback.contains("PENDING"));
        assertTrue(feedback.contains("impact in 10 ticks"));
        assertEquals(500L, impact.dueTick());
        assertEquals(100L, impact.effectiveDueTick());
    }
}
