package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ContentManagementScreen extends Screen {
	private static final int BUTTON_WIDTH = 118;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 22;

	private final Screen parent;
	private String selectedType = "Mods";
	private Path selectedFile;
	private EditBox nameBox;
	private String status = "Select a file to manage";
	private List<Path> files = List.of();

	public ContentManagementScreen(Screen parent) {
		super(Component.literal("Manage Content"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = this.width / 2 - BUTTON_WIDTH - 6;
		int right = this.width / 2 + 6;
		addRenderableWidget(typeButton("Mods", left, 30));
		addRenderableWidget(typeButton("Resource Packs", right, 30));
		addRenderableWidget(typeButton("Shaders", left, 52));
		addRenderableWidget(typeButton("Data Packs", right, 52));

		files = listFiles();
		int rowY = 84;
		for (Path file : files) {
			if (rowY + BUTTON_HEIGHT > 158) {
				break;
			}
			Path item = file;
			addRenderableWidget(Button.builder(Component.literal(trimName(file.getFileName().toString())),
					button -> selectFile(item))
					.bounds(this.width / 2 - 118, rowY, 236, BUTTON_HEIGHT)
					.build());
			rowY += GAP;
		}

		nameBox = new EditBox(this.font, this.width / 2 - 118, 166, 236, BUTTON_HEIGHT, Component.literal("File name"));
		nameBox.setHint(Component.literal("File name"));
		nameBox.setMaxLength(120);
		addRenderableWidget(nameBox);
		addRenderableWidget(Button.builder(Component.literal("Rename"), button -> renameSelected())
				.bounds(this.width / 2 - 118, 190, 74, BUTTON_HEIGHT).build());
		String toggleLabel = selectedFile != null && isDisabled(selectedFile) ? "On" : "Off";
		addRenderableWidget(Button.builder(Component.literal(toggleLabel), button -> toggleSelected())
				.bounds(this.width / 2 - 40, 190, 80, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal("Delete"), button -> deleteSelected())
				.bounds(this.width / 2 + 44, 190, 74, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(this.width / 2 - 118, 214, 236, BUTTON_HEIGHT).build());
	}

	private Button typeButton(String type, int x, int y) {
		return Button.builder(Component.literal(type), button -> {
			selectedType = type;
			selectedFile = null;
			status = "Select a file to manage";
			refresh();
		}).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
	}

	private List<Path> listFiles() {
		Path directory = directory();
		try {
			if (!Files.exists(directory)) {
				Files.createDirectories(directory);
			}
			try (var stream = Files.list(directory)) {
				return stream.filter(Files::isRegularFile).sorted().toList();
			}
		} catch (Exception error) {
			status = "Could not read " + selectedType;
			return List.of();
		}
	}

	private Path directory() {
		return ModManagerSettings.gameDirectory().resolve(switch (selectedType) {
			case "Resource Packs" -> "resourcepacks";
			case "Shaders" -> "shaderpacks";
			case "Data Packs" -> "datapacks";
			default -> "mods";
		});
	}

	private void selectFile(Path file) {
		selectedFile = file;
		nameBox.setValue(file.getFileName().toString());
		status = "Selected " + file.getFileName();
	}

	private void renameSelected() {
		if (selectedFile == null) {
			status = "Select a file first";
			return;
		}
		String requested = nameBox.getValue().trim();
		if (requested.isEmpty() || requested.contains("/") || requested.contains("\\")
				|| requested.equals(".") || requested.equals("..")) {
			status = "Enter a valid file name";
			return;
		}
		String original = selectedFile.getFileName().toString();
		int extension = original.lastIndexOf('.');
		if (extension > 0 && requested.lastIndexOf('.') <= 0) {
			requested += original.substring(extension);
		}
		try {
			Path target = directory().resolve(requested).normalize();
			if (!target.getParent().equals(directory()) || Files.exists(target)) {
				status = "Name already exists";
				return;
			}
			Files.move(selectedFile, target);
			status = "Renamed successfully";
			selectedFile = target;
			refresh();
		} catch (Exception error) {
			status = "Rename failed";
		}
	}

	private void deleteSelected() {
		if (selectedFile == null) {
			status = "Select a file first";
			return;
		}
		try {
			Files.deleteIfExists(selectedFile);
			status = "Deleted successfully";
			selectedFile = null;
			nameBox.setValue("");
			refresh();
		} catch (Exception error) {
			status = "Delete failed";
		}
	}

	private void toggleSelected() {
		if (selectedFile == null) {
			status = "Select a file first";
			return;
		}
		String filename = selectedFile.getFileName().toString();
		String toggledName = isDisabled(selectedFile)
				? filename.substring(0, filename.length() - ".disabled".length())
				: filename + ".disabled";
		try {
			Path target = directory().resolve(toggledName).normalize();
			if (!target.getParent().equals(directory()) || Files.exists(target)) {
				status = "A file with that name already exists";
				return;
			}
			Files.move(selectedFile, target);
			selectedFile = target;
			nameBox.setValue(target.getFileName().toString());
			status = isDisabled(target) ? "Enabled successfully" : "Disabled successfully";
			refresh();
		} catch (Exception error) {
			status = "Could not change file state";
		}
	}

	private boolean isDisabled(Path file) {
		return file.getFileName().toString().endsWith(".disabled");
	}

	private void refresh() {
		clearWidgets();
		init();
	}

	private String trimName(String name) {
		return name.length() > 28 ? name.substring(0, 25) + "..." : name;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		UiTheme.background(graphics, this);
		UiTheme.panel(graphics, this.width / 2 - 150, 22, this.width / 2 + 150, 250);
		graphics.drawCenteredString(this.font, Component.literal("File Browser"), this.width / 2, 28, 0xFFEAF7F5);
		graphics.drawCenteredString(this.font, Component.literal("Manage installed files and toggle them on or off"), this.width / 2, 42, 0xFF9ABAB5);
		graphics.drawCenteredString(this.font, Component.literal(status), this.width / 2, 160, 0xFF8AF0C5);
		super.render(graphics, mouseX, mouseY, delta);
	}
}