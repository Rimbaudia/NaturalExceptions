package naturalexceptions.minecraft.power.client;

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
import naturalexceptions.minecraft.power.BlockCable;

/** A cable: a knot at its middle and an arm out to each face it joins, as thick as its gauge. Baked into the chunk. */
@SideOnly(Side.CLIENT)
public final class CableRenderer implements ISimpleBlockRenderingHandler {

    public CableRenderer() {
        BlockCable.renderId = RenderingRegistry.getNextAvailableRenderId();
    }

    @Override
    public int getRenderId() {
        return BlockCable.renderId;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        int meta = world.getBlockMetadata(x, y, z);
        double t = BlockCable.thickness(meta), lo = 0.5 - t / 2, hi = 0.5 + t / 2;
        boolean previous = renderer.renderAllFaces;
        renderer.renderAllFaces = true;
        renderer.setOverrideBlockTexture(block.getIcon(0, meta));
        box(renderer, block, x, y, z, lo, lo, lo, hi, hi, hi);
        if (BlockCable.joins(world, x - 1, y, z)) box(renderer, block, x, y, z, 0, lo, lo, lo, hi, hi);
        if (BlockCable.joins(world, x + 1, y, z)) box(renderer, block, x, y, z, hi, lo, lo, 1, hi, hi);
        if (BlockCable.joins(world, x, y - 1, z)) box(renderer, block, x, y, z, lo, 0, lo, hi, lo, hi);
        if (BlockCable.joins(world, x, y + 1, z)) box(renderer, block, x, y, z, lo, hi, lo, hi, 1, hi);
        if (BlockCable.joins(world, x, y, z - 1)) box(renderer, block, x, y, z, lo, lo, 0, hi, hi, lo);
        if (BlockCable.joins(world, x, y, z + 1)) box(renderer, block, x, y, z, lo, lo, hi, hi, hi, 1);
        renderer.clearOverrideBlockTexture();
        renderer.renderAllFaces = previous;
        renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
        return true;
    }

    private static void box(RenderBlocks renderer, Block block, int x, int y, int z, double x0, double y0, double z0,
        double x1, double y1, double z1) {
        renderer.setRenderBounds(x0, y0, z0, x1, y1, z1);
        renderer.renderStandardBlock(block, x, y, z);
    }

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer) {
        double t = Math.max(0.2, BlockCable.thickness(meta) * 1.5), lo = 0.5 - t / 2, hi = 0.5 + t / 2;
        IIcon icon = block.getIcon(0, meta);
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        renderer.setRenderBounds(0.0, lo, lo, 1.0, hi, hi);
        Tessellator tess = Tessellator.instance;
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
        GL11.glPopMatrix();
        renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
    }
}
