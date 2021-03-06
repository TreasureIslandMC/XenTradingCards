package media.xen.tradingcards.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import media.xen.tradingcards.CardUtil;
import media.xen.tradingcards.ChatUtil;
import media.xen.tradingcards.DeckManager;
import media.xen.tradingcards.TradingCards;
import org.apache.commons.lang.Validate;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import static media.xen.tradingcards.TradingCards.sendMessage;

@CommandAlias("deck|decks")
public class DeckCommand extends BaseCommand {
	private TradingCards plugin;

	public DeckCommand(final TradingCards plugin) {
		this.plugin = plugin;
	}


	@Default
	@CommandPermission("cards.decks.open")
	@Description("Get a deck item. Or opens a deck.")
	public void onGetDeck(final Player player, final int deckNumber) {
		Validate.notNull(player, "Cannot run this command from console, or there was a problem getting the player object.");

		if (!player.hasPermission("cards.decks." + deckNumber)) {
			sendMessage(player, plugin.getPrefixedMessage(plugin.getMessagesConfig().maxDecks));
			return;
		}

		if (plugin.getMainConfig().useDeckItems) {
			if (!DeckManager.hasDeck(player, deckNumber)) {
				CardUtil.dropItem(player, DeckManager.createDeck(player, deckNumber));
			} else {
				ChatUtil.sendPrefixedMessage(player, plugin.getMessagesConfig().alreadyHaveDeck);
			}
			return;
		}

		if (player.getGameMode() == GameMode.CREATIVE) {
			if (plugin.getMainConfig().decksInCreative) {
				DeckManager.openDeck(player, deckNumber);
				return;
			}
			ChatUtil.sendPrefixedMessage(player, plugin.getMessagesConfig().deckCreativeError);
			return;
		}
		DeckManager.openDeck(player, deckNumber);

	}
}
