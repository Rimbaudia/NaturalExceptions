package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.TileEntityMarketCounter;
import naturalexceptions.minecraft.furniture.TileEntityPantryShelf;
import naturalexceptions.minecraft.furniture.TileEntityStorage;
import naturalexceptions.minecraft.furniture.TileEntityWoodenTable;

/**
 * Shows what furniture holds: the things on a table's top, the goods set out on a market counter, the food on a
 * pantry shelf's front.
 */
@SideOnly(Side.CLIENT)
public final class RenderFurnitureItems extends TileEntitySpecialRenderer {

    /** Places on a table's top, one per slot. */
    private static final double[][] TABLE = { { 0.28, 0.28 }, { 0.72, 0.28 }, { 0.28, 0.72 }, { 0.72, 0.72 } };
    private static final double[][] COUNTER = { { 0.2, 0.5 }, { 0.5, 0.5 }, { 0.8, 0.5 } };

    private EntityItem shown;

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityStorage)) return;
        TileEntityStorage storage = (TileEntityStorage) tile;
        // Lit by the air the goods sit in (above a counter or table, before a shelf's face), not by the inside of
        // the solid block, which is dark.
        int lx = tile.xCoord, ly = tile.yCoord, lz = tile.zCoord;
        if (tile instanceof TileEntityPantryShelf) {
            int facing = tile.getBlockMetadata();
            lz += facing == 2 ? -1 : facing == 3 ? 1 : 0;
            lx += facing == 4 ? -1 : facing == 5 ? 1 : 0;
        } else ly += 1;
        int light = tile.getWorldObj() != null ? tile.getWorldObj()
            .getLightBrightnessForSkyBlocks(lx, ly, lz, 0) : 15728880;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (tile instanceof TileEntityWoodenTable) {
            for (int i = 0; i < TABLE.length; i++)
                draw(storage, i, x + TABLE[i][0], y + 1.0, z + TABLE[i][1], 0.55F, 0.0F);
        } else if (tile instanceof TileEntityMarketCounter) {
            for (int i = 0; i < COUNTER.length; i++)
                draw(storage, i, x + COUNTER[i][0], y + 1.0, z + COUNTER[i][1], 0.6F, 0.0F);
        } else if (tile instanceof TileEntityPantryShelf) {
            int facing = tile.getBlockMetadata();
            for (int i = 0; i < 9; i++) {
                double u = 0.22 + 0.28 * (i % 3), v = 0.72 - 0.3 * (i / 3);
                double px, pz;
                float yaw;
                switch (facing) {
                    case 2:
                        px = 1 - u;
                        pz = -0.02;
                        yaw = 180.0F;
                        break;
                    case 4:
                        px = -0.02;
                        pz = u;
                        yaw = 270.0F;
                        break;
                    case 5:
                        px = 1.02;
                        pz = 1 - u;
                        yaw = 90.0F;
                        break;
                    default:
                        px = u;
                        pz = 1.02;
                        yaw = 0.0F;
                        break;
                }
                draw(storage, i, x + px, y + v - 0.12, z + pz, 0.4F, yaw);
            }
        }
    }

    private void draw(TileEntityStorage storage, int slot, double x, double y, double z, float scale, float yaw) {
        ItemStack stack = storage.getStackInSlot(slot);
        if (stack == null || stack.getItem() == null) return;
        if (shown == null || shown.worldObj != storage.getWorldObj()) shown = new EntityItem(storage.getWorldObj());
        ItemStack one = stack.copy();
        one.stackSize = 1;
        shown.setEntityItemStack(one);
        shown.hoverStart = 0.0F;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(scale, scale, scale);
        boolean frame = RenderItem.renderInFrame;
        RenderItem.renderInFrame = true;
        RenderManager.instance.renderEntityWithPosYaw(shown, 0.0, 0.0, 0.0, 0.0F, 0.0F);
        RenderItem.renderInFrame = frame;
        GL11.glPopMatrix();
    }
}
