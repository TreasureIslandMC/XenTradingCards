package media.xen.tradingcards;

import co.aikar.commands.BukkitCommandManager;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import media.xen.tradingcards.commands.CardsCommand;
import media.xen.tradingcards.commands.DeckCommand;
import media.xen.tradingcards.config.CardsConfig;
import media.xen.tradingcards.config.MessagesConfig;
import media.xen.tradingcards.config.TradingCardsConfig;
import media.xen.tradingcards.config.SimpleConfig;
import media.xen.tradingcards.db.Database;
import media.xen.tradingcards.db.DbUtil;
import media.xen.tradingcards.db.SQLite;
import media.xen.tradingcards.listeners.AddOnJoinListener;
import media.xen.tradingcards.listeners.DeckListener;
import media.xen.tradingcards.listeners.MobSpawnListener;
import media.xen.tradingcards.listeners.PackListener;
import media.xen.tradingcards.listeners.DropListener;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.sarhatabaot.configloader.ConfigLoader;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;


public class TradingCards extends JavaPlugin implements Listener {
	List<EntityType> hostileMobs = new ArrayList<>();
	List<EntityType> passiveMobs = new ArrayList<>();
	List<EntityType> neutralMobs = new ArrayList<>();
	List<EntityType> bossMobs = new ArrayList<>();
	private final Map<String, Database> databases = new HashMap<>();
	public static Permission permRarities = new Permission("cards.rarity");
	private boolean hasVault;
	private TradingCardsConfig mainConfig;
	private SimpleConfig deckConfig;
	private MessagesConfig messagesConfig;
	private CardsConfig cardsConfig;
	private boolean usingSqlite;
	private Economy econ = null;
	public static Permission perms = null;
	public static Chat chat = null;
	public Random r = new Random();
	int taskid;

	public boolean isHasVault() {
		return hasVault;
	}

	public Economy getEcon() {
		return econ;
	}

	public SimpleConfig getDeckConfig() {
		return deckConfig;
	}

	public CardsConfig getCardsConfig() {
		return cardsConfig;
	}

	public TradingCardsConfig getMainConfig() {
		return mainConfig;
	}

	private void hookVault() {
		if (this.getConfig().getBoolean("PluginSupport.Vault.Vault-Enabled")) {
			if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
				this.setupEconomy();
				getLogger().info("Vault hook successful!");
				this.hasVault = true;
			} else {
				getLogger().info("Vault not found, hook unsuccessful!");
			}
		}
	}

	public void initializeDatabase(String databaseName, String createStatement) {
		Database db = new SQLite(this, databaseName, createStatement, this.getDataFolder());
		db.load();
		this.databases.put(databaseName, db);
	}

	public Map<String, Database> getDatabases() {
		return this.databases;
	}

	public Database getDatabase(String databaseName) {
		return this.getDatabases().get(databaseName);
	}

	public Boolean exists(String statement) {
		return this.getDatabase("trading_cards").queryValue(statement, "ID") != null;
	}

	private void hookFileSystem() {
		if (this.getConfig().getBoolean("General.SQLite")) {
			this.usingSqlite = true;
			this.initializeDatabase("trading_cards", "CREATE TABLE IF NOT EXISTS cards(`id` INTEGER NOT NULL PRIMARY KEY, `rarity` varchar(255), `about` varchar(255), `series` varchar(255), `name` varchar(255), `type` varchar(255), `info` varchar(255), `price` int); CREATE TABLE IF NOT EXISTS decks(`id` INTEGER NOT NULL PRIMARY KEY, `uuid` varchar(512), `deckID` int, `card` int, `isShiny` int, `count` int)");
			getLogger().info("SQLite is enabled");
			DbUtil.init(this);
			DbUtil.convertToDb();
		} else {
			this.usingSqlite = false;
			getLogger().info("Legacy YML mode is enabled!");
		}
	}

	private void registerListeners() {
		PluginManager pm = Bukkit.getPluginManager();
		pm.addPermission(permRarities);
		pm.registerEvents(this, this);
		pm.registerEvents(new DropListener(this), this);
		pm.registerEvents(new PackListener(this), this);
		pm.registerEvents(new MobSpawnListener(this), this);
		pm.registerEvents(new AddOnJoinListener(this), this);
		pm.registerEvents(new DeckListener(this), this);
	}

	private void cacheMobs(){
		final String serverVersion = Bukkit.getServer().getVersion();
		ImmutableList.Builder<EntityType> safeHostileMobs = ImmutableList.builder();
		ImmutableList.Builder<EntityType> safeNeutralMobs = ImmutableList.builder();
		ImmutableList.Builder<EntityType> safePassiveMobs = ImmutableList.builder();
		ImmutableList.Builder<EntityType> safeBossMobs = ImmutableList.builder();
		safeHostileMobs.add(EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.BLAZE, EntityType.SILVERFISH, EntityType.GHAST, EntityType.SLIME, EntityType.EVOKER, EntityType.VINDICATOR, EntityType.VEX, EntityType.SHULKER, EntityType.GUARDIAN, EntityType.MAGMA_CUBE, EntityType.ELDER_GUARDIAN, EntityType.STRAY, EntityType.HUSK, EntityType.DROWNED, EntityType.WITCH, EntityType.ZOMBIE_VILLAGER, EntityType.ENDERMITE);
		safeNeutralMobs.add(EntityType.ENDERMAN, EntityType.POLAR_BEAR, EntityType.LLAMA, EntityType.WOLF, EntityType.DOLPHIN, EntityType.DOLPHIN, EntityType.SNOWMAN, EntityType.IRON_GOLEM);
		safePassiveMobs.add(EntityType.DONKEY, EntityType.MULE, EntityType.SKELETON_HORSE, EntityType.CHICKEN, EntityType.COW, EntityType.SQUID, EntityType.TURTLE, EntityType.TROPICAL_FISH, EntityType.PUFFERFISH, EntityType.SHEEP, EntityType.PIG, EntityType.PHANTOM, EntityType.SALMON, EntityType.COD, EntityType.RABBIT, EntityType.VILLAGER, EntityType.BAT, EntityType.PARROT, EntityType.HORSE);
		safeBossMobs.add(EntityType.ENDER_DRAGON, EntityType.WITHER);
		if(serverVersion.contains("1.14") || serverVersion.contains("1.15") || serverVersion.contains("1.16")){
			safeHostileMobs.add(EntityType.PILLAGER, EntityType.RAVAGER);
			safeNeutralMobs.add(EntityType.PANDA,EntityType.FOX);
			safePassiveMobs.add(EntityType.WANDERING_TRADER,EntityType.CAT,EntityType.MUSHROOM_COW,EntityType.TRADER_LLAMA);
			if(serverVersion.contains("1.15") || serverVersion.contains("1.16")){
				safeNeutralMobs.add(EntityType.BEE);
				if(serverVersion.contains("1.16")){
					safeHostileMobs.add(EntityType.HOGLIN,EntityType.PIGLIN,EntityType.STRIDER,EntityType.ZOGLIN,EntityType.ZOMBIFIED_PIGLIN);
					getLogger().info("1.16 mode enabled! Enjoy the plugin!");
				} else {
					getLogger().info("Legacy 1.15 mode enabled! Consider upgrading though <3");
				}
			} else {
				getLogger().info("Legacy 1.14 mode enabled! Consider upgrading though <3");
			}
		}

		this.hostileMobs = safeHostileMobs.build();
		this.neutralMobs = safeNeutralMobs.build();
		this.passiveMobs = safePassiveMobs.build();
		this.bossMobs = safeBossMobs.build();
	}

	@Override
	public void onEnable() {
		cacheMobs();
		registerListeners();
		this.saveDefaultConfig();
		mainConfig = new TradingCardsConfig(this);
		messagesConfig = new MessagesConfig(this);
		ConfigLoader.load(mainConfig);
		ConfigLoader.loadAndSave(messagesConfig);

		deckConfig = new SimpleConfig(this, "decks.yml");
		cardsConfig = new CardsConfig(this);

		deckConfig.saveDefaultConfig();
		//messagesConfig.saveDefaultConfig();
		cardsConfig.saveDefaultConfig();

		CardUtil.init(this);
		ChatUtil.init(this);
		CardManager.init(this);
		DeckManager.init(this);
		BukkitCommandManager commandManager = new BukkitCommandManager(this);
		commandManager.registerCommand(new CardsCommand(this));
		commandManager.registerCommand(new DeckCommand(this));
		commandManager.enableUnstableAPI("help");
		hookFileSystem();
		hookVault();

		if (this.getMainConfig().scheduleCards) {
			this.startTimer();
		}
	}


	@Override
	public void onDisable() {
		econ = null;
		TradingCards.perms = null;
		TradingCards.chat = null;
		this.getServer().getPluginManager().removePermission(permRarities);
	}

	private boolean setupEconomy() {
		if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		} else {
			RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
			if (rsp == null) {
				return false;
			} else {
				econ = rsp.getProvider();
				return econ != null;
			}
		}
	}

	public MessagesConfig getMessagesConfig() {
		return messagesConfig;
	}

	public boolean isMobHostile(EntityType e) {
		return this.hostileMobs.contains(e);
	}

	public boolean isMobNeutral(EntityType e) {
		return this.neutralMobs.contains(e);
	}

	public boolean isMobPassive(EntityType e) {
		return this.passiveMobs.contains(e);
	}

	public boolean isMobBoss(EntityType e) {
		return this.bossMobs.contains(e);
	}


	public int getCardID(String name, String rarity) {
		return (Integer) getDatabase("trading_cards").queryValue("SELECT id FROM cards WHERE name = '" + name + "' AND rarity = '" + rarity + "'", "ID");
	}

	public int getCardCount(String uuid, Integer deckNum, Integer cardID) {
		return (Integer) getDatabase("trading_cards").queryValue("SELECT count FROM decks WHERE uuid = '" + uuid + "' AND deckID = " + deckNum + " AND card = " + cardID + "", "ID");
	}

	public int getPlayerDeckFromCard(String uuid, Integer cardID) {
		return (Integer) getDatabase("trading_cards").queryValue("SELECT deckID FROM decks WHERE uuid = '" + uuid + "' AND card = " + cardID + "", "ID");
	}

	public boolean deleteCardSqlite(Player p, String card, String rarity) {
		int cardID = getCardID(card, rarity);
		String playerID = p.getUniqueId().toString();
		if (hasCard(p, card, rarity) == 1) {
			getDatabase("trading_cards").executeStatement("DELETE FROM decks WHERE uuid = '" + playerID + "' AND card=" + cardID + " LIMIT 1");
			return true;
		} else if (hasCard(p, card, rarity) > 1) {
			int cardCount = hasCard(p, card, rarity) - 1;
			getDatabase("trading_cards").executeStatement("UPDATE decks SET count = " + cardCount + " WHERE uuid = '" + playerID + "' AND card=" + cardID + " LIMIT 1");
			return true;
		} else return false;
	}

	@Deprecated
	public boolean deleteCard(Player p, String card, String rarity) {
		if (usingSqlite) return deleteCardSqlite(p, card, rarity);
		if (hasCard(p, card, rarity) > 0) {
			String uuidString = p.getUniqueId().toString();
			int deckNumber = 0;
			ConfigurationSection deckList = getDeckConfig().getConfig().getConfigurationSection("Decks.Inventories." + uuidString);
			if (deckList != null) for (String s : deckList.getKeys(false)) {
				deckNumber += Integer.parseInt(s);
				debug("Deck running total: " + deckNumber);
			}
			if (deckNumber == 0) {
				return true;
			}

			debug("Decks:" + deckNumber);
			for (int i = 0; i < deckNumber; i++) {
				if (getDeckConfig().getConfig().contains("Decks.Inventories." + uuidString + "." + (i + 1))) {
					List<String> contents = getDeckConfig().getConfig().getStringList("Decks.Inventories." + uuidString + "." + (i + 1));
					List<String> contentsNew = new ArrayList<>();
					for (String s2 : contents) {
						String[] splitContents = s2.split(",");
						if (getConfig().getBoolean("General.Eat-Shiny-Cards") && splitContents[3].equalsIgnoreCase("yes")) {
							debug("Eat-Shiny-Cards is true and card is shiny!");
							if (splitContents[0].equalsIgnoreCase(rarity)) {
								if (splitContents[1].equalsIgnoreCase(card)) {
									if (Integer.parseInt(splitContents[2]) <= 1) continue;
									int number = Integer.parseInt(splitContents[2]);
									splitContents[2] = String.valueOf(number - 1);
									StringBuilder strBuilder = new StringBuilder();
									for (final String splitContent : splitContents) {
										strBuilder.append(splitContent);
										strBuilder.append(",");
									}
									String newString = strBuilder.substring(0, strBuilder.length() - 1);
									contentsNew.add(newString);
									continue;
								}
								contentsNew.add(s2);
								continue;
							}
							contentsNew.add(s2);
							continue;
						}
						if (getConfig().getBoolean("General.Eat-Shiny-Cards") && splitContents[3].equalsIgnoreCase("no")) {
							debug("Eat-Shiny-Cards is true and card is not shiny!");
							if (splitContents[0].equalsIgnoreCase(rarity)) {
								if (splitContents[1].equalsIgnoreCase(card)) {
									if (Integer.parseInt(splitContents[2]) <= 1) continue;
									int number = Integer.parseInt(splitContents[2]);
									splitContents[2] = String.valueOf(number - 1);
									StringBuilder strBuilder = new StringBuilder();
									for (final String splitContent : splitContents) {
										strBuilder.append(splitContent);
										strBuilder.append(",");
									}
									String newString = strBuilder.substring(0, strBuilder.length() - 1);
									contentsNew.add(newString);
									continue;
								}
								contentsNew.add(s2);
								continue;
							}
							contentsNew.add(s2);
							continue;
						}
						if (!getConfig().getBoolean("General.Eat-Shiny-Cards") && splitContents[3].equalsIgnoreCase("yes")) {
							debug("Eat-Shiny-Cards is false and card is shiny!");
							if (!splitContents[0].equalsIgnoreCase(rarity)) continue;
							debug("Adding card..");
							contentsNew.add(s2);
							continue;
						}
						if (getConfig().getBoolean("General.Eat-Shiny-Cards") || !splitContents[3].equalsIgnoreCase("no"))
							continue;
						debug("Eat-Shiny-Cards is false and card is not shiny!");
						if (splitContents[0].equalsIgnoreCase(rarity)) {
							if (splitContents[1].equalsIgnoreCase(card)) {
								if (Integer.parseInt(splitContents[2]) <= 1) continue;
								int number = Integer.parseInt(splitContents[2]);
								splitContents[2] = String.valueOf(number - 1);
								StringBuilder strBuilder = new StringBuilder();
								for (final String splitContent : splitContents) {
									strBuilder.append(splitContent);
									strBuilder.append(",");
								}
								String newString = strBuilder.substring(0, strBuilder.length() - 1);
								contentsNew.add(newString);
								continue;
							}
							contentsNew.add(s2);
							continue;
						}
						contentsNew.add(s2);
					}
					getDeckConfig().getConfig().set("Decks.Inventories." + uuidString + "." + (i + 1), contentsNew);
					getDeckConfig().saveConfig();
					reloadAllConfig();
					contentsNew.clear();
				}
			}

		}
		return true;
	}

	@Deprecated
	//TODO Why doesn't this return a boolean?
	public int hasCard(Player p, String card, String rarity) {
		int deckNumber = 0;
		debug("Started check for card: " + card + ", " + rarity);

		String uuidString = p.getUniqueId().toString();
		ConfigurationSection deckList = getDeckConfig().getConfig().getConfigurationSection("Decks.Inventories." + uuidString);
		debug("Deck UUID: " + uuidString +",Card: "+ rarity+ " "+ card);


		if (deckList == null) {
			return 0;
		}

		debug(StringUtils.join(deckList.getKeys(false),","));
		HashSet<String> deck = new HashSet<>(deckList.getKeys(false));

		debug("Decks:" + deckNumber);

		for (int i = 0; i < deck.size(); ++i) {
			debug("Starting iteration " + i);

			if (getDeckConfig().getConfig().contains("Decks.Inventories." + uuidString + "." + (i + 1))) {
				List<String> contents = getDeckConfig().getConfig().getStringList("Decks.Inventories." + uuidString + "." + (i + 1));

				for (final String s2 : contents) {
					String[] splitContents = s2.split(",");
					debug("Deck file content: " + s2);
					debug(card + " - " + splitContents[1]);
					debug(rarity + " - " + splitContents[0]);

					if (splitContents[0].equalsIgnoreCase(rarity)) {
						debug("Rarity match: " + splitContents[0]);

						if (splitContents[1].equalsIgnoreCase(card) && splitContents[3].equalsIgnoreCase("no")) {
							debug("Card match: " + splitContents[1]);
							return Integer.parseInt(splitContents[2]);
						}
					}
				}
			}
		}

		return 0;
	}

	@Deprecated
	public boolean hasShiny(Player p, String card, String rarity) {
		int deckNumber = 0;
		debug("Started check for card: " + card + ", " + rarity);

		String uuidString = p.getUniqueId().toString();
		ConfigurationSection deckList = getDeckConfig().getConfig().getConfigurationSection("Decks.Inventories." + uuidString);
		debug("Deck UUID: " + uuidString);

		Iterator<String> var7;
		String s;

		if (deckList == null)
			return false;

		var7 = deckList.getKeys(false).iterator();

		while (var7.hasNext()) {
			s = var7.next();
			deckNumber += Integer.parseInt(s);
			debug("Deck running total: " + deckNumber);
		}

		if (deckNumber == 0) {
			return false;
		}
		debug("Decks:" + deckNumber);

		for (int i = 0; i < deckNumber; ++i) {
			if (getDeckConfig().getConfig().contains("Decks.Inventories." + uuidString + "." + (i + 1))) {
				List<String> contents = getDeckConfig().getConfig().getStringList("Decks.Inventories." + uuidString + "." + (i + 1));

				for (final String s2 : contents) {
					debug("Deck file content: " + s2);
					String[] splitContents = s2.split(",");
					debug(card + " - " + splitContents[1]);
					debug(rarity + " - " + splitContents[0]);

					if (splitContents[0].equalsIgnoreCase(rarity)) {
						debug("Rarity match: " + splitContents[0]);

						if (splitContents[1].equalsIgnoreCase(card)) {
							debug("Card match: " + splitContents[1]);
							if (splitContents[3].equalsIgnoreCase("yes")) {
								return true;
							}
						}
					}
				}
			}
		}

		return false;


	}

	@Deprecated
	public String isRarity(String input) {
		String output = input.substring(0, 1).toUpperCase() + input.substring(1);
		if (this.getConfig().contains("Rarities." + input.replaceAll("_", " "))) {
			return input.replaceAll("_", " ");
		} else if (this.getConfig().contains("Rarities." + input.replaceAll("_", " ").toUpperCase())) {
			return input.replaceAll("_", " ").toUpperCase();
		} else if (this.getConfig().contains("Rarities." + input.replaceAll("_", " ").toLowerCase())) {
			return input.replaceAll("_", " ").toLowerCase();
		} else if (this.getConfig().contains("Rarities." + output.replaceAll("_", " "))) {
			return output.replaceAll("_", " ");
		}

		return this.getConfig().contains("Rarities." + this.capitaliseUnderscores(input)) ? output.replaceAll("_", " ") : "None";
	}

	public String capitaliseUnderscores(String input) {
		String[] strArray = input.split("_");
		String[] finalArray = new String[strArray.length];
		StringBuilder finalized = new StringBuilder();

		for (int i = 0; i < strArray.length; ++i) {
			finalArray[i] = strArray[i].toLowerCase().substring(0, 1).toUpperCase() + strArray[i].substring(1);
			finalized.append(finalArray[i]);
			finalized.append("_");
		}

		return finalized.substring(0, finalized.length() - 1);
	}

	public boolean isMob(String input) {
		try {
			EntityType type = EntityType.valueOf(input.toUpperCase());
			return this.hostileMobs.contains(type) || this.neutralMobs.contains(type) || this.passiveMobs.contains(type) || this.bossMobs.contains(type);
		} catch (IllegalArgumentException var4) {
			return false;
		}
	}

	public boolean isOnList(Player p) {
		return getMainConfig().blacklistPlayers.contains(p.getName());
	}

	public void addToList(Player p) {
		List<String> playersOnList = this.getConfig().getStringList("Blacklist.Players");
		playersOnList.add(p.getName());
		this.getConfig().set("Blacklist.Players", null);
		this.getConfig().set("Blacklist.Players", playersOnList);
		this.saveConfig();
	}

	public void removeFromList(Player p) {
		List<String> playersOnList = this.getConfig().getStringList("Blacklist.Players");
		playersOnList.remove(p.getName());
		this.getConfig().set("Blacklist.Players", null);
		this.getConfig().set("Blacklist.Players", playersOnList);
		this.saveConfig();
	}

	public char blacklistMode() {
		return (this.getConfig().getBoolean("Blacklist.Whitelist-Mode") ? 'w' : 'b');
	}

	public List<String> wrapString(@NotNull String s) {
		String parsedString = ChatColor.stripColor(s);
		String addedString = WordUtils.wrap(parsedString, this.getConfig().getInt("General.Info-Line-Length", 25), "\n", true);
		String[] splitString = addedString.split("\n");
		List<String> finalArray = new ArrayList<>();

		for (String ss : splitString) {
			debug(ChatColor.getLastColors(ss));
			finalArray.add(this.cMsg("&f &7- &f" + ss));
		}

		return finalArray;
	}

	public void debug(final String message) {
		if (getConfig().getBoolean("General.Debug-Mode")) {
			getLogger().info("DEBUG " + message);
		}
	}

	public String getPrefixedMessage(final String message) {
		return cMsg(messagesConfig.prefix+ "&r " + message);
	}

	public void giveawayNatural(EntityType mob, Player sender) {
		if (this.isMobBoss(mob)) {
			if (sender == null) {
				Bukkit.broadcastMessage(getPrefixedMessage(messagesConfig.giveawayNaturalBossNoPlayer));
			} else {
				Bukkit.broadcastMessage(getPrefixedMessage(messagesConfig.giveawayNaturalBoss.replaceAll("%player%", sender.getName())));
			}
		} else if (this.isMobHostile(mob)) {
			if (sender == null) {
				Bukkit.broadcastMessage(getPrefixedMessage(messagesConfig.giveawayNaturalHostileNoPlayer));
			} else {
				Bukkit.broadcastMessage(getPrefixedMessage(messagesConfig.giveawayNaturalHostile.replaceAll("%player%", sender.getName())));
			}
		} else if (this.isMobNeutral(mob)) {
			if (sender == null) {
				Bukkit.broadcastMessage(getPrefixedMessage(messagesConfig.giveawayNaturalNeutralNoPlayer));
			} else {
				Bukkit.broadcastMessage(getPrefixedMessage(messagesConfig.giveawayNaturalNeutral.replaceAll("%player%", sender.getName())));
			}
		} else if (this.isMobPassive(mob)) {
			if (sender == null) {
				Bukkit.broadcastMessage(getPrefixedMessage(messagesConfig.giveawayNaturalPassiveNoPlayer));
			} else {
				Bukkit.broadcastMessage(getPrefixedMessage(messagesConfig.giveawayNaturalPassive.replaceAll("%player%", sender.getName())));
			}
		} else if (sender == null) {
			Bukkit.broadcastMessage(getPrefixedMessage(messagesConfig.giveawayNaturalNoPlayer));
		} else {
			Bukkit.broadcastMessage(getPrefixedMessage(messagesConfig.giveawayNatural.replaceAll("%player%", sender.getName())));
		}

		for (final Player p : Bukkit.getOnlinePlayers()) {
			String rare = CardUtil.calculateRarity(mob, true);
			debug("onCommand.rare: " + rare);
			CardUtil.dropItem(p, CardUtil.getRandomCard(rare, false));
		}

	}

	public void reloadAllConfig() {
		/*File file = new File(this.getDataFolder() + File.separator + "config-old.yml");
		if (!file.exists()) {
			this.getConfig().options().copyDefaults(true);
			this.saveDefaultConfig();
		}*/

		ConfigLoader.loadAndSave(mainConfig);
		//this.reloadConfig();
		this.deckConfig.reloadConfig();
		ConfigLoader.loadAndSave(messagesConfig);
		//this.messagesConfig.reloadConfig();
		this.cardsConfig.reloadConfig();
	}

	public static void sendMessage(final CommandSender toWhom, final String message) {
		toWhom.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
	}

	public boolean completedRarity(Player p, String rarity) {
		if ("None".equals(isRarity(rarity))) {
			return false;
		}

		ConfigurationSection cards = getCardsConfig().getConfig().getConfigurationSection("Cards." + this.isRarity(rarity));
		Set<String> cardKeys = cards.getKeys(false);
		int i = 0;
		int numCardsCounter = 0;

		for (Iterator<String> var7 = cardKeys.iterator(); var7.hasNext(); ++i) {
			String key = var7.next();
			debug("Iteration:: " + i);
			debug("Key: " + key);
			debug("Counter: " + numCardsCounter);

			boolean shinyVersion = false;
			boolean regularVersion = false;
			if (this.hasShiny(p, key, this.isRarity(rarity))) {
				shinyVersion = true;
			}

			if (this.hasCard(p, key, this.isRarity(rarity)) > 0) {
				regularVersion = true;
			}

			if (regularVersion || shinyVersion) {
				++numCardsCounter;
			}
		}

		return numCardsCounter >= i;

	}

	@Deprecated
	public boolean deleteRarity(Player p, String rarity) {
		return getCardsConfig().deleteRarity(p, rarity);
	}

	public String cMsg(String message) {
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	public void startTimer() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		if (scheduler.isQueued(this.taskid) || scheduler.isCurrentlyRunning(this.taskid)) {
			scheduler.cancelTask(this.taskid);
			debug("Successfully cancelled task " + this.taskid);
		}

		int hours = Math.max(this.getConfig().getInt("General.Schedule-Card-Time-In-Hours"), 1);

		String tmessage = messagesConfig.timerMessage.replaceAll("%hour%", String.valueOf(hours));
		Bukkit.broadcastMessage(getPrefixedMessage(tmessage));
		this.taskid = new CardSchedulerRunnable(this).runTaskTimer(this, (hours * 20 * 60 * 60), (hours * 20 * 60 * 60)).getTaskId();
	}

}