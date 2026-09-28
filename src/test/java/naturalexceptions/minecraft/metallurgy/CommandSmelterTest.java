package naturalexceptions.minecraft.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mojang.authlib.GameProfile;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

final class CommandSmelterTest {

    public static class TestCommandPlayer extends EntityPlayer {

        public List<String> messages;
        private ItemStack heldItem;

        public TestCommandPlayer() {
            super(null, new GameProfile(UUID.randomUUID(), "TestPlayer"));
        }

        public static TestCommandPlayer create() {
            try {
                java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
                TestCommandPlayer player = (TestCommandPlayer) unsafe.allocateInstance(TestCommandPlayer.class);
                player.messages = new ArrayList<>();
                player.inventory = new InventoryPlayer(player);
                return player;
            } catch (Exception e) {
                throw new RuntimeException("Failed to allocate TestCommandPlayer", e);
            }
        }

        @Override
        public ItemStack getHeldItem() {
            return heldItem;
        }

        public void setHeldItem(ItemStack stack) {
            this.heldItem = stack;
        }

        @Override
        public void addChatMessage(IChatComponent chat) {
            messages.add(
                chat.getUnformattedText()
                    .replaceAll("(?i)\u00a7[0-9a-fk-or]", ""));
        }

        @Override
        public boolean canCommandSenderUseCommand(int perm, String cmd) {
            return true;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates(0, 0, 0);
        }

        @Override
        public World getEntityWorld() {
            return null;
        }
    }

    private static class TestConsoleSender implements ICommandSender {

        final List<String> messages = new ArrayList<>();

        @Override
        public String getCommandSenderName() {
            return "Server";
        }

        @Override
        public IChatComponent func_145748_c_() {
            return new ChatComponentText("Server");
        }

        @Override
        public void addChatMessage(IChatComponent chat) {
            messages.add(
                chat.getUnformattedText()
                    .replaceAll("(?i)\u00a7[0-9a-fk-or]", ""));
        }

        @Override
        public boolean canCommandSenderUseCommand(int perm, String cmd) {
            return true;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates(0, 0, 0);
        }

        @Override
        public World getEntityWorld() {
            return null;
        }
    }

    @BeforeAll
    static void init() {
        NEItems.ensureBootstrapped();
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void commandMetadataMatchesSpecification() {
        CommandSmelter cmd = new CommandSmelter();
        assertEquals("ne_smelter", cmd.getCommandName());
        assertEquals(0, cmd.getRequiredPermissionLevel());
        assertTrue(
            cmd.getCommandUsage(new TestConsoleSender())
                .contains("/ne_smelter"));
    }

    @Test
    void structureCommandPrintsBuildingGuide() {
        CommandSmelter cmd = new CommandSmelter();
        TestConsoleSender sender = new TestConsoleSender();

        cmd.processCommand(sender, new String[] { "structure" });

        String output = String.join("\n", sender.messages);
        assertTrue(output.contains("Variable Starter Smelter"));
        assertTrue(output.contains("Minimum shell"));
        assertTrue(output.contains("3-7 blocks"));
        assertTrue(output.contains("cavity height 1-3"));
        assertTrue(output.contains("25 L usable volume"));
        assertTrue(output.contains("Tuyere"));
        assertTrue(output.contains("Outlet"));
    }

    @Test
    void helpCommandPrintsAllSubcommands() {
        CommandSmelter cmd = new CommandSmelter();
        TestConsoleSender sender = new TestConsoleSender();

        cmd.processCommand(sender, new String[] { "help" });

        String output = String.join("\n", sender.messages);
        assertTrue(output.contains("/ne_smelter structure"));
        assertTrue(output.contains("/ne_smelter mold"));
    }

    @Test
    void moldCommandRejectsConsoleSender() {
        CommandSmelter cmd = new CommandSmelter();
        TestConsoleSender sender = new TestConsoleSender();

        cmd.processCommand(sender, new String[] { "mold" });

        String output = String.join("\n", sender.messages);
        assertTrue(output.contains("must be run by a player holding a mold"));
    }

    @Test
    void moldCommandRequiresHeldMold() {
        CommandSmelter cmd = new CommandSmelter();
        TestCommandPlayer player = TestCommandPlayer.create();
        player.setHeldItem(null);

        cmd.processCommand(player, new String[] { "mold" });

        String output = String.join("\n", player.messages);
        assertTrue(output.contains("Please hold a Casting Mold"));
    }

    @Test
    void moldCommandInspectsEmptyMold() {
        CommandSmelter cmd = new CommandSmelter();
        TestCommandPlayer player = TestCommandPlayer.create();
        ItemStack emptyMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        player.setHeldItem(emptyMold);

        cmd.processCommand(player, new String[] { "mold" });

        String output = String.join("\n", player.messages);
        assertTrue(output.contains("Casting Mold Visual Diagnostics"));
        assertTrue(output.contains("Geometry:"));
        assertTrue(output.contains("pick_head"));
        assertTrue(output.contains("Filled: false"));
        assertTrue(output.contains("Payload Status: Clean"));
    }

    @Test
    void moldCommandInspectsFilledMoldWithDynamicTint() {
        CommandSmelter cmd = new CommandSmelter();
        TestCommandPlayer player = TestCommandPlayer.create();
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        MaterialQuantity mass = ComponentGeometry.pickHead()
            .requiredMass(BuiltinMaterials.COPPER_PROPERTIES);
        ActiveThermalState moltenThermal = ActiveThermalState
            .atTemperature(1450.0, 1.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer layer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                .asMolten(),
            BuiltinMaterials.COPPER_COMPOSITION,
            mass,
            moltenThermal,
            AggregatePhaseType.MOLTEN_METAL);
        ItemCastingMold.fillMold(mold, layer);
        player.setHeldItem(mold);

        cmd.processCommand(player, new String[] { "visual" });

        String output = String.join("\n", player.messages);
        assertTrue(output.contains("Geometry:"));
        assertTrue(output.contains("pick_head"));
        assertTrue(output.contains("Filled: true"));
        assertTrue(
            output.contains(
                "Material: " + BuiltinMaterials.COPPER.ref()
                    .asString()));
        assertTrue(output.contains("Pass 1 Tint: #"));
        assertTrue(output.contains("Temperature: 1450.0 K"));
    }

    @Test
    void moldCommandReportsMalformedPayloadDiagnostic() {
        CommandSmelter cmd = new CommandSmelter();
        TestCommandPlayer player = TestCommandPlayer.create();
        ItemStack corrupted = ItemCastingMold.createStack(GeometryId.PLATE);
        NBTTagCompound nbt = corrupted.getTagCompound();
        nbt.setBoolean(ItemCastingMold.KEY_FILLED, true);
        nbt.setString(
            ItemCastingMold.KEY_MATERIAL_REF,
            BuiltinMaterials.COPPER.ref()
                .asString());
        nbt.setString(ItemCastingMold.KEY_STATE_KEY, "invalid_state!@#");
        nbt.setLong(ItemCastingMold.KEY_MASS_MG, 50000L);
        player.setHeldItem(corrupted);

        cmd.processCommand(player, new String[] { "mold" });

        String output = String.join("\n", player.messages);
        assertTrue(output.contains("Diagnostic:"));
        assertTrue(output.contains("extraction is blocked"));
    }

    @Test
    void tabCompletionProvidesSubcommands() {
        CommandSmelter cmd = new CommandSmelter();
        TestConsoleSender sender = new TestConsoleSender();

        List<?> options = cmd.addTabCompletionOptions(sender, new String[] { "" });
        assertNotNull(options);
        assertTrue(options.contains("structure"));
        assertTrue(options.contains("mold"));
        assertTrue(options.contains("visual"));
        assertTrue(options.contains("help"));

        List<?> prefixOptions = cmd.addTabCompletionOptions(sender, new String[] { "m" });
        assertNotNull(prefixOptions);
        assertTrue(prefixOptions.contains("mold"));
        assertFalse(prefixOptions.contains("structure"));
    }
}
