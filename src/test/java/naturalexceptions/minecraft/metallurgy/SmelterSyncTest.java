package naturalexceptions.minecraft.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * The smelter syncs to watching players only when its saved state has moved (TileSync.toWatchersIfChanged). That is
 * only quiet if an idle smelter's state stays exactly the same tick to tick: nothing in it may count ticks while cold.
 */
final class SmelterSyncTest {

    @BeforeAll
    static void init() {
        try {
            net.minecraft.init.Bootstrap.func_151354_b();
        } catch (Throwable ignored) {}
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static NBTTagCompound saved(TileEntityStarterSmelter smelter) {
        NBTTagCompound tag = new NBTTagCompound();
        smelter.writeToNBT(tag);
        return tag;
    }

    @Test
    void aColdSmelterStaysExactlyTheSameSoItSendsNothing() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        smelter.updateEntity();
        NBTTagCompound before = saved(smelter);
        for (int i = 0; i < 200; i++) smelter.updateEntity();
        assertEquals(before, saved(smelter), "an idle smelter's state must not change, or it would sync forever");
    }

    @Test
    void aLitSmelterChangesSoPlayersSeeItWork() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        assertTrue(smelter.insertFuel(new ItemStack(Items.coal, 4)), "coal is fuel");
        smelter.updateEntity();
        NBTTagCompound before = saved(smelter);
        for (int i = 0; i < 40; i++) smelter.updateEntity();
        assertNotEquals(before, saved(smelter), "a burning smelter's state moves, so it does sync");
    }
}
