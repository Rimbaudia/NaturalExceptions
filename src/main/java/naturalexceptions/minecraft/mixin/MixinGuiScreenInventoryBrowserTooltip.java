package naturalexceptions.minecraft.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import naturalexceptions.minecraft.knowledge.client.InventoryBrowserOverlay;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenInventoryBrowserTooltip {

    @org.spongepowered.asm.mixin.Shadow
    public int width;

    @org.spongepowered.asm.mixin.Shadow
    protected FontRenderer fontRendererObj;

    @ModifyVariable(
        method = "drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        remap = false)
    private List<String> naturalexceptions$wrapTooltipLines(List<String> textLines) {
        if (textLines == null || textLines.isEmpty() || fontRendererObj == null) return textLines;
        int maxWidth = Math.max(40, Math.min(300, width - 24));
        List<String> wrapped = new ArrayList<>();
        for (String line : textLines) {
            List<String> parts = fontRendererObj.listFormattedStringToWidth(line, maxWidth);
            if (parts.isEmpty()) wrapped.add("");
            else wrapped.addAll(parts);
        }
        return wrapped;
    }

    @Inject(method = "renderToolTip", at = @At("HEAD"))
    private void naturalexceptions$drawInventoryBrowserBeforeContainerTooltip(ItemStack stack, int mouseX, int mouseY,
        CallbackInfo ci) {
        if ((Object) this instanceof GuiContainer) {
            InventoryBrowserOverlay.renderBeforeContainerTooltip((GuiContainer) (Object) this, mouseX, mouseY);
        }
    }
}
