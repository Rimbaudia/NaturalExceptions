package naturalexceptions.minecraft.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.item.Item;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.anomaly.RegionKey;
import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.event.EventRecord;
import naturalexceptions.core.event.EventRegion;
import naturalexceptions.core.event.P8WorldState;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;
import naturalexceptions.minecraft.worldgen.SkyCacheLoot;

/**
 * The hats and shoes fill the slots that had nothing to wear: each has its icon; the Sabatons are a vault find; the
 * Slippers keep monsters off until they are near; the Hood feels a Star Rain or a Blood Night a day ahead.
 */
final class HatAndShoeTest {

    private static final Treasure[] HATS = { Treasure.LAMPLIGHTERS_CAP, Treasure.WEATHERWISE_HOOD,
        Treasure.DIVERS_BELL_HELM };
    private static final Treasure[] SHOES = { Treasure.GALE_STRIDERS, Treasure.SKYSTONE_SABATONS,
        Treasure.FEATHERSTEP_SLIPPERS, Treasure.CLOUD_TREADS };

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void theHatAndShoeSlotsHaveSomethingToWear() {
        for (Treasure hat : HATS) assertEquals(GearKind.HAT, hat.kind(), hat + " is a hat");
        for (Treasure shoe : SHOES) assertEquals(GearKind.SHOE, shoe.kind(), shoe + " is a shoe");
        for (Treasure treasure : Treasure.values()) {
            assertNotNull(NEItems.TREASURES.get(treasure), treasure + " is registered");
            assertNotNull(
                HatAndShoeTest.class
                    .getResource("/assets/naturalexceptions/textures/items/" + treasure.registryName() + ".png"),
                treasure + " has its icon");
        }
        assertNotNull(NEBlocks.lamplight, "the Lamplighter's Cap has its light");
    }

    @Test
    void theSabatonsAreFoundOnlyInAVault() {
        Item sabatons = NEItems.TREASURES.get(Treasure.SKYSTONE_SABATONS);
        assertEquals(Treasure.Source.VAULT, Treasure.SKYSTONE_SABATONS.source());
        assertFalse(
            SkyCacheLoot.treasures()
                .contains(sabatons),
            "not in an ordinary chest");
        assertTrue(
            SkyCacheLoot.vaultTreasures()
                .contains(sabatons),
            "but in a vault");
        assertTrue(
            SkyCacheLoot.vaultTreasures()
                .containsAll(SkyCacheLoot.treasures()),
            "a vault still holds everything a chest can");
        for (Treasure treasure : Treasure.values()) if (treasure.relic()) assertFalse(
            SkyCacheLoot.vaultTreasures()
                .contains(NEItems.TREASURES.get(treasure)),
            "no relic in a vault");
    }

    @Test
    void featherstepSlippersKeepMonstersOffUntilTheyAreNear() {
        assertTrue(HatAndShoeEffects.notices(30.0, 0, false), "without them, as ever");
        assertFalse(HatAndShoeEffects.notices(12.0, 1, false));
        assertTrue(HatAndShoeEffects.notices(9.0, 1, false));
        assertFalse(HatAndShoeEffects.notices(9.0, 2, false), "a pair, nearer still");
        assertTrue(HatAndShoeEffects.notices(5.0, 2, false));
        assertTrue(HatAndShoeEffects.notices(30.0, 2, true), "but a monster you struck knows where you are");
    }

    @Test
    void theWeatherwiseHoodFeelsABloodNightOverYouADayAhead() {
        P8WorldState p8 = new P8WorldState(4242L, 1, 0L);
        long day = NaturalCalendar.TICKS_PER_DAY;
        RegionKey here = RegionKey.fromBlock(100, 100);
        RegionKey there = RegionKey.fromBlock(100 + 4 * RegionKey.SIZE_BLOCKS, 100);
        assertTrue(p8.scheduleBloodNight(EventRegion.of((int) here.x(), (int) here.z()), 1, 10 * day, 9000L, day));
        assertTrue(p8.scheduleBloodNight(EventRegion.of((int) there.x(), (int) there.z()), 2, 10 * day, 9000L, day));
        List<EventRecord> events = p8.events()
            .snapshot()
            .events();
        assertEquals(
            1,
            HatAndShoeEffects.coming(events, 9 * day + 500, 100, 100)
                .size(),
            "only the one over you");
        assertTrue(
            HatAndShoeEffects.coming(events, 8 * day, 100, 100)
                .isEmpty(),
            "not two days early");
        assertTrue(
            HatAndShoeEffects.coming(events, 10 * day + 10, 100, 100)
                .isEmpty(),
            "nor once it has begun");
    }
}
