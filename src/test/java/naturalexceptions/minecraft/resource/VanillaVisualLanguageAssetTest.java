package naturalexceptions.minecraft.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

public final class VanillaVisualLanguageAssetTest {

    private static final File ASSETS_DIR = new File("src/main/resources/assets/naturalexceptions");
    private static final File BLOCKS_DIR = new File(ASSETS_DIR, "textures/blocks");
    private static final File ITEMS_DIR = new File(ASSETS_DIR, "textures/items");

    /** A vertical strip of 16x16 frames with an animation descriptor is 16 pixels high per frame. */
    private static int frameHeight(File file, BufferedImage img) {
        boolean animated = new File(file.getPath() + ".mcmeta").isFile();
        return animated && img.getHeight() % img.getWidth() == 0 ? img.getWidth() : img.getHeight();
    }

    @Test
    void allBlockTexturesAreStrictly16x16() throws IOException {
        assertTrue(BLOCKS_DIR.exists() && BLOCKS_DIR.isDirectory(), "Blocks directory must exist");
        File[] files = BLOCKS_DIR.listFiles((dir, name) -> name.endsWith(".png"));
        assertNotNull(files);
        assertTrue(files.length > 0, "Blocks directory must not be empty");

        for (File f : files) {
            BufferedImage img = ImageIO.read(f);
            assertNotNull(img, "Must read image: " + f.getName());
            assertEquals(16, img.getWidth(), "Block texture width must be 16: " + f.getName());
            assertEquals(16, frameHeight(f, img), "Block texture frame height must be 16: " + f.getName());
        }
    }

    @Test
    void allItemTexturesAreStrictly16x16() throws IOException {
        assertTrue(ITEMS_DIR.exists() && ITEMS_DIR.isDirectory(), "Items directory must exist");
        File[] files = ITEMS_DIR.listFiles((dir, name) -> name.endsWith(".png"));
        assertNotNull(files);
        assertTrue(files.length > 0, "Items directory must not be empty");

        for (File f : files) {
            BufferedImage img = ImageIO.read(f);
            assertNotNull(img, "Must read image: " + f.getName());
            assertEquals(16, img.getWidth(), "Item texture width must be 16: " + f.getName());
            assertEquals(16, img.getHeight(), "Item texture height must be 16: " + f.getName());
        }
    }

    @Test
    void noAccidentalHdTexturesInAuditedMetallurgySet() throws IOException {
        List<File> allTextures = new ArrayList<>();
        if (BLOCKS_DIR.exists()) {
            File[] bf = BLOCKS_DIR.listFiles((dir, name) -> name.endsWith(".png"));
            if (bf != null) allTextures.addAll(Arrays.asList(bf));
        }
        if (ITEMS_DIR.exists()) {
            File[] ifiles = ITEMS_DIR.listFiles((dir, name) -> name.endsWith(".png"));
            if (ifiles != null) allTextures.addAll(Arrays.asList(ifiles));
        }

        for (File f : allTextures) {
            BufferedImage img = ImageIO.read(f);
            assertNotNull(img, "Must read image: " + f.getName());
            assertTrue(
                img.getWidth() <= 16 && frameHeight(f, img) <= 16,
                "Texture " + f.getName() + " is HD (" + img.getWidth() + "x" + img.getHeight() + "), expected 16x16");
        }
    }

    @Test
    void solidBlockTexturesHaveFullyOpaqueAlpha() throws IOException {
        String[] solidBlocks = { "refractory_brick_block.png", "starter_smelter_side.png", "starter_smelter_top.png",
            "starter_smelter_base.png", "starter_smelter_front.png", "starter_smelter_front_lit.png",
            "copper_block.png", "copper_ore.png" };

        for (String name : solidBlocks) {
            File f = new File(BLOCKS_DIR, name);
            assertTrue(f.exists(), "Solid block texture must exist: " + name);
            BufferedImage img = ImageIO.read(f);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                    assertEquals(
                        255,
                        alpha,
                        "Solid block " + name + " must have 100% opaque alpha at (" + x + "," + y + ")");
                }
            }
        }
    }

    @Test
    void emptyMaskIsStrictlyTransparent() throws IOException {
        File f = new File(ITEMS_DIR, "empty_mask.png");
        assertTrue(f.exists(), "empty_mask.png must exist");
        BufferedImage img = ImageIO.read(f);
        assertEquals(16, img.getWidth());
        assertEquals(16, img.getHeight());
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                assertEquals(0, alpha, "empty_mask pixel must have 0 alpha at (" + x + "," + y + ")");
            }
        }
    }

    @Test
    void vanillaDerivedToolSilhouettesRemainIntact() throws IOException {
        String[] toolSprites = { "tool_pickaxe_head.png", "tool_axe_head.png", "tool_shovel_head.png",
            "tool_hoe_head.png", "tool_blade.png", "tool_handle_pickaxe.png", "tool_handle_axe.png",
            "tool_handle_shovel.png", "tool_handle_hoe.png", "tool_handle_sword.png" };

        for (String name : toolSprites) {
            File f = new File(ITEMS_DIR, name);
            assertTrue(f.exists(), "Tool sprite must exist: " + name);
            BufferedImage img = ImageIO.read(f);
            assertEquals(16, img.getWidth(), "Tool sprite width must be 16: " + name);
            assertEquals(16, img.getHeight(), "Tool sprite height must be 16: " + name);

            // Count opaque pixels
            int opaqueCount = 0;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                    if (alpha > 0) {
                        opaqueCount++;
                    }
                }
            }
            assertTrue(
                opaqueCount >= 5,
                "Tool sprite must have substantial pixel count: " + name + " (" + opaqueCount + ")");
        }
    }

    @Test
    void castingMoldCavitiesAndFillMasksAlignCorrectly() throws IOException {
        String[] geoms = { "pick_head", "plate", "rod", "blade", "handle" };

        for (String g : geoms) {
            File baseFile = new File(ITEMS_DIR, "mold_" + g + ".png");
            File fillFile = new File(ITEMS_DIR, "mold_" + g + "_fill.png");
            assertTrue(baseFile.exists(), "Base mold must exist: " + baseFile.getName());
            assertTrue(fillFile.exists(), "Fill mask must exist: " + fillFile.getName());

            BufferedImage baseImg = ImageIO.read(baseFile);
            BufferedImage fillImg = ImageIO.read(fillFile);

            int fillPixelCount = 0;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int fillAlpha = (fillImg.getRGB(x, y) >> 24) & 0xFF;
                    int baseAlpha = (baseImg.getRGB(x, y) >> 24) & 0xFF;
                    if (fillAlpha > 0) {
                        fillPixelCount++;
                        // Wherever fill mask has pixels, base mold slab must also exist
                        assertTrue(
                            baseAlpha > 0,
                            "Fill pixel at (" + x
                                + ","
                                + y
                                + ") in mold_"
                                + g
                                + "_fill must be within base mold bounds");
                    }
                }
            }
            assertTrue(
                fillPixelCount >= 8,
                "Mold " + g + " fill mask must have at least 8 cavity pixels: " + fillPixelCount);
        }
    }
}
