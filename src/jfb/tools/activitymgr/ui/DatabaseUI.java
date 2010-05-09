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

import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.ui.util.CfgMgr;
import jfb.tools.activitymgr.ui.util.SafeRunner;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

/**
 * IHM associée à l'onglet de paramétrage de l'accès à la base de données.
 */
public class DatabaseUI {

	/** Logger */
	private static Logger log = Logger.getLogger(DatabaseUI.class);

	/** Composant parent */
	private Composite parent;

	/** Champs de saisie */
	private Text jdbcDriverText;
	private Text jdbcUrlText;
	private Text jdbcUserText;
	private Text jdbcPasswordText;

	/** Liste des contenus des autres onglets */
	private CollaboratorsUI collaboratorsUI;
	private TasksUI tasksUI;
	private ContributionsUI contributionsUI;

	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 * @param tabItem item parent.
	 */
	public DatabaseUI(TabItem tabItem) {
		this(tabItem.getParent());
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par défaut.
	 * @param parentComposite composant parent.
	 */
	public DatabaseUI(Composite parentComposite) {
		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));
		Composite centeredPanel = new Composite(parent, SWT.NONE);
		centeredPanel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		centeredPanel.setLayout(new GridLayout(2, false));

		// Driver JDBC
		Label jdbcDriverLabel = new Label(centeredPanel, SWT.NONE);
		jdbcDriverLabel.setText("JDBC Driver :");
		jdbcDriverText = new Text(centeredPanel, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.widthHint = 200;
		jdbcDriverText.setLayoutData(gridData);

		// URL de connexion
		Label jdbcUrlLabel = new Label(centeredPanel, SWT.NONE);
		jdbcUrlLabel.setText("Server URL :");
		jdbcUrlText = new Text(centeredPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 200;
		jdbcUrlText.setLayoutData(gridData);

		// User de connexion
		Label userLabel = new Label(centeredPanel, SWT.NONE);
		userLabel.setText("User ID :");
		jdbcUserText = new Text(centeredPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 80;
		jdbcUserText.setLayoutData(gridData);
		
		// Password de connexion
		Label pwdLabel = new Label(centeredPanel, SWT.NONE);
		pwdLabel.setText("Password :");
		jdbcPasswordText = new Text(centeredPanel, SWT.BORDER | SWT.PASSWORD);
		gridData = new GridData();
		gridData.widthHint = 80;
		jdbcPasswordText.setLayoutData(gridData);
		
		// Bouton de connexion
		Button connectButton = new Button(centeredPanel, SWT.NONE);
		connectButton.setText("Connect!");
		gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gridData.horizontalSpan = 2;
		connectButton.setLayoutData(gridData);
		connectButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			public void widgetSelected(SelectionEvent e) {
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						// Récupération des paramètres de connexion
						String jdbcDriver = jdbcDriverText.getText().trim();
						String jdbcUrl = jdbcUrlText.getText().trim();
						String jdbcUser = jdbcUserText.getText().trim();
						String jdbcPassword = jdbcPasswordText.getText();
						
						// Sauvagarde dans le fichier de config
						CfgMgr.set(CfgMgr.JDBC_DRIVER, jdbcDriver);
						CfgMgr.set(CfgMgr.JDBC_URL, jdbcUrl);
						CfgMgr.set(CfgMgr.JDBC_USER, jdbcUser);
						CfgMgr.set(CfgMgr.JDBC_PASSWORD, jdbcPassword);
						CfgMgr.save();
						
						// Changement des paramètres de connexion
						ModelMgr.initDatabaseAccess(
								jdbcDriver,
								jdbcUrl,
								jdbcUser,
								jdbcPassword
							);

						// Initialisation des IHM
						if (collaboratorsUI!=null)
							collaboratorsUI.initUI();
						if (tasksUI!=null)
							tasksUI.initUI();
						if (contributionsUI!=null)
							contributionsUI.initUI();
						log.debug("UI initialization done");
						return null;
					}
				};
				// Exécution du traitement
				runner.run(parent.getShell());
			}
		});
	}

	/**
	 * @param collaboratorsUI The collaboratorsUI to set.
	 */
	public void setCollaboratorsUI(CollaboratorsUI collaboratorsUI) {
		this.collaboratorsUI = collaboratorsUI;
	}

	/**
	 * @param contributionsUI The contributionsUI to set.
	 */
	public void setContributionsUI(ContributionsUI contributionsUI) {
		this.contributionsUI = contributionsUI;
	}

	/**
	 * @param tasksUI The tasksUI to set.
	 */
	public void setTasksUI(TasksUI tasksUI) {
		this.tasksUI = tasksUI;
	}

	/**
	 * Initialise l'IHM avec les données en base.
	 */
	public void initUI() {
		// Valeurs par défaut (à supprimer)
		String jdbcDriver = CfgMgr.get(CfgMgr.JDBC_DRIVER);
		String jdbcUrl = CfgMgr.get(CfgMgr.JDBC_URL);
		String jdbcUser = CfgMgr.get(CfgMgr.JDBC_USER);
		String jdbcPassword = CfgMgr.get(CfgMgr.JDBC_PASSWORD);
		jdbcDriverText.setText(jdbcDriver!=null ? jdbcDriver : "");
		jdbcUrlText.setText(jdbcUrl!=null ? jdbcUrl : "");
		jdbcUserText.setText(jdbcUser!=null ? jdbcUser : "");
		jdbcPasswordText.setText(jdbcPassword!=null ? jdbcPassword : "");
	}
	
}
