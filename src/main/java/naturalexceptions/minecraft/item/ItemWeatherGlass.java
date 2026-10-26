package naturalexceptions.minecraft.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.core.worldgen.SkyDungeon;
import naturalexceptions.core.worldgen.SkyIslands;
import naturalexceptions.minecraft.worldgen.DungeonBeacons;

/**
 * A Weather Glass: a sealed tube of clear liquid in which white crystals grow and lean, as the sky above it changes.
 * This one leans toward land in the sky. Held, a thin drift of mist runs from it toward the nearest sky island
 * overhead (shot with violet for one that holds a dungeon); used, it says how far, which way and how high.
 */
public final class ItemWeatherGlass extends Item {

    /** How far round it feels for islands, in blocks. */
    public static final int REACH = 384;
    /** How far along the lean its mist runs. */
    static final double STREAM = 6.0;

    public ItemWeatherGlass() {
        setMaxStackSize(1);
        setUnlocalizedName("weatherGlass");
        setTextureName("naturalexceptions:weather_glass");
    }

    /** The nearest sky island to (x, z) in the overworld, within {@link #REACH}, or null. */
    public static SkyIslands.Island nearest(World world, double x, double z) {
        if (world == null || world.provider.dimensionId != 0) return null;
        SkyIslands islands = DungeonBeacons.field(world);
        SkyIslands.Island best = null;
        double bestDistance = (double) REACH * REACH;
        for (SkyIslands.Island island : islands
            .near((int) x - REACH, (int) z - REACH, (int) x + REACH, (int) z + REACH)) {
            double dx = island.centreX() + 0.5 - x, dz = island.centreZ() + 0.5 - z;
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                best = island;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
        if (!held || world.isRemote || !(entity instanceof EntityPlayer) || world.getTotalWorldTime() % 20 != 0) return;
        SkyIslands.Island island = nearest(world, entity.posX, entity.posZ);
        if (island != null) lean((EntityPlayer) entity, island, STREAM);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) return stack;
        SkyIslands.Island island = nearest(world, player.posX, player.posZ);
        if (island == null) {
            player.addChatComponentMessage(
                new ChatComponentText(
                    world.provider.dimensionId == 0 ? "§7The crystals settle straight: no land in the sky near."
                        : "§7The crystals lie in a heap: there is no sky here to read."));
            return stack;
        }
        player.addChatComponentMessage(new ChatComponentText("§b" + reading(player, island) + "."));
        lean(player, island, STREAM * 2);
        world.playSoundAtEntity(player, "random.glass", 0.3F, 1.8F);
        return stack;
    }

    /** What the glass says of {@code island} from where {@code player} stands. */
    static String reading(EntityPlayer player, SkyIslands.Island island) {
        double dx = island.centreX() + 0.5 - player.posX, dz = island.centreZ() + 0.5 - player.posZ;
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        int up = island.centreY() - (int) Math.floor(player.posY);
        String what = island.tier() == SkyIslands.Tier.GRAND ? "a great island" : "an island";
        SkyDungeon.Kind kind = island.hasDungeon() ? DungeonBeacons.field(player.worldObj)
            .dungeonKind(island) : null;
        String held = kind == SkyDungeon.Kind.CITADEL ? ", and the glass clouds dark: a city stands on it"
            : kind == SkyDungeon.Kind.SANCTUM ? ", and the glass clouds dark: the dead lie under it"
                : kind != null ? ", and the glass clouds dark: something is built in it" : "";
        String way = distance < 12 ? "straight overhead" : DungeonBeacons.compass(dx, dz) + ", " + distance + " blocks";
        String height = up > 0 ? up + " blocks up" : "below you";
        return "The crystals lean toward " + what + ": " + way + ", " + height + held;
    }

    /** A thin drift of mist from the player's hand toward the island's middle, violet if it holds a dungeon. */
    private static void lean(EntityPlayer player, SkyIslands.Island island, double length) {
        if (!(player.worldObj instanceof WorldServer)) return;
        double x = player.posX, y = player.posY + player.getEyeHeight() - 0.4, z = player.posZ;
        double dx = island.centreX() + 0.5 - x, dy = island.centreY() - y, dz = island.centreZ() + 0.5 - z;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 1.0) return;
        dx /= d;
        dy /= d;
        dz /= d;
        String mist = island.hasDungeon() ? "witchMagic" : "cloud";
        for (double t = 1.0; t < length; t += 0.6) ((WorldServer) player.worldObj)
            .func_147487_a(mist, x + dx * t, y + dy * t, z + dz * t, 1, 0.03, 0.03, 0.03, 0.0);
    }
}
