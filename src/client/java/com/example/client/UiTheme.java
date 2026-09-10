package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class UiTheme {
	public static final int ACCENT = 0xFF5EE6A8;
	public static final int TEXT = 0xFFF4F7F5;
	public static final int MUTED = 0xFF9CAAA5;
	public static final int PANEL = 0xD91B2524;
	public static final int PANEL_EDGE = 0xFF30423D;

	private UiTheme() {
	}

	public static void background(GuiGraphics graphics, Screen screen) {
		graphics.fill(0, 0, screen.width, screen.height, 0xFF0B1111);
		graphics.fill(0, screen.height / 3, screen.width, screen.height, 0xFF101B19);
		graphics.fill(0, 0, screen.width, 2, ACCENT);
	}

	public static void panel(GuiGraphics graphics, int left, int top, int right, int bottom) {
		graphics.fill(left - 1, top - 1, right + 1, bottom + 1, PANEL_EDGE);
		graphics.fill(left, top, right, bottom, PANEL);
	}

	public static void title(GuiGraphics graphics, Screen screen, String subtitle, int y) {
		graphics.drawCenteredString(screen.getFont(), screen.getTitle(), screen.width / 2, y, TEXT);
		graphics.drawCenteredString(screen.getFont(), Component.literal(subtitle), screen.width / 2, y + 14, MUTED);
	}
}