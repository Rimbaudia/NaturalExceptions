package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.fluid.FluidSourceBoundary;
import naturalexceptions.core.fluid.FluidWorldState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

final class FluidWorldStateNbtCodecTest {
    private static FluidParcel water(long massMg, double energy) {
        CanonicalComposition c = CanonicalComposition.of(ResourceId.of("minecraft", "water"));
        CanonicalMaterialKey k = CanonicalMaterialKey.of(c, MaterialPhase.LIQUID, null, null);
        MaterialQuantity q = MaterialQuantity.ofMilligrams(massMg);
        MaterialProperties p = MaterialProperties.builder().density(1).specificHeatSolid(4180).specificHeatLiquid(4180)
            .meltingPointKelvin(273.15).latentHeatOfFusion(334000).build();
        return new FluidParcel(MaterialRef.authored("minecraft", "water"), k, c, q,
            new ActiveThermalState(energy, q.toKilograms(), p));
    }

    @Test
    void cellsQuantityThermalHistoryAndExplicitSourcesSurviveReload() {
        FluidWorldState original = new FluidWorldState(32);
        FluidCellKey cell = new FluidCellKey(0, -9, 62, 41);
        original.put(cell, water(500000L, 777777.25));
        original.registerSourceBoundary(cell,
            FluidSourceBoundary.infinite("overworld-water-spring", MaterialRef.authored("minecraft", "water")));
        NBTTagCompound saved = FluidWorldStateNbtCodec.write(original);
        FluidWorldState restored = FluidWorldStateNbtCodec.read(saved);

        assertEquals(500000L, restored.totalMass().milligrams());
        assertEquals(777777.25, restored.parcelAt(cell).thermalState().energyJoules(), 0.0);
        assertEquals(MaterialRef.authored("minecraft", "water"), restored.parcelAt(cell).materialRef());
        assertNotNull(restored.sourceAt(cell));
        assertTrue(restored.sourceAt(cell).infinite());
    }

    @Test
    void malformedFluidCellsAreSkippedWithoutInventingMatter() {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", FluidWorldStateNbtCodec.CURRENT_VERSION);
        net.minecraft.nbt.NBTTagList cells = new net.minecraft.nbt.NBTTagList();
        cells.appendTag(new NBTTagCompound()); root.setTag("Cells", cells);
        assertEquals(0L, FluidWorldStateNbtCodec.read(root).totalMass().milligrams());
    }
}
