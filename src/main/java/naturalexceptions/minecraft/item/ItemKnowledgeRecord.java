package naturalexceptions.minecraft.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import naturalexceptions.core.knowledge.KnowledgeEvidence;
import naturalexceptions.core.knowledge.KnowledgeSubjectRef;
import naturalexceptions.core.knowledge.PlayerKnowledge;
import naturalexceptions.minecraft.knowledge.KnowledgeRecordNbtCodec;
import naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketKnowledgeSnapshot;

/** Blank records export one subject's evidence; filled records import it as transferred evidence. */
public final class ItemKnowledgeRecord extends Item {

    public ItemKnowledgeRecord() {
        setUnlocalizedName("knowledgeRecord");
        setTextureName("naturalexceptions:knowledge_record");
        setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (stack == null || world == null || world.isRemote || player == null) return stack;
        if (KnowledgeRecordNbtCodec.isRecord(stack.getTagCompound())) {
            PlayerKnowledgeProperties props = PlayerKnowledgeProperties.get(player);
            boolean changed = KnowledgeRecordNbtCodec.importRecord(stack.getTagCompound(), props.knowledge());
            if (changed && player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                NENetwork.CHANNEL
                    .sendTo(new PacketKnowledgeSnapshot(props), (net.minecraft.entity.player.EntityPlayerMP) player);
            }
            player.addChatMessage(
                new net.minecraft.util.ChatComponentText(
                    changed ? "\u00a7eTransferred notes added to your knowledge."
                        : "\u00a77These notes are already recorded."));
            return stack;
        }
        PlayerKnowledge knowledge = PlayerKnowledgeProperties.get(player)
            .knowledge();
        for (KnowledgeSubjectRef subject : knowledge.subjects()) {
            boolean hasFacts = false;
            for (KnowledgeEvidence ignored : knowledge.evidenceFor(subject)) {
                hasFacts = true;
                break;
            }
            if (!hasFacts) continue;
            stack.setTagCompound(KnowledgeRecordNbtCodec.exportRecord(subject, knowledge));
            player.addChatMessage(
                new net.minecraft.util.ChatComponentText("\u00a7eCopied one subject's notes onto the record."));
            return stack;
        }
        player.addChatMessage(
            new net.minecraft.util.ChatComponentText("\u00a77Record an observation before copying notes."));
        return stack;
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, java.util.List lines, boolean advanced) {
        lines.add(
            KnowledgeRecordNbtCodec.isRecord(stack == null ? null : stack.getTagCompound())
                ? "\u00a77Right-click to import this record"
                : "\u00a77Right-click to copy your first recorded notes");
    }
}
