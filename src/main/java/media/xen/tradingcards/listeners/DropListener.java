package media.xen.tradingcards.listeners;


import media.xen.tradingcards.managers.CardManager;
import media.xen.tradingcards.CardUtil;
import media.xen.tradingcards.TradingCards;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;
import java.util.Set;

public class DropListener extends SimpleListener {
	private List<String> worlds;
	public DropListener(final TradingCards plugin) {
		super(plugin);
		worlds = plugin.getConfig().getStringList("World-Blacklist");
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (plugin.getConfig().getBoolean("General.Player-Drops-Card") && plugin.getConfig().getBoolean("General.Auto-Add-Players")) {
			Player player = e.getEntity().getKiller();
			if (player != null) {
				ConfigurationSection rarities = plugin.getConfig().getConfigurationSection("Rarities");
				Set<String> rarityKeys = rarities.getKeys(false);
				String k = null;

				for (final String key : rarityKeys) {
					if(plugin.getCardsConfig().getConfig().contains("Cards." + key + "." + e.getEntity().getName())) {
						plugin.debug(key);
						k = key;
					}
				}

				if (k != null) {
					int rndm = plugin.r.nextInt(100) + 1;
					if (rndm <= plugin.getConfig().getInt("General.Player-Drops-Card-Rarity")) {
						e.getDrops().add(CardManager.getCard(e.getEntity().getName(),k));
						plugin.debug(e.getDrops().toString());
					}
				} else {
					plugin.getLogger().info("k is null");
				}
			}
		}

	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent e) {
		final Player p = e.getEntity().getKiller();
		if(p == null)
			return;

		boolean drop = ((!plugin.isOnList(p) || plugin.blacklistMode() != 'b') && ((!plugin.isOnList(p) && plugin.blacklistMode() == 'b') || (plugin.isOnList(p) && plugin.blacklistMode() == 'w')));
		String worldName = e.getEntity().getLocation().getWorld().getName();
		if (drop && !worlds.contains(worldName)) {

			String rare = plugin.calculateRarity(e.getEntityType(), false);
			if (plugin.getConfig().getBoolean("Chances.Boss-Drop") && plugin.isMobBoss(e.getEntityType())) rare = plugin.getConfig().getString("Chances.Boss-Drop-Rarity");
			boolean cancelled = false;

			if (!rare.equals("None")) {
				if (plugin.getConfig().getBoolean("General.Spawner-Block") && e.getEntity().getCustomName() != null && e.getEntity().getCustomName().equals(plugin.getConfig().getString("General.Spawner-Mob-Name"))) {

					plugin.debug("Mob came from spawner, not dropping card.");
					cancelled = true;
				}
				if (!cancelled) {
					plugin.debug("Successfully generated card.");
					e.getDrops().add(CardUtil.getRandomCard(rare, false));
				}
			}
		}
	}
}
