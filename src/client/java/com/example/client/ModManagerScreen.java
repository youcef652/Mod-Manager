package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModManagerScreen extends Screen {
	private static final int MARGIN = 12;
	private static final int TOP_HEADER_HEIGHT = 52;
	private static final int LEFT_MENU_WIDTH = 220;
	private static final int TOP_BUTTON_HEIGHT = 38;
	private static final int SIDE_BUTTON_HEIGHT = 34;
	private static final int CONTENT_X = 260;
	private String status = "";

	public ModManagerScreen() {
		super(Component.literal("Mod Manager"));
	}

	@Override
	protected void init() {
		int topButtonWidth = 176;
		int firstTopButtonX = MARGIN;
		int secondTopButtonX = firstTopButtonX + topButtonWidth + 12;
		int thirdTopButtonX = secondTopButtonX + topButtonWidth + 12;
		int fourthTopButtonX = thirdTopButtonX + topButtonWidth + 12;
		int rightButtonX = this.width - 220 - MARGIN;
		int topY = 16;

		addRenderableWidget(Button.builder(Component.literal("Modrinth"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(firstTopButtonX, topY, topButtonWidth, TOP_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Updates"), button ->
				this.minecraft.setScreen(new UpdatesScreen(this)))
				.bounds(secondTopButtonX, topY, topButtonWidth, TOP_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Reload"), button -> reloadResources())
				.bounds(thirdTopButtonX, topY, topButtonWidth, TOP_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Settings"), button ->
				this.minecraft.setScreen(new SettingsScreen(this)))
				.bounds(fourthTopButtonX, topY, topButtonWidth, TOP_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("File Browser"), button ->
				this.minecraft.setScreen(new ContentManagementScreen(this)))
				.bounds(rightButtonX, topY, 220, TOP_BUTTON_HEIGHT)
				.build());

		int sideY = 82;
		addRenderableWidget(Button.builder(Component.literal("Mods"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(MARGIN, sideY, LEFT_MENU_WIDTH, SIDE_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Resource Packs"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(MARGIN, sideY + 44, LEFT_MENU_WIDTH, SIDE_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Shader Packs"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(MARGIN, sideY + 88, LEFT_MENU_WIDTH, SIDE_BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Data Packs"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(MARGIN, sideY + 132, LEFT_MENU_WIDTH, SIDE_BUTTON_HEIGHT)
				.build());

		addRenderableWidget(Button.builder(Component.literal("Import File"), button -> {
			}).bounds(this.width - 210, sideY, 160, 32).build());
		addRenderableWidget(Button.builder(Component.literal("Export Files"), button -> {
			}).bounds(this.width - 210, sideY + 40, 160, 32).build());
		addRenderableWidget(Button.builder(Component.literal("Open Game Directory"), button -> {
			}).bounds(this.width - 210, sideY + 80, 160, 32).build());

		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(this.width - 210, this.height - 52, 160, 32)
				.build());
	}

	private void restartMinecraft() {
		ModManagerSettings.save();
		this.minecraft.stop();
	}

	private void reloadResources() {
		status = "Reloading resources...";
		this.minecraft.reloadResourcePacks().thenRun(() -> this.minecraft.execute(() ->
				status = "Resources reloaded"));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, this.width, this.height, 0xFF071A22);
		graphics.fill(0, 0, this.width, TOP_HEADER_HEIGHT, 0xFF0C1F2F);
		graphics.fill(0, TOP_HEADER_HEIGHT, this.width, TOP_HEADER_HEIGHT + 2, 0xFF1E3B4C);
		graphics.fill(MARGIN, 64, this.width - MARGIN, this.height - 52, 0x0D1A2B33);
		graphics.fill(MARGIN, 64, MARGIN + LEFT_MENU_WIDTH, this.height - 52, 0x0F1D2A39);
		graphics.fill(MARGIN + LEFT_MENU_WIDTH + 8, 64, this.width - MARGIN - 220, this.height - 52, 0x0A182B38);
		graphics.fill(this.width - 220 - MARGIN, 64, this.width - MARGIN, this.height - 52, 0x0A1D2B36);
		graphics.fill(MARGIN, 64, this.width - MARGIN, 66, 0xFF2D4052);

		graphics.drawString(this.font, Component.literal("Mod Manager"), MARGIN + 8, 16, 0xFFF3F8FF, false);
		graphics.drawString(this.font, Component.literal("-"), this.width - 60, 12, 0xFFAFBED0, false);
		graphics.drawString(this.font, Component.literal("X"), this.width - 30, 10, 0xFFAFBED0, false);
		graphics.drawString(this.font, Component.literal("Mods"), MARGIN + 14, 90, 0xFFF2F7FF, false);
		graphics.drawString(this.font, Component.literal("Resource Packs"), MARGIN + 14, 134, 0xFFF2F7FF, false);
		graphics.drawString(this.font, Component.literal("Shader Packs"), MARGIN + 14, 178, 0xFFF2F7FF, false);
		graphics.drawString(this.font, Component.literal("Data Packs"), MARGIN + 14, 222, 0xFFF2F7FF, false);

		graphics.drawString(this.font, Component.literal("Search mods..."), CONTENT_X + 62, 96, 0xFF7D8E9D, false);
		graphics.drawString(this.font, Component.literal("Sort by:"), CONTENT_X + 530, 96, 0xFF7D8E9D, false);
		graphics.drawString(this.font, Component.literal("Name"), CONTENT_X + 620, 96, 0xFFECF5FF, false);
		graphics.drawString(this.font, Component.literal("8 mods installed"), MARGIN + 10, this.height - 28, 0xFF9DB2C2, false);
		graphics.drawString(this.font, Component.literal("Minecraft 1.21.1"), this.width - 200, this.height - 28, 0xFF9DB2C2, false);
		graphics.drawString(this.font, Component.literal("Fabric 0.16.14"), this.width - 90, this.height - 28, 0xFF9DB2C2, false);

		super.render(graphics, mouseX, mouseY, delta);
		if (!status.isEmpty()) {
			graphics.drawCenteredString(this.font, Component.literal(status), this.width / 2, this.height - 22, 0xFF6FE6C6);
		}
	}
}