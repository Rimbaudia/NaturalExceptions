package naturalexceptions.minecraft.metallurgy.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import naturalexceptions.minecraft.metallurgy.BlockSmelterOutlet;
import naturalexceptions.minecraft.metallurgy.SmelterOutletGeometry;

/** Static, lit spout geometry shared by chunk and inventory rendering. */
public final class RenderSmelterOutletBlock implements ISimpleBlockRenderingHandler {

    public RenderSmelterOutletBlock() {
        BlockSmelterOutlet.renderId = RenderingRegistry.getNextAvailableRenderId();
    }

    @Override
    public int getRenderId() {
        return BlockSmelterOutlet.renderId;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        boolean previous = renderer.renderAllFaces;
        renderer.renderAllFaces = true;
        for (double[] b : SmelterOutletGeometry.parts(world.getBlockMetadata(x, y, z))) {
            renderer.setRenderBounds(b[0], b[1], b[2], b[3], b[4], b[5]);
            renderer.renderStandardBlock(block, x, y, z);
        }
        renderer.renderAllFaces = previous;
        block.setBlockBoundsBasedOnState(world, x, y, z);
        renderer.setRenderBoundsFromBlock(block);
        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        Tessellator tess = Tessellator.instance;
        for (double[] b : SmelterOutletGeometry.parts(2)) {
            renderer.setRenderBounds(b[0], b[1], b[2], b[3], b[4], b[5]);
            for (int side = 0; side < 6; side++) {
                tess.startDrawingQuads();
                tess.setNormal(
                    side == 4 ? -1 : side == 5 ? 1 : 0,
                    side == 0 ? -1 : side == 1 ? 1 : 0,
                    side == 2 ? -1 : side == 3 ? 1 : 0);
                switch (side) {
                    case 0:
                        renderer.renderFaceYNeg(block, 0, 0, 0, block.getIcon(side, 2));
                        break;
                    case 1:
                        renderer.renderFaceYPos(block, 0, 0, 0, block.getIcon(side, 2));
                        break;
                    case 2:
                        renderer.renderFaceZNeg(block, 0, 0, 0, block.getIcon(side, 2));
                        break;
                    case 3:
                        renderer.renderFaceZPos(block, 0, 0, 0, block.getIcon(side, 2));
                        break;
                    case 4:
                        renderer.renderFaceXNeg(block, 0, 0, 0, block.getIcon(side, 2));
                        break;
                    default:
                        renderer.renderFaceXPos(block, 0, 0, 0, block.getIcon(side, 2));
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
