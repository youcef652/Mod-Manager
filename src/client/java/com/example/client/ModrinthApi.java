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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
		String facet = URLEncoder.encode("[[\"project_type:" + projectType(type) + "\"],[\"versions:1.21.11\"]"
				+ (type.equals("Mods") ? ",[\"categories:fabric\"]]" : "]"),
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
		return downloadLatest(result, type, gameDirectory, null);
	}

	public static CompletableFuture<String> downloadLatest(SearchResult result, String type, Path gameDirectory,
			Path existingFile) {
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
					return downloadVersion(version, type, gameDirectory, existingFile, new HashSet<>())
							.thenApply(files -> files.get(0));
				});
	}

	private static CompletableFuture<List<String>> downloadVersion(JsonObject version, String type,
			Path gameDirectory, Path existingFile, Set<String> downloadedProjects) {
		JsonObject file = primaryFile(version);
		Path directory = contentDirectory(type, gameDirectory);
		String projectId = version.has("project_id") ? version.get("project_id").getAsString() : "";
		if (!projectId.isBlank() && !downloadedProjects.add(projectId)) {
			return CompletableFuture.completedFuture(List.of());
		}

		return downloadFile(file.get("url").getAsString(), file.get("filename").getAsString(), directory, existingFile)
				.thenCompose(filename -> {
					List<JsonObject> dependencies = requiredDependencies(version);
					CompletableFuture<List<String>> chain = CompletableFuture.completedFuture(new ArrayList<>(List.of(filename)));
					for (JsonObject dependency : dependencies) {
						chain = chain.thenCompose(files -> resolveDependency(dependency, type)
								.thenCompose(dependencyVersion -> downloadVersion(dependencyVersion, type, gameDirectory, null,
										downloadedProjects))
								.thenApply(dependencyFiles -> {
									files.addAll(dependencyFiles);
									return files;
								}));
					}
					return chain;
				});
	}

	private static List<JsonObject> requiredDependencies(JsonObject version) {
		if (!version.has("dependencies")) {
			return List.of();
		}
		List<JsonObject> required = new ArrayList<>();
		for (JsonElement element : version.getAsJsonArray("dependencies")) {
			JsonObject dependency = element.getAsJsonObject();
			if ("required".equals(dependency.has("dependency_type")
					? dependency.get("dependency_type").getAsString() : "")) {
				required.add(dependency);
			}
		}
		return required;
	}

	private static CompletableFuture<JsonObject> resolveDependency(JsonObject dependency, String type) {
		if (dependency.has("version_id") && !dependency.get("version_id").isJsonNull()
				&& !dependency.get("version_id").getAsString().isBlank()) {
			return fetchVersion(dependency.get("version_id").getAsString());
		}
		if (!dependency.has("project_id") || dependency.get("project_id").isJsonNull()) {
			return CompletableFuture.failedFuture(new IllegalStateException("Dependency has no project or version id"));
		}
		String projectId = dependency.get("project_id").getAsString();
		String url = "https://api.modrinth.com/v2/project/" + projectId + "/version"
				+ "?game_versions=" + encode("[\"1.21.11\"]")
				+ (type.equals("Mods") ? "&loaders=" + encode("[\"fabric\"]") : "");
		return getJsonArray(url).thenCompose(versions -> {
			if (versions.isEmpty()) {
				return CompletableFuture.failedFuture(new IllegalStateException("No compatible dependency version found"));
			}
			return CompletableFuture.completedFuture(versions.get(0).getAsJsonObject());
		});
	}

	private static CompletableFuture<JsonObject> fetchVersion(String versionId) {
		return getJsonObject("https://api.modrinth.com/v2/version/" + versionId);
	}

	private static CompletableFuture<JsonArray> getJsonArray(String url) {
		return sendJson(url).thenApply(JsonElement::getAsJsonArray);
	}

	private static CompletableFuture<JsonObject> getJsonObject(String url) {
		return sendJson(url).thenApply(JsonElement::getAsJsonObject);
	}

	private static CompletableFuture<JsonElement> sendJson(String url) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(15))
				.header("User-Agent", "Mod-Manager/1.0 (github.com/youcef652/Mod-Manager)")
				.GET()
				.build();
		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					if (response.statusCode() / 100 != 2) {
						throw new IllegalStateException("Modrinth returned HTTP " + response.statusCode());
					}
					return JsonParser.parseString(response.body());
				});
	}

	public static CompletableFuture<Optional<UpdateResult>> checkForUpdate(String projectId, String currentVersion,
			String installedFilename) {
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
					String latestFilename = primaryFile(latest).get("filename").getAsString();
					if (sameVersion(latestVersion, currentVersion)
							|| (!installedFilename.isBlank() && latestFilename.equals(installedFilename))) {
						return Optional.<UpdateResult>empty();
					}
					return Optional.of(new UpdateResult(projectId, currentVersion, latestVersion,
							latest.get("name").getAsString(), latestFilename));
				});
	}

	private static boolean sameVersion(String first, String second) {
		return normalizeVersion(first).equals(normalizeVersion(second));
	}

	private static JsonObject primaryFile(JsonObject version) {
		return version.getAsJsonArray("files").asList().stream()
				.map(JsonElement::getAsJsonObject)
				.filter(file -> file.has("primary") && file.get("primary").getAsBoolean())
				.findFirst()
				.orElse(version.getAsJsonArray("files").get(0).getAsJsonObject());
	}

	private static String normalizeVersion(String version) {
		String normalized = version.trim().toLowerCase(java.util.Locale.ROOT);
		if (normalized.startsWith("v")) {
			normalized = normalized.substring(1);
		}
		return normalized;
	}

	private static CompletableFuture<String> downloadFile(String url, String filename, Path directory, Path existingFile) {
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
						Path target = directory.resolve(safeFilename);
						Files.write(target, response.body());
						if (existingFile != null && Files.isRegularFile(existingFile)
								&& !existingFile.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
							Files.deleteIfExists(existingFile);
						}
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

	public record UpdateResult(String projectId, String currentVersion, String latestVersion, String latestName,
			String latestFilename) {
	}
}