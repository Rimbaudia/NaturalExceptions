package naturalexceptions.minecraft.item;

import java.util.Locale;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import naturalexceptions.core.knowledge.KnowledgeEvidence;
import naturalexceptions.core.knowledge.KnowledgePrecision;
import naturalexceptions.core.knowledge.KnowledgeProvenance;
import naturalexceptions.core.knowledge.KnowledgeSubjectRef;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketKnowledgeSnapshot;

/** Minimal instrument: assay the first sample in inventory and record approximate density. */
public final class ItemFieldAssay extends Item {
    public ItemFieldAssay() { setUnlocalizedName("fieldAssay"); setTextureName("naturalexceptions:field_assay"); setMaxStackSize(1); }

    @Override public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world == null || world.isRemote || player == null) return stack;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack sample = player.inventory.getStackInSlot(i);
            if (sample == null || !(sample.getItem() instanceof ItemMaterialSample)) continue;
            MaterialRef ref = ItemMaterialSample.getMaterialRef(sample);
            if (ref == null) continue;
            MaterialProperties props = ItemMaterialSample.propertiesForKnowledge(sample);
            if (props == null || !(props.densityKgPerM3() > 0.0)) continue;
            double density = props.densityKgPerM3();
            double low = Math.max(0.0, density * 0.95);
            double high = density * 1.05;
            String statement = String.format(Locale.ROOT, "Density: %.0f-%.0f kg/m3", low, high);
            PlayerKnowledgeProperties knowledge = PlayerKnowledgeProperties.get(player);
            boolean changed = knowledge.knowledge().addEvidence(KnowledgeSubjectRef.material(ref), new KnowledgeEvidence(
                "density", statement, "field_assay:density:" + ref.asString(), KnowledgeProvenance.INSTRUMENT,
                KnowledgePrecision.QUANTITATIVE));
            if (changed && player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                NENetwork.CHANNEL.sendTo(new PacketKnowledgeSnapshot(knowledge), (net.minecraft.entity.player.EntityPlayerMP) player);
            }
            player.addChatMessage(new net.minecraft.util.ChatComponentText("\u00a7bField assay: " + statement));
            return stack;
        }
        player.addChatMessage(new net.minecraft.util.ChatComponentText("\u00a77No supported material sample could be measured."));
        return stack;
    }
}
