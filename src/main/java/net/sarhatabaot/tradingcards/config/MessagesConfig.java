package net.sarhatabaot.tradingcards.config;

import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;


public class MessagesConfig {
	final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
			.path(Path.of("messages.yml")) // This is the default config.
			.build();

}
