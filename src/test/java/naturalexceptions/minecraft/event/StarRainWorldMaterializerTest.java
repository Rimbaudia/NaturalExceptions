package naturalexceptions.minecraft.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import naturalexceptions.core.fluid.FluidCellKey;
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
        assertEquals(NEBlocks.fallenStarCell, world.getBlock(impact.x(), 64, impact.z()));
        FluidParcel parcel = saved.fluids()
            .parcelAt(new FluidCellKey(0, impact.x(), 64, impact.z()));
        assertNotNull(parcel);
        assertEquals(impact.id(), parcel.provenanceId());
        assertEquals(
            originalMass,
            parcel.mass()
                .milligrams());
        assertEquals(
            0L,
            impact.stellarMass()
                .milligrams());
        assertEquals(1, world.fluidPlacements);
        assertFalse(materializer.materialize(world, saved, impact));
        assertEquals(1, world.fluidPlacements);
        assertEquals(
            originalMass,
            saved.fluids()
                .totalMass()
                .milligrams());
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
