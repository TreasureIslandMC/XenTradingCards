package media.xen.tradingcards.config;

import media.xen.tradingcards.TradingCards;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Simple class to initialize, reload & save config files
 */
public class SimpleConfig {
	protected final TradingCards plugin;
	private final String fileName;

	private File file;
	private FileConfiguration config;

	public SimpleConfig(final TradingCards plugin, final String fileName) {
		this.plugin = plugin;
		this.fileName = fileName;
	}

	public void saveDefaultConfig() {
		if (this.file == null) {
			this.file = new File(plugin.getDataFolder(), fileName);
		}

		if (!this.file.exists()) {
			plugin.saveResource(fileName, false);
		}

		reloadConfig();
	}

	public void saveConfig(){
		if (this.config != null && file != null) {
			try {
				config.save(file);
			} catch (IOException var2) {
				plugin.getLogger().warning(var2.getMessage());
			}

		}
	}


	public void reloadConfig(){
		if (file == null) {
			file = new File(plugin.getDataFolder(), fileName);
		}

		config = YamlConfiguration.loadConfiguration(file);
	}

	public void reloadDefaultConfig(){
		if (file == null) {
			file = new File(plugin.getDataFolder(), fileName);
		}

		if(!file.exists()) {
			config = YamlConfiguration.loadConfiguration(file);
			Reader defConfigStream;
			defConfigStream = new InputStreamReader(plugin.getResource(fileName), StandardCharsets.UTF_8);
			if (defConfigStream != null) {
				YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
				config.setDefaults(defConfig);
			}
		}
	}


	public FileConfiguration getConfig(){
		if(config==null){
			reloadConfig();
		}
		return this.config;
	}

}
