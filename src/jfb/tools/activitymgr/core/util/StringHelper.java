/*
 * Copyright (c) 2004, Jean-François Brazeau. All rights reserved.
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
package jfb.tools.activitymgr.core.util;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;


import org.apache.log4j.Logger;

/**
 * Classe offrant des services de manipulation de chaines de caractères.
 */
public class StringHelper {

	/** Logger */
	private static Logger log = Logger.getLogger(StringHelper.class);

	/** Tableau de caractères utilisé pour la transformation Hexadécimale */
	private static final char[] c = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/** Formatteur de date */
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	
	/**
	 * Convertit un octet en hexadécimal.
	 * @param b l'octet à convertir.
	 * @return la valeur hexadécimale.
	 */
	public static String toHex(byte b) {
		char[] result = new char[2];
		result[0] = c[(b>>4) & 0x0F];
		result[1] = c[b & 0x0F ];
		return new String(result);
	}

	/**
	 * Convertit une chaine hexadécimal en octet.
	 * @param hex la chaine à convertir.
	 * @return la valeur binaire.
	 */
	public static byte toByte(String hex) {
		return (byte) Integer.parseInt(hex, 16);
	}

	/**
	 * Convertit une date en chaine de caractère.
	 * @param cal la date à convertir.
	 * @return la date convertie.
	 */
	public static String toYYYYMMDD(Calendar cal) {
		return sdf.format(cal.getTime());
	}
	
	/**
	 * Convertit une valeur en centièmes en valeur au format
	 * de saisie.
	 * @param hundredth la valeur en centièmes.
	 * @return la valeur convertie au format de saisie.
	 */
	public static String hundredthToEntry(long hundredth) {
		StringBuffer buf = new StringBuffer(String.valueOf(hundredth));
		switch (buf.length()) {
			case 0 :
				buf.insert(0, "000");
				break;
			case 1 :
				buf.insert(0, "00");
				break;
			case 2 :
				buf.insert(0, "0");
				break;
		}
		// Insertion du point
		buf.insert(buf.length()-2, '.');
		// Retour du résultat
		return buf.toString();
	}

	/**
	 * Convertit une saisie utilisateur en centièmes.
	 * @param entry l'entrée de l'utilisateur.
	 * @return la valeur convertie en centièmes.
	 * @throws StringFormatException levé en cas de problème de format de la saisie.
	 */
	public static long entryToHundredth(String entry) throws StringFormatException {
		BigDecimal decimal = null;
		try { decimal = new BigDecimal(entry); }
		catch (NumberFormatException e) {
			log.debug("Wrong format", e);
			throw new StringFormatException("Wrong format (XXXXXX.XX)");
		}
		decimal = decimal.movePointRight(2);
		if (decimal.scale()>0)
			throw new StringFormatException("Too many digits");
		return decimal.longValue();
	}

}
