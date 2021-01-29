package net.sarhatabaot.tradingcards.series;


import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Schedule implements ConfigurationSerializable {
	private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private final boolean enabled;
	private final Date startDate;
	private final Date endDate;

	public Schedule(final boolean enabled, final Date startDate, final Date endDate) {
		this.enabled = enabled;
		this.startDate = startDate;
		this.endDate = endDate;
	}


	@NotNull
	@Override
	public Map<String, Object> serialize() {
		Map<String,Object> map = new HashMap<>();
		map.put("enabled",enabled);
		map.put("startDate",dateFormat.format(startDate));
		map.put("endDate",dateFormat.format(endDate));
		return map;
	}

	public Schedule(Map<String, Object> map) throws ParseException {
		this.enabled = (boolean)map.get("enabled");
		this.startDate = DateFormat.getInstance().parse((String)map.get("startDate"));
		this.endDate = DateFormat.getInstance().parse((String)(map.get("endDate")));
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}
}
