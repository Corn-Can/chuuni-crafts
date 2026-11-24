package com.chuuni.client;

import java.util.ArrayList;
import java.util.List;

import com.chuuni.FxGroup;
import com.chuuni.ServerConfig;
import com.chuuni.client.fx.Settings;
import com.chuuni.net.FxPayload.Kind;
import com.chuuni.net.ServerEditPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * 個別演出設定（分頁 = 分組）。
 * <ul>
 *   <li>玩家模式：每一種演出各自開 / 關（存在 config/chuuni.properties）</li>
 *   <li>伺服器模式（OP / 單人世界主人）：改 config/chuuni-server.properties ——
 *       一般設定、每一種演出「預設（玩家決定）/ 強制開 / 強制關」，按下去立刻套用到所有在線玩家</li>
 * </ul>
 */
public class EffectsScreen extends Screen {
	private static final int TOP = 56;

	private final Screen parent;
	private final boolean server;
	/** 伺服器模式多一個「一般」分頁（tab = -1） */
	private int tab;
	private int page;
	private int pages = 1;

	public EffectsScreen(Screen parent, boolean server) {
		super(Component.translatable(server ? "chuuni.settings.server" : "chuuni.settings.each"));
		this.parent = parent;
		this.server = server;
		this.tab = server ? -1 : 0;
	}

	@Override
	protected void init() {
		Settings.onServerChange = () -> {
			if (minecraft != null && minecraft.screen == this) {
				// 管理員權限被拿掉時退回上一頁
				if (server && !Settings.serverAdmin) {
					onClose();
					return;
				}

				rebuildWidgets();
			}
		};

		tabs();

		List<AbstractWidget> items = tab < 0 ? generalItems() : groupItems(FxGroup.values()[tab]);
		int cols = width >= 480 ? 3 : 2;
		int w = Math.min(150, (width - 24) / cols - 4);
		int rows = Math.max(1, (height - TOP - 80) / 22);
		int per = cols * rows;
		pages = Math.max(1, (items.size() + per - 1) / per);
		page = Math.min(page, pages - 1);
		int x0 = width / 2 - (cols * (w + 4) - 4) / 2;

		for (int i = page * per; i < Math.min(items.size(), (page + 1) * per); i++) {
			int k = i - page * per;
			AbstractWidget wd = items.get(i);
			wd.setX(x0 + k % cols * (w + 4));
			wd.setY(TOP + k / cols * 22);
			wd.setWidth(w);
			addRenderableWidget(wd);
		}

		int by = height - 50;

		if (pages > 1) {
			addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
				page = (page + pages - 1) % pages;
				rebuildWidgets();
			}).bounds(width / 2 - 45, by - 24, 20, 20).build());
			addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
				page = (page + 1) % pages;
				rebuildWidgets();
			}).bounds(width / 2 + 25, by - 24, 20, 20).build());
		}

		bulk(by);
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, height - 26, 200, 20).build());
	}

	/** 上方的分頁按鈕 */
	private void tabs() {
		FxGroup[] groups = FxGroup.values();
		int n = groups.length + (server ? 1 : 0);
		int w = Math.min(64, (width - 16) / n - 2);
		int x = width / 2 - (n * (w + 2) - 2) / 2;

		for (int i = server ? -1 : 0; i < groups.length; i++) {
			int t = i;
			Component label = Component.translatable(i < 0 ? "chuuni.group.general" : "chuuni.group." + groups[i].key);
			Button b = Button.builder(label, btn -> {
				tab = t;
				page = 0;
				rebuildWidgets();
			}).bounds(x, 30, w, 20).build();
			b.active = t != tab;
			addRenderableWidget(b);
			x += w + 2;
		}
	}

	/** 下方的整批操作 */
	private void bulk(int y) {
		if (tab < 0) {
			addRenderableWidget(Button.builder(Component.translatable("chuuni.server.reload"), b -> edit("", ""))
				.bounds(width / 2 - 75, y, 150, 20).build());
			return;
		}

		FxGroup g = FxGroup.values()[tab];

		if (server) {
			addRenderableWidget(Button.builder(Component.translatable("chuuni.server.all_default"), b -> {
				for (Kind k : g.kinds()) {
					edit(FxGroup.key(k), "");
				}
			}).bounds(width / 2 - 128, y, 126, 20).build());
			addRenderableWidget(Button.builder(Component.translatable("chuuni.server.all_off"), b -> {
				for (Kind k : g.kinds()) {
					edit(FxGroup.key(k), "false");
				}
			}).bounds(width / 2 + 2, y, 126, 20).build());
			return;
		}

		// 玩家：整組開關 + 全開 / 全關
		CycleButton<Boolean> group = CycleButton.onOffBuilder(Settings.group(g)).create(width / 2 - 130, y, 100, 20,
			Component.translatable("chuuni.settings.group"), (b, v) -> {
				Settings.setGroup(g, v);
				Settings.save();
			});
		group.setTooltip(Tooltip.create(Component.translatable("chuuni.settings." + g.key + ".tip")));
		SettingsScreen.lock(group, g.key);
		addRenderableWidget(group);
		addRenderableWidget(Button.builder(Component.translatable("chuuni.settings.all_on"), b -> all(g, true)).bounds(width / 2 - 26, y, 76, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("chuuni.settings.all_off"), b -> all(g, false)).bounds(width / 2 + 54, y, 76, 20).build());
	}

	private void all(FxGroup g, boolean v) {
		for (Kind k : g.kinds()) {
			if (!Settings.forced(FxGroup.key(k))) {
				Settings.set(k, v);
			}
		}

		Settings.save();
		rebuildWidgets();
	}

	// ------------------------------------------------------------------ 項目

	private List<AbstractWidget> groupItems(FxGroup g) {
		List<AbstractWidget> out = new ArrayList<>();

		for (Kind k : g.kinds()) {
			String key = FxGroup.key(k);
			Component name = Component.translatable(Settings.nameKey(k));

			if (server) {
				out.add(tri(key, name, List.of("", "true", "false")));
				continue;
			}

			CycleButton<Boolean> b = CycleButton.onOffBuilder(Settings.on(k)).create(0, 0, 150, 20, name, (btn, v) -> {
				Settings.set(k, v);
				Settings.save();
			});
			SettingsScreen.lock(b, key);
			out.add(b);
		}

		return out;
	}

	/** 伺服器「一般」分頁：forceClient + 每一項一般設定 */
	private List<AbstractWidget> generalItems() {
		List<AbstractWidget> out = new ArrayList<>();
		String force = Settings.serverRaw.getProperty("forceClient", "false").trim();
		CycleButton<Boolean> f = CycleButton.onOffBuilder(Boolean.parseBoolean(force)).create(0, 0, 150, 20, Component.translatable("chuuni.server.force"),
			(b, v) -> edit("forceClient", String.valueOf(v)));
		f.setTooltip(Tooltip.create(Component.translatable("chuuni.server.force.tip")));
		out.add(f);

		for (String key : ServerConfig.KEYS) {
			List<String> values = switch (key) {
				case "intensity" -> List.of("", "0", "1", "2");
				case "shake" -> List.of("", "0", "0.5", "1", "1.5", "2");
				case "volAnime", "volSynth", "volTitle" -> List.of("", "0", "0.25", "0.5", "0.75", "1");
				default -> List.of("", "true", "false");
			};
			String label = switch (key) {
				case "shake", "volAnime", "volSynth", "volTitle" -> "chuuni.settings." + key + ".name";
				default -> "chuuni.settings." + key;
			};
			CycleButton<String> b = tri(key, Component.translatable(label), values);

			// 沒開「強制套用一般設定」時，這一段不會生效 → 變灰
			if (!Boolean.parseBoolean(force)) {
				b.active = false;
				b.setTooltip(Tooltip.create(Component.translatable("chuuni.server.need_force")));
			}

			out.add(b);
		}

		return out;
	}

	/** 伺服器設定的一格：在 values 之間循環，按下去立刻送給伺服器 */
	private CycleButton<String> tri(String key, Component name, List<String> values) {
		String now = Settings.serverRaw.getProperty(key, "").trim();

		if (!values.contains(now)) {
			now = values.getFirst();
		}

		CycleButton<String> b = CycleButton.<String>builder(EffectsScreen::show, now).withValues(values).create(0, 0, 150, 20, name, (btn, v) -> edit(key, v));
		b.setTooltip(Tooltip.create(Component.translatable("chuuni.server.value.tip")));
		return b;
	}

	private static Component show(String v) {
		return switch (v) {
			case "" -> Component.translatable("chuuni.server.default");
			case "true" -> Component.translatable("chuuni.server.on");
			case "false" -> Component.translatable("chuuni.server.off");
			default -> Component.literal(v);
		};
	}

	/** 送給伺服器（key = "" → 重新讀取設定檔）；先在本地記下新值，免得回覆前重畫又變回舊值 */
	private static void edit(String key, String value) {
		if (!key.isEmpty()) {
			Settings.serverRaw.setProperty(key, value);
		}

		if (ClientPlayNetworking.canSend(ServerEditPayload.TYPE)) {
			ClientPlayNetworking.send(new ServerEditPayload(key, value));
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		super.render(g, mouseX, mouseY, partial);
		g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);

		if (pages > 1) {
			g.drawCenteredString(font, (page + 1) + " / " + pages, width / 2, height - 68, 0xFFA0A0A0);
		}

		if (server) {
			g.drawCenteredString(font, Component.translatable("chuuni.server.hint"), width / 2, 18, 0xFFFFB040);
		}
		else if (tab >= 0 && !Settings.group(FxGroup.values()[tab])) {
			g.drawCenteredString(font, Component.translatable("chuuni.settings.group_off"), width / 2, 18, 0xFFFF8060);
		}
	}

	@Override
	public void onClose() {
		Settings.save();
		minecraft.setScreen(parent);
	}
}
