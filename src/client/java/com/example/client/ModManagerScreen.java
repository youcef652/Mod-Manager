package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModManagerScreen extends Screen {
	private static final int BUTTON_WIDTH = 180;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 24;
	private String status = "";

	public ModManagerScreen() {
		super(Component.literal("Mod Manager"));
	}

	@Override
	protected void init() {
		int buttonX = (this.width - BUTTON_WIDTH) / 2;
		int firstButtonY = this.height / 2 - BUTTON_GAP * 2;

		addRenderableWidget(Button.builder(Component.literal("Download"), button ->
				this.minecraft.setScreen(new DownloadScreen(this)))
				.bounds(buttonX, firstButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Updates"), button ->
				this.minecraft.setScreen(new UpdatesScreen(this)))
				.bounds(buttonX, firstButtonY + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Reload"), button -> reloadResources())
				.bounds(buttonX, firstButtonY + BUTTON_GAP * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Settings"), button ->
				this.minecraft.setScreen(new SettingsScreen(this)))
				.bounds(buttonX, firstButtonY + BUTTON_GAP * 3, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(buttonX, firstButtonY + BUTTON_GAP * 5, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
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
		renderBackground(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 40, 0xFFFFFF);
		super.render(graphics, mouseX, mouseY, delta);
		if (!status.isEmpty()) {
			graphics.drawCenteredString(this.font, Component.literal(status), this.width / 2, this.height / 2 + 110,
					0xBFBFBF);
		}
	}
}