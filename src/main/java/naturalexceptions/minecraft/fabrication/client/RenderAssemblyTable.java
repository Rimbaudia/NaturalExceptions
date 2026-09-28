package naturalexceptions.minecraft.fabrication.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.fabrication.TileEntityAssemblyTable;

/**
 * Special renderer for TileEntityAssemblyTable.
 * Renders the hosted components (head, handle, or assembled tool) resting flat on the table surface.
 */
@SideOnly(Side.CLIENT)
public final class RenderAssemblyTable extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        if (!(te instanceof TileEntityAssemblyTable)) {
            return;
        }
        TileEntityAssemblyTable table = (TileEntityAssemblyTable) te;

        ItemStack head = table.getStackInSlot(TileEntityAssemblyTable.SLOT_HEAD);
        ItemStack handle = table.getStackInSlot(TileEntityAssemblyTable.SLOT_HANDLE);
        ItemStack result = table.computeResult();

        if (result != null) {
            // Render assembled tool in the center
            renderFlatItem(te, result, x + 0.22D, y + 1.002D, z + 0.22D, 0.56D, 0.56D);
        } else {
            // Render head on the left and handle on the right
            if (head != null) {
                renderFlatItem(te, head, x + 0.08D, y + 1.002D, z + 0.30D, 0.40D, 0.40D);
            }
            if (handle != null) {
                renderFlatItem(te, handle, x + 0.52D, y + 1.002D, z + 0.30D, 0.40D, 0.40D);
            }
        }
    }

    private void renderFlatItem(TileEntity te, ItemStack stack, double minX, double yFace, double minZ, double sizeX,
        double sizeZ) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        int passes = stack.getItem()
            .requiresMultipleRenderPasses()
                ? stack.getItem()
                    .getRenderPasses(stack.getItemDamage())
                : 1;

        bindTexture(TextureMap.locationItemsTexture);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_CULL_FACE);

        int brightness = te.getWorldObj() != null ? te.getWorldObj()
            .getLightBrightnessForSkyBlocks(te.xCoord, te.yCoord + 1, te.zCoord, 0) : 15728880;
        int l1 = brightness % 65536;
        int l2 = brightness / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) l1, (float) l2);

        double maxX = minX + sizeX;
        double maxZ = minZ + sizeZ;
        Tessellator tessellator = Tessellator.instance;

        for (int pass = 0; pass < passes; pass++) {
            IIcon icon = stack.getItem()
                .getIcon(stack, pass);
            if (icon == null) {
                icon = stack.getItem()
                    .getIconIndex(stack);
            }
            if (icon == null) {
                icon = stack.getIconIndex();
            }
            if (icon == null) {
                continue;
            }

            int colorInt = stack.getItem()
                .getColorFromItemStack(stack, pass);
            float red = (float) (colorInt >> 16 & 255) / 255.0F;
            float green = (float) (colorInt >> 8 & 255) / 255.0F;
            float blue = (float) (colorInt & 255) / 255.0F;
            GL11.glColor4f(red, green, blue, 1.0F);

            double minU = icon.getMinU();
            double maxU = icon.getMaxU();
            double minV = icon.getMinV();
            double maxV = icon.getMaxV();
            double layerY = yFace + pass * 0.0002D;

            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(minX, layerY, maxZ, minU, maxV);
            tessellator.addVertexWithUV(maxX, layerY, maxZ, maxU, maxV);
            tessellator.addVertexWithUV(maxX, layerY, minZ, maxU, minV);
            tessellator.addVertexWithUV(minX, layerY, minZ, minU, minV);
            tessellator.draw();
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}
