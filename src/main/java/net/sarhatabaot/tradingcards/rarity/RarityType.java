package net.sarhatabaot.tradingcards.rarity;

import org.bukkit.Color;

public enum RarityType {
	COMMON("Common",Color.WHITE), //white
	UNCOMMON("Uncommon",Color.fromRGB(25,140,25)), //green
	RARE("Rare",Color.fromRGB(25,97,178)), //blue
	EPIC("Epic",Color.fromRGB(115,0,115)), //purple
	LEGENDARY("Legendary",Color.fromRGB(255,215,0)), //gold
	SPECIAL("Special",Color.fromRGB(165,42,42)), //brownish
	;
	private final String name;
	private final Color color;

	RarityType(final String name, final Color color) {
		this.name = name;
		this.color = color;
	}

	public String getName() {
		return name;
	}

	public Color getColor() {
		return color;
	}
}
