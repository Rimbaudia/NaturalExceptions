package naturalexceptions.minecraft.client;

import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.core.material.VanillaResourceClassification;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;

public class NEItemTooltipEventHandler {

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (event == null || event.itemStack == null) return;

        // 1. Smelter fuel capability qualitative label
        if (TileEntityStarterSmelter.getFuelBurnTime(event.itemStack) > 0) {
            String heatLabel = TileEntityStarterSmelter.getFuelHeatLabel(event.itemStack);
            if (heatLabel != null) {
                event.toolTip.add("\u00a76Smelter Fuel: \u00a7f" + heatLabel);
            }
        }

        // 2. Non-meltable notice for vanilla resources when advanced diagnostics active
        if (event.showAdvancedItemTooltips) {
            String rejection = VanillaResourceClassification.getRejectionReason(event.itemStack);
            if (rejection != null && TileEntityStarterSmelter.getFuelBurnTime(event.itemStack) == 0) {
                event.toolTip.add("\u00a78Crucible: Non-Meltable");
            }
        }
    }
}
