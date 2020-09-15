package media.xen.tradingcards;

import co.aikar.commands.BukkitCommandManager;
import com.garbagemule.MobArena.MobArena;
import com.garbagemule.MobArena.framework.ArenaMaster;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import media.xen.tradingcards.db.Database;
import media.xen.tradingcards.db.SQLite;
import media.xen.tradingcards.listeners.AddOnJoinListener;
import media.xen.tradingcards.listeners.DeckListener;
import media.xen.tradingcards.listeners.MobSpawnListener;
import media.xen.tradingcards.listeners.PackListener;
import media.xen.tradingcards.listeners.DropListener;
import media.xen.tradingcards.listeners.MobArenaListener;
import media.xen.tradingcards.listeners.MythicMobsListener;
import media.xen.tradingcards.listeners.TownyListener;
import media.xen.tradingcards.managers.CardManager;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

public class TradingCards extends JavaPlugin implements Listener, CommandExecutor {
	List<EntityType> hostileMobs = new ArrayList<>();
	List<EntityType> passiveMobs = new ArrayList<>();
	List<EntityType> neutralMobs = new ArrayList<>();
	List<EntityType> bossMobs = new ArrayList<>();
	private final Map<String, Database> databases = new HashMap<>();
	public static Permission permRarities = new Permission("cards.rarity");
	public ArenaMaster am;
	boolean hasVault;
	public boolean hasMobArena;
	private SimpleConfig deckConfig;
	private SimpleConfig messagesConfig;
	private SimpleConfig cardsConfig;
	private boolean usingSqlite;
	private boolean hasMythicMobs;


	public SimpleConfig getDeckConfig() {
		return deckConfig;
	}

	public SimpleConfig getMessagesConfig() {
		return messagesConfig;
	}

	public SimpleConfig getCardsConfig() {
		return cardsConfig;
	}


	public static Economy econ = null;
	public static Permission perms = null;
	public static Chat chat = null;
	public Random r = new Random();
	int taskid;

	private void hookTowny() {
		if (this.getConfig().getBoolean("PluginSupport.Towny.Towny-Enabled")) {
			if (this.getServer().getPluginManager().getPlugin("Towny") != null) {
				this.getServer().getPluginManager().registerEvents(new TownyListener(this), this);
				getLogger().info("Towny successfully hooked!");
			} else {
				getLogger().warning("Towny not found, hook unsuccessful!");
			}
		}
	}


	private void hookMythicMobs() {
		PluginManager pm;
		if (this.getConfig().getBoolean("PluginSupport.MythicMobs.MythicMobs-Enabled")) {
			if (this.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
				pm = this.getServer().getPluginManager();
				pm.registerEvents(new MythicMobsListener(this), this);
				getLogger().info("MythicMobs hook successful!");
				this.hasMythicMobs = true;
			} else {
				getLogger().info("MythicMobs not found, hook unsuccessful!");

			}
		}
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

	public void convertToDb() {
		ConfigurationSection cards = getCardsConfig().getConfig().getConfigurationSection("Cards");
		Set<String> cardKeys = cards.getKeys(false);
		Iterator<String> var3 = cardKeys.iterator();

		String series;
		String type;
		while (var3.hasNext()) {
			String key = var3.next();
			ConfigurationSection cardsWithKey = getCardsConfig().getConfig().getConfigurationSection("Cards." + key);
			Set<String> keyKeys = cardsWithKey.getKeys(false);

			for (final String key2 : keyKeys) {
				String cost = "None";
				series = getCardsConfig().getConfig().getString("Cards." + key + "." + key2 + ".Series");
				String about = getCardsConfig().getConfig().getString("Cards." + key + "." + key2 + ".About", "None");
				type = getCardsConfig().getConfig().getString("Cards." + key + "." + key2 + ".Type");
				String info = getCardsConfig().getConfig().getString("Cards." + key + "." + key2 + ".Info");
				if (getCardsConfig().getConfig().contains("Cards." + key + "." + key2 + ".Buy-Price")) {
					cost = String.valueOf(getCardsConfig().getConfig().getDouble("Cards." + key + "." + key2 + ".Buy-Price"));
				}

				if (!this.exists("SELECT * FROM cards WHERE rarity = '" + key + "' AND name = '" + key2 + "' AND about = '" + about + "' AND series = '" + series + "' AND type = '" + type + "' AND info = '" + info + "' AND price = '" + cost + "'")) {
					if (this.getDatabase("trading_cards").executeStatement("INSERT INTO cards (rarity, name, about, series, type, info, price) VALUES ('" + key + "', '" + key2 + "', '" + about + "', '" + series + "', '" + type + "', '" + info + "', '" + cost + "')")) {
						debug(key + ", " + key2 + " - Added to SQLite!");
					} else {
						debug(key + ", " + key2 + " - Unable to be added!");
					}
				}
			}
		}

		ConfigurationSection decks = getDeckConfig().getConfig().getConfigurationSection("Decks.Inventories");
		Set<String> deckKeys = decks.getKeys(false);
		int deckNum = 0;
		Iterator<String> var18 = deckKeys.iterator();
		String s;

		while (var18.hasNext()) {
			String key = var18.next();
			debug("Deck key is: " + key);

			ConfigurationSection deckList = getDeckConfig().getConfig().getConfigurationSection("Decks.Inventories." + key);
			if (deckList != null) {

				for (final String value : deckList.getKeys(false)) {
					s = value;
					deckNum += Integer.parseInt(s);

					debug("Deck running total: " + deckNum);
				}
			}

			if (deckNum == 0) {
				debug("No deck?!");
			} else {
				debug("Decks:" + deckNum);
				label127:
				for (int i = 0; i < deckNum; ++i) {
					List<String> contents = getDeckConfig().getConfig().getStringList("Decks.Inventories." + key + "." + deckNum);
					Iterator var24 = contents.iterator();

					while (true) {
						while (true) {
							String[] splitContents;
							do {
								if (!var24.hasNext()) {
									continue label127;
								}

								s = (String) var24.next();
								debug("Deck content: " + s);
								splitContents = s.split(",");
							} while (splitContents.length <= 1);

							if (splitContents[1] == null) {
								splitContents[1] = "None";
							}

							Integer cardID = (Integer) this.getDatabase("trading_cards").queryValue("SELECT id FROM cards WHERE name = '" + splitContents[1] + "' AND rarity = '" + splitContents[0] + "'", "ID");
							if (splitContents[3].equalsIgnoreCase("yes")) {
								if (!splitContents[0].equalsIgnoreCase("BLANK") && !splitContents[1].equalsIgnoreCase("None") && splitContents[1] != null && !splitContents[1].isEmpty() && this.getDatabase("trading_cards").queryValue("SELECT * FROM decks WHERE uuid = '" + key + "' AND deckID = '" + deckNum + "' AND card = '" + cardID + "' AND isShiny = 1", "ID") == null && !this.getDatabase("trading_cards").executeStatement("INSERT INTO decks (uuid, deckID, card, isShiny, count) VALUES ('" + key + "', '" + deckNum + "', '" + cardID + "', 1, " + Integer.valueOf(splitContents[2]) + ")") && this.getConfig().getBoolean("General.Debug-Mode")) {
									System.out.println("[Cards] Error adding shiny card to deck SQLite, check stack!");
								}
							} else if (!splitContents[1].equalsIgnoreCase("None") && !splitContents[0].equalsIgnoreCase("BLANK") && splitContents[1] != null && !splitContents[1].isEmpty()) {
								if (this.getDatabase("trading_cards").queryValue("SELECT * FROM decks WHERE uuid = '" + key + "' AND deckID = '" + deckNum + "' AND card = '" + cardID + "' AND isShiny = 0", "ID") == null && !this.getDatabase("trading_cards").executeStatement("INSERT INTO decks (uuid, deckID, card, isShiny, count) VALUES ('" + key + "', '" + deckNum + "', '" + cardID + "', 0, " + Integer.valueOf(splitContents[2]) + ")") && this.getConfig().getBoolean("General.Debug-Mode")) {
									System.out.println("[Cards] Error adding card to deck SQLite, check stack!");
								}
							} else {
								System.out.println("[Cards] Warning! A null card has been found in a deck. It was truncated for safety.");
							}
						}
					}
				}
			}
		}

	}

	private void hookMobArena() {
		if (this.getConfig().getBoolean("PluginSupport.MobArena.MobArena-Enabled")) {
			if (this.getServer().getPluginManager().getPlugin("MobArena") != null) {
				PluginManager pm = this.getServer().getPluginManager();
				MobArena maPlugin = (MobArena) pm.getPlugin("MobArena");
				this.am = maPlugin.getArenaMaster();
				pm.registerEvents(new MobArenaListener(this), this);
				getLogger().info("Mob Arena hook successful!");
				this.hasMobArena = true;
			} else {
				getLogger().info("Mob Arena not found, hook unsuccessful!");
			}
		}
	}

	private void hookFileSystem() {
		if (this.getConfig().getBoolean("General.SQLite")) {
			this.usingSqlite = true;
			this.initializeDatabase("trading_cards", "CREATE TABLE IF NOT EXISTS cards(`id` INTEGER NOT NULL PRIMARY KEY, `rarity` varchar(255), `about` varchar(255), `series` varchar(255), `name` varchar(255), `type` varchar(255), `info` varchar(255), `price` int); CREATE TABLE IF NOT EXISTS decks(`id` INTEGER NOT NULL PRIMARY KEY, `uuid` varchar(512), `deckID` int, `card` int, `isShiny` int, `count` int)");
			getLogger().info("SQLite is enabled");
			this.convertToDb();
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
		hookMythicMobs();
		hookMobArena();
		hookTowny();
	}

	@Override
	public void onEnable() {
		String serverVersion = Bukkit.getServer().getVersion();
		List<EntityType> safeHostileMobs = Arrays.asList(EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.BLAZE, EntityType.SILVERFISH, EntityType.GHAST, EntityType.SLIME, EntityType.EVOKER, EntityType.VINDICATOR, EntityType.VEX, EntityType.SHULKER, EntityType.GUARDIAN, EntityType.MAGMA_CUBE, EntityType.ELDER_GUARDIAN, EntityType.STRAY, EntityType.HUSK, EntityType.DROWNED, EntityType.WITCH, EntityType.ZOGLIN, EntityType.HOGLIN, EntityType.ZOMBIE_VILLAGER, EntityType.PILLAGER, EntityType.RAVAGER, EntityType.ENDERMITE);
		List<EntityType> safeNeutralMobs = Arrays.asList(EntityType.ENDERMAN, EntityType.POLAR_BEAR, EntityType.LLAMA, EntityType.WOLF, EntityType.BEE, EntityType.DOLPHIN, EntityType.PANDA, EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.DOLPHIN, EntityType.SNOWMAN, EntityType.IRON_GOLEM);
		List<EntityType> safePassiveMobs = Arrays.asList(EntityType.DONKEY, EntityType.MULE, EntityType.SKELETON_HORSE, EntityType.CHICKEN, EntityType.COW, EntityType.SQUID, EntityType.WANDERING_TRADER, EntityType.TURTLE, EntityType.STRIDER, EntityType.TROPICAL_FISH, EntityType.PUFFERFISH, EntityType.SHEEP, EntityType.PIG, EntityType.PHANTOM, EntityType.SALMON, EntityType.COD, EntityType.RABBIT, EntityType.VILLAGER, EntityType.BAT, EntityType.PARROT, EntityType.HORSE);
		List<EntityType> safeBossMobs = Arrays.asList(EntityType.ENDER_DRAGON, EntityType.WITHER);
		this.hostileMobs.addAll(safeHostileMobs);
		this.neutralMobs.addAll(safeNeutralMobs);
		this.passiveMobs.addAll(safePassiveMobs);
		this.bossMobs.addAll(safeBossMobs);
		// Previous version compatibility
		if (serverVersion.contains("1.14") || serverVersion.contains("1.15") || serverVersion.contains("1.16")) {
			this.neutralMobs.add(EntityType.PANDA);
			this.hostileMobs.add(EntityType.PILLAGER);
			this.hostileMobs.add(EntityType.RAVAGER);
			this.passiveMobs.add(EntityType.WANDERING_TRADER);
			this.neutralMobs.add(EntityType.FOX);
			this.passiveMobs.add(EntityType.CAT);
			this.passiveMobs.add(EntityType.MUSHROOM_COW);
			this.passiveMobs.add(EntityType.TRADER_LLAMA);
			if (serverVersion.contains("1.15") || serverVersion.contains("1.16")) {
				this.neutralMobs.add(EntityType.BEE);
				if (serverVersion.contains("1.16")) {
					this.hostileMobs.add(EntityType.HOGLIN);
					this.hostileMobs.add(EntityType.PIGLIN);
					this.hostileMobs.add(EntityType.STRIDER);
					this.hostileMobs.add(EntityType.ZOGLIN);
					this.hostileMobs.add(EntityType.ZOMBIFIED_PIGLIN);
					getLogger().info("1.16 mode enabled! Enjoy the plugin!");
				} else {
					getLogger().info("Legacy 1.15 mode enabled! Consider upgrading though <3");
				}
			} else {
				getLogger().info("Legacy 1.14 mode enabled! Consider upgrading though <3");
			}
		}
		registerListeners();
		this.saveDefaultConfig();
		deckConfig = new SimpleConfig(this, "decks.yml");
		messagesConfig = new SimpleConfig(this, "messages.yml");
		cardsConfig = new SimpleConfig(this, "cards.yml");

		deckConfig.saveDefaultConfig();
		messagesConfig.saveDefaultConfig();
		cardsConfig.saveDefaultConfig();

		new CardUtil(this);
		new CardManager(this);
		BukkitCommandManager commandManager = new BukkitCommandManager(this);
		commandManager.registerCommand(new CardsCommand(this));
		commandManager.enableUnstableAPI("help");
		hookFileSystem();
		hookVault();

		if (this.getConfig().getBoolean("General.Schedule-Cards")) {
			this.startTimer();
		}
	}


	@Override
	public void onDisable() {
		econ = null;
		perms = null;
		chat = null;
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
				econ = (Economy) rsp.getProvider();
				return econ != null;
			}
		}
	}

	public String formatDouble(double value) {
		NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
		return nf.format(value);
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

	public ItemStack getBlankCard(int quantity) {
		return new ItemStack(Material.getMaterial(this.getConfig().getString("General.Card-Material")), quantity);
	}

	public ItemStack getBlankBoosterPack() {
		return new ItemStack(Material.getMaterial(this.getConfig().getString("General.BoosterPack-Material")));
	}

	public ItemStack getBlankDeck() {
		return new ItemStack(Material.getMaterial(this.getConfig().getString("General.Deck-Material")));
	}

	public ItemStack createDeck(Player p, int num) {
		ItemStack deck = this.getBlankDeck();
		ItemMeta deckMeta = deck.getItemMeta();
		deckMeta.setDisplayName(this.cMsg(this.getConfig().getString("General.Deck-Prefix") + p.getName() + "'s Deck #" + num));
		if (this.getConfig().getBoolean("General.Hide-Enchants", true)) {
			deckMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}

		deck.setItemMeta(deckMeta);
		deck.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
		return deck;
	}

	public boolean hasDeck(Player p, int num) {
		for (final ItemStack i : p.getInventory()) {
			if (i != null && i.getType() == Material.valueOf(this.getConfig().getString("General.Deck-Material")) && i.containsEnchantment(Enchantment.DURABILITY) && i.getEnchantmentLevel(Enchantment.DURABILITY) == 10) {
				String name = i.getItemMeta().getDisplayName();
				String[] splitName = name.split("#");
				if (num == Integer.parseInt(splitName[1])) {
					return true;
				}
			}
		}

		return false;
	}

	public int getCardID(String name, String rarity) {
		Integer cardID = (Integer) getDatabase("trading_cards").queryValue("SELECT id FROM cards WHERE name = '" + name + "' AND rarity = '" + rarity + "'", "ID");
		return cardID;
	}

	public int getCardCount(String uuid, Integer deckNum, Integer cardID) {
		Integer cardCount = (Integer) getDatabase("trading_cards").queryValue("SELECT count FROM decks WHERE uuid = '" + uuid + "' AND deckID = " + deckNum + " AND card = " + cardID + "", "ID");
		return cardCount;
	}

	public int getPlayerDeckFromCard(String uuid, Integer cardID) {
		Integer deckID = (Integer) getDatabase("trading_cards").queryValue("SELECT deckID FROM decks WHERE uuid = '" + uuid + "' AND card = " + cardID + "", "ID");
		return deckID;
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

	public boolean deleteCard(Player p, String card, String rarity) {
		if (usingSqlite) return deleteCardSqlite(p, card, rarity);
		if (hasCard(p, card, rarity) > 0) {
			String uuidString = p.getUniqueId().toString();
			int deckNumber = 0;
			ConfigurationSection deckList = getDeckConfig().getConfig().getConfigurationSection("Decks.Inventories." + uuidString);
			if (deckList != null) for (String s : deckList.getKeys(false)) {
				deckNumber += Integer.valueOf(s);
				debug("Deck running total: " + deckNumber);
			}
			if (deckNumber == 0) {
				debug("No decks?!");
			} else {
				debug("[TradingCards] Decks:" + deckNumber);
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
										if (Integer.valueOf(splitContents[2]).intValue() <= 1) continue;
										int number = Integer.valueOf(splitContents[2]).intValue();
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
									if (Integer.valueOf(splitContents[2]).intValue() <= 1) continue;
									int number = Integer.valueOf(splitContents[2]).intValue();
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
		}
		return true;
	}

	public int hasCard(Player p, String card, String rarity) {
		int deckNumber = 0;
		debug("Started check for card: " + card + ", " + rarity);

		String uuidString = p.getUniqueId().toString();
		ConfigurationSection deckList = getDeckConfig().getConfig().getConfigurationSection("Decks.Inventories." + uuidString);
		debug("Deck UUID: " + uuidString);


		if (getDeckConfig().getConfig().contains("Decks.Inventories." + uuidString)) {
			debug("Deck.yml contains player!");
		}

		Iterator<String> var7;
		String s;
		if (this.getConfig().getBoolean("General.Debug-Mode")) {
			var7 = deckList.getKeys(false).iterator();

			while (var7.hasNext()) {
				s = var7.next();
				getLogger().info("Deck rarity content: " + s);
			}
			debug("Done!");
		}

		if (deckList == null) {
			return 0;
		}

		var7 = deckList.getKeys(false).iterator();

		while (var7.hasNext()) {
			s = var7.next();
			deckNumber += Integer.parseInt(s);
			debug("[Deck running total: " + deckNumber);
		}

		if (deckNumber == 0) {
			debug("No decks?!");
			return 0;
		}

		debug("Decks:" + deckNumber);

		for (int i = 0; i < deckNumber; ++i) {
			debug("Starting iteration " + i);

			if (getDeckConfig().getConfig().contains("Decks.Inventories." + uuidString + "." + (i + 1))) {
				List<String> contents = getDeckConfig().getConfig().getStringList("Decks.Inventories." + uuidString + "." + (i + 1));

				for (final String s2 : contents) {
					String[] splitContents = s2.split(",");
					debug("Deck file content: " + s2);
					debug(card + " - " + splitContents[1]);
					debug(rarity + " - " + splitContents[0]);

					if (splitContents[0].equalsIgnoreCase(rarity)) {
						if (this.getConfig().getBoolean("General.Debug-Mode")) {
							System.out.println("[Cards] Rarity match: " + splitContents[0]);
						}

						if (splitContents[1].equalsIgnoreCase(card) && splitContents[3].equalsIgnoreCase("no")) {
							if (this.getConfig().getBoolean("General.Debug-Mode")) {
								System.out.println("[Cards] Card match: " + splitContents[1]);
							}

							return Integer.parseInt(splitContents[2]);
						}
					}
				}
			}
		}

		return 0;


	}

	public boolean hasShiny(Player p, String card, String rarity) {
		int deckNumber = 0;
		debug("Started check for card: " + card + ", " + rarity);

		String uuidString = p.getUniqueId().toString();
		ConfigurationSection deckList = getDeckConfig().getConfig().getConfigurationSection("Decks.Inventories." + uuidString);
		debug("[Cards] Deck UUID: " + uuidString);

		Iterator var7;
		String s;
		debug(StringUtils.join(deckList.getKeys(false), "Deck rarity content:"));


		if (deckList == null)
			return false;

		var7 = deckList.getKeys(false).iterator();

		while (var7.hasNext()) {
			s = (String) var7.next();
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
					if (this.getConfig().getBoolean("General.Debug-Mode")) {
						System.out.println("[Cards] Deck file content: " + s2);
					}

					String[] splitContents = s2.split(",");
					if (this.getConfig().getBoolean("General.Debug-Mode")) {
						System.out.println("[Cards] " + card + " - " + splitContents[1]);
					}

					if (this.getConfig().getBoolean("General.Debug-Mode")) {
						System.out.println("[Cards] " + rarity + " - " + splitContents[0]);
					}

					if (splitContents[0].equalsIgnoreCase(rarity)) {
						debug("[Cards] Rarity match: " + splitContents[0]);

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

	public void openDeck(Player p, int deckNum) {
		debug("Deck opened");
		String uuidString = p.getUniqueId().toString();
		debug("Deck UUID: " + uuidString);

		List<String> contents = getDeckConfig().getConfig().getStringList("Decks.Inventories." + uuidString + "." + deckNum);
		List<ItemStack> cards = new ArrayList<>();
		List<Integer> quantity = new ArrayList<>();
		ItemStack card = null;
		boolean isNull = false;

		for (final String s : contents) {
			if (this.getConfig().getBoolean("General.Debug-Mode")) {
				System.out.println("[Cards] Deck file content: " + s);
			}

			String[] splitContents = s.split(",");
			if (splitContents.length > 1) {
				if (splitContents[1] == null) {
					splitContents[1] = "None";
				}

				if (splitContents[3].equalsIgnoreCase("yes")) {
					if (!splitContents[0].equalsIgnoreCase("BLANK") && !splitContents[1].equalsIgnoreCase("None") && splitContents[1] != null && !splitContents[1].isEmpty()) {
						card = this.createPlayerCard(splitContents[1], splitContents[0], Integer.valueOf(splitContents[2]), true);
					} else {
						getLogger().warning("A null card has been found in a deck. It was truncated for safety.");
						isNull = true;
					}
				} else if (!splitContents[1].equalsIgnoreCase("None") && !splitContents[0].equalsIgnoreCase("BLANK") && splitContents[1] != null && !splitContents[1].isEmpty()) {
					card = this.getNormalCard(splitContents[1], splitContents[0], Integer.parseInt(splitContents[2]));
				} else {
					getLogger().warning("A null card has been found in a deck. It was truncated for safety.");
					isNull = true;
				}
			}

			if (!isNull) {
				cards.add(card);
			}

			if (splitContents.length > 1) {
				quantity.add(Integer.valueOf(splitContents[2]));
			} else {
				quantity.add(1);
			}

			isNull = false;
			if (splitContents.length > 1) {
				debug("Put " + card + "," + splitContents[2] + " into respective lists.");
			} else {
				debug("Put spacer into list.");
			}
		}

		int invSlots = 27;
		if (this.getConfig().getBoolean("General.Use-Large-Decks")) {
			invSlots = 54;
		}

		Inventory inv = Bukkit.createInventory(null, invSlots, this.cMsg("&c" + p.getName() + "'s Deck #" + deckNum));
		if (this.getConfig().getBoolean("General.Debug-Mode")) {
			System.out.println("[Cards] Created inventory.");
		}

		int iter = 0;

		for (Iterator var12 = cards.iterator(); var12.hasNext(); ++iter) {
			ItemStack i = (ItemStack) var12.next();
			if (this.getConfig().getBoolean("General.Debug-Mode")) {
				System.out.println("[Cards] Item " + i.getType().toString() + " added to inventory!");
			}

			i.setAmount(quantity.get(iter));
			inv.addItem(i);
		}

		p.openInventory(inv);
	}

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

	@Deprecated
	public ItemStack createBoosterPack(String name) {
		ItemStack boosterPack = this.getBlankBoosterPack();
		int numNormalCards = this.getConfig().getInt("BoosterPacks." + name + ".NumNormalCards");
		int numSpecialCards = this.getConfig().getInt("BoosterPacks." + name + ".NumSpecialCards");
		String prefix = this.getConfig().getString("General.BoosterPack-Prefix");
		String normalCardColour = this.getConfig().getString("Colours.BoosterPackNormalCards");
		String extraCardColour = this.getConfig().getString("Colours.BoosterPackExtraCards");
		String loreColour = this.getConfig().getString("Colours.BoosterPackLore");
		String nameColour = this.getConfig().getString("Colours.BoosterPackName");
		String normalRarity = this.getConfig().getString("BoosterPacks." + name + ".NormalCardRarity");
		String specialRarity = this.getConfig().getString("BoosterPacks." + name + ".SpecialCardRarity");
		String extraRarity = "";
		int numExtraCards = 0;
		boolean hasExtraRarity = false;
		if (this.getConfig().contains("BoosterPacks." + name + ".ExtraCardRarity") && this.getConfig().contains("BoosterPacks." + name + ".NumExtraCards")) {
			hasExtraRarity = true;
			extraRarity = this.getConfig().getString("BoosterPacks." + name + ".ExtraCardRarity");
			numExtraCards = this.getConfig().getInt("BoosterPacks." + name + ".NumExtraCards");
		}

		String specialCardColour = this.getConfig().getString("Colours.BoosterPackSpecialCards");
		ItemMeta pMeta = boosterPack.getItemMeta();
		pMeta.setDisplayName(this.cMsg(prefix + nameColour + name.replaceAll("_", " ")));
		List<String> lore = new ArrayList<>();
		lore.add(this.cMsg(normalCardColour + numNormalCards + loreColour + " " + normalRarity.toUpperCase()));
		if (hasExtraRarity) {
			lore.add(this.cMsg(extraCardColour + numExtraCards + loreColour + " " + extraRarity.toUpperCase()));
		}

		lore.add(this.cMsg(specialCardColour + numSpecialCards + loreColour + " " + specialRarity.toUpperCase()));
		pMeta.setLore(lore);
		if (this.getConfig().getBoolean("General.Hide-Enchants", true)) {
			pMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}

		boosterPack.setItemMeta(pMeta);
		boosterPack.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 10);
		return boosterPack;
	}

	public String calculateRarity(EntityType e, boolean alwaysDrop) {
		int shouldItDrop = this.r.nextInt(100) + 1;
		boolean bossRarity = false;
		String type = "";
		debug("shouldItDrop Num: " + shouldItDrop);

		if (this.isMobHostile(e)) {
			if (!alwaysDrop) {
				if (shouldItDrop > this.getConfig().getInt("Chances.Hostile-Chance")) {
					return "None";
				}
			}
			type = "Hostile";
		} else if (this.isMobNeutral(e)) {
			if (!alwaysDrop) {
				if (shouldItDrop > this.getConfig().getInt("Chances.Neutral-Chance")) {
					return "None";
				}

			}
			type = "Neutral";
		} else if (this.isMobPassive(e)) {
			if (!alwaysDrop) {
				if (shouldItDrop > this.getConfig().getInt("Chances.Passive-Chance")) {
					return "None";
				}

			}
			type = "Passive";
		} else {
			if (!this.isMobBoss(e)) {
				return "None";
			}

			if (!alwaysDrop) {
				if (shouldItDrop > this.getConfig().getInt("Chances.Boss-Chance")) {
					return "None";
				}

				if (this.getConfig().getBoolean("Chances.Boss-Drop")) {
					int var16 = this.getConfig().getInt("Chances.Boss-Drop-Rarity");
				}

			}
			type = "Boss";
		}

		ConfigurationSection rarities = this.getConfig().getConfigurationSection("Rarities");
		Set<String> rarityKeys = rarities.getKeys(false);
		Map<String, Integer> rarityChances = new HashMap<>();
		Map<Integer, String> rarityIndexes = new HashMap<>();
		int i = 0;
		int mini = 0;
		int random = this.r.nextInt(100000) + 1;
		if (this.getConfig().getBoolean("General.Debug-Mode")) {
			System.out.println("[Cards] Random Card Num: " + random);
		}

		if (this.getConfig().getBoolean("General.Debug-Mode")) {
			System.out.println("[Cards] Type: " + type);
		}

		String key;
		int chance;
		for (Iterator var13 = rarityKeys.iterator(); var13.hasNext(); rarityChances.put(key, chance)) {
			key = (String) var13.next();
			rarityIndexes.put(i, key);
			++i;
			if (this.getConfig().getBoolean("General.Debug-Mode")) {
				System.out.println("[Cards] " + i + ", " + key);
			}

			if (this.getConfig().contains("Chances." + key + "." + StringUtils.capitalize(e.getKey().getKey())) && mini == 0) {
				if (this.getConfig().getBoolean("General.Debug-Mode")) {
					System.out.println("[Cards] Mini: " + i);
				}

				mini = i;
			}

			chance = this.getConfig().getInt("Chances." + key + "." + type, -1);
			if (this.getConfig().getBoolean("General.Debug-Mode")) {
				System.out.println("[Cards] Keys: " + key + ", " + chance + ", i=" + i);
			}
		}

		if (mini != 0) {
			if (this.getConfig().getBoolean("General.Debug-Mode")) {
				System.out.println("[Cards] Mini: " + mini);
			}

			if (this.getConfig().getBoolean("General.Debug-Mode")) {
				System.out.println("[Cards] i: " + i);
			}

			while (i >= mini) {
				--i;
				if (this.getConfig().getBoolean("General.Debug-Mode")) {
					System.out.println("[Cards] i: " + i);
				}

				chance = this.getConfig().getInt("Chances." + rarityIndexes.get(i) + "." + StringUtils.capitalize(e.getKey().getKey()), -1);
				if (this.getConfig().getBoolean("General.Debug-Mode")) {
					System.out.println("[Cards] Chance: " + chance);
				}

				if (this.getConfig().getBoolean("General.Debug-Mode")) {
					System.out.println("[Cards] Rarity: " + rarityIndexes.get(i));
				}

				if (chance > 0) {
					if (this.getConfig().getBoolean("General.Debug-Mode")) {
						System.out.println("[Cards] Chance > 0");
					}

					if (random <= chance) {
						if (this.getConfig().getBoolean("General.Debug-Mode")) {
							System.out.println("[Cards] Random <= Chance");
						}

						return rarityIndexes.get(i);
					}
				}
			}
		} else {
			while (i > 0) {
				--i;
				if (this.getConfig().getBoolean("General.Debug-Mode")) {
					System.out.println("[Cards] Final loop iteration " + i);
				}

				if (this.getConfig().getBoolean("General.Debug-Mode")) {
					System.out.println("[Cards] Iteration " + i + " in HashMap is: " + rarityIndexes.get(i) + ", " + this.getConfig().getString("Rarities." + (String) rarityIndexes.get(i) + ".Name"));
				}

				chance = this.getConfig().getInt("Chances." + rarityIndexes.get(i) + "." + type, -1);
				if (this.getConfig().getBoolean("General.Debug-Mode")) {
					System.out.println("[Cards] " + this.getConfig().getString("Rarities." + rarityIndexes.get(i) + ".Name") + "'s chance of dropping: " + chance + " out of 100,000");
				}

				if (this.getConfig().getBoolean("General.Debug-Mode")) {
					System.out.println("[Cards] The random number we're comparing that against is: " + random);
				}

				if (chance > 0 && random <= chance) {
					if (this.getConfig().getBoolean("General.Debug-Mode")) {
						System.out.println("[Cards] Yup, looks like " + random + " is definitely lower than " + chance + "!");
					}

					if (this.getConfig().getBoolean("General.Debug-Mode")) {
						System.out.println("[Cards] Giving a " + this.getConfig().getString("Rarities." + (String) rarityIndexes.get(i) + ".Name") + " card.");
					}

					return rarityIndexes.get(i);
				}
			}
		}

		return "None";
	}

	public boolean isOnList(Player p) {
		List<String> playersOnList = this.getConfig().getStringList("Blacklist.Players");
		return playersOnList.contains(p.getName());
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

	@Deprecated
	/**
	 * @deprecated Use {@link CardUtil#generateRandomCard(String, String, boolean)}
	 */
	public ItemStack generateRandomCard(String rare, boolean forcedShiny) {
		return CardUtil.generateRandomCard(rare, forcedShiny);
	}

	public List<String> wrapString(@NotNull String s) {
		String parsedString = ChatColor.stripColor(s);
		String addedString = WordUtils.wrap(parsedString, this.getConfig().getInt("General.Info-Line-Length", 25), "\n", true);
		String[] splitString = addedString.split("\n");
		List<String> finalArray = new ArrayList<>();
		String[] arrayOfString1 = splitString;
		int j = splitString.length;

		for (int i = 0; i < j; ++i) {
			String ss = arrayOfString1[i];
			debug(ChatColor.getLastColors(ss));
			finalArray.add(this.cMsg("&f &7- &f" + ss));
		}

		return finalArray;
	}

	public String[] splitStringEvery(String s, int interval) {
		int arrayLength = s.length() / interval;
		String[] result = new String[arrayLength];
		int j = 0;
		int lastIndex = result.length - 1;

		for (int i = 0; i < lastIndex; ++i) {
			result[i] = s.substring(j, j + interval);
			j += interval;
		}

		result[lastIndex] = s.substring(j);
		return result;
	}


	public void debug(final String message) {
		if (getConfig().getBoolean("General.Debug-Mode")) {
			getLogger().info("DEBUG " + message);
		}
	}

	@Deprecated
	public ItemStack createPlayerCard(String cardName, String rarity, Integer num, boolean forcedShiny) {
		ItemStack card = CardUtil.generateCard(cardName, rarity, forcedShiny);
		card.setAmount(num);
		return card;
	}

	@Deprecated
	public ItemStack getNormalCard(String cardName, String rarity, int num) {
		ItemStack card = CardUtil.generateCard(cardName, rarity, false);
		card.setAmount(num);
		return card;
	}


	public String getPrefixedMessage(final String message) {
		return cMsg(getMessagesConfig().getConfig().getString("Messages.Prefix") + "&r " + message);
	}

	public void giveawayNatural(EntityType mob, Player sender) {
		if (this.isMobBoss(mob)) {
			if (sender == null) {
				Bukkit.broadcastMessage(getPrefixedMessage(getMessagesConfig().getConfig().getString("Messages.GiveawayNaturalBossNoPlayer")));
			} else {
				Bukkit.broadcastMessage(getPrefixedMessage(getMessagesConfig().getConfig().getString("Messages.GiveawayNaturalBoss").replaceAll("%player%", sender.getName())));
			}
		} else if (this.isMobHostile(mob)) {
			if (sender == null) {
				Bukkit.broadcastMessage(getPrefixedMessage(getMessagesConfig().getConfig().getString("Messages.GiveawayNaturalHostileNoPlayer")));
			} else {
				Bukkit.broadcastMessage(getPrefixedMessage(getMessagesConfig().getConfig().getString("Messages.GiveawayNaturalHostile").replaceAll("%player%", sender.getName())));
			}
		} else if (this.isMobNeutral(mob)) {
			if (sender == null) {
				Bukkit.broadcastMessage(getPrefixedMessage(getMessagesConfig().getConfig().getString("Messages.GiveawayNaturalNeutralNoPlayer")));
			} else {
				Bukkit.broadcastMessage(getPrefixedMessage(getMessagesConfig().getConfig().getString("Messages.GiveawayNaturalNeutral").replaceAll("%player%", sender.getName())));
			}
		} else if (this.isMobPassive(mob)) {
			if (sender == null) {
				Bukkit.broadcastMessage(getPrefixedMessage(getMessagesConfig().getConfig().getString("Messages.GiveawayNaturalPassiveNoPlayer")));
			} else {
				Bukkit.broadcastMessage(getPrefixedMessage(getMessagesConfig().getConfig().getString("Messages.GiveawayNaturalPassive").replaceAll("%player%", sender.getName())));
			}
		} else if (sender == null) {
			Bukkit.broadcastMessage(getPrefixedMessage(getMessagesConfig().getConfig().getString("Messages.GiveawayNaturalNoPlayer")));
		} else {
			Bukkit.broadcastMessage(getPrefixedMessage(getMessagesConfig().getConfig().getString("Messages.GiveawayNatural").replaceAll("%player%", sender.getName())));
		}

		for (final Player p : Bukkit.getOnlinePlayers()) {
			String rare = this.calculateRarity(mob, true);
			debug("onCommand.rare: " + rare);
			CardUtil.dropItem(p, CardUtil.getRandomCard(rare, false));
		}

	}

	private final String nameTemplate = "^[a-zA-Z0-9-_]+$";

	public void createCard(Player creator, String rarity, String name, String series, String type, boolean hasShiny, String info, String about) {
		if (!getCardsConfig().getConfig().contains("Cards." + rarity + "." + name)) {
			if (name.matches(nameTemplate)) {
				if (this.isPlayerCard(name)) {
					name = name.replaceAll(" ", "_");
				}

				ConfigurationSection rarities = getCardsConfig().getConfig().getConfigurationSection("Cards");
				Set<String> rarityKeys = rarities.getKeys(false);
				String keyToUse = "";
				Iterator var12 = rarityKeys.iterator();

				String type2;
				while (var12.hasNext()) {
					type2 = (String) var12.next();
					if (type2.equalsIgnoreCase(rarity)) {
						keyToUse = type2;
					}
				}

				if (!keyToUse.equals("")) {
					String series2 = "";
					type2 = "";
					String info2 = "";
					if (series.matches(nameTemplate)) {
						series2 = series;
					} else {
						series2 = "None";
					}

					if (type.matches(nameTemplate)) {
						type2 = type;
					} else {
						type2 = "None";
					}

					if (info.matches(nameTemplate)) {
						info2 = info;
					} else {
						info2 = "None";
					}

					boolean hasShiny2 = hasShiny;
					getCardsConfig().getConfig().set("Cards." + rarity + "." + name + ".Series", series2);
					getCardsConfig().getConfig().set("Cards." + rarity + "." + name + ".Type", type2);
					getCardsConfig().getConfig().set("Cards." + rarity + "." + name + ".Has-Shiny-Version", hasShiny2);
					getCardsConfig().getConfig().set("Cards." + rarity + "." + name + ".Info", info2);
					getCardsConfig().saveConfig();
					getCardsConfig().reloadConfig();
					sendMessage(creator, getPrefixedMessage(getMessagesConfig().getConfig().getString("Messages.CreateSuccess").replaceAll("%name%", name).replaceAll("%rarity%", rarity)));
				} else {
					creator.sendMessage(this.cMsg(getMessagesConfig().getConfig().getString("Messages.Prefix") + " " + getMessagesConfig().getConfig().getString("Messages.NoRarity")));
				}
			} else {
				creator.sendMessage(this.cMsg(getMessagesConfig().getConfig().getString("Messages.Prefix") + " " + getMessagesConfig().getConfig().getString("Messages.CreateNoName")));
			}
		} else {
			creator.sendMessage(this.cMsg(getMessagesConfig().getConfig().getString("Messages.Prefix") + " " + getMessagesConfig().getConfig().getString("Messages.CreateExists")));
		}

	}

	public void reloadAllConfig() {
		File file = new File(this.getDataFolder() + File.separator + "config.yml");
		if (!file.exists()) {
			this.getConfig().options().copyDefaults(true);
			this.saveDefaultConfig();
		}

		this.reloadConfig();
		this.deckConfig.reloadConfig();
		this.messagesConfig.reloadConfig();
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


	public boolean deleteRarity(Player p, String rarity) {
		if (!this.isRarity(rarity).equals("None")) {
			ConfigurationSection cards = getCardsConfig().getConfig().getConfigurationSection("Cards." + this.isRarity(rarity));
			Set<String> cardKeys = cards.getKeys(false);
			int numCardsCounter = 0;

			for (final String key : cardKeys) {
				debug("deleteRarity iteration: " + numCardsCounter);

				if (this.hasShiny(p, key, rarity) && this.hasCard(p, key, rarity) == 0) {
					debug("Deleted: Cards." + key + ".key2");

					this.deleteCard(p, key, rarity);
					++numCardsCounter;
				}

				if (this.hasCard(p, key, rarity) > 0) {
					debug("Deleted: Cards." + key + ".key2");

					this.deleteCard(p, key, rarity);
					++numCardsCounter;
				}
			}
			return true;
		}

		return false;
	}

	public String cMsg(String message) {
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	public class CardSchedulerRunnable extends BukkitRunnable {
		private final TradingCards plugin;

		public CardSchedulerRunnable(final TradingCards plugin) {
			this.plugin = plugin;
		}

		@Override
		public void run() {
			plugin.debug(getClass().getSimpleName() + " task running");
			//check this before the task is registered.
			if (!plugin.getConfig().getBoolean("General.Schedule-Cards"))
				return;

			if (plugin.getConfig().getBoolean("General.Schedule-Cards-Natural")) {
				String mob = plugin.getConfig().getString("General.Schedule-Card-Mob");
				if (plugin.isMob(mob.toUpperCase())) {
					plugin.giveawayNatural(EntityType.valueOf(mob.toUpperCase()), null);
					return;
				}
				plugin.getLogger().info("Error! schedule-card-mob is an invalid mob?");
				return;
			}
			debug("Schedule cards is true.");

			ConfigurationSection rarities = getCardsConfig().getConfig().getConfigurationSection("Cards");
			Set<String> rarityKeys = rarities.getKeys(false);
			String keyToUse = "";

			for (final String key : rarityKeys) {
				debug("Rarity key: " + key);
				if (key.equalsIgnoreCase(plugin.getConfig().getString("General.Schedule-Card-Rarity"))) {
					keyToUse = key;
				}
			}
			debug("keyToUse: " + keyToUse);

			if (!keyToUse.equals("")) {
				Bukkit.broadcastMessage(plugin.cMsg(getMessagesConfig().getConfig().getString("Messages.Prefix") + " " + getMessagesConfig().getConfig().getString("Messages.ScheduledGiveaway")));

				for (final Player p : Bukkit.getOnlinePlayers()) {
					ConfigurationSection cards = getCardsConfig().getConfig().getConfigurationSection("Cards." + keyToUse);
					Set<String> cardKeys = cards.getKeys(false);
					int rIndex = plugin.r.nextInt(cardKeys.size());
					int i = 0;
					String cardName = "";

					for (Iterator<String> var11 = cardKeys.iterator(); var11.hasNext(); ++i) {
						String theCardName = var11.next();
						if (i == rIndex) {
							cardName = theCardName;
							break;
						}
					}


					if (p.getInventory().firstEmpty() != -1) {
						p.getInventory().addItem(CardManager.getCard(cardName, keyToUse, false));
					} else {
						World curWorld = p.getWorld();
						if (p.getGameMode() == GameMode.SURVIVAL) {
							curWorld.dropItem(p.getLocation(), CardManager.getCard(cardName, keyToUse, false));
						}
					}
				}
			}
		}


	}

	public void startTimer() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		if (scheduler.isQueued(this.taskid) || scheduler.isCurrentlyRunning(this.taskid)) {
			scheduler.cancelTask(this.taskid);
			debug("Successfully cancelled task " + this.taskid);
		}

		int hours = Math.max(this.getConfig().getInt("General.Schedule-Card-Time-In-Hours"), 1);

		String tmessage = getMessagesConfig().getConfig().getString("Messages.TimerMessage").replaceAll("%hour%", String.valueOf(hours));
		Bukkit.broadcastMessage(getPrefixedMessage(tmessage));
		this.taskid = new CardSchedulerRunnable(this).runTaskTimer(this, (hours * 20 * 60 * 60), (hours * 20 * 60 * 60)).getTaskId();
	}

	public boolean isPlayerCard(String name) {
		String rarity = this.getConfig().getString("General.Auto-Add-Player-Rarity");
		String type = this.getConfig().getString("General.Player-Type");
		return getCardsConfig().getConfig().contains("Cards." + rarity + "." + name) && getCardsConfig().getConfig().getString("Cards." + rarity + "." + name + ".Type").equalsIgnoreCase(type);
	}
}