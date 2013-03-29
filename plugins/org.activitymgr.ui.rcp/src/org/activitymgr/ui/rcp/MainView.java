package org.activitymgr.ui.rcp;

import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.DatabaseUI.IDbStatusListener;
import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class MainView extends ViewPart {
	public static final String ID = "org.activitymgr.ui.view";

	/** Logger */
	private static Logger log = Logger.getLogger(MainView.class);

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
	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		// Création du groupe d'onglets
		final TabFolder tabFolder = new TabFolder(parent, SWT.TOP);
		tabFolder.setLayout(new FillLayout(SWT.VERTICAL));
		tabFolder
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Création de l'onglet de paramétrage de l'accès à la base de
		// données
		databaseTab = new TabItem(tabFolder, SWT.NONE);
		databaseTab.setText(Strings.getString("Main.tabs.DATABASE")); //$NON-NLS-1$
		databaseUI = new DatabaseUI(databaseTab);

		// Création de l'onglet de gestion des durées
		durationsTab = new TabItem(tabFolder, SWT.NONE);
		durationsTab.setText(Strings.getString("Main.tabs.DURATIONS")); //$NON-NLS-1$
		durationsUI = new DurationsUI(durationsTab);

		// Création de l'onglet de gestion des collaborateurs
		collaboratorsTab = new TabItem(tabFolder, SWT.NONE);
		collaboratorsTab.setText(Strings
				.getString("Main.tabs.COLLABORATORS")); //$NON-NLS-1$
		collaboratorsUI = new CollaboratorsUI(collaboratorsTab);

		// Création de l'onglet de gestion des taches
		tasksTab = new TabItem(tabFolder, SWT.NONE);
		tasksTab.setText(Strings.getString("Main.tabs.TASKS")); //$NON-NLS-1$
		tasksUI = new TasksUI(tasksTab);

		// Création de l'onglet de gestion des contributions
		contributionsTab = new TabItem(tabFolder, SWT.NONE);
		contributionsTab.setText(Strings
				.getString("Main.tabs.CONTRIBUTIONS")); //$NON-NLS-1$
		contributionsUI = new ContributionsUI(contributionsTab);

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
		final Label statusBar = new Label(parent, SWT.NONE);
		statusBar.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false,
				false));
		statusBar.setAlignment(SWT.RIGHT);
		statusBar.setText(Strings.getString("Main.status.NOT_CONNECTED")); //$NON-NLS-1$
		databaseUI.addDbStatusListener(new IDbStatusListener() {
			public void databaseOpened() {
				statusBar.setText(Strings
						.getString("Main.status.CONNECTED")); //$NON-NLS-1$
			}

			public void databaseClosed() {
				statusBar.setText(Strings
						.getString("Main.status.NOT_CONNECTED")); //$NON-NLS-1$
			}
		});

		// Initialisation des attributs de connexion par défaut
		databaseUI.initUI();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
	}

}