package naturalexceptions.minecraft.fabrication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/** The Assembly and Mold Assembly screen textures line up with the slots and tiles the code places on them. */
final class WorkbenchScreenArtTest {

    private static BufferedImage texture(String name) throws Exception {
        return ImageIO.read(new File("src/main/resources/assets/naturalexceptions/textures/gui/" + name + ".png"));
    }

    private static void assertSlotFrame(BufferedImage image, int x, int y, String what) {
        assertEquals(0xFF373737, image.getRGB(x - 1, y - 1), what + " frame corner");
        assertEquals(0xFF8B8B8B, image.getRGB(x + 8, y + 8), what + " slot fill");
    }

    @Test
    void assemblySlotsMatchTheTexture() throws Exception {
        BufferedImage image = texture("assembly_table");
        assertSlotFrame(image, ContainerAssemblyTable.HEAD_SLOT_X, ContainerAssemblyTable.RECIPE_SLOT_Y, "head");
        assertSlotFrame(image, ContainerAssemblyTable.HANDLE_SLOT_X, ContainerAssemblyTable.RECIPE_SLOT_Y, "handle");
        assertEquals(
            0xFF8B8B8B,
            image.getRGB(ContainerAssemblyTable.RESULT_SLOT_X + 8, ContainerAssemblyTable.RECIPE_SLOT_Y + 8),
            "result");
        assertEquals(0xFFFFFFFF, image.getRGB(176 + 3, 7), "filled arrow sprite beside the screen art");
    }

    @Test
    void moldTilesSitBetweenMachineAndInventory() throws Exception {
        BufferedImage image = texture("mold_assembly_table");
        assertSlotFrame(image, MoldAssemblyLayout.INPUT_X, MoldAssemblyLayout.MACHINE_Y, "input");
        assertSlotFrame(image, MoldAssemblyLayout.OUTPUT_X, MoldAssemblyLayout.MACHINE_Y, "output");
        int shapes = TileEntityMoldAssemblyTable.selectableGeometries()
            .size();
        int[] first = MoldAssemblyLayout.tile(0);
        int[] last = MoldAssemblyLayout.tile(shapes - 1);
        assertTrue(first[0] >= 7, "first tile inside the panel");
        assertTrue(last[0] + MoldAssemblyLayout.TILE_SIZE <= MoldAssemblyLayout.WIDTH - 7, "last tile inside");
        assertEquals(MoldAssemblyLayout.WIDTH - (last[0] + MoldAssemblyLayout.TILE_SIZE), first[0], "row centred");
        assertTrue(first[1] > MoldAssemblyLayout.MACHINE_Y + 17, "below the machine slots");
        assertTrue(
            MoldAssemblyLayout.INSTRUCTION_Y >= first[1] + MoldAssemblyLayout.TILE_SIZE,
            "instruction under the tiles");
        assertTrue(MoldAssemblyLayout.INSTRUCTION_Y + 9 <= MoldAssemblyLayout.PLAYER_Y - 11, "above the label");
    }

    @Test
    void everyMoldShapeHasItsSpritesAndItsOwnVolume() throws Exception {
        java.util.Set<Double> volumes = new java.util.HashSet<>();
        for (naturalexceptions.core.fabrication.GeometryId shape : naturalexceptions.minecraft.item.ItemCastingMold
            .shapes()) {
            for (String name : naturalexceptions.minecraft.item.ItemCastingMoldAccess.spriteNames(shape)) {
                File file = new File(
                    "src/main/resources/assets/naturalexceptions/textures/items/"
                        + name.substring("naturalexceptions:".length())
                        + ".png");
                assertTrue(file.isFile(), shape + ": " + name);
            }
            volumes.add(
                naturalexceptions.core.fabrication.ComponentGeometry.forId(shape)
                    .volumeCm3());
        }
        assertTrue(volumes.size() >= 5, "shapes hold different amounts of metal");
    }
}
