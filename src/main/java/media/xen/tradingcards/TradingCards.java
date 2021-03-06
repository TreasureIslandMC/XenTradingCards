package media.xen.tradingcards;

import co.aikar.commands.BukkitCommandManager;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import media.xen.tradingcards.commands.CardsCommand;
import media.xen.tradingcards.commands.DeckCommand;
import media.xen.tradingcards.config.CardsConfig;
import media.xen.tradingcards.config.DeckConfig;
import media.xen.tradingcards.config.MessagesConfig;
import media.xen.tradingcards.config.TradingCardsConfig;
import media.xen.tradingcards.listeners.DeckListener;
import media.xen.tradingcards.listeners.MobSpawnListener;
import media.xen.tradingcards.listeners.PackListener;
import media.xen.tradingcards.listeners.DropListener;
import net.milkbowl.vault.economy.Economy;
import net.sarhatabaot.configloader.ConfigLoader;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;


public class TradingCards extends JavaPlugin {
	private List<EntityType> hostileMobs = new ArrayList<>();
	private List<EntityType> passiveMobs = new ArrayList<>();
	private List<EntityType> neutralMobs = new ArrayList<>();
	private List<EntityType> bossMobs = new ArrayList<>();
	private boolean hasVault;
	private TradingCardsConfig mainConfig;
	private DeckConfig deckConfig;
	private MessagesConfig messagesConfig;
	private CardsConfig cardsConfig;
	private Economy econ = null;
	private Random random = new Random();
	int taskid;

	public boolean isHasVault() {
		return hasVault;
	}

	public Economy getEcon() {
		return econ;
	}

	public DeckConfig getDeckConfig() {
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


	private void hookFileSystem() {
		getLogger().info("Legacy YML mode is enabled!");
	}

	private void registerListeners() {
		PluginManager pm = Bukkit.getPluginManager();
		pm.addPermission(new Permission("cards.rarity"));
		pm.registerEvents(new DropListener(this), this);
		pm.registerEvents(new PackListener(this), this);
		pm.registerEvents(new MobSpawnListener(this), this);
		pm.registerEvents(new DeckListener(this), this);
	}

	private void cacheMobs() {
		final String serverVersion = Bukkit.getServer().getVersion();
		ImmutableList.Builder<EntityType> safeHostileMobs = ImmutableList.builder();
		ImmutableList.Builder<EntityType> safeNeutralMobs = ImmutableList.builder();
		ImmutableList.Builder<EntityType> safePassiveMobs = ImmutableList.builder();
		ImmutableList.Builder<EntityType> safeBossMobs = ImmutableList.builder();
		safeHostileMobs.add(EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.BLAZE, EntityType.SILVERFISH, EntityType.GHAST, EntityType.SLIME, EntityType.EVOKER, EntityType.VINDICATOR, EntityType.VEX, EntityType.SHULKER, EntityType.GUARDIAN, EntityType.MAGMA_CUBE, EntityType.ELDER_GUARDIAN, EntityType.STRAY, EntityType.HUSK, EntityType.DROWNED, EntityType.WITCH, EntityType.ZOMBIE_VILLAGER, EntityType.ENDERMITE);
		safeNeutralMobs.add(EntityType.ENDERMAN, EntityType.POLAR_BEAR, EntityType.LLAMA, EntityType.WOLF, EntityType.DOLPHIN, EntityType.DOLPHIN, EntityType.SNOWMAN, EntityType.IRON_GOLEM);
		safePassiveMobs.add(EntityType.DONKEY, EntityType.MULE, EntityType.SKELETON_HORSE, EntityType.CHICKEN, EntityType.COW, EntityType.SQUID, EntityType.TURTLE, EntityType.TROPICAL_FISH, EntityType.PUFFERFISH, EntityType.SHEEP, EntityType.PIG, EntityType.PHANTOM, EntityType.SALMON, EntityType.COD, EntityType.RABBIT, EntityType.VILLAGER, EntityType.BAT, EntityType.PARROT, EntityType.HORSE);
		safeBossMobs.add(EntityType.ENDER_DRAGON, EntityType.WITHER);
		if (serverVersion.contains("1.14") || serverVersion.contains("1.15") || serverVersion.contains("1.16")) {
			safeHostileMobs.add(EntityType.PILLAGER, EntityType.RAVAGER);
			safeNeutralMobs.add(EntityType.PANDA, EntityType.FOX);
			safePassiveMobs.add(EntityType.WANDERING_TRADER, EntityType.CAT, EntityType.MUSHROOM_COW, EntityType.TRADER_LLAMA);
			if (serverVersion.contains("1.15") || serverVersion.contains("1.16")) {
				safeNeutralMobs.add(EntityType.BEE);
				if (serverVersion.contains("1.16")) {
					safeHostileMobs.add(EntityType.HOGLIN, EntityType.PIGLIN, EntityType.STRIDER, EntityType.ZOGLIN, EntityType.ZOMBIFIED_PIGLIN);
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

		deckConfig = new DeckConfig(this);
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
		this.getServer().getPluginManager().removePermission("cards.rarity");
	}

	private boolean setupEconomy() {
		if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}

		RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}

		econ = rsp.getProvider();
		return econ != null;
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


	@Deprecated
	public boolean deleteCard(Player p, String card, String rarity) {
		return getCardsConfig().deleteCard(p,card,rarity);
	}

	@Deprecated
	public boolean hasCard(Player player, String card, String rarity) {
		return getDeckConfig().containsCard(player.getUniqueId(), card, rarity);
	}

	@Deprecated
	public boolean hasShiny(Player p, String card, String rarity) {
		return getDeckConfig().containsShinyCard(p.getUniqueId(), card, rarity);
	}

	@Deprecated
	public String isRarityAndFormat(String input) {
		String output = input.substring(0, 1).toUpperCase() + input.substring(1);
		if (this.getConfig().contains("Rarities." + input.replace("_", " "))) {
			return input.replace("_", " ");
		} else if (this.getConfig().contains("Rarities." + input.replace("_", " ").toUpperCase())) {
			return input.replace("_", " ").toUpperCase();
		} else if (this.getConfig().contains("Rarities." + input.replace("_", " ").toLowerCase())) {
			return input.replace("_", " ").toLowerCase();
		} else if (this.getConfig().contains("Rarities." + output.replace("_", " "))) {
			return output.replace("_", " ");
		}

		return this.getConfig().contains("Rarities." + this.capitaliseUnderscores(input)) ? output.replace("_", " ") : "None";
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
		return cMsg(messagesConfig.prefix + "&r " + message);
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
		ConfigLoader.loadAndSave(mainConfig);
		this.deckConfig.reloadConfig();
		ConfigLoader.loadAndSave(messagesConfig);
		this.cardsConfig.reloadConfig();
	}

	public static void sendMessage(final CommandSender toWhom, final String message) {
		toWhom.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
	}

	public boolean completedRarity(Player p, String rarity) {
		if ("None".equals(isRarityAndFormat(rarity))) {
			return false;
		}

		Set<String> cardNamesKeys = getCardsConfig().getConfig().getConfigurationSection("Cards." + this.isRarityAndFormat(rarity)).getKeys(false);
		return countCardsInRarity(p,rarity,cardNamesKeys) >= cardNamesKeys.size()-1;
	}

	private int countCardsInRarity(final Player player, final String rarity, final Set<String> cardNamesKeys) {
		int numCardsCounter = 0;

		for (String cardName: cardNamesKeys) {
			debug("CardName: " + cardName);
			debug("Counter: " + numCardsCounter);

			if (hasShiny(player, cardName, isRarityAndFormat(rarity))
					|| hasCard(player, cardName, isRarityAndFormat(rarity))) {
				numCardsCounter++;
			}
		}
		return numCardsCounter;
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

		Bukkit.broadcastMessage(getPrefixedTimerMessage(hours));
		this.taskid = new CardSchedulerRunnable(this).runTaskTimer(this, ((long) hours * 20 * 60 * 60), ((long) hours * 20 * 60 * 60)).getTaskId();
	}

	private String getPrefixedTimerMessage(int hours) {
		return getPrefixedMessage(messagesConfig.timerMessage.replace("%hour%", String.valueOf(hours)));
	}

	public Random getRandom() {
		return random;
	}

	public void setRandom(Random random) {
		this.random = random;
	}
}