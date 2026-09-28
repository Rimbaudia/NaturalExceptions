package naturalexceptions.minecraft.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.event.EventInstanceFactory;
import naturalexceptions.core.event.EventRecord;
import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.core.event.WorldEventSchedule;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.persistence.WorldMetadata;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.minecraft.registry.NEBlocks;

final class StarRainWorldMaterializerTest {

    @Test
    void unloadedImpactDoesNotLoadChunkAndLoadedRetryMaterializesSameMassOnlyOnce() throws Exception {
        NEBlocks.register();
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(521L, 2, 10L, 0L, 0L, 100L);
        WorldEventSchedule schedule = new WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        NEWorldSavedData saved = new NEWorldSavedData();
        saved.setMetadata(WorldMetadata.createNew("p11-materializer-test", "N0.01.0-dev"));
        saved.p11()
            .synchronize(schedule.snapshot(), 30, -30);
        StarRainImpact impact = saved.p11()
            .dueImpacts(Long.MAX_VALUE, 1)
            .get(0);
        saved.p11()
            .resolve(impact.id());
        long originalMass = impact.stellarMass()
            .milligrams();
        FakeWorld world = FakeWorld.create();
        StarRainWorldMaterializer materializer = new StarRainWorldMaterializer();

        world.loaded = false;
        assertFalse(materializer.materialize(world, saved, impact));
        assertEquals(0, world.fluidPlacements);
        assertFalse(impact.isMaterialized());
        assertEquals(
            originalMass,
            impact.stellarMass()
                .milligrams());

        world.loaded = true;
        assertTrue(materializer.materialize(world, saved, impact));
        naturalexceptions.core.event.StarRainCraterPlan plan = naturalexceptions.core.event.StarRainCraterPlan
            .of(impact.severity(), naturalexceptions.core.event.StarRainCraterPlan.seedOf(impact.id()));
        int coreY = 64 + plan.ballCenterDy();
        assertEquals(
            NEBlocks.fallenStarCell,
            world.getBlock(impact.x(), coreY, impact.z()),
            "molten core at the heart");
        assertEquals(
            NEBlocks.impactCrust,
            world.getBlock(impact.x(), coreY + (impact.severity() >= 3 ? 2 : 1), impact.z()),
            "fused crust shell above it");
        assertEquals(Blocks.air, world.getBlock(impact.x() + plan.radius() - 1, 64, impact.z()), "crater cleared");
        long stellar = 0L;
        int crust = 0;
        for (FluidParcel parcel : saved.fluids()
            .snapshotCells()
            .values()) {
            assertEquals(impact.id(), parcel.provenanceId());
            if (parcel.materialRef()
                .equals(naturalexceptions.content.material.BuiltinMaterials.STELLAR_MATTER.ref())) {
                stellar += parcel.mass()
                    .milligrams();
            } else crust++;
        }
        assertEquals(originalMass, stellar, "all stellar matter is in the core, none lost or created");
        assertEquals(
            plan.cellsWithRole(naturalexceptions.core.event.StarRainCraterPlan.Role.CRUST)
                .size(),
            crust);
        assertEquals(
            0L,
            impact.stellarMass()
                .milligrams());
        int placements = world.fluidPlacements;
        assertEquals(
            plan.cellsWithRole(naturalexceptions.core.event.StarRainCraterPlan.Role.CORE)
                .size(),
            placements);
        assertFalse(materializer.materialize(world, saved, impact));
        assertEquals(placements, world.fluidPlacements, "materializing twice does nothing");
    }

    @Test
    void craterNeverDestroysTileEntitiesBedrockOrParcelHosts() {
        NEBlocks.register();
        assertTrue(StarRainWorldMaterializer.carvable(Blocks.stone, false, 1.5F));
        assertTrue(
            StarRainWorldMaterializer.carvable(Blocks.planks, false, 2.0F),
            "builds in the blast zone are damaged");
        assertFalse(StarRainWorldMaterializer.carvable(Blocks.chest, true, 2.5F), "tile entities are spared");
        assertFalse(StarRainWorldMaterializer.carvable(Blocks.bedrock, false, -1.0F));
        assertFalse(StarRainWorldMaterializer.carvable(Blocks.obsidian, false, -1.0F), "unbreakable stays");
        assertFalse(
            StarRainWorldMaterializer.carvable(NEBlocks.fluidCell, false, 0.1F),
            "other parcels are never erased");
    }

    private static final class FakeWorld extends World {

        private IChunkProvider providerFixture;
        private Map<String, Block> blocks;
        private boolean loaded;
        private int fluidPlacements;

        private FakeWorld() {
            super(
                null,
                "p11-test",
                new WorldProviderSurface(),
                new WorldSettings(0L, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT),
                new Profiler());
        }

        static FakeWorld create() throws Exception {
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            FakeWorld world = (FakeWorld) unsafe.allocateInstance(FakeWorld.class);
            world.blocks = new HashMap<>();
            java.lang.reflect.Field providerField = World.class.getField("provider");
            providerField.setAccessible(true);
            providerField.set(world, new WorldProviderSurface());
            world.providerFixture = (IChunkProvider) Proxy.newProxyInstance(
                IChunkProvider.class.getClassLoader(),
                new Class<?>[] { IChunkProvider.class },
                (proxy, method, args) -> {
                    if ("chunkExists".equals(method.getName())) return world.loaded;
                    throw new AssertionError("Unexpected forced chunk operation: " + method.getName());
                });
            return world;
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return providerFixture;
        }

        @Override
        protected int func_152379_p() {
            return 8;
        }

        @Override
        public Entity getEntityByID(int id) {
            return null;
        }

        @Override
        public IChunkProvider getChunkProvider() {
            return providerFixture;
        }

        @Override
        public int getTopSolidOrLiquidBlock(int x, int z) {
            return 65;
        }

        @Override
        public int getHeightValue(int x, int z) {
            return 65;
        }

        @Override
        public net.minecraft.world.Explosion newExplosion(Entity entity, double x, double y, double z, float size,
            boolean flaming, boolean smoking) {
            return null;
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            Block changed = blocks.get(key(x, y, z));
            return changed != null ? changed : y == 64 ? Blocks.stone : Blocks.air;
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            return null;
        }

        @Override
        public boolean setBlock(int x, int y, int z, Block block, int metadata, int flags) {
            blocks.put(key(x, y, z), block);
            if (block == NEBlocks.fallenStarCell) fluidPlacements++;
            return true;
        }

        @Override
        public boolean setBlockToAir(int x, int y, int z) {
            blocks.put(key(x, y, z), Blocks.air);
            return true;
        }

        @Override
        public boolean isAirBlock(int x, int y, int z) {
            return getBlock(x, y, z) == Blocks.air;
        }

        @Override
        public void playSoundEffect(double x, double y, double z, String name, float volume, float pitch) {}

        @Override
        public <T> java.util.List<T> getEntitiesWithinAABB(Class<T> type, AxisAlignedBB bounds) {
            return Collections.emptyList();
        }

        private static String key(int x, int y, int z) {
            return x + ":" + y + ":" + z;
        }
    }
}
