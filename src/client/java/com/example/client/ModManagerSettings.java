package com.example.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ModManagerSettings {
	private static final String FILE_NAME = "modmanager.json";
	public static boolean autoUpdate;
	public static boolean updateMods = true;
	public static boolean updateResourcePacks = true;
	public static boolean updateShaderPacks = true;
	public static boolean updateDataPacks = true;
	public static String modsSource = "Modrinth";
	public static String resourcePacksSource = "Modrinth";
	public static String shaderPacksSource = "Modrinth";
	public static String dataPacksSource = "Modrinth";
	public static String curseForgeApiKey = "";

	private ModManagerSettings() {
	}

	public static void load() {
		Path file = configFile();
		if (!Files.exists(file)) {
			return;
		}
		try {
			JsonObject config = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			autoUpdate = booleanValue(config, "autoUpdate", autoUpdate);
			updateMods = booleanValue(config, "updateMods", updateMods);
			updateResourcePacks = booleanValue(config, "updateResourcePacks", updateResourcePacks);
			updateShaderPacks = booleanValue(config, "updateShaderPacks", updateShaderPacks);
			updateDataPacks = booleanValue(config, "updateDataPacks", updateDataPacks);
			modsSource = stringValue(config, "modsSource", modsSource);
			resourcePacksSource = stringValue(config, "resourcePacksSource", resourcePacksSource);
			shaderPacksSource = stringValue(config, "shaderPacksSource", shaderPacksSource);
			dataPacksSource = stringValue(config, "dataPacksSource", dataPacksSource);
			curseForgeApiKey = stringValue(config, "curseForgeApiKey", curseForgeApiKey);
		} catch (Exception ignored) {
		}
	}

	public static void save() {
		JsonObject config = new JsonObject();
		config.addProperty("autoUpdate", autoUpdate);
		config.addProperty("updateMods", updateMods);
		config.addProperty("updateResourcePacks", updateResourcePacks);
		config.addProperty("updateShaderPacks", updateShaderPacks);
		config.addProperty("updateDataPacks", updateDataPacks);
		config.addProperty("modsSource", modsSource);
		config.addProperty("resourcePacksSource", resourcePacksSource);
		config.addProperty("shaderPacksSource", shaderPacksSource);
		config.addProperty("dataPacksSource", dataPacksSource);
		config.addProperty("curseForgeApiKey", curseForgeApiKey);
		try {
			Path file = configFile();
			Files.createDirectories(file.getParent());
			Files.writeString(file, config.toString());
		} catch (Exception ignored) {
		}
	}

	private static boolean booleanValue(JsonObject config, String key, boolean fallback) {
		return config.has(key) ? config.get(key).getAsBoolean() : fallback;
	}

	private static String stringValue(JsonObject config, String key, String fallback) {
		return config.has(key) ? config.get(key).getAsString() : fallback;
	}

	public static String sourceFor(String type) {
		return switch (type) {
			case "Resource Packs" -> resourcePacksSource;
			case "Shader Packs" -> shaderPacksSource;
			case "Data Packs" -> dataPacksSource;
			default -> modsSource;
		};
	}

	public static void setSourceFor(String type, String source) {
		switch (type) {
			case "Resource Packs" -> resourcePacksSource = source;
			case "Shader Packs" -> shaderPacksSource = source;
			case "Data Packs" -> dataPacksSource = source;
			default -> modsSource = source;
		}
		save();
	}

	private static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}
}