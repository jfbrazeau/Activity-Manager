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
package org.activitymgr.ui.rcp.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.activitymgr.core.IModelMgr;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskSearchFilter;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.dialogs.TaskChooserDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * Paneau offrant la possibilité de rechercher une tache par son nom ou son
 * code.
 */
public class TaskFinderPanel extends Composite {

	/**
	 * Liste de valeurs pour la liste de sélection du nom de l'attribut utilisé
	 * pour la recherche
	 */
	public static final String TASK_NAME_SEARCH_FIELD_LABEL = Strings
			.getString("TaskFinderPanel.attributes.TASK_NAME"); //$NON-NLS-1$
	public static final String TASK_CODE_SEARCH_FIELD_LABEL = Strings
			.getString("TaskFinderPanel.attributes.TASK_CODE"); //  @jve:decl-index=0: //$NON-NLS-1$

	/**
	 * Liste de valeurs pour la liste de sélection du type de critère utilisé
	 * pour la recherche
	 */
	public static final String IS_EQUAL_TO_CRITERIA_LABEL = Strings
			.getString("TaskFinderPanel.criterias.IS_EQUAL_TO"); //  @jve:decl-index=0: //$NON-NLS-1$
	public static final String STARTS_WITH_CRITERIA_LABEL = Strings
			.getString("TaskFinderPanel.criterias.STARTS_WITH"); //  @jve:decl-index=0: //$NON-NLS-1$
	public static final String ENDS_WITH_CRITERIA_LABEL = Strings
			.getString("TaskFinderPanel.criterias..ENDS_WITH"); //$NON-NLS-1$
	public static final String CONTAINS_CRITERIA_LABEL = Strings
			.getString("TaskFinderPanel.criterias.CONTAINS"); //  @jve:decl-index=0: //$NON-NLS-1$

	/** Group contenant les controles */
	private Group group = null;

	/** Liste des noms d'attributs utilisables pour la recherche */
	private Combo searchFieldCombo = null;

	/** Liste de critères de recherche */
	private Combo searchCriteriaCombo = null;

	/** Valeur utilisée pour la recherche */
	private Text searchText = null;

	/** Bouton de recherche */
	private Button findButton = null;

	/** Objet à l'écoute de la sélectiond d'une tache */
	private List<ITaskSelectionListener> taskSelectionListeners = new ArrayList<ITaskSelectionListener>(); // @jve:decl-index=0:

	/** Dialogue permettant de sélectionner une tache parmi une liste */
	private TaskChooserDialog taskChooserDialog = null;
	private IModelMgr modelMgr;

	/**
	 * Constructeur par défaut.
	 * 
	 * @param parent
	 *            le composant parent.
	 * @param modelMgr
	 *            the model manager.
	 */
	public TaskFinderPanel(Composite parent, IModelMgr modelMgr) {
		this(parent, SWT.NONE, modelMgr);
	}

	/**
	 * Constructeur par défaut.
	 * 
	 * @param parent
	 *            le composant parent.
	 * @param style
	 *            le style du panneau.
	 * @param modelMgr
	 *            the model manager.
	 */
	public TaskFinderPanel(Composite parent, int style, IModelMgr modelMgr) {
		super(parent, style);
		this.modelMgr = modelMgr;
		initialize();
		// Initialisation de la liste de choix du champ utilisé pour la
		// recherche (nom, code)
		searchFieldCombo.add(TASK_NAME_SEARCH_FIELD_LABEL,
				TaskSearchFilter.TASK_NAME_FIELD_IDX);
		searchFieldCombo.add(TASK_CODE_SEARCH_FIELD_LABEL,
				TaskSearchFilter.TASK_CODE_FIELD_IDX);
		searchFieldCombo.setText(TASK_NAME_SEARCH_FIELD_LABEL);

		// Initialisation de la liste de choix du critère de recherche
		searchCriteriaCombo.add(IS_EQUAL_TO_CRITERIA_LABEL,
				TaskSearchFilter.IS_EQUAL_TO_CRITERIA_IDX);
		searchCriteriaCombo.add(STARTS_WITH_CRITERIA_LABEL,
				TaskSearchFilter.STARTS_WITH_CRITERIA_IDX);
		searchCriteriaCombo.add(ENDS_WITH_CRITERIA_LABEL,
				TaskSearchFilter.ENDS_WITH_CRITERIA_IDX);
		searchCriteriaCombo.add(CONTAINS_CRITERIA_LABEL,
				TaskSearchFilter.CONTAINS_CRITERIA_IDX);
		searchCriteriaCombo.setText(CONTAINS_CRITERIA_LABEL);

		// Ajout du KeyListener permettant de valider la saisie sur
		// l'utilisation de
		// la touche entrée
		KeyListener listener = new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.CR)
					buttonPressed();
			}
		};
		searchFieldCombo.addKeyListener(listener);
		searchCriteriaCombo.addKeyListener(listener);
		searchText.addKeyListener(listener);

		// Initialisation du dialogue de coix de tache
		taskChooserDialog = new TaskChooserDialog(getShell(), modelMgr);

	}

	/**
	 * Initialise l'IHM.
	 */
	private void initialize() {
		GridLayout gridLayout = new GridLayout();
		this.setLayout(gridLayout);
		createGroup();
	}

	/**
	 * Lance le traitement de recherche de tache à partir du filtre spécifié.
	 */
	private void buttonPressed() {
		// Chargement des données
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				// Recherche en base de données
				TaskSearchFilter filter = new TaskSearchFilter();
				filter.setFieldIndex(searchFieldCombo.getSelectionIndex());
				filter.setCriteriaIndex(searchCriteriaCombo.getSelectionIndex());
				filter.setFieldValue(searchText.getText());
				Task[] tasks = modelMgr.getTasks(filter);

				// Traitement du résultat
				Task selectedTask = null;
				switch (tasks.length) {
				case 0:
					MessageDialog
							.openInformation(
									getShell(),
									Strings.getString("TaskFinderPanel.titles.SEARCH_STATUS"), Strings.getString("TaskFinderPanel.errors.NOTHING_FOUND")); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case 1:
					selectedTask = tasks[0];
					break;
				default:
					taskChooserDialog.setTasks(tasks);
					if (taskChooserDialog.open() == Dialog.OK) {
						selectedTask = (Task) taskChooserDialog.getValue();
					}
					break;
				}

				// Notification de la sélection
				if (selectedTask != null) {
					Iterator<ITaskSelectionListener> it = taskSelectionListeners
							.iterator();
					while (it.hasNext()) {
						ITaskSelectionListener listener = it.next();
						listener.taskSelected(selectedTask);
					}
				}

				// Pas de retour
				return null;
			}
		};
		// Exécution
		safeRunner.run(getShell());
	}

	/**
	 * This method initializes searchCriteriaCombo
	 * 
	 */
	private void createSearchCriteriaCombo() {
		searchCriteriaCombo = new Combo(group, SWT.READ_ONLY);
	}

	/**
	 * This method initializes searchFieldCombo
	 * 
	 */
	private void createSearchFieldCombo() {
		searchFieldCombo = new Combo(group, SWT.READ_ONLY);
	}

	/**
	 * Ajoute un objet à l'écoute de la sélectiond d'une tache.
	 * 
	 * @param listener
	 *            le nouveau listener.
	 */
	public void addTaskListener(ITaskSelectionListener listener) {
		taskSelectionListeners.add(listener);
	}

	/**
	 * Supprime un listener.
	 * 
	 * @param listener
	 *            le listener.
	 */
	public void removeTaskListener(ITaskSelectionListener listener) {
		taskSelectionListeners.remove(listener);
	}

	/**
	 * This method initializes group
	 * 
	 */
	private void createGroup() {
		GridData gridData1 = new GridData();
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = 4;
		group = new Group(this, SWT.NONE);
		group.setText(Strings.getString("TaskFinderPanel.labels.SEARCH_FILTER")); //$NON-NLS-1$
		group.setLayout(gridLayout1);
		group.setLayoutData(gridData1);
		createSearchFieldCombo();
		createSearchCriteriaCombo();
		searchText = new Text(group, SWT.BORDER);
		searchText.setLayoutData(gridData);
		findButton = new Button(group, SWT.NONE);
		findButton.setText(Strings.getString("TaskFinderPanel.buttons.FIND")); //$NON-NLS-1$
		findButton
				.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
					public void widgetSelected(
							org.eclipse.swt.events.SelectionEvent e) {
						buttonPressed();
					}
				});
	}

} // @jve:decl-index=0:visual-constraint="10,10"
