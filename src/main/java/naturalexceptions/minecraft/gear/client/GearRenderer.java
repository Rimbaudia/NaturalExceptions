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

/** Draws worn gear on players in the world. For now: the filter mask over mouth and nose, tied behind the head. */
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

    /** Entity ids are reused between worlds; forget faces when the client's world goes away. */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) PacketGearAppearance.CLIENT_FACES.clear();
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
