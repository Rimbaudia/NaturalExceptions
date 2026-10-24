package naturalexceptions.minecraft.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

class MultiblockArchitectureTest {

    @BeforeAll
    static void setup() {
        BuiltinContent.register();
        NEItems.ensureBootstrapped();
        NEBlocks.register();
        NEItems.register();
        KnowledgeRegistry.instance()
            .init();
    }

    private static final class Pos {

        final int x, y, z;

        Pos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pos)) return false;
            Pos pos = (Pos) o;
            return x == pos.x && y == pos.y && z == pos.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    private IBlockAccess createMockWorld(Map<Pos, Block> blockMap) {
        return createMockWorld(blockMap, java.util.Collections.emptyMap());
    }

    private IBlockAccess createMockWorld(Map<Pos, Block> blockMap, Map<Pos, Integer> metadata) {
        return (IBlockAccess) Proxy.newProxyInstance(
            IBlockAccess.class.getClassLoader(),
            new Class<?>[] { IBlockAccess.class },
            (proxy, method, args) -> {
                if ("getBlock".equals(method.getName())) {
                    int x = (Integer) args[0];
                    int y = (Integer) args[1];
                    int z = (Integer) args[2];
                    Block b = blockMap.get(new Pos(x, y, z));
                    return b != null ? b : Blocks.air;
                }
                if ("isAirBlock".equals(method.getName())) {
                    int x = (Integer) args[0];
                    int y = (Integer) args[1];
                    int z = (Integer) args[2];
                    Block b = blockMap.get(new Pos(x, y, z));
                    return b == null || b == Blocks.air;
                }
                if ("getBlockMetadata".equals(method.getName())) {
                    Integer value = metadata.get(new Pos((Integer) args[0], (Integer) args[1], (Integer) args[2]));
                    return value == null ? 0 : value;
                }
                return null;
            });
    }

    private void placeStructureInWorld(Map<Pos, Block> world, int cx, int cy, int cz, int facing) {
        StarterSmelterDefinition def = StarterSmelterDefinition.INSTANCE;
        for (MultiblockPattern pattern : def.patterns()) {
            int[] pos = MultiblockDefinition.localToWorld(cx, cy, cz, facing, pattern.dx(), pattern.dy(), pattern.dz());
            if (pattern.role() == MultiblockRole.REQUIRED_AIR) {
                world.put(new Pos(pos[0], pos[1], pos[2]), Blocks.air);
            } else if (pattern.role() == MultiblockRole.REQUIRED_CONTROLLER) {
                world.put(new Pos(pos[0], pos[1], pos[2]), NEBlocks.starterSmelter);
            } else if (pattern.role() == MultiblockRole.REQUIRED_BLOCK) {
                world.put(new Pos(pos[0], pos[1], pos[2]), NEBlocks.refractoryBrick);
            }
        }
    }

    private void placeVariableVessel(Map<Pos, Block> map, int outerWidth, int outerDepth, int height) {
        placeVariableVessel(map, outerWidth, outerDepth, height, 2);
    }

    private void placeVariableVessel(Map<Pos, Block> map, int outerWidth, int outerDepth, int height, int facing) {
        int half = outerWidth / 2;
        for (int x = -half; x <= half; x++) for (int z = 0; z < outerDepth; z++) {
            int[] floor = MultiblockDefinition.localToWorld(0, 64, 0, facing, x, -1, z);
            map.put(new Pos(floor[0], floor[1], floor[2]), NEBlocks.refractoryBrick);
        }
        for (int y = 0; y <= height; y++) for (int x = -half; x <= half; x++) for (int z = 0; z < outerDepth; z++) {
            boolean edge = x == -half || x == half || z == 0 || z == outerDepth - 1;
            boolean controller = x == 0 && z == 0 && y == 0;
            if (!edge) continue;
            int[] pos = MultiblockDefinition.localToWorld(0, 64, 0, facing, x, y, z);
            map.put(new Pos(pos[0], pos[1], pos[2]), controller ? NEBlocks.starterSmelter : NEBlocks.refractoryBrick);
        }
    }

    @Test
    void variableVesselDiscoveryAcceptsMinimumAndLargerRectangularCavities() {
        Map<Pos, Block> minimum = new HashMap<>();
        placeVariableVessel(minimum, 3, 3, 1);
        SmelterVesselGeometry minGeometry = StarterSmelterDefinition.INSTANCE
            .discoverVessel(createMockWorld(minimum), 0, 64, 0, 2);
        assertNotNull(minGeometry);
        assertEquals(1, minGeometry.cavityCellCount());
        assertEquals(25.0, minGeometry.capacityLitres(), 1.0e-8);
        assertEquals(0.5, minGeometry.fillFractionForVolume(12.5), 1.0e-8);
        assertEquals(0.5, minGeometry.heightForVolume(12.5), 1.0e-8);
        assertEquals(1.0, minGeometry.fillFractionForVolume(100.0), 1.0e-8);

        Map<Pos, Block> larger = new HashMap<>();
        placeVariableVessel(larger, 5, 5, 2);
        SmelterVesselGeometry largeGeometry = StarterSmelterDefinition.INSTANCE
            .discoverVessel(createMockWorld(larger), 0, 64, 0, 2);
        assertNotNull(largeGeometry);
        assertEquals(18, largeGeometry.cavityCellCount());
        assertTrue(largeGeometry.capacityLitres() > minGeometry.capacityLitres());
    }

    @Test
    void aColossusCrucibleStandsInTheShellForABrickAndIsCounted() {
        Map<Pos, Block> plain = new HashMap<>();
        placeVariableVessel(plain, 3, 3, 1);
        assertEquals(
            0,
            StarterSmelterDefinition.INSTANCE.discoverVessel(createMockWorld(plain), 0, 64, 0, 2)
                .crucibles());
        Map<Pos, Block> withCrucible = new HashMap<>();
        placeVariableVessel(withCrucible, 3, 3, 1);
        int[] wall = MultiblockDefinition.localToWorld(0, 64, 0, 2, 1, 0, 1);
        withCrucible.put(new Pos(wall[0], wall[1], wall[2]), NEBlocks.colossusCrucible);
        SmelterVesselGeometry geometry = StarterSmelterDefinition.INSTANCE
            .discoverVessel(createMockWorld(withCrucible), 0, 64, 0, 2);
        assertNotNull(geometry, "a crucible in place of a wall brick still makes a vessel");
        assertEquals(1, geometry.crucibles());
        assertEquals(1, geometry.cavityCellCount());
    }

    @Test
    void anElectricHeaterStandsInTheShellForABrick() {
        Map<Pos, Block> withHeater = new HashMap<>();
        placeVariableVessel(withHeater, 3, 3, 1);
        int[] wall = MultiblockDefinition.localToWorld(0, 64, 0, 2, -1, 0, 1);
        withHeater.put(new Pos(wall[0], wall[1], wall[2]), NEBlocks.electricHeater);
        SmelterVesselGeometry geometry = StarterSmelterDefinition.INSTANCE
            .discoverVessel(createMockWorld(withHeater), 0, 64, 0, 2);
        assertNotNull(geometry, "a heater in place of a wall brick still makes a vessel");
        assertTrue(geometry.inShell(wall[0], wall[1], wall[2]), "and it is in the shell");
        assertTrue(!geometry.inShell(0, 64, 1), "the cavity is not");
    }

    @Test
    void variableVesselDiscoveryRejectsBlockedCavityAndMissingFloor() {
        Map<Pos, Block> blocked = new HashMap<>();
        placeVariableVessel(blocked, 3, 3, 1);
        blocked.put(new Pos(0, 64, 1), NEBlocks.refractoryBrick);
        assertEquals(null, StarterSmelterDefinition.INSTANCE.discoverVessel(createMockWorld(blocked), 0, 64, 0, 2));

        Map<Pos, Block> floorless = new HashMap<>();
        placeVariableVessel(floorless, 3, 3, 1);
        floorless.remove(new Pos(0, 63, 1));
        assertEquals(null, StarterSmelterDefinition.INSTANCE.discoverVessel(createMockWorld(floorless), 0, 64, 0, 2));
    }

    @Test
    void tuyerePortOrientationUsesControllerLocalFrameForAllFourFacings() {
        for (int facing : new int[] { 2, 3, 4, 5 }) {
            Map<Pos, Block> blocks = new HashMap<>();
            placeVariableVessel(blocks, 3, 3, 1, facing);
            SmelterVesselGeometry geometry = StarterSmelterDefinition.INSTANCE
                .discoverVessel(createMockWorld(blocks), 0, 64, 0, facing);
            assertNotNull(geometry, "Expected vessel facing " + facing);
            int[] wall = geometry.world(1, 0, 1);
            int[] outer = geometry.world(2, 0, 1);
            int inward = naturalexceptions.minecraft.metallurgy.SmelterPortLookup
                .directionFor(wall[0] - outer[0], wall[2] - outer[2]);
            Pos tuyerePos = new Pos(outer[0], outer[1], outer[2]);
            blocks.put(tuyerePos, NEBlocks.smelterTuyere);
            Map<Pos, Integer> metadata = new HashMap<>();
            metadata.put(tuyerePos, inward);
            assertTrue(
                naturalexceptions.minecraft.metallurgy.SmelterPortLookup
                    .isTuyerePort(createMockWorld(blocks, metadata), geometry, outer[0], outer[1], outer[2]),
                "Nozzle must face inward for controller orientation " + facing);
            metadata.put(tuyerePos, inward ^ 1);
            assertFalse(
                naturalexceptions.minecraft.metallurgy.SmelterPortLookup
                    .isTuyerePort(createMockWorld(blocks, metadata), geometry, outer[0], outer[1], outer[2]),
                "Outward nozzle must be rejected for controller orientation " + facing);
        }
    }

    @Test
    void outletPortsAreLowerHorizontalWallsAndFaceOutwardInEveryRotation() {
        for (int facing : new int[] { 2, 3, 4, 5 }) {
            Map<Pos, Block> blocks = new HashMap<>();
            placeVariableVessel(blocks, 3, 3, 1, facing);
            SmelterVesselGeometry geometry = StarterSmelterDefinition.INSTANCE
                .discoverVessel(createMockWorld(blocks), 0, 64, 0, facing);
            int[] wall = geometry.world(1, 0, 1);
            int[] port = geometry.world(2, 0, 1);
            int outward = naturalexceptions.minecraft.metallurgy.SmelterPortLookup
                .directionFor(port[0] - wall[0], port[2] - wall[2]);
            assertTrue(
                naturalexceptions.minecraft.metallurgy.SmelterPortLookup
                    .isOutletPort(geometry, port[0], port[1], port[2], outward));
            assertFalse(
                naturalexceptions.minecraft.metallurgy.SmelterPortLookup
                    .isOutletPort(geometry, port[0], port[1], port[2], outward ^ 1));
            int[] formerHearthPort = geometry.world(0, 0, -1);
            int[] controllerFront = geometry.world(0, 0, 0);
            int frontFacing = naturalexceptions.minecraft.metallurgy.SmelterPortLookup
                .directionFor(formerHearthPort[0] - controllerFront[0], formerHearthPort[2] - controllerFront[2]);
            assertFalse(
                naturalexceptions.minecraft.metallurgy.SmelterPortLookup
                    .isOutletPort(geometry, formerHearthPort[0], formerHearthPort[1], formerHearthPort[2], frontFacing),
                "Former controller-front attachment is not a lower side-wall tap.");
        }
    }

    @Test
    void testAuthoritativeBlockCountsDerivedDynamically() {
        StarterSmelterDefinition def = StarterSmelterDefinition.INSTANCE;
        Map<String, Integer> counts = def.getRequiredCountsByName();

        // Must derive exactly 24 Refractory Bricks and 1 Controller
        assertEquals(2, counts.size(), "Should have exactly 2 required material types");
        assertEquals(
            24,
            counts.get("Refractory Brick Block")
                .intValue());
        assertEquals(
            1,
            counts.get("Starter Smelter Controller")
                .intValue());

        List<MultiblockDefinition.RequiredBlockEntry> entries = def.getRequiredBlockEntries();
        assertEquals(2, entries.size());

        int totalBlocks = entries.stream()
            .mapToInt(MultiblockDefinition.RequiredBlockEntry::count)
            .sum();
        assertEquals(25, totalBlocks, "Total required structural blocks must be exactly 25 (24 bricks + 1 controller)");
    }

    @Test
    void testOptionalAttachmentsAndCavitiesExcludedFromRequiredCounts() {
        StarterSmelterDefinition def = StarterSmelterDefinition.INSTANCE;
        Map<String, Integer> counts = def.getRequiredCountsByName();

        // Optional tuyere and related outlet/casting table must NOT be counted as required
        assertFalse(counts.containsKey("Tuyere (Optional Air Inlet)"));
        assertFalse(counts.containsKey("Smelter Outlet (Pouring Spout)"));
        assertFalse(counts.containsKey("Casting Table (Mold Station)"));
        assertFalse(counts.containsKey("Air"));
    }

    @Test
    void testMaturityIsStableWithAuthoritativeNotice() {
        StarterSmelterDefinition def = StarterSmelterDefinition.INSTANCE;
        assertFalse(
            def.maturity()
                .isExperimental(),
            "the Starter Smelter's layout is settled");
        assertEquals(MultiblockMaturity.STABLE, def.maturity());
        assertEquals(MultiblockMaturity.STABLE.notice(), def.maturityNotice());
    }

    @Test
    void testAuthoritativeValidationSucceedsAcrossAllFourFacings() {
        StarterSmelterDefinition def = StarterSmelterDefinition.INSTANCE;
        int[] facings = { 2, 3, 4, 5 }; // North, South, West, East

        for (int facing : facings) {
            Map<Pos, Block> blockMap = new HashMap<>();
            placeStructureInWorld(blockMap, 100, 64, 100, facing);
            IBlockAccess world = createMockWorld(blockMap);

            MultiblockValidationResult result = def.validate(world, 100, 64, 100, facing);
            assertTrue(result.isValid(), "Validation should succeed for facing " + facing);
            assertFalse(result.isFailed());
        }
    }

    @Test
    void testAuthoritativeValidationFailsWhenMissingBaseBrick() {
        StarterSmelterDefinition def = StarterSmelterDefinition.INSTANCE;
        Map<Pos, Block> blockMap = new HashMap<>();
        placeStructureInWorld(blockMap, 0, 64, 0, 2);

        // Remove one brick in the base layer (dx=-1, dy=-1, dz=0) -> world (x=-1, y=63, z=0)
        blockMap.remove(new Pos(-1, 63, 0));
        IBlockAccess world = createMockWorld(blockMap);

        MultiblockValidationResult result = def.validate(world, 0, 64, 0, 2);
        assertFalse(result.isValid(), "Validation must fail when base brick is missing");
        assertTrue(result.isFailed());
        assertTrue(
            result.failureReason()
                .contains("bounded"));
    }

    @Test
    void testAuthoritativeValidationFailsWhenAirCavityObstructed() {
        StarterSmelterDefinition def = StarterSmelterDefinition.INSTANCE;
        Map<Pos, Block> blockMap = new HashMap<>();
        placeStructureInWorld(blockMap, 0, 64, 0, 2);

        // Place solid brick inside chimney flue (dx=0, dy=1, dz=1) -> world (x=0, y=65, z=1)
        blockMap.put(new Pos(0, 65, 1), NEBlocks.refractoryBrick);
        IBlockAccess world = createMockWorld(blockMap);

        MultiblockValidationResult result = def.validate(world, 0, 64, 0, 2);
        assertFalse(result.isValid(), "Validation must fail when chimney flue is obstructed");
        assertTrue(
            result.failureReason()
                .contains("clear cavity"));
    }

    @Test
    void testKnowledgeBrowserAndGameplayShareIdenticalDefinition() {
        KnowledgeEntry entry = KnowledgeRegistry.instance()
            .getEntry("starter_smelter");
        assertNotNull(entry, "starter_smelter entry must be registered in KnowledgeRegistry");
        assertTrue(entry.hasMultiblock(), "Entry must have multiblock attached");
        assertSame(
            StarterSmelterDefinition.INSTANCE,
            entry.multiblock(),
            "KnowledgeEntry and gameplay MUST share identical definition singleton");
        assertEquals(MultiblockMaturity.STABLE, entry.maturity());
        assertFalse(entry.isExperimental(), "the browser shows no EXPERIMENTAL banner");
    }
}
