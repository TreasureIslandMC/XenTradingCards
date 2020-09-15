package media.xen.tradingcards.listeners;

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import media.xen.tradingcards.CardUtil;
import media.xen.tradingcards.TradingCards;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MythicMobsListener extends SimpleListener {
	private final Random r = plugin.r;

	public MythicMobsListener(TradingCards plugin) {
		super(plugin);
	}

	@EventHandler
	public void onMythicMobDeath(MythicMobDeathEvent e) {
		boolean drop = false;
		String worldName = "";
		List<String> worlds = new ArrayList<>();
		if (e.getKiller() instanceof Player) {
			Player p = (Player)e.getKiller();
			drop = (!this.plugin.isOnList(p) || this.plugin.blacklistMode() != 'b') && (!this.plugin.isOnList(p) && this.plugin.blacklistMode() == 'b' || this.plugin.isOnList(p) && this.plugin.blacklistMode() == 'w');
			worldName = p.getWorld().getName();
			worlds = this.plugin.getConfig().getStringList("World-Blacklist");
		}

		if (drop && !(worlds).contains(worldName)) {
			String rare = this.plugin.calculateRarity(e.getEntity().getType(), false);
			if (this.plugin.getConfig().getBoolean("PluginSupport.MythicMobs.Per-Level-Chances")) {
				ActiveMob mob = e.getMob();
				int level = mob.getLevel();
				String oldRarity = rare;
				rare = this.calculateMMRarity(level, false);
				debug("Mob is a mythic mob of level " + level + ", rarity changed from " + oldRarity + " to " + rare);
			} else {
				debug("Per-Level-Chances disabled, continuing as normal.");
			}

			if (this.plugin.getConfig().getBoolean("Chances.Boss-Drop") && this.plugin.isMobBoss(e.getEntity().getType())) {
				rare = this.plugin.getConfig().getString("Chances.Boss-Drop-Rarity");
			}

			boolean cancelled = false;
			if (!rare.equals("None")) {
				if (this.plugin.getConfig().getBoolean("General.Spawner-Block") && e.getEntity().getCustomName() != null && e.getEntity().getCustomName().equals(this.plugin.getConfig().getString("General.Spawner-Mob-Name"))) {
					debug("Mob came from spawner, not dropping card.");
					cancelled = true;
				}

				if (!cancelled) {
					debug("Successfully generated card.");
					boolean isShiny = false;
					int shinyRandom = this.r.nextInt(100) + 1;
					debug("Shiny chance for level " + e.getMobLevel() + " is " + this.plugin.getConfig().getInt("PluginSupport.MythicMobs.Levels." + (int)e.getMobLevel() + ".Shiny-Version-Chance"));
					if (shinyRandom <= this.plugin.getConfig().getInt("PluginSupport.MythicMobs.Levels." + e.getMobLevel() + ".Shiny-Version-Chance")) {
						debug("Card is shiny! Yay!");
						isShiny = true;
					}
					if (CardUtil.getRandomCard(rare, isShiny) != null) {
						e.getDrops().add(CardUtil.getRandomCard(rare, isShiny));
					}
				}
			}
		}

	}

	public String calculateMMRarity(int mobLvl, boolean alwaysDrop) {
		debug("Mythic mobs: Starting rarity calculation for level " + mobLvl + ", alwaysDrop is " + alwaysDrop);
		ConfigurationSection levels = this.plugin.getConfig().getConfigurationSection("PluginSupport.MythicMobs.Levels");
		Set<String> levelKeys = levels.getKeys(false);
		int finalLvl = 0;
		Iterator var6 = levelKeys.iterator();

		String type;
		while(var6.hasNext()) {
			type = (String)var6.next();
			int level = Integer.parseInt(type);
			if (level == mobLvl) {
				if (level >= finalLvl) {
					finalLvl = level;
				}

				debug("Mythic mobs: Correct level is: " + level);
			} else {
				if (level >= finalLvl && level <= mobLvl) {
					finalLvl = level;
				}

				debug("Mythic mobs: Not the correct level.. iteration is: " + level);
			}
		}

		int shouldItDrop = this.r.nextInt(100) + 1;
		type = "";
		debug("shouldItDrop Num: " + shouldItDrop);
		if (!alwaysDrop) {
			if (shouldItDrop > this.plugin.getConfig().getInt("PluginSupport.MythicMobs.Levels." + finalLvl + ".Drop-Chance")) {
				return "None";
			}

			type = "MythicMob";
		} else {
			type = "MythicMobs";
		}

		ConfigurationSection rarities = this.plugin.getConfig().getConfigurationSection("Rarities");
		Set<String> rarityKeys = rarities.getKeys(false);
		Map<Integer, String> rarityIndexes = new HashMap<>();
		int i = 0;
		int mini = 0;
		int random = this.r.nextInt(100000) + 1;
		debug("Random Card Num: " + random);
		debug("Type: " + type);

		for (final String key : rarityKeys) {
			rarityIndexes.put(i, key);
			++i;
			this.plugin.debug(i + ", " + key);
			if (this.plugin.getConfig().contains("PluginSupport.MythicMobs.Levels." + finalLvl + ".Rarities." + key) && mini == 0) {
				debug("Path exists: PluginSupport.MythicMobs.Levels." + finalLvl + ".Rarities." + key);
				debug("Mini: " + i);
				mini = i;
			}
		}

		int chance;
		if (mini != 0) {
			debug("Mini: " + mini);
			debug("i: " + i);

			while(i >= mini) {
				--i;
				debug("i: " + i);
				chance = this.plugin.getConfig().getInt("PluginSupport.MythicMobs.Levels." + finalLvl + ".Rarities." + rarityIndexes.get(i), -1);
				debug(" Chance: " + chance);
				debug("Rarity: " + rarityIndexes.get(i));
				if (chance > 0) {
					debug("Chance > 0");
					if (random <= chance) {
						debug("Random <= Chance, returning " + rarityIndexes.get(i));
						return rarityIndexes.get(i);
					}
				}
			}
		} else {
			while(i > 0) {
				--i;
				debug("Final loop iteration " + i);
				debug("Iteration " + i + " in HashMap is: " + rarityIndexes.get(i) + ", " + this.plugin.getConfig().getString("Rarities." + rarityIndexes.get(i) + ".Name"));
				chance = this.plugin.getConfig().getInt("PluginSupport.MythicMobs.Levels." + finalLvl + ".Rarities." + rarityIndexes.get(i), -1);
				debug("" + this.plugin.getConfig().getString("Rarities." + rarityIndexes.get(i) + ".Name") + "'s chance of dropping: " + chance + " out of 100,000");
				debug("The random number we're comparing that against is: " + random);
				if (chance > 0 && random <= chance) {
					debug("Yup, looks like " + random + " is definitely lower than " + chance + "!");
					debug("Giving a " + this.plugin.getConfig().getString("Rarities." + rarityIndexes.get(i) + ".Name") + " card.");
					return rarityIndexes.get(i);
				}
			}
		}

		return "None";
	}
}
