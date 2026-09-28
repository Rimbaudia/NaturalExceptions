package naturalexceptions.minecraft.client.resource;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Dynamic runtime resource pack that extracts vanilla tool head and handle layers directly
 * from Minecraft's active vanilla item textures (e.g. minecraft:textures/items/iron_pickaxe.png)
 * preserving exact native 1.7.10 silhouettes, pixel coordinates, and shading curves.
 */
@SideOnly(Side.CLIENT)
public final class VanillaToolResourcePack implements IResourcePack {

    public static final String DOMAIN = "naturalexceptions";

    private static final Map<String, String[]> RESOURCE_MAPPINGS = new HashMap<>();
    private static final Map<String, Set<Integer>> HANDLE_COORDINATES = new HashMap<>();

    static {
        // [0] = tool type ("pickaxe", "axe", "shovel", "hoe", "sword"), [1] = layer ("head" or "handle")
        RESOURCE_MAPPINGS.put("textures/items/tool_pickaxe_head.png", new String[] { "pickaxe", "head" });
        RESOURCE_MAPPINGS.put("textures/items/component_pick_head.png", new String[] { "pickaxe", "head" });
        RESOURCE_MAPPINGS.put("textures/items/tool_axe_head.png", new String[] { "axe", "head" });
        RESOURCE_MAPPINGS.put("textures/items/component_axe_head.png", new String[] { "axe", "head" });
        RESOURCE_MAPPINGS.put("textures/items/tool_shovel_head.png", new String[] { "shovel", "head" });
        RESOURCE_MAPPINGS.put("textures/items/component_shovel_head.png", new String[] { "shovel", "head" });
        RESOURCE_MAPPINGS.put("textures/items/tool_hoe_head.png", new String[] { "hoe", "head" });
        RESOURCE_MAPPINGS.put("textures/items/component_hoe_head.png", new String[] { "hoe", "head" });
        RESOURCE_MAPPINGS.put("textures/items/tool_blade.png", new String[] { "sword", "head" });
        RESOURCE_MAPPINGS.put("textures/items/component_blade.png", new String[] { "sword", "head" });

        RESOURCE_MAPPINGS.put("textures/items/tool_handle_pickaxe.png", new String[] { "pickaxe", "handle" });
        RESOURCE_MAPPINGS.put("textures/items/tool_handle.png", new String[] { "pickaxe", "handle" });
        RESOURCE_MAPPINGS.put("textures/items/component_handle.png", new String[] { "pickaxe", "handle" });
        RESOURCE_MAPPINGS.put("textures/items/tool_handle_axe.png", new String[] { "axe", "handle" });
        RESOURCE_MAPPINGS.put("textures/items/tool_handle_shovel.png", new String[] { "shovel", "handle" });
        RESOURCE_MAPPINGS.put("textures/items/tool_handle_hoe.png", new String[] { "hoe", "handle" });
        RESOURCE_MAPPINGS.put("textures/items/tool_handle_sword.png", new String[] { "sword", "handle" });

        // Initialize exact vanilla handle pixel coordinates (x + y * 16)
        // Pickaxe & Hoe: 31 handle pixels
        Set<Integer> pickHandle = new HashSet<>();
        addCoord(pickHandle, 12, 3);
        addCoord(pickHandle, 13, 3);
        addCoord(pickHandle, 12, 4);
        addCoord(pickHandle, 13, 4);
        addCoord(pickHandle, 10, 5);
        addCoord(pickHandle, 9, 6);
        addCoord(pickHandle, 10, 6);
        addCoord(pickHandle, 11, 6);
        addCoord(pickHandle, 8, 7);
        addCoord(pickHandle, 9, 7);
        addCoord(pickHandle, 10, 7);
        addCoord(pickHandle, 7, 8);
        addCoord(pickHandle, 8, 8);
        addCoord(pickHandle, 9, 8);
        addCoord(pickHandle, 6, 9);
        addCoord(pickHandle, 7, 9);
        addCoord(pickHandle, 8, 9);
        addCoord(pickHandle, 5, 10);
        addCoord(pickHandle, 6, 10);
        addCoord(pickHandle, 7, 10);
        addCoord(pickHandle, 4, 11);
        addCoord(pickHandle, 5, 11);
        addCoord(pickHandle, 6, 11);
        addCoord(pickHandle, 3, 12);
        addCoord(pickHandle, 4, 12);
        addCoord(pickHandle, 5, 12);
        addCoord(pickHandle, 2, 13);
        addCoord(pickHandle, 3, 13);
        addCoord(pickHandle, 4, 13);
        addCoord(pickHandle, 2, 14);
        addCoord(pickHandle, 3, 14);
        HANDLE_COORDINATES.put("pickaxe", pickHandle);
        HANDLE_COORDINATES.put("hoe", pickHandle);

        // Axe: 27 handle pixels
        Set<Integer> axeHandle = new HashSet<>();
        addCoord(axeHandle, 11, 4);
        addCoord(axeHandle, 12, 4);
        addCoord(axeHandle, 12, 5);
        addCoord(axeHandle, 9, 6);
        addCoord(axeHandle, 8, 7);
        addCoord(axeHandle, 9, 7);
        addCoord(axeHandle, 10, 7);
        addCoord(axeHandle, 7, 8);
        addCoord(axeHandle, 8, 8);
        addCoord(axeHandle, 9, 8);
        addCoord(axeHandle, 6, 9);
        addCoord(axeHandle, 7, 9);
        addCoord(axeHandle, 8, 9);
        addCoord(axeHandle, 5, 10);
        addCoord(axeHandle, 6, 10);
        addCoord(axeHandle, 7, 10);
        addCoord(axeHandle, 4, 11);
        addCoord(axeHandle, 5, 11);
        addCoord(axeHandle, 6, 11);
        addCoord(axeHandle, 3, 12);
        addCoord(axeHandle, 4, 12);
        addCoord(axeHandle, 5, 12);
        addCoord(axeHandle, 2, 13);
        addCoord(axeHandle, 3, 13);
        addCoord(axeHandle, 4, 13);
        addCoord(axeHandle, 2, 14);
        addCoord(axeHandle, 3, 14);
        HANDLE_COORDINATES.put("axe", axeHandle);

        // Shovel: 25 handle pixels
        Set<Integer> shovelHandle = new HashSet<>();
        addCoord(shovelHandle, 9, 6);
        addCoord(shovelHandle, 8, 7);
        addCoord(shovelHandle, 9, 7);
        addCoord(shovelHandle, 10, 7);
        addCoord(shovelHandle, 7, 8);
        addCoord(shovelHandle, 8, 8);
        addCoord(shovelHandle, 9, 8);
        addCoord(shovelHandle, 6, 9);
        addCoord(shovelHandle, 7, 9);
        addCoord(shovelHandle, 8, 9);
        addCoord(shovelHandle, 5, 10);
        addCoord(shovelHandle, 6, 10);
        addCoord(shovelHandle, 7, 10);
        addCoord(shovelHandle, 4, 11);
        addCoord(shovelHandle, 5, 11);
        addCoord(shovelHandle, 6, 11);
        addCoord(shovelHandle, 2, 12);
        addCoord(shovelHandle, 3, 12);
        addCoord(shovelHandle, 4, 12);
        addCoord(shovelHandle, 5, 12);
        addCoord(shovelHandle, 2, 13);
        addCoord(shovelHandle, 3, 13);
        addCoord(shovelHandle, 4, 13);
        addCoord(shovelHandle, 3, 14);
        addCoord(shovelHandle, 4, 14);
        HANDLE_COORDINATES.put("shovel", shovelHandle);

        // Sword: 7 handle grip pixels
        Set<Integer> swordHandle = new HashSet<>();
        addCoord(swordHandle, 3, 11);
        addCoord(swordHandle, 4, 11);
        addCoord(swordHandle, 2, 12);
        addCoord(swordHandle, 3, 12);
        addCoord(swordHandle, 4, 12);
        addCoord(swordHandle, 2, 13);
        addCoord(swordHandle, 3, 13);
        HANDLE_COORDINATES.put("sword", swordHandle);
    }

    private static void addCoord(Set<Integer> set, int x, int y) {
        set.add(y * 16 + x);
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if (!resourceExists(location)) {
            throw new FileNotFoundException(location.toString());
        }

        String[] mapping = RESOURCE_MAPPINGS.get(location.getResourcePath());
        if (mapping != null) {
            String tool = mapping[0];
            String layer = mapping[1];

            try {
                BufferedImage derived = deriveTexture(tool, layer);
                if (derived != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(derived, "png", baos);
                    return new ByteArrayInputStream(baos.toByteArray());
                }
            } catch (Throwable ignored) {
                // Fallback to classpath resource below
            }
        }

        // Fallback to classpath asset
        InputStream fallback = getClass()
            .getResourceAsStream("/assets/naturalexceptions/" + location.getResourcePath());
        if (fallback != null) {
            return fallback;
        }

        throw new FileNotFoundException(location.toString());
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (location == null || !DOMAIN.equals(location.getResourceDomain())) {
            return false;
        }
        return RESOURCE_MAPPINGS.containsKey(location.getResourcePath());
    }

    @Override
    public Set<String> getResourceDomains() {
        return Collections.singleton(DOMAIN);
    }

    @Override
    public IMetadataSection getPackMetadata(IMetadataSerializer serializer, String sectionName) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public String getPackName() {
        return "NaturalExceptionsVanillaToolAssets";
    }

    private BufferedImage deriveTexture(String tool, String layer) throws IOException {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getResourceManager() == null) {
            return null;
        }

        ResourceLocation vanillaLoc = new ResourceLocation("minecraft", "textures/items/iron_" + tool + ".png");
        IResource vanillaResource = mc.getResourceManager()
            .getResource(vanillaLoc);
        if (vanillaResource == null) {
            return null;
        }

        BufferedImage src;
        try (InputStream in = vanillaResource.getInputStream()) {
            src = ImageIO.read(in);
        }
        if (src == null) {
            return null;
        }

        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Set<Integer> handleCoords = HANDLE_COORDINATES.get(tool);
        if (handleCoords == null) {
            handleCoords = Collections.emptySet();
        }

        for (int y = 0; y < height; y++) {
            int gridY = (y * 16) / height;
            for (int x = 0; x < width; x++) {
                int gridX = (x * 16) / width;
                int argb = src.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }

                boolean isHandlePixel = handleCoords.contains(gridY * 16 + gridX);

                if ("handle".equals(layer)) {
                    if (isHandlePixel) {
                        // Map handle pixel to exact grayscale value curve
                        int r = (argb >> 16) & 0xFF;
                        int gVal;
                        if (r >= 130) gVal = 255;
                        else if (r >= 100) gVal = 194;
                        else if (r >= 70) gVal = 136;
                        else gVal = 75;

                        int outArgb = (255 << 24) | (gVal << 16) | (gVal << 8) | gVal;
                        result.setRGB(x, y, outArgb);
                    }
                } else {
                    // Head layer
                    if (!isHandlePixel) {
                        // In vanilla iron tools, head pixels are already pure grayscale (255, 216, 193, 68)
                        result.setRGB(x, y, argb);
                    }
                }
            }
        }

        return result;
    }
}
