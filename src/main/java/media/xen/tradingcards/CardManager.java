package media.xen.tradingcards;

import lombok.Data;
import media.xen.tradingcards.config.TradingCardsConfig;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardManager {
	private static TradingCards plugin;
	private static ItemStack blankCard;
	private static final Map<String,ItemStack> cards = new HashMap<>();

	/**
	 * Pre-loads all existing cards.
	 */
	public static void init(final TradingCards plugin) {
		CardManager.plugin = plugin;
		CardManager.blankCard = new ItemStack(TradingCardsConfig.getBlankCard(1));
		for(String rarity: plugin.getCardsConfig().getConfig().getConfigurationSection("Cards").getKeys(false)){
			for(String name: plugin.getCardsConfig().getConfig().getConfigurationSection("Cards."+rarity).getKeys(false)) {
				cards.put(rarity+"."+name, CardUtil.generateCard(name,rarity,false));
			}
		}

		plugin.getLogger().info(String.format("Loaded %d cards.",cards.size()));
		plugin.debug(StringUtils.join(cards.keySet(), ","));
	}

	public static Map<String,ItemStack> getCards(){
		return cards;
	}


	/**
	 *
	 * @param cardName
	 * @param rarity
	 * @return false if cards already exists. True if successfully added.
	 */
	public static boolean addCard(final String cardName, final String rarity){
		if(cards.containsKey(rarity+"."+cardName))
			return false;
		cards.put(rarity+"."+cardName, CardUtil.generateCard(cardName,rarity,false));
		return true;
	}

	public static ItemStack getCard(final String cardName,final String rarity, final boolean forcedShiny){
		if(cards.containsKey(rarity+"."+cardName))
			return cards.get(rarity+"."+cardName);
		return CardUtil.generateCard(cardName,rarity,forcedShiny);
	}

	public static ItemStack getCard(final String cardName,final String rarity, int num){
		ItemStack card = cards.get(rarity+"."+cardName);
		card.setAmount(num);
		return card;
	}

	public static ItemStack generatePack(final String name) {
		ItemStack boosterPack = TradingCardsConfig.getBlankBoosterPack();
		int numNormalCards = plugin.getConfig().getInt("BoosterPacks." + name + ".NumNormalCards");
		int numSpecialCards = plugin.getConfig().getInt("BoosterPacks." + name + ".NumSpecialCards");
		String prefix = plugin.getMainConfig().boosterPackPrefix;
		String normalCardColour = plugin.getConfig().getString("Colours.BoosterPackNormalCards");
		String extraCardColour = plugin.getConfig().getString("Colours.BoosterPackExtraCards");
		String loreColour = plugin.getMainConfig().boosterPackLoreColour;
		String nameColour = plugin.getMainConfig().boosterPackNameColour;
		String normalRarity = plugin.getConfig().getString("BoosterPacks." + name + ".NormalCardRarity");
		String specialRarity = plugin.getConfig().getString("BoosterPacks." + name + ".SpecialCardRarity");
		String extraRarity = "";
		int numExtraCards = 0;
		boolean hasExtraRarity = false;
		if (plugin.getConfig().contains("BoosterPacks." + name + ".ExtraCardRarity") && plugin.getConfig().contains("BoosterPacks." + name + ".NumExtraCards")) {
			hasExtraRarity = true;
			extraRarity = plugin.getConfig().getString("BoosterPacks." + name + ".ExtraCardRarity");
			numExtraCards = plugin.getConfig().getInt("BoosterPacks." + name + ".NumExtraCards");
		}

		String specialCardColour = plugin.getConfig().getString("Colours.BoosterPackSpecialCards");
		ItemMeta pMeta = boosterPack.getItemMeta();
		pMeta.setDisplayName(plugin.cMsg(prefix + nameColour + name.replaceAll("_", " ")));
		List<String> lore = new ArrayList<>();
		lore.add(plugin.cMsg(normalCardColour + numNormalCards + loreColour + " " + normalRarity.toUpperCase()));
		if (hasExtraRarity) {
			lore.add(plugin.cMsg(extraCardColour + numExtraCards + loreColour + " " + extraRarity.toUpperCase()));
		}

		lore.add(plugin.cMsg(specialCardColour + numSpecialCards + loreColour + " " + specialRarity.toUpperCase()));
		pMeta.setLore(lore);
		if (plugin.getMainConfig().hideEnchants) {
			pMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}

		boosterPack.setItemMeta(pMeta);
		boosterPack.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 10);
		return boosterPack;
	}


	public static class CardBuilder {
		private final String cardName;
		private String rarity;
		private boolean isShiny = false;
		private boolean isPlayerCard = false;
		private String rarityColour;
		private String prefix;
		private CardInfo series;
		private CardInfo about;
		private CardInfo type;
		private CardInfo info;
		private String shinyPrefix = null;
		private String cost;

		public CardBuilder(final String cardName) {
			this.cardName = cardName;
		}

		public CardBuilder isShiny(boolean isShiny){
			this.isShiny = isShiny;
			return this;
		}

		public CardBuilder rarityColour(String rarityColour){
			this.rarityColour = rarityColour;
			return this;
		}

		public CardBuilder prefix(String prefix){
			this.prefix = prefix;
			return this;
		}

		public CardBuilder series(String name, String colour, String display){
			this.series = new CardInfo(name,colour,display);
			return this;
		}

		public CardBuilder about(String name, String colour, String display){
			this.about = new CardInfo(name,colour,display);
			return this;
		}

		public CardBuilder type(String name, String colour, String display){
			this.type = new CardInfo(name,colour,display);
			return this;
		}

		public CardBuilder info(String name, String colour, String display){
			this.info = new CardInfo(name,colour,display);
			return this;
		}

		public CardBuilder shinyPrefix(String shinyPrefix){
			this.shinyPrefix = shinyPrefix;
			return this;
		}

		public CardBuilder cost(String cost){
			this.cost = cost;
			return this;
		}

		public CardBuilder rarity(String rarity){
			this.rarity = rarity;
			return this;
		}

		public CardBuilder isPlayerCard(boolean isPlayerCard){
			this.isPlayerCard = isPlayerCard;
			return this;
		}

		/**
		 * Saves the card to disk.
		 * @return Returns false if the card already exists, or if the rarity doesn't exist
		 */
		public boolean save(){
			//Checks should happen in the commands... this should be a stupid method, towny hooks should handle this as well.
			//This method should only return false if there was an io exception or something like that.
			if(plugin.getCardsConfig().getConfig().contains("Cards."+rarity+"."+cardName)) {
				plugin.debug("Card already exists. Try a different name?");
				return false;
			}
			if(!plugin.getCardsConfig().getConfig().contains("Cards."+rarity)){
				plugin.debug("No such rarity, try /cards rarities");
				return false;
			}

			//checks for formatting should be done through the command.
			return true;
		}


		public ItemStack build(){
			ItemStack card = blankCard.clone();
			ItemMeta cardMeta = blankCard.getItemMeta();
			cardMeta.setDisplayName(formatDisplayName(isPlayerCard,isShiny,prefix,rarityColour,cardName.replace('_',' '),cost,shinyPrefix));
			cardMeta.setLore(formatLore());
			if(isShiny) {
				cardMeta.addEnchant(Enchantment.ARROW_INFINITE,1,false);
			}
			if (plugin.getMainConfig().hideEnchants) {
				cardMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			}

			card.setItemMeta(cardMeta);
			return card;
		}

		private List<String> formatLore(){
			List<String> lore = new ArrayList<>();
			lore.add(plugin.cMsg(type.getColor() + type.getDisplay() + ": &f" + type.getName()));
			if (!"None".equals(info.getName()) && !"".equals(info.getName())) {
				lore.add(plugin.cMsg(info.getColor() + info.getDisplay() + ":"));
				lore.addAll(plugin.wrapString(info.getName()));
			} else {
				lore.add(plugin.cMsg(info.getColor() + info.getDisplay() + ": &f" + info.getName()));
			}

			lore.add(plugin.cMsg(series.getColor() + series.getDisplay() + ": &f" + series.getName()));
			if (plugin.getCardsConfig().getConfig().contains("Cards." + rarity + "." + cardName + ".About")) {
				lore.add(plugin.cMsg(about.getColor() + about.getDisplay() + ": &f" + about.getName()));
			}

			if (isShiny) {
				lore.add(plugin.cMsg(rarityColour + ChatColor.BOLD + plugin.getConfig().getString("General.Shiny-Name") + " " +rarity.replace('_',' ') ));
			} else {
				lore.add(plugin.cMsg(rarityColour + ChatColor.BOLD + rarity.replace('_',' ')));
			}

			return lore;
		}
		@NotNull
		private String formatDisplayName(boolean isPlayerCard, boolean isShiny, String prefix, String rarityColour, String cardName, String cost, String shinyPrefix) {
			final String[] shinyPlayerCardFormat = new String[]{"%PREFIX%", "%COLOUR%", "%NAME%", "%COST%", "%SHINYPREFIX%"};
			final String[] shinyCardFormat = new String[]{"%PREFIX%", "%COLOUR%", "%NAME%", "%COST%", "%SHINYPREFIX%", "_"};

			final String[] cardFormat = new String[]{"%PREFIX%","%COLOUR%","%NAME%","%COST%","_"};
			final String[] playerCardFormat = new String[]{"%PREFIX%","%COLOUR%","%NAME%","%COST%"};


			final String shinyTitle = plugin.getConfig().getString("DisplayNames.Cards.ShinyTitle");
			final String title = plugin.getConfig().getString("DisplayNames.Cards.Title");
			if (isShiny && shinyPrefix!= null) {
				if (isPlayerCard) {
					return plugin.cMsg(StringUtils.replaceEach(shinyTitle, shinyPlayerCardFormat, new String[]{prefix,rarityColour, cardName, cost, shinyPrefix}));
				}
				return plugin.cMsg(StringUtils.replaceEach(shinyTitle, shinyCardFormat, new String[]{prefix, rarityColour,cardName, cost, shinyPrefix, " "}));
			}
			if (isPlayerCard) {
				return plugin.cMsg(StringUtils.replaceEach(title, playerCardFormat, new String[]{prefix,rarityColour,cardName,cost}));
			}
			return plugin.cMsg(StringUtils.replaceEach(title, cardFormat, new String[]{prefix,rarityColour,cardName,cost, " "}));
		}

	}

	@Data
	public static class CardInfo {
		private final String name;
		private final String color;
		private final String display;
	}
}
