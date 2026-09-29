package naturalexceptions.minecraft.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

final class GearTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static ItemStack mask() {
        return new ItemStack(NEItems.exposureMask);
    }

    @Test
    void nineteenSlotsOneForEachPartTwoGlovesTwoShoesTenRings() {
        Map<GearKind, Integer> counts = new EnumMap<>(GearKind.class);
        for (GearSlot slot : GearSlot.values()) counts.merge(slot.kind(), 1, Integer::sum);
        assertEquals(19, GearSlot.COUNT);
        assertEquals(10, (int) counts.get(GearKind.RING));
        assertEquals(2, (int) counts.get(GearKind.GLOVE));
        assertEquals(2, (int) counts.get(GearKind.SHOE));
        for (GearKind kind : new GearKind[] { GearKind.HAT, GearKind.FACE, GearKind.NECK, GearKind.BACK,
            GearKind.BELT }) assertEquals(1, (int) counts.get(kind), kind.name());
    }

    @Test
    void slotsTakeOnlyTheirOwnKind() {
        PlayerGear gear = new PlayerGear();
        assertTrue(gear.isItemValidForSlot(GearSlot.FACE.ordinal(), mask()));
        assertFalse(gear.isItemValidForSlot(GearSlot.HAT.ordinal(), mask()));
        assertFalse(gear.isItemValidForSlot(GearSlot.RING_LEFT_INDEX.ordinal(), mask()));
        assertFalse(gear.isItemValidForSlot(GearSlot.FACE.ordinal(), new ItemStack(Items.apple)));
        assertEquals(1, gear.getInventoryStackLimit());
    }

    @Test
    void wearingFillsTheFreeSlotAndSurvivesSaving() {
        PlayerGear gear = new PlayerGear();
        ItemStack two = mask();
        two.stackSize = 2;
        assertTrue(gear.wear(two));
        assertEquals(1, gear.get(GearSlot.FACE).stackSize, "one worn, not the whole stack");
        assertFalse(gear.wear(mask()), "only one face");
        assertNotNull(gear.worn(GearKind.FACE));
        assertFalse(gear.wear(new ItemStack(Items.apple)));

        NBTTagCompound saved = new NBTTagCompound();
        gear.saveNBTData(saved);
        PlayerGear loaded = new PlayerGear();
        loaded.loadNBTData(saved);
        assertNotNull(loaded.get(GearSlot.FACE));
        assertEquals(
            NEItems.exposureMask,
            loaded.get(GearSlot.FACE)
                .getItem());
        for (GearSlot slot : GearSlot.values()) if (slot != GearSlot.FACE) assertNull(loaded.get(slot));
    }

    @Test
    void faceChangesAreReportedForOtherPlayersToSee() {
        PlayerGear gear = new PlayerGear();
        gear.consumeAppearanceDirty();
        gear.setInventorySlotContents(GearSlot.RING_RIGHT_LITTLE.ordinal(), null);
        assertFalse(gear.consumeAppearanceDirty(), "rings are not drawn");
        gear.wear(mask());
        assertTrue(gear.consumeAppearanceDirty());
        assertFalse(gear.consumeAppearanceDirty());
        assertEquals(
            1,
            gear.takeAll()
                .size());
        assertTrue(gear.consumeAppearanceDirty());
        assertNull(gear.worn(GearKind.FACE));
    }

    @Test
    void screenSlotsDoNotOverlapAndMatchTheTexture() throws Exception {
        Set<Long> cells = new HashSet<>();
        File texture = new File("src/main/resources/assets/naturalexceptions/textures/gui/gear.png");
        java.awt.image.BufferedImage image = ImageIO.read(texture);
        for (GearSlot slot : GearSlot.values()) {
            int[] at = GearLayout.slot(slot);
            assertTrue(at[0] >= 4 && at[0] + 17 <= GearLayout.WIDTH - 4, slot + " inside horizontally");
            assertTrue(at[1] >= 4 && at[1] + 17 <= GearLayout.PLAYER_INVENTORY_Y - 2, slot + " above the inventory");
            assertTrue(cells.add(((long) at[0] << 32) | at[1]), slot + " has its own place");
            int[] figure = GearLayout.FIGURE;
            boolean clear = at[0] + 17 <= figure[0] || at[0] - 1 >= figure[0] + figure[2]
                || at[1] + 17 <= figure[1]
                || at[1] - 1 >= figure[1] + figure[3];
            assertTrue(clear, slot + " clear of the figure window");
            // The drawn frame: dark top-left corner, slot fill inside.
            assertEquals(0xFF373737, image.getRGB(at[0] - 1, at[1] - 1), slot + " frame corner");
            assertEquals(0xFF8B8B8B, image.getRGB(at[0] + 8, at[1] + 8), slot + " slot fill");
        }
        assertEquals(0xFF8B8B8B, image.getRGB(8 + 8, GearLayout.HOTBAR_Y + 8), "hotbar drawn");
    }

    @Test
    void shiftClickRoutesAMaskToTheFaceSlot() {
        PlayerGear gear = new PlayerGear();
        assertEquals(GearSlot.FACE.ordinal(), ContainerGear.freeGearSlotFor(gear, mask()));
        gear.wear(mask());
        assertEquals(-1, ContainerGear.freeGearSlotFor(gear, mask()), "face already taken");
        assertEquals(-1, ContainerGear.freeGearSlotFor(gear, new ItemStack(Items.apple)));
    }
}
