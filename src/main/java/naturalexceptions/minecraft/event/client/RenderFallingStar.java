package naturalexceptions.minecraft.event.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.event.GlowingStar;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A falling star (or a sword-called one) as the eye sees it: a white-hot tumbling body of stellar glass wrapped in a
 * cyan halo, with a
 * fading tail along its path. Drawn full-bright; no lighting or fog is applied so it reads against a night sky.
 */
@SideOnly(Side.CLIENT)
public final class RenderFallingStar extends Render {

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        float size = entity instanceof GlowingStar ? ((GlowingStar) entity).starSize() : 1.0F;
        float spin = (entity.ticksExisted + partialTicks) * 9.0F;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

        // Body: a tumbling block of molten stellar glass.
        bindTexture(TextureMap.locationBlocksTexture);
        IIcon icon = NEBlocks.fallenStarCell != null ? NEBlocks.fallenStarCell.getIcon(1, 0) : null;
        GL11.glPushMatrix();
        GL11.glRotatef(spin, 0.6F, 1.0F, 0.3F);
        GL11.glScalef(size, size, size);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (icon != null) drawCube(icon);
        GL11.glPopMatrix();

        // Halo and tail: additive, untextured, depth-tested but not written.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        GL11.glPushMatrix();
        GL11.glRotatef(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        drawHalo(size * 1.6F, 0.55F, 0.95F, 1.0F, 0.45F);
        drawHalo(size * 3.2F, 0.35F, 0.80F, 1.0F, 0.18F);
        GL11.glPopMatrix();
        drawTail(entity, size);
        GL11.glDepthMask(true);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    private static void drawCube(IIcon icon) {
        double u0 = icon.getMinU(), u1 = icon.getMaxU(), v0 = icon.getMinV(), v1 = icon.getMaxV();
        double h = 0.5;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setBrightness(0xF000F0);
        // -Z / +Z
        t.addVertexWithUV(-h, -h, -h, u1, v1);
        t.addVertexWithUV(-h, h, -h, u1, v0);
        t.addVertexWithUV(h, h, -h, u0, v0);
        t.addVertexWithUV(h, -h, -h, u0, v1);
        t.addVertexWithUV(-h, -h, h, u0, v1);
        t.addVertexWithUV(h, -h, h, u1, v1);
        t.addVertexWithUV(h, h, h, u1, v0);
        t.addVertexWithUV(-h, h, h, u0, v0);
        // -X / +X
        t.addVertexWithUV(-h, -h, -h, u0, v1);
        t.addVertexWithUV(-h, -h, h, u1, v1);
        t.addVertexWithUV(-h, h, h, u1, v0);
        t.addVertexWithUV(-h, h, -h, u0, v0);
        t.addVertexWithUV(h, -h, -h, u1, v1);
        t.addVertexWithUV(h, h, -h, u1, v0);
        t.addVertexWithUV(h, h, h, u0, v0);
        t.addVertexWithUV(h, -h, h, u0, v1);
        // -Y / +Y
        t.addVertexWithUV(-h, -h, -h, u0, v0);
        t.addVertexWithUV(h, -h, -h, u1, v0);
        t.addVertexWithUV(h, -h, h, u1, v1);
        t.addVertexWithUV(-h, -h, h, u0, v1);
        t.addVertexWithUV(-h, h, -h, u0, v0);
        t.addVertexWithUV(-h, h, h, u0, v1);
        t.addVertexWithUV(h, h, h, u1, v1);
        t.addVertexWithUV(h, h, -h, u1, v0);
        t.draw();
    }

    private static void drawHalo(float radius, float r, float g, float b, float alpha) {
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLE_FAN);
        t.setColorRGBA_F(r, g, b, alpha);
        t.addVertex(0.0, 0.0, 0.0);
        t.setColorRGBA_F(r, g, b, 0.0F);
        for (int i = 0; i <= 16; i++) {
            double a = i / 16.0 * Math.PI * 2.0;
            t.addVertex(Math.cos(a) * radius, Math.sin(a) * radius, 0.0);
        }
        t.draw();
    }

    /** A fading streak back along the direction of travel. */
    private static void drawTail(Entity star, float size) {
        double mx = star.motionX, my = star.motionY, mz = star.motionZ;
        double speed = Math.sqrt(mx * mx + my * my + mz * mz);
        if (speed < 1.0e-3) {
            mx = 0.4;
            my = -1.0;
            mz = 0.3;
            speed = Math.sqrt(mx * mx + my * my + mz * mz);
        }
        double length = 14.0 + 6.0 * size;
        double dx = -mx / speed * length, dy = -my / speed * length, dz = -mz / speed * length;
        Tessellator t = Tessellator.instance;
        double w = size * 0.45;
        for (int plane = 0; plane < 2; plane++) {
            t.startDrawingQuads();
            double ox = plane == 0 ? w : 0.0, oz = plane == 0 ? 0.0 : w;
            t.setColorRGBA_F(0.75F, 0.98F, 1.0F, 0.55F);
            t.addVertex(-ox, 0.0, -oz);
            t.addVertex(ox, 0.0, oz);
            t.setColorRGBA_F(0.30F, 0.70F, 1.0F, 0.0F);
            t.addVertex(dx + ox * 0.2, dy, dz + oz * 0.2);
            t.addVertex(dx - ox * 0.2, dy, dz - oz * 0.2);
            t.draw();
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TextureMap.locationBlocksTexture;
    }
}
