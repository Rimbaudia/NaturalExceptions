package naturalexceptions.minecraft.mixin;

import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import naturalexceptions.minecraft.knowledge.client.InventoryBrowserOverlay;

@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreativeInventoryBrowser {

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$handleCreativeInventoryBrowserInput(char typedChar, int keyCode, CallbackInfo ci) {
        if (InventoryBrowserOverlay.handleContainerKeyTyped((GuiContainerCreative) (Object) this, typedChar, keyCode)) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$handleCreativeInventoryBrowserClick(int mouseX, int mouseY, int mouseButton,
        CallbackInfo ci) {
        if (InventoryBrowserOverlay
            .handleContainerMouseClick((GuiContainerCreative) (Object) this, mouseX, mouseY, mouseButton)) ci.cancel();
    }

    @Inject(method = "mouseMovedOrUp", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$handleCreativeInventoryBrowserRelease(int mouseX, int mouseY, int state,
        CallbackInfo ci) {
        if (InventoryBrowserOverlay.shouldConsumeContainerMouse((GuiContainerCreative) (Object) this, mouseX, mouseY))
            ci.cancel();
    }

    @Inject(method = "renderToolTip", at = @At("HEAD"))
    private void naturalexceptions$drawCreativeBrowserBeforeTooltip(ItemStack stack, int mouseX, int mouseY,
        CallbackInfo ci) {
        InventoryBrowserOverlay.renderBeforeContainerTooltip((GuiContainerCreative) (Object) this, mouseX, mouseY);
    }

    @Inject(method = "renderCreativeInventoryHoveringText", at = @At("HEAD"))
    private void naturalexceptions$drawCreativeBrowserBeforeTabTooltip(CreativeTabs tab, int mouseX, int mouseY,
        CallbackInfoReturnable<Boolean> cir) {
        InventoryBrowserOverlay.renderBeforeContainerTooltip((GuiContainerCreative) (Object) this, mouseX, mouseY);
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void naturalexceptions$drawCreativeInventoryBrowserLast(int mouseX, int mouseY, float partialTicks,
        CallbackInfo ci) {
        InventoryBrowserOverlay.renderAfterCreativeContainer((GuiContainerCreative) (Object) this, mouseX, mouseY);
    }
}
