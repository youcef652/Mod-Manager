package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {
	private static final int BUTTON_WIDTH = 240;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 24;

	private final Screen parent;
	private EditBox curseForgeKeyBox;

	public SettingsScreen(Screen parent) {
		super(Component.literal("Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int buttonX = (this.width - BUTTON_WIDTH) / 2;
		int firstButtonY = 24;

		addRenderableWidget(toggleButton("Auto Update", firstButtonY,
				() -> ModManagerSettings.autoUpdate,
				value -> {
					ModManagerSettings.autoUpdate = value;
					ModManagerSettings.save();
					if (value) {
						AutoUpdateService.startIfEnabled();
					}
				}));
		addRenderableWidget(toggleButton("Update Mods", firstButtonY + BUTTON_GAP,
				() -> ModManagerSettings.updateMods,
				value -> {
					ModManagerSettings.updateMods = value;
					ModManagerSettings.save();
				}));
		addRenderableWidget(toggleButton("Update Resource Packs", firstButtonY + BUTTON_GAP * 2,
				() -> ModManagerSettings.updateResourcePacks,
				value -> {
					ModManagerSettings.updateResourcePacks = value;
					ModManagerSettings.save();
				}));
		addRenderableWidget(toggleButton("Update Shader Packs", firstButtonY + BUTTON_GAP * 3,
				() -> ModManagerSettings.updateShaderPacks,
				value -> {
					ModManagerSettings.updateShaderPacks = value;
					ModManagerSettings.save();
				}));
		addRenderableWidget(toggleButton("Update Data Packs", firstButtonY + BUTTON_GAP * 4,
				() -> ModManagerSettings.updateDataPacks,
				value -> {
					ModManagerSettings.updateDataPacks = value;
					ModManagerSettings.save();
				}));
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(buttonX, firstButtonY + BUTTON_GAP * 6, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		curseForgeKeyBox = new EditBox(this.font, buttonX, firstButtonY + BUTTON_GAP * 5,
				BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("CurseForge API Key"));
		curseForgeKeyBox.setValue(ModManagerSettings.curseForgeApiKey);
		curseForgeKeyBox.setHint(Component.literal("CurseForge API Key"));
		curseForgeKeyBox.setMaxLength(256);
		addRenderableWidget(curseForgeKeyBox);
	}

	private Button toggleButton(String label, int y, ToggleValue getter, ToggleSetter setter) {
		Button button = Button.builder(toggleLabel(label, getter.get()), ignored -> {
			boolean value = !getter.get();
			setter.set(value);
			buttonMessage(ignored, toggleLabel(label, value));
		}).bounds((this.width - BUTTON_WIDTH) / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
		return button;
	}

	private Component toggleLabel(String label, boolean enabled) {
		return Component.literal((enabled ? "[x] " : "[ ] ") + label);
	}

	private void buttonMessage(Button button, Component message) {
		button.setMessage(message);
	}

	@Override
	public void onClose() {
		if (curseForgeKeyBox != null) {
			ModManagerSettings.curseForgeApiKey = curseForgeKeyBox.getValue().trim();
			ModManagerSettings.save();
		}
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		UiTheme.background(graphics, this);
		UiTheme.panel(graphics, this.width / 2 - BUTTON_WIDTH / 2, 18,
				this.width / 2 + BUTTON_WIDTH / 2, this.height - 10);
		UiTheme.title(graphics, this, "Choose how Mod Manager behaves", 4);
		super.render(graphics, mouseX, mouseY, delta);
	}

	@FunctionalInterface
	private interface ToggleValue {
		boolean get();
	}

	@FunctionalInterface
	private interface ToggleSetter {
		void set(boolean value);
	}
}