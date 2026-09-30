package naturalexceptions.minecraft.gear;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketGearAppearance;
import naturalexceptions.minecraft.network.PacketGearSync;

/** Gear lifecycle: attach to players, keep through respawn when the world keeps inventories, drop on death. */
public final class GearEvents {

    @SubscribeEvent
    public void onConstruct(EntityEvent.EntityConstructing event) {
        if (event != null && event.entity instanceof EntityPlayer) PlayerGear.get((EntityPlayer) event.entity);
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (event == null || event.original == null || event.entityPlayer == null) return;
        if (event.wasDeath && !keepsInventory(event.entityPlayer)) return;
        PlayerGear.get(event.entityPlayer)
            .copyFrom(PlayerGear.get(event.original));
    }

    @SubscribeEvent
    public void onDrops(PlayerDropsEvent event) {
        if (event == null || event.entityPlayer == null || event.entityPlayer.worldObj.isRemote) return;
        EntityPlayer player = event.entityPlayer;
        if (keepsInventory(player)) return;
        for (ItemStack stack : PlayerGear.get(player)
            .takeAll()) {
            EntityItem item = new EntityItem(player.worldObj, player.posX, player.posY + 0.5, player.posZ, stack);
            item.delayBeforeCanPickup = 40;
            event.drops.add(item);
        }
    }

    /** Once a second: let worn items act, and tell watchers when the face changed. */
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null || !(event.entityLiving instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;
        if (player.ticksExisted % 20 != 0) return;
        PlayerGear gear = PlayerGear.get(player);
        for (GearSlot slot : GearSlot.values()) {
            ItemStack stack = gear.get(slot);
            if (stack != null && stack.getItem() instanceof IWearable)
                ((IWearable) stack.getItem()).onWornTick(player, stack);
        }
        if (gear.consumeAppearanceDirty()) broadcastAppearance(player, gear);
        if (gear.consumeOwnerDirty()) NENetwork.CHANNEL.sendTo(new PacketGearSync(gear.snapshot()), player);
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event == null || !(event.target instanceof EntityPlayer) || !(event.entityPlayer instanceof EntityPlayerMP))
            return;
        EntityPlayer target = (EntityPlayer) event.target;
        NENetwork.CHANNEL.sendTo(
            new PacketGearAppearance(
                target.getEntityId(),
                PlayerGear.get(target)
                    .get(GearSlot.FACE),
                PlayerGear.get(target)
                    .get(GearSlot.BACK)),
            (EntityPlayerMP) event.entityPlayer);
    }

    static void broadcastAppearance(EntityPlayerMP player, PlayerGear gear) {
        PacketGearAppearance packet = new PacketGearAppearance(
            player.getEntityId(),
            gear.get(GearSlot.FACE),
            gear.get(GearSlot.BACK));
        NENetwork.CHANNEL.sendTo(packet, player);
        if (player.worldObj instanceof WorldServer) {
            for (Object watcher : ((WorldServer) player.worldObj).getEntityTracker()
                .getTrackingPlayers(player)) {
                if (watcher instanceof EntityPlayerMP) NENetwork.CHANNEL.sendTo(packet, (EntityPlayerMP) watcher);
            }
        }
    }

    static boolean keepsInventory(EntityPlayer player) {
        return player.worldObj != null && player.worldObj.getGameRules()
            .getGameRuleBooleanValue("keepInventory");
    }
}
