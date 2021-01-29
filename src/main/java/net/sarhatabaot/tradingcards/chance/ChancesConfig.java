package net.sarhatabaot.tradingcards.chance;


import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;

public class ChancesConfig {
	final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
			.path(Path.of("chances.yml")) // This is the default config.
			.build();
}
