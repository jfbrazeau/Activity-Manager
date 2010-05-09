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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Collaborator;
import jfb.tools.activitymgr.core.beans.Contribution;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tools.activitymgr.core.util.StringHelper;
import jfb.tools.activitymgr.ui.CollaboratorsUI.CollaboratorListener;
import jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener;
import jfb.tools.activitymgr.ui.DurationsUI.DurationListener;
import jfb.tools.activitymgr.ui.dialogs.DialogException;
import jfb.tools.activitymgr.ui.dialogs.ITaskChooserValidator;
import jfb.tools.activitymgr.ui.dialogs.TasksChooserDialog;
import jfb.tools.activitymgr.ui.util.AbstractTableMgr;
import jfb.tools.activitymgr.ui.util.SWTHelper;
import jfb.tools.activitymgr.ui.util.SafeRunner;
import jfb.tools.activitymgr.ui.util.TableOrTreeColumnsMgr;
import jfb.tools.activitymgr.ui.util.WeekContributions;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * IHM de gestion des contributions.
 */
public class ContributionsUI 
	extends AbstractTableMgr 
	implements DbStatusListener, ICellModifier, SelectionListener, MenuListener, CollaboratorListener, DurationListener {

	/** Logger */
	private static Logger log = Logger.getLogger(ContributionsUI.class);

	/** Constantes associées aux colonnes */
	public static final int TASK_PATH_COLUMN_IDX = 0;
	public static final int TASK_COLUMN_IDX =      1;
	public static final int MONDAY_COLUMN_IDX =    2;
	public static final int TUESDAY_COLUMN_IDX =   3;
	public static final int WEDNESDAY_COLUMN_IDX = 4;
	public static final int THURSDAY_COLUMN_IDX =  5;
	public static final int FRIDAY_COLUMN_IDX =    6;
	public static final int SATURDAY_COLUMN_IDX =  7;
	public static final int SUNDAY_COLUMN_IDX =    8;
	private static TableOrTreeColumnsMgr tableColsMgr;

	/** Initiales des jours de la semaine */
	private static final String[] weekDaysInitials = new String[] {
		"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"
	};

	/** Viewer */
	private TableViewer tableViewer;

	/** Items de menu */
	private MenuItem newItem;
	//private MenuItem removeItem; // A implémenter...
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
	private long[] durations;
	
	/** Liste des collaborateurs */
	private Collaborator[] collaborators;
	private Combo collaboratorsCombo;

	/** Date associé au Lundi de la semaine */
	private Calendar currentMonday;

	/** Label contenant les dates de la semaine */
	private Label weekLabel;
	
	/** Popup permettant de choisir une tache */
	private TasksChooserDialog taskChooserDialog;

	/** Editeur de durées */
	private ComboBoxCellEditor durationCellEditor;
	
	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 * @param tabItem item parent.
	 */
	public ContributionsUI(TabItem tabItem) {
		this(tabItem.getParent());
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par défaut.
	 * @param parentComposite composant parent.
	 */
	public ContributionsUI(Composite parentComposite) {
		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(3, false));

		// Liste des collaborateurs
		Label collaboratorsLabel = new Label(parent, SWT.NONE);
		collaboratorsLabel.setText("Collaborator : ");
		GridData gridData = new GridData(SWT.NONE, SWT.FILL, false, false);
		gridData.verticalAlignment = SWT.CENTER;
		collaboratorsLabel.setLayoutData(gridData);
		collaboratorsCombo = new Combo(parent, SWT.READ_ONLY);
		collaboratorsCombo.setVisibleItemCount(20);
		gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
		collaboratorsCombo.setLayoutData(gridData);
		collaboratorsCombo.addSelectionListener(this);

		// Date
		weekLabel = new Label(parent, SWT.NONE);
		weekLabel.setAlignment(SWT.RIGHT);
		gridData = new GridData(SWT.FILL, SWT.NONE, false, false);
		gridData.verticalAlignment = SWT.CENTER;
		gridData.widthHint = 200;
		weekLabel.setLayoutData(gridData);

		// Table
		final Table table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.HIDE_SELECTION);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.horizontalSpan = 3;
		gridData.heightHint = 75;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setEnabled(true);

		// Création du viewer
		tableViewer = new TableViewer(table);
		tableViewer.setCellModifier(this);
		tableViewer.setContentProvider(this);
		tableViewer.setLabelProvider(this);

		// Configuration des colonnes
		tableColsMgr = new TableOrTreeColumnsMgr();
		tableColsMgr.addColumn("TASK_PATH", "Task path", 200, SWT.LEFT);
		tableColsMgr.addColumn("TASK", "Task", 100, SWT.LEFT);
		tableColsMgr.addColumn("MONDAY", "MON", 50, SWT.CENTER);
		tableColsMgr.addColumn("TUESDAY", "TUE", 50, SWT.CENTER);
		tableColsMgr.addColumn("WEDNESDAY", "WED", 50, SWT.CENTER);
		tableColsMgr.addColumn("THURSDAY", "TUE", 50, SWT.CENTER);
		tableColsMgr.addColumn("FRIDAY", "FRI", 50, SWT.CENTER);
		tableColsMgr.addColumn("SATURDAY", "SAT", 50, SWT.CENTER);
		tableColsMgr.addColumn("SUNDAY", "SUN", 50, SWT.CENTER);
		tableColsMgr.configureTable(tableViewer);

		// Configuration des éditeurs de cellules
		CellEditor[] editors = new CellEditor[9];
		durationCellEditor = new ComboBoxCellEditor(table, new String[] {}, SWT.READ_ONLY) {
			protected Control createControl(Composite parent) {
				CCombo ccombo = (CCombo) super.createControl(parent);
				ccombo.setVisibleItemCount(10);
				return ccombo;
			}
		};
		
		editors[TASK_PATH_COLUMN_IDX] = null; // Read-only column
		editors[TASK_COLUMN_IDX] = new DialogCellEditor(table) {
			protected Object openDialogBox(Control cellEditorWindow) {
				Object result = null;
				// Préparation du dialogue
				prepareTaskChooserDialog();
				// Positionnement de la valeur par défaut
				WeekContributions w =
					(WeekContributions)
						((IStructuredSelection)tableViewer.getSelection())
							.getFirstElement();
				taskChooserDialog.setValue(w.getTask());
				if (taskChooserDialog.open()==Dialog.OK) {
					result = taskChooserDialog.getValue();
				}
				return result;
			}
		    protected void updateContents(Object value) {
		        Label defaultLabel = getDefaultLabel();
		    	if (defaultLabel == null)
		            return;
		        String text = "";
	        	if (value instanceof Task) {
		        	Task task = (Task) value;
		        	text = task.getName();
	        	}
	        	else if (value != null) {
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
		newItem.setText("New contribution");
		newItem.addSelectionListener(this);
		// TODO Implémenter la suppression
		//removeItem = new MenuItem(menu, SWT.CASCADE);
		//removeItem.setText("Remove");
		//removeItem.addSelectionListener(this);
		exportItem = new MenuItem(menu, SWT.CASCADE);
		exportItem.setText("Export");
		exportItem.addSelectionListener(this);
		table.setMenu(menu);
		
		// Panneau contenant les boutons de navigation
		Composite navigationButtonsPanel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		navigationButtonsPanel.setLayout(layout);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.horizontalSpan = 3;
		navigationButtonsPanel.setLayoutData(gridData);
		
		// Panneau contenant les boutons 'Précédent'
		Composite previousButtonsPanel = new Composite(navigationButtonsPanel, SWT.NONE);
		previousButtonsPanel.setLayout(new FillLayout());
		gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.horizontalAlignment = SWT.LEFT;
		previousButtonsPanel.setLayoutData(gridData);
		previousYearButton = new Button(previousButtonsPanel, SWT.NONE);
		previousYearButton.setText("<< year");
		previousYearButton.setToolTipText("Previous year");
		previousYearButton.addSelectionListener(this);
		previousMonthButton = new Button(previousButtonsPanel, SWT.NONE);
		previousMonthButton.setText("<< month");
		previousMonthButton.setToolTipText("Previous month");
		previousMonthButton.addSelectionListener(this);
		previousWeekButton = new Button(previousButtonsPanel, SWT.NONE);
		previousWeekButton.setText("<< week");
		previousWeekButton.setToolTipText("Previous week");
		previousWeekButton.addSelectionListener(this);
		
		// Panneau contenant les boutons 'Prochains'
		Composite nextButtonsPanel = new Composite(navigationButtonsPanel, SWT.NONE);
		nextButtonsPanel.setLayout(new FillLayout());
		gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.horizontalAlignment = SWT.RIGHT;
		nextButtonsPanel.setLayoutData(gridData);
		nextWeekButton = new Button(nextButtonsPanel, SWT.NONE);
		nextWeekButton.setText("week >>");
		nextWeekButton.setToolTipText("Next week");
		nextWeekButton.addSelectionListener(this);
		nextMonthButton = new Button(nextButtonsPanel, SWT.NONE);
		nextMonthButton.setText("month >>");
		nextMonthButton.setToolTipText("Next month");
		nextMonthButton.addSelectionListener(this);
		nextYearButton = new Button(nextButtonsPanel, SWT.NONE);
		nextYearButton.setText("year >>");
		nextYearButton.setToolTipText("Next year");
		nextYearButton.addSelectionListener(this);
		
		// Initialisation du popup de choix des taches
		taskChooserDialog = new TasksChooserDialog(parent.getShell());
		
		// Recherche du 1° Lundi précédent la date courante
		currentMonday = getMondayBefore(new GregorianCalendar());
		log.debug("Date courante : " + currentMonday);
	}

	/**
	 * Retourne le premier lundi précédent la date spécifiée.
	 * @param date la date.
	 * @return le premier lundi précédent la date spécifiée.
	 */
	private static Calendar getMondayBefore(Calendar date) {
		Calendar dateCursor = (Calendar) date.clone();
		while (dateCursor.get(Calendar.DAY_OF_WEEK)!=Calendar.MONDAY) 
			dateCursor.add(Calendar.DATE, -1);
		return dateCursor;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		// Chargement des données
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				// Mise à jour des dates de la semaine :
				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
				Calendar sunday = (Calendar) currentMonday.clone();
				sunday.add(Calendar.DATE, 6);
				weekLabel.setText("Week : " + sdf.format(currentMonday.getTime()) + " -> " + sdf.format(sunday.getTime()));
				
				// Mise à jour du nom des colonnes
				TableColumn[] tableColumns = tableViewer.getTable().getColumns();
				Calendar date = (Calendar) currentMonday.clone();
				for (int i=2; i<tableColumns.length; i++) {
					TableColumn tableColumn = tableColumns[i];
					tableColumn.setText(weekDaysInitials[i-2] + date.get(Calendar.DAY_OF_MONTH));
					date.add(Calendar.DATE, 1);
				}

				Collaborator selectedCollaborator = getSelectedCollaborator();
				// Recherche des taches déclarées pour cet utilisateur 
				// pour la semaine courante
				Calendar fromDate = (Calendar) currentMonday.clone();
				fromDate.add(Calendar.DATE, -7);
				Calendar toDate = (Calendar) currentMonday.clone();
				toDate.add(Calendar.DATE, 6);
				ArrayList list = new ArrayList();
				if (selectedCollaborator!=null) {
					Task[] tasks = ModelMgr.getTasks(selectedCollaborator, fromDate, toDate);
					// Ajout des tâches dans le tableau
					for (int i=0; i<tasks.length; i++) {
						Task task = tasks[i];
						list.add(getWeekContributions(task));
					}
				}
				// Pas de retour
				return (WeekContributions[]) list.toArray(new WeekContributions[list.size()]);
			}
		};
		// Exécution
		Object result = (Object) safeRunner.run(parent.getShell());
		return (WeekContributions[]) (result!=null ? result : new WeekContributions[] {});
	}
	
	/**
	 * @return le collaborateur sélectionné.
	 */
	private Collaborator getSelectedCollaborator() {
		int selectedIndex = collaboratorsCombo.getSelectionIndex();
		return selectedIndex>=0 ? collaborators[selectedIndex] : null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
	 */
	public boolean canModify(Object element, String property) {
		log.debug("ICellModifier.canModify(" + element + ", " + property + ")");
		boolean result = false;
		int propertyIdx = tableColsMgr.getColumnIndex(property);
		switch (propertyIdx) {
			case (TASK_PATH_COLUMN_IDX) :
				result = false;
				break;
			case (TASK_COLUMN_IDX) :
			case (MONDAY_COLUMN_IDX) :
			case (TUESDAY_COLUMN_IDX) :
			case (WEDNESDAY_COLUMN_IDX) :
			case (THURSDAY_COLUMN_IDX) :
			case (FRIDAY_COLUMN_IDX) :
			case (SATURDAY_COLUMN_IDX) :
			case (SUNDAY_COLUMN_IDX) :
				result = true;
				break;
			default : throw new Error("Colonne inconnue");
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
	 */
	public Object getValue(Object element, String property) {
		log.debug("ICellModifier.getValue(" + element + ", " + property + ")");
		WeekContributions weekContributions = (WeekContributions) element;
		Object value = null;
		int columnIndex = tableColsMgr.getColumnIndex(property);
		switch (columnIndex) {
			case (TASK_PATH_COLUMN_IDX) :
				throw new Error("Task path colum is not supposed to be modified");
			case (TASK_COLUMN_IDX) :
				value = weekContributions.getTask();
				break;
			case (MONDAY_COLUMN_IDX) :
			case (TUESDAY_COLUMN_IDX) :
			case (WEDNESDAY_COLUMN_IDX) :
			case (THURSDAY_COLUMN_IDX) :
			case (FRIDAY_COLUMN_IDX) :
			case (SATURDAY_COLUMN_IDX) :
			case (SUNDAY_COLUMN_IDX) :
				Contribution contribution = weekContributions.getContribution(columnIndex-2);
				value = contribution!=null ? getDurationIndex(contribution.getDuration()) : new Integer(0);
				break;
			default : throw new Error("Colonne inconnue");
		}
		return value;
	}

	/**
	 * @return le numéro de la durée sélectionnée.
	 * @param duration le durée considérée.
	 */
	private Integer getDurationIndex(long duration) {
		Integer result = null;
		for (int i=0; i<durations.length && result == null; i++) {
			if (durations[i]==duration)
				result = new Integer(i+1); // +1 car l'index 0 correspond à la valeur "vide"
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	public void modify(final Object element, String property, final Object value) {
		log.debug("ICellModifier.modify(" + element + ", " + property + ", " + value + ")");
		TableItem item = (TableItem) element;
		final WeekContributions weekContributions = (WeekContributions) item.getData();
		final IBaseLabelProvider labelProvider = this;
		final int columnIndex = tableColsMgr.getColumnIndex(property);
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				switch (columnIndex) {
					case (TASK_PATH_COLUMN_IDX) :
						throw new Error("Task path colum is not supposed to be modified");
					case (TASK_COLUMN_IDX) :
						Task task = (Task) value;
						weekContributions.setTask(task);
						ArrayList nonNullContributions = new ArrayList();
						// Récupération des contributions
						Contribution[] contributions = weekContributions.getContributions();
						// Suppression des contributions nulles
						for (int i=0; i<contributions.length; i++) {
							Contribution contribution = contributions[i];
							if (contribution!=null)
								nonNullContributions.add(contribution);
						}
						// Mise à jour des contributions
						contributions = (Contribution[]) nonNullContributions.toArray(new Contribution[nonNullContributions.size()]);
						ModelMgr.changeContributionTask(contributions, task);
						// Notification des listeners
						notifyLabelProviderListener(new LabelProviderChangedEvent(labelProvider, weekContributions));
						break;
					case (MONDAY_COLUMN_IDX) :
					case (TUESDAY_COLUMN_IDX) :
					case (WEDNESDAY_COLUMN_IDX) :
					case (THURSDAY_COLUMN_IDX) :
					case (FRIDAY_COLUMN_IDX) :
					case (SATURDAY_COLUMN_IDX) :
					case (SUNDAY_COLUMN_IDX) :
						Integer selectedIndex = (Integer) value;
						Contribution contribution = weekContributions.getContribution(columnIndex-2);
						// Cas d'une suppression (choix de la valeu N° 0 de la liste)
						if (selectedIndex.intValue()==0) {
							// Suppression effective en base si la contribution existait
							if (contribution!=null)
								ModelMgr.removeContribution(contribution);
							weekContributions.setContribution(columnIndex-2, null);
						}
						// Sinon création ou modification
						else {
							long duration = durations[selectedIndex.intValue() - 1];
							boolean create = (contribution==null);
							// Cas d'une création
							if (create) {
								contribution = new Contribution();
								contribution.setContributorId(getSelectedCollaborator().getId());
								contribution.setTaskId(weekContributions.getTask().getId());
								Calendar date = (Calendar) currentMonday.clone();
								date.add(Calendar.DATE, columnIndex - MONDAY_COLUMN_IDX);
								contribution.setDate(date);
								weekContributions.setContribution(columnIndex-2, contribution);
							}
							// Mise à jour des champs
							contribution.setDuration(duration);
							if (create)
								ModelMgr.createContribution(contribution);
							else
								ModelMgr.updateContribution(contribution);
						}
						// Notification des listeners
						notifyLabelProviderListener(new LabelProviderChangedEvent(labelProvider, weekContributions));
						break;
					default : throw new Error("Colonne inconnue");
				}
				return null;
			}
		};
		// Exécution
		safeRunner.run(parent.getShell());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	public String getColumnText(final Object element, final int columnIndex) {
		log.debug("ITableLabelProvider.getColumnText(" + element + ", " + columnIndex + ")");
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				WeekContributions weekContributions = (WeekContributions) element;
				String text = null;
				switch (columnIndex) {
					case (TASK_PATH_COLUMN_IDX) :
						// Construction du chemin de la tache
						Task task = weekContributions.getTask();
						text = ModelMgr.getTaskCodePath(task);
						break;
					case (TASK_COLUMN_IDX) :
						text = weekContributions.getTask().getName();
						break;
					case (MONDAY_COLUMN_IDX) :
					case (TUESDAY_COLUMN_IDX) :
					case (WEDNESDAY_COLUMN_IDX) :
					case (THURSDAY_COLUMN_IDX) :
					case (FRIDAY_COLUMN_IDX) :
					case (SATURDAY_COLUMN_IDX) :
					case (SUNDAY_COLUMN_IDX) :
						Contribution contribution = weekContributions.getContribution(columnIndex-2);
						text = contribution!=null ? StringHelper.hundredthToEntry(contribution.getDuration()) : "";
						break;
					default : throw new Error("Colonne inconnue");
				}
				return text;
			}
		};
		// Exécution
		return (String) safeRunner.run(parent.getShell(), "");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetSelected(final SelectionEvent e) {
		log.debug("SelectionListener.widgetSelected(" + e + ")");
		final Object source = e.getSource();
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				//TableItem[] selection = tableViewer.getTable().getSelection();
				// Cas d'une création
				if (newItem.equals(source)) {
					// Préparation du dialogue
					prepareTaskChooserDialog();
					// Affichage du popup
					Task task = null;
					if (taskChooserDialog.open()==Dialog.OK) {
						task = (Task) taskChooserDialog.getValue();
						log.debug("Selected task=" + task);
						newLine(task);
					}
				}
				// Cas d'une suppression
				// TODO implémenter
				//else if (removeItem.equals(source)) {
				//}
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
				// Cas d'un changement de collaborateur
				else if (collaboratorsCombo.equals(source)) {
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
	private void prepareTaskChooserDialog() {
		TableItem[] items = tableViewer.getTable().getItems();
		// Recherche des taches ayant déjà été sélectionnées
		final Task[] forbiddenTasks = new Task[items.length];
		for (int i=0; i<items.length; i++)
			forbiddenTasks[i] = ((WeekContributions) items[i].getData()).getTask();
		// Création du valideur
		taskChooserDialog.setValidator(new ITaskChooserValidator() {
			public void validateChoosenTask(Task selectedTask) throws DialogException {
				if (selectedTask.getSubTasksCount()>0)
					throw new DialogException("This is a parent task. Please choose one of its subtasks.", null);
				if (forbiddenTasks!=null 
						&& Arrays.asList(forbiddenTasks).contains(selectedTask))
					throw new DialogException("This task is already present in the main window. Please choose another task.", null);
			}
		});
	}
	
	/**
	 * Ajoute une ligne dans le tableau.
	 * @param task la tache associée à l'ajout.
	 * @throws DbException levée en cas d'incident technique d'accès à la BDD.
	 * @throws ModelException levée en cas d'incident fonctionnel.
	 */
	private void newLine(Task task) throws DbException, ModelException {
		WeekContributions weekContributions = getWeekContributions(task);
		// Ajout dans l'arbre
		tableViewer.add(weekContributions);
		tableViewer.setSelection(new StructuredSelection(weekContributions), true);
	}
	
	/**
	 * Construit la liste des contributions d'une semaine.
	 * @param task la tache considérée.
	 * @return la liste des contributions d'une semaine.
	 * @throws DbException levée en cas d'incident technique d'accès à la BDD.
	 * @throws ModelException levée en cas d'incident fonctionnel.
	 */
	private WeekContributions getWeekContributions(Task task) throws DbException, ModelException {
		// Création de la ligne de semaine
		WeekContributions weekContributions = new WeekContributions();
		weekContributions.setTask(task);
		// Parcours des jours
		Calendar fromDate = (Calendar) currentMonday.clone();
		Calendar toDate = (Calendar) currentMonday.clone();
		toDate.add(Calendar.DATE, 6);
		// Récupération des contributions
		weekContributions.setContributions(ModelMgr.getDaysContributions(getSelectedCollaborator(),task, fromDate, toDate));
		return weekContributions;
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent e) {
		widgetSelected(e);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MenuListener#menuShown(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuShown(MenuEvent e) {
		log.debug("menuShown(" + e + ")");
		TableItem[] selection = tableViewer.getTable().getSelection();
		boolean emptySelection = selection.length==0;
		boolean singleSelection = selection.length==1;
		newItem.setEnabled(emptySelection || singleSelection);
		// La suppression n'est pas implémentée
		//removeItem.setEnabled(!emptySelection);
		exportItem.setEnabled(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MenuListener#menuHidden(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuHidden(MenuEvent e) {
		// Do nothing...
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener#databaseClosed()
	 */
	public void databaseClosed() {
		collaboratorsCombo.setItems(new String[] {});
		Table table = tableViewer.getTable();
		TableItem[] items = table.getItems();
		for (int i=0; i<items.length; i++) {
			items[i].dispose();
		}
	}
	
	/**
	 * Initialise l'IHM avec les données en base.
	 */
	private void initUI() {
		// Chargement de la liste des durées et des utilisateurs
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				// Chargement du référentiel de durées
				durations = ModelMgr.getDurations();
				String[] durationsStr = new String[durations.length + 1];
				durationsStr[0] = "";
				for (int i=0; i<durations.length; i++)
					durationsStr[i+1] = StringHelper.hundredthToEntry(durations[i]);
				durationCellEditor.setItems(durationsStr);
				
				// Chargement des collaborateurs
				collaborators = ModelMgr.getCollaborators();
				int nbClbs = collaborators.length;
				String[] names = new String[nbClbs];
				for (int i=0; i<nbClbs; i++) {
					Collaborator c = collaborators[i];
					names[i] = c.getFirstName() + " " + c.getLastName();
				}
				collaboratorsCombo.setItems(names);
				return null;
			}
		};
		// Exécution
		safeRunner.run(parent.getShell());

		// Choix du collaborateur
		if (collaborators!=null && collaborators.length>0) {
			collaboratorsCombo.select(0);
		}

		// Initialisation de la table
		tableViewer.setInput(ROOT_NODE);
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.CollaboratorsUI.CollaboratorListener#collaboratorAdded(jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorAdded(Collaborator collaborator) {
		// Réinitialisation de l'IHM
		initUI();
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.CollaboratorsUI.CollaboratorListener#collaboratorRemoved(jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorRemoved(Collaborator collaborator) {
		// Réinitialisation de l'IHM
		initUI();
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.CollaboratorsUI.CollaboratorListener#collaboratorUpdated(jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorUpdated(Collaborator collaborator) {
		// Réinitialisation de l'IHM
		initUI();
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener#databaseOpened()
	 */
	public void databaseOpened() {
		initUI();
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.DurationsUI.DurationListener#durationAdded(long)
	 */
	public void durationAdded(long duration) {
		initUI();
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.DurationsUI.DurationListener#durationRemoved(long)
	 */
	public void durationRemoved(long duration) {
		initUI();
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.DurationsUI.DurationListener#durationUpdated(long, long)
	 */
	public void durationUpdated(long oldDuration, long newDuration) {
		initUI();
	}
	
}
