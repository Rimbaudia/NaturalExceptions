package naturalexceptions.minecraft.furniture;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import naturalexceptions.minecraft.biology.BiologicalProperties;

/**
 * Eating at a table or from a pantry shelf: a proper meal. The food does all it would if eaten from the hand (the
 * body is fed through the same event), and the sit-down meal nourishes half as much again.
 */
public final class Meals {

    /** Extra nourishment for a proper meal, as a share of the food's own. */
    public static final double BONUS = 0.5;

    private Meals() {}

    /** Whether the player would eat this now. */
    public static boolean wouldEat(EntityPlayer player, ItemStack food) {
        return food != null && food.getItem() instanceof ItemFood && player.canEat(false);
    }

    /** Eats one of {@code food} (the stack given is used up by one); server side. */
    public static void eat(World world, EntityPlayer player, ItemStack food) {
        ItemStack one = food.splitStack(1);
        ItemFood item = (ItemFood) one.getItem();
        int nourishment = item.func_150905_g(one);
        ItemStack left = item.onEaten(one, world, player);
        net.minecraftforge.event.ForgeEventFactory.onItemUseFinish(player, one, 0, left);
        BiologicalProperties.get(player)
            .state()
            .replenishEnergyJoules(
                Math.max(1, nourishment) * BiologicalProperties.get(player)
                    .state()
                    .physiology()
                    .energyPerHungerPoint() * BONUS);
        world.playSoundAtEntity(player, "random.burp", 0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
    }
}
