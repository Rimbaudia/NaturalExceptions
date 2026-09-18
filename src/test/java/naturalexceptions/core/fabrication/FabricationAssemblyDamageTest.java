package naturalexceptions.core.fabrication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.assembly.AttachmentInterface;
import naturalexceptions.core.assembly.DerivedCapability;
import naturalexceptions.core.assembly.TargetMaterialProfile;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.metallurgy.VesselState;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.property.ResolvedProperties;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.persistence.AssemblyNbtCodec;

final class FabricationAssemblyDamageTest {

    private static final ResourceId CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId SN = ResourceId.of("naturalexceptions", "tin");
    private static final ResourceId PB = ResourceId.of("naturalexceptions", "lead");
    private static final ResourceId WOOD = ResourceId.of("naturalexceptions", "wood");

    private static final MaterialRef CU_REF = MaterialRef.authored(CU);
    private static final MaterialRef WOOD_REF = MaterialRef.authored(WOOD);

    @BeforeAll
    static void init() {
        BuiltinContent.register();
    }

    private static AggregateLayer createMoltenCopperLayer(MaterialQuantity mass, double tempK) {
        ActiveThermalState thermal = ActiveThermalState
            .atTemperature(tempK, 1.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        return new AggregateLayer(
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU))
                .asMolten(),
            CanonicalComposition.of(CU),
            mass,
            thermal,
            AggregatePhaseType.MOLTEN_METAL);
    }

    private static AggregateLayer createMoltenAlloyLayer(MaterialQuantity mass, double tempK, MaterialProperties props,
        MaterialRef ref, CanonicalComposition comp) {
        ActiveThermalState thermal = ActiveThermalState.atTemperature(tempK, 1.0, mass.toKilograms(), props);
        return new AggregateLayer(
            ref,
            CanonicalMaterialKey.of(comp)
                .asMolten(),
            comp,
            mass,
            thermal,
            AggregatePhaseType.MOLTEN_METAL);
    }

    @Test
    void moldAcceptsAnyCompatibleMaterial() {
        CastingMold mold = new CastingMold(ComponentGeometry.pickHead());
        AggregateLayer cuLayer = createMoltenCopperLayer(MaterialQuantity.ofKilograms(1.0), 1400.0);

        MaterialQuantity poured = mold.pour(cuLayer, BuiltinMaterials.COPPER_PROPERTIES);
        assertTrue(poured.compareTo(MaterialQuantity.ZERO) > 0);
        assertEquals(CU_REF, mold.materialRef());
        assertEquals(mold.requiredMass(), mold.filledMass());
    }

    @Test
    void moldCapacityDependsOnGeometryNotMaterialWhitelist() {
        ComponentGeometry geometry = ComponentGeometry.pickHead(); // 30.0 cm³
        CastingMold moldCu = new CastingMold(geometry);
        CastingMold moldAlloy = new CastingMold(geometry);

        MaterialProperties heavyProps = MaterialProperties.builder()
            .density(11.34) // e.g. lead-heavy alloy (11.34 g/cm³)
            .meltingPointKelvin(600.0)
            .specificHeatSolid(130.0)
            .specificHeatLiquid(140.0)
            .latentHeatOfFusion(23000.0)
            .thermalConductivity(35.0)
            .hardness(1.5)
            .build();

        AggregateLayer cuLayer = createMoltenCopperLayer(MaterialQuantity.ofKilograms(1.0), 1400.0);
        AggregateLayer alloyLayer = createMoltenAlloyLayer(
            MaterialQuantity.ofKilograms(1.0),
            700.0,
            heavyProps,
            MaterialRef.generated(DeterministicId.derive("mat", "heavy_alloy")),
            CanonicalComposition.of(PB));

        moldCu.pour(cuLayer, BuiltinMaterials.COPPER_PROPERTIES);
        moldAlloy.pour(alloyLayer, heavyProps);

        // Required mass = 30 cm³ * 8.96 g/cm³ = 268.8 g = 268,800 mg
        assertEquals(
            268800L,
            moldCu.requiredMass()
                .milligrams());
        // Required mass = 30 cm³ * 11.34 g/cm³ = 340.2 g = 340,200 mg
        assertEquals(
            340200L,
            moldAlloy.requiredMass()
                .milligrams());
        assertTrue(
            moldAlloy.requiredMass()
                .compareTo(moldCu.requiredMass()) > 0);
    }

    @Test
    void castingTransfersRealMassFromVessel() {
        VesselState vessel = new VesselState();
        MaterialQuantity initialCu = MaterialQuantity.ofGrams(1000.0);
        AggregateLayer layer = createMoltenCopperLayer(initialCu, 1400.0);
        vessel.addLayer(layer);

        CastingMold mold = new CastingMold(ComponentGeometry.pickHead());
        MaterialProperties props = BuiltinMaterials.COPPER_PROPERTIES;
        MaterialQuantity poured = mold.pour(layer, props);

        assertEquals(268800L, poured.milligrams());
        assertEquals(
            268800L,
            mold.filledMass()
                .milligrams());
        assertEquals(
            731200L,
            layer.mass()
                .milligrams());
        // Conservation check
        assertEquals(
            initialCu.milligrams(),
            mold.filledMass()
                .add(layer.mass())
                .milligrams());
    }

    @Test
    void partialMoldFillRemainsPartial() {
        CastingMold mold = new CastingMold(ComponentGeometry.pickHead());
        MaterialQuantity partialMass = MaterialQuantity.ofGrams(100.0);
        AggregateLayer layer = createMoltenCopperLayer(partialMass, 1400.0);

        MaterialQuantity poured = mold.pour(layer, BuiltinMaterials.COPPER_PROPERTIES);
        assertEquals(partialMass, poured);
        assertEquals(partialMass, mold.filledMass());
        assertFalse(mold.isFilled());
        assertFalse(mold.isSolid());
        assertThrows(IllegalStateException.class, mold::extractComponent);
    }

    @Test
    void castPieceSolidifiesThroughThermalModel() {
        CastingMold mold = new CastingMold(ComponentGeometry.pickHead());
        AggregateLayer layer = createMoltenCopperLayer(MaterialQuantity.ofGrams(500.0), 1450.0);
        mold.pour(layer, BuiltinMaterials.COPPER_PROPERTIES);

        assertTrue(mold.isFilled());
        assertFalse(mold.isSolid());
        assertThrows(IllegalStateException.class, mold::extractComponent);

        // Cool mold until solid (melting point 1358 K)
        mold.cool(200.0, 300.0);
        assertTrue(
            mold.thermalState()
                .temperatureKelvin() < BuiltinMaterials.COPPER_PROPERTIES.meltingPointKelvin());
        assertEquals(
            MaterialPhase.SOLID,
            mold.thermalState()
                .phase());
        assertTrue(mold.isSolid());

        ComponentInstance component = mold.extractComponent();
        assertNotNull(component);
        assertEquals(GeometryId.PICK_HEAD, component.geometryId());
        assertEquals(ManufacturingState.AS_CAST, component.manufacturingState());
        assertEquals(
            268800L,
            component.mass()
                .milligrams());
    }

    @Test
    void workingOperationConservesMatter() {
        MaterialQuantity mass = MaterialQuantity.ofGrams(200.0);
        ComponentInstance blank = new ComponentInstance(
            GeometryId.BLANK,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            mass,
            ManufacturingState.AS_CAST);

        WorkingOperation.RESHAPE.apply(blank, GeometryId.PLATE);
        assertEquals(GeometryId.PLATE, blank.geometryId());
        assertEquals(ManufacturingState.WORKED, blank.manufacturingState());
        assertEquals(
            mass.milligrams(),
            blank.mass()
                .milligrams());

        WorkingOperation.FINISH.apply(blank, GeometryId.PICK_HEAD);
        assertEquals(GeometryId.PICK_HEAD, blank.geometryId());
        assertEquals(ManufacturingState.FINISHED, blank.manufacturingState());
        assertEquals(
            mass.milligrams(),
            blank.mass()
                .milligrams());
    }

    @Test
    void componentCarriesGeneratedMaterialRef() {
        MaterialRef generatedRef = MaterialRef.generated(DeterministicId.derive("mat", "cu70_sn20_pb10"));
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 7000)
            .add(SN, 2000)
            .add(PB, 1000)
            .build();
        ComponentInstance compInst = new ComponentInstance(
            GeometryId.PICK_HEAD,
            generatedRef,
            CanonicalMaterialKey.of(comp),
            MaterialQuantity.ofGrams(250.0),
            ManufacturingState.FINISHED);

        assertEquals(generatedRef, compInst.materialRef());
        assertEquals(
            comp,
            compInst.stateKey()
                .composition());
    }

    @Test
    void componentPersistencePreservesManufacturingState() {
        ComponentInstance original = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        original.damage()
            .addWear(0.12);
        original.damage()
            .addDeformation(0.08);
        original.damage()
            .addCracking(0.04);

        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(120.0),
            ManufacturingState.FINISHED);

        AssemblyGraph graph = AssemblyGraph.ofTool(original, handle);

        NBTTagCompound nbt = new NBTTagCompound();
        AssemblyNbtCodec.writeToNbt(graph, nbt);

        AssemblyGraph restored = AssemblyNbtCodec.readFromNbt(nbt);
        assertNotNull(restored);
        assertEquals(
            ManufacturingState.FINISHED,
            restored.head()
                .manufacturingState());
        assertEquals(
            GeometryId.PICK_HEAD,
            restored.head()
                .geometryId());
        assertEquals(
            original.mass()
                .milligrams(),
            restored.head()
                .mass()
                .milligrams());
        assertEquals(
            0.12,
            restored.head()
                .damage()
                .wear(),
            1.0e-5);
        assertEquals(
            0.08,
            restored.head()
                .damage()
                .deformation(),
            1.0e-5);
        assertEquals(
            0.04,
            restored.head()
                .damage()
                .cracking(),
            1.0e-5);
    }

    @Test
    void attachmentRequiresCompatibleInterfaces() {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);

        ComponentInstance validHandle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        ComponentInstance invalidHandle = new ComponentInstance(
            GeometryId.PLATE,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        assertNotNull(new AssemblyGraph(head, validHandle, AttachmentInterface.SOCKET));
        assertThrows(
            IllegalArgumentException.class,
            () -> new AssemblyGraph(head, invalidHandle, AttachmentInterface.SOCKET));
    }

    @Test
    void assemblyPreservesIndividualComponentMaterials() {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);

        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(120.0),
            ManufacturingState.FINISHED);

        AssemblyGraph assembly = AssemblyGraph.ofTool(head, handle);
        assertEquals(
            CU_REF,
            assembly.head()
                .materialRef());
        assertEquals(
            WOOD_REF,
            assembly.handle()
                .materialRef());
        assertEquals(
            2,
            assembly.components()
                .size());
        assertEquals(
            MaterialQuantity.ofGrams(388.8)
                .milligrams(),
            assembly.totalMass()
                .milligrams());
    }

    @Test
    void capabilitiesChangeWithMaterialProperties() {
        ComponentInstance copperHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);

        ComponentInstance bronzeHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            MaterialRef.authored(ResourceId.of("naturalexceptions", "bronze")),
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);

        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        MaterialProperties bronzeProps = MaterialProperties.builder()
            .density(8.8)
            .meltingPointKelvin(1220.0)
            .specificHeatSolid(380.0)
            .specificHeatLiquid(400.0)
            .latentHeatOfFusion(200000.0)
            .thermalConductivity(100.0)
            .hardness(6.0) // Much harder than copper (3.0)
            .build();

        DerivedCapability cuCap = new DerivedCapability(
            AssemblyGraph.ofTool(copperHead, handle),
            BuiltinMaterials.COPPER_PROPERTIES,
            BuiltinMaterials.WOOD_PROPERTIES);
        DerivedCapability bronzeCap = new DerivedCapability(
            AssemblyGraph.ofTool(bronzeHead, handle),
            bronzeProps,
            BuiltinMaterials.WOOD_PROPERTIES);

        assertTrue(bronzeCap.penetration() > cuCap.penetration());
        assertTrue(
            bronzeCap.miningSpeed(TargetMaterialProfile.stone()) > cuCap.miningSpeed(TargetMaterialProfile.stone()));
    }

    @Test
    void capabilitiesChangeWithGeometry() {
        ComponentInstance pickHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(250.0),
            ManufacturingState.FINISHED);

        ComponentInstance blade = new ComponentInstance(
            GeometryId.BLADE,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(250.0),
            ManufacturingState.FINISHED);

        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        DerivedCapability pickCap = new DerivedCapability(
            AssemblyGraph.ofTool(pickHead, handle),
            BuiltinMaterials.COPPER_PROPERTIES,
            BuiltinMaterials.WOOD_PROPERTIES);
        DerivedCapability bladeCap = new DerivedCapability(
            AssemblyGraph.ofTool(blade, handle),
            BuiltinMaterials.COPPER_PROPERTIES,
            BuiltinMaterials.WOOD_PROPERTIES);

        // Pick head has concentrated tip geometry multiplier (1.5x)
        assertTrue(pickCap.penetration() > bladeCap.penetration());
    }

    @Test
    void capabilitiesDegradeWithDamage() {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        DerivedCapability cap = new DerivedCapability(
            AssemblyGraph.ofTool(head, handle),
            BuiltinMaterials.COPPER_PROPERTIES,
            BuiltinMaterials.WOOD_PROPERTIES);

        double pristineSpeed = cap.miningSpeed(TargetMaterialProfile.stone());
        double pristinePen = cap.penetration();

        head.damage()
            .addWear(0.50);
        double wornSpeed = cap.miningSpeed(TargetMaterialProfile.stone());
        double wornPen = cap.penetration();

        assertTrue(wornPen < pristinePen);
        assertTrue(wornSpeed < pristineSpeed);
    }

    @Test
    void wearReducesEffectiveEdgeCapability() {
        ComponentDamage dmg = new ComponentDamage();
        assertEquals(1.0, dmg.effectiveMultiplier(), 1.0e-5);

        dmg.addWear(0.40);
        assertEquals(0.84, dmg.effectiveMultiplier(), 1.0e-2);
        assertTrue(dmg.effectiveMultiplier() < 1.0);
    }

    @Test
    void excessiveStressCanPermanentlyDeformComponent() {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF, // Hardness 3.0
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        DerivedCapability cap = new DerivedCapability(
            AssemblyGraph.ofTool(head, handle),
            BuiltinMaterials.COPPER_PROPERTIES,
            BuiltinMaterials.WOOD_PROPERTIES);

        TargetMaterialProfile hardTarget = new TargetMaterialProfile("hard_quartz", 5.0, 3.0);
        cap.interactWith(hardTarget);

        assertTrue(
            head.damage()
                .deformation() > 0.0);
        assertTrue(
            head.damage()
                .cracking() > 0.0);
    }

    @Test
    void cracksReduceFailureMargin() {
        ComponentDamage dmg = new ComponentDamage();
        dmg.addCracking(0.40);
        assertFalse(dmg.isFractured());

        dmg.addCracking(0.60);
        assertTrue(dmg.isFractured());
        assertEquals(0.0, dmg.effectiveMultiplier(), 1.0e-5);
    }

    @Test
    void fracturedComponentStopsProvidingCapability() {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        AssemblyGraph tool = AssemblyGraph.ofTool(head, handle);
        DerivedCapability cap = new DerivedCapability(
            tool,
            BuiltinMaterials.COPPER_PROPERTIES,
            BuiltinMaterials.WOOD_PROPERTIES);

        head.damage()
            .fracture();
        assertTrue(tool.isBroken());
        assertEquals(0.1, cap.penetration(), 1.0e-5);
        assertEquals(1.0, cap.miningSpeed(TargetMaterialProfile.stone()), 1.0e-5);
    }

    @Test
    void assemblyCanBeDisassembled() {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        AssemblyGraph tool = AssemblyGraph.ofTool(head, handle);
        List<ComponentInstance> parts = tool.disassemble();

        assertEquals(2, parts.size());
        assertTrue(parts.contains(head));
        assertTrue(parts.contains(handle));
    }

    @Test
    void survivingComponentsRetainDamageAfterDisassembly() {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        AssemblyGraph tool = AssemblyGraph.ofTool(head, handle);
        DerivedCapability cap = new DerivedCapability(
            tool,
            BuiltinMaterials.COPPER_PROPERTIES,
            BuiltinMaterials.WOOD_PROPERTIES);

        for (int i = 0; i < 10; i++) {
            cap.interactWith(TargetMaterialProfile.copperOre());
        }

        assertTrue(
            head.damage()
                .wear() > 0.0);
        double accumulatedWear = head.damage()
            .wear();

        List<ComponentInstance> parts = tool.disassemble();
        ComponentInstance disassembledHead = parts.get(0);
        assertEquals(
            accumulatedWear,
            disassembledHead.damage()
                .wear(),
            1.0e-6);
    }

    @Test
    void damagedComponentCanBeRepairedWhereAllowed() {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);

        head.damage()
            .addWear(0.30);
        head.damage()
            .addDeformation(0.25);

        head.damage()
            .repairWear(0.20);
        head.damage()
            .repairDeformation(0.15);

        assertEquals(
            0.10,
            head.damage()
                .wear(),
            1.0e-5);
        assertEquals(
            0.10,
            head.damage()
                .deformation(),
            1.0e-5);
    }

    @Test
    void fracturedComponentCanBeReplaced() {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        AssemblyGraph tool = AssemblyGraph.ofTool(head, handle);
        handle.damage()
            .fracture();
        assertTrue(tool.isBroken());

        ComponentInstance newHandle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        tool.replaceHandle(newHandle);
        assertFalse(tool.isBroken());
    }

    @Test
    void componentCanBeRemeltedWithoutMatterLoss() {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        head.damage()
            .addWear(0.80); // Heavily worn

        VesselState vessel = new VesselState();
        vessel.addComponentCharge(head, BuiltinMaterials.COPPER_PROPERTIES);

        // Vessel has 1 solid charge layer of 268.8 g
        assertEquals(
            1,
            vessel.layers()
                .size());
        assertEquals(
            AggregatePhaseType.SOLID_CHARGE,
            vessel.layers()
                .get(0)
                .phaseType());

        // Heat vessel above copper melting point (1358 K)
        vessel.addFuel(2000);
        for (int tick = 0; tick < 200; tick++) {
            vessel.step(10, 300.0);
        }

        assertEquals(
            1,
            vessel.layers()
                .size());
        AggregateLayer molten = vessel.layers()
            .get(0);
        assertEquals(AggregatePhaseType.MOLTEN_METAL, molten.phaseType());
        assertEquals(
            268800L,
            molten.mass()
                .milligrams());
        // Zero slag created for refined metal remelting
        assertTrue(
            vessel.slagLayers()
                .isEmpty());
    }

    @Test
    void undocumentedAlloyCanBecomeFunctionalComponent() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        CanonicalComposition ternaryComp = CanonicalComposition.builder()
            .add(CU, 5700)
            .add(SN, 2300)
            .add(PB, 2000)
            .build();
        ResolvedProperties resolved = resolver
            .resolve(MaterialRef.generated(DeterministicId.derive("mat", "cu57sn23pb20")), ternaryComp);

        CastingMold mold = new CastingMold(ComponentGeometry.pickHead());
        MaterialQuantity alloyMass = MaterialQuantity.ofGrams(500.0);
        AggregateLayer alloyLayer = createMoltenAlloyLayer(
            alloyMass,
            1200.0,
            resolved.properties(),
            MaterialRef.generated(DeterministicId.derive("mat", "cu57sn23pb20")),
            ternaryComp);

        mold.pour(alloyLayer, resolved.properties());
        mold.cool(200.0, 300.0);

        ComponentInstance alloyHead = mold.extractComponent();
        WorkingOperation.FINISH.apply(alloyHead, GeometryId.PICK_HEAD);

        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        AssemblyGraph tool = AssemblyGraph.ofTool(alloyHead, handle);
        DerivedCapability cap = new DerivedCapability(tool, resolved.properties(), BuiltinMaterials.WOOD_PROPERTIES);

        assertTrue(cap.penetration() > 0.0);
        assertTrue(cap.miningSpeed(TargetMaterialProfile.stone()) > 1.0);
    }

    @Test
    void mixedMaterialAssemblyCanBeUsedAsTool() {
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            CU_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(CU)),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        AssemblyGraph tool = AssemblyGraph.ofTool(head, handle);
        DerivedCapability cap = new DerivedCapability(
            tool,
            BuiltinMaterials.COPPER_PROPERTIES,
            BuiltinMaterials.WOOD_PROPERTIES);

        double speedStone = cap.miningSpeed(TargetMaterialProfile.stone());
        double speedOre = cap.miningSpeed(TargetMaterialProfile.copperOre());
        assertTrue(speedStone > 1.0);
        assertTrue(speedOre > 1.0);

        cap.interactWith(TargetMaterialProfile.stone());
        assertTrue(
            head.damage()
                .isDamaged());
    }

    @Test
    void noArtificialMaterialTierRejectsUnknownAlloy() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        CanonicalComposition wildComp = CanonicalComposition.builder()
            .add(CU, 3333)
            .add(SN, 3333)
            .add(PB, 3334)
            .build();
        ResolvedProperties resolved = resolver
            .resolve(MaterialRef.generated(DeterministicId.derive("mat", "wild_ternary")), wildComp);

        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            MaterialRef.generated(DeterministicId.derive("mat", "wild_ternary")),
            CanonicalMaterialKey.of(wildComp),
            MaterialQuantity.ofGrams(270.0),
            ManufacturingState.FINISHED);
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);

        AssemblyGraph tool = AssemblyGraph.ofTool(head, handle);
        DerivedCapability cap = new DerivedCapability(tool, resolved.properties(), BuiltinMaterials.WOOD_PROPERTIES);

        // Tool works against stone and ore without any tier check failure
        double speed = cap.miningSpeed(TargetMaterialProfile.stone());
        assertTrue(speed > 1.0);
        cap.interactWith(TargetMaterialProfile.copperOre());
    }

    @Test
    void fullVerticalAcceptanceChain() {
        // Step 1: Initialize P4 vessel
        VesselState vessel = new VesselState();
        MaterialQuantity cuMass = MaterialQuantity.ofGrams(500.0);
        MaterialQuantity snMass = MaterialQuantity.ofGrams(150.0);
        MaterialQuantity pbMass = MaterialQuantity.ofGrams(100.0);
        MaterialQuantity totalInitialMass = cuMass.add(snMass)
            .add(pbMass);

        MaterialPropertyResolver resolver = new MaterialPropertyResolver();

        AggregateLayer cuLayer = createMoltenCopperLayer(cuMass, 1400.0);
        AggregateLayer snLayer = createMoltenAlloyLayer(
            snMass,
            1400.0,
            BuiltinMaterials.COPPER_PROPERTIES,
            MaterialRef.authored(SN),
            CanonicalComposition.of(SN));
        AggregateLayer pbLayer = createMoltenAlloyLayer(
            pbMass,
            1400.0,
            BuiltinMaterials.COPPER_PROPERTIES,
            MaterialRef.authored(PB),
            CanonicalComposition.of(PB));

        vessel.addLayer(cuLayer);
        vessel.addLayer(snLayer);
        vessel.addLayer(pbLayer);

        // Step 2: Mix molten layers into undocumented alloy
        vessel.step(10, 300.0);
        assertEquals(
            1,
            vessel.layers()
                .size());
        AggregateLayer alloyLayer = vessel.layers()
            .get(0);
        assertEquals(
            totalInitialMass.milligrams(),
            alloyLayer.mass()
                .milligrams());
        ResolvedProperties alloyProps = resolver.resolve(alloyLayer.materialRef(), alloyLayer.composition());

        // Step 3: Pour into pick head casting mold
        CastingMold mold = new CastingMold(ComponentGeometry.pickHead());
        MaterialQuantity poured = mold.pour(alloyLayer, alloyProps.properties());
        assertTrue(mold.isFilled());
        assertEquals(
            poured.milligrams(),
            mold.filledMass()
                .milligrams());
        assertEquals(
            totalInitialMass.subtract(poured)
                .milligrams(),
            alloyLayer.mass()
                .milligrams());

        // Step 4: Cool thermally to solidify
        mold.cool(250.0, 300.0);
        assertTrue(mold.isSolid());

        // Step 5: Extract component as-cast
        ComponentInstance castHead = mold.extractComponent();
        assertEquals(ManufacturingState.AS_CAST, castHead.manufacturingState());
        assertEquals(
            poured.milligrams(),
            castHead.mass()
                .milligrams());

        // Step 6: Work & finish pick head (conserving mass)
        WorkingOperation.FINISH.apply(castHead, GeometryId.PICK_HEAD);
        assertEquals(ManufacturingState.FINISHED, castHead.manufacturingState());
        assertEquals(
            poured.milligrams(),
            castHead.mass()
                .milligrams());

        // Step 7: Craft wood handle & assemble
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            WOOD_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(WOOD)),
            MaterialQuantity.ofGrams(110.0),
            ManufacturingState.FINISHED);

        AssemblyGraph tool = AssemblyGraph.ofTool(castHead, woodHandle);
        assertEquals(
            castHead.mass()
                .add(woodHandle.mass())
                .milligrams(),
            tool.totalMass()
                .milligrams());

        // Step 8: Derive physical capabilities & mine targets
        DerivedCapability capability = new DerivedCapability(
            tool,
            alloyProps.properties(),
            BuiltinMaterials.WOOD_PROPERTIES);
        assertTrue(capability.penetration() > 0.0);
        double initialMiningSpeed = capability.miningSpeed(TargetMaterialProfile.stone());
        assertTrue(initialMiningSpeed > 1.0);

        // Mine stone targets accumulating progressive wear
        for (int i = 0; i < 20; i++) {
            capability.interactWith(TargetMaterialProfile.stone());
        }
        assertTrue(
            castHead.damage()
                .wear() > 0.0);
        assertTrue(capability.miningSpeed(TargetMaterialProfile.stone()) < initialMiningSpeed);

        // Step 9: Disassemble tool
        List<ComponentInstance> parts = tool.disassemble();
        assertEquals(2, parts.size());
        ComponentInstance disassembledHead = parts.get(0);
        assertTrue(
            disassembledHead.damage()
                .wear() > 0.0);
        assertEquals(
            poured.milligrams(),
            disassembledHead.mass()
                .milligrams());

        // Step 10: Remelt worn pick head in P4 vessel without matter loss
        VesselState remelter = new VesselState();
        remelter.addComponentCharge(disassembledHead, alloyProps.properties());
        assertEquals(
            1,
            remelter.layers()
                .size());
        assertEquals(
            AggregatePhaseType.SOLID_CHARGE,
            remelter.layers()
                .get(0)
                .phaseType());

        // Heat above alloy melting point
        remelter.addFuel(2000);
        for (int tick = 0; tick < 200; tick++) {
            remelter.step(10, 300.0);
        }

        assertEquals(
            1,
            remelter.layers()
                .size());
        AggregateLayer remeltedLayer = remelter.layers()
            .get(0);
        assertEquals(AggregatePhaseType.MOLTEN_METAL, remeltedLayer.phaseType());
        // Exact mass conservation: 100% recovered
        assertEquals(
            poured.milligrams(),
            remeltedLayer.mass()
                .milligrams());
        // Alloy composition fully preserved
        assertEquals(alloyLayer.composition(), remeltedLayer.composition());
        // No slag generated from remelting pure metal component
        assertTrue(
            remelter.slagLayers()
                .isEmpty());
    }
}
