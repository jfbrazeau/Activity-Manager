/*
 * Copyright (c) 2004-2012, Jean-Francois Brazeau. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jfb.tools.activitymgr.report;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jfb.tools.activitymgr.core.util.Strings;

import org.apache.log4j.Logger;

/**
 * Classe offrant des services de manipulation de date.
 */
public class DateHelper {

	/** Logger */
	private static Logger log = Logger.getLogger(DateHelper.class);

	/** Formatteurs de date */
	private Map<String, SimpleDateFormat> dateFormats = Collections.synchronizedMap(new HashMap<String, SimpleDateFormat>());

	/**
	 * Retourne le formatteur de date associ� � un format.
	 * 
	 * @param format
	 *            le format de date.
	 * @return le formatteur de date.
	 */
	private SimpleDateFormat getDateFormat(String format) {
		SimpleDateFormat sdf = (SimpleDateFormat) dateFormats.get(format);
		if (sdf == null) {
			sdf = new SimpleDateFormat(format);
			dateFormats.put(format, sdf);
		}
		return sdf;
	}

	/**
	 * Convertit une cha�ne de caract�res au format YYYYMMDD en date.
	 * 
	 * @param yyyyMMdd
	 *            la date au format YYYYMMDD.
	 * @return la date convertie.
	 * @throws ParseException
	 *             lev� en cas de probl�me de format de la cha�ne.
	 */
	public Calendar toDate(String yyyyMMdd) throws ParseException {
		return toDate("yyyyMMdd", yyyyMMdd); //$NON-NLS-1$
	}

	/**
	 * Convertit une cha�ne de caract�res au format sp�cifi� en date.
	 * 
	 * @param format
	 *            le format de date.
	 * @param date
	 *            la chapine de caract�res.
	 * @return la date convertie.
	 * @throws ParseException
	 *             lev� en cas de probl�me de format de la cha�ne.
	 */
	public Calendar toDate(String format, String date) throws ParseException {
		Calendar _date = new GregorianCalendar();
		_date.setTime(getDateFormat(format).parse(date));
		log.debug("toDate(" + format + ", " + date + ")=" + _date); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return _date;
	}

	/**
	 * Convertit une date au format sp�cifi�.
	 * 
	 * @param format
	 *            le format de date.
	 * @param date
	 *            la date � convertir.
	 * @return la date convertie.
	 */
	public String toString(String format, Calendar date) {
		return getDateFormat(format).format(date.getTime());
	}

	/**
	 * Convertit une date au format YYYYMMDD.
	 * 
	 * @param date
	 *            la date � convertir.
	 * @return la date convertie.
	 */
	public String toYYYYMMDD(Calendar date) {
		return toString("yyyyMMdd", date); //$NON-NLS-1$
	}

	/**
	 * Retourne l'ann�e associ�e � une date.
	 * 
	 * @param date
	 *            la date.
	 * @return l'ann�e.
	 */
	public Integer getYear(Calendar date) {
		log.debug("getYear()"); //$NON-NLS-1$
		return new Integer(date.get(Calendar.YEAR));
	}

	/**
	 * Retourne le mois associ� � une date.
	 * 
	 * @param date
	 *            la date.
	 * @return le mois.
	 */
	public Integer getMonth(Calendar date) {
		log.debug("getMonth()"); //$NON-NLS-1$
		return new Integer(date.get(Calendar.MONTH) + 1);
	}

	/**
	 * Retourne le jour associ� � une date.
	 * 
	 * @param date
	 *            la date.
	 * @return le jour.
	 */
	public Integer getDay(Calendar date) {
		log.debug("getDay()"); //$NON-NLS-1$
		return new Integer(date.get(Calendar.DATE));
	}

	/**
	 * Construit un interval de dates entre deux dates sp�cifi�es dont la
	 * granularit� est le jour.
	 * 
	 * @param fromDate
	 *            date de d�part de l'interval.
	 * @param toDate
	 *            date de fin de l'interval.
	 * @return l'interval de dates.
	 */
	public static Calendar[] buildDayInterval(Calendar fromDate, Calendar toDate) {
		return buildInterval(fromDate, toDate, Calendar.DATE);
	}

	/**
	 * Construit un interval de dates entre deux dates sp�cifi�es dont la
	 * granularit� est le mois.
	 * 
	 * @param fromDate
	 *            date de d�part de l'interval.
	 * @param toDate
	 *            date de fin de l'interval.
	 * @return l'interval de dates.
	 */
	public static Calendar[] buildMonthInterval(Calendar fromDate,
			Calendar toDate) {
		return buildInterval(fromDate, toDate, Calendar.MONTH);
	}

	/**
	 * Construit un interval de dates entre deux dates sp�cifi�es dont la
	 * granularit� est sp�cifi�e en param�tre.
	 * 
	 * @param fromDate
	 *            date de d�part de l'interval.
	 * @param toDate
	 *            date de fin de l'interval.
	 * @param dateIncrementType
	 *            granularit� de l'interval.
	 * @return l'interval de dates.
	 */
	private static Calendar[] buildInterval(Calendar fromDate, Calendar toDate,
			int dateIncrementType) {
		log.debug("buildInterval(" + fromDate + ", " + toDate + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (dateIncrementType != Calendar.DATE
				&& dateIncrementType != Calendar.MONTH)
			throw new Error(
					Strings.getString("DateHelper.errors.UNKNOWN_DATE_INCREMENT_TYPE")); //$NON-NLS-1$
		List<Calendar> list = new ArrayList<Calendar>();
		Calendar cal = (Calendar) toDate.clone();
		cal.add(dateIncrementType, 1);
		if (dateIncrementType == Calendar.MONTH)
			cal.add(Calendar.DATE, -1);
		long toDateInMillis = cal.getTimeInMillis();
		cal = (Calendar) fromDate.clone();
		while (cal.getTimeInMillis() < toDateInMillis) {
			list.add((Calendar) cal.clone());
			cal.add(dateIncrementType, 1);
		}
		return (Calendar[]) list.toArray(new Calendar[list.size()]);
	}

	/**
	 * Retourne le dernier jour du mois de la date sp�cifi�e.
	 * 
	 * @param date
	 *            la date associ� au mois dont on veut conna�tre le dernier
	 *            jour.
	 * @return le dernier jour du mois.
	 */
	public Calendar lastDayInMonth(Calendar date) {
		Calendar lastMonthDate = (Calendar) date.clone();
		lastMonthDate.add(Calendar.MONTH, 1);
		lastMonthDate.add(Calendar.DATE, -1);
		return lastMonthDate;
	}

}
