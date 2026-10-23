package naturalexceptions.minecraft.multiblock;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * Authoritative multiblock rules and minimum reference layout for the variable Starter Smelter vessel.
 *
 * <p>
 * Marked EXPERIMENTAL as the layout is still in active development.
 */
public final class StarterSmelterDefinition extends MultiblockDefinition {

    public static final StarterSmelterDefinition INSTANCE = new StarterSmelterDefinition();

    public StarterSmelterDefinition() {
        super("starter_smelter", "Starter Smelter", MultiblockMaturity.STABLE, null);
        buildPatterns();
    }

    private void buildPatterns() {
        Block brick = NEBlocks.refractoryBrick != null ? NEBlocks.refractoryBrick
            : new naturalexceptions.minecraft.block.BlockRefractoryBrick();
        Block controller = NEBlocks.starterSmelter != null ? NEBlocks.starterSmelter
            : new naturalexceptions.minecraft.metallurgy.BlockStarterSmelter();
        Block tuyere = NEBlocks.smelterTuyere;
        Block outlet = NEBlocks.smelterOutlet;
        Block castingTable = NEBlocks.castingTable;

        ItemStack brickStack = new ItemStack(brick);
        ItemStack controllerStack = new ItemStack(controller);
        ItemStack tuyereStack = tuyere != null ? new ItemStack(tuyere) : null;
        ItemStack outletStack = outlet != null ? new ItemStack(outlet) : null;
        ItemStack tableStack = castingTable != null ? new ItemStack(castingTable) : null;

        String brickName = "Refractory Brick Block";
        String controllerName = "Starter Smelter Controller";

        // ==========================================
        // Layer 1: Hearth Base (dy = -1, 3x3 solid)
        // ==========================================
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = 0; dz <= 2; dz++) {
                addPattern(
                    MultiblockPattern.ofBlock(dx, -1, dz, MultiblockRole.REQUIRED_BLOCK, brick, brickName, brickStack));
            }
        }

        // ==========================================
        // Layer 2: Crucible Chamber (dy = 0)
        // ==========================================
        // Controller at front-center (0, 0, 0)
        addPattern(
            new MultiblockPattern(
                0,
                0,
                0,
                MultiblockRole.REQUIRED_CONTROLLER,
                controllerName,
                controllerStack,
                (world, x, y, z) -> world != null
                    && world.getBlock(x, y, z) instanceof naturalexceptions.minecraft.metallurgy.BlockStarterSmelter));

        // Center air cavity for crucible hearth (0, 0, 1)
        addPattern(MultiblockPattern.ofAir(0, 0, 1));

        // 7 surrounding crucible chamber refractory walls
        addPattern(MultiblockPattern.ofBlock(-1, 0, 0, MultiblockRole.REQUIRED_BLOCK, brick, brickName, brickStack));
        addPattern(MultiblockPattern.ofBlock(1, 0, 0, MultiblockRole.REQUIRED_BLOCK, brick, brickName, brickStack));
        addPattern(MultiblockPattern.ofBlock(-1, 0, 1, MultiblockRole.REQUIRED_BLOCK, brick, brickName, brickStack));
        addPattern(MultiblockPattern.ofBlock(1, 0, 1, MultiblockRole.REQUIRED_BLOCK, brick, brickName, brickStack));
        addPattern(MultiblockPattern.ofBlock(-1, 0, 2, MultiblockRole.REQUIRED_BLOCK, brick, brickName, brickStack));
        addPattern(MultiblockPattern.ofBlock(0, 0, 2, MultiblockRole.REQUIRED_BLOCK, brick, brickName, brickStack));
        addPattern(MultiblockPattern.ofBlock(1, 0, 2, MultiblockRole.REQUIRED_BLOCK, brick, brickName, brickStack));

        // Optional Tuyere attachment ports (air inlet on exterior faces)
        if (tuyere != null) {
            addPattern(
                new MultiblockPattern(
                    -2,
                    0,
                    1,
                    MultiblockRole.OPTIONAL_ATTACHMENT,
                    "Tuyere (Optional Air Inlet)",
                    tuyereStack,
                    (world, x, y, z) -> world != null && world.getBlock(x, y, z) == tuyere));
            addPattern(
                new MultiblockPattern(
                    2,
                    0,
                    1,
                    MultiblockRole.OPTIONAL_ATTACHMENT,
                    "Tuyere (Optional Air Inlet)",
                    tuyereStack,
                    (world, x, y, z) -> world != null && world.getBlock(x, y, z) == tuyere));
        }

        // Minimum-layout example: the outlet is a lower back wall port, not the controller-front hearth.
        if (outlet != null) {
            addPattern(
                new MultiblockPattern(
                    0,
                    0,
                    3,
                    MultiblockRole.RELATED_EXTERNAL,
                    "Smelter Outlet (Lower Side-Wall Tap)",
                    outletStack,
                    (world, x, y, z) -> world != null && world.getBlock(x, y, z) == outlet));
        }
        if (castingTable != null) {
            addPattern(
                new MultiblockPattern(
                    0,
                    -1,
                    3,
                    MultiblockRole.RELATED_EXTERNAL,
                    "Casting Table (Below Outlet Spout)",
                    tableStack,
                    (world, x, y, z) -> world != null && world.getBlock(x, y, z) == castingTable));
        }

        // ==========================================
        // Layer 3: Upper Rim / Flue (dy = 1, 3x3 ring)
        // ==========================================
        // Center chimney flue
        addPattern(MultiblockPattern.ofAir(0, 1, 1));

        // 8 perimeter ring blocks
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = 0; dz <= 2; dz++) {
                if (dx == 0 && dz == 1) continue; // Skip center chimney
                addPattern(
                    MultiblockPattern.ofBlock(dx, 1, dz, MultiblockRole.REQUIRED_BLOCK, brick, brickName, brickStack));
            }
        }
    }

    /** Runtime cavity discovery owned by the same definition used by the smelter knowledge entry. */
    public SmelterVesselGeometry discoverVessel(net.minecraft.world.IBlockAccess world, int x, int y, int z,
        int facing) {
        return SmelterVesselGeometry.discover(world, x, y, z, facing);
    }

    @Override
    public MultiblockValidationResult validate(net.minecraft.world.IBlockAccess world, int x, int y, int z,
        int facing) {
        return discoverVessel(world, x, y, z, facing) != null ? MultiblockValidationResult.ok()
            : MultiblockValidationResult.fail(
                x,
                y,
                z,
                null,
                "Starter Smelter requires a bounded 3-7 block refractory shell, 1-3 clear cavity layers, and open exhaust.");
    }
}
