package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.fabrication.TileEntityForgingAnvil;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;

/**
 * Blacksmith Tongs used for safely holding, transporting, heating, and manipulating hot workpieces.
 */
public final class ItemBlacksmithTongs extends Item {

    public static final String KEY_WORKPIECE = "Workpiece";

    public ItemBlacksmithTongs() {
        setMaxStackSize(1);
        setMaxDamage(100);
        setUnlocalizedName("blacksmithTongs");
        setTextureName("naturalexceptions:blacksmith_tongs");
        setFull3D();
    }

    public static boolean hasWorkpiece(ItemStack tongs) {
        return tongs != null && tongs.hasTagCompound()
            && tongs.getTagCompound()
                .hasKey(KEY_WORKPIECE, 10);
    }

    public static ItemStack getWorkpiece(ItemStack tongs) {
        if (!hasWorkpiece(tongs)) {
            return null;
        }
        NBTTagCompound wpTag = tongs.getTagCompound()
            .getCompoundTag(KEY_WORKPIECE);
        return ItemStack.loadItemStackFromNBT(wpTag);
    }

    public static void setWorkpiece(ItemStack tongs, ItemStack workpiece) {
        if (tongs == null) return;
        if (!tongs.hasTagCompound()) {
            tongs.setTagCompound(new NBTTagCompound());
        }
        if (workpiece != null) {
            NBTTagCompound wpTag = new NBTTagCompound();
            workpiece.writeToNBT(wpTag);
            tongs.getTagCompound()
                .setTag(KEY_WORKPIECE, wpTag);
        } else {
            clearWorkpiece(tongs);
        }
    }

    public static void clearWorkpiece(ItemStack tongs) {
        if (tongs != null && tongs.hasTagCompound()) {
            tongs.getTagCompound()
                .removeTag(KEY_WORKPIECE);
        }
    }

    @Override
    public boolean onItemUse(ItemStack tongs, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(x, y, z);

        // 0. Water / Cauldron Quenching
        net.minecraft.block.Block hitBlock = world.getBlock(x, y, z);
        boolean isWater = hitBlock == net.minecraft.init.Blocks.water
            || hitBlock == net.minecraft.init.Blocks.flowing_water
            || hitBlock == net.minecraft.init.Blocks.cauldron;
        if (isWater && hasWorkpiece(tongs)) {
            ItemStack wp = getWorkpiece(tongs);
            if (wp != null) {
                ActiveThermalState thermal = ItemGenericComponent.getThermalState(wp);
                double curTemp = thermal != null ? thermal.temperatureKelvin() : 293.15;
                if (curTemp > 373.15) {
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.fizz", 1.0F, 1.0F);
                    if (world.isRemote) {
                        for (int i = 0; i < 8; i++) {
                            world.spawnParticle(
                                "smoke",
                                x + 0.5D + (world.rand.nextDouble() - 0.5D) * 0.4D,
                                y + 0.8D,
                                z + 0.5D + (world.rand.nextDouble() - 0.5D) * 0.4D,
                                0.0D,
                                0.08D,
                                0.0D);
                        }
                    }
                }
                MaterialProperties props = thermal != null ? thermal.properties()
                    : naturalexceptions.content.material.BuiltinMaterials.COPPER_PROPERTIES;
                double massKg = thermal != null ? thermal.massKg() : 0.5;
                ActiveThermalState quenched = ActiveThermalState.atTemperature(293.15, 0.0, massKg, props);
                ItemGenericComponent.saveThermalState(wp, quenched);
                ComponentInstance comp = ItemGenericComponent.getComponent(wp);
                if (comp != null) {
                    comp.setStateKey(
                        comp.stateKey()
                            .withMicrostructure(naturalexceptions.core.material.Microstructure.QUENCHED));
                    naturalexceptions.minecraft.persistence.AssemblyNbtCodec
                        .writeComponentToNbt(comp, wp.getTagCompound());
                }
                setWorkpiece(tongs, wp);
                if (!world.isRemote) {
                    player.addChatMessage(
                        new ChatComponentText(
                            String.format(
                                "\u00a7b[Blacksmith Tongs] Quenched %s in water with a hiss.",
                                wp.getDisplayName())));
                }
                return true;
            }
        }

        // 1. Physical Hearth interaction with Starter Smelter (place / retrieve workpiece)
        if (te instanceof TileEntityStarterSmelter) {
            TileEntityStarterSmelter smelter = (TileEntityStarterSmelter) te;
            if (hasWorkpiece(tongs)) {
                if (smelter.getHearthWorkpiece() != null) {
                    if (!world.isRemote) {
                        player.addChatMessage(
                            new ChatComponentText(
                                "\u00a7e[Starter Smelter] Hearth already holds " + smelter.getHearthWorkpiece()
                                    .getDisplayName() + ". Retrieve it first."));
                    }
                    return true;
                }
                ItemStack wp = getWorkpiece(tongs);
                smelter.setHearthWorkpiece(wp);
                clearWorkpiece(tongs);
                if (!world.isRemote) {
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "fire.ignite", 1.0F, 1.0F);
                    player.addChatMessage(
                        new ChatComponentText(
                            "\u00a76[Starter Smelter] Placed " + wp.getDisplayName() + " into hearth to heat."));
                }
                return true;
            } else {
                ItemStack hearthWp = smelter.getHearthWorkpiece();
                if (hearthWp == null) {
                    if (!world.isRemote) {
                        player.addChatMessage(
                            new ChatComponentText(
                                "\u00a77[Starter Smelter] Hearth is empty. Place a workpiece to heat it."));
                    }
                    return true;
                }
                smelter.setHearthWorkpiece(null);
                setWorkpiece(tongs, hearthWp);
                if (!world.isRemote) {
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "fire.ignite", 1.0F, 1.2F);
                    player.addChatMessage(
                        new ChatComponentText(
                            "\u00a7a[Blacksmith Tongs] Safely retrieved " + hearthWp.getDisplayName()
                                + " from hearth."));
                }
                return true;
            }
        }

        // 2. Interaction with Forging Anvil
        if (te instanceof TileEntityForgingAnvil) {
            TileEntityForgingAnvil anvil = (TileEntityForgingAnvil) te;
            return anvil.tongsInteract(player, tongs);
        }

        return false;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack tongs, World world, EntityPlayer player) {
        if (player.isSneaking() && hasWorkpiece(tongs)) {
            ItemStack workpiece = getWorkpiece(tongs);
            clearWorkpiece(tongs);
            if (workpiece != null && !world.isRemote) {
                if (!player.inventory.addItemStackToInventory(workpiece)) {
                    player.dropPlayerItemWithRandomChoice(workpiece, false);
                }
                player.inventoryContainer.detectAndSendChanges();
                player.addChatMessage(
                    new ChatComponentText("\u00a7e[Blacksmith Tongs] Released " + workpiece.getDisplayName() + "."));
            }
        }
        return tongs;
    }

    @Override
    public void onUpdate(ItemStack tongs, World world, Entity entity, int slot, boolean isCurrentItem) {
        if (hasWorkpiece(tongs)) {
            ItemStack workpiece = getWorkpiece(tongs);
            if (workpiece != null && workpiece.getItem() != null) {
                workpiece.getItem()
                    .onUpdate(workpiece, world, entity, slot, false);
                setWorkpiece(tongs, workpiece);
            }
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack tongs, EntityPlayer player, List list, boolean advanced) {
        if (!hasWorkpiece(tongs)) {
            list.add("\u00a77Empty tongs");
            list.add("\u00a78Right-click mold, anvil, or hearth to manipulate hot metal.");
            return;
        }

        ItemStack workpiece = getWorkpiece(tongs);
        if (workpiece != null) {
            list.add("\u00a76Holding: \u00a7f" + workpiece.getDisplayName());
            ActiveThermalState thermal = ItemGenericComponent.getThermalState(workpiece);
            ComponentInstance comp = ItemGenericComponent.getComponent(workpiece);
            MaterialProperties props = ItemGenericComponent.resolveProperties(comp);
            double tempK = thermal != null ? thermal.temperatureKelvin() : 293.15;
            ItemGenericComponent.ThermalCondition cond = ItemGenericComponent.getThermalCondition(tempK, props);

            list.add("  \u00a77Heat: " + cond.label());

            boolean showAdvanced = advanced;
            try {
                if (org.lwjgl.input.Keyboard.isCreated()) {
                    showAdvanced |= org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                        || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
                }
            } catch (Throwable ignored) {}

            if (advanced && thermal != null) {
                list.add(
                    String.format(
                        "  \u00a78Temperature: %.1f K (%.1f \u00b0C)",
                        thermal.temperatureKelvin(),
                        thermal.temperatureKelvin() - 273.15));
            }
            list.add("\u00a78Sneak right-click air to release workpiece.");
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack tongs) {
        if (!hasWorkpiece(tongs)) {
            return "Blacksmith Tongs";
        }
        ItemStack workpiece = getWorkpiece(tongs);
        if (workpiece != null) {
            return "Blacksmith Tongs (" + workpiece.getDisplayName() + ")";
        }
        return "Blacksmith Tongs";
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private net.minecraft.util.IIcon workpieceIcon;

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public void registerIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
        itemIcon = register.registerIcon("naturalexceptions:blacksmith_tongs");
        workpieceIcon = register.registerIcon("naturalexceptions:tongs_workpiece");
    }

    /** Pass 0: the tongs; pass 1: the stock held in the jaws, glowing if hot. */
    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.IIcon getIcon(ItemStack stack, int pass) {
        return pass == 1 && hasWorkpiece(stack) ? workpieceIcon : itemIcon;
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        if (pass != 1 || !hasWorkpiece(stack)) return 0xFFFFFF;
        ItemStack workpiece = getWorkpiece(stack);
        naturalexceptions.core.fabrication.ComponentInstance component = ItemGenericComponent.getComponent(workpiece);
        if (component == null) return 0xFFFFFF;
        return HeatWords.seenMetalColour(
            component.materialRef(),
            component.stateKey(),
            ItemGenericComponent.getThermalState(workpiece));
    }
}
