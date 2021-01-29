package net.sarhatabaot.tradingcards.card;

import net.sarhatabaot.tradingcards.rarity.RarityType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Card {
	private ItemStack itemStack; //cached
	private String name;
	private RarityType rarity;
	private CardType cardType;
	private boolean shiny;

	public Card(final String name, final RarityType rarity, final CardType cardType, final boolean hasShiny) {
		this.name = name;
		this.rarity = rarity;
		this.cardType = cardType;
		this.shiny = hasShiny;
	}

	public ItemStack toItem(){
		if(itemStack == null) {
			return makeItemStack();
		}
		return itemStack;
	}

	public String getName() {
		return name;
	}

	public RarityType getRarity() {
		return rarity;
	}

	public CardType getCardType() {
		return cardType;
	}

	public boolean isShiny() {
		return shiny;
	}

	//TODO, get from config here
	private String formatTitle() {
		return "[Card]"+name;
	}
	//[Card] <- Prefix name
	//Series
	//Rarity
	//additional lore
	private ItemStack makeItemStack(){
		//todo: get default config
		ItemStack item = new ItemStack(Material.PAPER);

		return item;
	}


}
