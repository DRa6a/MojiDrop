package mod.dra6a.client;

import mod.dra6a.client.config.MojiDropConfig;
import mod.dra6a.client.config.MojiDropConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class MojiDropClient implements ClientModInitializer {
	private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
		"key.mojidrop.openConfig",
		GLFW.GLFW_KEY_O,
		KeyMapping.Category.MISC
	);

	@Override
	public void onInitializeClient() {
		MojiDropConfig.load();

		KeyMappingHelper.registerKeyMapping(OPEN_CONFIG_KEY);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (OPEN_CONFIG_KEY.consumeClick() && !(client.gui.screen() instanceof MojiDropConfigScreen)) {
				client.setScreenAndShow(new MojiDropConfigScreen(client.gui.screen()));
			}
		});
	}
}
