package media.xen.tradingcards.managers;

import lombok.Data;
import media.xen.tradingcards.CardUtil;
import media.xen.tradingcards.TradingCards;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

	public CardManager(final TradingCards plugin) {
		CardManager.plugin = plugin;
		init();
	}

	/**
	 * Pre-loads all existing cards.
	 */
	private void init() {
		CardManager.blankCard = new ItemStack(Material.getMaterial(plugin.getConfig().getString("General.Card-Material")));
		for(String rarity: plugin.getCardsConfig().getConfig().getConfigurationSection("Cards").getKeys(false)){
			for(String name: plugin.getCardsConfig().getConfig().getConfigurationSection("Cards."+rarity).getKeys(false)) {
				cards.put(rarity+"."+name, CardUtil.generateCard(name,rarity,false));
			}
		}

		plugin.getLogger().info(String.format("Loaded %d cards.",cards.size()));
		plugin.debug(StringUtils.join(cards.keySet(), ","));
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

	public static ItemStack getCard(final String cardName,final String rarity){
		if(cards.containsKey(rarity+"."+cardName))
			return cards.get(rarity+"."+cardName);
		return CardUtil.generateCard(cardName,rarity,false);
	}

	public static ItemStack getCard(final String cardName,final String rarity,final boolean forcedShiny){
		if(cards.containsKey(rarity+"."+cardName))
			return cards.get(rarity+"."+cardName);
		return CardUtil.generateCard(cardName,rarity,forcedShiny);
	}

	public static ItemStack getCard(final String cardName,final String rarity, int num){
		ItemStack card = cards.get(rarity+"."+cardName);
		card.setAmount(num);
		return card;
	}

	public static class CardBuilder implements Builder{
		private final String cardName;
		private String rarity;
		private boolean isShiny = false;
		private boolean isPlayerCard = false;
		private String rarityColour;
		private String prefix;
		private ItemInfo series;
		private ItemInfo about;
		private ItemInfo type;
		private ItemInfo info;
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
			this.series = new ItemInfo(name,colour,display);
			return this;
		}

		public CardBuilder about(String name, String colour, String display){
			this.about = new ItemInfo(name,colour,display);
			return this;
		}

		public CardBuilder type(String name, String colour, String display){
			this.type = new ItemInfo(name,colour,display);
			return this;
		}

		public CardBuilder info(String name, String colour, String display){
			this.info = new ItemInfo(name,colour,display);
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
			if (plugin.getConfig().getBoolean("General.Hide-Enchants", true)) {
				cardMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			}

			card.setItemMeta(cardMeta);
			return card;
		}

		private List<String> formatLore(){
			List<String> lore = new ArrayList<>();
			lore.add(plugin.cMsg(type.getColour() + type.getDisplay() + ": &f" + type.getName()));
			if (!"None".equals(info) && !"".equals(info)) {
				lore.add(plugin.cMsg(info.getColour() + info.getDisplay() + ":"));
				lore.addAll(plugin.wrapString(info.getName()));
			} else {
				lore.add(plugin.cMsg(info.getColour() + info.getDisplay() + ": &f" + info.getName()));
			}

			lore.add(plugin.cMsg(series.getColour() + series.getDisplay() + ": &f" + series.getName()));
			if (plugin.getCardsConfig().getConfig().contains("Cards." + rarity + "." + cardName + ".About")) {
				lore.add(plugin.cMsg(about.getColour() + about.getDisplay() + ": &f" + about.getName()));
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
}
