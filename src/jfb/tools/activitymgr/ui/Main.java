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

import jfb.tools.activitymgr.ui.util.CfgMgr;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * Classe principale de l'application des gestion des l'activité.
 */
public class Main {

	/** Logger */
	private static Logger log = Logger.getLogger(Main.class);

	public static void main(String[] args) {
		try {
			// Initialisation des logs et chargement de la config
			PropertyConfigurator.configure("cfg/log4j.properties");
			CfgMgr.load();
			
			// Ouverture de la fenêtre
			Display display = new Display();
			Shell shell = new Shell(display);
			shell.setSize(700, 500);
			shell.setText("ActivityManager");
			shell.setLayout(new FillLayout(SWT.VERTICAL));
			URL iconUrl = Main.class.getResource("logo-16.ico");
			shell.setImage(new Image(display, iconUrl.openStream()));

			// Création du groupe d'onglets
			final TabFolder tabFolder = new TabFolder(shell, SWT.TOP);
			tabFolder.setLayout(new FillLayout(SWT.VERTICAL));
			// Création de l'onglet de paramétrage de l'accès à la base de données
			TabItem databaseTab = new TabItem(tabFolder, SWT.NONE);
			databaseTab.setText("Database");
			final DatabaseUI databaseUI = new DatabaseUI(databaseTab);
			
			// Création de l'onglet de gestion des collaborateurs
			TabItem collaboratorsTab = new TabItem(tabFolder, SWT.NONE);
			collaboratorsTab.setText("Collaborators");
			final CollaboratorsUI collaboratorsUI = new CollaboratorsUI(collaboratorsTab);

			// Création de l'onglet de gestion des taches
			TabItem tasksTab = new TabItem(tabFolder, SWT.NONE);
			tasksTab.setText("Tasks");
			final TasksUI tasksUI = new TasksUI(tasksTab);

			// Création de l'onglet de gestion des contributions
			TabItem contributionsTab = new TabItem(tabFolder, SWT.NONE);
			contributionsTab.setText("Contributions");
			final ContributionsUI contributionsUI = new ContributionsUI(contributionsTab);

			// Enregistrement du listener
			collaboratorsUI.addCollaboratorListener(contributionsUI);
			
			// Création du listener provoquant les rafraichissement lors d'un 
			// changement d'onglet
//			tabFolder.addSelectionListener(new SelectionListener() {
//				public void widgetSelected(SelectionEvent e) {
//					log.debug("widgetSelected(" + e + ")");
//					log.debug("SelectionIndex=" + tabFolder.getSelectionIndex());
//					switch (tabFolder.getSelectionIndex()) {
//					// Onglet Collaborateurs
//					case 1:
//						collaboratorsUI.refreshUI();
//						break;
//					// Onglet Taches
//					case 2:
//						tasksUI.refreshUI();
//						break;
//					// Onglet contributions
//					case 3:
//						contributionsUI.refreshUI();
//						break;
//					}
//					log.debug("source=" + e.getSource());
//				}
//				public void widgetDefaultSelected(SelectionEvent e) {
//					widgetSelected(e);
//				}
//			});
			
			// Enregistrement des IHM dans l'onglet de paramétrage de la connexion
			// à la base
			databaseUI.setCollaboratorsUI(collaboratorsUI);
			databaseUI.setTasksUI(tasksUI);
			databaseUI.setContributionsUI(contributionsUI);
			
			// Ouverture de la fenêtre
			shell.open();
			log.info("Application started");

			// Initialisation des attributs de connexion par défaut
			databaseUI.initUI();

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
