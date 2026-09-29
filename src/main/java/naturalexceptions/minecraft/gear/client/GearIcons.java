package naturalexceptions.minecraft.gear.client;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.gear.GearKind;

/** Grey silhouettes shown in empty gear slots, stitched into the item atlas like vanilla's empty armour slots. */
@SideOnly(Side.CLIENT)
public final class GearIcons {

    /** Item atlas texture type in 1.7.10's {@code TextureMap}. */
    private static final int ITEM_ATLAS = 1;
    private static final Map<GearKind, IIcon> ICONS = new EnumMap<>(GearKind.class);

    private GearIcons() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new GearIcons());
    }

    @SubscribeEvent
    public void onStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != ITEM_ATLAS) return;
        for (GearKind kind : GearKind.values()) ICONS.put(kind, event.map.registerIcon(kind.slotIcon()));
    }

    public static IIcon slotIcon(GearKind kind) {
        return ICONS.get(kind);
    }
}
