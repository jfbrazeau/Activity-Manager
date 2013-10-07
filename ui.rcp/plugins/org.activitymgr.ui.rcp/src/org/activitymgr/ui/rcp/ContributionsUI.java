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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;


import org.activitymgr.core.DbException;
import org.activitymgr.core.ModelException;
import org.activitymgr.core.ModelMgr;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.IntervalContributions;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskContributions;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.CollaboratorsUI.ICollaboratorListener;
import org.activitymgr.ui.rcp.DatabaseUI.IDbStatusListener;
import org.activitymgr.ui.rcp.DurationsUI.IDurationListener;
import org.activitymgr.ui.rcp.TasksUI.ITaskListener;
import org.activitymgr.ui.rcp.dialogs.DialogException;
import org.activitymgr.ui.rcp.dialogs.ITaskChooserValidator;
import org.activitymgr.ui.rcp.dialogs.TaskChooserTreeWithHistoryDialog;
import org.activitymgr.ui.rcp.util.AbstractTableMgr;
import org.activitymgr.ui.rcp.util.ICollaboratorSelectionListener;
import org.activitymgr.ui.rcp.util.SWTHelper;
import org.activitymgr.ui.rcp.util.SafeRunner;
import org.activitymgr.ui.rcp.util.SelectableCollaboratorPanel;
import org.activitymgr.ui.rcp.util.TableOrTreeColumnsMgr;
import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

/**
 * IHM de gestion des contributions.
 */
public class ContributionsUI extends AbstractTableMgr implements
		IDbStatusListener, ICellModifier, SelectionListener, MenuListener,
		ITaskListener, ICollaboratorListener, ICollaboratorSelectionListener,
		IDurationListener, ITableFontProvider {

	/** Logger */
	private static Logger log = Logger.getLogger(ContributionsUI.class);

	/**
	 * Constantes associées aux colonnes de la table de saisie des contributions
	 */
	public static final int TASK_PATH_COLUMN_IDX = 0;
	public static final int TASK_NAME_COLUMN_IDX = 1;
	public static final int MONDAY_COLUMN_IDX = 2;
	public static final int TUESDAY_COLUMN_IDX = 3;
	public static final int WEDNESDAY_COLUMN_IDX = 4;
	public static final int THURSDAY_COLUMN_IDX = 5;
	public static final int FRIDAY_COLUMN_IDX = 6;
	public static final int SATURDAY_COLUMN_IDX = 7;
	public static final int SUNDAY_COLUMN_IDX = 8;
	private static TableOrTreeColumnsMgr tableColsMgr;

	/** Initiales des jours de la semaine */
	private static final String[] weekDaysInitials = new String[] {
			"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	};

	/**
	 * Objet utilisé dans le tableau pour marquer la ligne contenant les totaux
	 * des contributions
	 */
	private static class WeekContributionsSum {

		/** Instance singleton */
		private static WeekContributionsSum singleton = new WeekContributionsSum();

		/**
		 * Constructeur privé (singleton).
		 */
		private WeekContributionsSum() {
		}

		/**
		 * Retourne l'instance singleton de la classe.
		 * 
		 * @return l'instance singleton de la classe.
		 */
		public static WeekContributionsSum getInstance() {
			return singleton;
		}

	}

	/**
	 * Interface utilisée pour permettre l'écoute de la suppression ou de
	 * l'ajout de durées.
	 */
	public static interface IContributionListener {

		/**
		 * Indique qu'une contribution a été ajoutée au référentiel.
		 * 
		 * @param contribution
		 *            la contribution ajoutée.
		 */
		public void contributionAdded(Contribution contribution);

		/**
		 * Indique que des contributions ont été supprimées du référentiel.
		 * 
		 * @param contributions
		 *            les contributions supprimées.
		 */
		public void contributionsRemoved(Contribution[] contributions);

		/**
		 * Indique que des contributions ont été modifiée dans le référentiel.
		 * 
		 * @param contributions
		 *            les contributions modifiées.
		 */
		public void contributionsUpdated(Contribution[] contributions);

	}

	/** Model manager */
	private ModelMgr modelMgr;
	
	/** Listeners */
	private List<IContributionListener> listeners = new ArrayList<IContributionListener>();

	/** Viewer */
	private TableViewer tableViewer;

	/** Items de menu */
	private MenuItem newItem;
	private MenuItem removeItem;
	private MenuItem pasteItem;
	private MenuItem exportItem;

	/** Boutons d'action */
	private Button previousYearButton;
	private Button previousMonthButton;
	private Button previousWeekButton;
	private Button nextWeekButton;
	private Button nextMonthButton;
	private Button nextYearButton;

	/** Composant parent */
	private Composite parent;

	/** Liste des durées */
	private Duration[] durations;

	/** Table présentant la liste des collaborateurs */
	private SelectableCollaboratorPanel selectableCollaboratorPanel;

	/** Date associé au Lundi de la semaine */
	private Calendar currentMonday;

	/** Label contenant les dates de la semaine */
	private Label weekLabel;

	/** Popup permettant de choisir une tache */
	private TaskChooserTreeWithHistoryDialog taskChooserDialog;

	/** Editeur de durées */
	private ComboBoxCellEditor durationCellEditor;

	/** Presse papier */
	private Clipboard clipboard;

	/** Police de caractère utilisée pour les contribution */
	private Font normalFont;

	/** Police de caractère utilisée pour la ligne des totaux */
	private Font italicFont;

	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 * 
	 * @param tabItem
	 *            item parent.
	 * @param modelMgr
	 *            the model manager instance.
	 */
	public ContributionsUI(TabItem tabItem, ModelMgr modelMgr) {
		this(tabItem.getParent());
		tabItem.setControl(parent);
		this.modelMgr = modelMgr;
	}

	/**
	 * Constructeur par défaut.
	 * 
	 * @param parentComposite
	 *            composant parent.
	 */
	public ContributionsUI(Composite parentComposite) {
		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));

		// Liste des collaborateurs
		Label collaboratorsLabel = new Label(parent, SWT.NONE);
		collaboratorsLabel.setText(Strings
				.getString("ContributionsUI.labels.SELECT_A_COLLABORATOR")); //$NON-NLS-1$
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData.verticalAlignment = SWT.CENTER;
		collaboratorsLabel.setLayoutData(gridData);

		// Date
		weekLabel = new Label(parent, SWT.NONE);
		weekLabel.setAlignment(SWT.RIGHT);
		gridData = new GridData(SWT.FILL, SWT.NONE, false, false);
		gridData.verticalAlignment = SWT.RIGHT;
		gridData.widthHint = 200;
		weekLabel.setLayoutData(gridData);

		// Liste des collaborateurs
		gridData = new GridData(SWT.FILL, SWT.FILL, false, true);
		gridData.widthHint = 190;
		gridData.verticalSpan = 2;
		selectableCollaboratorPanel = new SelectableCollaboratorPanel(parent,
				gridData, modelMgr);
		selectableCollaboratorPanel.addSelectionListener(this);

		// Table
		final Table table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION
				| SWT.BORDER | SWT.HIDE_SELECTION);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 75;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setEnabled(true);

		// Création du viewer
		tableViewer = new TableViewer(table) {
			public void refresh() {
				super.refresh();
				// When the viewer is refreshed, the last line
				// with the sums must be refreshed at last
				refresh(WeekContributionsSum.getInstance());
			}

		};
		tableViewer.setCellModifier(this);
		tableViewer.setContentProvider(this);
		tableViewer.setLabelProvider(this);

		// Création des polices de caractère
		FontData tableFont = table.getFont().getFontData()[0];
		normalFont = table.getFont();
		italicFont = new Font(table.getDisplay(), tableFont.getName(),
				tableFont.getHeight(), SWT.ITALIC);

		// Configuration des colonnes
		tableColsMgr = new TableOrTreeColumnsMgr();
		tableColsMgr
				.addColumn(
						"TASK_PATH", Strings.getString("ContributionsUI.columns.TASK_PATH"), 200, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr
				.addColumn(
						"TASK NAME", Strings.getString("ContributionsUI.columns.TASK_NAME"), 100, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr
				.addColumn(
						"MONDAY", Strings.getString("ContributionsUI.columns.MONDAY"), 50, SWT.CENTER); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr
				.addColumn(
						"TUESDAY", Strings.getString("ContributionsUI.columns.TUESDAY"), 50, SWT.CENTER); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr
				.addColumn(
						"WEDNESDAY", Strings.getString("ContributionsUI.columns.WEDNESDAY"), 50, SWT.CENTER); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr
				.addColumn(
						"THURSDAY", Strings.getString("ContributionsUI.columns.THURSDAY"), 50, SWT.CENTER); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr
				.addColumn(
						"FRIDAY", Strings.getString("ContributionsUI.columns.FRIDAY"), 50, SWT.CENTER); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr
				.addColumn(
						"SATURDAY", Strings.getString("ContributionsUI.columns.SATURDAY"), 50, SWT.CENTER); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr
				.addColumn(
						"SUNDAY", Strings.getString("ContributionsUI.columns.SUNDAY"), 50, SWT.CENTER); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr.configureTable(tableViewer);

		// Configuration des éditeurs de cellules
		CellEditor[] editors = new CellEditor[9];
		durationCellEditor = new ComboBoxCellEditor(table, new String[] {},
				SWT.READ_ONLY) {
			protected Control createControl(Composite parent) {
				CCombo ccombo = (CCombo) super.createControl(parent);
				ccombo.setVisibleItemCount(10);
				return ccombo;
			}
		};

		editors[TASK_PATH_COLUMN_IDX] = null; // Read-only column
		editors[TASK_NAME_COLUMN_IDX] = new DialogCellEditor(table) {
			protected Object openDialogBox(Control cellEditorWindow) {
				Object result = null;
				// Positionnement de la valeur par défaut
				TaskContributions w = (TaskContributions) ((IStructuredSelection) tableViewer
						.getSelection()).getFirstElement();
				// Préparation du dialogue
				taskChooserDialog.setValidator(buildTaskChooserValidator());
				taskChooserDialog.setValue(w.getTask());
				if (taskChooserDialog.open() == Dialog.OK) {
					result = taskChooserDialog.getValue();
				}
				return result;
			}

			protected void updateContents(Object value) {
				Label defaultLabel = getDefaultLabel();
				if (defaultLabel == null)
					return;
				String text = ""; //$NON-NLS-1$
				if (value instanceof Task) {
					Task task = (Task) value;
					text = task.getName();
				} else if (value != null) {
					text = value.toString();
				}
				defaultLabel.setText(text);
			}
		};
		editors[MONDAY_COLUMN_IDX] = durationCellEditor;
		editors[TUESDAY_COLUMN_IDX] = durationCellEditor;
		editors[WEDNESDAY_COLUMN_IDX] = durationCellEditor;
		editors[THURSDAY_COLUMN_IDX] = durationCellEditor;
		editors[FRIDAY_COLUMN_IDX] = durationCellEditor;
		editors[SATURDAY_COLUMN_IDX] = durationCellEditor;
		editors[SUNDAY_COLUMN_IDX] = durationCellEditor;
		tableViewer.setCellEditors(editors);

		// Configuration du menu popup
		final Menu menu = new Menu(table);
		menu.addMenuListener(this);
		newItem = new MenuItem(menu, SWT.CASCADE);
		newItem.setText(Strings
				.getString("ContributionsUI.menuitems.NEW_CONTRIBUTION")); //$NON-NLS-1$
		newItem.addSelectionListener(this);
		pasteItem = new MenuItem(menu, SWT.CASCADE);
		pasteItem.setText(Strings.getString("ContributionsUI.menuitems.PASTE")); //$NON-NLS-1$
		pasteItem.addSelectionListener(this);
		removeItem = new MenuItem(menu, SWT.CASCADE);
		removeItem.setText(Strings
				.getString("ContributionsUI.menuitems.REMOVE")); //$NON-NLS-1$
		removeItem.addSelectionListener(this);
		exportItem = new MenuItem(menu, SWT.CASCADE);
		exportItem.setText(Strings
				.getString("ContributionsUI.menuitems.EXPORT")); //$NON-NLS-1$
		exportItem.addSelectionListener(this);
		table.setMenu(menu);

		// Ajout de KeyListeners pour faciliter le 'coller' d'une tache en
		// provenance
		// de l'onglet de gestion des tache
		// (Rq: les accélérateurs sont ignorés dans les menus contextuels)
		KeyListener keyListener = new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				Widget simulatedWidget = null;
				if ((e.keyCode == 'v') && (e.stateMask == SWT.CTRL))
					simulatedWidget = pasteItem;
				// else if ...
				if (simulatedWidget != null) {
					Event event = new Event();
					event.widget = simulatedWidget;
					SelectionEvent se = new SelectionEvent(event);
					widgetSelected(se);
				}
			}
		};
		parentComposite.addKeyListener(keyListener);
		table.addKeyListener(keyListener);

		// Panneau contenant les boutons de navigation
		Composite navigationButtonsPanel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		navigationButtonsPanel.setLayout(layout);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		// gridData.horizontalSpan = 2;
		navigationButtonsPanel.setLayoutData(gridData);

		// Panneau contenant les boutons 'Précédent'
		Composite previousButtonsPanel = new Composite(navigationButtonsPanel,
				SWT.NONE);
		previousButtonsPanel.setLayout(new FillLayout());
		gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.horizontalAlignment = SWT.LEFT;
		previousButtonsPanel.setLayoutData(gridData);
		previousYearButton = new Button(previousButtonsPanel, SWT.NONE);
		previousYearButton.setText(Strings
				.getString("ContributionsUI.buttons.PREVIOUS_YEAR")); //$NON-NLS-1$
		previousYearButton.setToolTipText(Strings
				.getString("ContributionsUI.buttons.PREVIOUS_YEAR_TOOLTIP")); //$NON-NLS-1$
		previousYearButton.addSelectionListener(this);
		previousMonthButton = new Button(previousButtonsPanel, SWT.NONE);
		previousMonthButton.setText(Strings
				.getString("ContributionsUI.buttons.PREVIOUS_MONTH")); //$NON-NLS-1$
		previousMonthButton.setToolTipText(Strings
				.getString("ContributionsUI.buttons.PREVIOUS_MONTH_TOOLTIP")); //$NON-NLS-1$
		previousMonthButton.addSelectionListener(this);
		previousWeekButton = new Button(previousButtonsPanel, SWT.NONE);
		previousWeekButton.setText(Strings
				.getString("ContributionsUI.buttons.PREVIOUS_WEEK")); //$NON-NLS-1$
		previousWeekButton.setToolTipText(Strings
				.getString("ContributionsUI.buttons.PREVIOUS_WEEK_TOOLTIP")); //$NON-NLS-1$
		previousWeekButton.addSelectionListener(this);

		// Panneau contenant les boutons 'Prochains'
		Composite nextButtonsPanel = new Composite(navigationButtonsPanel,
				SWT.NONE);
		nextButtonsPanel.setLayout(new FillLayout());
		gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.horizontalAlignment = SWT.RIGHT;
		nextButtonsPanel.setLayoutData(gridData);
		nextWeekButton = new Button(nextButtonsPanel, SWT.NONE);
		nextWeekButton.setText(Strings
				.getString("ContributionsUI.buttons.NEXT_WEEK")); //$NON-NLS-1$
		nextWeekButton.setToolTipText(Strings
				.getString("ContributionsUI.buttons.NEXT_WEEK_TOOLTIP")); //$NON-NLS-1$
		nextWeekButton.addSelectionListener(this);
		nextMonthButton = new Button(nextButtonsPanel, SWT.NONE);
		nextMonthButton.setText(Strings
				.getString("ContributionsUI.buttons.NEXT_MONTH")); //$NON-NLS-1$
		nextMonthButton.setToolTipText(Strings
				.getString("ContributionsUI.buttons.NEXT_MONTH_TOOLTIP")); //$NON-NLS-1$
		nextMonthButton.addSelectionListener(this);
		nextYearButton = new Button(nextButtonsPanel, SWT.NONE);
		nextYearButton.setText(Strings
				.getString("ContributionsUI.buttons.NEXT_YEAR")); //$NON-NLS-1$
		nextYearButton.setToolTipText(Strings
				.getString("ContributionsUI.buttons.NEXT_YEAR_TOOLTIP")); //$NON-NLS-1$
		nextYearButton.addSelectionListener(this);

		// Initialisation du popup de choix des taches
		taskChooserDialog = new TaskChooserTreeWithHistoryDialog(
				parent.getShell(), modelMgr);

		// Recherche du 1er Lundi précédent la date courante
		currentMonday = getMondayBefore(new GregorianCalendar());
		log.debug("Date courante : " + currentMonday); //$NON-NLS-1$

		// Création du presse papier
		clipboard = new Clipboard(parentComposite.getDisplay());
	}

	/**
	 * Retourne le premier lundi précédent la date spécifiée.
	 * 
	 * @param date
	 *            la date.
	 * @return le premier lundi précédent la date spécifiée.
	 */
	private static Calendar getMondayBefore(Calendar date) {
		Calendar dateCursor = (Calendar) date.clone();
		while (dateCursor.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
			dateCursor.add(Calendar.DATE, -1);
		return dateCursor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java
	 * .lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		// Chargement des données
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				// Mise à jour des dates de la semaine :
				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); //$NON-NLS-1$
				Calendar sunday = (Calendar) currentMonday.clone();
				sunday.add(Calendar.DATE, 6);
				weekLabel
						.setText(Strings
								.getString(
										"ContributionsUI.labels.WEEK", sdf.format(currentMonday.getTime()), sdf.format(sunday.getTime()))); //$NON-NLS-1$ //$NON-NLS-2$

				// Mise à jour du nom des colonnes
				TableColumn[] tableColumns = tableViewer.getTable()
						.getColumns();
				Calendar date = (Calendar) currentMonday.clone();
				for (int i = 2; i < tableColumns.length; i++) {
					TableColumn tableColumn = tableColumns[i];
					tableColumn.setText(weekDaysInitials[i - 2]
							+ date.get(Calendar.DAY_OF_MONTH));
					date.add(Calendar.DATE, 1);
				}

				Collaborator selectedCollaborator = selectableCollaboratorPanel
						.getSelectedCollaborator();
				List<Object> list = new ArrayList<Object>();
				if (selectedCollaborator != null) {
					// Recherche des taches déclarées pour cet utilisateur
					// pour la semaine courante (et la semaine passée pour
					// réafficher automatiquement les taches de la semaine
					// passée)
					Calendar fromDate = (Calendar) currentMonday.clone();
					fromDate.add(Calendar.DATE, -7);
					Calendar toDate = (Calendar) currentMonday.clone();
					toDate.add(Calendar.DATE, 6);
					IntervalContributions ic = modelMgr
							.getIntervalContributions(selectedCollaborator,
									null, null, fromDate, toDate);
					// The result contains the contributions of the previous
					// week
					// We truncate it before proceeding.
					for (TaskContributions tc : ic.getTaskContributions()) {
						Contribution[] newContribs = new Contribution[7];
						System.arraycopy(tc.getContributions(), 7,
								newContribs, 0, 7);
						tc.setContributions(newContribs);
					}
					list.addAll(Arrays.asList(ic.getTaskContributions()));
				}
				// Ajout d'un élément pour la ligne des totaux
				list.add(WeekContributionsSum.getInstance());
				// Retour du résultat
				return list.toArray();
			}
		};
		// Exécution
		Object result = (Object) safeRunner.run(parent.getShell());
		return (Object[]) (result != null ? result : new TaskContributions[] {});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object,
	 * java.lang.String)
	 */
	public boolean canModify(Object element, String property) {
		log.debug("ICellModifier.canModify(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		boolean canModify = false;
		// Cas de la ligne des totaux
		if (element == WeekContributionsSum.getInstance()) {
			canModify = false;
		}
		// Cas des autres lignes
		else {
			int propertyIdx = tableColsMgr.getColumnIndex(property);
			switch (propertyIdx) {
			case (TASK_PATH_COLUMN_IDX):
				canModify = false;
				break;
			case (TASK_NAME_COLUMN_IDX):
			case (MONDAY_COLUMN_IDX):
			case (TUESDAY_COLUMN_IDX):
			case (WEDNESDAY_COLUMN_IDX):
			case (THURSDAY_COLUMN_IDX):
			case (FRIDAY_COLUMN_IDX):
			case (SATURDAY_COLUMN_IDX):
			case (SUNDAY_COLUMN_IDX):
				canModify = true;
				break;
			default:
				throw new Error(
						Strings.getString("ContributionsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
			}
		}
		return canModify;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object,
	 * java.lang.String)
	 */
	public Object getValue(Object element, String property) {
		log.debug("ICellModifier.getValue(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		TaskContributions weekContributions = (TaskContributions) element;
		Object value = null;
		int columnIndex = tableColsMgr.getColumnIndex(property);
		switch (columnIndex) {
		case (TASK_PATH_COLUMN_IDX):
			throw new Error(
					Strings.getString("ContributionsUI.errors.TASK_PATH_CANNOT_BE_MODIFIED")); //$NON-NLS-1$
		case (TASK_NAME_COLUMN_IDX):
			value = weekContributions.getTask();
			break;
		case (MONDAY_COLUMN_IDX):
		case (TUESDAY_COLUMN_IDX):
		case (WEDNESDAY_COLUMN_IDX):
		case (THURSDAY_COLUMN_IDX):
		case (FRIDAY_COLUMN_IDX):
		case (SATURDAY_COLUMN_IDX):
		case (SUNDAY_COLUMN_IDX):
			Contribution contribution = weekContributions.getContributions()[columnIndex - 2];
			value = contribution != null ? getDurationIndex(contribution
					.getDurationId()) : null;
			// Par défaut on prend la première sélection
			if (value == null)
				value = new Integer(0);
			break;
		default:
			throw new Error(
					Strings.getString("ContributionsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
		}
		return value;
	}

	/**
	 * @return le numéro de la durée sélectionnée.
	 * @param duration
	 *            le durée considérée.
	 */
	private Integer getDurationIndex(long durationId) {
		Integer result = null;
		for (int i = 0; i < durations.length && result == null; i++) {
			if (durations[i].getId() == durationId)
				result = new Integer(i + 1); // +1 car l'index 0 correspond à la
												// valeur "vide"
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object,
	 * java.lang.String, java.lang.Object)
	 */
	public void modify(final Object element, String property, final Object value) {
		log.debug("ICellModifier.modify(" + element + ", " + property + ", " + value + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		TableItem item = (TableItem) element;
		final TaskContributions weekContributions = (TaskContributions) item
				.getData();
		final IBaseLabelProvider labelProvider = this;
		final int columnIndex = tableColsMgr.getColumnIndex(property);
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				switch (columnIndex) {
				case (TASK_PATH_COLUMN_IDX):
					throw new Error(
							Strings.getString("ContributionsUI.errors.TASK_PATH_CANNOT_BE_MODIFIED")); //$NON-NLS-1$
				case (TASK_NAME_COLUMN_IDX):
					Task task = (Task) value;
					weekContributions.setTask(task);
					List<Contribution> nonNullContributions = new ArrayList<Contribution>();
					// Récupération des contributions
					Contribution[] contributions = weekContributions
							.getContributions();
					// Suppression des contributions nulles
					for (int i = 0; i < contributions.length; i++) {
						Contribution contribution = contributions[i];
						if (contribution != null)
							nonNullContributions.add(contribution);
					}
					// Mise à jour des contributions
					contributions = (Contribution[]) nonNullContributions
							.toArray(new Contribution[nonNullContributions
									.size()]);
					modelMgr.changeContributionTask(contributions, task);
					// Notification des listeners
					notifyLabelProviderListener(new LabelProviderChangedEvent(
							labelProvider, weekContributions));
					notifyContributionsUpdated(contributions);
					break;
				case (MONDAY_COLUMN_IDX):
				case (TUESDAY_COLUMN_IDX):
				case (WEDNESDAY_COLUMN_IDX):
				case (THURSDAY_COLUMN_IDX):
				case (FRIDAY_COLUMN_IDX):
				case (SATURDAY_COLUMN_IDX):
				case (SUNDAY_COLUMN_IDX):
					Integer selectedIndex = (Integer) value;
					Contribution contribution = weekContributions
							.getContributions()[columnIndex - 2];
					// Cas d'une suppression (choix de la valeur N° 0 de la
					// liste)
					if (selectedIndex.intValue() == 0) {
						// Suppression effective en base si la contribution
						// existait
						if (contribution != null)
							modelMgr.removeContribution(contribution, true);
						weekContributions.getContributions()[columnIndex - 2] = null;
						// Notification des listeners
						notifyContributionsRemoved(new Contribution[] { contribution });
					}
					// Sinon création ou modification
					else {
						boolean create = (contribution == null);
						// Cas d'une création
						if (create) {
							Collaborator selectedCollaboprator = selectableCollaboratorPanel
									.getSelectedCollaborator();
							contribution = new Contribution();
							contribution.setContributorId(selectedCollaboprator
									.getId());
							contribution.setTaskId(weekContributions.getTask()
									.getId());
							Calendar date = (Calendar) currentMonday.clone();
							date.add(Calendar.DATE, columnIndex
									- MONDAY_COLUMN_IDX);
							contribution.setDate(date);
							weekContributions.getContributions()[columnIndex - 2] = contribution;
						}
						// Mise à jour des champs
						Duration duration = durations[selectedIndex.intValue() - 1];
						contribution.setDurationId(duration.getId());
						if (create) {
							modelMgr.createContribution(contribution, true);
							// Notification des listeners
							notifyContributionAdded(contribution);
						} else {
							modelMgr.updateContribution(contribution, true);
							// Notification des listeners
							notifyContributionsUpdated(new Contribution[] { contribution });
						}
					}
					// Notification des listeners
					notifyLabelProviderListener(new LabelProviderChangedEvent(
							labelProvider, weekContributions));
					// Mise à jour des totaux
					tableViewer.refresh(WeekContributionsSum.getInstance());
					break;
				default:
					throw new Error(
							Strings.getString("ContributionsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				return null;
			}
		};
		// Exécution
		safeRunner.run(parent.getShell());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITableFontProvider#getFont(java.lang.Object,
	 * int)
	 */
	public Font getFont(Object element, int columnIndex) {
		return element == WeekContributionsSum.getInstance() ? italicFont
				: normalFont;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang
	 * .Object, int)
	 */
	public String getColumnText(final Object element, final int columnIndex) {
		log.debug("ITableLabelProvider.getColumnText(" + element + ", " + columnIndex + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				String text = null;
				// Cas de la ligne des totaux
				if (element == WeekContributionsSum.getInstance()) {
					switch (columnIndex) {
					case (TASK_PATH_COLUMN_IDX):
						text = ""; //$NON-NLS-1$
						break;
					case (TASK_NAME_COLUMN_IDX):
						text = Strings
								.getString("ContributionsUI.labels.TOTAL"); //$NON-NLS-1$
						break;
					case (MONDAY_COLUMN_IDX):
					case (TUESDAY_COLUMN_IDX):
					case (WEDNESDAY_COLUMN_IDX):
					case (THURSDAY_COLUMN_IDX):
					case (FRIDAY_COLUMN_IDX):
					case (SATURDAY_COLUMN_IDX):
					case (SUNDAY_COLUMN_IDX):
						int tasksCount = tableViewer.getTable().getItemCount() - 1;
						int sum = 0;
						for (int i = 0; i < tasksCount; i++) {
							TaskContributions tc = (TaskContributions) tableViewer
									.getElementAt(i);
							// Sometimes, when a refresh is performed, the total
							// line is refreshed before the previous ones (it 
							// happens for an obscur reason when the table lines
							// count doesn't change after the refresh). In that
							// case (and only in that case), the other elements
							// are null. That is why the refresh method has been 
							// overrided in the tableviewer : after having performed
							// the normal refresh, a specific refresh is performed
							// on the last line to update the contribution sums.
							if (tc != null) {
								Contribution[] contributions = tc
										.getContributions();
								Contribution contribution = contributions[columnIndex - 2];
								if (contribution != null) {
									sum += contribution.getDurationId();
								}
							}
						}
						text = StringHelper.hundredthToEntry(sum);
						break;
					default:
						throw new Error(
								Strings.getString("ContributionsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
					}
				}
				// Cas des autres lignes
				else {
					TaskContributions weekContributions = (TaskContributions) element;
					switch (columnIndex) {
					case (TASK_PATH_COLUMN_IDX):
						// Construction du chemin de la tache
						text = weekContributions.getTaskCodePath();
						break;
					case (TASK_NAME_COLUMN_IDX):
						text = weekContributions.getTask().getName();
						break;
					case (MONDAY_COLUMN_IDX):
					case (TUESDAY_COLUMN_IDX):
					case (WEDNESDAY_COLUMN_IDX):
					case (THURSDAY_COLUMN_IDX):
					case (FRIDAY_COLUMN_IDX):
					case (SATURDAY_COLUMN_IDX):
					case (SUNDAY_COLUMN_IDX):
						Contribution contribution = weekContributions
								.getContributions()[columnIndex - 2];
						text = contribution != null ? StringHelper
								.hundredthToEntry(contribution.getDurationId())
								: ""; //$NON-NLS-1$
						break;
					default:
						throw new Error(
								Strings.getString("ContributionsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
					}
				}
				return text;
			}
		};
		// Exécution
		return (String) safeRunner.run(parent.getShell(), ""); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt
	 * .events.SelectionEvent)
	 */
	public void widgetSelected(final SelectionEvent e) {
		log.debug("SelectionListener.widgetSelected(" + e + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		final Object source = e.getSource();
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				TableItem[] selection = tableViewer.getTable().getSelection();
				// Cas d'une création
				if (newItem.equals(source)) {
					// Préparation du dialogue
					taskChooserDialog.setValidator(buildTaskChooserValidator());
					// Affichage du popup
					Task task = null;
					if (taskChooserDialog.open() == Dialog.OK) {
						task = (Task) taskChooserDialog.getValue();
						log.debug("Selected task=" + task); //$NON-NLS-1$
						addNewLineOrSelectTaskLine(task);
					}
				}
				// Cas d'une demande de 'collage' du contenu du presse papier
				else if (pasteItem.equals(source)) {
					String taskCodePath = (String) clipboard
							.getContents(TextTransfer.getInstance());
					Task task = modelMgr.getTaskByCodePath(taskCodePath);
					ITaskChooserValidator validator = buildTaskChooserValidator();
					// Validation de la conformité de la tache pour ajout dans
					// l'IHM
					validator.validateChoosenTask(task);
					// Sélection de la ligne ou ajout d'une nouvelle ligne
					addNewLineOrSelectTaskLine(task);
				}
				// Cas d'une suppression
				else if (removeItem.equals(source)) {
					TableItem selectedItem = selection[0];
					TaskContributions wc = (TaskContributions) selectedItem
							.getData();
					// Récupération des contributions
					Contribution[] contributions = wc.getContributions();
					// Suppression des contributions nulles
					List<Contribution> list = new ArrayList<Contribution>();
					for (int i = 0; i < contributions.length; i++) {
						Contribution contribution = contributions[i];
						if (contribution != null)
							list.add(contribution);
					}
					if (list.size() > 0) {
						// Suppression des contributions non nulles
						contributions = (Contribution[]) list
								.toArray(new Contribution[list.size()]);
						modelMgr.removeContributions(contributions);
						// Notification des listeners
						notifyContributionsRemoved(contributions);
					}
					// Mise à jour de l'IHM
					tableViewer.remove(wc);
					tableViewer.refresh(WeekContributionsSum.getInstance());
				}
				// Cas d'une demande d'export vers un fichier EXCEL
				else if (exportItem.equals(source)) {
					SWTHelper.exportToWorkBook(tableViewer.getTable());
				}
				// Cas d'un changement d'une année en arrière
				else if (previousYearButton.equals(source)) {
					currentMonday.add(Calendar.YEAR, -1);
					currentMonday = getMondayBefore(currentMonday);
					tableViewer.refresh();
				}
				// Cas d'un changement d'un mois en arrière
				else if (previousMonthButton.equals(source)) {
					currentMonday.add(Calendar.MONTH, -1);
					currentMonday = getMondayBefore(currentMonday);
					tableViewer.refresh();
				}
				// Cas d'un changement de semaine en arrière
				else if (previousWeekButton.equals(source)) {
					currentMonday.add(Calendar.DATE, -7);
					tableViewer.refresh();
				}
				// Cas d'un changement de semaine en avant
				else if (nextWeekButton.equals(source)) {
					currentMonday.add(Calendar.DATE, 7);
					tableViewer.refresh();
				}
				// Cas d'un changement d'un mois en avant
				else if (nextMonthButton.equals(source)) {
					currentMonday.add(Calendar.MONTH, 1);
					currentMonday = getMondayBefore(currentMonday);
					tableViewer.refresh();
				}
				// Cas d'un changement d'une année en avant
				else if (nextYearButton.equals(source)) {
					currentMonday.add(Calendar.YEAR, 1);
					currentMonday = getMondayBefore(currentMonday);
					tableViewer.refresh();
				}
				return null;
			}
		};
		// Exécution
		safeRunner.run(parent.getShell());
	}

	/**
	 * Prépare le dialogue de choix d'une tâche.
	 */
	private ITaskChooserValidator buildTaskChooserValidator() {
		// Création du valideur
		ITaskChooserValidator taskChooserValidator = new ITaskChooserValidator() {
			public void validateChoosenTask(Task selectedTask)
					throws DialogException {
				if (selectedTask.getSubTasksCount() > 0)
					throw new DialogException(
							Strings.getString("ContributionsUI.errors.PARENT_TASK_SELECTED"), null); //$NON-NLS-1$
			}
		};
		// Retour du résultat
		return taskChooserValidator;
	}

	/**
	 * Ajoute une ligne dans le tableau ou sélectionne celle déja existante pour
	 * la tache.
	 * 
	 * @param task
	 *            la tache associée à l'ajout ou la sélection.
	 * @throws DbException
	 *             levée en cas d'incident technique d'accès à la BDD.
	 * @throws ModelException
	 *             levée en cas d'incident fonctionnel.
	 */
	private void addNewLineOrSelectTaskLine(Task task) throws DbException,
			ModelException {
		// La tache est elle déja associée à une semaine de contributions
		TaskContributions weekContributions = null;
		TableItem[] items = tableViewer.getTable().getItems();
		for (int i = 0; i < items.length; i++) {
			Object data = items[i].getData();
			if (data instanceof TaskContributions) {
				Task currentTask = ((TaskContributions) data).getTask();
				if (task.equals(currentTask))
					weekContributions = (TaskContributions) data;
			}
		}
		// Si ce n'est pas le cas c'est une nouvelle ligne
		if (weekContributions == null) {
			weekContributions = new TaskContributions();
			weekContributions.setTaskCodePath(modelMgr.getTaskCodePath(task));
			weekContributions.setTask(task);
			weekContributions.setContributions(new Contribution[7]);
			int itemCount = tableViewer.getTable().getItemCount();
			// Ajout dans l'arbre
			tableViewer.insert(weekContributions, itemCount - 1);
		}
		// Sélection
		tableViewer.setSelection(new StructuredSelection(weekContributions),
				true);
		tableViewer.getTable().setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse
	 * .swt.events.SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent e) {
		widgetSelected(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.MenuListener#menuShown(org.eclipse.swt.events.
	 * MenuEvent)
	 */
	public void menuShown(MenuEvent e) {
		log.debug("menuShown(" + e + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		boolean collaboratorSelected = (selectableCollaboratorPanel
				.getSelectedCollaborator() != null);
		TableItem[] selection = tableViewer.getTable().getSelection();
		boolean emptySelection = selection.length == 0;
		boolean singleSelection = selection.length == 1;
		newItem.setEnabled(collaboratorSelected
				&& (emptySelection || singleSelection));
		pasteItem.setEnabled(collaboratorSelected
				&& clipboard.getContents(TextTransfer.getInstance()) != null);
		removeItem.setEnabled(collaboratorSelected && singleSelection);
		exportItem.setEnabled(collaboratorSelected && true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.MenuListener#menuHidden(org.eclipse.swt.events
	 * .MenuEvent)
	 */
	public void menuHidden(MenuEvent e) {
		// Do nothing...
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener#databaseClosed()
	 */
	public void databaseClosed() {
		// Suppression des items de collaborateurs
		selectableCollaboratorPanel.databaseClosed();
		// Suppression des items de contributions
		Table table = tableViewer.getTable();
		TableItem[] items = table.getItems();
		for (int i = 0; i < items.length; i++) {
			items[i].dispose();
		}
	}

	/**
	 * Initialise l'IHM avec les données en base.
	 */
	private void initialize() {
		// Chargement du référentiel de durées
		loadDurations();

		// Chargement des collaborateurs et suppression de l'ancienne sélection
		// si elle existe
		selectableCollaboratorPanel.initialize();

		// Choix du collaborateur
		if (selectableCollaboratorPanel.getCollaboratorsCount() > 0) {
			selectableCollaboratorPanel.setSelectedIndex(0);
		}

		// Initialisation de la table
		tableViewer.setInput(ROOT_NODE);

	}

	/**
	 * Charge le référentiel de durées.
	 */
	private void loadDurations() {
		// Chargement de la liste des durées et des utilisateurs
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				// Chargement du référentiel de durées
				durations = modelMgr.getActiveDurations();
				String[] durationsStr = new String[durations.length + 1];
				durationsStr[0] = ""; //$NON-NLS-1$
				for (int i = 0; i < durations.length; i++)
					durationsStr[i + 1] = StringHelper
							.hundredthToEntry(durations[i].getId());
				durationCellEditor.setItems(durationsStr);
				return null;
			}
		};
		// Exécution
		safeRunner.run(parent.getShell());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.util.ICollaboratorSelectionListener#
	 * collaboratorSelected(jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorSelected(Collaborator selectedCollaborator) {
		tableViewer.refresh();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.CollaboratorsUI.CollaboratorListener#
	 * collaboratorAdded(jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorAdded(Collaborator collaborator) {
		selectableCollaboratorPanel.collaboratorAdded(collaborator);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.CollaboratorsUI.CollaboratorListener#
	 * collaboratorRemoved(jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorRemoved(Collaborator collaborator) {
		selectableCollaboratorPanel.collaboratorRemoved(collaborator);
		// Dans le cas ou le collaborateur supprimé est celui qui était
		// sélectionné...
		if (selectableCollaboratorPanel.getSelectedCollaborator() == null) {
			// ... on essaye d'en sélectionner un autre (si il y a d'autres
			// collaborateurs)...
			if (selectableCollaboratorPanel.getCollaboratorsCount() > 0)
				selectableCollaboratorPanel.setSelectedIndex(0);
			// ... et on rafraichit les contributions affichées
			tableViewer.refresh();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.CollaboratorsUI.CollaboratorListener#
	 * collaboratorUpdated(jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorUpdated(Collaborator collaborator) {
		selectableCollaboratorPanel.collaboratorUpdated(collaborator);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.CollaboratorsUI.ICollaboratorListener#
	 * collaboratorActivationStatusChanged
	 * (jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorActivationStatusChanged(Collaborator collaborator) {
		selectableCollaboratorPanel
				.collaboratorActivationStatusChanged(collaborator);
		// Dans le cas ou le collaborateur supprimé est celui qui était
		// sélectionné...
		if (selectableCollaboratorPanel.getSelectedCollaborator() == null) {
			// ... on essaye d'en sélectionner un autre (si il y a d'autres
			// collaborateurs)...
			if (selectableCollaboratorPanel.getCollaboratorsCount() > 0)
				selectableCollaboratorPanel.setSelectedIndex(0);
			// ... et on rafraichit les contributions affichées
			tableViewer.refresh();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener#databaseOpened()
	 */
	public void databaseOpened() {
		// Annulation de la sélection d'un collaborateur si une sélection
		// est en cours (peut arriver si la base a été réinstallée)
		selectableCollaboratorPanel.setSelectedCollaborator(null);
		initialize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.DurationsUI.IDurationListener#durationAdded(
	 * jfb.tools.activitymgr.core.beans.Duration)
	 */
	public void durationAdded(Duration duration) {
		loadDurations();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.DurationsUI.DurationListener#durationRemoved
	 * (jfb.tools.activitymgr.core.beans.Duration)
	 */
	public void durationRemoved(Duration duration) {
		loadDurations();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.DurationsUI.DurationListener#durationUpdated
	 * (jfb.tools.activitymgr.core.beans.Duration,
	 * jfb.tools.activitymgr.core.beans.Duration)
	 */
	public void durationUpdated(Duration oldDuration, Duration newDuration) {
		loadDurations();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.DurationsUI.IDurationListener#
	 * durationActivationStatusChanged
	 * (jfb.tools.activitymgr.core.beans.Duration)
	 */
	public void durationActivationStatusChanged(Duration duration) {
		loadDurations();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.TasksUI.TaskListener#taskAdded(jfb.tools.activitymgr
	 * .core.beans.Task)
	 */
	public void taskAdded(Task task) {
		// Transfert de la notification au popup de choix de tache
		taskChooserDialog.taskAdded(task);
		// Rien à faire par contre du côté de l'IHM de saisie des contributions
		// car une nouvelle
		// tâche ne pouvait forcément pas être déja affichée
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.TasksUI.TaskListener#taskRemoved(jfb.tools.
	 * activitymgr.core.beans.Task)
	 */
	public void taskRemoved(final Task removedTask) {
		new SafeRunner() {
			protected Object runUnsafe() throws Exception {
				// Transfert de la notification au popup de choix de tache
				taskChooserDialog.taskRemoved(removedTask);
				// Parcours des taches présentes dans le tableau
				int itemCount = tableViewer.getTable().getItemCount();
				int itemIdxToRemove = -1;
				for (int i = 0; i < itemCount; i++) {
					Object data = tableViewer.getElementAt(i);
					if (data != WeekContributionsSum.getInstance()) {
						TaskContributions weekContribution = (TaskContributions) data;
						Task currentTask = weekContribution.getTask();
						// Cas ou la tache supprimée est dans le tableau
						// dans ce cas, on sauvegarde le N° pour effectuer
						// la suppression par la suite
						if (currentTask.getId() == removedTask.getId()) {
							itemIdxToRemove = i;
						}
						// Autre cas : la tache supprimée est la soeur d'une des
						// taches parent
						// de la tache en cours ; c'est le cas si le chemin de
						// la tache en cours
						// commence par le chemin de la tache qui a été
						// supprimée
						else if (currentTask.getPath().startsWith(
								removedTask.getPath())) {
							String removedTaskFullpath = removedTask
									.getFullPath();
							String removedTaskSisterFullPath = currentTask
									.getFullPath().substring(0,
											removedTaskFullpath.length());
							// La tache n'est impactée que si sa tache parent se
							// trouvant être la soeur de
							// celle qui a été supprimée possède un numéro
							// supérieur à celui de la
							// tache supprimée
							if (removedTaskSisterFullPath
									.compareTo(removedTaskFullpath) > 0) {
								// Dans ce cas il faut mettre à jour le chemin
								// de la tache
								currentTask = modelMgr.getTask(currentTask
										.getId());
								weekContribution.setTask(currentTask);
								tableViewer.refresh(weekContribution);
							}
						}
					}
				}
				// Si on a trouvé l'item supprimé, on le supprime
				if (itemIdxToRemove >= 0)
					tableViewer.remove(tableViewer
							.getElementAt(itemIdxToRemove));
				return null;
			}

		}.run(parent.getShell());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.TasksUI.TaskListener#taskUpdated(jfb.tools.
	 * activitymgr.core.beans.Task)
	 */
	public void taskUpdated(final Task updatedTask) {
		// Transfert de la notification au popup de choix de tache
		taskChooserDialog.taskUpdated(updatedTask);
		// Parcours des taches présentes dans le tableau
		int itemCount = tableViewer.getTable().getItemCount();
		for (int i = 0; i < itemCount; i++) {
			Object data = tableViewer.getElementAt(i);
			if (data != WeekContributionsSum.getInstance()) {
				TaskContributions weekContribution = (TaskContributions) data;
				Task currentTask = weekContribution.getTask();
				// Cas ou la tache modifiée est dans le tableau
				if (currentTask.getId() == updatedTask.getId()) {
					weekContribution.setTask(updatedTask);
					tableViewer.refresh(weekContribution);
				}
				// Autre cas : la tache a modifiée est une tache
				// parent de la tache en cours
				else if (currentTask.getPath().startsWith(
						updatedTask.getFullPath())) {
					// Il faut faire un refresh pour que le chemin de code de la
					// tache
					// soit mis à jour
					tableViewer.refresh(weekContribution);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.TasksUI.ITaskListener#taskMoved(java.lang.String
	 * , jfb.tools.activitymgr.core.beans.Task)
	 */
	public void taskMoved(final String oldTaskFullpath, final Task movedTask) {
		new SafeRunner() {
			protected Object runUnsafe() throws Exception {
				// Transfert de la notification au popup de choix de tache
				taskChooserDialog.taskMoved(oldTaskFullpath, movedTask);
				// Déduction de l'ancien chemin de la tache à partir de l'ancien
				// chemin complet
				String oldTaskPath = oldTaskFullpath.substring(0,
						oldTaskFullpath.length() - 2);
				// Parcours des taches présentes dans le tableau
				int itemCount = tableViewer.getTable().getItemCount();
				for (int i = 0; i < itemCount; i++) {
					Object data = tableViewer.getElementAt(i);
					if (data != WeekContributionsSum.getInstance()) {
						TaskContributions weekContribution = (TaskContributions) data;
						Task currentTask = weekContribution.getTask();
						// Cas ou la tache modifiée est dans le tableau
						if (currentTask.getId() == movedTask.getId()) {
							currentTask = modelMgr.getTask(currentTask.getId());
							weekContribution.setTask(currentTask);
							tableViewer.refresh(weekContribution);
						}
						// Autre cas : la tache a déplacée est une tache
						// parent de la tache en cours
						else if (currentTask.getPath().startsWith(
								oldTaskFullpath)) {
							// Il faut faire un refresh pour que le chemin de
							// code de la tache
							// soit mis à jour
							currentTask = modelMgr.getTask(currentTask.getId());
							weekContribution.setTask(currentTask);
							tableViewer.refresh(weekContribution);
						}
						// Autre cas : la tache déplacée est la soeur d'une des
						// taches parent
						// de la tache en cours
						else if (currentTask.getPath().startsWith(oldTaskPath)) {
							// Dans ce cas il faut mettre à jour le chemin de la
							// tache
							currentTask = modelMgr.getTask(currentTask.getId());
							weekContribution.setTask(currentTask);
							tableViewer.refresh(weekContribution);
						}
					}
				}
				return null;
			}

		}.run(parent.getShell());
	}

	/**
	 * Ajoute un listener.
	 * 
	 * @param listener
	 *            le nouveau listener.
	 */
	public void addContributionListener(IContributionListener listener) {
		listeners.add(listener);
	}

	/**
	 * Ajoute un listener.
	 * 
	 * @param listener
	 *            le nouveau listener.
	 */
	public void removeContributionListener(IContributionListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Notifie les listeners qu'une contribution a été ajoutée.
	 * 
	 * @param newContribution
	 *            la contribution ajoutée.
	 */
	private void notifyContributionAdded(Contribution newContribution) {
		Iterator<IContributionListener> it = listeners.iterator();
		while (it.hasNext()) {
			IContributionListener listener = it.next();
			listener.contributionAdded(newContribution);
		}
	}

	/**
	 * Notifie les listeners que des contributions ont été supprimées.
	 * 
	 * @param contributions
	 *            les contributions supprimées.
	 */
	private void notifyContributionsRemoved(Contribution[] contributions) {
		Iterator<IContributionListener> it = listeners.iterator();
		while (it.hasNext()) {
			IContributionListener listener = it.next();
			listener.contributionsRemoved(contributions);
		}
	}

	/**
	 * Notifie les listeners que des contributions ont été modifiées.
	 * 
	 * @param contributions
	 *            les contributions modifiées.
	 */
	private void notifyContributionsUpdated(Contribution[] contributions) {
		Iterator<IContributionListener> it = listeners.iterator();
		while (it.hasNext()) {
			IContributionListener listener = it.next();
			listener.contributionsUpdated(contributions);
		}
	}

}
