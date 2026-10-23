package naturalexceptions.minecraft.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import naturalexceptions.minecraft.furniture.EntityHoistLoad;
import naturalexceptions.minecraft.furniture.TileEntityHoist;

/**
 * Client-to-server: a rider in a hoist's basket is working its hand-line this moment, hauling it up (-1) or letting
 * it down (+1). Sent while they hold use, looking up or down.
 */
public final class PacketHoistHaul implements IMessage {

    private int way;

    public PacketHoistHaul() {}

    public PacketHoistHaul(int way) {
        this.way = way;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        way = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(way);
    }

    public static final class Handler implements IMessageHandler<PacketHoistHaul, IMessage> {

        @Override
        public IMessage onMessage(PacketHoistHaul message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null || message == null) return null;
            final int way = message.way < 0 ? -1 : 1;
            // Drives the hoist the game tick is stepping: on the main thread (see MainThreadTasks).
            MainThreadTasks.submit(new Runnable() {

                @Override
                public void run() {
                    if (player.isDead || !(player.ridingEntity instanceof EntityHoistLoad)) return;
                    EntityHoistLoad load = (EntityHoistLoad) player.ridingEntity;
                    TileEntity tile = player.worldObj.getTileEntity(load.windlassX, load.windlassY, load.windlassZ);
                    if (tile instanceof TileEntityHoist) ((TileEntityHoist) tile).haul(way);
                }
            });
            return null;
        }
    }
}
