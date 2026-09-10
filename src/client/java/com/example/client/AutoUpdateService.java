package com.example.client;

import com.example.ExampleMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

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
				.toList();

		List<CompletableFuture<Optional<ModrinthApi.UpdateResult>>> checks = installedMods.stream()
				.map(mod -> ModrinthApi.checkForUpdate(mod.getMetadata().getId(),
						mod.getMetadata().getVersion().getFriendlyString())
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

	private static void download(ModrinthApi.UpdateResult update) {
		ModrinthApi.downloadLatest(new ModrinthApi.SearchResult(update.latestName(), update.projectId(), ""),
				"Mods", FabricLoader.getInstance().getGameDir()).thenAccept(filename ->
				ExampleMod.LOGGER.info("Auto-updated {} to {} ({})", update.projectId(), update.latestVersion(), filename))
				.exceptionally(error -> {
					ExampleMod.LOGGER.warn("Could not download update for {}", update.projectId(), error);
					return null;
				});
	}
}