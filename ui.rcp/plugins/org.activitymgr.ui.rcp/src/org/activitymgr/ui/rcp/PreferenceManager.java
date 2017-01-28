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
package org.activitymgr.ui.rcp;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/**
 * Preference manager.
 * 
 * @author jbrazeau
 *
 */
public class PreferenceManager extends AbstractPreferenceInitializer {

	/** Constantes associées au paramétrage */
	public static final String DATABASE_TYPE = "database.type"; //$NON-NLS-1$
	public static final String JDBC_DRIVER = "jdbc.driver"; //$NON-NLS-1$
	public static final String DATABASE_HOST = "database.host"; //$NON-NLS-1$
	public static final String DATABASE_PORT = "database.port"; //$NON-NLS-1$
	public static final String DATABASE_DATA_FILE = "datafile.name"; //$NON-NLS-1$
	public static final String DATABASE_NAME = "database.name"; //$NON-NLS-1$
	public static final String JDBC_URL = "jdbc.url"; //$NON-NLS-1$
	public static final String JDBC_USER = "jdbc.user"; //$NON-NLS-1$
	public static final String JDBC_PASSWORD = "jdbc.password"; //$NON-NLS-1$

	public void initializeDefaultPreferences() {
//		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
//		store.setDefault(DATABASE_TYPE, 1);
//		store.setDefault(JDBC_DRIVER, "org.hsqldb.jdbcDriver"); //$NON-NLS-1$
//		store.setDefault(DATABASE_HOST = "database.host"; //$NON-NLS-1$
//		store.setDefault(DATABASE_PORT = "database.port"; //$NON-NLS-1$
//		store.setDefault(DATABASE_DATA_FILE = "datafile.name"; //$NON-NLS-1$
//		store.setDefault(DATABASE_NAME = "database.name"; //$NON-NLS-1$
//		store.setDefault(JDBC_URL = "jdbc.url"; //$NON-NLS-1$
//		store.setDefault(JDBC_USER = "jdbc.user"; //$NON-NLS-1$
//		store.setDefault(JDBC_PASSWORD = "jdbc.password"; //$NON-NLS-1$
	}

}
