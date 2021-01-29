package media.xen.tradingcards.config;

import media.xen.tradingcards.TradingCards;
import net.sarhatabaot.configloader.Config;
import net.sarhatabaot.configloader.ConfigOption;
import net.sarhatabaot.configloader.transform.ListClone;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TradingCardsConfig implements Config {
	private static TradingCards plugin;
	private final File file;

	public TradingCardsConfig(final TradingCards plugin) {
		this.file = new File(plugin.getDataFolder(), "config-old.yml");
		TradingCardsConfig.plugin = plugin;
	}

	@ConfigOption(path = "General.Server-Name")
	public String serverName = "Server";

	@ConfigOption(path = "General.Debug-Mode")
	public boolean debugMode = false;

	@ConfigOption(path = "General.Hide-Enchants")
	public boolean hideEnchants = true;

	@ConfigOption(path = "General.American-Mode")
	public boolean americanMode = false;

	@ConfigOption(path = "General.Card-Material")
	public String cardMaterial = "PAPER";

	@ConfigOption(path = "General.Card-Prefix")
	public String cardPrefix = "&7[&fCard&7]&f ";

	@ConfigOption(path = "General.BoosterPack-Material")
	public String boosterPackMaterial = "BOOK";
	@ConfigOption(path = "General.BoosterPack-Prefix")
	public String boosterPackPrefix = "&7[&fPack&7]&f ";
	@ConfigOption(path = "General.Decks-In-Creative")
	public boolean decksInCreative = false;
	@ConfigOption(path = "General.Use-Deck-Item")
	public boolean useDeckItems = true;
	@ConfigOption(path = "General.Use-Large-Decks")
	public boolean useLargeDecks = false;
	@ConfigOption(path = "General.Deck-Material")
	public String deckMaterial = "BOOK";
	@ConfigOption(path = "General.Deck-Prefix")
	public String deckPrefix = "&7[&fDeck&7]&f ";
	@ConfigOption(path = "General.Shiny-Name")
	public String shinyName = "Shiny";
	@ConfigOption(path = "General.Schedule-Cards")
	public boolean scheduleCards = false;
	@ConfigOption(path = "General.Schedule-Cards-Natural")
	public boolean scheduleCardNatural = false;
	@ConfigOption(path = "General.Schedule-Card-Mob")
	public String scheduleCardMob = "ZOMBIE";
	@ConfigOption(path = "General.Schedule-Card-Rarity")
	public String scheduleCardRarity = "Common";
	@ConfigOption(path = "General.Schedule-Card-Time-In-Hours")
	public int scheduleCardTimeInHours = 1;
	@ConfigOption(path = "General.Spawner-Block")
	public boolean spawnerBlock = true;
	@ConfigOption(path = "General.Spawner-Mob-Name")
	public String spawnerMobName = "Spawned Mob";
	@ConfigOption(path = "General.Auto-Add-Players")
	public boolean autoAddPlayers = false;
	@ConfigOption(path = "General.Auto-Add-Player-Rarity")
	public String autoAddPlayersRarity = "Common";
	@ConfigOption(path = "General.Player-Op-Rarity")
	public String playerOpRarity = "Legendary";
	@ConfigOption(path = "General.Player-Series")
	public String playerSeries = "2020";
	@ConfigOption(path = "General.Player-Type")
	public String playerType = "Player";
	@ConfigOption(path = "General.Player-Has-Shiny-Version")
	public boolean playerHasShinyVersion = true;
	@ConfigOption(path = "General.Player-Drops-Card")
	public boolean playerDropCard = true;

	@ConfigOption(path = "General.Player-Drops-Card-Rarity")
	public int playerDropCardRarity = 100;
	@ConfigOption(path = "General.Info-Line-Length")
	public int infoLineLength = 25;
	@ConfigOption(path = "General.Drop-Deck-Items")
	public boolean dropDeckItems = false;
	@ConfigOption(path = "General.Allow-Rewards")
	public boolean allowRewards = true;
	@ConfigOption(path = "General.Eat-Shiny-Cards")
	public boolean eatShinyCards = false;

	@ConfigOption(path = "General.SQLite")
	public boolean SQLite = false;

	@ConfigOption(path = "World-Blacklist", transform = ListClone.class)
	public List<String> worldBlackList = new ArrayList<>();

	@ConfigOption(path = "Blacklist.Whitelist-Mode")
	public boolean blacklistWhitelistMode = false;
	@ConfigOption(path = "Blacklist.Players", transform = ListClone.class)
	public List<String> blacklistPlayers = Collections.singletonList("Herobrine");

	@ConfigOption(path = "PluginSupport.Vault.Vault-Enabled")
	public boolean vaultEnabled = true;
	@ConfigOption(path = "PluginSupport.Vault.Closed-Economy")
	public boolean closedEconomy = false;
	@ConfigOption(path = "PluginSupport.Vault.Server-Account")
	public String serverAccount = "TradingCards-Bank";

	@ConfigOption(path = "Colours.Series")
	public String seriesColour = "&a";
	@ConfigOption(path = "Colours.Type")
	public String typeColour = "&b";
	@ConfigOption(path = "Colours.Info")
	public String infoColour = "&e";
	@ConfigOption(path = "Colours.About")
	public String aboutColour = "&c";
	@ConfigOption(path = "Colours.Rarity")
	public String rarityColour = "&6";
	@ConfigOption(path = "Colours.BoosterPackName")
	public String boosterPackNameColour;
	@ConfigOption(path = "Colours.BoosterPackLore")
	public String boosterPackLoreColour;
	@ConfigOption(path = "Colours.BoosterPackNormalCards")
	public String boosterPackNormalCardsColour;
	@ConfigOption(path = "Colours.BoosterPackSpecialCards")
	public String boosterPackSpecialCardsColour;
	@ConfigOption(path = "Colours.BoosterPackExtraCards")
	public String boosterPackExtraCardsColour;
	@ConfigOption(path = "Colours.ListHaveCard")
	public String listHaveCardColour;
	@ConfigOption(path = "Colours.ListHaveShinyCard")
	public String listHaveShinyCardColour;
	@ConfigOption(path = "Colours.ListRarityComplete")
	public String listRarityCompleteColour;

	@ConfigOption(path = "DisplayNames.Cards.Title")
	public String titleDisplay = "%PREFIX%%COLOUR%%NAME%";
	@ConfigOption(path = "DisplayNames.Cards.ShinyTitle")
	public String shinyTitleDisplay = "%PREFIX%%COLOUR%%SHINYPREFIX% %NAME%";
	@ConfigOption(path = "DisplayNames.Cards.Series")
	public String seriesDisplay = "Series";
	@ConfigOption(path = "DisplayNames.Cards.Type")
	public String typeDisplay = "Type";
	@ConfigOption(path = "DisplayNames.Cards.Info")
	public String infoDisplay = "Info";
	@ConfigOption(path = "DisplayNames.Cards.About")
	public String aboutDisplay = "About";


	public static ItemStack getBlankCard(int quantity) {
		return new ItemStack(Material.getMaterial(plugin.getMainConfig().cardMaterial), quantity);
	}

	public static ItemStack getBlankBoosterPack() {
		return new ItemStack(Material.getMaterial(plugin.getMainConfig().boosterPackMaterial));
	}

	public static ItemStack getBlankDeck() {
		return new ItemStack(Material.getMaterial(plugin.getMainConfig().deckMaterial));
	}

	@Override
	public File getFile() {
		return file;
	}
}
