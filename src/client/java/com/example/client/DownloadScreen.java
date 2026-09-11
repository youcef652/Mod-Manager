package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class DownloadScreen extends Screen {
	private static final int BUTTON_WIDTH = 120;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 24;

	private final Screen parent;
	private EditBox searchBox;
	private String selectedType = "Mods";
	private String selectedSource;
	private String status = "Enter a name to search";
	private List<ModrinthApi.SearchResult> results = List.of();

	public DownloadScreen(Screen parent) {
		super(Component.literal("Download"));
		this.parent = parent;
		this.selectedSource = ModManagerSettings.sourceFor(selectedType);
	}

	@Override
	protected void init() {
		int leftColumnX = this.width / 2 - BUTTON_WIDTH - 8;
		int rightColumnX = this.width / 2 + 8;
		int firstButtonY = 30;

		addRenderableWidget(selectionButton("Mods", leftColumnX, firstButtonY,
				() -> selectType("Mods")));
		addRenderableWidget(selectionButton("Resource Packs", rightColumnX, firstButtonY,
				() -> selectType("Resource Packs")));
		addRenderableWidget(selectionButton("Data Packs", leftColumnX, firstButtonY + BUTTON_GAP,
				() -> selectType("Data Packs")));
		addRenderableWidget(selectionButton("Shader Packs", rightColumnX, firstButtonY + BUTTON_GAP,
				() -> selectType("Shader Packs")));

		addRenderableWidget(selectionButton("Modrinth", leftColumnX, firstButtonY + BUTTON_GAP * 2,
				() -> selectSource("Modrinth")));
		addRenderableWidget(selectionButton("CurseForge", rightColumnX, firstButtonY + BUTTON_GAP * 2,
				() -> {
					selectSource("CurseForge");
					status = CurseForgeApi.isConfigured()
							? "CurseForge selected"
							: "Set CURSEFORGE_API_KEY before searching";
				}));

		searchBox = new EditBox(this.font, this.width / 2 - BUTTON_WIDTH, 102, BUTTON_WIDTH * 2, BUTTON_HEIGHT,
				Component.literal("Search"));
		searchBox.setHint(Component.literal("Search Modrinth"));
		searchBox.setMaxLength(80);
		addRenderableWidget(searchBox);
		addRenderableWidget(Button.builder(Component.literal("Search"), button -> search())
				.bounds(this.width / 2 - BUTTON_WIDTH, 126, BUTTON_WIDTH - 5, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(this.width / 2 + 5, 126, BUTTON_WIDTH - 5, BUTTON_HEIGHT)
				.build());

		int resultY = 174;
		for (ModrinthApi.SearchResult result : results) {
			if (resultY + BUTTON_HEIGHT > this.height) {
				break;
			}
			addRenderableWidget(Button.builder(Component.literal(result.title()), button -> download(result))
					.bounds(this.width / 2 - BUTTON_WIDTH, resultY, BUTTON_WIDTH * 2, BUTTON_HEIGHT)
					.build());
			resultY += BUTTON_GAP;
		}
	}

	private void selectType(String type) {
		selectedType = type;
		selectedSource = ModManagerSettings.sourceFor(type);
	}

	private void selectSource(String source) {
		selectedSource = source;
		ModManagerSettings.setSourceFor(selectedType, source);
	}

	private void search() {
		String query = searchBox.getValue().trim();
		if (query.isEmpty()) {
			status = "Enter a name to search";
			results = List.of();
			return;
		}
		if (selectedSource.equals("CurseForge") && !CurseForgeApi.isConfigured()) {
			status = "Set CURSEFORGE_API_KEY before searching";
			return;
		}

		status = "Searching " + selectedSource + "...";
		results = List.of();
		var search = selectedSource.equals("Modrinth")
				? ModrinthApi.search(selectedType, query)
				: CurseForgeApi.search(selectedType, query);
		search.thenAccept(foundResults ->
				this.minecraft.execute(() -> {
					results = foundResults;
					status = foundResults.isEmpty() ? "No results found" : "Results from " + selectedSource;
					rebuildWidgets(query);
				})).exceptionally(error -> {
				this.minecraft.execute(() -> {
					status = "Search failed: " + error.getMessage();
					rebuildWidgets(query);
				});
				return null;
			});
	}

	private void download(ModrinthApi.SearchResult result) {
		if (selectedSource.equals("CurseForge")) {
			downloadCurseForge(result);
			return;
		}
		status = "Downloading " + result.title() + "...";
		ModrinthApi.downloadLatest(result, selectedType, ModManagerSettings.gameDirectory())
				.thenAccept(filename -> this.minecraft.execute(() ->
						status = "Downloaded " + filename))
				.exceptionally(error -> {
					this.minecraft.execute(() -> status = "Download failed: " + error.getMessage());
					return null;
				});
	}

	private void downloadCurseForge(ModrinthApi.SearchResult result) {
		status = "Downloading " + result.title() + "...";
		CurseForgeApi.downloadLatest(result, selectedType, ModManagerSettings.gameDirectory())
				.thenAccept(filename -> this.minecraft.execute(() -> status = "Downloaded " + filename))
				.exceptionally(error -> {
					this.minecraft.execute(() -> status = "Download failed: " + error.getMessage());
					return null;
				});
	}

	private void rebuildWidgets(String query) {
		clearWidgets();
		init();
		searchBox.setValue(query);
	}

	private Button selectionButton(String label, int x, int y, Runnable action) {
		return Button.builder(Component.literal(label), button -> action.run())
				.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		UiTheme.background(graphics, this);
		UiTheme.panel(graphics, this.width / 2 - BUTTON_WIDTH - 8, 26,
				this.width / 2 + BUTTON_WIDTH + 8, 151);
		UiTheme.title(graphics, this, "Find content from trusted platforms", 8);
		graphics.drawCenteredString(this.font,
				Component.literal("Selected: " + selectedType + " | " + selectedSource),
				this.width / 2, 148, UiTheme.ACCENT);
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(this.font, Component.literal(status), this.width / 2, 158, UiTheme.MUTED);
	}
}