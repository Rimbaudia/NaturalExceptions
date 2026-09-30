package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.entity.EntityStormwisp;
import naturalexceptions.minecraft.weapon.client.CrystalDraw;

/**
 * A Stormwisp as light: three Stormglass splinters wheeling round a glowing, crackling core. As it gathers a shot the
 * splinters draw in and whiten, the glow swells and the arcs thicken; struck, it flashes.
 */
@SideOnly(Side.CLIENT)
public final class RenderStormwisp extends Render {

    public RenderStormwisp() {
        shadowSize = 0.25F;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        float charge = entity instanceof EntityStormwisp ? ((EntityStormwisp) entity).chargeLevel() : 0.0F;
        boolean hurt = entity instanceof EntityLivingBase && ((EntityLivingBase) entity).hurtTime > 0;
        float t = entity.ticksExisted + partialTicks;
        float bright = Math.max(hurt ? 0.7F : 0.0F, charge * 0.8F);
        GL11.glPushMatrix();
        CrystalDraw.begin();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GL11.glTranslated(x, y + entity.height / 2 + Math.sin(t * 0.1) * 0.06, z);
        // The three splinters, points outward and a little up, wheeling round; closer in as it charges.
        float orbit = 0.34F - 0.12F * charge;
        for (int i = 0; i < 3; i++) {
            GL11.glPushMatrix();
            GL11.glRotatef(t * (6.0F + 10.0F * charge) + i * 120.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(orbit, (float) Math.sin(t * 0.15 + i * 2.1) * 0.08F, 0.0F);
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-25.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(t * 12.0F, 0.0F, 0.0F, 1.0F);
            CrystalDraw.crystal(0.26F, 0.12F, 0.06F, bright);
            GL11.glPopMatrix();
        }
        CrystalDraw
            .arcs(entity.getEntityId() * 17L + entity.ticksExisted, 2 + (int) (charge * 3), 0.4F + 0.2F * charge, 0.9F);
        GL11.glRotatef(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        CrystalDraw.glow(0.45F + 0.35F * charge + (hurt ? 0.2F : 0.0F), 1.0F);
        CrystalDraw.end();
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
