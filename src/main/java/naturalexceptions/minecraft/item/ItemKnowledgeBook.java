package naturalexceptions.minecraft.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import java.util.List;
import naturalexceptions.content.knowledge.BuiltinKnowledgeSources;
import naturalexceptions.core.knowledge.KnowledgeSubjectRef;
import naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketKnowledgeSnapshot;

/** A physical source that adds claims only. */
public final class ItemKnowledgeBook extends Item {
    private final String sourceId;
    public ItemKnowledgeBook(String sourceId, String name, String texture) {
        this.sourceId = sourceId;
        setUnlocalizedName(name);
        setTextureName(texture);
        setMaxStackSize(1);
    }
    public String sourceId() { return sourceId; }

    @Override public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world == null || world.isRemote || player == null) return stack;
        BuiltinKnowledgeSources.Source source = BuiltinKnowledgeSources.get(sourceId);
        if (source == null) return stack;
        PlayerKnowledgeProperties props = PlayerKnowledgeProperties.get(player);
        int learned = 0;
        for (BuiltinKnowledgeSources.Claim claim : source.claims()) {
            try {
                if (props.knowledge().addEvidence(KnowledgeSubjectRef.parse(claim.subject()), claim.evidence())) {
                    learned++;
                    player.addChatMessage(new net.minecraft.util.ChatComponentText("\u00a7f• "
                        + claim.evidence().statement()));
                }
            } catch (IllegalArgumentException ignored) { }
        }
        if (learned > 0 && player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            NENetwork.CHANNEL.sendTo(new PacketKnowledgeSnapshot(props), (net.minecraft.entity.player.EntityPlayerMP) player);
        }
        player.addChatMessage(new net.minecraft.util.ChatComponentText("\u00a7eRead " + source.title() + ". "
            + (learned > 0 ? "New information added." : "Your notes already contain this information.")));
        return stack;
    }

    @Override @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        BuiltinKnowledgeSources.Source source = BuiltinKnowledgeSources.get(sourceId);
        if (source != null) lines.add("\u00a77Right-click to read " + source.title());
    }
}
