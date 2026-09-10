package com.example.client.mixin;

import com.example.client.ModManagerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class ExampleClientMixin {
	@Inject(at = @At("TAIL"), method = "init")
	private void modmanager$addButton(CallbackInfo info) {
		TitleScreen titleScreen = (TitleScreen) (Object) this;
		int buttonWidth = 140;
		int buttonX = (titleScreen.width - buttonWidth) / 2;

		((TitleScreenAccessor) titleScreen).modmanager$addButton(Button.builder(
				Component.literal("Mod Manager"),
				button -> Minecraft.getInstance().setScreen(new ModManagerScreen())
		).bounds(buttonX, 20, buttonWidth, 20).build());
	}
}