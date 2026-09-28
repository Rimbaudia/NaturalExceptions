package naturalexceptions.minecraft.knowledge;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.IExtendedEntityProperties;
import naturalexceptions.core.knowledge.PlayerKnowledge;

/** Player-owned, persistent knowledge. Server mutations are explicitly synchronized to that player only. */
public final class PlayerKnowledgeProperties implements IExtendedEntityProperties {
    public static final String KEY = "naturalexceptions.player_knowledge.v1";
    private PlayerKnowledge knowledge = new PlayerKnowledge();
    private boolean manualDelivered;

    public static PlayerKnowledgeProperties get(EntityPlayer player) {
        if (player == null) return null;
        IExtendedEntityProperties current = player.getExtendedProperties(KEY);
        if (current instanceof PlayerKnowledgeProperties) return (PlayerKnowledgeProperties) current;
        PlayerKnowledgeProperties created = new PlayerKnowledgeProperties();
        player.registerExtendedProperties(KEY, created);
        return created;
    }

    public PlayerKnowledge knowledge() { return knowledge; }
    public boolean manualDelivered() { return manualDelivered; }
    public void markManualDelivered() { manualDelivered = true; }

    public void replace(PlayerKnowledge value) { knowledge = value == null ? new PlayerKnowledge() : value; }

    @Override public void saveNBTData(NBTTagCompound compound) {
        if (compound == null) return;
        compound.setTag("Knowledge", PlayerKnowledgeNbtCodec.write(knowledge));
        compound.setBoolean("ManualDelivered", manualDelivered);
    }

    @Override public void loadNBTData(NBTTagCompound compound) {
        if (compound == null) return;
        knowledge = PlayerKnowledgeNbtCodec.read(compound.hasKey("Knowledge", 10)
            ? compound.getCompoundTag("Knowledge") : null);
        manualDelivered = compound.getBoolean("ManualDelivered");
    }

    @Override public void init(Entity entity, net.minecraft.world.World world) { }

    public void copyFrom(PlayerKnowledgeProperties source) {
        if (source == null) return;
        NBTTagCompound copy = new NBTTagCompound();
        source.saveNBTData(copy);
        loadNBTData(copy);
    }
}
