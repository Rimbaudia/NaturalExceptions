package naturalexceptions.minecraft.knowledge;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketKnowledgeSnapshot;
import naturalexceptions.minecraft.registry.NEItems;

/** Player data lifecycle only; no polling or world scans. */
public final class PlayerKnowledgeEvents {
    @SubscribeEvent public void onConstruct(EntityEvent.EntityConstructing event) {
        if (event != null && event.entity instanceof EntityPlayer) PlayerKnowledgeProperties.get((EntityPlayer) event.entity);
    }

    @SubscribeEvent public void onClone(PlayerEvent.Clone event) {
        if (event == null || event.original == null || event.entityPlayer == null) return;
        PlayerKnowledgeProperties oldData = PlayerKnowledgeProperties.get(event.original);
        PlayerKnowledgeProperties.get(event.entityPlayer).copyFrom(oldData);
    }

    @SubscribeEvent public void onLogin(PlayerLoggedInEvent event) {
        if (event == null || event.player == null || event.player.worldObj.isRemote) return;
        PlayerKnowledgeProperties data = PlayerKnowledgeProperties.get(event.player);
        if (!data.manualDelivered()) {
            if (NEItems.startingManual != null
                && event.player.inventory.addItemStackToInventory(new ItemStack(NEItems.startingManual))) {
                data.markManualDelivered();
            }
        }
        if (event.player instanceof EntityPlayerMP) {
            NENetwork.CHANNEL.sendTo(new PacketKnowledgeSnapshot(data), (EntityPlayerMP) event.player);
        }
    }
}
