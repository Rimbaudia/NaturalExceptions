package naturalexceptions.minecraft.fabrication.client;

import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.fabrication.TileEntityCastingTable;

/**
 * Renders the hosted casting mold sitting flat on top of the casting table.
 */
@SideOnly(Side.CLIENT)
public final class RenderCastingTable extends TileEntitySpecialRenderer {

    private EntityItem dummyItem;

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        if (!(te instanceof TileEntityCastingTable)) {
            return;
        }
        TileEntityCastingTable table = (TileEntityCastingTable) te;
        ItemStack mold = table.getMold();
        if (mold == null) {
            return;
        }

        if (dummyItem == null) {
            dummyItem = new EntityItem(te.getWorldObj(), 0.0D, 0.0D, 0.0D, mold);
        }
        dummyItem.worldObj = te.getWorldObj();
        dummyItem.setEntityItemStack(mold);
        dummyItem.hoverStart = 0.0F;

        GL11.glPushMatrix();
        // Center on the top surface of the casting table (y + 1.01 to prevent z-fighting with top face)
        GL11.glTranslatef((float) x + 0.5F, (float) y + 1.01F, (float) z + 0.5F);
        // Lie flat on the surface
        GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(0.85F, 0.85F, 0.85F);

        RenderItem.renderInFrame = true;
        RenderManager.instance.renderEntityWithPosYaw(dummyItem, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
        RenderItem.renderInFrame = false;

        GL11.glPopMatrix();
    }
}
