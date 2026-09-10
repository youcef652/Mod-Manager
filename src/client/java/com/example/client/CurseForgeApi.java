package com.example.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CurseForgeApi {
	private static final String API_URL = "https://api.curseforge.com/v1";
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	private CurseForgeApi() {
	}

	public static boolean isConfigured() {
		return apiKey() != null;
	}

	public static CompletableFuture<List<ModrinthApi.SearchResult>> search(String type, String query) {
		String url = API_URL + "/mods/search?gameId=432&classId=" + classId(type)
				+ "&searchFilter=" + encode(query) + "&gameVersion=1.21.11&pageSize=8"
				+ (type.equals("Mods") ? "&modLoaderType=4" : "");
		return send(url).thenApply(response -> {
			JsonArray data = JsonParser.parseString(response).getAsJsonObject().getAsJsonArray("data");
			List<ModrinthApi.SearchResult> results = new ArrayList<>();
			for (JsonElement item : data) {
				JsonObject project = item.getAsJsonObject();
				results.add(new ModrinthApi.SearchResult(
						project.get("name").getAsString(),
						project.get("id").getAsString(),
						project.has("summary") ? project.get("summary").getAsString() : ""
				));
			}
			return results;
		});
	}

	public static CompletableFuture<String> downloadLatest(ModrinthApi.SearchResult project, String type,
			Path gameDirectory) {
		String url = API_URL + "/mods/" + project.projectId() + "/files?gameVersion=1.21.11&pageSize=1"
				+ (type.equals("Mods") ? "&modLoaderType=4" : "");
		return send(url).thenCompose(response -> {
			JsonArray data = JsonParser.parseString(response).getAsJsonObject().getAsJsonArray("data");
			if (data.isEmpty()) {
				return CompletableFuture.failedFuture(new IllegalStateException("No compatible CurseForge file found"));
			}
			JsonObject file = data.get(0).getAsJsonObject();
			CompletableFuture<String> resolvedUrl;
			if (file.has("downloadUrl") && !file.get("downloadUrl").isJsonNull()) {
				resolvedUrl = CompletableFuture.completedFuture(file.get("downloadUrl").getAsString());
			} else {
				String urlEndpoint = API_URL + "/mods/" + project.projectId() + "/files/"
						+ file.get("id").getAsString() + "/download-url";
				resolvedUrl = send(urlEndpoint).thenApply(body ->
						JsonParser.parseString(body).getAsJsonObject().get("data").getAsString());
			}
			return resolvedUrl.thenCompose(downloadUrl -> sendBytes(downloadUrl)).thenApply(bytes -> {
				try {
					Path directory = gameDirectory.resolve(switch (type) {
						case "Resource Packs" -> "resourcepacks";
						case "Data Packs" -> "datapacks";
						case "Shader Packs" -> "shaderpacks";
						default -> "mods";
					});
					Files.createDirectories(directory);
					String filename = file.get("fileName").getAsString();
					Files.write(directory.resolve(Path.of(filename).getFileName()), bytes);
					return filename;
				} catch (Exception error) {
					throw new IllegalStateException("Could not save CurseForge file", error);
				}
			});
		});
	}

	private static CompletableFuture<String> send(String url) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofMinutes(2))
				.header("Accept", "application/json")
				.header("x-api-key", apiKey())
				.GET()
				.build();
		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					if (response.statusCode() / 100 != 2) {
						throw new IllegalStateException("CurseForge returned HTTP " + response.statusCode());
					}
					return response.body();
				});
	}

	private static CompletableFuture<byte[]> sendBytes(String url) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofMinutes(2))
				.header("x-api-key", apiKey())
				.GET()
				.build();
		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
				.thenApply(response -> {
					if (response.statusCode() / 100 != 2) {
						throw new IllegalStateException("CurseForge download returned HTTP " + response.statusCode());
					}
					return response.body();
				});
	}

	private static int classId(String type) {
		return switch (type) {
			case "Resource Packs" -> 12;
			case "Shader Packs" -> 6552;
			case "Data Packs" -> 6945;
			default -> 6;
		};
	}

	private static String apiKey() {
		if (!ModManagerSettings.curseForgeApiKey.isBlank()) {
			return ModManagerSettings.curseForgeApiKey.trim();
		}
		String key = System.getenv("CURSEFORGE_API_KEY");
		return key == null || key.isBlank() ? null : key;
	}

	private static String encode(String value) {
		return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}