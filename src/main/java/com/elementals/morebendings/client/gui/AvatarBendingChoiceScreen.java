package com.elementals.morebendings.client.gui;

import com.elementals.morebendings.bending.avatarstate.AvatarBendingOptions;
import com.elementals.morebendings.network.packets.CancelAvatarBendingChoicePacket;
import com.elementals.morebendings.network.packets.ChooseAvatarBendingPacket;
import com.elementals.morebendings.network.packets.OpenAvatarBendingChoicePacket;
import commonnetwork.api.Dispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * GUI cliente-only aberta por {@link OpenAvatarBendingChoicePacket}: lista
 * as opções já prontas em {@code optionsBlob} (ver {@link
 * AvatarBendingOptions#parseOptionsBlob}) e manda a escolha de volta pro
 * servidor em {@link ChooseAvatarBendingPacket}. Se o jogador fechar sem
 * escolher (ESC, tecla de inventário, etc.), {@link #onClose} avisa o
 * servidor via {@link CancelAvatarBendingChoicePacket} pra liberar o alvo
 * na hora, em vez de deixá-lo congelado até o timeout.
 */
public class AvatarBendingChoiceScreen extends Screen {

    private static final int LIST_WIDTH = 260;
    private static final int ROW_HEIGHT = 22;
    private static final int BUTTON_HEIGHT = 32;

    private final boolean grantMode;
    private final String targetName;
    private final List<AvatarBendingOptions.Option> options;
    private boolean resolved = false;

    private AvatarBendingChoiceScreen(boolean grantMode, String targetName, List<AvatarBendingOptions.Option> options) {
        super(Component.literal((grantMode ? "Grant bending -- " : "Remove bending -- ") + targetName));
        this.grantMode = grantMode;
        this.targetName = targetName;
        this.options = options;
    }

    /** Chamado por {@link OpenAvatarBendingChoicePacket#handle}. */
    public static void open(OpenAvatarBendingChoicePacket message) {
        List<AvatarBendingOptions.Option> options = AvatarBendingOptions.parseOptionsBlob(message.optionsBlob());
        Minecraft.getInstance().setScreen(
                new AvatarBendingChoiceScreen(message.grantMode(), message.targetName(), options));
    }

    @Override
    protected void init() {
        List<AvatarBendingOptions.Option> eligible = options.stream()
                .filter(option -> grantMode != option.alreadyHas())
                .toList();

        OptionList list = new OptionList(this.minecraft, LIST_WIDTH, this.height,
                40, this.height - 40, ROW_HEIGHT, eligible);
        list.setX((this.width - LIST_WIDTH) / 2);
        this.addRenderableWidget(list);

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
                .bounds((this.width - 100) / 2, this.height - 28, 100, 20)
                .build());
    }

    private void choose(AvatarBendingOptions.Option option) {
        resolved = true;
        Dispatcher.sendToServer(new ChooseAvatarBendingPacket(grantMode, option.id()));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void onClose() {
        if (!resolved) {
            resolved = true;
            Dispatcher.sendToServer(new CancelAvatarBendingChoicePacket());
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        if (this.options.stream().noneMatch(option -> grantMode != option.alreadyHas())) {
            graphics.drawCenteredString(this.font,
                    Component.literal(grantMode ? "No bending left to grant." : "No bending to remove."),
                    this.width / 2, this.height / 2, 0xAAAAAA);
        }
    }

    private class OptionList extends ObjectSelectionList<OptionList.Entry> {

        OptionList(Minecraft minecraft, int width, int screenHeight, int top, int bottom, int itemHeight,
                   List<AvatarBendingOptions.Option> eligible) {
            super(minecraft, width, screenHeight, top, itemHeight);
            this.setY(top);
            for (AvatarBendingOptions.Option option : eligible) {
                this.addEntry(new Entry(option));
            }
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final AvatarBendingOptions.Option option;

            Entry(AvatarBendingOptions.Option option) {
                this.option = option;
            }

            @Override
            public Component getNarration() {
                return Component.literal(option.displayName());
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                boolean isHovering = hovering && mouseY >= top && mouseY < top + height;
                int color = isHovering ? 0xFFFFFF : 0xCCCCCC;
                graphics.drawCenteredString(AvatarBendingChoiceScreen.this.font,
                        Component.literal(option.displayName()), left + width / 2, top + (height - 8) / 2, color);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                AvatarBendingChoiceScreen.this.choose(option);
                return true;
            }
        }
    }
}