package com.example.client;

import com.example.ExampleMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class AutoUpdateService {
	private AutoUpdateService() {
	}

	public static void start() {
		List<ModContainer> installedMods = FabricLoader.getInstance().getAllMods().stream()
				.filter(mod -> !mod.getMetadata().getId().equals("minecraft"))
				.filter(mod -> !mod.getMetadata().getId().equals("fabricloader"))
				.filter(mod -> !mod.getMetadata().getId().equals(ExampleMod.MOD_ID))
				.filter(mod -> mod.getOrigin().getKind() != net.fabricmc.loader.api.metadata.ModOrigin.Kind.NESTED)
				.toList();

		List<CompletableFuture<Optional<ModrinthApi.UpdateResult>>> checks = installedMods.stream()
				.map(mod -> ModrinthApi.checkForUpdate(mod.getMetadata().getId(),
						mod.getMetadata().getVersion().getFriendlyString(), installedFilename(mod))
						.exceptionally(error -> {
							ExampleMod.LOGGER.debug("Could not check {} for updates", mod.getMetadata().getId(), error);
							return Optional.empty();
						}))
				.toList();

		CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new)).thenRun(() -> {
			for (CompletableFuture<Optional<ModrinthApi.UpdateResult>> check : checks) {
				check.join().ifPresent(AutoUpdateService::download);
			}
		});
	}

	private static String installedFilename(ModContainer mod) {
		try {
			return mod.getOrigin().getPaths().stream()
					.filter(java.nio.file.Files::isRegularFile)
					.map(path -> path.getFileName().toString())
					.findFirst()
					.orElse("");
		} catch (UnsupportedOperationException ignored) {
			return "";
		}
	}

	private static void download(ModrinthApi.UpdateResult update) {
		Path existingFile = findInstalledFile(update.projectId());
		ModrinthApi.downloadLatest(new ModrinthApi.SearchResult(update.latestName(), update.projectId(), ""),
				"Mods", FabricLoader.getInstance().getGameDir(), existingFile).thenAccept(filename ->
				ExampleMod.LOGGER.info("Auto-updated {} to {} ({})", update.projectId(), update.latestVersion(), filename))
				.exceptionally(error -> {
					ExampleMod.LOGGER.warn("Could not download update for {}", update.projectId(), error);
					return null;
				});
	}

	private static Path findInstalledFile(String projectId) {
		return FabricLoader.getInstance().getAllMods().stream()
				.filter(mod -> mod.getMetadata().getId().equals(projectId))
				.map(AutoUpdateService::originPathsSafely)
				.flatMap(List::stream)
				.filter(java.nio.file.Files::isRegularFile)
				.findFirst()
				.orElse(null);
	}

	private static List<Path> originPathsSafely(ModContainer mod) {
		try {
			return mod.getOrigin().getPaths();
		} catch (UnsupportedOperationException ignored) {
			return List.of();
		}
	}
}