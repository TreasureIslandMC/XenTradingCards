package media.xen.tradingcards.managers;

import org.bukkit.inventory.ItemStack;

public interface Builder {
	/**
	 * Saves to disk.
	 * @return Return true if saved with no issues. False if I/O exception or something else.
	 */
	boolean save();
	ItemStack build();
}
