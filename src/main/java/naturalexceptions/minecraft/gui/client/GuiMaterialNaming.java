package naturalexceptions.minecraft.gui.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.knowledge.KnowledgeSubjectRef;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.minecraft.gui.ContainerMaterialNaming;
import naturalexceptions.minecraft.item.MaterialNamingTarget;
import naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties;
import naturalexceptions.minecraft.knowledge.client.InventoryBrowserOverlay;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketApplyAlloyName;

@SideOnly(Side.CLIENT)
public class GuiMaterialNaming extends GuiContainer {

    private static final ResourceLocation DEMO_BG = new ResourceLocation(
        "naturalexceptions",
        "textures/gui/material_naming.png");

    private final ContainerMaterialNaming container;
    private GuiTextField nameField;
    private MaterialRef materialRef;
    private String resolvedName = "Unidentified material";
    private String existingAlias;
    private final EntityPlayer player;

    public GuiMaterialNaming(EntityPlayer player, ItemStack sampleStack) {
        super(new ContainerMaterialNaming(player, sampleStack));
        this.player = player;
        this.container = (ContainerMaterialNaming) this.inventorySlots;
        this.xSize = 248;
        this.ySize = 136;

        if (sampleStack != null) {
            this.materialRef = MaterialNamingTarget.getMaterialRef(sampleStack);
            if (materialRef != null) {
                naturalexceptions.core.knowledge.KnowledgeSubjectRef subject = KnowledgeSubjectRef
                    .material(materialRef);
                existingAlias = PlayerKnowledgeProperties.get(player)
                    .knowledge()
                    .aliasFor(subject);
                resolvedName = naturalexceptions.minecraft.knowledge.KnowledgePresentation
                    .materialName(player, materialRef, MaterialNamingTarget.getStateKey(sampleStack));
                if (existingAlias != null) resolvedName = existingAlias;
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        this.buttonList.clear();
        this.buttonList
            .add(new GuiButton(0, this.guiLeft + (this.xSize - 100) / 2, this.guiTop + 96, 100, 20, "Apply Name"));
        ((GuiButton) this.buttonList.get(0)).enabled = MaterialNamingTarget.canOpenNaming(container.getSampleStack());

        this.nameField = new GuiTextField(this.fontRendererObj, this.guiLeft + 24, this.guiTop + 70, 200, 16);
        this.nameField.setMaxStringLength(32);
        this.nameField.setFocused(true);
        if (existingAlias != null) this.nameField.setText(existingAlias);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    public boolean isNameFieldFocused() {
        return nameField != null && nameField.getVisible() && nameField.isFocused();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (InventoryBrowserOverlay.isSearchFocused()
            && InventoryBrowserOverlay.handleContainerKeyTyped(this, typedChar, keyCode)) return;
        if (this.nameField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (keyCode == 28 || keyCode == 156) { // Enter
            applyName();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.nameField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            applyName();
        }
    }

    private void applyName() {
        String entered = this.nameField.getText()
            .trim();
        if (!entered.isEmpty() && materialRef != null
            && MaterialNamingTarget.canOpenNaming(container.getSampleStack())) {
            NENetwork.CHANNEL.sendToServer(new PacketApplyAlloyName(materialRef, entered));
            // The server closes the dialog after applying the queued rename.
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Title
        this.fontRendererObj.drawString("Material Naming & Inspection", 24, 12, 0x404040);

        // Current Material Information
        this.fontRendererObj.drawString("\u00a77Current Name: \u00a70" + resolvedName, 24, 28, 0x333333);
        this.fontRendererObj.drawString("\u00a77Personal alias:", 24, 42, 0x333333);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(DEMO_BG);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        this.nameField.drawTextBox();
    }
}
