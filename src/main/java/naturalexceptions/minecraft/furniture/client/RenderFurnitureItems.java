package naturalexceptions.minecraft.furniture.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.BlockPantryShelf;
import naturalexceptions.minecraft.furniture.TileEntityMarketCounter;
import naturalexceptions.minecraft.furniture.TileEntityPantryShelf;
import naturalexceptions.minecraft.furniture.TileEntityStorage;
import naturalexceptions.minecraft.furniture.TileEntityWallShelf;
import naturalexceptions.minecraft.furniture.TileEntityWeaponRack;
import naturalexceptions.minecraft.furniture.TileEntityWoodenTable;

/**
 * Shows what furniture holds, drawn as a held item is (a sprite with thickness, or a small block): goods lying on
 * a counter or a table, food inside a pantry shelf's tiers, things standing on a wall shelf, weapons in a rack.
 */
@SideOnly(Side.CLIENT)
public final class RenderFurnitureItems extends TileEntitySpecialRenderer {

    /**
     * Places on a table's top and a counter's, one per slot, and how each lies turned; each thing is drawn small
     * enough to stay inside its own place, so none runs into the next or over the edge.
     */
    private static final double[][] TABLE = { { 0.27, 0.27, 10 }, { 0.73, 0.27, -10 }, { 0.27, 0.73, 5 },
        { 0.73, 0.73, -5 } };
    private static final double[][] COUNTER = { { 0.3, 0.5, 0 }, { 0.7, 0.5, 0 } };

    /** Beyond this (squared) a sprite's sixteenth of thickness can't be seen, so it is drawn as a flat card. */
    private static final double THICK_WITHIN_SQ = 12.0 * 12.0;

    private final RenderBlocks blocks = new RenderBlocks();
    private boolean flat;

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityStorage)) return;
        TileEntityStorage storage = (TileEntityStorage) tile;
        if (storage.isEmpty()) return;
        flat = x * x + y * y + z * z > THICK_WITHIN_SQ;
        int meta = tile.getBlockMetadata();
        light(tile, meta);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (tile instanceof TileEntityWoodenTable) {
            for (int i = 0; i < TABLE.length; i++) lying(
                storage.getStackInSlot(i),
                x + TABLE[i][0],
                y + 1.0,
                z + TABLE[i][1],
                (float) TABLE[i][2],
                0.3F,
                0.2F);
        } else if (tile instanceof TileEntityMarketCounter) {
            for (int i = 0; i < COUNTER.length; i++) lying(
                storage.getStackInSlot(i),
                x + COUNTER[i][0],
                y + 1.0,
                z + COUNTER[i][1],
                (float) COUNTER[i][2],
                0.34F,
                0.22F);
        } else if (tile instanceof TileEntityPantryShelf) {
            // Inside the cabinet, three to a tier. Its front faces away from its wall: meta is that face (2..5).
            int wall = meta ^ 1;
            for (int i = 0; i < 9; i++) {
                double u = 0.2 + 0.3 * (i % 3), floor = BlockPantryShelf.TIERS[i / 3];
                standing(storage.getStackInSlot(i), x, y, z, wall, u, floor, 0.5, 0.26F, 0.18F, false);
            }
        } else if (tile instanceof TileEntityWallShelf) {
            for (int i = 0; i < 3; i++)
                standing(storage.getStackInSlot(i), x, y, z, meta, 0.2 + 0.3 * i, 0.5625, 0.28, 0.3F, 0.2F, false);
        } else if (tile instanceof naturalexceptions.minecraft.furniture.TileEntityTanningRack) {
            // Stretched across the frame, in its middle, a third each.
            for (int i = 0; i < 3; i++)
                standing(storage.getStackInSlot(i), x, y, z, meta, 0.2 + 0.3 * i, 0.35, 0.5, 0.55F, 0.3F, false);
        } else if (tile instanceof TileEntityWeaponRack) {
            for (int i = 0; i < 3; i++)
                // A third of the rack each, a little further out each, so no two share a plane.
                standing(
                    storage.getStackInSlot(i),
                    x,
                    y,
                    z,
                    meta,
                    0.2 + 0.3 * i,
                    0.1,
                    0.14 + 0.03 * i,
                    0.42F,
                    0.3F,
                    true);
        }
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
    }

    /** Lit by the air the goods sit in (above a counter or table, before a shelf), not by the solid block. */
    private static void light(TileEntity tile, int meta) {
        int lx = tile.xCoord, ly = tile.yCoord, lz = tile.zCoord;
        if (tile instanceof TileEntityPantryShelf) {
            lz += meta == 2 ? -1 : meta == 3 ? 1 : 0;
            lx += meta == 4 ? -1 : meta == 5 ? 1 : 0;
        } else if (tile instanceof TileEntityMarketCounter) ly += 1;
        int light = tile.getWorldObj() != null ? tile.getWorldObj()
            .getLightBrightnessForSkyBlocks(lx, ly, lz, 0) : 15728880;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
    }

    /** A thing lying on a surface at (x, y, z), turned {@code yaw} degrees. */
    private void lying(ItemStack stack, double x, double y, double z, float yaw, float itemScale, float blockScale) {
        if (stack == null || stack.getItem() == null) return;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
        float scale = isBlock(stack) ? blockScale : itemScale;
        GL11.glScalef(scale, scale, scale);
        if (isBlock(stack)) {
            GL11.glTranslatef(0.0F, 0.5F, 0.0F);
            drawBlock(stack);
        } else {
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F); // laid down flat
            GL11.glTranslatef(-0.5F, -0.5F, -0.03F);
            drawSprite(stack);
        }
        GL11.glPopMatrix();
    }

    /**
     * A thing standing against a wall. {@code wall} is the side the wall is on (2 north, 3 south, 4 west, 5 east);
     * {@code u} runs along the wall, {@code up} is the height of its foot, {@code out} its distance from the wall. A
     * weapon is turned upright (its picture lies on the diagonal, blade to the upper right).
     */
    private void standing(ItemStack stack, double x, double y, double z, int wall, double u, double up, double out,
        float itemScale, float blockScale, boolean weapon) {
        if (stack == null || stack.getItem() == null) return;
        double px, pz;
        float yaw;
        switch (wall) {
            case 3:
                px = 1 - u;
                pz = 1 - out;
                yaw = 180.0F;
                break;
            case 4:
                px = out;
                pz = 1 - u;
                yaw = 90.0F;
                break;
            case 5:
                px = 1 - out;
                pz = u;
                yaw = -90.0F;
                break;
            default:
                px = u;
                pz = out;
                yaw = 0.0F;
                break;
        }
        GL11.glPushMatrix();
        GL11.glTranslated(x + px, y + up, z + pz);
        GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
        float scale = isBlock(stack) ? blockScale : itemScale;
        GL11.glScalef(scale, scale, scale);
        if (isBlock(stack)) {
            GL11.glTranslatef(0.0F, 0.5F, 0.0F);
            drawBlock(stack);
        } else {
            GL11.glTranslatef(-0.5F, 0.0F, 0.03F);
            if (weapon) {
                GL11.glTranslatef(0.5F, 0.5F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 0.0F, 1.0F);
                GL11.glTranslatef(-0.5F, -0.5F, 0.0F);
            }
            drawSprite(stack);
        }
        GL11.glPopMatrix();
    }

    private static boolean isBlock(ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        return stack.getItemSpriteNumber() == 0 && block != null && RenderBlocks.renderItemIn3d(block.getRenderType());
    }

    private void drawBlock(ItemStack stack) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        blocks.renderBlockAsItem(Block.getBlockFromItem(stack.getItem()), stack.getItemDamage(), 1.0F);
    }

    /**
     * The item's picture with a sixteenth of thickness, every render pass in its colour, in the unit square; far
     * off, a flat two-sided card instead (one draw rather than the thick sprite's six).
     */
    private void drawSprite(ItemStack stack) {
        Item item = stack.getItem();
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(
                stack.getItemSpriteNumber() == 0 ? TextureMap.locationBlocksTexture : TextureMap.locationItemsTexture);
        int passes = item.requiresMultipleRenderPasses() ? item.getRenderPasses(stack.getItemDamage()) : 1;
        for (int pass = 0; pass < passes; pass++) {
            IIcon icon = item.requiresMultipleRenderPasses() ? item.getIcon(stack, pass) : stack.getIconIndex();
            if (icon == null) continue;
            int colour = item.getColorFromItemStack(stack, pass);
            GL11.glColor4f((colour >> 16 & 255) / 255.0F, (colour >> 8 & 255) / 255.0F, (colour & 255) / 255.0F, 1.0F);
            if (flat) {
                card(icon);
                continue;
            }
            ItemRenderer.renderItemIn2D(
                Tessellator.instance,
                icon.getMaxU(),
                icon.getMinV(),
                icon.getMinU(),
                icon.getMaxV(),
                icon.getIconWidth(),
                icon.getIconHeight(),
                0.0625F);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The picture on both faces of the unit square, mapped as {@link ItemRenderer#renderItemIn2D} maps it. */
    private static void card(IIcon icon) {
        float u0 = icon.getMaxU(), v0 = icon.getMinV(), u1 = icon.getMinU(), v1 = icon.getMaxV();
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setNormal(0.0F, 0.0F, 1.0F);
        t.addVertexWithUV(0.0, 0.0, 0.0, u0, v1);
        t.addVertexWithUV(1.0, 0.0, 0.0, u1, v1);
        t.addVertexWithUV(1.0, 1.0, 0.0, u1, v0);
        t.addVertexWithUV(0.0, 1.0, 0.0, u0, v0);
        t.setNormal(0.0F, 0.0F, -1.0F);
        t.addVertexWithUV(0.0, 1.0, 0.0, u0, v0);
        t.addVertexWithUV(1.0, 1.0, 0.0, u1, v0);
        t.addVertexWithUV(1.0, 0.0, 0.0, u1, v1);
        t.addVertexWithUV(0.0, 0.0, 0.0, u0, v1);
        t.draw();
    }
}
