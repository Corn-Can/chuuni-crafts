package com.chuuni.client;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import com.chuuni.FxGroup;
import com.chuuni.client.fx.Settings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;

/** 遊戲內設定畫面（按 K 或 /chuuni）：改了立刻生效並存檔 */
public class SettingsScreen extends Screen {
	private final Screen parent;
	private int groupsY;

	public SettingsScreen(Screen parent) {
		super(Component.translatable("chuuni.settings.title"));
		this.parent = parent;
	}

	private CycleButton<Boolean> toggle(int x, int y, int w, String key, boolean value, Consumer<Boolean> set) {
		CycleButton<Boolean> b = CycleButton.onOffBuilder(value).create(x, y, w, 20, Component.translatable("chuuni.settings." + key), (btn, v) -> {
			set.accept(v);
			Settings.save();
		});
		b.setTooltip(Tooltip.create(Component.translatable("chuuni.settings." + key + ".tip")));
		lock(b, key);
		return b;
	}

	/** 0~max 的滑桿，顯示百分比 */
	private AbstractSliderButton slider(int x, int y, int w, String key, float value, float max, DoubleConsumer set) {
		AbstractSliderButton s = new AbstractSliderButton(x, y, w, 20, Component.empty(), value / max) {
			{
				updateMessage();
			}

			@Override
			protected void updateMessage() {
				setMessage(Component.translatable("chuuni.settings." + key, Math.round(value * max * 100) + "%"));
			}

			@Override
			protected void applyValue() {
				set.accept(Mth.clamp(value * max, 0, max));
				Settings.save();
			}
		};
		s.setTooltip(Tooltip.create(Component.translatable("chuuni.settings." + key + ".tip")));
		lock(s, key);
		return s;
	}

	/** 被伺服器強制的項目：變灰、不能按，提示說明原因 */
	static void lock(AbstractWidget w, String key) {
		if (Settings.forced(key)) {
			w.active = false;
			w.setTooltip(Tooltip.create(Component.translatable("chuuni.settings.locked.tip")));
		}
	}

	@Override
	protected void init() {
		Settings.onServerChange = () -> {
			if (minecraft != null && minecraft.screen == this) {
				rebuildWidgets();
			}
		};

		// 三欄
		int w = Math.min(150, (width - 24) / 3 - 4);
		int[] x = {width / 2 - w * 3 / 2 - 6, width / 2 - w / 2, width / 2 + w / 2 + 6};
		int y = 34;

		addRenderableWidget(toggle(x[0], y, w, "enabled", Settings.enabled, v -> Settings.enabled = v));
		addRenderableWidget(toggle(x[1], y, w, "cinematic", Settings.cinematic, v -> Settings.cinematic = v));
		CycleButton<Integer> intensity = CycleButton.<Integer>builder(v -> Component.translatable("chuuni.settings.intensity." + v), () -> Settings.intensity)
			.withValues(0, 1, 2)
			.create(x[2], y, w, 20, Component.translatable("chuuni.settings.intensity"), (btn, v) -> {
				Settings.intensity = v;
				Settings.save();
			});
		intensity.setTooltip(Tooltip.create(Component.translatable("chuuni.settings.intensity.tip")));
		lock(intensity, "intensity");
		addRenderableWidget(intensity);
		y += 24;

		addRenderableWidget(toggle(x[0], y, w, "titles", Settings.titles, v -> Settings.titles = v));
		addRenderableWidget(toggle(x[1], y, w, "post", Settings.post, v -> Settings.post = v));
		addRenderableWidget(slider(x[2], y, w, "shake", Settings.shake, 2, v -> Settings.shake = (float)v));
		y += 24;

		addRenderableWidget(toggle(x[0], y, w, "sound", Settings.sound, v -> Settings.sound = v));
		addRenderableWidget(slider(x[1], y, w, "volAnime", Settings.volAnime, 1, v -> Settings.volAnime = (float)v));
		addRenderableWidget(slider(x[2], y, w, "volSynth", Settings.volSynth, 1, v -> Settings.volSynth = (float)v));
		y += 24;
		addRenderableWidget(slider(x[1], y, w, "volTitle", Settings.volTitle, 1, v -> Settings.volTitle = (float)v));
		y += 30;

		// 整組開關
		groupsY = y - 11;
		FxGroup[] groups = FxGroup.values();

		for (int i = 0; i < groups.length; i++) {
			FxGroup g = groups[i];
			addRenderableWidget(toggle(x[i % 3], y + i / 3 * 24, w, g.key, Settings.group(g), v -> Settings.setGroup(g, v)));
		}

		// 第三排剩下的兩格：個別演出 / 伺服器設定
		int by = y + 2 * 24;
		Button each = Button.builder(Component.translatable("chuuni.settings.each"), b -> minecraft.setScreen(new EffectsScreen(this, false)))
			.bounds(x[1], by, w, 20).build();
		each.setTooltip(Tooltip.create(Component.translatable("chuuni.settings.each.tip")));
		addRenderableWidget(each);

		if (Settings.serverAdmin) {
			Button server = Button.builder(Component.translatable("chuuni.settings.server"), b -> minecraft.setScreen(new EffectsScreen(this, true)))
				.bounds(x[2], by, w, 20).build();
			server.setTooltip(Tooltip.create(Component.translatable("chuuni.settings.server.tip")));
			addRenderableWidget(server);
		}

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, height - 26, 200, 20).build());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		super.render(g, mouseX, mouseY, partial);
		g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
		g.drawCenteredString(font, Component.translatable("chuuni.settings.tiers"), width / 2, groupsY, 0xFFA0A0A0);

		if (Settings.locked()) {
			g.drawCenteredString(font, Component.translatable("chuuni.settings.locked"), width / 2, 20, 0xFFFFB040);
		}
	}

	@Override
	public void onClose() {
		Settings.save();
		minecraft.setScreen(parent);
	}
}
