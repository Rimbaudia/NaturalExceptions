package naturalexceptions.minecraft.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.worldgen.dungeon.TrapLinks;
import naturalexceptions.minecraft.block.TileEntityGaleVent;
import naturalexceptions.minecraft.gear.RelicRules;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;
import naturalexceptions.minecraft.worldgen.SkyCacheLoot;

/** The Sky Warden's fight and drops, the relics' numbers, and the trap devices' reach and rhythm. */
final class WardenAndRelicsTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static Set<Item> relics() {
        Set<Item> relics = new HashSet<>();
        for (Treasure treasure : Treasure.values()) if (treasure.relic()) relics.add(NEItems.TREASURES.get(treasure));
        relics.add(NEItems.galeMaul);
        return relics;
    }

    @Test
    void aMiniBossNotATank() {
        assertEquals(70.0F, WardenRules.MAX_HEALTH);
        assertEquals(WardenRules.Phase.BOUND, WardenRules.phase(70.0F, 70.0F));
        assertEquals(WardenRules.Phase.BOUND, WardenRules.phase(35.5F, 70.0F));
        assertEquals(WardenRules.Phase.UNBOUND, WardenRules.phase(35.0F, 70.0F));
        assertTrue(WardenRules.SLAM_WINDUP >= 20, "the slam is telegraphed for at least a second");
        assertTrue(WardenRules.slamStrength(0.0) > WardenRules.slamStrength(4.0));
        assertEquals(0.5, WardenRules.slamStrength(WardenRules.SLAM_RADIUS), 1e-9);
        assertEquals(0.0, WardenRules.slamStrength(WardenRules.SLAM_RADIUS + 0.1));
    }

    @Test
    void everyWardenDropsPlentyAndExactlyOneRelic() {
        Set<Item> relics = relics();
        Random random = new Random(42);
        Map<Treasure, Integer> wearables = new EnumMap<>(Treasure.class);
        int mauls = 0, treasures = 0, rounds = 4000;
        Set<Item> pool = new HashSet<>(SkyCacheLoot.treasures());
        for (int i = 0; i < rounds; i++) {
            List<ItemStack> drops = EntitySkyWarden.drops(random);
            int relicCount = 0, cores = 0, diamonds = 0, lumenite = 0;
            for (ItemStack stack : drops) {
                Item item = stack.getItem();
                if (relics.contains(item)) {
                    relicCount++;
                    if (item == NEItems.galeMaul) mauls++;
                    else for (Treasure t : Treasure.values())
                        if (NEItems.TREASURES.get(t) == item) wearables.merge(t, 1, Integer::sum);
                }
                if (pool.contains(item)) treasures++;
                if (item == NEItems.galeCore) cores += stack.stackSize;
                if (item == Items.diamond) diamonds += stack.stackSize;
                if (item == NEItems.lumeniteShard) lumenite += stack.stackSize;
            }
            assertEquals(1, relicCount, "one relic every time");
            assertTrue(cores >= 1 && cores <= 2 && diamonds >= 1 && lumenite >= 12);
        }
        assertTrue(mauls > rounds * 0.11 && mauls < rounds * 0.19, "the maul is the rarest: " + mauls);
        assertTrue(wearables.get(Treasure.WARDENS_VISOR) > mauls && wearables.get(Treasure.AEGIS_SIGIL) > mauls);
        assertTrue(treasures > rounds * 0.07 && treasures < rounds * 0.13, "about one in ten: " + treasures);
    }

    @Test
    void relicsAreNeverInAChest() {
        Set<Item> relics = relics();
        for (SkyCacheLoot.Tier tier : SkyCacheLoot.Tier.values()) {
            for (long seed = 0; seed < 300; seed++) {
                InventoryBasic chest = new InventoryBasic("chest", false, 27);
                SkyCacheLoot.fill(chest, seed, tier);
                for (int i = 0; i < chest.getSizeInventory(); i++) {
                    ItemStack stack = chest.getStackInSlot(i);
                    assertTrue(stack == null || !relics.contains(stack.getItem()), tier + " chest holds a relic");
                }
            }
        }
    }

    @Test
    void relicNumbers() {
        double[] ahead = RelicRules.dash(0.0, 0.0, 1.0);
        assertEquals(RelicRules.DASH_SPEED, ahead[2], 1e-9);
        assertEquals(RelicRules.DASH_MIN_LIFT, ahead[1], 1e-9, "a flat dash still lifts off the ground");
        double[] down = RelicRules.dash(0.6, -0.8, 0.0);
        assertTrue(down[1] > 0.0 && down[0] > 1.0, "looking down still dashes forward, not into the floor");
        assertTrue(RelicRules.ready(Long.MIN_VALUE, 0L, RelicRules.DASH_COOLDOWN_TICKS));
        assertTrue(!RelicRules.ready(100L, 120L, RelicRules.DASH_COOLDOWN_TICKS));
        assertTrue(RelicRules.ready(100L, 100L + RelicRules.AEGIS_COOLDOWN_TICKS, RelicRules.AEGIS_COOLDOWN_TICKS));
        assertEquals(0.0, RelicRules.maulCharge(RelicRules.MAUL_MIN_CHARGE - 1));
        assertEquals(1.0, RelicRules.maulCharge(RelicRules.MAUL_FULL_CHARGE), 1e-9);
        assertEquals(1.0, RelicRules.maulCharge(500), 1e-9);
        double last = 0.0;
        for (int t = RelicRules.MAUL_MIN_CHARGE; t <= RelicRules.MAUL_FULL_CHARGE; t++) {
            assertTrue(RelicRules.maulCharge(t) >= last, "charge only builds");
            last = RelicRules.maulCharge(t);
        }
    }

    @Test
    void triggersReachNearbyDevicesAndVentsKeepABeat() {
        assertTrue(TrapLinks.reaches(0, 64, 0, 5, 66, -5));
        assertTrue(!TrapLinks.reaches(0, 64, 0, 6, 64, 0));
        assertTrue(!TrapLinks.reaches(0, 64, 0, 0, 68, 0));
        int on = 0;
        for (long t = 0; t < TileEntityGaleVent.CYCLE_TICKS; t++) if (TileEntityGaleVent.gusting(t, 3, 70, -9)) on++;
        assertEquals(TileEntityGaleVent.GUST_TICKS, on, "half the beat on, half off: time to run past");
        Set<Integer> phases = new HashSet<>();
        for (int x = 0; x < 8; x++) phases.add(TileEntityGaleVent.beat(0L, x, 70, 0));
        assertTrue(phases.size() > 3, "vents in a row do not all gust at once");
    }
}
