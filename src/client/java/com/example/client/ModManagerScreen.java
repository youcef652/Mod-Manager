package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModManagerScreen extends Screen {
	private static final int MARGIN = 18;
	private static final int HEADER_HEIGHT = 62;
	private static final int SIDEBAR_WIDTH = 212;
	private static final int CONTENT_LEFT = 244;
	private static final int TOP_BUTTON_HEIGHT = 34;
	private static final int SIDE_BUTTON_HEIGHT = 36;
	private String status = "";

	public ModManagerScreen() {
		super(Component.literal("Mod Manager"));
	}

	@Override
	protected void init() {
		int topLeft = MARGIN;
		int topGap = 10;
		int topButtonWidth = 140;
		int headerY = 12;

		addRenderableWidget(Button.builder(Component.literal("Modrinth"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(topLeft, headerY, topButtonWidth, TOP_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Updates"), button ->
				this.minecraft.setScreen(new UpdatesScreen(this)))
				.bounds(topLeft + topButtonWidth + topGap, headerY, topButtonWidth, TOP_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Reload"), button -> reloadResources())
				.bounds(topLeft + (topButtonWidth + topGap) * 2, headerY, 120, TOP_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Settings"), button ->
				this.minecraft.setScreen(new SettingsScreen(this)))
				.bounds(topLeft + (topButtonWidth + topGap) * 2 + 130, headerY, 120, TOP_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("File Browser"), button ->
				this.minecraft.setScreen(new ContentManagementScreen(this)))
				.bounds(this.width - 186, headerY, 168, TOP_BUTTON_HEIGHT)
				.build());

		int sidebarY = 84;
		addRenderableWidget(Button.builder(Component.literal("Mods"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(MARGIN, sidebarY, SIDEBAR_WIDTH, SIDE_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Resource Packs"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(MARGIN, sidebarY + 46, SIDEBAR_WIDTH, SIDE_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Shader Packs"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(MARGIN, sidebarY + 92, SIDEBAR_WIDTH, SIDE_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Data Packs"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(MARGIN, sidebarY + 138, SIDEBAR_WIDTH, SIDE_BUTTON_HEIGHT)
				.build());

		addRenderableWidget(Button.builder(Component.literal("Import File"), button -> {})
				.bounds(this.width - 202, sidebarY, 170, 32)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Export Files"), button -> {})
				.bounds(this.width - 202, sidebarY + 42, 170, 32)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Open Game Dir"), button -> {})
				.bounds(this.width - 202, sidebarY + 84, 170, 32)
				.build());

		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(this.width - 202, this.height - 52, 170, 32)
				.build());
	}

	private void reloadResources() {
		status = "Reloading resources...";
		this.minecraft.reloadResourcePacks().thenRun(() -> this.minecraft.execute(() -> status = "Resources reloaded"));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, this.width, this.height, 0xFF07161A);
		graphics.fill(0, 0, this.width, HEADER_HEIGHT + 8, 0xFF0B1D25);
		graphics.fill(0, HEADER_HEIGHT + 8, this.width, HEADER_HEIGHT + 10, 0xFF2BDE9A);

		graphics.fill(MARGIN, 68, this.width - MARGIN, this.height - 24, 0x0F1F2D34);
		graphics.fill(MARGIN, 68, MARGIN + SIDEBAR_WIDTH, this.height - 24, 0x0D1D2B30);
		graphics.fill(CONTENT_LEFT, 68, this.width - 202 - MARGIN, this.height - 24, 0x121E2B31);
		graphics.fill(this.width - 202 - MARGIN, 68, this.width - MARGIN, this.height - 24, 0x101C2B30);

		graphics.drawString(this.font, Component.literal("Mod Manager"), MARGIN + 8, 20, 0xFFEAF7F4, false);
		graphics.drawString(this.font, Component.literal("v1.0"), this.width - 84, 20, 0xFF89A5A7, false);
		graphics.drawString(this.font, Component.literal("•"), this.width - 34, 18, 0xFF9BC7D0, false);
		graphics.drawString(this.font, Component.literal("×"), this.width - 20, 16, 0xFF9BC7D0, false);

		graphics.fill(MARGIN + 6, 88, MARGIN + SIDEBAR_WIDTH - 6, 120, 0xFF0C2D39);
		graphics.fill(MARGIN + 12, 94, MARGIN + 22, 114, 0xFF46E2A9);
		graphics.drawString(this.font, Component.literal("Installed"), MARGIN + 30, 97, 0xFFEAF7F5, false);
		graphics.drawString(this.font, Component.literal("8 mods"), MARGIN + 30, 108, 0xFF7EA0AB, false);

		drawCategoryLabel(graphics, "Mods", MARGIN + 24, 146, true);
		drawCategoryLabel(graphics, "Resource Packs", MARGIN + 24, 192, false);
		drawCategoryLabel(graphics, "Shader Packs", MARGIN + 24, 238, false);
		drawCategoryLabel(graphics, "Data Packs", MARGIN + 24, 284, false);

		graphics.fill(CONTENT_LEFT + 18, 88, this.width - 230, 128, 0xFF0F1F2C);
		graphics.fill(CONTENT_LEFT + 28, 98, CONTENT_LEFT + 42, 118, 0xFF20475B);
		graphics.drawString(this.font, Component.literal("Search mods..."), CONTENT_LEFT + 52, 100, 0xFF7F98A5, false);
		graphics.drawString(this.font, Component.literal("Sort: Name"), this.width - 290, 100, 0xFF8CA4AF, false);
		graphics.fill(this.width - 240, 98, this.width - 224, 118, 0xFF1A2D3B);
		graphics.drawString(this.font, Component.literal("▾"), this.width - 234, 96, 0xFFE9F1F4, false);

		int listY = 154;
		String[] names = {"Fabric API", "Sodium", "Lithium", "Indium", "Mod Menu", "JourneyMap"};
		String[] descs = {"Core hooks and compatibility", "Fast rendering", "Better gameplay performance", "Rendering support", "In-game config", "Realtime map"};
		String[] tags = {"Core", "Optimized", "Boost", "Compat", "UI", "Map"};
		for (int i = 0; i < names.length; i++) {
			int y = listY + i * 58;
			graphics.fill(CONTENT_LEFT + 18, y, this.width - 230, y + 48, i % 2 == 0 ? 0xFF142A38 : 0xFF102230);
			graphics.fill(CONTENT_LEFT + 28, y + 10, CONTENT_LEFT + 54, y + 36, 0xFF2B455A);
			graphics.drawString(this.font, Component.literal(names[i]), CONTENT_LEFT + 68, y + 10, 0xFFEAF5F7, false);
			graphics.drawString(this.font, Component.literal(descs[i]), CONTENT_LEFT + 68, y + 25, 0xFF8198A7, false);
			graphics.fill(this.width - 220, y + 13, this.width - 192, y + 35, 0xFF2EE2A3);
			graphics.drawString(this.font, Component.literal(tags[i]), this.width - 214, y + 17, 0xFF0A1A18, false);
		}

		graphics.fill(this.width - 202 - MARGIN, 88, this.width - MARGIN, 260, 0x0D1E2D34);
		graphics.drawString(this.font, Component.literal("Quick Actions"), this.width - 172, 96, 0xFFE9F7F6, false);
		graphics.fill(this.width - 190, 118, this.width - 28, 150, 0xFF172C38);
		graphics.drawString(this.font, Component.literal("Import File"), this.width - 166, 130, 0xFFEAF5F7, false);
		graphics.fill(this.width - 190, 164, this.width - 28, 196, 0xFF172C38);
		graphics.drawString(this.font, Component.literal("Export Files"), this.width - 172, 176, 0xFFEAF5F7, false);
		graphics.fill(this.width - 190, 210, this.width - 28, 242, 0xFF172C38);
		graphics.drawString(this.font, Component.literal("Game Dir"), this.width - 146, 222, 0xFFEAF5F7, false);

		graphics.drawString(this.font, Component.literal("Installed: 8"), MARGIN + 10, this.height - 30, 0xFF9AB2B9, false);
		graphics.drawString(this.font, Component.literal("Fabric 0.16.14"), this.width - 160, this.height - 30, 0xFF9AB2B9, false);
		graphics.drawString(this.font, Component.literal("Minecraft 1.21.1"), this.width - 290, this.height - 30, 0xFF9AB2B9, false);

		super.render(graphics, mouseX, mouseY, delta);
		if (!status.isEmpty()) {
			graphics.drawCenteredString(this.font, Component.literal(status), this.width / 2, this.height - 18, 0xFF8AF0C5);
		}
	}

	private void drawCategoryLabel(GuiGraphics graphics, String text, int x, int y, boolean active) {
		int bg = active ? 0xFF16384B : 0x1A24313F;
		graphics.fill(x, y, x + SIDEBAR_WIDTH - 10, y + 30, bg);
		graphics.drawString(this.font, Component.literal(text), x + 10, y + 9, active ? 0xFFEAF7F5 : 0xFFB8C9CF, false);
	}
}