package dev.cammiescorner.arcanuscontinuum.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.cammiescorner.arcanuscontinuum.Arcanus;
import dev.cammiescorner.arcanuscontinuum.api.spells.SpellComponent;
import dev.cammiescorner.arcanuscontinuum.api.spells.SpellGroup;
import dev.cammiescorner.arcanuscontinuum.api.spells.Weight;
import dev.cammiescorner.arcanuscontinuum.common.items.SpellBookItem;
import dev.cammiescorner.arcanuscontinuum.common.screens.SpellBookScreenHandler;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector2i;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class SpellBookScreen extends HandledScreen<SpellBookScreenHandler> {
	public static final Identifier BOOK_TEXTURE = Arcanus.id("textures/gui/spell_book.png");
	public static final Identifier PANEL_TEXTURE = Arcanus.id("textures/gui/spell_crafting.png");
	public static final LinkedList<SpellGroup> SPELL_GROUPS = new LinkedList<>();

	public SpellBookScreen(SpellBookScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
		super(screenHandler, playerInventory, text);
	}

	@Override
	protected void init() {
		super.init();
		x = (width - 256) / 2;
		y = (height - 180) / 2;
		playerInventoryTitleY = -10000;

		addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> closeScreen()).position(width / 2 - 49, y + 170).size(98, 20).build());
		SPELL_GROUPS.addAll(SpellBookItem.getSpell(getScreenHandler().getSpellBook()).getComponentGroups());
	}

	@Override
	protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		RenderSystem.setShaderTexture(0, BOOK_TEXTURE);
		DrawableHelper.drawTexture(matrices, x, y, 0, 0, 256, 180, 256, 256);
	}

	@Override
	protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
		MutableText title = this.title.copy().formatted(Formatting.BOLD, Formatting.UNDERLINE);
		textRenderer.draw(matrices, title, 128 - textRenderer.getWidth(title) * 0.5F, 11, 0x50505D);

		for(int i = 0; i < SPELL_GROUPS.size(); i++) {
			SpellGroup group = SPELL_GROUPS.get(i);
			List<Vector2i> positions = group.positions();
			RenderSystem.setShader(GameRenderer::getPositionShader);
			RenderSystem.setShaderColor(0.25F, 0.25F, 0.3F, 1F);
			BufferBuilder bufferBuilder = Tessellator.getInstance().getBufferBuilder();

			bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
			matrices.push();
			matrices.translate(12, 12, 0);
			Matrix4f matrix = matrices.peek().getModel();

			for(int j = 0; j < positions.size(); j++) {
				Vector2i pos = positions.get(j);
				Vector2i prevPos = positions.get(Math.max(0, j - 1));

				if(j == 0 && i > 0 && !SPELL_GROUPS.get(i - 1).isEmpty()) {
					List<Vector2i> prevPositions = SPELL_GROUPS.get(i - 1).positions();
					prevPos = prevPositions.get(prevPositions.size() - 1);
				}
				if(pos.equals(prevPos))
					continue;

				int x1 = prevPos.x();
				int y1 = prevPos.y();
				int x2 = pos.x();
				int y2 = pos.y();
				float angle = (float) (Math.atan2(y2 - y1, x2 - x1) - (Math.PI * 0.5));
				float dx = MathHelper.cos(angle);
				float dy = MathHelper.sin(angle);

				bufferBuilder.vertex(matrix, x2 - dx, y2 - dy, 0).color(0).next();
				bufferBuilder.vertex(matrix, x2 + dx, y2 + dy, 0).color(0).next();
				bufferBuilder.vertex(matrix, x1 + dx, y1 + dy, 0).color(0).next();
				bufferBuilder.vertex(matrix, x1 - dx, y1 - dy, 0).color(0).next();
			}

			BufferRenderer.drawWithShader(bufferBuilder.end());
			matrices.pop();
		}

		for(SpellGroup group : SPELL_GROUPS) {
			List<Vector2i> positions = group.positions();

			for(int j = 0; j < positions.size(); j++) {
				Vector2i pos = positions.get(j);
				RenderSystem.setShader(GameRenderer::getPositionTexShader);
				RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
				RenderSystem.setShaderTexture(0, PANEL_TEXTURE);
				drawTexture(matrices, pos.x - 3, pos.y - 3, 60, 208, 30, 30, 384, 256);

				RenderSystem.setShaderColor(0.25F, 0.25F, 0.3F, 1F);
				drawTexture(matrices, pos.x - 3, pos.y - 3, 30, 208, 30, 30, 384, 256);

				RenderSystem.setShaderTexture(0, group.getAllComponents().toList().get(j).getTexture());
				drawTexture(matrices, pos.x, pos.y, 0, 0, 24, 24, 24, 24);
			}
		}

		MutableText weight = Arcanus.translate("spell_book", "weight", getWeight().toString().toLowerCase(Locale.ROOT)).formatted(Formatting.DARK_GREEN);
		MutableText mana = Text.literal(String.format("%.2f", getManaCost())).formatted(Formatting.BLUE);
		MutableText coolDown = Text.literal(String.format("%.2f", getCoolDown() / 20D)).append(Arcanus.translate("spell_book", "seconds")).formatted(Formatting.RED);

		textRenderer.draw(matrices, weight, 240 - textRenderer.getWidth(weight), 7, 0xffffff);
		textRenderer.draw(matrices, mana, 240 - textRenderer.getWidth(mana), 17, 0xffffff);
		textRenderer.draw(matrices, coolDown, 240 - textRenderer.getWidth(coolDown), 27, 0xffffff);

		for(SpellGroup group : SPELL_GROUPS) {
			for(int i = 0; i < group.positions().size(); i++) {
				Vector2i position = group.positions().get(i);

				if(isPointWithinBounds(position.x() - 2, position.y() - 2, 28, 28, mouseX, mouseY)) {
					SpellComponent component = group.getAllComponents().toList().get(i);
					MutableText componentName = Text.translatable(component.getTranslationKey(client.player));
					MutableText componentWeight = Arcanus.translate("spell_book", "weight").append(": ").formatted(Formatting.GREEN).append(Arcanus.translate("spell_book", "weight", component.getWeight().toString().toLowerCase(Locale.ROOT)).formatted(Formatting.GRAY));
					MutableText componentMana = Arcanus.translate("spell_book", "mana_cost").append(": ").formatted(Formatting.BLUE).append(Text.literal(String.format("%.2f", component.getManaCost())).formatted(Formatting.GRAY));
					MutableText componentCoolDown = Arcanus.translate("spell_book", "cool_down").append(": ").formatted(Formatting.RED).append(Text.literal(String.format("%.2f", component.getCoolDown() / 20D)).append(Arcanus.translate("spell_book", "seconds")).formatted(Formatting.GRAY));
					List<Text> textList = List.of(componentName, componentWeight, componentMana, componentCoolDown);

					renderTooltip(matrices, textList, mouseX - x, mouseY - y);
				}
			}
		}
	}

	@Override
	protected void clearChildren() {
		super.clearChildren();
		SPELL_GROUPS.clear();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	protected boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY) {
		int i = this.x;
		int j = this.y;
		pointX -= i;
		pointY -= j;

		return pointX >= (double)(x - 1) && pointX < (double)(x + width + 1) && pointY >= (double)(y - 1) && pointY < (double)(y + height + 1);
	}

	public Weight getWeight() {
		int averageWeightIndex = 0;

		if(!SPELL_GROUPS.isEmpty()) {
			int i = 0;

			for(SpellGroup group : SPELL_GROUPS) {
				if(group.isEmpty())
					continue;

				averageWeightIndex += group.getAverageWeight().ordinal();
				i++;
			}

			averageWeightIndex = Math.round(averageWeightIndex / (float) i);
		}

		return Weight.values()[averageWeightIndex];
	}

	public double getManaCost() {
		double manaCost = 0;

		for(SpellGroup group : SPELL_GROUPS)
			manaCost += group.getManaCost();

		return manaCost;
	}

	public int getCoolDown() {
		int coolDown = 0;

		if(!SPELL_GROUPS.isEmpty())
			for(SpellGroup group : SPELL_GROUPS)
				coolDown += group.getCoolDown();

		return coolDown;
	}
}
