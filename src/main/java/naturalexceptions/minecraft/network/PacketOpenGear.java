package naturalexceptions.minecraft.network;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import naturalexceptions.NaturalExceptions;
import naturalexceptions.minecraft.gui.NEGuiHandler;

/** Client-to-server: open the Gear screen (it has server-side slots, so the server opens it). */
public final class PacketOpenGear implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static final class Handler implements IMessageHandler<PacketOpenGear, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenGear message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) return null;
            // Opening a container swaps the player's openContainer: on the main thread (see MainThreadTasks).
            MainThreadTasks.submit(player.getUniqueID(), new Runnable() {

                @Override
                public void run() {
                    if (player.isDead || player.inventory.getItemStack() != null) return;
                    player.openGui(NaturalExceptions.instance, NEGuiHandler.GUI_GEAR, player.worldObj, 0, 0, 0);
                }
            });
            return null;
        }
    }
}
