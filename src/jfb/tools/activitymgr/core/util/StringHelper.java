/*
 * Copyright (c) 2004-2010, Jean-Fran�ois Brazeau. All rights reserved.
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
 * Classe offrant des services de manipulation de chaines de caract�res.
 */
public class StringHelper {

	/** Logger */
	private static Logger log = Logger.getLogger(StringHelper.class);

	/** Tableau de caract�res utilis� pour la transformation Hexad�cimale */
	private static final char[] c = new char[] { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/** Formatteur de date */
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); //$NON-NLS-1$

	/**
	 * Convertit un octet en hexad�cimal.
	 * 
	 * @param b
	 *            l'octet � convertir.
	 * @return la valeur hexad�cimale.
	 */
	public static String toHex(byte b) {
		char[] result = new char[2];
		result[0] = c[(b >> 4) & 0x0F];
		result[1] = c[b & 0x0F];
		return new String(result);
	}

	/**
	 * Convertit une chaine hexad�cimal en octet.
	 * 
	 * @param hex
	 *            la chaine � convertir.
	 * @return la valeur binaire.
	 */
	public static byte toByte(String hex) {
		return (byte) Integer.parseInt(hex, 16);
	}

	/**
	 * Convertit une date en chaine de caract�re.
	 * 
	 * @param cal
	 *            la date � convertir.
	 * @return la date convertie.
	 */
	public static String toYYYYMMDD(Calendar cal) {
		return sdf.format(cal.getTime());
	}

	/**
	 * Convertit une valeur en centi�mes en valeur au format de saisie.
	 * 
	 * @param hundredth
	 *            la valeur en centi�mes.
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
		// Retour du r�sultat
		return buf.toString();
	}

	/**
	 * Convertit une saisie utilisateur en centi�mes.
	 * 
	 * @param entry
	 *            l'entr�e de l'utilisateur.
	 * @return la valeur convertie en centi�mes.
	 * @throws StringFormatException
	 *             lev� en cas de probl�me de format de la saisie.
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
	 * Retourne le contenu d'un flux sous forme d'une cha�ne de caract�res.
	 * 
	 * @param in
	 *            le flux de lecture.
	 * @return la cha�ne de caract�re.
	 * @throws IOException
	 *             lev� en cas d'incident I/O.
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
	 * D�coupe un script pour en extraire les requ�tes SQL.
	 * 
	 * @param script
	 *            le script � d�couper.
	 * @return les requ�tes.
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
			// encore totalement trait�e
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
					// Sinon on regarde si la ligne poss�de
					// un point virgule
					int idx = line.indexOf(';');
					// Si c'est le cas, on d�coupe la cha�ne et on
					// ex�cute la requ�te
					if (idx >= 0) {
						buf.append(line.subSequence(0, idx));
						line = line.substring(idx);
						String sql = buf.toString();
						buf.setLength(0);
						log.debug(" - sql='" + sql + "'"); //$NON-NLS-1$ //$NON-NLS-2$
						if (!"".equals(sql)) //$NON-NLS-1$
							queries.add(sql);
					}
					// sinon on ajoute la ligne au buffer de reque�te
					else {
						buf.append(line);
						buf.append('\n');
					}
				}
			}

		} while (proceed);
		// Ajout de la derni�re requ�te (�ventuellement)
		if (buf.length() != 0)
			queries.add(buf.toString());
		return (String[]) queries.toArray(new String[queries.size()]);
	}

}
