package net.sarhatabaot.tradingcards.series;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author sarhatabaot
 */
class ScheduleTest {
	private Schedule schedule = new Schedule(true,
			new Date(121, Calendar.JANUARY,20), /*TODO */
			new Date(121, Calendar.MARCH,25));

	@Test
	void serialize() {
		//compare the expected map with the result one
		Map<String,Object> map = new HashMap<>();
		map.put("enabled",true);
		map.put("startDate","2021-01-20 00:00");
		map.put("endDate","2021-03-25 00:00");
		assertEquals(map,schedule.serialize());
	}

}