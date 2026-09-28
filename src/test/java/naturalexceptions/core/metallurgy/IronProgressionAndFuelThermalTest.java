package naturalexceptions.core.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

public final class IronProgressionAndFuelThermalTest {

    private static final double AMBIENT_TEMP_K = 295.0;

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void fuelPropertiesAndLabelsMatchProgression() {
        ItemStack charcoal = new ItemStack(Items.coal, 1, 1);
        ItemStack coal = new ItemStack(Items.coal, 1, 0);
        ItemStack blazeRod = new ItemStack(Items.blaze_rod, 1);
        ItemStack blazePowder = new ItemStack(Items.blaze_powder, 1);

        assertEquals(1950.0, TileEntityStarterSmelter.getFuelFlameTemp(charcoal), 1e-3);
        assertEquals("Charcoal \u2014 Ordinary Heat", TileEntityStarterSmelter.getFuelHeatLabel(charcoal));

        assertEquals(2010.0, TileEntityStarterSmelter.getFuelFlameTemp(coal), 1e-3);
        assertEquals("Coal \u2014 Strong Heat", TileEntityStarterSmelter.getFuelHeatLabel(coal));

        assertEquals(2600.0, TileEntityStarterSmelter.getFuelFlameTemp(blazeRod), 1e-3);
        assertEquals("Blaze Fuel \u2014 Extreme Heat", TileEntityStarterSmelter.getFuelHeatLabel(blazeRod));

        assertEquals(2600.0, TileEntityStarterSmelter.getFuelFlameTemp(blazePowder), 1e-3);
        assertEquals("Blaze Fuel \u2014 Extreme Heat", TileEntityStarterSmelter.getFuelHeatLabel(blazePowder));
    }

    @Test
    void ironMeltsUnderNaturalDraftWithCharcoal() {
        VesselState vessel = new VesselState();
        MaterialQuantity ironMass = MaterialQuantity.ofKilograms(1.0);
        ActiveThermalState ironThermal = ActiveThermalState
            .atTemperature(AMBIENT_TEMP_K, 0.0, ironMass.toKilograms(), BuiltinMaterials.IRON_PROPERTIES);
        AggregateLayer ironLayer = new AggregateLayer(
            BuiltinMaterials.IRON.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.IRON_COMPOSITION),
            BuiltinMaterials.IRON_COMPOSITION,
            ironMass,
            ironThermal,
            AggregatePhaseType.SOLID_CHARGE);
        vessel.addLayer(ironLayer);

        // Add 50,000 burn ticks of Charcoal at 1950 K (well above 1811 K melting point)
        vessel.addFuel(50000, 1950.0, "Charcoal \u2014 Ordinary Heat");

        // Step vessel under natural draft (airMultiplier = 1.0)
        boolean reachedMelting = false;
        for (int tick = 0; tick < 200; tick++) {
            vessel.step(20, AMBIENT_TEMP_K, 1.0);
            AggregateLayer current = vessel.layers()
                .get(0);
            if (current.temperatureKelvin() >= BuiltinMaterials.IRON_PROPERTIES.meltingPointKelvin()) {
                reachedMelting = true;
                break;
            }
        }

        assertTrue(
            reachedMelting,
            "Iron must reach melting point (1811 K) under natural draft with ordinary charcoal fuel");

        // Continue heating to verify phase transition into MOLTEN_METAL
        for (int tick = 0; tick < 300; tick++) {
            vessel.step(20, AMBIENT_TEMP_K, 1.0);
            AggregateLayer current = vessel.layers()
                .get(0);
            if (current.phaseType() == AggregatePhaseType.MOLTEN_METAL) {
                break;
            }
        }

        AggregateLayer melted = vessel.layers()
            .get(0);
        assertEquals(
            AggregatePhaseType.MOLTEN_METAL,
            melted.phaseType(),
            "Iron must fully melt into MOLTEN_METAL under natural draft");
        assertTrue(melted.temperatureKelvin() >= BuiltinMaterials.IRON_PROPERTIES.meltingPointKelvin());
    }

    @Test
    void forcedDraftAcceleratesMeltingAndReachesHigherTemperatures() {
        VesselState naturalVessel = new VesselState();
        VesselState forcedVessel = new VesselState();

        MaterialQuantity ironMass = MaterialQuantity.ofKilograms(1.0);

        naturalVessel.addLayer(
            new AggregateLayer(
                BuiltinMaterials.IRON.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.IRON_COMPOSITION),
                BuiltinMaterials.IRON_COMPOSITION,
                ironMass,
                ActiveThermalState
                    .atTemperature(AMBIENT_TEMP_K, 0.0, ironMass.toKilograms(), BuiltinMaterials.IRON_PROPERTIES),
                AggregatePhaseType.SOLID_CHARGE));

        forcedVessel.addLayer(
            new AggregateLayer(
                BuiltinMaterials.IRON.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.IRON_COMPOSITION),
                BuiltinMaterials.IRON_COMPOSITION,
                ironMass,
                ActiveThermalState
                    .atTemperature(AMBIENT_TEMP_K, 0.0, ironMass.toKilograms(), BuiltinMaterials.IRON_PROPERTIES),
                AggregatePhaseType.SOLID_CHARGE));

        naturalVessel.addFuel(50000, 2010.0, "Coal \u2014 Strong Heat");
        forcedVessel.addFuel(50000, 2010.0, "Coal \u2014 Strong Heat");

        int ticksTo1000Natural = -1;
        int ticksTo1000Forced = -1;

        for (int i = 1; i <= 200; i++) {
            if (ticksTo1000Natural < 0) {
                naturalVessel.step(20, AMBIENT_TEMP_K, 1.0);
                if (naturalVessel.layers()
                    .get(0)
                    .temperatureKelvin() >= 1000.0) {
                    ticksTo1000Natural = i * 20;
                }
            }
            if (ticksTo1000Forced < 0) {
                forcedVessel.step(20, AMBIENT_TEMP_K, 1.8);
                if (forcedVessel.layers()
                    .get(0)
                    .temperatureKelvin() >= 1000.0) {
                    ticksTo1000Forced = i * 20;
                }
            }
            if (ticksTo1000Natural > 0 && ticksTo1000Forced > 0) {
                break;
            }
        }

        assertTrue(ticksTo1000Forced > 0, "Forced draft must reach 1000 K");
        assertTrue(ticksTo1000Natural > 0, "Natural draft must reach 1000 K");
        assertTrue(
            ticksTo1000Forced < ticksTo1000Natural,
            "Forced draft must reach 1000 K significantly faster than natural draft");

        // Continue running forced draft to check peak temperature (500 steps = 10,000 ticks)
        for (int i = 0; i < 500; i++) {
            forcedVessel.step(20, AMBIENT_TEMP_K, 1.8);
        }
        assertTrue(
            forcedVessel.layers()
                .get(0)
                .temperatureKelvin() > 2100.0,
            "Forced draft with coal must achieve equilibrium exceeding 2100 K");
    }

    @Test
    void blazeFuelReachesExtremeNetherTemperatures() {
        VesselState vessel = new VesselState();
        MaterialQuantity ironMass = MaterialQuantity.ofKilograms(1.0);
        vessel.addLayer(
            new AggregateLayer(
                BuiltinMaterials.IRON.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.IRON_COMPOSITION),
                BuiltinMaterials.IRON_COMPOSITION,
                ironMass,
                ActiveThermalState
                    .atTemperature(AMBIENT_TEMP_K, 0.0, ironMass.toKilograms(), BuiltinMaterials.IRON_PROPERTIES),
                AggregatePhaseType.SOLID_CHARGE));

        vessel.addFuel(60000, 2600.0, "Blaze Fuel \u2014 Extreme Heat");

        for (int i = 0; i < 650; i++) {
            vessel.step(20, AMBIENT_TEMP_K, 1.0);
        }

        double finalTemp = vessel.layers()
            .get(0)
            .temperatureKelvin();
        assertTrue(
            finalTemp > 2250.0,
            "Blaze fuel must reach extreme temperatures exceeding 2250 K under natural draft (actual: " + finalTemp
                + " K)");
    }
}
