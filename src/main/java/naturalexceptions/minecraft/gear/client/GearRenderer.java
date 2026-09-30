package naturalexceptions.minecraft.gear.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.item.ItemExposureMask;
import naturalexceptions.minecraft.network.PacketGearAppearance;

/**
 * Draws worn gear on players in the world: the filter mask over mouth and nose, tied behind the head, and a Glider
 * Cloak hanging from the shoulders in the colour of its wool, spreading out when its wearer falls or glides.
 */
@SideOnly(Side.CLIENT)
public final class GearRenderer {

    private static final ResourceLocation MASK_TEXTURE = new ResourceLocation(
        "naturalexceptions",
        "textures/models/filter_mask.png");
    private static final float PIXEL = 0.0625F;

    private final MaskModel mask = new MaskModel();

    private GearRenderer() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new GearRenderer());
    }

    @SubscribeEvent
    public void onSpecials(RenderPlayerEvent.Specials.Post event) {
        EntityPlayer player = event.entityPlayer;
        if (player == null || player.isInvisible()) return;
        ItemStack back = PacketGearAppearance.CLIENT_BACKS.get(player.getEntityId());
        if (back != null && back.getItem() instanceof naturalexceptions.minecraft.item.ItemTreasureGear
            && ((naturalexceptions.minecraft.item.ItemTreasureGear) back.getItem()).treasure()
                == naturalexceptions.minecraft.gear.Treasure.GLIDER_CLOAK)
            cloak(event, player, naturalexceptions.minecraft.item.GliderCloaks.colour(back));
        ItemStack face = PacketGearAppearance.CLIENT_FACES.get(player.getEntityId());
        if (face == null || !(face.getItem() instanceof ItemExposureMask)) return;
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        event.renderer.modelBipedMain.bipedHead.postRender(PIXEL);
        net.minecraft.client.Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(MASK_TEXTURE);
        mask.render(PIXEL);
        GL11.glPopMatrix();
    }

    /**
     * The cloak: a sheet of the wool it was sewn from, ten pixels wide and sixteen long, hung from the shoulders
     * behind the body; it lifts away from the back as its wearer falls, and spreads nearly flat when gliding.
     */
    private static void cloak(RenderPlayerEvent.Specials.Post event, EntityPlayer player, int wool) {
        net.minecraft.util.IIcon icon = net.minecraft.init.Blocks.wool.getIcon(0, wool);
        float falling = player.onGround ? 0.0F : (float) Math.max(0.0, Math.min(1.0, -player.motionY * 2.5));
        float lift = 6.0F + 64.0F * falling;
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        event.renderer.modelBipedMain.bipedBody.postRender(PIXEL);
        GL11.glTranslatef(0.0F, 0.0F, 2.2F * PIXEL);
        GL11.glRotatef(lift, 1.0F, 0.0F, 0.0F);
        net.minecraft.client.Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(net.minecraft.client.renderer.texture.TextureMap.locationBlocksTexture);
        GL11.glDisable(GL11.GL_CULL_FACE);
        net.minecraft.client.renderer.Tessellator t = net.minecraft.client.renderer.Tessellator.instance;
        float x0 = -5 * PIXEL, x1 = 5 * PIXEL, y1 = 16 * PIXEL, z = 0.0F;
        t.startDrawingQuads();
        t.setNormal(0.0F, 0.0F, 1.0F);
        t.addVertexWithUV(x0, 0.0F, z, icon.getMinU(), icon.getMinV());
        t.addVertexWithUV(x0, y1, z, icon.getMinU(), icon.getMaxV());
        t.addVertexWithUV(x1, y1, z, icon.getMaxU(), icon.getMaxV());
        t.addVertexWithUV(x1, 0.0F, z, icon.getMaxU(), icon.getMinV());
        t.draw();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    /** Entity ids are reused between worlds; forget faces when the client's world goes away. */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) {
            PacketGearAppearance.CLIENT_FACES.clear();
            PacketGearAppearance.CLIENT_BACKS.clear();
        }
    }

    /** A cloth pad over the lower face and a strap around the head, in head-model units. */
    static final class MaskModel extends ModelBase {

        private final ModelRenderer pad;
        private final ModelRenderer strap;

        MaskModel() {
            textureWidth = 32;
            textureHeight = 16;
            pad = new ModelRenderer(this, 0, 0);
            pad.addBox(-3.0F, -4.0F, -5.0F, 6, 3, 1, 0.05F);
            strap = new ModelRenderer(this, 0, 4);
            strap.addBox(-4.0F, -4.0F, -4.0F, 8, 1, 8, 0.3F);
        }

        void render(float scale) {
            pad.render(scale);
            strap.render(scale);
        }
    }
}
