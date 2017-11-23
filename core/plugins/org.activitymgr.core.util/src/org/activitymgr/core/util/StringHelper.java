/*
 * Copyright (c) 2004-2017, Jean-Francois Brazeau. All rights reserved.
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
package org.activitymgr.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.log4j.Logger;

/**
 * Classe offrant des services de manipulation de chaines de caractères.
 */
public class StringHelper {

	/** Max base32 value (1024) */
	private static final int MAX_BASE32_VALUE = 0x400;

	/** Logger */
	private static Logger log = Logger.getLogger(StringHelper.class);

	/** Tableau de caractères utilisé pour la transformation Hexadécimale */
	private static final char[] HEX_CARS = new char[] { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/** Base 32 encoding characters array */
	private static char BASE32_CARS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
		'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V' };

	/** Formatteur de date */
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); //$NON-NLS-1$

	/**
	 * Convertit un octet en hexadécimal.
	 * 
	 * @param b
	 *            l'octet à convertir.
	 * @return la valeur hexadécimale.
	 */
	public static String toHex(byte b) {
		char[] result = new char[2];
		result[0] = HEX_CARS[(b >> 4) & 0x0F];
		result[1] = HEX_CARS[b & 0x0F];
		return new String(result);
	}

	/**
	 * Encodes a value in Base 32.
	 * <p>The given value must be less than <code>0x400</code> (=<code>1024</code>) to ensure that the result is 2 characters long.
	 * 
	 * @param value
	 *            the value to encode.
	 * @return the encoded value.
	 */
	public static String toBase32(int value) {
		if (value >= MAX_BASE32_VALUE) {
			throw new IllegalArgumentException("Value is greater than " + MAX_BASE32_VALUE);
		}
		char[] result = new char[2];
		result[0] = BASE32_CARS[(value >> 5) & 0x1F];
		result[1] = BASE32_CARS[value & 0x1F];
		return new String(result);
	}
	
	/**
	 * Decodes a base 32 string.
	 * <p>
	 * The string is expected to be 2 characters long.
	 * </p>
	 * 
	 * @param str
	 *            the string to decode.
	 * @return the decoded value.
	 */
	public static int fromBase32(String str) {
		char[] cars = str.toCharArray();
		if (cars.length != 2) {
			throw new IllegalArgumentException("Base 32 value is expected to be 2 characters long");
		}
		return (decodeBase32Digit((byte) cars[0]) << 5)
				+ decodeBase32Digit((byte) cars[1]);
	}

	private static int decodeBase32Digit(byte data) {
		char charData = (char) data;
		if (charData <= 'V' && charData >= 'A')
			return charData - 'A' + 10;
		if (charData <= '9' && charData >= '0')
			return charData - '0';
		throw new IllegalArgumentException("Invalid base 32 char to decode: " + data); //$NON-NLS-1$
	}
	
	/**
	 * Convertit une chaine hexadécimal en octet.
	 * 
	 * @param hex
	 *            la chaine à convertir.
	 * @return la valeur binaire.
	 */
	public static byte toByte(String hex) {
		return (byte) Integer.parseInt(hex, 16);
	}

	/**
	 * Convertit une date en chaine de caractère.
	 * 
	 * @param cal
	 *            la date à convertir.
	 * @return la date convertie.
	 */
	public static String toYYYYMMDD(Calendar cal) {
		return sdf.format(cal.getTime());
	}

	/**
	 * Convertit une valeur en centièmes en valeur au format de saisie.
	 * 
	 * @param hundredth
	 *            la valeur en centièmes.
	 * @return la valeur convertie au format de saisie.
	 */
	public static String hundredthToEntry(long hundredth) {
		StringBuffer buf = new StringBuffer(String.valueOf(hundredth));
		switch (buf.length()) {
		case 0:
			buf.insert(0, "000"); //$NON-NLS-1$
			break;
		case 1:
			buf.insert(0, "00"); //$NON-NLS-1$
			break;
		case 2:
			buf.insert(0, "0"); //$NON-NLS-1$
			break;
		}
		// Insertion du point
		buf.insert(buf.length() - 2, '.');
		// Retour du résultat
		return buf.toString();
	}

	/**
	 * Convertit une saisie utilisateur en centièmes.
	 * 
	 * @param entry
	 *            l'entrée de l'utilisateur.
	 * @return la valeur convertie en centièmes.
	 * @throws StringFormatException
	 *             levé en cas de problème de format de la saisie.
	 */
	public static long entryToHundredth(String entry)
			throws StringFormatException {
		BigDecimal decimal = null;
		try {
			decimal = new BigDecimal(entry);
		} catch (NumberFormatException e) {
			log.debug("Wrong format", e); //$NON-NLS-1$
			throw new StringFormatException(
					Strings.getString("StringHelper.errors.WRONG_NUMBER_FORMAT")); //$NON-NLS-1$
		}
		decimal = decimal.movePointRight(2);
		if (decimal.scale() > 0)
			throw new StringFormatException(
					Strings.getString("StringHelper.errors.TOO_MANY_DIGITS")); //$NON-NLS-1$
		return decimal.longValue();
	}

	/**
	 * Retourne le contenu d'un flux sous forme d'une chaîne de caractères.
	 * 
	 * @param in
	 *            le flux de lecture.
	 * @return la chaîne de caractère.
	 * @throws IOException
	 *             levé en cas d'incident I/O.
	 */
	public static String fromInputStream(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int n = -1;
		byte[] buf = new byte[1024];
		while ((n = in.read(buf)) > 0) {
			out.write(buf, 0, n);
		}
		in.close();
		return new String(out.toByteArray());
	}

	/**
	 * Découpe un script pour en extraire les requêtes SQL.
	 * 
	 * @param script
	 *            le script à découper.
	 * @return les requêtes.
	 */
	public static String[] getQueries(String script) {
		ArrayList<String> queries = new ArrayList<String>();
		LineNumberReader lnr = new LineNumberReader(new StringReader(
				script.trim()));
		StringBuffer buf = new StringBuffer();
		boolean proceed = true;
		do {
			String line = null;
			// On ne lit dans le flux que si la ligne courante n'est pas
			// encore totalement traitée
			if (line == null) {
				try {
					line = lnr.readLine();
				} catch (IOException e) {
					log.debug(
							"Unexpected I/O error while reading memory stream!", e); //$NON-NLS-1$
					throw new Error(
							Strings.getString("StringHelper.errors.MEMORY_IO_FAILURE"), null); //$NON-NLS-1$
				}
				log.debug("Line read : '" + line + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// Si le flux est vide, on sort de la boucle
			if (line == null) {
				proceed = false;
			}
			// Sinon on traite la ligne
			else {
				line = line.trim();
				// Si la ligne est un commentaire on l'ignore
				if (line.startsWith("--")) { //$NON-NLS-1$
					line = null;
				} else {
					// Sinon on regarde si la ligne possède
					// un point virgule
					int idx = line.indexOf(';');
					// Si c'est le cas, on découpe la chaîne et on
					// exécute la requête
					if (idx >= 0) {
						buf.append(line.subSequence(0, idx));
						line = line.substring(idx);
						String sql = buf.toString();
						buf.setLength(0);
						log.debug(" - sql='" + sql + "'"); //$NON-NLS-1$ //$NON-NLS-2$
						if (!"".equals(sql)) //$NON-NLS-1$
							queries.add(sql);
					}
					// sinon on ajoute la ligne au buffer de requête
					else {
						buf.append(line);
						buf.append('\n');
					}
				}
			}

		} while (proceed);
		// Ajout de la dernière requête (éventuellement)
		if (buf.length() != 0)
			queries.add(buf.toString());
		return (String[]) queries.toArray(new String[queries.size()]);
	}

	/**
	 * Turn the first character to upper case.
	 * 
	 * @param s
	 *            the string to convert.
	 * @return the resulting string.
	 */
	public static String toLowerFirst(String s) {
		String result = null;
		if (s != null && s.length() > 0) {
			char[] cars = s.toCharArray();
			cars[0] = Character.toUpperCase(cars[0]);
			result = new String(cars);
		}
		return result;
	}

}
