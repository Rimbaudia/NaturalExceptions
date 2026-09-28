package naturalexceptions.minecraft.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.core.thermal.ThermodynamicModel;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

final class SmelterBreakPreservationTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static AggregateLayer hotCopper(double kg, double kelvin) {
        CanonicalComposition cu = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        double energy = ThermodynamicModel.energyForTemperature(kelvin, 1.0, kg, BuiltinMaterials.COPPER_PROPERTIES);
        return new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(cu),
            cu,
            MaterialQuantity.ofKilograms(kg),
            new ActiveThermalState(energy, kg, BuiltinMaterials.COPPER_PROPERTIES),
            AggregatePhaseType.MOLTEN_METAL);
    }

    @Test
    void chargedSmelterDropCarriesFuelFeedMeltAndHeat() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        smelter.vesselState()
            .addFuel(800);
        smelter.vesselState()
            .addLayer(hotCopper(1.25, 1500.0));
        smelter.setInventorySlotContents(TileEntityStarterSmelter.SLOT_FUEL, new ItemStack(Items.coal, 5));
        double energyBefore = smelter.vesselState()
            .layers()
            .get(0)
            .thermalState()
            .energyJoules();
        MaterialQuantity massBefore = smelter.vesselState()
            .totalMass();

        assertTrue(smelter.hasPreservableContents());
        ItemStack drop = BlockStarterSmelter.createDrop(smelter);
        assertNotNull(drop);
        assertTrue(ItemBlockStarterSmelter.carriesContents(drop));

        TileEntityStarterSmelter replaced = new TileEntityStarterSmelter();
        replaced.readContentsTag(
            drop.getTagCompound()
                .getCompoundTag(TileEntityStarterSmelter.CONTENTS_TAG));

        assertEquals(massBefore, replaced.vesselState()
            .totalMass());
        assertEquals(
            energyBefore,
            replaced.vesselState()
                .layers()
                .get(0)
                .thermalState()
                .energyJoules(),
            1.0e-6);
        assertEquals(
            800,
            replaced.vesselState()
                .fuelState()
                .remainingTicks());
        assertEquals(5, replaced.getStackInSlot(TileEntityStarterSmelter.SLOT_FUEL).stackSize);
    }

    @Test
    void emptySmelterDropsPlainControllerAndNothingInCreative() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        assertFalse(smelter.hasPreservableContents());
        ItemStack plain = BlockStarterSmelter.createDrop(smelter);
        assertNotNull(plain);
        assertFalse(ItemBlockStarterSmelter.carriesContents(plain));

        smelter.markCreativeRemoval();
        assertNull(BlockStarterSmelter.createDrop(smelter));
    }

    @Test
    void creativeRemovalNeverDeletesContents() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        smelter.vesselState()
            .addLayer(hotCopper(0.5, 1400.0));
        smelter.markCreativeRemoval();
        ItemStack drop = BlockStarterSmelter.createDrop(smelter);
        assertNotNull(drop);
        assertTrue(ItemBlockStarterSmelter.carriesContents(drop));
    }

    @Test
    void metalAtItsMeltOnsetIsNotYetPourable() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        CanonicalComposition cu = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        double kg = 0.5;
        double onset = ThermodynamicModel.energyForTemperature(
            BuiltinMaterials.COPPER_PROPERTIES.meltingPointKelvin(),
            0.0,
            kg,
            BuiltinMaterials.COPPER_PROPERTIES);
        smelter.vesselState()
            .addLayer(
                new AggregateLayer(
                    BuiltinMaterials.COPPER.ref(),
                    CanonicalMaterialKey.of(cu),
                    cu,
                    MaterialQuantity.ofKilograms(kg),
                    new ActiveThermalState(onset + 1.0, kg, BuiltinMaterials.COPPER_PROPERTIES),
                    AggregatePhaseType.MOLTEN_METAL));
        assertFalse(smelter.hasPourableMetal());
        assertNull(
            smelter.vesselState()
                .pourLiquidMetal(MaterialQuantity.ofGrams(10.0)));

        smelter.vesselState()
            .addLayer(hotCopper(0.5, 1500.0));
        assertTrue(smelter.hasPourableMetal() || smelter.vesselState()
            .layers()
            .stream()
            .anyMatch(AggregateLayer::isPourable));
        // Without an outlet in the world, the GUI says so rather than offering a dead pour control.
        assertEquals(SmelterOutletState.NO_OUTLET, smelter.outletState());
    }

    @Test
    void slagSkimmingKeepsMassAndHeatInASample() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        double kg = 0.4;
        double energy = ThermodynamicModel.energyForTemperature(
            1400.0,
            1.0,
            kg,
            naturalexceptions.core.metallurgy.OreFeedstockDefinition.SLAG_PROPERTIES);
        CanonicalComposition slag = CanonicalComposition
            .of(naturalexceptions.core.metallurgy.OreFeedstockDefinition.SLAG_RESOURCE_ID);
        smelter.vesselState()
            .addLayer(
                new AggregateLayer(
                    naturalexceptions.core.metallurgy.OreFeedstockDefinition.SLAG_REF,
                    CanonicalMaterialKey.of(slag),
                    slag,
                    MaterialQuantity.ofKilograms(kg),
                    new ActiveThermalState(energy, kg, naturalexceptions.core.metallurgy.OreFeedstockDefinition.SLAG_PROPERTIES),
                    AggregatePhaseType.SLAG));
        ItemStack skimmed = smelter.extractSlag(TileEntityStarterSmelter.SLAG_SKIM_AMOUNT);
        assertNotNull(skimmed);
        MaterialQuantity left = smelter.vesselState()
            .totalMass();
        assertEquals(
            MaterialQuantity.ofKilograms(kg),
            left.add(naturalexceptions.minecraft.item.ItemMaterialSample.getMass(skimmed)));
    }
}
