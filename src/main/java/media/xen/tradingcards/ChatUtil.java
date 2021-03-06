package media.xen.tradingcards;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import static media.xen.tradingcards.TradingCards.sendMessage;

public class ChatUtil {
	private static TradingCards plugin;

	public static void init(final TradingCards plugin){
		ChatUtil.plugin = plugin;
	}

	public static void sendPrefixedMessage(final CommandSender toWhom, final String message) {
		sendMessage(toWhom, plugin.getPrefixedMessage(message));
	}

	public static void sendMessage(final CommandSender toWhom, final String message) {
		toWhom.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
	}
}
