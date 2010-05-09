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
package jfb.tools.activitymgr.ui.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Gestionnaire de la configuration.
 */
public class CfgMgr {

	/** Constantes associées au paramétrage */
	public static final String DATABASE_TYPE = "database.type";
	public static final String JDBC_DRIVER = "jdbc.driver";
	public static final String DATABASE_HOST = "database.host";
	public static final String DATABASE_PORT = "database.port";
	public static final String DATABASE_DATA_FILE = "datafile.name";
	public static final String DATABASE_NAME = "database.name";
	public static final String JDBC_URL = "jdbc.url";
	public static final String JDBC_USER = "jdbc.user";
	public static final String JDBC_PASSWORD = "jdbc.password";
	
	/** Nom du fichier de configuration */
	private static final String CFG_FILE = "cfg/activitymgr.properties";

	/** Configuration */
	private static Properties props = new Properties();
	
	/**
	 * Retourne la valeur associée à un paramètre.
	 * @param key le code de paramètre.
	 * @return la valeur du paramètre.
	 */
	public static String get(String key) {
		return props.getProperty(key);
	}

	/**
	 * Définit la valeur d'un paramètre.
	 * @param key le code de paramètre.
	 * @param value la nouvelle valeur du paramètre.
	 */
	public static void set(String key, String value) {
		props.setProperty(key, value);
	}
	
	/**
	 * Sauve le paramétrage.
	 * @throws IOException levé en cas d'incident I/O lors de l'accès en 
	 * 		écriture sur le fichier de configuration.
	 */
	public static void save() throws IOException {
		FileOutputStream out = new FileOutputStream(CFG_FILE);
		props.store(out, "Activity Manager configuration data");
		out.close();
	}

	/**
	 * Charge la configuration.
	 * @throws IOException levé en cas d'incident I/O lors de l'accès en 
	 * 		lecture sur le fichier de configuration.
	 */
	public static void load() throws IOException {
		props.clear();
		File cfgFile = new File(CFG_FILE);
		if (cfgFile.exists()) {
			FileInputStream in = new FileInputStream(CFG_FILE);
			props.load(in);
			in.close();
		}
	}

	/**
	 * Initie la configuration à partir d'un dioctionnaire spécifique.
	 * @param props le dictionnaire de propriétés.
	 */
	public static void init(Properties props) {
		CfgMgr.props = props;
	}

}
