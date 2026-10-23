package naturalexceptions.minecraft.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import naturalexceptions.core.knowledge.KnowledgeSubjectRef;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.minecraft.gui.ContainerMaterialNaming;
import naturalexceptions.minecraft.item.MaterialNamingTarget;
import naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties;

/**
 * Network packet sent from client to server when a player assigns a custom alloy name in the naming UI.
 */
public class PacketApplyAlloyName implements IMessage {

    private String subjectString;
    private String customName;

    public PacketApplyAlloyName() {}

    public PacketApplyAlloyName(MaterialRef ref, String customName) {
        this.subjectString = ref != null ? KnowledgeSubjectRef.material(ref)
            .asString() : "";
        this.customName = customName != null ? customName.trim() : "";
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.subjectString = ByteBufUtils.readUTF8String(buf);
        this.customName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, subjectString != null ? subjectString : "");
        ByteBufUtils.writeUTF8String(buf, customName != null ? customName : "");
    }

    public static class Handler implements IMessageHandler<PacketApplyAlloyName, IMessage> {

        @Override
        public IMessage onMessage(PacketApplyAlloyName message, MessageContext ctx) {
            if (message == null || message.customName == null || message.customName.isEmpty()) {
                return null;
            }
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }
            final String rawName = message.customName;
            final String rawSubject = message.subjectString;
            // SimpleImpl handlers may run on Netty. Validate the live container and mutate player data on the world
            // thread.
            naturalexceptions.minecraft.network.MainThreadTasks.submit(new Runnable() {

                @Override
                public void run() {
                    if (player.worldObj == null) return;
                    String trimmedName = rawName.trim();
                    if (trimmedName.isEmpty() || !(player.openContainer instanceof ContainerMaterialNaming)
                        || !player.openContainer.canInteractWith(player)) return;
                    if (trimmedName.length() > 32) trimmedName = trimmedName.substring(0, 32);
                    try {
                        KnowledgeSubjectRef subject = KnowledgeSubjectRef.parse(rawSubject);
                        MaterialRef requested = subject.materialRefOrNull();
                        ItemStack held = player.getHeldItem();
                        if (held == null || !MaterialNamingTarget.canOpenNaming(held)) return;
                        MaterialRef heldRef = MaterialNamingTarget.getMaterialRef(held);
                        if (requested == null || !requested.equals(heldRef)) return;
                        PlayerKnowledgeProperties props = PlayerKnowledgeProperties.get(player);
                        props.knowledge()
                            .setAlias(subject, trimmedName);
                        NENetwork.CHANNEL.sendTo(new PacketKnowledgeSnapshot(props), player);
                        player.closeScreen();
                        player.addChatMessage(
                            new ChatComponentText(
                                "\u00a7a[Metallurgy] Registered custom alloy name: \u00a7f" + trimmedName
                                    + " \u00a77(personal name)"));
                    } catch (IllegalArgumentException ignored) {
                        // Reject malformed material identities without renaming an unrelated held item.
                    }
                }
            });

            return null;
        }
    }

}
