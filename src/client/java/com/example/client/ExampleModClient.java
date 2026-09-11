package com.example.client;

import com.example.ExampleMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.impl.client.screen.ScreenExtensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModManagerSettings.load();
		logGameDirectories();
		ScreenEvents.AFTER_INIT.register(this::addTitleScreenButton);
	}

	private void logGameDirectories() {
		Path gameDirectory = ModManagerSettings.gameDirectory();
		try {
			Files.createDirectories(gameDirectory.resolve("mods"));
			ExampleMod.LOGGER.info("Mod Manager will install mods in: {}", gameDirectory.resolve("mods"));
		} catch (Exception error) {
			ExampleMod.LOGGER.error("Could not access Minecraft mods directory: {}", gameDirectory.resolve("mods"), error);
		}
	}

	private void addTitleScreenButton(Minecraft client, net.minecraft.client.gui.screens.Screen screen,
			int scaledWidth, int scaledHeight) {
		if (!(screen instanceof TitleScreen)) {
			return;
		}
		AutoUpdateService.startIfEnabled();

		int buttonWidth = 140;
		int buttonHeight = 20;
		int margin = 10;
		int buttonX = margin;
		int buttonY = margin;
		ScreenExtensions.getExtensions(screen).fabric_getButtons().add(Button.builder(
				Component.literal("Mod Manager"),
				button -> client.setScreen(new ModManagerScreen())
		).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build());
	}
}