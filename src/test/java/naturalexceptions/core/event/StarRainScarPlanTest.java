package naturalexceptions.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class StarRainScarPlanTest {
    @Test
    void severityProducesDeterministicBoundedSurfaceFootprint() {
        assertEquals(1, StarRainScarPlan.offsets(1).size());
        assertEquals(2, StarRainScarPlan.offsets(2).size());
        assertEquals(5, StarRainScarPlan.offsets(3).size());
        assertEquals(StarRainScarPlan.offsets(3), StarRainScarPlan.offsets(3));
        assertEquals(new StarRainScarPlan.Offset(-1, 0), StarRainScarPlan.offsets(3).get(1));
        assertEquals(new StarRainScarPlan.Offset(0, 1), StarRainScarPlan.offsets(3).get(4));
    }
}
