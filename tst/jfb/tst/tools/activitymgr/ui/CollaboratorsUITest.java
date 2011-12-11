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
package jfb.tst.tools.activitymgr.ui;

import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.ui.CollaboratorsUI;
import jfb.tools.activitymgr.ui.util.CfgMgr;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class CollaboratorsUITest {

	/** Logger */
	private static Logger log = Logger.getLogger(CollaboratorsUITest.class);

	public static void main(String[] args) {
		try {
			// Initialisation des logs et chargement de la config
			PropertyConfigurator.configure("cfg/log4j.properties");
			CfgMgr.load();
			// Initialisation de la connexion à la base de données
			String jdbcDriver = CfgMgr.get(CfgMgr.JDBC_DRIVER);
			String jdbcUrl = CfgMgr.get(CfgMgr.JDBC_URL);
			String jdbcUser = CfgMgr.get(CfgMgr.JDBC_USER);
			String jdbcPassword = CfgMgr.get(CfgMgr.JDBC_PASSWORD);
			ModelMgr.initDatabaseAccess(
					jdbcDriver,
					jdbcUrl,
					jdbcUser,
					jdbcPassword
				);
			

			// Ouverture de la denêtre
			Display display = new Display();
			Shell shell = new Shell(display);
			shell.setSize(500, 400);
			shell.setText("Collaborators");
			shell.setLayout(new FillLayout(SWT.VERTICAL));

			CollaboratorsUI ui = new CollaboratorsUI(shell);
			shell.open();

			ui.databaseOpened();
			log.debug("UI initialized");

			while (!shell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

}