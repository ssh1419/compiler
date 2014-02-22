package boa.functions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Some less trivial casts provided by Boa.
 * 
 * @author anthonyu
 * 
 */
public class BoaCasts {
	/**
	 * Convert a {@link String} into a boolean.
	 * 
	 * @param s
	 *            The {@link String} to be converted
	 * 
	 * @return True iff <em>s</em> begins with 'T' or 't'
	 * 
	 */
	public static boolean stringToBoolean(final String s) {
		final char c = s.charAt(0);

		if (c == 'T' || c == 't')
			return true;

		return false;
	}

	/**
	 * Convert a boolean into a long.
	 * 
	 * @param b
	 *            The boolean to be converted
	 * 
	 * @return A long representing the boolean value <em>b</em>
	 */
	public static long booleanToLong(final boolean b) {
		if (b)
			return 1;
		return 0;
	}

	/**
	 * Parse a time string.
	 * 
	 * @param s
	 *            A {@link String} containing a time
	 * 
	 * @return A long containing the time represented by <em>s</em>.
	 * 
	 * @throws ParseException
	 */
	public static long stringToTime(final String s, final String tz) throws ParseException {
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy");

		simpleDateFormat.setCalendar(Calendar.getInstance(TimeZone.getTimeZone(tz)));

		return simpleDateFormat.parse(s).getTime() * 1000;
	}

	/**
	 * Parse a time string.
	 * 
	 * @param s
	 *            A {@link String} containing a time
	 * 
	 * @return A long containing the time represented by <em>s</em>.
	 * 
	 * @throws ParseException
	 */
	public static long stringToTime(final String s) throws ParseException {
		return BoaCasts.stringToTime(s, "PST8PDT");
	}

	/**
	 * Format a long into a {@link String} in the given radix.
	 * 
	 * @param l
	 *            A long
	 * 
	 * @param radix
	 *            The desired radix
	 * 
	 * @return A {@link String} containing the number <em>l</em> in base
	 *         <em>radix<em>
	 */
	public static String longToString(final long l, final long radix) {
		return Long.toString(l, (int) radix);
	}

	/**
	 * Format a time string.
	 * 
	 * @param t
	 *            A long containing a time
	 * 
	 * @param tz
	 *            A String containing the time zone to be used for formatting
	 * 
	 * @return A {@link String} containing the time represented by <em>t</em>.
	 * 
	 */
	public static String timeToString(final long t, final String tz) {
		final SimpleDateFormat boaDateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy");

		final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(tz));

		calendar.setTimeInMillis(t / 1000);

		boaDateFormat.setCalendar(calendar);

		return boaDateFormat.format(calendar.getTime());
	}

	/**
	 * Format a time string.
	 * 
	 * @param t
	 *            A long containing a time
	 * 
	 * @return A {@link String} containing the time represented by <em>t</em>.
	 * 
	 */
	public static String timeToString(final long t) {
		return BoaCasts.timeToString(t, "PST8PDT");
	}
}