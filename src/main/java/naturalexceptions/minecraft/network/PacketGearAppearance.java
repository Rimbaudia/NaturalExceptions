package naturalexceptions.minecraft.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server-to-client: what a player wears on their face and back, so it can be drawn on them. */
public final class PacketGearAppearance implements IMessage {

    /** Client-side: entity id to face item, for rendering. Entries are dropped when the item is removed. */
    public static final Map<Integer, ItemStack> CLIENT_FACES = new ConcurrentHashMap<>();
    /** Client-side: entity id to back item (a glider cloak), for rendering. */
    public static final Map<Integer, ItemStack> CLIENT_BACKS = new ConcurrentHashMap<>();

    private int entityId;
    private ItemStack face, back;

    public PacketGearAppearance() {}

    public PacketGearAppearance(int entityId, ItemStack face) {
        this(entityId, face, null);
    }

    public PacketGearAppearance(int entityId, ItemStack face, ItemStack back) {
        this.entityId = entityId;
        this.face = face == null ? null : face.copy();
        this.back = back == null ? null : back.copy();
    }

    public ItemStack back() {
        return back;
    }

    public int entityId() {
        return entityId;
    }

    public ItemStack face() {
        return face;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        face = ByteBufUtils.readItemStack(buf);
        back = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        ByteBufUtils.writeItemStack(buf, face);
        ByteBufUtils.writeItemStack(buf, back);
    }

    /** Only stores data, so it is safe to register on both physical sides. */
    public static final class Handler implements IMessageHandler<PacketGearAppearance, IMessage> {

        @Override
        public IMessage onMessage(PacketGearAppearance message, MessageContext ctx) {
            if (message == null) return null;
            if (message.face == null) CLIENT_FACES.remove(message.entityId);
            else CLIENT_FACES.put(message.entityId, message.face);
            if (message.back == null) CLIENT_BACKS.remove(message.entityId);
            else CLIENT_BACKS.put(message.entityId, message.back);
            return null;
        }
    }
}
