package naturalexceptions.minecraft.weapon;

/** The Shardcaster's ammunition rule: each Lumenite shard loaded gives {@link #SHOTS_PER_SHARD} bolts. */
public final class ShardcasterCharge {

    public static final int SHOTS_PER_SHARD = 8;

    private ShardcasterCharge() {}

    /** Result of trying to fire with {@code charge} bolts loaded: {newCharge, shardsUsed}, or null if it cannot. */
    public static int[] fire(int charge, boolean shardAvailable) {
        if (charge > 0) return new int[] { charge - 1, 0 };
        if (!shardAvailable) return null;
        return new int[] { SHOTS_PER_SHARD - 1, 1 };
    }
}
