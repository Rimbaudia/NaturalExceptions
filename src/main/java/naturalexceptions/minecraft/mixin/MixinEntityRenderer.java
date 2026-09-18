package naturalexceptions.minecraft.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import naturalexceptions.core.gravity.GravitySample;
import naturalexceptions.core.gravity.LocalBodyStance;
import naturalexceptions.core.gravity.Vector3d;
import naturalexceptions.minecraft.gravity.NECameraHandler;
import naturalexceptions.minecraft.gravity.NEGravityService;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Shadow
    private Minecraft mc;

    @Shadow
    private boolean cloudFog;

    @Shadow
    private float camRoll;

    @Shadow
    private float prevCamRoll;

    @Shadow
    private float thirdPersonDistance;

    @Shadow
    private float thirdPersonDistanceTemp;

    @Inject(method = "orientCamera", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$onOrientCamera(float partialTicks, CallbackInfo ci) {
        if (!NEGravityService.global()
            .isNonstandardGravityActive()) {
            return;
        }

        EntityLivingBase entitylivingbase = this.mc.renderViewEntity;
        if (entitylivingbase == null) {
            return;
        }

        double d0 = entitylivingbase.prevPosX
            + (entitylivingbase.posX - entitylivingbase.prevPosX) * (double) partialTicks;
        double basePosY = entitylivingbase.prevPosY
            + (entitylivingbase.posY - entitylivingbase.prevPosY) * (double) partialTicks;
        double d2 = entitylivingbase.prevPosZ
            + (entitylivingbase.posZ - entitylivingbase.prevPosZ) * (double) partialTicks;

        GravitySample sample = NEGravityService.global()
            .sampleFor(entitylivingbase);
        double hw = entitylivingbase.width * 0.5;
        double hh = entitylivingbase.height * 0.5;
        Vector3d aabbCenter = new Vector3d(d0, basePosY + hh, d2);
        LocalBodyStance stance = LocalBodyStance
            .fromAabb(aabbCenter, sample.localDown(), hw, hh, entitylivingbase.getEyeHeight());
        Vector3d entityPos = new Vector3d(d0, basePosY, d2);
        Vector3d eyeDelta = stance.cameraEyeDelta(entityPos);

        GL11.glRotatef(this.prevCamRoll + (this.camRoll - this.prevCamRoll) * partialTicks, 0.0F, 0.0F, 1.0F);

        if (this.mc.gameSettings.thirdPersonView > 0) {
            double d7 = (double) (this.thirdPersonDistanceTemp
                + (this.thirdPersonDistance - this.thirdPersonDistanceTemp) * partialTicks);
            if (this.mc.gameSettings.thirdPersonView == 2) {
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            }
            GL11.glTranslatef(0.0F, 0.0F, (float) (-d7));
        } else {
            GL11.glTranslatef(0.0F, 0.0F, -0.1F);
        }

        NECameraHandler.applyOrientation(entitylivingbase, partialTicks);

        GL11.glTranslatef((float) eyeDelta.x, (float) eyeDelta.y, (float) eyeDelta.z);

        Vector3d eye = stance.eyePoint();
        this.cloudFog = this.mc.renderGlobal.hasCloudFog(eye.x, eye.y, eye.z, partialTicks);

        ci.cancel();
    }
}
