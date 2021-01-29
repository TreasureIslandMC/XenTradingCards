package media.xen.tradingcards;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author sarhatabaot
 */
public class CardUtil {
	private static TradingCards plugin;

	public static void init(final TradingCards plugin) {
		CardUtil.plugin = plugin;
	}

	public static String getRarityName(@NotNull final String rarity) {
		return rarity.replace(stripAllColor(plugin.getMainConfig().shinyName), "").trim();
	}

	public String upgradeRarity(String packName, String rarity) {
		plugin.debug("Starting booster pack upgrade check - Current rarity is " + rarity + "!");
		ConfigurationSection rarities = plugin.getConfig().getConfigurationSection("Rarities");
		Set<String> rarityKeys = rarities.getKeys(false);
		Map<Integer, String> rarityMap = new HashMap<>();
		int i = 0;
		int curRarity = 0;
		for (String key : rarityKeys) {
			rarityMap.put(i, key);
			if (key.equalsIgnoreCase(rarity)) curRarity = i;
			plugin.debug("Rarity " + i + " is " + key);
			i++;
		}
		int chance = plugin.getConfig().getInt("BoosterPacks." + packName + ".UpgradeChance", 0);
		if (chance <= 0) {
			plugin.debug("Pack has upgrade chance set to 0! Exiting..");
			return rarityMap.get(curRarity);
		}
		int random = plugin.r.nextInt(100000) + 1;
		if (random <= chance) {
			if (curRarity < i) curRarity++;
			plugin.debug("Card upgraded! new rarity is " + rarityMap.get(curRarity) + "!");
			return rarityMap.get(curRarity);
		}
		plugin.debug("Card not upgraded! Rarity remains at " + rarityMap.get(curRarity) + "!");
		return rarityMap.get(curRarity);
	}


	/**
	 * Drops an item at the player's location.
	 *
	 * @param player Player
	 * @param item   Item
	 */
	public static void dropItem(final Player player, final ItemStack item) {
		if (player.getInventory().firstEmpty() != -1) {
			player.getInventory().addItem(item);
		} else {
			World curWorld4 = player.getWorld();
			if (player.getGameMode() == GameMode.SURVIVAL) {
				curWorld4.dropItem(player.getLocation(), item);
			}
		}
	}

	public static void createCard(Player creator, String rarity, String name, String series, String type, boolean hasShiny, String info, String about) {
		final String nameTemplate = "^[a-zA-Z0-9-_]+$";
		if (!plugin.getCardsConfig().getConfig().contains("Cards." + rarity + "." + name)) {
			if (name.matches(nameTemplate)) {
				if (isPlayerCard(name)) {
					name = name.replaceAll(" ", "_");
				}

				ConfigurationSection rarities = plugin.getCardsConfig().getConfig().getConfigurationSection("Cards");
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

					plugin.getCardsConfig().getConfig().set("Cards." + rarity + "." + name + ".Series", series2);
					plugin.getCardsConfig().getConfig().set("Cards." + rarity + "." + name + ".Type", type2);
					plugin.getCardsConfig().getConfig().set("Cards." + rarity + "." + name + ".Has-Shiny-Version", hasShiny);
					plugin.getCardsConfig().getConfig().set("Cards." + rarity + "." + name + ".Info", info2);
					plugin.getCardsConfig().saveConfig();
					plugin.getCardsConfig().reloadConfig();
					plugin.sendMessage(creator, plugin.getPrefixedMessage(plugin.getMessagesConfig().createSuccess.replaceAll("%name%", name).replaceAll("%rarity%", rarity)));
				} else {
					creator.sendMessage(plugin.cMsg(plugin.getMessagesConfig().prefix + " " + plugin.getMessagesConfig().noRarity));
				}
			} else {
				creator.sendMessage(plugin.cMsg(plugin.getMessagesConfig().prefix + " " + plugin.getMessagesConfig().createNoName));
			}
		} else {
			creator.sendMessage(plugin.cMsg(plugin.getMessagesConfig().prefix + " " + plugin.getMessagesConfig().createExists));
		}

	}

	@NotNull
	public static ItemStack generateCard(String cardName, String rarityName, boolean forcedShiny) {
		if (rarityName.equals("None")) {
			return new ItemStack(Material.AIR);
		}
		//plugin.reloadAllConfig();
		plugin.debug("generateCard.cardSection: " + plugin.getCardsConfig().getConfig().contains("Cards." + rarityName));
		plugin.debug("generateCard.rarity: " + rarityName);

		CardManager.CardBuilder builder = new CardManager.CardBuilder(cardName);
		boolean hasShinyVersion = plugin.getCardsConfig().getConfig().getBoolean("Cards." + rarityName + "." + cardName + ".Has-Shiny-Version");
		boolean isShiny = false;
		if (hasShinyVersion) {
			int shinyRandom = plugin.r.nextInt(100) + 1;
			if (shinyRandom <= plugin.getConfig().getInt("Chances.Shiny-Version-Chance")) {
				isShiny = true;
			}
		}

		if (forcedShiny) {
			isShiny = true;
		}
		String rarityColour = plugin.getConfig().getString("Rarities." + rarityName + ".Colour");
		String prefix = plugin.getConfig().getString("General.Card-Prefix");
		String series = plugin.getCardsConfig().getConfig().getString("Cards." + rarityName + "." + cardName + ".Series");
		String seriesColour = plugin.getConfig().getString("Colours.Series");
		String seriesDisplay = plugin.getConfig().getString("DisplayNames.Cards.Series", "Series");
		String about = plugin.getCardsConfig().getConfig().getString("Cards." + rarityName + "." + cardName + ".About", "None");
		String aboutColour = plugin.getConfig().getString("Colours.About");
		String aboutDisplay = plugin.getConfig().getString("DisplayNames.Cards.About", "About");
		String type = plugin.getCardsConfig().getConfig().getString("Cards." + rarityName + "." + cardName + ".Type");
		String typeColour = plugin.getConfig().getString("Colours.Type");
		String typeDisplay = plugin.getConfig().getString("DisplayNames.Cards.Type", "Type");
		String info = plugin.getCardsConfig().getConfig().getString("Cards." + rarityName + "." + cardName + ".Info");
		String infoColour = plugin.getConfig().getString("Colours.Info");
		String infoDisplay = plugin.getConfig().getString("DisplayNames.Cards.Info", "Info");
		String shinyPrefix = plugin.getConfig().getString("General.Shiny-Name");
		String cost;
		if (plugin.getCardsConfig().getConfig().contains("Cards." + rarityName + "." + cardName + ".Buy-Price")) {
			cost = String.valueOf(plugin.getCardsConfig().getConfig().getDouble("Cards." + rarityName + "." + cardName + ".Buy-Price"));
		} else {
			cost = "None";
		}

		boolean isPlayerCard = false;
		if (isPlayerCard(cardName)) {
			isPlayerCard = true;
		}

		return builder.isShiny(isShiny)
				.rarityColour(rarityColour)
				.prefix(prefix)
				.series(series, seriesColour, seriesDisplay)
				.about(about, aboutColour, aboutDisplay)
				.type(type, typeColour, typeDisplay)
				.info(info, infoColour, infoDisplay)
				.shinyPrefix(shinyPrefix)
				.isPlayerCard(isPlayerCard)
				.cost(cost)
				.rarity(rarityName).build();
	}


	@NotNull
	public static ItemStack getRandomCard(@NotNull final String rarityName, final boolean forcedShiny) {
		ConfigurationSection cardSection = plugin.getCardsConfig().getConfig().getConfigurationSection("Cards." + rarityName);
		Validate.notNull(cardSection, "No such section." + rarityName);

		Set<String> cards = cardSection.getKeys(false);
		List<String> cardNames = new ArrayList<>(cards);
		int cIndex = plugin.r.nextInt(cardNames.size());
		String cardName = cardNames.get(cIndex);
		return CardManager.getCard(cardName, rarityName, forcedShiny);
	}

	private static final char ALT_COLOR_CHAR = '&';
	private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + ALT_COLOR_CHAR + "[0-9A-FK-ORX]");

	/**
	 * Strips the given message of all color codes
	 *
	 * @param input String to strip of color
	 * @return A copy of the input string, without any coloring
	 */
	@Contract("!null -> !null; null -> null")
	@Nullable
	public static String stripAllColor(@Nullable final String input) {
		if (input == null) {
			return null;
		}

		return ChatColor.stripColor(STRIP_COLOR_PATTERN.matcher(input).replaceAll(""));
	}

	@NotNull
	public static String getCardName(@NotNull final String displayRarity, @NotNull final String displayCard) {
		final String strippedRarity = getRarityName(displayRarity);
		final boolean hasPrefix = plugin.getMainConfig().cardPrefix != null || !plugin.getMainConfig().cardPrefix.equals("");
		final String strippedPrefix = stripAllColor(plugin.getMainConfig().cardPrefix);
		final String strippedShiny = stripAllColor(plugin.getMainConfig().shinyName);
		final String strippedDisplay = StringUtils.replaceEach(stripAllColor(displayCard), new String[]{strippedPrefix, strippedShiny}, new String[]{"", ""}).trim();
		plugin.debug("stripped|rarity=" + strippedRarity + "|hasPrefix=" + hasPrefix + "|prefix=" + strippedPrefix + "|shiny=" + strippedShiny + "|display=" + strippedDisplay);

		if (!plugin.getCardsConfig().getConfig().contains("Cards." + strippedRarity)) {
			plugin.debug("No such card. card=" + strippedDisplay + "rarity=" + strippedRarity);
			return "None";
		}

		final ConfigurationSection configurationSection = plugin.getCardsConfig().getConfig().getConfigurationSection("Cards." + strippedRarity);
		for (String name : configurationSection.getKeys(false)) {
			if (name.equals(strippedDisplay.replace(" ", "_")))
				return name;
		}
		return "None";
	}


	private static String getMobType(EntityType e, int shouldItDrop, boolean alwaysDrop) {
		if (plugin.isMobHostile(e)) {
			if (!alwaysDrop) {
				if (shouldItDrop > plugin.getConfig().getInt("Chances.Hostile-Chance")) {
					return "None";
				}
			}
			return "Hostile";
		}
		if (plugin.isMobNeutral(e)) {
			if (!alwaysDrop) {
				if (shouldItDrop > plugin.getConfig().getInt("Chances.Neutral-Chance")) {
					return "None";
				}

			}
			return "Neutral";
		}
		if (plugin.isMobPassive(e)) {
			if (!alwaysDrop) {
				if (shouldItDrop > plugin.getConfig().getInt("Chances.Passive-Chance")) {
					return "None";
				}

			}
			return "Passive";
		}

		if (!plugin.isMobBoss(e)) {
			return "None";
		}

		if (!alwaysDrop) {
			if (shouldItDrop > plugin.getConfig().getInt("Chances.Boss-Chance")) {
				return "None";
			}

			//TODO: Implement this
			if (plugin.getConfig().getBoolean("Chances.Boss-Drop")) {
				int var16 = plugin.getConfig().getInt("Chances.Boss-Drop-Rarity");
			}

		}
		return "Boss";

	}
	//TODO This guy is responsible for the drop rate.. Really confusing, should use a "weight" system instead

	/**
	 * Returns the rarity that should drop.
	 *
	 * @param e
	 * @param alwaysDrop
	 * @return
	 */
	public static String calculateRarity(EntityType e, boolean alwaysDrop) {
		int shouldItDrop = plugin.r.nextInt(100) + 1;
		String type = getMobType(e, shouldItDrop, alwaysDrop);
		plugin.debug("shouldItDrop Num: " + shouldItDrop);

		ConfigurationSection rarities = plugin.getConfig().getConfigurationSection("Rarities");
		Set<String> rarityKeys = rarities.getKeys(false);
		Map<String, Integer> rarityChances = new HashMap<>();
		Map<Integer, String> rarityIndexes = new HashMap<>();
		plugin.debug(printIntOrStringMap(rarityChances));
		plugin.debug(printIntOrStringMap(rarityIndexes));
		//todo loads the rarirty index + chances everytime it needs to caclulate a rarity..
		int i = 0;
		int mini = 0;
		int random = plugin.r.nextInt(100000) + 1;
		plugin.debug("Random Card Num: " + random);
		plugin.debug("Type: " + type);

		String key;
		int chance;
		for (Iterator<String> var13 = rarityKeys.iterator(); var13.hasNext(); rarityChances.put(key, chance)) {
			key = var13.next();
			rarityIndexes.put(i, key);
			++i;

			plugin.debug(i + ", " + key);


			if (plugin.getConfig().contains("Chances." + key + "." + StringUtils.capitalize(e.getKey().getKey())) && mini == 0) {
				plugin.debug("Mini: " + i);
				mini = i;
			}

			chance = plugin.getConfig().getInt("Chances." + key + "." + type, -1);
			plugin.debug("Keys: " + key + ", " + chance + ", i=" + i);
		}

		if (mini != 0) {
			plugin.debug("Mini: " + mini);
			plugin.debug("i: " + i);

			while (i >= mini) {
				--i;
				plugin.debug("i: " + i);


				chance = plugin.getConfig().getInt("Chances." + rarityIndexes.get(i) + "." + StringUtils.capitalize(e.getKey().getKey()), -1);
				plugin.debug("Chance: " + chance);
				plugin.debug("Rarity: " + rarityIndexes.get(i));

				if (chance > 0 && random <= chance) {
					return rarityIndexes.get(i);
				}
			}
		} else {
			while (i > 0) {
				--i;
				plugin.debug("Iteration " + i + " in HashMap is: " + rarityIndexes.get(i) + ", " + plugin.getConfig().getString("Rarities." + rarityIndexes.get(i) + ".Name"));

				chance = plugin.getConfig().getInt("Chances." + rarityIndexes.get(i) + "." + type, -1);
				plugin.debug(plugin.getConfig().getString("Rarities." + rarityIndexes.get(i) + ".Name") + "'s chance of dropping: " + chance + " out of 100,000");

				if (chance > 0 && random <= chance) {
					plugin.debug("Giving a " + plugin.getConfig().getString("Rarities." + rarityIndexes.get(i) + ".Name") + " card.");
					return rarityIndexes.get(i);
				}
			}
		}
		return "None";
	}

	private static String printIntOrStringMap(Map<?, ?> map) {
		final StringBuilder sb = new StringBuilder();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			sb.append(entry.getKey()).append(":").append(entry.getValue());
		}
		return sb.toString();
	}

	public static boolean isPlayerCard(String name) {
		String rarity = plugin.getConfig().getString("General.Auto-Add-Player-Rarity");
		String type = plugin.getConfig().getString("General.Player-Type");
		return plugin.getCardsConfig().getConfig().contains("Cards." + rarity + "." + name) && plugin.getCardsConfig().getConfig().getString("Cards." + rarity + "." + name + ".Type").equalsIgnoreCase(type);
	}

	@Deprecated
	/*
	 * TODO
	 * We should check if an item is a card a different way, as the card material is an obtainable item
	 */
	public static boolean isCard(final ItemStack itemStack){
		return itemStack.getType() == Material.valueOf(plugin.getConfig().getString("General.Card-Material"));
	}
}
