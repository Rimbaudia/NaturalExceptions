package naturalexceptions.minecraft.weapon;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import naturalexceptions.minecraft.gear.RelicRules;

/**
 * The rarest Warden Relic: a two-handed maul of Skystone bound round a Gale Core. It hits hard; held back (right
 * mouse) it gathers the wind, and let go it slams the ground in a shockwave that throws everything around up and
 * away. Found only on a fallen Sky Warden.
 */
public final class ItemGaleMaul extends ItemSword {

    static final float DAMAGE = 9.0F;
    private static final int USE_TICKS = 72000;

    public ItemGaleMaul() {
        super(ToolMaterial.IRON);
        setMaxDamage(RelicRules.MAUL_DURABILITY);
        setUnlocalizedName("galeMaul");
        setTextureName("naturalexceptions:gale_maul");
        setFull3D();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Multimap getItemAttributeModifiers() {
        Multimap map = HashMultimap.create();
        map.put(
            SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
            new AttributeModifier(field_111210_e, "Weapon modifier", DAMAGE, 0));
        return map;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.bow;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return USE_TICKS;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        player.setItemInUse(stack, getMaxItemUseDuration(stack));
        if (!world.isRemote) world.playSoundAtEntity(player, "naturalexceptions:weapon.maul_charge", 0.7F, 1.0F);
        return stack;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        if (!player.worldObj.isRemote) return;
        int held = USE_TICKS - count;
        double charge = RelicRules.maulCharge(held);
        if (charge <= 0.0) return;
        double a = held * 0.5;
        double r = 1.2 - 0.6 * charge;
        player.worldObj.spawnParticle(
            "cloud",
            player.posX + Math.cos(a) * r,
            player.posY - 1.0 + charge,
            player.posZ + Math.sin(a) * r,
            0.0,
            0.03,
            0.0);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        double charge = RelicRules.maulCharge(USE_TICKS - timeLeft);
        if (charge <= 0.0 || world.isRemote) return;
        double radius = RelicRules.MAUL_RADIUS * (0.5 + 0.5 * charge);
        @SuppressWarnings("unchecked")
        List<EntityLivingBase> caught = world
            .getEntitiesWithinAABB(EntityLivingBase.class, player.boundingBox.expand(radius, 1.5, radius));
        for (EntityLivingBase target : caught) {
            if (target == player) continue;
            double dx = target.posX - player.posX, dz = target.posZ - player.posZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > radius) continue;
            double falloff = 1.0 - 0.5 * distance / radius;
            target.attackEntityFrom(
                DamageSource.causePlayerDamage(player),
                (float) (RelicRules.MAUL_MAX_DAMAGE * charge * falloff));
            if (distance < 1.0E-3) {
                dx = 1.0;
                distance = 1.0;
            }
            target.motionX = dx / distance * 1.1 * charge * falloff;
            target.motionZ = dz / distance * 1.1 * charge * falloff;
            target.motionY = 0.6 * charge * falloff + 0.2;
            target.velocityChanged = true;
        }
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            int bx = MathHelper.floor_double(player.posX), by = MathHelper.floor_double(player.boundingBox.minY) - 1,
                bz = MathHelper.floor_double(player.posZ);
            net.minecraft.block.Block ground = world.getBlock(bx, by, bz);
            if (ground.getMaterial() != net.minecraft.block.material.Material.air) server.func_147487_a(
                "blockcrack_" + net.minecraft.block.Block.getIdFromBlock(ground)
                    + "_"
                    + world.getBlockMetadata(bx, by, bz),
                player.posX,
                player.boundingBox.minY + 0.1,
                player.posZ,
                40,
                radius * 0.4,
                0.1,
                radius * 0.4,
                0.2);
            int puffs = (int) (16 + 16 * charge);
            for (int i = 0; i < puffs; i++) {
                double a = i * Math.PI * 2.0 / puffs;
                server.func_147487_a(
                    "cloud",
                    player.posX + Math.cos(a),
                    player.boundingBox.minY + 0.2,
                    player.posZ + Math.sin(a),
                    0,
                    Math.cos(a),
                    0.02,
                    Math.sin(a),
                    0.35 + 0.35 * charge);
            }
        }
        world.playSoundAtEntity(player, "naturalexceptions:weapon.maul_slam", 1.2F, 1.1F - 0.2F * (float) charge);
        stack.damageItem(2, player);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.epic;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("\u00a75Castellan's Relic");
        lines.add("\u00a77Hold right-click to gather the wind;");
        lines.add("\u00a77let go to slam a shockwave into the ground.");
    }
}
