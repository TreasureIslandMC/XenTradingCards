package net.sarhatabaot.tradingcards.series;

import net.sarhatabaot.tradingcards.card.Card;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;


@ConfigSerializable
public class Series implements ConfigurationSerializable {
	private String name;
	private Schedule schedule;
	private boolean active;
	private List<Card> cards;

	public Series(final String name, final Schedule schedule, final boolean active, final List<Card> cards) {
		this.name = name;
		this.schedule = schedule;
		this.active = active;
		this.cards = cards;
	}

	@NotNull
	@Override
	public Map<String, Object> serialize() {
		return null;
	}

	public Series (Map<String,Object> map ){

	}

	public String getName() {
		return name;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public boolean isActive() {
		return active;
	}

	public List<Card> getCards() {
		return cards;
	}

	/**
	 * @return true if the series is enabled & within the time constraints
	 */
	public boolean isEnabledAndInSchedule() {
		Date now = convertToDateViaInstant(LocalDate.now());
		return active && isWithinRange(now,schedule.getStartDate(),schedule.getEndDate());
	}

	private boolean isWithinRange(Date date, Date startDate, Date endDate) {
		return date.after(startDate) && date.before(endDate);
	}

	private Date convertToDateViaInstant(LocalDate dateToConvert) {
		return Date.from(dateToConvert.atStartOfDay()
				.atZone(ZoneId.systemDefault())
				.toInstant());
	}
}
