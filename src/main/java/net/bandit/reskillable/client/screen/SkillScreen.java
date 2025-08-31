package net.bandit.reskillable.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.buttons.SkillButton;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillScreen extends Screen {
    public static final ResourceLocation RESOURCES = ResourceLocation.fromNamespaceAndPath("reskillable", "textures/gui/skills.png");
    private final Map<Skill, String> xpCostDisplay = new HashMap<>();
    private final Map<Skill, Integer> xpCostColor = new HashMap<>();


    public SkillScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        int left = (width - 162) / 2;
        int top = (height - 128) / 2;

        for (int i = 0; i < 8; i++) {
            int x = left + i % 2 * 83;
            int y = top + i / 2 * 36;
            Skill skill = Skill.values()[i];

            addRenderableWidget(new SkillButton(x, y, skill));
        }
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int panelWidth = 176;
        int panelHeight = 166;

        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        guiGraphics.fill(left, top, left + panelWidth, top + panelHeight, 0x99000000);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderTransparentBackground(guiGraphics);
        RenderSystem.setShaderTexture(0, RESOURCES);

        int left = (width - 176) / 2;
        int top = (height - 166) / 2;

        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.blit(RESOURCES, left, top, 0, 0, 176, 166);

        int i = 0;
        for (Skill skill : Skill.values()) {
            int x = left + (i % 2) * 83 + 10;
            int y = top + (i / 2) * 36 + 20;

            String xpCost = xpCostDisplay.getOrDefault(skill, "N/A");
            int color = xpCostColor.getOrDefault(skill, 0xFFFFFF);

            guiGraphics.drawString(font, "XP: " + xpCost, x, y, color, false);
            i++;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        int labelX = width / 2 - font.width("Skills") / 2;
        int labelY = top + 6;
        if (mouseX > labelX && mouseX < labelX + font.width("Skills") && mouseY > labelY && mouseY < labelY + font.lineHeight) {
            renderTotalXPTooltip(guiGraphics, mouseX, mouseY);
        }

        for (var widget : this.renderables) {
            if (widget instanceof SkillButton button && button.isMouseOver(mouseX, mouseY)) {
                var tooltipLines = button.getTooltipLines(Minecraft.getInstance().player);
                guiGraphics.renderTooltip(
                        font,
                        tooltipLines.stream().map(Component::getVisualOrderText).toList(),
                        mouseX,
                        mouseY
                );
            }
        }

    }


    private void renderTotalXPTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    Player player = Minecraft.getInstance().player;
    if (player != null) {
        int totalXP = calculateTotalXP(player);
        int level = getLevelForTotalXP(totalXP);

        Component totalXPComponent = Component.literal(String.valueOf(totalXP)).withStyle(ChatFormatting.GREEN);
        Component levelComponent = Component.literal(String.valueOf(level));
        Component tooltip = Component.translatable("tooltip.rereskillable.total_xp", totalXPComponent, levelComponent);

        List<Component> tooltipLines = new ArrayList<>();
        tooltipLines.add(tooltip);

        guiGraphics.renderTooltip(
                Minecraft.getInstance().font,
                tooltipLines.stream().map(Component::getVisualOrderText).toList(),
                mouseX,
                mouseY
        );
    }
}
    private int calculateTotalXP(Player player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;

        if (level <= 16) {
            return (level * level) + (6 * level) + Math.round(progress * (2 * level + 7));
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360) + Math.round(progress * (5 * level - 38));
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220) + Math.round(progress * (9 * level - 158));
        }
    }

    private int getLevelForTotalXP(int totalXP) {
        int level = 0;
        while (getCumulativeXPForLevel(level + 1) <= totalXP) {
            level++;
        }
        return level;
    }

    private int getCumulativeXPForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) return (level * (level + 1)) + (level * 6);
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        SkillModel skillModel = SkillModel.get(player);
        if (skillModel == null) return; // Avoid null crash

        for (Skill skill : Skill.values()) {
            int skillLevel = skillModel.getSkillLevel(skill);
            int xpCost = Configuration.calculateCostForLevel(skillLevel + 1);
            boolean hasXP = skillModel.hasSufficientXP(player, skill);

            xpCostDisplay.put(skill, String.valueOf(xpCost));
            xpCostColor.put(skill, hasXP ? 0x00FF00 : 0xFF0000);
        }
    }

}