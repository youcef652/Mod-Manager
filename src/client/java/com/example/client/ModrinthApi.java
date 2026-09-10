package com.example.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ModrinthApi {
	private static final String SEARCH_URL = "https://api.modrinth.com/v2/search";
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	private ModrinthApi() {
	}

	public static CompletableFuture<List<SearchResult>> search(String type, String query) {
		String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
		String facet = URLEncoder.encode("[[\"project_type:" + projectType(type) + "\"]]",
				StandardCharsets.UTF_8);
		URI uri = URI.create(SEARCH_URL + "?query=" + encodedQuery + "&facets=" + facet + "&limit=8");
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(15))
				.header("User-Agent", "Mod-Manager/1.0 (github.com/youcef652/Mod-Manager)")
				.GET()
				.build();

		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(ModrinthApi::parseResults);
	}

	public static CompletableFuture<String> downloadLatest(SearchResult result, String type, Path gameDirectory) {
		String versionsUrl = "https://api.modrinth.com/v2/project/" + result.projectId() + "/version"
				+ "?game_versions=" + encode("[\"1.21.11\"]")
				+ (type.equals("Mods") ? "&loaders=" + encode("[\"fabric\"]") : "");
		HttpRequest versionsRequest = HttpRequest.newBuilder(URI.create(versionsUrl))
				.timeout(Duration.ofSeconds(15))
				.header("User-Agent", "Mod-Manager/1.0 (github.com/youcef652/Mod-Manager)")
				.GET()
				.build();

		return HTTP_CLIENT.sendAsync(versionsRequest, HttpResponse.BodyHandlers.ofString())
				.thenCompose(response -> {
					if (response.statusCode() / 100 != 2) {
						return CompletableFuture.failedFuture(
								new IllegalStateException("Modrinth returned HTTP " + response.statusCode()));
					}
					JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
					if (versions.isEmpty()) {
						return CompletableFuture.failedFuture(
								new IllegalStateException("No compatible version found"));
					}
					JsonObject version = versions.get(0).getAsJsonObject();
					JsonObject file = version.getAsJsonArray("files").asList().stream()
							.map(JsonElement::getAsJsonObject)
							.filter(candidate -> candidate.has("primary") && candidate.get("primary").getAsBoolean())
							.findFirst()
							.orElse(version.getAsJsonArray("files").get(0).getAsJsonObject());
					return downloadFile(file.get("url").getAsString(), file.get("filename").getAsString(),
							contentDirectory(type, gameDirectory));
				});
	}

	public static CompletableFuture<Optional<UpdateResult>> checkForUpdate(String projectId, String currentVersion) {
		String versionsUrl = "https://api.modrinth.com/v2/project/" + projectId + "/version"
				+ "?game_versions=" + encode("[\"1.21.11\"]")
				+ "&loaders=" + encode("[\"fabric\"]");
		HttpRequest request = HttpRequest.newBuilder(URI.create(versionsUrl))
				.timeout(Duration.ofSeconds(15))
				.header("User-Agent", "Mod-Manager/1.0 (github.com/youcef652/Mod-Manager)")
				.GET()
				.build();

		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					if (response.statusCode() == 404) {
						return Optional.<UpdateResult>empty();
					}
					if (response.statusCode() / 100 != 2) {
						throw new IllegalStateException("Modrinth returned HTTP " + response.statusCode());
					}
					JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
					if (versions.isEmpty()) {
						return Optional.<UpdateResult>empty();
					}
					JsonObject latest = versions.get(0).getAsJsonObject();
					String latestVersion = latest.get("version_number").getAsString();
					if (latestVersion.equals(currentVersion)) {
						return Optional.<UpdateResult>empty();
					}
					return Optional.of(new UpdateResult(projectId, currentVersion, latestVersion,
							latest.get("name").getAsString()));
				});
	}

	private static CompletableFuture<String> downloadFile(String url, String filename, Path directory) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofMinutes(2))
				.header("User-Agent", "Mod-Manager/1.0 (github.com/youcef652/Mod-Manager)")
				.GET()
				.build();
		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
				.thenApply(response -> {
					if (response.statusCode() / 100 != 2) {
						throw new IllegalStateException("Download returned HTTP " + response.statusCode());
					}
					try {
						Files.createDirectories(directory);
						String safeFilename = Path.of(filename).getFileName().toString();
						Files.write(directory.resolve(safeFilename), response.body());
						return safeFilename;
					} catch (Exception error) {
						throw new IllegalStateException("Could not save downloaded file", error);
					}
				});
	}

	private static Path contentDirectory(String type, Path gameDirectory) {
		return gameDirectory.resolve(switch (type) {
			case "Resource Packs" -> "resourcepacks";
			case "Data Packs" -> "datapacks";
			case "Shader Packs" -> "shaderpacks";
			default -> "mods";
		});
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String projectType(String type) {
		return switch (type) {
			case "Resource Packs" -> "resourcepack";
			case "Data Packs" -> "datapack";
			case "Shader Packs" -> "shader";
			default -> "mod";
		};
	}

	private static List<SearchResult> parseResults(HttpResponse<String> response) {
		if (response.statusCode() / 100 != 2) {
			throw new IllegalStateException("Modrinth returned HTTP " + response.statusCode());
		}

		JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
		JsonArray hits = root.getAsJsonArray("hits");
		List<SearchResult> results = new ArrayList<>();
		for (JsonElement hitElement : hits) {
			JsonObject hit = hitElement.getAsJsonObject();
			results.add(new SearchResult(
					hit.get("title").getAsString(),
					hit.get("project_id").getAsString(),
					hit.has("description") ? hit.get("description").getAsString() : ""
			));
		}
		return results;
	}

	public record SearchResult(String title, String projectId, String description) {
	}

	public record UpdateResult(String projectId, String currentVersion, String latestVersion, String latestName) {
	}
}