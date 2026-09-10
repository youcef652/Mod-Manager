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
			return resolvedUrl.thenCompose(downloadUrl -> sendBytes(downloadUrl)).thenCompose(bytes -> {
				try {
					Path directory = gameDirectory.resolve(switch (type) {
						case "Resource Packs" -> "resourcepacks";
						case "Data Packs" -> "datapacks";
						case "Shader Packs" -> "shaderpacks";
						default -> "mods";
					});
					Files.createDirectories(directory);
					String filename = file.get("fileName").getAsString();
					Path target = directory.resolve(Path.of(filename).getFileName());
					Files.write(target, bytes);
					return downloadRequiredDependencies(file, type, gameDirectory)
							.thenApply(ignored -> filename);
				} catch (Exception error) {
					return CompletableFuture.failedFuture(new IllegalStateException("Could not save CurseForge file", error));
				}
			});
		});
	}

	private static CompletableFuture<Void> downloadRequiredDependencies(JsonObject file, String type,
			Path gameDirectory) {
		if (!file.has("dependencies") || file.get("dependencies").isJsonNull()) {
			return CompletableFuture.completedFuture(null);
		}
		CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
		for (JsonElement element : file.getAsJsonArray("dependencies")) {
			JsonObject dependency = element.getAsJsonObject();
			if (dependency.has("relationType") && dependency.get("relationType").getAsInt() == 3
					&& dependency.has("modId") && dependency.has("fileId")) {
				chain = chain.thenCompose(ignored -> downloadDependency(dependency, type, gameDirectory));
			}
		}
		return chain;
	}

	private static CompletableFuture<Void> downloadDependency(JsonObject dependency, String type, Path gameDirectory) {
		String url = API_URL + "/mods/" + dependency.get("modId").getAsLong()
				+ "/files/" + dependency.get("fileId").getAsLong();
		return send(url).thenCompose(response -> {
			JsonObject file = JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("data");
			CompletableFuture<String> resolvedUrl;
			if (file.has("downloadUrl") && !file.get("downloadUrl").isJsonNull()) {
				resolvedUrl = CompletableFuture.completedFuture(file.get("downloadUrl").getAsString());
			} else {
				String endpoint = API_URL + "/mods/" + dependency.get("modId").getAsLong()
						+ "/files/" + dependency.get("fileId").getAsLong() + "/download-url";
				resolvedUrl = send(endpoint).thenApply(body ->
						JsonParser.parseString(body).getAsJsonObject().get("data").getAsString());
			}
			return resolvedUrl.thenCompose(downloadUrl -> sendBytes(downloadUrl))
					.thenAccept(bytes -> saveFile(file.get("fileName").getAsString(), bytes, type, gameDirectory));
		});
	}

	private static void saveFile(String filename, byte[] bytes, String type, Path gameDirectory) {
		try {
			Path directory = gameDirectory.resolve(switch (type) {
				case "Resource Packs" -> "resourcepacks";
				case "Data Packs" -> "datapacks";
				case "Shader Packs" -> "shaderpacks";
				default -> "mods";
			});
			Files.createDirectories(directory);
			Files.write(directory.resolve(Path.of(filename).getFileName()), bytes);
		} catch (Exception error) {
			throw new IllegalStateException("Could not save CurseForge dependency", error);
		}
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