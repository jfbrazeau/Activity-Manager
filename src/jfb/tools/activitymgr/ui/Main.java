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
package jfb.tools.activitymgr.ui;


import java.net.URL;

import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener;
import jfb.tools.activitymgr.ui.util.CfgMgr;
import jfb.tools.activitymgr.ui.util.SafeRunner;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * Classe principale de l'application des gestion des l'activité.
 */
public class Main {

	/** Logger */
	private static Logger log = Logger.getLogger(Main.class);
	
	/** Onglets */
	private static TabItem databaseTab;
	private static TabItem durationsTab;
	private static TabItem collaboratorsTab;
	private static TabItem tasksTab;
	private static TabItem contributionsTab;
	private static TabItem aboutTab;

	/** Contenu des onglets */
	private static DatabaseUI databaseUI;
	private static DurationsUI durationsUI;
	private static CollaboratorsUI collaboratorsUI;
	private static TasksUI tasksUI;
	private static ContributionsUI contributionsUI;

	public static void main(String[] args) {
		try {
			// Initialisation des logs et chargement de la config
			PropertyConfigurator.configure("cfg/log4j.properties");
			CfgMgr.load();
			Display display = new Display();

			// Splash screen
			Shell splash = new Shell(display, SWT.ON_TOP);
			splash.setLayout(new FillLayout());
			Label splashLabel = new Label(splash, SWT.NONE);
			URL splashImageUrl = AboutUI.class.getResource("logo-385x100.png");
			Image splashImage = new Image(splash.getDisplay(), splashImageUrl.openStream());
			splashLabel.setImage(splashImage);
			splash.pack();
			Rectangle splashRect = splash.getBounds();
			Rectangle displayRect = display.getBounds();
			int x = (displayRect.width - splashRect.width) / 2;
			int y = (displayRect.height - splashRect.height) / 2;
			splash.setLocation(x, y);
			splash.open();
			
			// Ouverture de la fenêtre
			final Shell shell = new Shell(display);
			shell.setSize(700, 500);
			shell.setText("ActivityManager");
			shell.setLayout(new GridLayout(1, false));
			URL iconUrl = Main.class.getResource("logo-16.ico");
			Image icon = new Image(display, iconUrl.openStream());
			shell.setImage(icon);
			shell.addShellListener(new ShellAdapter() {
				public void shellClosed(ShellEvent e) {
					new SafeRunner() {
						protected Object runUnsafe() throws Exception {
							ModelMgr.closeDatabaseAccess();
							return null;
						}
					}.run(shell);
				}
			});
			
			// Création du groupe d'onglets
			final TabFolder tabFolder = new TabFolder(shell, SWT.TOP);
			tabFolder.setLayout(new FillLayout(SWT.VERTICAL));
			tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			// Création de l'onglet de paramétrage de l'accès à la base de données
			databaseTab = new TabItem(tabFolder, SWT.NONE);
			databaseTab.setText("Database");
			databaseUI = new DatabaseUI(databaseTab);
			
			// Création de l'onglet de gestion des durées
			durationsTab = new TabItem(tabFolder, SWT.NONE);
			durationsTab.setText("Durations");
			durationsUI = new DurationsUI(durationsTab);

			// Création de l'onglet de gestion des collaborateurs
			collaboratorsTab = new TabItem(tabFolder, SWT.NONE);
			collaboratorsTab.setText("Collaborators");
			collaboratorsUI = new CollaboratorsUI(collaboratorsTab);

			// Création de l'onglet de gestion des taches
			tasksTab = new TabItem(tabFolder, SWT.NONE);
			tasksTab.setText("Tasks");
			tasksUI = new TasksUI(tasksTab);

			// Création de l'onglet de gestion des contributions
			contributionsTab = new TabItem(tabFolder, SWT.NONE);
			contributionsTab.setText("Contributions");
			contributionsUI = new ContributionsUI(contributionsTab);

			// Création de l'onglet contenant les informations générales
			aboutTab = new TabItem(tabFolder, SWT.NONE);
			aboutTab.setText("About");
			new AboutUI(aboutTab);

			// Enregistrement des listeners
			durationsUI.addDurationListener(contributionsUI);
			collaboratorsUI.addCollaboratorListener(contributionsUI);
			databaseUI.addDbStatusListener(durationsUI);
			databaseUI.addDbStatusListener(collaboratorsUI);
			databaseUI.addDbStatusListener(tasksUI);
			databaseUI.addDbStatusListener(contributionsUI);
			
			// Barre de statut
			final Label statusBar = new Label(shell, SWT.NONE);
			statusBar.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
			statusBar.setAlignment(SWT.RIGHT);
			statusBar.setText("Not connected");
			databaseUI.addDbStatusListener(new DbStatusListener() {
				public void databaseOpened() {
					statusBar.setText("Connected");
				}
				public void databaseClosed() {
					statusBar.setText("Not connected");
				}
			});

			// Ouverture de la fenêtre
			shell.open();
			shell.setEnabled(false);

			// Initialisation des attributs de connexion par défaut
			databaseUI.initUI();

			// Fermeture du splash
			splash.dispose();
			splashLabel.dispose();
			splashImage.dispose();
			shell.setEnabled(true);
			log.info("Application started");

			// Exécution jusqu'à l'arrêt
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
