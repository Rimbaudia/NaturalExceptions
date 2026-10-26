package naturalexceptions.minecraft.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
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
        if (cpw.mods.fml.common.FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            try {
                clientHandler = Class
                    .forName("naturalexceptions.minecraft.knowledge.client.PacketKnowledgeSnapshotHandler")
                    .asSubclass(IMessageHandler.class);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Client knowledge snapshot handler is missing", e);
            }
        } else {
            clientHandler = PacketKnowledgeSnapshot.ServerRegistrationHandler.class;
        }
        registerClientboundSnapshotHandler(clientHandler);
        CHANNEL.registerMessage(PacketP8Status.Handler.class, PacketP8Status.class, 2, Side.CLIENT);
        CHANNEL.registerMessage(PacketBodyStatus.Handler.class, PacketBodyStatus.class, 3, Side.CLIENT);
        CHANNEL.registerMessage(PacketGearAppearance.Handler.class, PacketGearAppearance.class, 4, Side.CLIENT);
        CHANNEL.registerMessage(PacketOpenGear.Handler.class, PacketOpenGear.class, 5, Side.SERVER);
        CHANNEL.registerMessage(PacketTreatRegion.Handler.class, PacketTreatRegion.class, 6, Side.SERVER);
        CHANNEL.registerMessage(PacketGearSync.Handler.class, PacketGearSync.class, 7, Side.CLIENT);
        CHANNEL.registerMessage(PacketGearAction.Handler.class, PacketGearAction.class, 8, Side.SERVER);
        CHANNEL.registerMessage(PacketDungeonChart.Handler.class, PacketDungeonChart.class, 9, Side.CLIENT);
        CHANNEL.registerMessage(PacketHoistHaul.Handler.class, PacketHoistHaul.class, 10, Side.SERVER);
        CHANNEL.registerMessage(PacketUpdraft.Handler.class, PacketUpdraft.class, 11, Side.CLIENT);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void registerClientboundSnapshotHandler(Class handler) {
        CHANNEL.registerMessage(handler, PacketKnowledgeSnapshot.class, 1, Side.CLIENT);
    }
}
