package naturalexceptions.minecraft.biology;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayerMP;

import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketBodyStatus;

/** Sends each player their own body state when it changes, and at least every few seconds. */
final class BodyStatusSync {

    static final long REFRESH_TICKS = 100L;

    private static final class Sent {

        long fingerprint;
        long tick;
    }

    private static final Map<EntityPlayerMP, Sent> SENT = new WeakHashMap<>();

    private BodyStatusSync() {}

    static void maybeSend(EntityPlayerMP player, BiologicalState state, long now) {
        if (player == null || state == null || player.playerNetServerHandler == null) return;
        BodyStatusSnapshot snapshot = BodyStatusSnapshot.from(state);
        long fingerprint = snapshot.fingerprint();
        Sent sent;
        synchronized (SENT) {
            sent = SENT.get(player);
            if (sent == null) {
                sent = new Sent();
                sent.tick = Long.MIN_VALUE / 2;
                SENT.put(player, sent);
            }
        }
        if (!shouldSend(sent.fingerprint, sent.tick, fingerprint, now)) return;
        sent.fingerprint = fingerprint;
        sent.tick = now;
        NENetwork.CHANNEL.sendTo(new PacketBodyStatus(snapshot), player);
    }

    /** Sends now, whatever was sent before (after a treatment, so the panel answers at once). */
    static void sendNow(EntityPlayerMP player, BiologicalState state, long now) {
        synchronized (SENT) {
            SENT.remove(player);
        }
        maybeSend(player, state, now);
    }

    static boolean shouldSend(long lastFingerprint, long lastTick, long fingerprint, long now) {
        return fingerprint != lastFingerprint || now - lastTick >= REFRESH_TICKS || now < lastTick;
    }
}
