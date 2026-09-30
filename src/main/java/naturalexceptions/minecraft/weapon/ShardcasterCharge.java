package naturalexceptions.minecraft.weapon;

/** The Shardcaster's ammunition rule: each Lumenite shard loaded gives {@link #SHOTS_PER_SHARD} bolts. */
public final class ShardcasterCharge {

    public static final int SHOTS_PER_SHARD = 8;
    /** The most it holds: four shards' worth. */
    public static final int MAX_CHARGE = 4 * SHOTS_PER_SHARD;

    /** How many shards a reload takes, with {@code charge} loaded and {@code shards} to hand. */
    public static int shardsToLoad(int charge, int shards) {
        return Math.max(0, Math.min(shards, (MAX_CHARGE - charge) / SHOTS_PER_SHARD));
    }

    private ShardcasterCharge() {}

    /** Result of trying to fire with {@code charge} bolts loaded: {newCharge, shardsUsed}, or null if it cannot. */
    public static int[] fire(int charge, boolean shardAvailable) {
        if (charge > 0) return new int[] { charge - 1, 0 };
        if (!shardAvailable) return null;
        return new int[] { SHOTS_PER_SHARD - 1, 1 };
    }
}
