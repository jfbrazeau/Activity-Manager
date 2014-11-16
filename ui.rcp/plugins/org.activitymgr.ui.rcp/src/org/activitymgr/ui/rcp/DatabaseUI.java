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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.activitymgr.core.IModelMgr;
import org.activitymgr.core.ModelException;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.util.DbHelper;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.util.SafeRunner;
import org.activitymgr.ui.rcp.util.UITechException;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.xml.sax.SAXException;

/**
 * IHM associée à l'onglet de paramétrage de l'accès à la base de données.
 */
public class DatabaseUI implements ModifyListener {

	/**
	 * Interface utilisée pour notifier de l'état de la connexion à la base de
	 * données.
	 */
	public static interface IDbStatusListener {

		/**
		 * Notifie de l'ouverture de l'accès à la base de données.
		 */
		public void databaseOpened();

		/**
		 * Notifie de l'ouverture de la fermeture.
		 */
		public void databaseClosed();

	}

	/** Logger */
	private static Logger log = Logger.getLogger(DatabaseUI.class);

	/** Constantes */
	public static final int STANDALONE_MODE = 0;
	public static final int MYSQL_SERVER_MODE = 1;
	public static final int USER_DEFINED_MODE = 2;

	/** Model manager */
	private IModelMgr modelMgr;
	
	/** Datasource */
	private BasicDataSource datasource;

	/** Listener */
	private List<IDbStatusListener> listeners = new ArrayList<IDbStatusListener>();

	/** Composant parent */
	private Composite parent;

	/** Panneau contenant les controles */
	private Composite centeredPanel;

	/** Panneau contenant les données de connexion à la BDD */
	private Composite conectionPanel;

	/** Panneau contenant les boutons d'export/import */
	private Composite xmlPanel;

	/** Champs de saisie, controles et labels */
	private Label dbTypeLabel;
	private Combo dbTypeCombo;
	private Label jdbcDriverLabel;
	private Text jdbcDriverText;
	private Label dbHostLabel;
	private Text dbHostText;
	private Label dbPortLabel;
	private Text dbPortText;
	private FileFieldEditor dbDataFileText;
	private Label dbNameLabel;
	private Text dbNameText;
	private Label jdbcUrlLabel;
	private Text jdbcUrlText;
	private Label jdbcUserIdLabel;
	private Text jdbcUserIdText;
	private Label jdbcPasswordLabel;
	private Text jdbcPasswordText;
	private Label jdbcPasswordWarning;
	private Button openDbButton;
	private Button closeDbButton;
	private Button resetDbDataButton;
	private FileFieldEditor xmlFileText;
	private Button xmlExportButton;
	private Button xmlImportButton;

	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 * 
	 * @param tabItem
	 *            item parent.
	 * @param modelMgr
	 *            the model manager instance.
	 */
	public DatabaseUI(TabItem tabItem, IModelMgr modelMgr) {
		this(tabItem.getParent(), modelMgr);
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par défaut.
	 * 
	 * @param parentComposite
	 *            composant parent.
	 * @param modelMgr
	 *            the model manager instance.
	 */
	public DatabaseUI(Composite parentComposite, IModelMgr modelMgr) {
		this.modelMgr = modelMgr;

		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));
		centeredPanel = new Composite(parent, SWT.NONE);
		centeredPanel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
				true));
		centeredPanel.setLayout(new GridLayout(1, false));

		// Groupe et pannneau contenant les données de connexion à la BDD
		Group conectionGroup = new Group(centeredPanel, SWT.NONE);
		conectionGroup.setText(Strings
				.getString("DatabaseUI.labels.CONNECTION_PROPERTIES")); //$NON-NLS-1$
		FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
		fillLayout.marginWidth = 5;
		fillLayout.marginHeight = 5;
		conectionGroup.setLayout(fillLayout);
		conectionPanel = new Composite(conectionGroup, SWT.NONE);
		conectionPanel.setLayout(new GridLayout(3, false));

		// Type de BDD
		dbTypeLabel = new Label(conectionPanel, SWT.NONE);
		dbTypeLabel.setText(Strings
				.getString("DatabaseUI.labels.DATABASE_TYPE")); //$NON-NLS-1$
		dbTypeCombo = new Combo(conectionPanel, SWT.READ_ONLY);
		dbTypeCombo.add(Strings
				.getString("DatabaseUI.databasetypes.STANDALONE_MODE")); //$NON-NLS-1$
		dbTypeCombo.add(Strings
				.getString("DatabaseUI.databasetypes.MYSQL_SERVER")); //$NON-NLS-1$
		dbTypeCombo.add(Strings
				.getString("DatabaseUI.databasetypes.USER_DEFINED")); //$NON-NLS-1$
		dbTypeCombo.select(STANDALONE_MODE);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.horizontalSpan = 2;
		dbTypeCombo.setLayoutData(gridData);
		dbTypeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Construction d'un contexte d'exécution sécurisé
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						dbTypeChanged();
						return null;
					}
				};
				// Exécution du traitement
				runner.run(parent.getShell());
			}
		});

		// Driver JDBC
		jdbcDriverLabel = new Label(conectionPanel, SWT.NONE);
		jdbcDriverLabel.setText(Strings
				.getString("DatabaseUI.labels.JDBC_DRIVER")); //$NON-NLS-1$
		jdbcDriverText = new Text(conectionPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.horizontalSpan = 2;
		jdbcDriverText.setLayoutData(gridData);

		// Nom d'hôte & port d'écoute de la BDD
		dbHostLabel = new Label(conectionPanel, SWT.NONE);
		dbHostLabel.setText(Strings
				.getString("DatabaseUI.labels.DATABASE_HOST")); //$NON-NLS-1$
		Composite hostAndPortPanel = new Composite(conectionPanel, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		hostAndPortPanel.setLayout(layout);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = SWT.FILL;
		hostAndPortPanel.setLayoutData(gridData);
		// Host
		dbHostText = new Text(hostAndPortPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		dbHostText.setLayoutData(gridData);
		// Port d'écoute de la BDD
		dbPortLabel = new Label(hostAndPortPanel, SWT.NONE);
		dbPortLabel.setText(Strings
				.getString("DatabaseUI.labels.DATABASE_PORT")); //$NON-NLS-1$
		dbPortText = new Text(hostAndPortPanel, SWT.BORDER);
		dbPortText.setText("XXXX"); //$NON-NLS-1$

		// Fichier de données
		dbDataFileText = new FileFieldEditor(
				"datafile", Strings.getString("DatabaseUI.labels.DATA_FILE"), conectionPanel); //$NON-NLS-1$ //$NON-NLS-2$

		// Nom de la BDD
		dbNameLabel = new Label(conectionPanel, SWT.NONE);
		dbNameLabel.setText(Strings
				.getString("DatabaseUI.labels.DATABASE_NAME")); //$NON-NLS-1$
		dbNameText = new Text(conectionPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.horizontalSpan = 2;
		dbNameText.setLayoutData(gridData);

		// URL de connexion
		jdbcUrlLabel = new Label(conectionPanel, SWT.NONE);
		jdbcUrlLabel.setText(Strings.getString("DatabaseUI.labels.SERVER_URL")); //$NON-NLS-1$
		jdbcUrlText = new Text(conectionPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 250;
		gridData.horizontalSpan = 2;
		jdbcUrlText.setLayoutData(gridData);

		// User de connexion
		jdbcUserIdLabel = new Label(conectionPanel, SWT.NONE);
		jdbcUserIdLabel.setText(Strings.getString("DatabaseUI.labels.USER_ID")); //$NON-NLS-1$
		jdbcUserIdText = new Text(conectionPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 80;
		gridData.horizontalSpan = 2;
		jdbcUserIdText.setLayoutData(gridData);

		// Password de connexion
		jdbcPasswordLabel = new Label(conectionPanel, SWT.NONE);
		jdbcPasswordLabel.setText(Strings
				.getString("DatabaseUI.labels.USER_PASSWORD")); //$NON-NLS-1$
		// Panneau contenant le champ + le warning
		Composite jdbcPasswordAndWarningPanel = new Composite(conectionPanel,
				SWT.NONE);
		layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		jdbcPasswordAndWarningPanel.setLayout(layout);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = SWT.FILL;
		jdbcPasswordAndWarningPanel.setLayoutData(gridData);
		// Champ password
		jdbcPasswordText = new Text(jdbcPasswordAndWarningPanel, SWT.BORDER
				| SWT.PASSWORD);
		gridData = new GridData();
		gridData.widthHint = 80;
		jdbcPasswordText.setLayoutData(gridData);
		// Warning
		jdbcPasswordWarning = new Label(jdbcPasswordAndWarningPanel, SWT.NONE);
		jdbcPasswordWarning.setText(Strings
				.getString("DatabaseUI.labels.PASSWORD_WARNING")); //$NON-NLS-1$

		// Panneau contenant les boutons d'ouverture/fermeture de la BDD
		Composite openCloseDbButtonsPanel = new Composite(conectionPanel,
				SWT.NONE);
		openCloseDbButtonsPanel.setLayout(new GridLayout(3, false));
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gridData.horizontalSpan = 3;
		openCloseDbButtonsPanel.setLayoutData(gridData);

		// Bouton d'ouverture/fermeture de la BDD
		openDbButton = new Button(openCloseDbButtonsPanel, SWT.NONE);
		openDbButton.setText(Strings
				.getString("DatabaseUI.buttons.OPEN_DATABASE")); //$NON-NLS-1$
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		openDbButton.setLayoutData(gridData);
		openDbButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						openDatabase();
						return null;
					}
				};
				// Exécution du traitement
				runner.run(parent.getShell());
			}
		});
		closeDbButton = new Button(openCloseDbButtonsPanel, SWT.NONE);
		closeDbButton.setText(Strings
				.getString("DatabaseUI.buttons.CLOSE_DATABASE")); //$NON-NLS-1$
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		closeDbButton.setLayoutData(gridData);
		closeDbButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						closeDatabase();
						return null;
					}
				};
				// Exécution du traitement
				runner.run(parent.getShell());
			}
		});
		// Désactivation du bouton
		closeDbButton.setEnabled(false);

		// Bouton de réinstallation de la base de données
		resetDbDataButton = new Button(openCloseDbButtonsPanel, SWT.NONE);
		resetDbDataButton.setText(Strings
				.getString("DatabaseUI.buttons.RESET_DATABASE")); //$NON-NLS-1$
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		resetDbDataButton.setLayoutData(gridData);
		resetDbDataButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						reinstallDatabaseWithWarnings();
						return null;
					}
				};
				// Exécution du traitement
				runner.run(parent.getShell());
			}
		});
		// Désactivation du bouton
		resetDbDataButton.setEnabled(false);

		// Groupe et pannneau contenant les bouton d'export/import
		Group xmlGroup = new Group(centeredPanel, SWT.NONE);
		xmlGroup.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false));
		xmlGroup.setText(Strings.getString("DatabaseUI.labels.EXPORT_IMPORT")); //$NON-NLS-1$
		xmlGroup.setLayout(fillLayout);
		xmlPanel = new Composite(xmlGroup, SWT.NONE);
		xmlPanel.setLayout(new GridLayout(3, false));

		// Fichier de données
		xmlFileText = new FileFieldEditor(
				"xmlFile", Strings.getString("DatabaseUI.labels.XML_FILE"), xmlPanel); //$NON-NLS-1$ //$NON-NLS-2$
		disableField(xmlFileText, xmlPanel);

		// Panneau contenant les boutons d'ouverture/fermeture de la BDD
		Composite xmlButtonsPanel = new Composite(xmlPanel, SWT.NONE);
		xmlButtonsPanel.setLayout(new GridLayout(2, false));
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gridData.horizontalSpan = 3;
		xmlButtonsPanel.setLayoutData(gridData);

		// Bouton d'ouverture/fermeture de la BDD
		xmlExportButton = new Button(xmlButtonsPanel, SWT.NONE);
		xmlExportButton.setText(Strings
				.getString("DatabaseUI.buttons.EXPORT_DATABASE")); //$NON-NLS-1$
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		xmlExportButton.setLayoutData(gridData);
		xmlExportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						exportToXML();
						return null;
					}
				};
				// Exécution du traitement
				runner.run(parent.getShell());
			}
		});
		disableField(xmlExportButton);
		xmlImportButton = new Button(xmlButtonsPanel, SWT.NONE);
		xmlImportButton.setText(Strings
				.getString("DatabaseUI.buttons.IMPORT_DATABASE")); //$NON-NLS-1$
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		xmlImportButton.setLayoutData(gridData);
		xmlImportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						importFromXML();
						return null;
					}
				};
				// Exécution du traitement
				runner.run(parent.getShell());
			}
		});
		// Désactivation du bouton
		disableField(xmlImportButton);

	}

	/**
	 * Initialise l'IHM avec les données en base.
	 */
	public void initUI() {
		IPreferenceStore cfg = Activator.getDefault().getPreferenceStore();
		// Valeurs par défaut (à supprimer)
		int databaseType = cfg.getInt(PreferenceManager.DATABASE_TYPE);
		String jdbcDriver = cfg.getString(PreferenceManager.JDBC_DRIVER);
		String dbHost = cfg.getString(PreferenceManager.DATABASE_HOST);
		String dbPort = cfg.getString(PreferenceManager.DATABASE_PORT);
		String dbDataFile = cfg.getString(PreferenceManager.DATABASE_DATA_FILE);
		String dbName = cfg.getString(PreferenceManager.DATABASE_NAME);
		String jdbcUrl = cfg.getString(PreferenceManager.JDBC_URL);
		String jdbcUser = cfg.getString(PreferenceManager.JDBC_USER);
		String jdbcPassword = cfg.getString(PreferenceManager.JDBC_PASSWORD);
		dbTypeCombo.select(databaseType);
		dbHostText.setText(dbHost != null ? dbHost : ""); //$NON-NLS-1$
		dbPortText.setText(dbPort != null ? dbPort : ""); //$NON-NLS-1$
		dbDataFileText.setStringValue(dbDataFile != null ? dbDataFile : ""); //$NON-NLS-1$
		dbNameText.setText(dbName != null ? dbName : ""); //$NON-NLS-1$
		jdbcDriverText.setText(jdbcDriver != null ? jdbcDriver : ""); //$NON-NLS-1$
		jdbcUrlText.setText(jdbcUrl != null ? jdbcUrl : ""); //$NON-NLS-1$
		jdbcUserIdText.setText(jdbcUser != null ? jdbcUser : ""); //$NON-NLS-1$
		jdbcPasswordText.setText(jdbcPassword != null ? jdbcPassword : ""); //$NON-NLS-1$
		// Mise à jour des données
		dbTypeChanged();
	}

	/**
	 * Méthode invoquée lorsque l'utilisateur change le type de BDD dans l'IHM.
	 */
	protected void dbTypeChanged() {
		log.debug("dbTypeCombo.getSelectionIndex()=" + dbTypeCombo.getSelectionIndex()); //$NON-NLS-1$
		// Désactivation de tout les champs
		disableField(jdbcDriverText);
		disableField(dbHostText);
		disableField(dbPortText);
		disableField(dbDataFileText, conectionPanel);
		disableField(dbNameText);
		disableField(jdbcUrlText);
		disableField(jdbcUserIdText);
		disableField(jdbcPasswordText);
		switch (dbTypeCombo.getSelectionIndex()) {
		// Cas d'une connexion JDBC HSQL embarqué
		case STANDALONE_MODE:
			enabledField(dbDataFileText, conectionPanel,
					"data/activitymgr", false); //$NON-NLS-1$
			break;
		// Cas d'une connexion MySQL
		case MYSQL_SERVER_MODE:
			enabledField(dbHostText, "localhost", false); //$NON-NLS-1$
			enabledField(dbPortText, "3306", true); //$NON-NLS-1$
			enabledField(dbNameText, "taskmgr_db", false); //$NON-NLS-1$
			enabledField(jdbcUserIdText, "taskmgr_db", false); //$NON-NLS-1$
			enabledField(jdbcPasswordText, "", false); //$NON-NLS-1$
			break;
		// Cas d'une connexion autre
		case USER_DEFINED_MODE:
			enabledField(jdbcDriverText,
					Strings.getString("DatabaseUI.defaults.JDBC_DRIVER"), false); //$NON-NLS-1$
			enabledField(jdbcUrlText,
					Strings.getString("DatabaseUI.defaults.JDBC_URL"), false); //$NON-NLS-1$
			enabledField(
					jdbcUserIdText,
					Strings.getString("DatabaseUI.defaults.JDBC_USER_ID"), false); //$NON-NLS-1$
			enabledField(jdbcPasswordText, "", false); //$NON-NLS-1$
			break;
		// Autre cas : erreur
		default:
			throw new Error(
					Strings.getString("DatabaseUI.errors.UNKNOWN_DATABASE_TYPE")); //$NON-NLS-1$
		}
		// Activation/désactivation des labels
		jdbcDriverLabel.setEnabled(jdbcDriverText.getEnabled());
		dbHostLabel.setEnabled(dbHostText.getEnabled());
		dbPortLabel.setEnabled(dbPortText.getEnabled());
		dbNameLabel.setEnabled(dbNameText.getEnabled());
		jdbcUrlLabel.setEnabled(jdbcUrlText.getEnabled());
		jdbcUserIdLabel.setEnabled(jdbcUserIdText.getEnabled());
		jdbcPasswordLabel.setEnabled(jdbcPasswordText.getEnabled());
		jdbcPasswordWarning.setEnabled(jdbcPasswordText.getEnabled());
		// Mise à jour des champs
		entriesChanged();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events
	 * .ModifyEvent)
	 */
	public void modifyText(ModifyEvent e) {
		entriesChanged();
	}

	/**
	 * Désactive le champ spécifié.
	 * 
	 * @param field
	 *            le champ à désactiver.
	 */
	private void disableField(Text field) {
		Text text = (Text) field;
		text.setEnabled(false);
		text.removeModifyListener(this);
	}

	/**
	 * Désactive le champ spécifié.
	 * 
	 * @param field
	 *            le champ à désactiver.
	 */
	private void disableField(Control field) {
		field.setEnabled(false);
	}

	/**
	 * Désactive l'éditeur de nom de fichier.
	 * 
	 * @param field
	 *            le champ à désactiver.
	 * @param parent
	 *            le composant parent.
	 */
	private void disableField(FileFieldEditor field, Composite parent) {
		FileFieldEditor fileFieldEditor = (FileFieldEditor) field;
		fileFieldEditor.setEnabled(false, parent);
		fileFieldEditor.setPropertyChangeListener(null);
	}

	/**
	 * Active l'éditeur de nom de fichier.
	 * 
	 * @param field
	 *            le champ à désactiver.
	 * @param defaultValue
	 *            valeur par défaut.
	 * @param forceDefaultValue
	 *            booléen indiquant si la valeur par défaut doit être forcée
	 *            même quand le champ a déja une valeur.
	 */
	private void enabledField(Text field, String defaultValue,
			boolean forceDefaultValue) {
		// Cas d'un textfield
		field.setEnabled(true);
		if (forceDefaultValue || "".equals(field.getText())) //$NON-NLS-1$
			field.setText(defaultValue);
		field.addModifyListener(this);
	}

	/**
	 * Active le champ.
	 * 
	 * @param field
	 *            le champ à désactiver.
	 * @param parent
	 *            le composant parent.
	 * @param defaultValue
	 *            valeur par défaut.
	 * @param forceDefaultValue
	 *            booléen indiquant si la valeur par défaut doit être forcée
	 *            même quand le champ a déja une valeur.
	 */
	private void enabledField(FileFieldEditor field, Composite parent,
			String defaultValue, boolean forceDefaultValue) {
		field.setEnabled(true, parent);
		if (forceDefaultValue || "".equals(field.getStringValue())) //$NON-NLS-1$
			field.setStringValue(defaultValue);
		field.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				entriesChanged();
			}
		});
	}

	/**
	 * Réagit à un changement des données saisies par l'utilisateur.
	 */
	private void entriesChanged() {
		log.debug("Entries changed"); //$NON-NLS-1$
		switch (dbTypeCombo.getSelectionIndex()) {
		// Cas d'une connexion JDBC HSQL embarqué
		case STANDALONE_MODE:
			jdbcDriverText.setText("org.hsqldb.jdbcDriver"); //$NON-NLS-1$
			dbHostText.setText(""); //$NON-NLS-1$
			dbPortText.setText(""); //$NON-NLS-1$
			dbNameText.setText(""); //$NON-NLS-1$
			jdbcUrlText
					.setText("jdbc:hsqldb:file:" + dbDataFileText.getStringValue()); //$NON-NLS-1$
			jdbcUserIdText.setText("sa"); //$NON-NLS-1$
			jdbcPasswordText.setText(""); //$NON-NLS-1$
			break;
		// Cas d'une connexion MySQL
		case MYSQL_SERVER_MODE:
			jdbcDriverText.setText("com.mysql.jdbc.Driver"); //$NON-NLS-1$
			dbDataFileText.setStringValue(""); //$NON-NLS-1$
			jdbcUrlText
					.setText("jdbc:mysql://" + dbHostText.getText() + ":" + dbPortText.getText() + "/" + dbNameText.getText()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			break;
		// Cas d'une connexion autre
		case USER_DEFINED_MODE:
			dbHostText.setText(""); //$NON-NLS-1$
			dbPortText.setText(""); //$NON-NLS-1$
			dbNameText.setText(""); //$NON-NLS-1$
			dbDataFileText.setStringValue(""); //$NON-NLS-1$
			break;
		// Autre cas : erreur
		default:
			throw new Error(
					Strings.getString("DatabaseUI.errors.UNKNOWN_DATABASE_TYPE")); //$NON-NLS-1$
		}
	}

	/**
	 * Ajoute un listener.
	 * 
	 * @param listener
	 *            le nouveau listener.
	 */
	public void addDbStatusListener(IDbStatusListener listener) {
		listeners.add(listener);
	}

	/**
	 * Ajoute un listener.
	 * 
	 * @param listener
	 *            le nouveau listener.
	 */
	public void removeDbStatusListener(IDbStatusListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Ouvre la connexion à la base de données.
	 * 
	 * @throws IOException
	 *             levé en cas d'incident I/O lors du chargement de la
	 *             configuration.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws UITechException
	 *             levé en cas d'incident inattendu lors de la création des
	 *             durées.
	 */
	private void openDatabase() throws IOException, DAOException,
			UITechException {
		// Récupération des paramétres de connexion
		String databaseType = String.valueOf(dbTypeCombo.getSelectionIndex());
		String jdbcDriver = jdbcDriverText.getText().trim();
		String dbHost = dbHostText.getText().trim();
		String dbPort = dbPortText.getText().trim();
		String dbDataFile = dbDataFileText.getStringValue().trim();
		String dbName = dbNameText.getText().trim();
		String jdbcUrl = jdbcUrlText.getText().trim();
		String jdbcUser = jdbcUserIdText.getText().trim();
		String jdbcPassword = jdbcPasswordText.getText();

		// Sauvagarde de la config
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setValue(PreferenceManager.DATABASE_TYPE, databaseType);
		store.setValue(PreferenceManager.JDBC_DRIVER, jdbcDriver);
		store.setValue(PreferenceManager.DATABASE_HOST, dbHost);
		store.setValue(PreferenceManager.DATABASE_PORT, dbPort);
		store.setValue(PreferenceManager.DATABASE_DATA_FILE, dbDataFile);
		store.setValue(PreferenceManager.DATABASE_NAME, dbName);
		store.setValue(PreferenceManager.JDBC_URL, jdbcUrl);
		store.setValue(PreferenceManager.JDBC_USER, jdbcUser);
		store.setValue(PreferenceManager.JDBC_PASSWORD, jdbcPassword);

		// Changement des paramètres de connexion
		datasource = new BasicDataSource();
		datasource.setDriverClassName(jdbcDriver);
		datasource.setUrl(jdbcUrl);
		datasource.setUsername(jdbcUser);
		datasource.setPassword(jdbcPassword);
		datasource.setDefaultAutoCommit(false);

		// Test de l'existence du modèle en base
		boolean dbModelOk = modelMgr.tablesExist();
		// Si le modèle n'est pas installé et que l'utilisateur
		// le désire, l'application créée automatiquement les tables
		if (!dbModelOk) {
			if (MessageDialog
					.openConfirm(
							parent.getShell(),
							Strings.getString("DatabaseUI.labels.CONFIRMATION"), //$NON-NLS-1$
							Strings.getString("DatabaseUI.questions.DATABASE_NOT_INSTALLED"))) { //$NON-NLS-1$
				// Création des tables
				reinstallDatabase();
				dbModelOk = true;
			} else {
				MessageDialog
						.openError(parent.getShell(),
								Strings.getString("DatabaseUI.labels.ERROR"), //$NON-NLS-1$
								Strings.getString("DatabaseUI.errors.DATABASE_NOT_INSTALLED")); //$NON-NLS-1$
			}
		}

		// Si le modèle de données est bien installé
		if (dbModelOk) {
			// Activation/désactivation des boutons et des champs
			disableField(dbTypeCombo);
			disableField(dbTypeLabel);
			disableField(jdbcDriverLabel);
			disableField(jdbcDriverText);
			disableField(dbHostLabel);
			disableField(dbHostText);
			disableField(dbPortLabel);
			disableField(dbPortText);
			disableField(dbDataFileText, conectionPanel);
			disableField(dbNameLabel);
			disableField(dbNameText);
			disableField(jdbcUrlLabel);
			disableField(jdbcUrlText);
			disableField(jdbcUserIdLabel);
			disableField(jdbcUserIdText);
			disableField(jdbcPasswordLabel);
			disableField(jdbcPasswordText);
			disableField(jdbcPasswordWarning);
			openDbButton.setEnabled(false);
			closeDbButton.setEnabled(true);
			resetDbDataButton.setEnabled(true);
			enabledField(xmlFileText, xmlPanel, "", false); //$NON-NLS-1$
			xmlExportButton.setEnabled(true);
			xmlImportButton.setEnabled(true);

			// Notification de changement de statut de la connexion
			Iterator<IDbStatusListener> it = listeners.iterator();
			while (it.hasNext()) {
				IDbStatusListener listener = it.next();
				listener.databaseOpened();
			}
		}

	}

	/**
	 * Ferme la connexion à la base de données.
	 * 
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public void closeDatabase() throws UITechException, DAOException {
		// Changement des paramétres de connexion
		try {
			Connection con = datasource.getConnection();
			if (DbHelper.isEmbeddedHsqlOrH2(con, jdbcUrlText.getText().trim())) {
				DbHelper.shutdowHsqlOrH2(con);
			}
			con.close();
			datasource.close();
			datasource = null;
		} catch (SQLException e) {
			throw new UITechException("Unexpected error while closing the database",e); // TODO internationalize
		}

		// Activation/désactivation des boutons et des champs
		openDbButton.setEnabled(true);
		closeDbButton.setEnabled(false);
		resetDbDataButton.setEnabled(false);
		dbTypeCombo.setEnabled(true);
		dbTypeLabel.setEnabled(true);
		disableField(xmlFileText, xmlPanel);
		xmlExportButton.setEnabled(false);
		xmlImportButton.setEnabled(false);
		dbTypeChanged();

		// Notification de changement de statut de la connexion
		Iterator<IDbStatusListener> it = listeners.iterator();
		while (it.hasNext()) {
			IDbStatusListener listener = it.next();
			listener.databaseClosed();
		}

	}

	/**
	 * Réinstalle la base de données (tables drop + creation).
	 * 
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws UITechException
	 *             levé en cas d'incident inattendu lors de la création des
	 *             durées.
	 */
	private void reinstallDatabase() throws DAOException, UITechException {
		// Suppression et recréation des tables
		modelMgr.createTables();
		// Question concernant le référentiel de durées par défaut
		if (MessageDialog
				.openQuestion(parent.getShell(),
						Strings.getString("DatabaseUI.labels.CONFIRMATION"), //$NON-NLS-1$
						Strings.getString("DatabaseUI.questions.CREATE_DEFAULT_DURATIONS"))) { //$NON-NLS-1$
			try {
				Duration duration = new Duration();
				duration.setId(25);
				modelMgr.createDuration(duration);
				duration.setId(50);
				modelMgr.createDuration(duration);
				duration.setId(75);
				modelMgr.createDuration(duration);
				duration.setId(100);
				modelMgr.createDuration(duration);
			} catch (ModelException e) {
				log.error(
						"Unexpected error while creating default durations", e); //$NON-NLS-1$
				throw new UITechException(
						Strings.getString("DatabaseUI.errors.DURATIONS_CREATION_ERROR"), e); //$NON-NLS-1$
			}
		}
		// Notification des listeners (reset équivalent à réouverture de la BDD)
		Iterator<IDbStatusListener> it = listeners.iterator();
		while (it.hasNext()) {
			IDbStatusListener listener = it.next();
			listener.databaseOpened();
		}
	}

	/**
	 * Réinstalle la base de données (tables drop + creation).
	 * 
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws UITechException
	 *             levé en cas d'incident inattendu lors de la création des
	 *             durées.
	 */
	private void reinstallDatabaseWithWarnings() throws DAOException,
			UITechException {
		if (MessageDialog.openQuestion(parent.getShell(),
				Strings.getString("DatabaseUI.labels.CONFIRMATION"), //$NON-NLS-1$
				Strings.getString("DatabaseUI.questions.RESET_CONFIRMATION_1"))) { //$NON-NLS-1$
			if (MessageDialog
					.openQuestion(
							parent.getShell(),
							Strings.getString("DatabaseUI.labels.CONFIRMATION"), //$NON-NLS-1$
							Strings.getString("DatabaseUI.questions.RESET_CONFIRMATION_2"))) { //$NON-NLS-1$
				reinstallDatabase();
			}
		}
	}

	/**
	 * Exporte le contenu de la BDD vers un fichier XML.
	 * 
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de l'écriture dans le fichier
	 *             XML.
	 */
	private void exportToXML() throws DAOException, IOException {
		String fileName = xmlFileText.getStringValue();
		if ("".equals(fileName.trim())) { //$NON-NLS-1$
			MessageDialog
					.openWarning(
							parent.getShell(),
							Strings.getString("DatabaseUI.errors.FILE_NAME_ERROR"), //$NON-NLS-1$
							Strings.getString("DatabaseUI.errors.XML_FILE_NOT_SPECIFIED")); //$NON-NLS-1$
		} else {
			File xmlFile = new File(fileName);
			if (!xmlFile.exists()
					|| MessageDialog
							.openConfirm(
									parent.getShell(),
									Strings.getString("DatabaseUI.labels.CONFIRMATION"), //$NON-NLS-1$
									Strings.getString("DatabaseUI.questions.OVERWRITE_CONFIRMATION"))) { //$NON-NLS-1$
				FileOutputStream out = new FileOutputStream(xmlFile);
				modelMgr.exportToXML(out);
				out.close();
				// Popup d'info de fin de traitement
				MessageDialog
						.openInformation(
								parent.getShell(),
								Strings.getString("DatabaseUI.labels.INFORMATION"), //$NON-NLS-1$
								Strings.getString("DatabaseUI.informations.DATABASE_SUCCESSFULLY_EXPORTED")); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Importe les données contenues dans un fichier XML.
	 * 
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de la lecture du fichier XML.
	 * @throws ParserConfigurationException
	 *             levé en cas de mauvaise configuration du parser XML.
	 * @throws SAXException
	 *             levé en cas de mauvais format du fichier XML.
	 * @throws ModelException
	 *             levé en cas de violation du modèle de données.
	 * @throws UITechException
	 *             levé en cas d'incident inattendu.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	private void importFromXML() throws IOException,
			ParserConfigurationException, SAXException, ModelException,
			UITechException, DAOException {
		String fileName = xmlFileText.getStringValue();
		File xmlFile = new File(fileName);
		if ("".equals(fileName.trim())) { //$NON-NLS-1$
			MessageDialog
					.openWarning(
							parent.getShell(),
							Strings.getString("DatabaseUI.errors.FILE_NAME_ERROR"), //$NON-NLS-1$
							Strings.getString("DatabaseUI.errors.XML_FILE_NOT_SPECIFIED")); //$NON-NLS-1$
		} else if (!xmlFile.exists()) {
			MessageDialog.openWarning(parent.getShell(),
					Strings.getString("DatabaseUI.errors.FILE_ERROR"), //$NON-NLS-1$
					Strings.getString("DatabaseUI.errors.FILE_DOES_NOT_EXIST")); //$NON-NLS-1$
		} else {
			if (MessageDialog
					.openConfirm(
							parent.getShell(),
							Strings.getString("DatabaseUI.labels.CONFIRMATION"), //$NON-NLS-1$
							Strings.getString("DatabaseUI.questions.IMPORTATION_CONFIRMATION"))) { //$NON-NLS-1$
				// Peut-être l'utilisateur veut faire un reset sur la base
				// avant import
				if (MessageDialog
						.openQuestion(
								parent.getShell(),
								Strings.getString("DatabaseUI.labels.CONFIRMATION"), //$NON-NLS-1$
								Strings.getString("DatabaseUI.questions.DATABASE_RESET_BEFORE_IMPORTATION"))) { //$NON-NLS-1$
					// Même traitement que pour le bouton 'Reset database data'
					reinstallDatabaseWithWarnings();
				}
				// Importation des données
				FileInputStream in = new FileInputStream(xmlFile);
				modelMgr.importFromXML(in);
				in.close();
				// Notification de fikn de chargement (équivalent ouverture BDD)
				Iterator<IDbStatusListener> it = listeners.iterator();
				while (it.hasNext()) {
					IDbStatusListener listener = it.next();
					listener.databaseOpened();
				}
				// Popup d'info de fin de traitement
				MessageDialog
						.openInformation(
								parent.getShell(),
								Strings.getString("DatabaseUI.labels.INFORMATION"), //$NON-NLS-1$
								Strings.getString("DatabaseUI.informations.DATABASE_SUCCESSFULLY_IMPORTED")); //$NON-NLS-1$
			}
		}
	}

	/**
	 * @return the datasource.
	 */
	public BasicDataSource getDatasource() {
		return datasource;
	}

}
