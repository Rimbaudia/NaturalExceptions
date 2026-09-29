package naturalexceptions.minecraft.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import naturalexceptions.minecraft.knowledge.client.InventoryBrowserOverlay;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainerInventoryBrowser {

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$handleInventoryBrowserInput(char typedChar, int keyCode, CallbackInfo ci) {
        if (InventoryBrowserOverlay.handleContainerKeyTyped((GuiContainer) (Object) this, typedChar, keyCode)) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$handleInventoryBrowserClick(int mouseX, int mouseY, int mouseButton,
        CallbackInfo ci) {
        if (naturalexceptions.minecraft.gear.client.InventoryTabs
            .handleClick((GuiContainer) (Object) this, mouseX, mouseY, mouseButton)) {
            ci.cancel();
            return;
        }
        if (naturalexceptions.minecraft.biology.client.BodyStatusPanel
            .handleClick((GuiContainer) (Object) this, mouseX, mouseY, mouseButton)) {
            ci.cancel();
            return;
        }
        if (InventoryBrowserOverlay
            .handleContainerMouseClick((GuiContainer) (Object) this, mouseX, mouseY, mouseButton)) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseMovedOrUp", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$handleInventoryBrowserRelease(int mouseX, int mouseY, int state, CallbackInfo ci) {
        if (naturalexceptions.minecraft.biology.client.BodyStatusPanel
            .consumes((GuiContainer) (Object) this, mouseX, mouseY)) {
            ci.cancel();
            return;
        }
        if (InventoryBrowserOverlay.shouldConsumeContainerMouse((GuiContainer) (Object) this, mouseX, mouseY)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void naturalexceptions$drawInventoryBrowserAfterContainer(int mouseX, int mouseY, float partialTicks,
        CallbackInfo ci) {
        InventoryBrowserOverlay.renderAtContainerTail((GuiContainer) (Object) this, mouseX, mouseY);
    }
}
