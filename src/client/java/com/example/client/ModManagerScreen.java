package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModManagerScreen extends Screen {
	private static final int BUTTON_WIDTH = 140;
	private static final int BUTTON_HEIGHT = 20;
	private static final int COLUMN_GAP = 8;
	private static final int ROW_GAP = 24;
	private String status = "";

	public ModManagerScreen() {
		super(Component.literal("Mod Manager"));
	}

	@Override
	protected void init() {
		int leftX = this.width / 2 - BUTTON_WIDTH - COLUMN_GAP / 2;
		int rightX = this.width / 2 + COLUMN_GAP / 2;
		int firstButtonY = 72;

		addRenderableWidget(Button.builder(Component.literal("Download"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(leftX, firstButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Updates"), button ->
				this.minecraft.setScreen(new UpdatesScreen(this)))
				.bounds(rightX, firstButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Manage"), button ->
				this.minecraft.setScreen(new ContentManagementScreen(this)))
				.bounds(leftX, firstButtonY + ROW_GAP, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Reload"), button -> reloadResources())
				.bounds(rightX, firstButtonY + ROW_GAP, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Restart Minecraft"), button -> restartMinecraft())
				.bounds(leftX, firstButtonY + ROW_GAP * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Settings"), button ->
				this.minecraft.setScreen(new SettingsScreen(this)))
				.bounds(rightX, firstButtonY + ROW_GAP * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(this.width / 2 - BUTTON_WIDTH / 2, firstButtonY + ROW_GAP * 3, BUTTON_WIDTH, BUTTON_HEIGHT)
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

	private Button menuButton(String label, int x, int y) {
		return Button.builder(Component.literal(label), button -> {
		}).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		UiTheme.background(graphics, this);
		UiTheme.panel(graphics, this.width / 2 - 154, 30, this.width / 2 + 154, this.height - 12);
		UiTheme.title(graphics, this, "Manage your Minecraft content", 42);
		super.render(graphics, mouseX, mouseY, delta);
		if (!status.isEmpty()) {
				graphics.drawCenteredString(this.font, Component.literal(status), this.width / 2, this.height - 22,
					UiTheme.ACCENT);
		}
	}
}