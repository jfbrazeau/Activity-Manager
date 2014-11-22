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
package org.activitymgr.ui.rcp;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

import org.activitymgr.core.CoreModule;
import org.activitymgr.core.IBeanFactory;
import org.activitymgr.core.IModelMgr;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.DatabaseUI.IDbStatusListener;
import org.activitymgr.ui.rcp.util.UITechException;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.part.ViewPart;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

public class MainView extends ViewPart {
	public static final String ID = "org.activitymgr.ui.view";

	/** Logger */
	//private static Logger log = Logger.getLogger(MainView.class);

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

	/** Model manager */
	private IModelMgr modelMgr;

	/** Factory */
	private IBeanFactory factory;

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		// Création du groupe d'onglets
		final TabFolder tabFolder = new TabFolder(parent, SWT.TOP);
		tabFolder
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// TODO move the core module & model manager initialization 
		initialize();
		
		// Création de l'onglet de paramétrage de l'accès à la base de
		// données
		databaseTab = new TabItem(tabFolder, SWT.NONE);
		databaseTab.setText(Strings.getString("Main.tabs.DATABASE")); //$NON-NLS-1$
		databaseUI = new DatabaseUI(databaseTab, modelMgr, factory);

		// Création de l'onglet de gestion des durées
		durationsTab = new TabItem(tabFolder, SWT.NONE);
		durationsTab.setText(Strings.getString("Main.tabs.DURATIONS")); //$NON-NLS-1$
		durationsUI = new DurationsUI(durationsTab, modelMgr, factory);

		// Création de l'onglet de gestion des collaborateurs
		collaboratorsTab = new TabItem(tabFolder, SWT.NONE);
		collaboratorsTab.setText(Strings
				.getString("Main.tabs.COLLABORATORS")); //$NON-NLS-1$
		collaboratorsUI = new CollaboratorsUI(collaboratorsTab, modelMgr);

		// Création de l'onglet de gestion des taches
		tasksTab = new TabItem(tabFolder, SWT.NONE);
		tasksTab.setText(Strings.getString("Main.tabs.TASKS")); //$NON-NLS-1$
		tasksUI = new TasksUI(tasksTab, modelMgr);

		// Création de l'onglet de gestion des contributions
		contributionsTab = new TabItem(tabFolder, SWT.NONE);
		contributionsTab.setText(Strings
				.getString("Main.tabs.CONTRIBUTIONS")); //$NON-NLS-1$
		contributionsUI = new ContributionsUI(contributionsTab, modelMgr, factory);

		// Création de l'onglet contenant les informations générales
		aboutTab = new TabItem(tabFolder, SWT.NONE);
		aboutTab.setText(Strings.getString("Main.tabs.ABOUT")); //$NON-NLS-1$
		new AboutUI(aboutTab);

		// Enregistrement des listeners
		databaseUI.addDbStatusListener(durationsUI);
		databaseUI.addDbStatusListener(collaboratorsUI);
		databaseUI.addDbStatusListener(tasksUI);
		databaseUI.addDbStatusListener(contributionsUI);
		durationsUI.addDurationListener(contributionsUI);
		collaboratorsUI.addCollaboratorListener(contributionsUI);
		tasksUI.addTaskListener(contributionsUI);
		contributionsUI.addContributionListener(tasksUI);

		// Barre de statut
		final IStatusLineManager statusBar = getViewSite().getActionBars().getStatusLineManager();
		final Shell shell = parent.getShell();
		IDbStatusListener dbStatusListener = new IDbStatusListener() {
			public void databaseOpened() {
				shell.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						statusBar.setMessage(Strings
								.getString("Main.status.CONNECTED")); //$NON-NLS-1$
					}
				});
			}

			public void databaseClosed() {
				shell.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						statusBar.setMessage(Strings
								.getString("Main.status.NOT_CONNECTED")); //$NON-NLS-1$
					}
				});
			}
		};
		databaseUI.addDbStatusListener(dbStatusListener);
		dbStatusListener.databaseClosed();
		// Initialisation des attributs de connexion par défaut
		databaseUI.initUI();
	}

	/**
	 * Initializes transaction management and model manager.
	 */
	private void initialize() {
		// Create Guice injector
		final ThreadLocal<Connection> dbTxs = new ThreadLocal<Connection>();
		final Injector injector = Guice.createInjector(new CoreModule(),
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(Connection.class).toProvider(
								new Provider<Connection>() {
									@Override
									public Connection get() {
										return dbTxs.get();
									}
								});
					}
				});
		// Creates a new model manager wrapper (managing the transaction)
		modelMgr = (IModelMgr) Proxy.newProxyInstance(
				MainView.class.getClassLoader(),
				new Class<?>[] { IModelMgr.class }, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						Connection tx = null;
						try {
							// Open the transaction
							tx = databaseUI.getDatasource().getConnection();
							dbTxs.set(tx);
							// Call the real model manager
							IModelMgr wrappedModelMgr = injector.getInstance(IModelMgr.class);
							Object result = method.invoke(wrappedModelMgr, args);
							// Commit the transaction
							tx.commit();
							return result;
						} catch (InvocationTargetException t) {
							// Rollback the transaction in case of failure
							tx.rollback();
							throw t.getCause();
						} finally {
							// Release the transaction
							dbTxs.remove();
							tx.close();
						}
					}
				});
		factory = injector.getInstance(IBeanFactory.class);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
	}

	/**
	 * Ferme la connexion à la base de données.
	 * 
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public void closeDatabase() throws UITechException, DAOException {
		if (databaseUI != null) {
			databaseUI.closeDatabase();
		}
	}

}