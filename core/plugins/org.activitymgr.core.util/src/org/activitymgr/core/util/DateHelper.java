package org.activitymgr.core.util;

import java.util.Calendar;

/**
 * Date manipulation helper class.
 * @author jbrazeau
 *
 */
public class DateHelper {

	/**
	 * @param date1
	 *            the first date.
	 * @param date2
	 *            the second date.
	 * @return the days count between the two dates.
	 */
	public static int countDaysBetween(Calendar date1, Calendar date2) {
		Calendar from = date1;
		Calendar to = date2;
		if (date1.after(date2)) {
			from = date2;
			to = date1;
		}
		int y1 = from.get(Calendar.YEAR);
		int y2 = to.get(Calendar.YEAR);
		// If both dates are within the same year, we only have to compare
		// "day of year" fields
		if (y1 == y2) {
			return to.get(Calendar.DAY_OF_YEAR)
					- from.get(Calendar.DAY_OF_YEAR);
		}
		// In other cases, we have to increment a cursor to count how many days
		// there is
		// between the current date and the 31th Dec. until the current year is
		// equal to
		// the target date (because not all years have 365 days, some have
		// 366!).
		else {
			int result = 0;
			Calendar fromClone = (Calendar) from.clone();
			while (fromClone.get(Calendar.YEAR) != y2) {
				// Save current day of year
				int dayOfYear = fromClone.get(Calendar.DAY_OF_YEAR);
				// Goto 31th of December
				fromClone.set(Calendar.MONTH, 11);
				fromClone.set(Calendar.DAY_OF_MONTH, 31);
				// Compute days count
				result += fromClone.get(Calendar.DAY_OF_YEAR) - dayOfYear;
				// Goto next year (= add one day)
				fromClone.add(Calendar.DATE, 1);
				result++;
			}
			// Compute last year days count
			result += to.get(Calendar.DAY_OF_YEAR)
					- fromClone.get(Calendar.DAY_OF_YEAR);
			return result;
		}
	}

}
