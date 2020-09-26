package media.xen.tradingcards;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static media.xen.tradingcards.TradingCards.econ;
import static media.xen.tradingcards.TradingCards.sendMessage;

@CommandAlias("cards")
public class CardsCommand extends BaseCommand {
	private final TradingCards plugin;
	private final boolean showUsage;
	private PlayerBlacklist playerBlacklist;

	public CardsCommand(
			final TradingCards plugin,
			final PlayerBlacklist playerBlacklist) {
		this.plugin = plugin;
		this.playerBlacklist = playerBlacklist;
		this.showUsage = plugin.getConfig().getBoolean("General.Show-Command-Usage", true);
	}

	private void sendHelpMessage(final CommandSender sender, final String permission, final String usagePath, final String helpPath) {
		if (sender.hasPermission(permission)) {
			sendMessage(sender, "&7> &3" + plugin.getMessagesConfig().getConfig().getString(usagePath));
			if (showUsage) {
				sendMessage(sender, "&7- &f&o" + plugin.getMessagesConfig().getConfig().getString(helpPath));
			}
		}
	}

	public String formatTitle(String title) {
		String line = "&7[&foOo&7]&f____________________________________________________&7[&foOo&7]&f";
		int pivot = line.length() / 2;
		String center = "&7.< &3" + title + "&7 >.&f";
		String out = line.substring(0, Math.max(0, pivot - center.length() / 2));
		out = out + center + line.substring(pivot + center.length() / 2);
		return out;
	}

	private void helpCommand(final CommandSender sender) {
		sendMessage(sender, formatTitle(plugin.getConfig().getString("General.Server-Name") + " Trading Cards"));
		sendHelpMessage(sender, "cards.reload", "Messages.ReloadUsage", "Messages.ReloadHelp");
		sendHelpMessage(sender, "cards.givecard", "Messages.GiveCardUsage", "Messages.GiveCardHelp");
		sendHelpMessage(sender, "cards.giveshinycard", "Messages.GiveShinyCardUsage", "Messages.GiveShinyCardHelp");
		sendHelpMessage(sender, "cards.giverandomcard", "Messages.GiveRandomCardUsage", "Messages.GiveRandomCardHelp");
		sendHelpMessage(sender, "cards.giveboosterpack", "Messages.GiveBoosterPackUsage", "Messages.GiveBoosterPackHelp");
		sendHelpMessage(sender, "cards.giveaway", "Messages.GiveawayUsage", "Messages.GiveawayHelp");
		sendHelpMessage(sender, "cards.getdeck", "Messages.GetDeckUsage", "Messages.GetDeckHelp");
		sendHelpMessage(sender, "cards.list", "Messages.ListUsage", "Messages.ListHelp");
		sendHelpMessage(sender, "cards.listpacks", "Messages.ListPacksUsage", "Messages.ListPacksHelp");
		sendHelpMessage(sender, "cards.toggle", "Messages.ToggleUsage", "Messages.ToggleHelp");
		sendHelpMessage(sender, "cards.create", "Messages.CreateUsage", "Messages.CreateHelp");
		/*if (plugin.hasVault) {
			sendHelpMessage(sender, "cards.buy", "Messages.BuyUsage", "Messages.BuyHelp");
			sendHelpMessage(sender, "cards.worth", "Messages.WorthUsage", "Messages.WorthHelp");
		}*/
		sendHelpMessage(sender, "cards.resolve", "Messages.ResolveUsage", "Messages.ResolveHelp");
		sendHelpMessage(sender, "cards.reward", "Messages.RewardUsage", "Messages.RewardHelp");
	}

	@CatchUnknown
	@HelpCommand
	public void onHelp(final CommandSender sender, CommandHelp help) {
		help.showHelp();
		//helpCommand(sender);
	}
	@CommandAlias("version|ver")
	@CommandPermission("cards.version")
	public void onVersion(final CommandSender sender){
		final String format = "%s %s API-%s";
		sendMessage(sender,String.format(format,plugin.getName(), plugin.getDescription().getVersion(),plugin.getDescription().getAPIVersion()));
	}

	@CommandAlias("reload")
	@CommandPermission("cards.reload")
	public void onReload(final CommandSender sender) {
		final String format = "%s %s";
		sendMessage(sender, String.format(format, plugin.getMessagesConfig().getConfig().getString("Messages.Prefix"), plugin.getMessagesConfig().getConfig().getString("Messages.Reload")));
		plugin.reloadAllConfig();
		if (plugin.getConfig().getBoolean("General.Schedule-Cards")) {
			plugin.startTimer();
		}
	}

	@CommandAlias("rarities")
	@CommandPermission("cards.list.rarities")
	public void onRarities(final CommandSender sender) {
		ConfigurationSection cardsConfigSection = plugin.getCardsConfig().getConfig().getConfigurationSection("Cards");
		if(cardsConfigSection == null)
			return;

		final List<String> rarities = new ArrayList<>(cardsConfigSection.getKeys(false));
		sender.sendMessage(StringUtils.join(rarities, ", "));
	}


	@CommandAlias("resolve")
	@CommandPermission("cards.resolve")
	public void onResolve(final CommandSender sender, final Player player) {
		String resolveMessageTemplate = plugin.getMessagesConfig().getConfig().getString("Messages.ResolveMsg");
		if(resolveMessageTemplate == null)
			return;
		sendMessage(sender, resolveMessageTemplate.replaceAll("%name%", player.getName()).replaceAll("%uuid%", player.getUniqueId().toString()));
	}

	@CommandAlias("toggle")
	@CommandPermission("cards.toggle")
	public void onToggle(final Player player) {
		if(playerBlacklist.isAllowed(player)){
			playerBlacklist.remove(player);
			sendMessage(player, plugin.getPrefixedMessage(plugin.getMessagesConfig().getConfig().getString("Messages.ToggleEnabled")));
		}
		else{
			playerBlacklist.add(player);
			sendMessage(player, plugin.getPrefixedMessage(plugin.getMessagesConfig().getConfig().getString("Messages.ToggleEnabled")));
		}
	}

	@CommandAlias("create")
	@CommandPermission("cards.create")
	public void onCreate(final Player player, final String rarity, final String name, final String series, final String type, final boolean shiny, final String info) {
		plugin.createCard(player, rarity.replaceAll("_", " "), name, series.replaceAll("_", " "), type.replaceAll("_", " "), shiny, info.replaceAll("_", " "));
	}

	@CommandAlias("givecard")
	@CommandPermission("cards.givecard")
	public void onGiveCard(final Player player, final String name, final String rarity) {
		if (plugin.getCardsConfig().getConfig().contains("Cards." + rarity + "." + name)) {
			player.getInventory().addItem(CardManager.getCard(name, rarity));
			return;
		}
		sendMessage(player, plugin.getPrefixedMessage(plugin.getMessagesConfig().getConfig().getString("Messages.NoCard")));
	}


	@CommandAlias("giveshinycard")
	@CommandPermission("cards.giveshinycard")
	public void onGiveShinyCard(final Player player, final String name, final String rarity) {
		if (plugin.getCardsConfig().getConfig().contains("Cards." + rarity + "." + name)) {
			player.getInventory().addItem(CardManager.getCard(name, rarity, true));
			return;
		}
		sendMessage(player, plugin.getPrefixedMessage(plugin.getMessagesConfig().getConfig().getString("Messages.NoCard")));

	}
/*
	@CommandAlias("giveboosterpack")
	@CommandPermission("cards.giveboosterpack")
	public void onGiveBoosterpack(final CommandSender sender, final Player player, final String boosterpack) {
		World curWorld;
		if (player.getInventory().firstEmpty() != -1) {
			sendMessage(player, plugin.getPrefixedMessage(plugin.getMessagesConfig().getConfig().getString("Messages.BoosterPackMsg")));
			player.getInventory().addItem(plugin.createBoosterPack(boosterpack));
			return;
		}

		curWorld = player.getWorld();
		if (player.getGameMode() == GameMode.SURVIVAL) {
			sendMessage(player, plugin.getPrefixedMessage(plugin.getMessagesConfig().getConfig().getString("Messages.BoosterPackMsg")));
			curWorld.dropItem(player.getLocation(), plugin.createBoosterPack(boosterpack));
		}

	}
*/
	private void sendPrefixedMessage(final CommandSender toWhom, final String message) {
		sendMessage(toWhom, plugin.getPrefixedMessage(message));
	}
/*
	@CommandAlias("modules")
	@CommandPermission("cards.admin.modules")
	public void onModules(final CommandSender sender){
		final StringBuilder builder = new StringBuilder("Enabled Modules:");
		builder.append("\n");
		for(String depend: plugin.getDescription().getSoftDepend()){
			if(Bukkit.getPluginManager().getPlugin(depend) == null)
				builder.append(ChatColor.GRAY);
			else {
				builder.append(ChatColor.GREEN);
			}
			builder.append(depend).append(" ");
		}
		sender.sendMessage(builder.toString());
	}

	@CommandAlias("getdeck")
	public void onGetDeck(final Player player, final int deckNumber) {
		World curWorld;
		if (player.hasPermission("cards.decks." + deckNumber)) {
			if (plugin.getConfig().getBoolean("General.Use-Deck-Item")) {
				if (!plugin.hasDeck(player, deckNumber)) {
					if (player.getInventory().firstEmpty() != -1) {
						sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.GiveDeck"));
						player.getInventory().addItem(plugin.createDeck(player, deckNumber));
					} else {
						curWorld = player.getWorld();
						if (player.getGameMode() == GameMode.SURVIVAL) {
							sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.GiveDeck"));

							curWorld.dropItem(player.getLocation(), plugin.createDeck(player, deckNumber));
						}
					}
				} else {
					sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.AlreadyHaveDeck"));
				}
				return;
			}

			if (player.getGameMode() == GameMode.CREATIVE) {
				if (plugin.getConfig().getBoolean("General.Decks-In-Creative")) {
					plugin.openDeck(player, deckNumber);
					return;
				}
				sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.DeckCreativeError"));
				return;
			}
			plugin.openDeck(player, deckNumber);
			return;
		}

		sendMessage(player, plugin.getPrefixedMessage(plugin.getMessagesConfig().getConfig().getString("Messages.MaxDecks")));
	}

	@CommandAlias("giverandomcard")
	@CommandPermission("cards.randomcard")
	public void onGiveRandomCard(final CommandSender sender, final Player player, final String entityType) {
		try {
			EntityType.valueOf(entityType.toUpperCase());
			String rare = plugin.calculateRarity(EntityType.valueOf(entityType.toUpperCase()), true);
			plugin.debug("onCommand.rare: " + rare);
			sendPrefixedMessage(sender, plugin.getMessagesConfig().getConfig().getString("Messages.GiveRandomCardMsg").replaceAll("%player%", player.getName()));

			if (player.getInventory().firstEmpty() != -1) {
				sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.GiveRandomCard"));
				player.getInventory().addItem(CardUtil.getRandomCard(rare, false));
			} else {
				World curWorld2 = player.getWorld();
				if (player.getGameMode() == GameMode.SURVIVAL) {
					sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.GiveRandomCard"));
					curWorld2.dropItem(player.getLocation(), CardUtil.getRandomCard(rare, false));
				}
			}
		} catch (IllegalArgumentException exception) {
			sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.NoEntity"));
		}
	}

	@CommandAlias("list")
	@CommandPermission("cards.list")
	public void onList(final CommandSender sender, @Optional final String name) {
		StringBuilder cardName2;
		String cardName;
		Set<String> rarityKeys;
		ConfigurationSection rarities;
		boolean canBuy2;
		boolean hasExtra;
		String rarity;

		ConfigurationSection cardsWithKey;
		Set<String> keyKeys;
		Iterator<String> var17;
		String key2;
		String colour;
		int j;
		int numCardsCounter2;
		Player p4;
		Iterator<String> var43;
		String thisKey;
		if (name != null) {
			if (plugin.isRarity(name).equalsIgnoreCase("None")) {
				if (sender.hasPermission("cards.list.others")) {
					if (showUsage) {
						Player p3 = Bukkit.getPlayer(name);
						ConfigurationSection cards = plugin.getCardsConfig().getConfig().getConfigurationSection("Cards");
						Set<String> cardKeys = cards.getKeys(false);
						cardName2 = new StringBuilder();
						int i = 0;
						int numCardsCounter = 0;
						String finalMsg = "";
						sendMessage(sender, "&e&l------- &7(&6&l" + p3.getName() + "'s Collection&7)&e&l -------");

						for (Iterator var13 = cardKeys.iterator(); var13.hasNext(); numCardsCounter = 0) {
							cardName = (String) var13.next();
							cardsWithKey = plugin.getCardsConfig().getConfig().getConfigurationSection("Cards." + cardName);
							keyKeys = cardsWithKey.getKeys(false);
							var17 = keyKeys.iterator();


							while (var17.hasNext()) {
								key2 = var17.next();
								if (i > 32) {
									if (plugin.hasCard(p3, key2, cardName) > 0) {
										++numCardsCounter;
									}
									finalMsg = cardName2 + "&7and more!";
								} else {
									plugin.debug(cardName + ", " + key2);

									colour = plugin.getConfig().getString("Colours.ListHaveCard");
									if (plugin.hasShiny(p3, key2, cardName)) {
										++numCardsCounter;
										colour = plugin.getConfig().getString("Colours.ListHaveShinyCard");
										cardName2.append(colour).append(key2.replaceAll("_", " ")).append("&f, ");
									} else if (plugin.hasCard(p3, key2, cardName) > 0 && !plugin.hasShiny(p3, key2, cardName)) {
										++numCardsCounter;
										cardName2.append(colour).append(key2.replaceAll("_", " ")).append("&f, ");
									} else {
										cardName2.append("&7").append(key2.replaceAll("_", " ")).append("&f, ");
									}

								}
								++i;
							}

							if (numCardsCounter >= i) {
								sendMessage(sender, "&6--- " + cardName + " &7(" + plugin.getConfig().getString("Colours.ListRarityComplete") + "Complete&7)&6 ---");
							} else {
								sendMessage(sender, "&6--- " + cardName + " &7(&c" + numCardsCounter + "&f/&a" + i + "&7)&6 ---");
							}

							cardName2 = new StringBuilder(StringUtils.removeEnd(cardName2.toString(), ", "));
							if (finalMsg.equals("")) {
								sendMessage(sender, cardName2.toString());
							} else {
								sendMessage(sender, finalMsg);
							}

							cardName2 = new StringBuilder();
							finalMsg = "";
							i = 0;
							break;

						}
					} else {
						sendPrefixedMessage(sender, plugin.getMessagesConfig().getConfig().getString("Messages.ListError").replaceAll("%name%", name));
					}
				} else {
					sendPrefixedMessage(sender, plugin.getMessagesConfig().getConfig().getString("Messages.NoPerms"));
				}
			} else {
				rarities = plugin.getCardsConfig().getConfig().getConfigurationSection("Cards." + plugin.isRarity(name));
				rarityKeys = rarities.getKeys(false);
				StringBuilder keyToUse = new StringBuilder();
				j = 0;
				numCardsCounter2 = 0;
				p4 = (Player) sender;
				rarity = "";
				var43 = rarityKeys.iterator();

				while (var43.hasNext()) {
					thisKey = var43.next();
					if (j > 100) {
						if (plugin.hasCard(p4, thisKey, plugin.isRarity(name)) > 0) {
							++numCardsCounter2;
						}

						rarity = keyToUse + "&7and more!";
						++j;
					} else {
						plugin.debug(thisKey + ", " + plugin.isRarity(name));

						cardName = plugin.getConfig().getString("Colours.ListHaveCard");
						if (plugin.hasShiny((Player) sender, thisKey, plugin.isRarity(name))) {
							++numCardsCounter2;
							cardName = plugin.getConfig().getString("Colours.ListHaveShinyCard");
							keyToUse.append(cardName).append(thisKey.replaceAll("_", " ")).append("&f, ");
						} else if (plugin.hasCard((Player) sender, thisKey, plugin.isRarity(name)) > 0 && !plugin.hasShiny((Player) sender, thisKey, plugin.isRarity(name))) {
							++numCardsCounter2;
							keyToUse.append(cardName).append(thisKey.replaceAll("_", " ")).append("&f, ");
						}

						++j;
					}
				}

				if (numCardsCounter2 >= j) {
					sendMessage(sender, "&6--- " + plugin.isRarity(name) + " &7(" + plugin.getConfig().getString("Colours.ListRarityComplete") + "Complete&7)&6 ---");
				} else {
					sendMessage(sender, "&6--- " + plugin.isRarity(name) + " &7(&c" + numCardsCounter2 + "&f/&a" + j + "&7)&6 ---");
				}

				keyToUse = new StringBuilder(StringUtils.removeEnd(keyToUse.toString(), ", "));
				if (rarity.equals("")) {
					sendMessage(sender, keyToUse.toString());
				} else {
					sendMessage(sender, rarity);
				}

				keyToUse = new StringBuilder();
				rarity = "";
				canBuy2 = false;
				hasExtra = false;
			}
		} else {
			rarities = plugin.getCardsConfig().getConfig().getConfigurationSection("Cards");
			rarityKeys = rarities.getKeys(false);
			StringBuilder keyToUse = new StringBuilder();
			j = 0;
			numCardsCounter2 = 0;
			p4 = (Player) sender;
			rarity = "";

			for (var43 = rarityKeys.iterator(); var43.hasNext(); numCardsCounter2 = 0) {
				thisKey = var43.next();
				cardsWithKey = plugin.getCardsConfig().getConfig().getConfigurationSection("Cards." + thisKey);
				keyKeys = cardsWithKey.getKeys(false);
				var17 = keyKeys.iterator();


				while (var17.hasNext()) {
					key2 = var17.next();
					if (j > 32) {
						if (plugin.hasCard(p4, key2, thisKey) > 0) {
							++numCardsCounter2;
						}

						rarity = keyToUse + "&7and more!";
						++j;
					} else {
						plugin.debug(thisKey + ", " + key2);

						colour = plugin.getConfig().getString("Colours.ListHaveCard");
						if (plugin.hasShiny((Player) sender, key2, thisKey)) {
							++numCardsCounter2;
							colour = plugin.getConfig().getString("Colours.ListHaveShinyCard");
							keyToUse.append(colour).append(key2.replaceAll("_", " ")).append("&f, ");
						} else if (plugin.hasCard((Player) sender, key2, thisKey) > 0 && !plugin.hasShiny((Player) sender, key2, thisKey)) {
							++numCardsCounter2;
							keyToUse.append(colour).append(key2.replaceAll("_", " ")).append("&f, ");
						} else {
							keyToUse.append("&7").append(key2.replaceAll("_", " ")).append("&f, ");
						}

						++j;
					}
				}

				if (numCardsCounter2 >= j) {
					sendMessage(sender, "&6--- " + thisKey + " &7(" + plugin.getConfig().getString("Colours.ListRarityComplete") + "Complete&7)&6 ---");
				} else {
					sendMessage(sender, "&6--- " + thisKey + " &7(&c" + numCardsCounter2 + "&f/&a" + j + "&7)&6 ---");

				}

				keyToUse = new StringBuilder(StringUtils.removeEnd(keyToUse.toString(), ", "));
				if (rarity.equals("")) {
					sendMessage(sender, keyToUse.toString());
				} else {
					sendMessage(sender, rarity);
				}

				keyToUse = new StringBuilder();
				rarity = "";
				j = 0;
				break;
			}

		}
	}

	@CommandAlias("listpack")
	@CommandPermission("cards.listpacks")
	public void onListPack(final CommandSender sender) {
		ConfigurationSection rarities = plugin.getConfig().getConfigurationSection("BoosterPacks");
		Set<String> rarityKeys = rarities.getKeys(false);
		int k = 0;
		sendMessage(sender, "&6--- Booster Packs --- ");
		boolean canBuy2 = false;
		boolean hasExtra = false;

		for (Iterator<String> iterator = rarityKeys.iterator(); iterator.hasNext(); hasExtra = false) {
			String rarity = iterator.next();
			if (plugin.getConfig().getBoolean("PluginSupport.Vault.Vault-Enabled") && plugin.getConfig().contains("BoosterPacks." + rarity + ".Price") && plugin.getConfig().getDouble("BoosterPacks." + rarity + ".Price") > 0.0D) {
				canBuy2 = true;
			}

			if (plugin.getConfig().contains("BoosterPacks." + rarity + ".ExtraCardRarity") && plugin.getConfig().contains("BoosterPacks." + rarity + ".NumExtraCards")) {
				hasExtra = true;
			}

			++k;
			if (canBuy2) {
				sendPrefixedMessage(sender, "&6" + k + ") &e" + rarity + " &7(&aPrice: " + plugin.getConfig().getDouble("BoosterPacks." + rarity + ".Price") + "&7)");
			} else {
				sendPrefixedMessage(sender, "&6" + k + ") &e" + rarity);
			}

			if (hasExtra) {
				sendMessage(sender, "  &7- &f&o" + plugin.getConfig().getInt("BoosterPacks." + rarity + ".NumNormalCards") + " " + plugin.getConfig().getString("BoosterPacks." + rarity + ".NormalCardRarity") + ", " + plugin.getConfig().getInt("BoosterPacks." + rarity + ".NumExtraCards") + " " + plugin.getConfig().getString("BoosterPacks." + rarity + ".ExtraCardRarity") + ", " + plugin.getConfig().getInt("BoosterPacks." + rarity + ".NumSpecialCards") + " " + plugin.getConfig().getString("BoosterPacks." + rarity + ".SpecialCardRarity"));
			} else {
				sendMessage(sender, "  &7- &f&o" + plugin.getConfig().getInt("BoosterPacks." + rarity + ".NumNormalCards") + " " + plugin.getConfig().getString("BoosterPacks." + rarity + ".NormalCardRarity") + ", " + plugin.getConfig().getInt("BoosterPacks." + rarity + ".NumSpecialCards") + " " + plugin.getConfig().getString("BoosterPacks." + rarity + ".SpecialCardRarity"));
			}

			canBuy2 = false;
		}

	}

	private void execCommand(final CommandSender sender, final String rarity, final String path) {
		if (plugin.getConfig().contains("Rarities." + plugin.isRarity(rarity) + path) && !plugin.getConfig().getString("Rarities." + plugin.isRarity(rarity) + path).equalsIgnoreCase("None")) {
			plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), plugin.getConfig().getString("Rarities." + plugin.isRarity(rarity + path).replaceAll("%player%", sender.getName())));
		}
	}

	@CommandAlias("reward")
	@CommandPermission("cards.reward")
	public void onReward(final CommandSender sender, final String rarity) {
		if (!plugin.getConfig().getBoolean("General.Allow-Rewards")) {
			sendMessage(sender, plugin.getPrefixedMessage(plugin.getMessagesConfig().getConfig().getString("Messages.RewardDisabled")));
			return;
		}
		if (plugin.isRarity(rarity).equalsIgnoreCase("None")) {
			sendPrefixedMessage(sender, plugin.getMessagesConfig().getConfig().getString("Messages.RewardError"));
			return;
		}

		if (plugin.completedRarity((Player) sender, plugin.isRarity(rarity))) {
			execCommand(sender, rarity, ".RewardCmd1");
			execCommand(sender, rarity, ".RewardCmd2");
			execCommand(sender, rarity, ".RewardCmd3");

			if (plugin.getConfig().getBoolean("General.Reward-Broadcast")) {
				Bukkit.broadcastMessage(plugin.getPrefixedMessage(plugin.getMessagesConfig().getConfig().getString("Messages.RewardBroadcast").replaceAll("%player%", sender.getName()).replaceAll("%rarity%", plugin.isRarity(rarity))));
			}

			if (!plugin.deleteRarity((Player) sender, plugin.isRarity(rarity)) && plugin.getConfig().getBoolean("General.Debug-Mode")) {
				plugin.getLogger().warning("Cannot delete rarity: " + plugin.isRarity(rarity));
			}
		} else if (plugin.getConfig().getBoolean("General.Eat-Shiny-Cards")) {
			sendPrefixedMessage(sender, plugin.getMessagesConfig().getConfig().getString("Messages.RewardError2"));
		} else {
			sendPrefixedMessage(sender, plugin.getMessagesConfig().getConfig().getString("Messages.RewardError3").replaceAll("%shinyName%", plugin.getConfig().getString("General.Shiny-Name")));
		}


	}

	@CommandAlias("giveaway")
	@CommandPermission("cards.giveaway")
	public void onGiveaway(final CommandSender sender, final String rarity) {
		ConfigurationSection rarities = plugin.getCardsConfig().getConfig().getConfigurationSection("Cards");
		Set<String> rarityKeys = rarities.getKeys(false);
		String keyToUse = "";
		if (plugin.isMob(rarity)) {
			if (sender instanceof ConsoleCommandSender) {
				plugin.giveawayNatural(EntityType.valueOf(rarity.toUpperCase()), null);
			} else {
				plugin.giveawayNatural(EntityType.valueOf(rarity.toUpperCase()), (Player) sender);
			}
		} else {

			for (final String rarityKey : rarityKeys) {
				if (rarityKey.equalsIgnoreCase(rarity.replaceAll("_", " "))) {
					keyToUse = rarityKey;
				}
			}

			if (!keyToUse.equals("")) {
				Bukkit.broadcastMessage(plugin.getPrefixedMessage(plugin.getMessagesConfig().getConfig().getString("Messages.Giveaway").replaceAll("%player%", sender.getName()).replaceAll("%rarity%", keyToUse)));

				for (final Player p5 : Bukkit.getOnlinePlayers()) {
					ConfigurationSection cards4 = plugin.getCardsConfig().getConfig().getConfigurationSection("Cards." + keyToUse);
					Set<String> cardKeys4 = cards4.getKeys(false);
					int rIndex = plugin.r.nextInt(cardKeys4.size());
					int l = 0;
					String cardName = "";

					for (Iterator<String> var51 = cardKeys4.iterator(); var51.hasNext(); ++l) {
						String theCardName = var51.next();
						if (l == rIndex) {
							cardName = theCardName;
							break;
						}
					}

					dropCard(p5, cardName, keyToUse);
				}
			} else {
				sendPrefixedMessage(sender, plugin.getMessagesConfig().getConfig().getString("Messages.NoRarity"));
			}
		}


	}

	/**
	 * Drops a card at player's location. If the player has space in his inventory, the item gets placed directly into
	 * the inventory.
	 *
	 * @param player Player
	 * @param card   Card name
	 * @param rarity Card Rarity
	 */
	@Deprecated
	private void dropCard(final Player player, final String card, final String rarity) {
		if (player.getInventory().firstEmpty() != -1) {
			player.getInventory().addItem(CardManager.getCard(card, rarity));
		} else {
			World curWorld4 = player.getWorld();
			if (player.getGameMode() == GameMode.SURVIVAL) {
				curWorld4.dropItem(player.getLocation(), CardManager.getCard(card, rarity));
			}
		}
	}



	/*@CommandAlias("worth")
	@CommandPermission("cards.worth")
	public void onWorth(final Player player) {
		if (!hasVault(player)) {
			return;
		}
		if (player.getInventory().getItemInMainHand().getType() != Material.valueOf(plugin.getConfig().getString("General.Card-Material"))) {
			sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.NotACard"));
			return;
		}

		ItemStack itemInHand = player.getInventory().getItemInMainHand();
		ItemMeta itemInHandMeta = itemInHand.getItemMeta();
		if(itemInHandMeta == null)
			return;

		final String keyToUse = itemInHandMeta.getDisplayName();
		plugin.debug(keyToUse);
		plugin.debug(ChatColor.stripColor(keyToUse));

		String[] splitName = ChatColor.stripColor(keyToUse).split(" ");
		String cardName2;
		if (splitName.length > 1) {
			cardName2 = splitName[1];
		} else {
			cardName2 = splitName[0];
		}
		plugin.debug(cardName2);


		List<String> lore = itemInHand.getItemMeta().getLore();
		if(lore == null)
			return;

		String rarity = ChatColor.stripColor(lore.get(3));
		plugin.debug(rarity);

		boolean canBuy = false;
		double buyPrice = 0.0D;
		if (plugin.getCardsConfig().getConfig().contains("Cards." + rarity + "." + cardName2 + ".Buy-Price")) {
			buyPrice = plugin.getCardsConfig().getConfig().getDouble("Cards." + rarity + "." + cardName2 + ".Buy-Price");
			if (buyPrice > 0.0D) {
				canBuy = true;
			}
		}

		if (canBuy) {
			String canBuyMessageTemplate = plugin.getMessagesConfig().getConfig().getString("Messages.CanBuy");
			if(canBuyMessageTemplate == null)
				return;
			sendPrefixedMessage(player, canBuyMessageTemplate.replaceAll("%buyAmount%", String.valueOf(buyPrice)));
		} else {
			sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.CanNotBuy"));
		}

	}*/

	/*private boolean hasVault(final Player player) {
		if (!plugin.hasVault) {
			sendMessage(player, plugin.getPrefixedMessage(plugin.getMessagesConfig().getConfig().getString("Messages.NoVault")));
		}
		return plugin.hasVault;
	}*/


	/*@Subcommand("buy")
	@CommandPermission("cards.buy")
	public class BuySubCommand extends BaseCommand {

		@Subcommand("pack|boosterpack")
		@CommandPermission("cards.buy.pack")
		public void onBuyPack(final Player player, final String name) {
			if (!hasVault(player))
				return;
			if (!plugin.getConfig().contains("BoosterPacks." + name)) {
				sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.PackDoesntExist"));
				return;
			}


			double buyPrice2 = plugin.getConfig().getDouble("BoosterPacks." + name + ".Price", 0.0D);

			if (buyPrice2 <= 0.0D) {
				sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.CannotBeBought"));
				return;
			}

			EconomyResponse economyResponse = econ.withdrawPlayer(player, buyPrice2);
			if (economyResponse.transactionSuccess()) {
				if (plugin.getConfig().getBoolean("PluginSupport.Vault.Closed-Economy")) {//TODO
					econ.bankDeposit(plugin.getConfig().getString("PluginSupport.Vault.Server-Account"), buyPrice2);
				}

				String boughtCardMessageTemplate = plugin.getMessagesConfig().getConfig().getString("Messages.BoughtCard");
				if(boughtCardMessageTemplate == null)
					return;

				sendPrefixedMessage(player, boughtCardMessageTemplate.replaceAll("%amount%", String.valueOf(buyPrice2)));
				CardUtil.dropItem(player, plugin.createBoosterPack(name));
				return;
			}

			sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.NotEnoughMoney"));
		}


		@Subcommand("card")
		@CommandPermission("cards.buy.card")
		public void onBuyCard(final Player player, @NotNull final String rarity, @NotNull final String card) {
			if (!hasVault(player))
				return;

			if (!plugin.getCardsConfig().getConfig().contains("Cards." + rarity + "." + card)) {
				sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.CardDoesntExist"));
				return;
			}


			double buyPrice2 = plugin.getCardsConfig().getConfig().getDouble("Cards." + rarity + "." + card + ".Buy-Price", 0.0D);

			EconomyResponse economyResponse = econ.withdrawPlayer(player, buyPrice2);
			if (economyResponse.transactionSuccess()) {
				if (plugin.getConfig().getBoolean("PluginSupport.Vault.Closed-Economy")) {//TODO
					econ.bankDeposit(plugin.getConfig().getString("PluginSupport.Vault.Server-Account"), buyPrice2);
				}
				CardUtil.dropItem(player, CardManager.getCard(card, rarity));
				String boughtCardMessageTemplate = plugin.getMessagesConfig().getConfig().getString("Messages.BoughtCard");
				if(boughtCardMessageTemplate == null)
					return;

				sendPrefixedMessage(player, boughtCardMessageTemplate.replaceAll("%amount%", String.valueOf(buyPrice2)));
				return;
			}
			sendPrefixedMessage(player, plugin.getMessagesConfig().getConfig().getString("Messages.NotEnoughMoney"));
		}
	}*/
}





