package naturalexceptions.minecraft.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialDefinition;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.registry.NEItems;

final class VanillaToolPixelParityTest {

    private static final String[] NAMES = { "pickaxe", "axe", "shovel", "hoe", "sword" };
    private static final GeometryId[] HEADS = { GeometryId.PICK_HEAD, GeometryId.AXE_HEAD, GeometryId.SHOVEL_HEAD,
        GeometryId.HOE_HEAD, GeometryId.BLADE };

    @BeforeAll
    static void init() {
        net.minecraft.init.Bootstrap.func_151354_b();
        BuiltinContent.register();
        NEItems.register();
    }

    private static ComponentInstance component(GeometryId geometry, MaterialDefinition material) {
        return new ComponentInstance(
            geometry,
            material.ref(),
            CanonicalMaterialKey.of(material.standardComposition()),
            MaterialQuantity.ofGrams(100),
            ManufacturingState.FINISHED);
    }

    private static ItemStack tool(GeometryId head, MaterialDefinition headMaterial, MaterialDefinition handleMaterial) {
        return ItemGenericAssembly.createStack(
            AssemblyGraph.ofTool(component(head, headMaterial), component(GeometryId.HANDLE, handleMaterial)));
    }

    private static BufferedImage render(ItemStack stack) throws Exception {
        ItemGenericAssembly item = (ItemGenericAssembly) stack.getItem();
        // Register real named sprites without requiring an OpenGL texture atlas.
        item.registerIcons(name -> new TextureAtlasSprite(name) {});
        BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int pass = 0; pass < item.getRenderPasses(0); pass++) {
            String name = item.getIcon(stack, pass)
                .getIconName()
                .split(":")[1];
            BufferedImage layer = ImageIO
                .read(new File("src/main/resources/assets/naturalexceptions/textures/items/" + name + ".png"));
            assertNotNull(layer);
            int tint = item.getColorFromItemStack(stack, pass);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int pixel = layer.getRGB(x, y);
                    if ((pixel >>> 24) == 0) continue;
                    int colored = 0xFF000000;
                    for (int shift : new int[] { 16, 8, 0 }) {
                        int channel = Math.round(((pixel >> shift) & 255) * ((tint >> shift) & 255) / 255.0F);
                        colored |= channel << shift;
                    }
                    result.setRGB(x, y, colored);
                }
            }
        }
        return result;
    }

    /** Minecraft's own iron tool texture, from the game on the test classpath. */
    private static BufferedImage vanillaIron(String tool) throws java.io.IOException {
        try (java.io.InputStream in = VanillaToolPixelParityTest.class
            .getResourceAsStream("/assets/minecraft/textures/items/iron_" + tool + ".png")) {
            if (in == null) throw new java.io.FileNotFoundException("Minecraft's iron_" + tool + ".png");
            return ImageIO.read(in);
        }
    }

    @Test
    void ironToolsWithWoodenHandlesMatchEveryVanillaPixel() throws Exception {
        for (int n = 0; n < NAMES.length; n++) {
            BufferedImage expected = vanillaIron(NAMES[n]);
            BufferedImage actual = render(tool(HEADS[n], BuiltinMaterials.IRON, BuiltinMaterials.WOOD));
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int pixel = expected.getRGB(x, y);
                    assertEquals(
                        (pixel >>> 24) == 0 ? 0 : pixel,
                        actual.getRGB(x, y),
                        NAMES[n] + " pixel (" + x + "," + y + ")");
                }
            }
        }
    }

    @Test
    void copperKeepsVanillaGeometryAndWoodColorsWhileMetalHandlesRemainTintable() throws Exception {
        for (int n = 0; n < NAMES.length; n++) {
            BufferedImage vanilla = vanillaIron(NAMES[n]);
            BufferedImage copper = render(tool(HEADS[n], BuiltinMaterials.COPPER, BuiltinMaterials.WOOD));
            BufferedImage metalHandle = render(tool(HEADS[n], BuiltinMaterials.COPPER, BuiltinMaterials.IRON));
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int p = vanilla.getRGB(x, y);
                    assertEquals(p >>> 24, copper.getRGB(x, y) >>> 24);
                    assertEquals(p >>> 24, metalHandle.getRGB(x, y) >>> 24);
                    if ((p >>> 24) != 0 && ((p >> 16) & 255) != (p & 255)) {
                        assertEquals(p, copper.getRGB(x, y), "Wood must retain its exact vanilla color");
                        assertNotEquals(p, metalHandle.getRGB(x, y), "Non-wood handle must use its own material");
                    }
                }
            }
        }
    }
}
