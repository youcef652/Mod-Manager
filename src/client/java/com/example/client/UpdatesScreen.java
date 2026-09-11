package com.example.client;

import com.example.ExampleMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class UpdatesScreen extends Screen {
	private static final int BUTTON_WIDTH = 220;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 24;

	private final Screen parent;
	private final List<ModrinthApi.UpdateResult> updates = new ArrayList<>();
	private String status = "Checking for updates...";

	public UpdatesScreen(Screen parent) {
		super(Component.literal("Updates"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.literal("Check Again"), button -> scan())
				.bounds((this.width - BUTTON_WIDTH) / 2, 70, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds((this.width - BUTTON_WIDTH) / 2, this.height - 45, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());

		int updateY = 120;
		for (ModrinthApi.UpdateResult update : updates) {
			addRenderableWidget(Button.builder(Component.literal("Update " + update.latestName()),
					button -> update(update))
					.bounds((this.width - BUTTON_WIDTH) / 2, updateY, BUTTON_WIDTH, BUTTON_HEIGHT)
					.build());
			updateY += BUTTON_GAP;
		}
	}

	@Override
	public void added() {
		super.added();
		scan();
	}

	private void scan() {
		status = "Checking for updates...";
		updates.clear();
		clearWidgets();
		init();

		List<ModContainer> installedMods = FabricLoader.getInstance().getAllMods().stream()
				.filter(mod -> !mod.getMetadata().getId().equals("minecraft"))
				.filter(mod -> !mod.getMetadata().getId().equals("fabricloader"))
				.filter(mod -> !mod.getMetadata().getId().equals(ExampleMod.MOD_ID))
				.filter(mod -> mod.getOrigin().getKind() != net.fabricmc.loader.api.metadata.ModOrigin.Kind.NESTED)
				.filter(new java.util.function.Predicate<>() {
					private final Set<String> seen = new HashSet<>();

					@Override
					public boolean test(ModContainer mod) {
						return seen.add(mod.getMetadata().getId());
					}
				})
				.toList();

		List<CompletableFuture<Optional<ModrinthApi.UpdateResult>>> checks = installedMods.stream()
				.map(mod -> ModrinthApi.checkForUpdate(mod.getMetadata().getId(),
						mod.getMetadata().getVersion().getFriendlyString(), installedFilename(mod))
						.exceptionally(error -> Optional.empty()))
				.toList();
		CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new)).thenRun(() -> this.minecraft.execute(() -> {
			for (CompletableFuture<Optional<ModrinthApi.UpdateResult>> check : checks) {
				check.join().ifPresent(updates::add);
			}
			status = updates.isEmpty() ? "Everything is up to date" : updates.size() + " update(s) available";
			clearWidgets();
			init();
		}));
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

	private void update(ModrinthApi.UpdateResult update) {
		status = "Downloading " + update.latestName() + "...";
		Path existingFile = findInstalledFile(update.projectId());
		ModrinthApi.downloadLatest(new ModrinthApi.SearchResult(update.latestName(), update.projectId(), ""),
				"Mods", ModManagerSettings.gameDirectory(), existingFile).thenAccept(filename -> this.minecraft.execute(() -> {
			status = "Downloaded " + filename;
			updates.remove(update);
			clearWidgets();
			init();
		})).exceptionally(error -> {
			this.minecraft.execute(() -> status = "Update failed: " + error.getMessage());
			return null;
		});
	}

	private static Path findInstalledFile(String projectId) {
		return FabricLoader.getInstance().getAllMods().stream()
				.filter(mod -> mod.getMetadata().getId().equals(projectId))
				.map(UpdatesScreen::originPathsSafely)
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

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		UiTheme.background(graphics, this);
		UiTheme.panel(graphics, this.width / 2 - BUTTON_WIDTH / 2, 28,
				this.width / 2 + BUTTON_WIDTH / 2, this.height - 10);
		UiTheme.title(graphics, this, "Keep installed content current", 12);
		graphics.drawCenteredString(this.font, Component.literal(status), this.width / 2, 100, UiTheme.MUTED);
		super.render(graphics, mouseX, mouseY, delta);
	}
}