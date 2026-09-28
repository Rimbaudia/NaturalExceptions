package naturalexceptions.minecraft.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.relauncher.Side;

public final class NENetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("NaturalExceptions");
    private static boolean initialized = false;

    private NENetwork() {}

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        CHANNEL.registerMessage(PacketApplyAlloyName.Handler.class, PacketApplyAlloyName.class, 0, Side.SERVER);
        Class<? extends IMessageHandler> clientHandler;
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient()) {
            try {
                clientHandler = Class.forName(
                    "naturalexceptions.minecraft.knowledge.client.PacketKnowledgeSnapshotHandler")
                    .asSubclass(IMessageHandler.class);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Client knowledge snapshot handler is missing", e);
            }
        } else {
            clientHandler = PacketKnowledgeSnapshot.ServerRegistrationHandler.class;
        }
        registerClientboundSnapshotHandler(clientHandler);
        CHANNEL.registerMessage(PacketP8Status.Handler.class, PacketP8Status.class, 2, Side.CLIENT);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void registerClientboundSnapshotHandler(Class handler) {
        CHANNEL.registerMessage(handler, PacketKnowledgeSnapshot.class, 1, Side.CLIENT);
    }
}
