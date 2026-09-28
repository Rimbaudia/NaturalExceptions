package naturalexceptions.core.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.property.ResolvedProperties;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.persistence.VesselStateNbtCodec;

final class SmelterPhysicalMetallurgyTest {

    private static final ResourceId CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId SN = ResourceId.of("naturalexceptions", "tin");
    private static final ResourceId PB = ResourceId.of("naturalexceptions", "lead");

    @Test
    void occupiedVolumeUsesPhaseAppropriateDensityInLitres() {
        MaterialProperties properties = MaterialProperties.builder()
            .density(8.0)
            .set(MaterialProperties.KEY_DENSITY_LIQUID, 4.0)
            .build();
        MaterialQuantity mass = MaterialQuantity.ofKilograms(8.0);
        ActiveThermalState liquidThermal = ActiveThermalState.atTemperature(1400.0, 1.0, 8.0, properties);
        AggregateLayer liquid = new AggregateLayer(
            MaterialRef.authored(CU),
            CanonicalMaterialKey.of(CanonicalComposition.of(CU))
                .asMolten(),
            CanonicalComposition.of(CU),
            mass,
            liquidThermal,
            AggregatePhaseType.MOLTEN_METAL);
        assertEquals(2.0, liquid.occupiedVolumeLitres(), 1.0e-8);
        assertEquals(4.0, liquid.density(), 1.0e-8);
        AggregateLayer solid = new AggregateLayer(
            MaterialRef.authored(CU),
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            CanonicalComposition.of(CU),
            mass,
            ActiveThermalState.ambient(8.0, properties),
            AggregatePhaseType.SOLID_CHARGE);
        assertEquals(1.0, solid.occupiedVolumeLitres(), 1.0e-8);
    }

    @Test
    void batchMassCannotBecomeNegative() {
        MaterialQuantity q1 = MaterialQuantity.ofGrams(10.0);
        MaterialQuantity q2 = MaterialQuantity.ofGrams(20.0);
        assertThrows(ArithmeticException.class, () -> q1.subtract(q2));
    }

    @Test
    void batchMergeConservesMass() {
        MaterialQuantity m1 = MaterialQuantity.ofGrams(12.5);
        MaterialQuantity m2 = MaterialQuantity.ofGrams(7.5);
        MaterialQuantity merged = m1.add(m2);
        assertEquals(20.0, merged.toGrams(), 1.0e-6);
    }

    @Test
    void mixtureCompositionIsMassWeightedAndCanonical() {
        VesselState vessel = new VesselState();

        // 3 kg pure Cu
        MaterialQuantity cuMass = MaterialQuantity.ofKilograms(3.0);
        ActiveThermalState cuThermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer cuLayer = new AggregateLayer(
            MaterialRef.authored(CU),
            CanonicalMaterialKey.of(CanonicalComposition.of(CU))
                .asMolten(),
            CanonicalComposition.of(CU),
            cuMass,
            cuThermal,
            AggregatePhaseType.MOLTEN_METAL);

        // 1 kg pure Sn
        MaterialQuantity snMass = MaterialQuantity.ofKilograms(1.0);
        MaterialProperties snProps = MaterialProperties.of(7.31, 1.5, 15.0);
        ActiveThermalState snThermal = ActiveThermalState.atTemperature(1400.0, 1.0, snMass.toKilograms(), snProps);
        AggregateLayer snLayer = new AggregateLayer(
            MaterialRef.authored(SN),
            CanonicalMaterialKey.of(CanonicalComposition.of(SN))
                .asMolten(),
            CanonicalComposition.of(SN),
            snMass,
            snThermal,
            AggregatePhaseType.MOLTEN_METAL);

        vessel.addLayer(cuLayer);
        vessel.addLayer(snLayer);

        // Step simulation to trigger mixing
        vessel.step(1, 293.15);

        assertEquals(
            1,
            vessel.layers()
                .size());
        AggregateLayer alloy = vessel.layers()
            .get(0);
        assertTrue(Double.isFinite(alloy.occupiedVolumeLitres()));
        assertTrue(alloy.occupiedVolumeLitres() > 0.0);
        assertEquals(MaterialQuantity.ofKilograms(4.0), alloy.mass());

        // 75% Cu (7500 bp), 25% Sn (2500 bp)
        CanonicalComposition comp = alloy.composition();
        assertEquals(0.75, comp.fraction(CU), 0.001);
        assertEquals(0.25, comp.fraction(SN), 0.001);
    }

    @Test
    void unknownMoltenCompositionIsAccepted() {
        VesselState vessel = new VesselState();

        // Ternary mixture: 57% Cu + 23% Sn + 20% Pb
        CanonicalComposition ternaryComp = CanonicalComposition.builder()
            .add(CU, 5700)
            .add(SN, 2300)
            .add(PB, 2000)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey.of(ternaryComp)
            .asMolten();
        MaterialRef ref = MaterialRef.generatedFrom(key);

        MaterialQuantity mass = MaterialQuantity.ofKilograms(2.0);
        ResolvedProperties resolved = vessel.propertyResolver()
            .resolve(ref, ternaryComp, MaterialPhase.LIQUID);
        ActiveThermalState thermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, mass.toKilograms(), resolved.properties());

        AggregateLayer layer = new AggregateLayer(
            ref,
            key,
            ternaryComp,
            mass,
            thermal,
            AggregatePhaseType.MOLTEN_METAL);
        vessel.addLayer(layer);

        assertEquals(
            1,
            vessel.layers()
                .size());
        assertEquals(
            ref,
            vessel.layers()
                .get(0)
                .materialRef());
        assertTrue(
            vessel.layers()
                .get(0)
                .density() > 0.0);
    }

    @Test
    void unknownMixtureDoesNotMatchAuthoredAlloyRule() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        CanonicalComposition ternaryComp = CanonicalComposition.builder()
            .add(CU, 5700)
            .add(SN, 2300)
            .add(PB, 2000)
            .build();
        MaterialRef ref = MaterialRef.generatedFrom(CanonicalMaterialKey.of(ternaryComp));

        ResolvedProperties solidRes = resolver.resolve(ref, ternaryComp, MaterialPhase.SOLID);
        // Ensure this undocumented alloy does NOT match the alpha-bronze rule
        assertFalse(
            solidRes.trace()
                .layerApplied("phase:alpha_bronze"));
        assertTrue(
            solidRes.properties()
                .hardness() > 0.0);
    }

    @Test
    void thermalEnergyControlsMeltingNotProgressTimer() {
        VesselState vessel = new VesselState();
        vessel.addRawOre(OreFeedstockDefinition.copperOre());

        // Run 5,000 ticks with 0 fuel (ambient)
        for (int i = 0; i < 5000; i++) {
            vessel.step(1, 293.15);
        }

        // Must remain 100% unmelted solid charge
        assertEquals(
            1,
            vessel.layers()
                .size());
        assertEquals(
            AggregatePhaseType.SOLID_CHARGE,
            vessel.layers()
                .get(0)
                .phaseType());
        assertEquals(
            MaterialPhase.SOLID,
            vessel.layers()
                .get(0)
                .physicalPhase());

        // Now add fuel to drive thermal energy up
        vessel.addFuel(500); // 500 burn ticks
        // Run until temperature reaches smelting point
        for (int i = 0; i < 300; i++) {
            vessel.step(1, 293.15);
            if (vessel.layers()
                .get(0)
                .phaseType() == AggregatePhaseType.MOLTEN_METAL) {
                break;
            }
        }

        // Smelting occurred exclusively because temperature rose above 1350 K
        assertTrue(
            vessel.layers()
                .stream()
                .anyMatch(l -> l.phaseType() == AggregatePhaseType.MOLTEN_METAL));
    }

    @Test
    void coldSmelterSleeps() {
        VesselState vessel = new VesselState();
        vessel.addRawOre(OreFeedstockDefinition.copperOre());
        assertTrue(vessel.isDormant(293.15));
    }

    @Test
    void dormancyUsesTheLayerMeltingPoint() {
        MaterialProperties highMeltProperties = MaterialProperties.builder()
            .density(8.0)
            .hardness(4.0)
            .conductivity(20.0)
            .thermalConductivity(80.0)
            .specificHeatSolid(400.0)
            .specificHeatLiquid(450.0)
            .meltingPointKelvin(1800.0)
            .latentHeatOfFusion(100000.0)
            .electricalResistivity(1.0e-6)
            .build();
        ResourceId materialId = ResourceId.of("naturalexceptions", "high_melt_test_material");
        MaterialQuantity mass = MaterialQuantity.ofGrams(100.0);
        VesselState vessel = new VesselState();
        vessel.addLayer(
            new AggregateLayer(
                MaterialRef.authored(materialId),
                CanonicalMaterialKey.of(CanonicalComposition.of(materialId)),
                CanonicalComposition.of(materialId),
                mass,
                ActiveThermalState.atTemperature(1400.0, 0.0, mass.toKilograms(), highMeltProperties),
                AggregatePhaseType.SOLID_CHARGE));
        assertTrue(vessel.isDormant(1400.0));
    }

    @Test
    void activeSmelterUsesBoundedScheduledWork() {
        VesselState vessel = new VesselState();
        vessel.addRawOre(OreFeedstockDefinition.copperOre());
        vessel.addFuel(100);
        assertFalse(vessel.isDormant(293.15), "Burning vessel is not dormant");
    }

    @Test
    void copperFeedProducesAccountedUsefulAndWasteMatter() {
        VesselState vessel = new VesselState();
        OreFeedstockDefinition oreDef = OreFeedstockDefinition.copperOre();
        vessel.addRawOre(oreDef);

        MaterialQuantity initialMass = vessel.totalMass();
        assertEquals(1_000_000L, initialMass.milligrams()); // 1000g

        // Inject thermal energy to melt
        vessel.addFuel(1000);
        for (int i = 0; i < 400; i++) {
            vessel.step(1, 293.15);
        }

        // Check products
        AggregateLayer metal = vessel.layers()
            .stream()
            .filter(l -> l.phaseType() == AggregatePhaseType.MOLTEN_METAL)
            .findFirst()
            .orElse(null);
        AggregateLayer slag = vessel.layers()
            .stream()
            .filter(l -> l.phaseType() == AggregatePhaseType.SLAG)
            .findFirst()
            .orElse(null);

        assertNotNull(metal);
        assertNotNull(slag);

        assertEquals(
            600_000L,
            metal.mass()
                .milligrams()); // 600g Cu
        assertEquals(
            350_000L,
            slag.mass()
                .milligrams()); // 350g slag
        assertEquals(
            50_000L,
            vessel.exhaustSink()
                .totalVentedMass()
                .milligrams()); // 50g gas

        // Exact conservation
        MaterialQuantity totalFinal = metal.mass()
            .add(slag.mass())
            .add(
                vessel.exhaustSink()
                    .totalVentedMass());
        assertEquals(initialMass, totalFinal);
    }

    @Test
    void registeredNonCopperFeedstocksReduceToTheirOwnMetal() {
        assertRawOreReduction(OreFeedstockDefinition.tinOreChunk(), BuiltinMaterials.TIN.ref());
        assertRawOreReduction(OreFeedstockDefinition.leadOreChunk(), BuiltinMaterials.LEAD.ref());
    }

    private static void assertRawOreReduction(OreFeedstockDefinition oreDef, MaterialRef expectedMetalRef) {
        VesselState vessel = new VesselState();
        vessel.addRawOre(oreDef);
        vessel.addFuel(1000);
        for (int i = 0; i < 400; i++) {
            vessel.step(1, 293.15);
        }

        AggregateLayer metal = vessel.layers()
            .stream()
            .filter(l -> l.phaseType() == AggregatePhaseType.MOLTEN_METAL)
            .findFirst()
            .orElse(null);
        AggregateLayer slag = vessel.layers()
            .stream()
            .filter(l -> l.phaseType() == AggregatePhaseType.SLAG)
            .findFirst()
            .orElse(null);

        assertNotNull(metal);
        assertEquals(expectedMetalRef, metal.materialRef());
        assertEquals(
            60_000L,
            metal.mass()
                .milligrams());
        assertNotNull(slag);
        assertEquals(
            35_000L,
            slag.mass()
                .milligrams());
        assertEquals(
            5_000L,
            vessel.exhaustSink()
                .totalVentedMass()
                .milligrams());
        assertEquals(
            MaterialQuantity.ofGrams(100.0),
            metal.mass()
                .add(slag.mass())
                .add(
                    vessel.exhaustSink()
                        .totalVentedMass()));
    }

    @Test
    void declaredExhaustSinkAccountsForDeferredGasProducts() {
        VesselState vessel = new VesselState();
        vessel.addRawOre(OreFeedstockDefinition.copperOre());
        vessel.addFuel(1000);
        for (int i = 0; i < 400; i++) {
            vessel.step(1, 293.15);
        }

        assertEquals(
            50_000L,
            vessel.exhaustSink()
                .getVented(ExhaustSink.EXHAUST_GAS)
                .milligrams());
    }

    @Test
    void moltenCompatibleMaterialsMix() {
        VesselState vessel = new VesselState();
        ActiveThermalState cuThermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, 1.0, BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer cu = new AggregateLayer(
            MaterialRef.authored(CU),
            CanonicalMaterialKey.of(CanonicalComposition.of(CU))
                .asMolten(),
            CanonicalComposition.of(CU),
            MaterialQuantity.ofGrams(100.0),
            cuThermal,
            AggregatePhaseType.MOLTEN_METAL);

        ActiveThermalState snThermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, 0.1, MaterialProperties.of(7.31, 1.5, 15.0));
        AggregateLayer sn = new AggregateLayer(
            MaterialRef.authored(SN),
            CanonicalMaterialKey.of(CanonicalComposition.of(SN))
                .asMolten(),
            CanonicalComposition.of(SN),
            MaterialQuantity.ofGrams(10.0),
            snThermal,
            AggregatePhaseType.MOLTEN_METAL);

        vessel.addLayer(cu);
        vessel.addLayer(sn);

        vessel.step(1, 293.15);

        assertEquals(
            1,
            vessel.layers()
                .size());
        assertEquals(
            110_000L,
            vessel.layers()
                .get(0)
                .mass()
                .milligrams());
    }

    @Test
    void slagRemainsSeparateFromMetal() {
        VesselState vessel = new VesselState();
        vessel.addRawOre(OreFeedstockDefinition.copperOre());
        vessel.addFuel(1000);
        for (int i = 0; i < 400; i++) {
            vessel.step(1, 293.15);
        }

        // Metal and slag are separate layers
        assertEquals(
            2,
            vessel.layers()
                .size());
        assertTrue(
            vessel.layers()
                .stream()
                .anyMatch(l -> l.phaseType() == AggregatePhaseType.MOLTEN_METAL));
        assertTrue(
            vessel.layers()
                .stream()
                .anyMatch(l -> l.phaseType() == AggregatePhaseType.SLAG));
    }

    @Test
    void layerOrderingIsDeterministic() {
        VesselState vessel = new VesselState();
        vessel.addRawOre(OreFeedstockDefinition.copperOre());
        vessel.addFuel(1000);
        for (int i = 0; i < 400; i++) {
            vessel.step(1, 293.15);
        }

        // Layer 0 is dense molten metal (~8.96 g/cm³), Layer 1 is lighter slag (~2.8 g/cm³)
        assertEquals(
            AggregatePhaseType.MOLTEN_METAL,
            vessel.layers()
                .get(0)
                .phaseType());
        assertEquals(
            AggregatePhaseType.SLAG,
            vessel.layers()
                .get(1)
                .phaseType());
    }

    @Test
    void skimmingRemovesActualTopLayerMass() {
        VesselState vessel = new VesselState();
        vessel.addRawOre(OreFeedstockDefinition.copperOre());
        vessel.addFuel(1000);
        for (int i = 0; i < 400; i++) {
            vessel.step(1, 293.15);
        }

        MaterialQuantity vesselMassBefore = vessel.totalMass();
        AggregateLayer skimmed = vessel.skimSlag(MaterialQuantity.ofGrams(20.0));

        assertNotNull(skimmed);
        assertEquals(
            20_000L,
            skimmed.mass()
                .milligrams());
        assertEquals(AggregatePhaseType.SLAG, skimmed.phaseType());

        // Vessel mass decreased by exactly skimmed amount
        assertEquals(vesselMassBefore.subtract(MaterialQuantity.ofGrams(20.0)), vessel.totalMass());
    }

    @Test
    void pouringRemovesActualAccessibleLiquidMass() {
        VesselState vessel = new VesselState();
        vessel.addRawOre(OreFeedstockDefinition.copperOre());
        vessel.addFuel(1000);
        for (int i = 0; i < 400; i++) {
            vessel.step(1, 293.15);
        }

        MaterialQuantity vesselMassBefore = vessel.totalMass();
        AggregateLayer poured = vessel.pourLiquidMetal(MaterialQuantity.ofGrams(40.0));

        assertNotNull(poured);
        assertEquals(
            40_000L,
            poured.mass()
                .milligrams());
        assertEquals(AggregatePhaseType.MOLTEN_METAL, poured.phaseType());

        assertEquals(vesselMassBefore.subtract(MaterialQuantity.ofGrams(40.0)), vessel.totalMass());
    }

    @Test
    void smallBatchProcessWorks() {
        VesselState vessel = new VesselState();
        OreFeedstockDefinition oreDef = new OreFeedstockDefinition(
            ResourceId.of("naturalexceptions", "small_ore_chunk"),
            MaterialQuantity.ofGrams(10.0), // 10g small batch
            0.60,
            0.35,
            0.05,
            MaterialRef.authored(BuiltinMaterials.COPPER_RESOURCE_ID),
            BuiltinMaterials.COPPER_PROPERTIES);

        vessel.addRawOre(oreDef);
        vessel.addFuel(500);
        for (int i = 0; i < 400; i++) {
            vessel.step(1, 293.15);
        }

        AggregateLayer metal = vessel.pourLiquidMetal(MaterialQuantity.ofGrams(6.0));
        assertNotNull(metal);
        assertEquals(
            6_000L,
            metal.mass()
                .milligrams());
    }

    @Test
    void vesselOvertemperatureProducesDamage() {
        VesselState vessel = new VesselState();
        assertFalse(
            vessel.lining()
                .isDamaged());

        // Heat vessel layers to 1750 K (safe limit is 1500 K)
        ActiveThermalState superheated = ActiveThermalState
            .atTemperature(1750.0, 1.0, 1.0, BuiltinMaterials.COPPER_PROPERTIES);
        vessel.addLayer(
            new AggregateLayer(
                MaterialRef.authored(CU),
                CanonicalMaterialKey.of(CanonicalComposition.of(CU))
                    .asMolten(),
                CanonicalComposition.of(CU),
                MaterialQuantity.ofKilograms(1.0),
                superheated,
                AggregatePhaseType.MOLTEN_METAL));

        // Step simulation for 60 seconds
        for (int i = 0; i < 1200; i++) {
            vessel.step(1, 1750.0); // ambient at 1750 to prevent cooling in test
        }

        assertTrue(
            vessel.lining()
                .isDamaged());
        assertTrue(
            vessel.lining()
                .damage() > 0.05);
    }

    @Test
    void extractedMaterialCanBeRemelted() {
        VesselState vessel = new VesselState();
        vessel.addRawOre(OreFeedstockDefinition.copperOre());
        vessel.addFuel(1000);
        for (int i = 0; i < 400; i++) {
            vessel.step(1, 293.15);
        }

        AggregateLayer poured = vessel.pourLiquidMetal(MaterialQuantity.ofGrams(30.0));
        assertNotNull(poured);

        // Cool to ambient (solidify)
        ActiveThermalState cooledThermal = ActiveThermalState.ambient(
            poured.mass()
                .toKilograms(),
            BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer cooledSample = new AggregateLayer(
            poured.materialRef(),
            poured.stateKey()
                .asSolidified(),
            poured.composition(),
            poured.mass(),
            cooledThermal,
            AggregatePhaseType.SOLID_CHARGE);

        MaterialQuantity massBeforeReinsert = vessel.totalMass();

        // Remelt back into vessel
        vessel.remelt(cooledSample);
        assertEquals(massBeforeReinsert.add(poured.mass()), vessel.totalMass());

        // Heat up to melt it
        vessel.addFuel(1000);
        for (int i = 0; i < 400; i++) {
            vessel.step(1, 293.15);
        }

        // Metal layer merged back to 600g Cu
        AggregateLayer metal = vessel.layers()
            .stream()
            .filter(l -> l.phaseType() == AggregatePhaseType.MOLTEN_METAL)
            .findFirst()
            .orElse(null);
        assertNotNull(metal);
        assertEquals(
            600_000L,
            metal.mass()
                .milligrams());
    }

    @Test
    void smelterStateSurvivesSaveReload() {
        VesselState vessel = new VesselState();
        vessel.addRawOre(OreFeedstockDefinition.copperOre());
        vessel.addFuel(500);

        NBTTagCompound nbt = new NBTTagCompound();
        VesselStateNbtCodec.writeToNbt(vessel, nbt);

        VesselState reloaded = VesselStateNbtCodec.readFromNbt(nbt, new MaterialPropertyResolver());

        assertEquals(vessel.totalMass(), reloaded.totalMass());
        assertEquals(
            vessel.fuelState()
                .remainingTicks(),
            reloaded.fuelState()
                .remainingTicks());
        assertEquals(
            vessel.layers()
                .size(),
            reloaded.layers()
                .size());
    }

    @Test
    void partialMeltSurvivesSaveReload() {
        VesselState vessel = new VesselState();
        ActiveThermalState mushy = ActiveThermalState
            .atTemperature(1357.77, 0.50, 1.0, BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer layer = new AggregateLayer(
            MaterialRef.authored(CU),
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            CanonicalComposition.of(CU),
            MaterialQuantity.ofKilograms(1.0),
            mushy,
            AggregatePhaseType.SOLID_CHARGE);
        vessel.addLayer(layer);

        NBTTagCompound nbt = new NBTTagCompound();
        VesselStateNbtCodec.writeToNbt(vessel, nbt);

        VesselState reloaded = VesselStateNbtCodec.readFromNbt(nbt, new MaterialPropertyResolver());
        AggregateLayer reloadedLayer = reloaded.layers()
            .get(0);

        assertEquals(
            0.50,
            reloadedLayer.thermalState()
                .meltFraction(),
            1.0e-3);
        assertEquals(1357.77, reloadedLayer.temperatureKelvin(), 0.05);
    }

    @Test
    void generatedMixtureSurvivesSaveReload() {
        VesselState vessel = new VesselState();
        CanonicalComposition ternaryComp = CanonicalComposition.builder()
            .add(CU, 5700)
            .add(SN, 2300)
            .add(PB, 2000)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey.of(ternaryComp)
            .asMolten();
        MaterialRef ref = MaterialRef.generatedFrom(key);

        MaterialQuantity mass = MaterialQuantity.ofKilograms(2.5);
        ResolvedProperties resolved = vessel.propertyResolver()
            .resolve(ref, ternaryComp, MaterialPhase.LIQUID);
        ActiveThermalState thermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, mass.toKilograms(), resolved.properties());

        AggregateLayer layer = new AggregateLayer(
            ref,
            key,
            ternaryComp,
            mass,
            thermal,
            AggregatePhaseType.MOLTEN_METAL);
        vessel.addLayer(layer);

        NBTTagCompound nbt = new NBTTagCompound();
        VesselStateNbtCodec.writeToNbt(vessel, nbt);

        VesselState reloaded = VesselStateNbtCodec.readFromNbt(nbt, vessel.propertyResolver());
        AggregateLayer reloadedLayer = reloaded.layers()
            .get(0);

        assertEquals(ref, reloadedLayer.materialRef());
        assertEquals(ternaryComp, reloadedLayer.composition());
        assertEquals(mass, reloadedLayer.mass());
    }

    @Test
    void completeCopperProcessConservesTrackedMatter() {
        VesselState vessel = new VesselState();
        OreFeedstockDefinition ore = OreFeedstockDefinition.copperOre();
        vessel.addRawOre(ore); // 1000g

        MaterialQuantity initialMass = vessel.totalMass();

        // Smelt
        vessel.addFuel(1000);
        for (int i = 0; i < 400; i++) {
            vessel.step(1, 293.15);
        }

        // Pour 40g metal
        AggregateLayer poured = vessel.pourLiquidMetal(MaterialQuantity.ofGrams(40.0));
        assertNotNull(poured);

        // Skim 20g slag
        AggregateLayer skimmed = vessel.skimSlag(MaterialQuantity.ofGrams(20.0));
        assertNotNull(skimmed);

        // Check conservation equation:
        // initialMass == vessel.totalMass() + poured.mass() + skimmed.mass() + exhaustSink.totalVentedMass()
        MaterialQuantity remainingVesselMass = vessel.totalMass();
        MaterialQuantity finalTotalAccounted = remainingVesselMass.add(poured.mass())
            .add(skimmed.mass())
            .add(
                vessel.exhaustSink()
                    .totalVentedMass());

        assertEquals(initialMass, finalTotalAccounted);
        assertEquals(1_000_000L, finalTotalAccounted.milligrams());
    }

    @Test
    void fractionalLiquidLayerPouringConservesMassAndProportionalEnergy() {
        VesselState vessel = new VesselState();
        MaterialQuantity totalMass = MaterialQuantity.ofGrams(500.0);
        ActiveThermalState thermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, totalMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        double initialJoules = thermal.energyJoules();

        AggregateLayer layer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                .asMolten(),
            BuiltinMaterials.COPPER_COMPOSITION,
            totalMass,
            thermal,
            AggregatePhaseType.MOLTEN_METAL);
        vessel.addLayer(layer);

        // Pour 200g of copper
        MaterialQuantity pour1Mass = MaterialQuantity.ofGrams(200.0);
        AggregateLayer poured1 = vessel.pourLiquidMetal(pour1Mass);
        assertNotNull(poured1);
        assertEquals(pour1Mass, poured1.mass());
        assertEquals(MaterialQuantity.ofGrams(300.0), vessel.totalMass());

        // Check conservation
        double remainingJoules1 = vessel.layers()
            .get(0)
            .thermalState()
            .energyJoules();
        double pouredJoules1 = poured1.thermalState()
            .energyJoules();
        assertEquals(initialJoules, remainingJoules1 + pouredJoules1, 1e-4);
        assertEquals(initialJoules * 0.40, pouredJoules1, 1e-4);
        assertEquals(initialJoules * 0.60, remainingJoules1, 1e-4);

        // Second pour of 300g (exhausts the remaining layer)
        MaterialQuantity pour2Mass = MaterialQuantity.ofGrams(300.0);
        AggregateLayer poured2 = vessel.pourLiquidMetal(pour2Mass);
        assertNotNull(poured2);
        assertEquals(pour2Mass, poured2.mass());
        assertEquals(MaterialQuantity.ZERO, vessel.totalMass());
        assertEquals(
            0,
            vessel.layers()
                .size(),
            "Exhausted layer must be removed from vessel");

        double pouredJoules2 = poured2.thermalState()
            .energyJoules();
        assertEquals(remainingJoules1, pouredJoules2, 1e-4);
        assertEquals(initialJoules, pouredJoules1 + pouredJoules2, 1e-4);
    }

    @Test
    void fractionalPouringFiltersByMaterialRefAndCapsToAvailableMass() {
        VesselState vessel = new VesselState();
        MaterialQuantity cuMass = MaterialQuantity.ofGrams(300.0);
        ActiveThermalState cuThermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        vessel.addLayer(
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                    .asMolten(),
                BuiltinMaterials.COPPER_COMPOSITION,
                cuMass,
                cuThermal,
                AggregatePhaseType.MOLTEN_METAL));

        // Requesting tin must return null because no tin is in vessel
        AggregateLayer tinPour = vessel.pourLiquidMetal(BuiltinMaterials.TIN.ref(), MaterialQuantity.ofGrams(100.0));
        assertNull(tinPour);
        assertEquals(cuMass, vessel.totalMass());

        // Requesting 500g of copper when only 300g is available transfers available 300g
        AggregateLayer cuPour = vessel.pourLiquidMetal(BuiltinMaterials.COPPER.ref(), MaterialQuantity.ofGrams(500.0));
        assertNotNull(cuPour);
        assertEquals(MaterialQuantity.ofGrams(300.0), cuPour.mass());
        assertEquals(MaterialQuantity.ZERO, vessel.totalMass());
    }

    @Test
    void performanceScalingManyDormantSmeltersDoNearZeroWork() {
        List<VesselState> smelters = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            VesselState v = new VesselState();
            v.addRawOre(OreFeedstockDefinition.copperOre());
            smelters.add(v);
        }

        long start = System.nanoTime();
        int activeCount = 0;
        for (VesselState v : smelters) {
            if (!v.isDormant(293.15)) {
                v.step(1, 293.15);
                activeCount++;
            }
        }
        long durationNs = System.nanoTime() - start;

        assertEquals(0, activeCount, "All cold smelters must be dormant");
        // 500 dormant checks should take well under 10 milliseconds
        assertTrue(durationNs < 50_000_000L, "Dormant check took " + (durationNs / 1_000_000.0) + " ms");
    }
}
