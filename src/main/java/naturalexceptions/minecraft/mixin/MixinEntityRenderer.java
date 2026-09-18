package naturalexceptions.minecraft.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import naturalexceptions.core.gravity.Vector3d;
import naturalexceptions.minecraft.gravity.NECameraHandler;
import naturalexceptions.minecraft.gravity.NEGravityService;
import naturalexceptions.minecraft.gravity.NEPlayerPresentationFrame;
import naturalexceptions.minecraft.gravity.NEPlayerPresentationService;

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

    /**
     * Disables vanilla global-Y view bobbing under nonstandard gravity to prevent
     * vertical bobbing, camera roll corruption, and artificial pitch oscillation on walls.
     */
    @Inject(method = "setupViewBobbing", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$onSetupViewBobbing(float partialTicks, CallbackInfo ci) {
        if (NEGravityService.global()
            .isNonstandardGravityActive()) {
            ci.cancel();
        }
    }

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

        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entitylivingbase, partialTicks);

        double d0 = entitylivingbase.prevPosX
            + (entitylivingbase.posX - entitylivingbase.prevPosX) * (double) partialTicks;
        double basePosY = entitylivingbase.prevPosY
            + (entitylivingbase.posY - entitylivingbase.prevPosY) * (double) partialTicks;
        double d2 = entitylivingbase.prevPosZ
            + (entitylivingbase.posZ - entitylivingbase.prevPosZ) * (double) partialTicks;

        Vector3d entityPos = new Vector3d(d0, basePosY, d2);
        Vector3d eyePoint = frame.eyePoint();
        Vector3d eyeDelta = entityPos.subtract(eyePoint);

        GL11.glRotatef(this.prevCamRoll + (this.camRoll - this.prevCamRoll) * partialTicks, 0.0F, 0.0F, 1.0F);

        Vector3d look = frame.computeLookVector();

        if (this.mc.gameSettings.thirdPersonView > 0) {
            double d7 = (double) (this.thirdPersonDistanceTemp
                + (this.thirdPersonDistance - this.thirdPersonDistanceTemp) * partialTicks);

            // Raytrace backward along -look from eyePoint to avoid camera clipping into obstacles
            if (this.mc.theWorld != null) {
                Vec3 start = Vec3.createVectorHelper(eyePoint.x, eyePoint.y, eyePoint.z);
                Vec3 end = Vec3
                    .createVectorHelper(eyePoint.x - look.x * d7, eyePoint.y - look.y * d7, eyePoint.z - look.z * d7);
                MovingObjectPosition mop = this.mc.theWorld.rayTraceBlocks(start, end);
                if (mop != null) {
                    double dist = mop.hitVec.distanceTo(start);
                    if (dist < d7) {
                        d7 = Math.max(0.1, dist - 0.1);
                    }
                }
            }

            if (this.mc.gameSettings.thirdPersonView == 2) {
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            }
            GL11.glTranslatef(0.0F, 0.0F, (float) (-d7));
        } else {
            GL11.glTranslatef(0.0F, 0.0F, -0.1F);
        }

        NECameraHandler.applyOrientation(entitylivingbase, partialTicks);

        GL11.glTranslatef((float) eyeDelta.x, (float) eyeDelta.y, (float) eyeDelta.z);

        this.cloudFog = this.mc.renderGlobal.hasCloudFog(eyePoint.x, eyePoint.y, eyePoint.z, partialTicks);

        ci.cancel();
    }
}
