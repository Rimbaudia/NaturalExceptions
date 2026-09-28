package naturalexceptions.minecraft.knowledge.client;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import naturalexceptions.minecraft.knowledge.PlayerKnowledgeNbtCodec;
import naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties;
import naturalexceptions.minecraft.network.PacketKnowledgeSnapshot;

/** Applies owner-only server snapshots on the Minecraft client thread. */
@SideOnly(Side.CLIENT)
public final class PacketKnowledgeSnapshotHandler implements IMessageHandler<PacketKnowledgeSnapshot, IMessage> {
    @Override public IMessage onMessage(final PacketKnowledgeSnapshot message, MessageContext context) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return null;
        mc.func_152344_a(new Runnable() {
            @Override public void run() {
                if (mc.thePlayer != null) {
                    PlayerKnowledgeProperties.get(mc.thePlayer)
                        .replace(PlayerKnowledgeNbtCodec.read(message.payload()));
                }
            }
        });
        return null;
    }
}
