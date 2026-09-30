package naturalexceptions.minecraft.mixin;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainerCreative;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainerCreative.class)
public interface GuiContainerCreativeAccessor {

    @Accessor("searchField")
    GuiTextField naturalexceptions$getSearchField();
}
