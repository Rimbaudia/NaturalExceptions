package naturalexceptions.minecraft.client.model;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.block.PartModel;

/** Draws any {@link PartModel} block in the world and as an item, one textured box per part. */
@SideOnly(Side.CLIENT)
public final class PartModelRenderer implements ISimpleBlockRenderingHandler {

    public PartModelRenderer() {
        PartModel.RenderType.id = RenderingRegistry.getNextAvailableRenderId();
    }

    @Override
    public int getRenderId() {
        return PartModel.RenderType.id;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        if (!(block instanceof PartModel)) return false;
        PartModel model = (PartModel) block;
        int metadata = world.getBlockMetadata(x, y, z);
        boolean previous = renderer.renderAllFaces;
        renderer.renderAllFaces = true;
        double[][] parts = model.parts(metadata);
        for (int i = 0; i < parts.length; i++) {
            double[] b = parts[i];
            renderer.setOverrideBlockTexture(model.partIcon(i, metadata));
            renderer.setRenderBounds(b[0], b[1], b[2], b[3], b[4], b[5]);
            renderer.renderStandardBlock(block, x, y, z);
        }
        renderer.clearOverrideBlockTexture();
        renderer.renderAllFaces = previous;
        block.setBlockBoundsBasedOnState(world, x, y, z);
        renderer.setRenderBoundsFromBlock(block);
        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        if (!(block instanceof PartModel)) return;
        PartModel model = (PartModel) block;
        int meta = model.inventoryMetadata();
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        Tessellator tess = Tessellator.instance;
        double[][] parts = model.parts(meta);
        for (int i = 0; i < parts.length; i++) {
            double[] b = parts[i];
            IIcon icon = model.partIcon(i, meta);
            renderer.setRenderBounds(b[0], b[1], b[2], b[3], b[4], b[5]);
            for (int side = 0; side < 6; side++) {
                tess.startDrawingQuads();
                tess.setNormal(
                    side == 4 ? -1 : side == 5 ? 1 : 0,
                    side == 0 ? -1 : side == 1 ? 1 : 0,
                    side == 2 ? -1 : side == 3 ? 1 : 0);
                switch (side) {
                    case 0:
                        renderer.renderFaceYNeg(block, 0, 0, 0, icon);
                        break;
                    case 1:
                        renderer.renderFaceYPos(block, 0, 0, 0, icon);
                        break;
                    case 2:
                        renderer.renderFaceZNeg(block, 0, 0, 0, icon);
                        break;
                    case 3:
                        renderer.renderFaceZPos(block, 0, 0, 0, icon);
                        break;
                    case 4:
                        renderer.renderFaceXNeg(block, 0, 0, 0, icon);
                        break;
                    default:
                        renderer.renderFaceXPos(block, 0, 0, 0, icon);
                        break;
                }
                tess.draw();
            }
        }
        block.setBlockBoundsForItemRender();
        renderer.setRenderBoundsFromBlock(block);
        GL11.glPopMatrix();
    }
}
