/*
 * Copyright (c) 2004-2010, Jean-Fran�ois Brazeau. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Contribution;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tools.activitymgr.core.beans.TaskSums;
import jfb.tools.activitymgr.core.util.StringHelper;
import jfb.tools.activitymgr.core.util.Strings;
import jfb.tools.activitymgr.ui.ContributionsUI.IContributionListener;
import jfb.tools.activitymgr.ui.DatabaseUI.IDbStatusListener;
import jfb.tools.activitymgr.ui.dialogs.ContributionsViewerDialog;
import jfb.tools.activitymgr.ui.dialogs.DialogException;
import jfb.tools.activitymgr.ui.dialogs.ITaskChooserValidator;
import jfb.tools.activitymgr.ui.dialogs.TaskChooserTreeWithHistoryDialog;
import jfb.tools.activitymgr.ui.util.AbstractTableMgr;
import jfb.tools.activitymgr.ui.util.ITaskSelectionListener;
import jfb.tools.activitymgr.ui.util.SWTHelper;
import jfb.tools.activitymgr.ui.util.SafeRunner;
import jfb.tools.activitymgr.ui.util.TableOrTreeColumnsMgr;
import jfb.tools.activitymgr.ui.util.TaskFinderPanel;
import jfb.tools.activitymgr.ui.util.UITechException;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/**
 * IHM de gestion des t�ches.
 */
public class TasksUI extends AbstractTableMgr implements IDbStatusListener,
		ICellModifier, SelectionListener, MenuListener, ITreeContentProvider,
		ITableColorProvider, IContributionListener {

	/** Logger */
	private static Logger log = Logger.getLogger(TasksUI.class);

	/** Constantes associ�es aux colonnes */
	public static final int NAME_COLUMN_IDX = 0;
	public static final int CODE_COLUMN_IDX = 1;
	public static final int INITIAL_FUND_COLUMN_IDX = 2;
	public static final int INITIALLY_CONSUMED_COLUMN_IDX = 3;
	public static final int CONSUMED_COLUMN_IDX = 4;
	public static final int TODO_COLUMN_IDX = 5;
	public static final int DELTA_COLUMN_IDX = 6;
	public static final int COMMENT_COLUMN_IDX = 7;
	private static TableOrTreeColumnsMgr treeColsMgr;

	/**
	 * Interface utilis�e pour permettre l'�coute de la suppression ou de
	 * l'ajout de taches.
	 */
	public static interface ITaskListener {

		/**
		 * Indique qu'une tache a �t� ajout�e au r�f�rentiel.
		 * 
		 * @param task
		 *            la tache ajout�e.
		 */
		public void taskAdded(Task task);

		/**
		 * Indique qu'une tache a �t� supprim�e du r�f�rentiel.
		 * 
		 * @param task
		 *            la tache supprim�e.
		 */
		public void taskRemoved(Task task);

		/**
		 * Indique qu'une tache a �t� modifi�e duans le r�f�rentiel.
		 * 
		 * @param task
		 *            la tache modifi�e.
		 */
		public void taskUpdated(Task task);

		/**
		 * Indique qu'une tache a �t� d�plac�e duans le r�f�rentiel.
		 * 
		 * @param oldTaskFullpath
		 *            ancien chemin de la tache.
		 * @param task
		 *            la tache d�plac�e.
		 */
		public void taskMoved(String oldTaskFullpath, Task task);
	}

	/** Listeners */
	private List<ITaskListener> listeners = new ArrayList<ITaskListener>();

	/** Viewer */
	private TreeViewer treeViewer;

	/** Items de menu */
	private MenuItem newTaskItem;
	private MenuItem newSubtaskItem;
	private MenuItem moveUpItem;
	private MenuItem moveDownItem;
	private MenuItem moveBeforeAnotherTaskItem;
	private MenuItem moveAfterAnotherTaskItem;
	private MenuItem moveToAnotherTaskItem;
	private MenuItem moveToRootItem;
	private MenuItem copyItem;
	private MenuItem removeItem;
	private MenuItem expandItem;
	private MenuItem collapseItem;
	private MenuItem listTaskContributionsItem;
	private MenuItem refreshItem;
	private MenuItem exportItem;

	/** Composant parent */
	private Composite parent;

	/** Table contenant les sommes associ�es aux taches */
	private Map<Task, TaskSums> tasksSums = new HashMap<Task, TaskSums>();

	/** Popup permettant de choisir une tache */
	private TaskChooserTreeWithHistoryDialog taskChooserDialog;

	/** Popup permettant de lister les contributions d'une tache */
	private ContributionsViewerDialog contribsViewerDialog;

	/** Panneau de recherche de tache */
	private TaskFinderPanel taskFinderPanel;

	/** Couleur de fond utilis�e pour les zones non modifiables */
	private Color disabledBGColor;

	/** Couleur de police de caract�re utilis�e pour les zones non modifiables */
	private Color disabledFGColor;

	/**
	 * Bool�en permettant de savoir si un refresh doit �tre ex�cut� lors du
	 * prochain paint
	 */
	private boolean needRefresh = false;

	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 * 
	 * @param tabItem
	 *            item parent.
	 */
	public TasksUI(TabItem tabItem) {
		this(tabItem.getParent());
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par d�faut.
	 * 
	 * @param parentComposite
	 *            composant parent.
	 */
	public TasksUI(Composite parentComposite) {
		// Cr�ation du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));

		// Panneau permettant de recherche une tache
		taskFinderPanel = new TaskFinderPanel(parent);
		GridData gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
		taskFinderPanel.setLayoutData(gridData);
		taskFinderPanel.addTaskListener(new ITaskSelectionListener() {
			public void taskSelected(Task selectedTask) {
				treeViewer.setSelection(new StructuredSelection(selectedTask));
				treeViewer.getTree().setFocus();
			}
		});

		// Arbre tableau
		final Tree tree = new Tree(parent, SWT.MULTI | SWT.FULL_SELECTION
				| SWT.BORDER | SWT.HIDE_SELECTION);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 75;
		tree.setLayoutData(gridData);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		tree.setEnabled(true);

		// Cr�ation du viewer
		treeViewer = new TreeViewer(tree);
		treeViewer.setCellModifier(this);
		treeViewer.setContentProvider(this);
		treeViewer.setLabelProvider(this);

		// Cr�ation des polices de caract�re
		disabledBGColor = tree.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);
		disabledFGColor = tree.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_DARK_SHADOW);

		// Configuration des colonnes
		treeColsMgr = new TableOrTreeColumnsMgr();
		treeColsMgr
				.addColumn(
						"NAME", Strings.getString("TasksUI.column.TASK_NAME"), 200, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		treeColsMgr
				.addColumn(
						"CODE", Strings.getString("TasksUI.columns.TASK_CODE"), 70, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		treeColsMgr
				.addColumn(
						"BUDGET", Strings.getString("TasksUI.columns.TASK_BUDGET"), 70, SWT.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		treeColsMgr
				.addColumn(
						"INI_CONS", Strings.getString("TasksUI.columns.TASK_INITIALLY_CONSUMED"), 70, SWT.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		treeColsMgr
				.addColumn(
						"CONSUMED", Strings.getString("TasksUI.columns.TASK_CONSUMED"), 70, SWT.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		treeColsMgr
				.addColumn(
						"TODO", Strings.getString("TasksUI.columns.TASK_ESTIMATED_TIME_TO_COMPLETE"), 70, SWT.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		treeColsMgr
				.addColumn(
						"DELTA", Strings.getString("TasksUI.columns.TASK_DELTA"), 70, SWT.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		treeColsMgr
				.addColumn(
						"COMMENT", Strings.getString("TasksUI.columns.TASK_COMMENT"), 200, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		treeColsMgr.configureTree(treeViewer);

		// Configuration des �diteurs de cellules
		CellEditor[] editors = new CellEditor[8];
		editors[NAME_COLUMN_IDX] = new TextCellEditor(tree);
		editors[CODE_COLUMN_IDX] = new TextCellEditor(tree);
		editors[INITIAL_FUND_COLUMN_IDX] = new TextCellEditor(tree);
		editors[INITIALLY_CONSUMED_COLUMN_IDX] = new TextCellEditor(tree);
		editors[CONSUMED_COLUMN_IDX] = null;
		editors[TODO_COLUMN_IDX] = new TextCellEditor(tree);
		editors[DELTA_COLUMN_IDX] = null;
		editors[COMMENT_COLUMN_IDX] = new TextCellEditor(tree);
		treeViewer.setCellEditors(editors);

		// Initialisation des popups
		taskChooserDialog = new TaskChooserTreeWithHistoryDialog(
				parent.getShell());
		contribsViewerDialog = new ContributionsViewerDialog(parent.getShell());

		// Configuration du menu popup
		final Menu menu = new Menu(tree);
		menu.addMenuListener(this);
		// Sous-menu 'Nouveau'
		MenuItem newItem = new MenuItem(menu, SWT.CASCADE);
		newItem.setText(Strings.getString("TasksUI.menuitems.NEW")); //$NON-NLS-1$
		Menu newMenu = new Menu(newItem);
		newItem.setMenu(newMenu);
		newTaskItem = new MenuItem(newMenu, SWT.CASCADE);
		newTaskItem.setText(Strings.getString("TasksUI.menuitems.NEW_TASK")); //$NON-NLS-1$
		newTaskItem.addSelectionListener(this);
		newSubtaskItem = new MenuItem(newMenu, SWT.CASCADE);
		newSubtaskItem.setText(Strings
				.getString("TasksUI.menuitems.NEW_SUBTASK")); //$NON-NLS-1$
		newSubtaskItem.addSelectionListener(this);
		// Sous-menu 'D�placer'
		MenuItem moveToItem = new MenuItem(menu, SWT.CASCADE);
		moveToItem.setText(Strings.getString("TasksUI.menuitems.MOVE")); //$NON-NLS-1$
		Menu moveToMenu = new Menu(moveToItem);
		moveToItem.setMenu(moveToMenu);
		moveUpItem = new MenuItem(moveToMenu, SWT.CASCADE);
		moveUpItem.setText(Strings.getString("TasksUI.menuitems.MOVE_UP")); //$NON-NLS-1$
		moveUpItem.addSelectionListener(this);
		moveDownItem = new MenuItem(moveToMenu, SWT.CASCADE);
		moveDownItem.setText(Strings.getString("TasksUI.menuitems.MOVE_DOWN")); //$NON-NLS-1$
		moveDownItem.addSelectionListener(this);
		moveBeforeAnotherTaskItem = new MenuItem(moveToMenu, SWT.CASCADE);
		moveBeforeAnotherTaskItem.setText(Strings
				.getString("TasksUI.menuitems.MOVE_BEFORE_ANOTHER_TASK")); //$NON-NLS-1$
		moveBeforeAnotherTaskItem.addSelectionListener(this);
		moveAfterAnotherTaskItem = new MenuItem(moveToMenu, SWT.CASCADE);
		moveAfterAnotherTaskItem.setText(Strings
				.getString("TasksUI.menuitems.MOVE_AFTER_ANOTHER_TASK")); //$NON-NLS-1$
		moveAfterAnotherTaskItem.addSelectionListener(this);
		moveToAnotherTaskItem = new MenuItem(moveToMenu, SWT.CASCADE);
		moveToAnotherTaskItem.setText(Strings
				.getString("TasksUI.menuitems.MOVE_UNDER_ANOTHER_TASK")); //$NON-NLS-1$
		moveToAnotherTaskItem.addSelectionListener(this);
		moveToRootItem = new MenuItem(moveToMenu, SWT.CASCADE);
		moveToRootItem.setText(Strings
				.getString("TasksUI.menuitems.MOVE_UNDER_ROOT")); //$NON-NLS-1$
		moveToRootItem.addSelectionListener(this);
		copyItem = new MenuItem(menu, SWT.CASCADE);
		copyItem.setText(Strings.getString("TasksUI.menuitems.COPY")); //$NON-NLS-1$
		copyItem.addSelectionListener(this);
		removeItem = new MenuItem(menu, SWT.CASCADE);
		removeItem.setText(Strings.getString("TasksUI.menuitems.REMOVE")); //$NON-NLS-1$
		removeItem.addSelectionListener(this);
		expandItem = new MenuItem(menu, SWT.CASCADE);
		expandItem.setText(Strings.getString("TasksUI.menuitems.EXPAND_ALL")); //$NON-NLS-1$
		expandItem.addSelectionListener(this);
		collapseItem = new MenuItem(menu, SWT.CASCADE);
		collapseItem.setText(Strings
				.getString("TasksUI.menuitems.COLLAPSE_ALL")); //$NON-NLS-1$
		collapseItem.addSelectionListener(this);
		listTaskContributionsItem = new MenuItem(menu, SWT.CASCADE);
		listTaskContributionsItem.setText(Strings
				.getString("TasksUI.menuitems.LIST_CONTRIBUTIONS")); //$NON-NLS-1$
		listTaskContributionsItem.addSelectionListener(this);
		refreshItem = new MenuItem(menu, SWT.CASCADE);
		refreshItem.setText(Strings.getString("TasksUI.menuitems.REFRESH")); //$NON-NLS-1$
		refreshItem.addSelectionListener(this);
		exportItem = new MenuItem(menu, SWT.CASCADE);
		exportItem.setText(Strings.getString("TasksUI.menuitems.EXPORT")); //$NON-NLS-1$
		exportItem.addSelectionListener(this);
		tree.setMenu(menu);

		log.debug("UI initialization done"); //$NON-NLS-1$
		// Ajout de KeyListeners pour faciliter le d�placement vers le bas et
		// vers le haut des taches
		// (Rq: les acc�l�rateurs sont ignor�s dans les menus contextuels)
		KeyListener keyListener = new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				Widget simulatedWidget = null;
				if ((e.keyCode == SWT.ARROW_UP) && (e.stateMask == SWT.CTRL))
					simulatedWidget = moveUpItem;
				else if ((e.keyCode == SWT.ARROW_DOWN)
						&& (e.stateMask == SWT.CTRL))
					simulatedWidget = moveDownItem;
				else if ((e.keyCode == 'c') && (e.stateMask == SWT.CTRL))
					simulatedWidget = copyItem;
				if (simulatedWidget != null) {
					Event event = new Event();
					event.widget = simulatedWidget;
					SelectionEvent se = new SelectionEvent(event);
					widgetSelected(se);
				}
			}
		};
		parentComposite.addKeyListener(keyListener);
		tree.addKeyListener(keyListener);

		// Ajout d'un listener permettant de d�tecter lorsque le
		// composant est affich� (passage d'un onglet � l'autre)
		parent.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent paintevent) {
				if (needRefresh) {
					needRefresh = false;
					System.out.println("Rafrachissement de l'UI");
					treeViewer.refresh();
				}

			}
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java
	 * .lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(null);
	}

	/**
	 * Retourne les sommes associ�es � la tache sp�cifi�e.
	 * 
	 * @param task
	 *            la tache pour laquelle on d�sire conna�tre les cumuls.
	 * @return les sommes associ�es � la tache.
	 * @throws ModelException
	 *             lev� en cas de d�tection d'incoh�rence au niveau du mod�le.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base de
	 *             donn�es.
	 */
	private TaskSums getTasksSums(Task task) throws ModelException, DbException {
		// 1� lecture dans le cache
		TaskSums taskSums = (TaskSums) tasksSums.get(task);
		if (taskSums == null) {
			synchronized (tasksSums) {
				// 2� lecture dans le cache (synchronis�e)
				taskSums = (TaskSums) tasksSums.get(task);
				if (taskSums == null) {
					taskSums = ModelMgr.getTaskSums(task, null, null);
					// D�pot dans le cache
					tasksSums.put(task, taskSums);
				}
			}
		}
		return taskSums;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object,
	 * java.lang.String)
	 */
	public boolean canModify(Object element, String property) {
		log.debug("ICellModifier.canModify(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final Task task = (Task) element;
		final int propertyIdx = treeColsMgr.getColumnIndex(property);
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				boolean hasChilds = (task.getSubTasksCount() > 0);
				boolean canModify = false;
				switch (propertyIdx) {
				case (NAME_COLUMN_IDX):
				case (CODE_COLUMN_IDX):
					canModify = true;
					break;
				case (INITIAL_FUND_COLUMN_IDX):
				case (INITIALLY_CONSUMED_COLUMN_IDX):
				case (TODO_COLUMN_IDX):
					canModify = !hasChilds;
					break;
				case (CONSUMED_COLUMN_IDX):
				case (DELTA_COLUMN_IDX):
					canModify = false;
					break;
				case (COMMENT_COLUMN_IDX):
					canModify = true;
					break;
				default:
					throw new UITechException(
							Strings.getString("TasksUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				return canModify ? Boolean.TRUE : Boolean.FALSE;
			}
		};
		// Retour du r�sultat
		return ((Boolean) safeRunner.run(parent.getShell())).booleanValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object,
	 * java.lang.String)
	 */
	public Object getValue(Object element, String property) {
		log.debug("ICellModifier.getValue(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final Task task = (Task) element;
		final int propertyIdx = treeColsMgr.getColumnIndex(property);
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				TaskSums taskSums = getTasksSums(task);
				boolean hasChilds = (task.getSubTasksCount() > 0);
				String value = null;
				switch (propertyIdx) {
				case (NAME_COLUMN_IDX):
					value = task.getName();
					break;
				case (CODE_COLUMN_IDX):
					value = task.getCode();
					break;
				case (INITIAL_FUND_COLUMN_IDX):
					value = StringHelper.hundredthToEntry(!hasChilds ? task
							.getBudget() : taskSums.getBudgetSum());
					break;
				case (INITIALLY_CONSUMED_COLUMN_IDX):
					value = StringHelper.hundredthToEntry(!hasChilds ? task
							.getInitiallyConsumed() : taskSums
							.getInitiallyConsumedSum());
					break;
				case (CONSUMED_COLUMN_IDX):
					value = StringHelper.hundredthToEntry(taskSums
							.getConsumedSum()
							+ taskSums.getInitiallyConsumedSum());
					break;
				case (TODO_COLUMN_IDX):
					value = StringHelper.hundredthToEntry(!hasChilds ? task
							.getTodo() : taskSums.getTodoSum());
					break;
				case (DELTA_COLUMN_IDX):
					long delta = taskSums.getBudgetSum()
							- taskSums.getInitiallyConsumedSum()
							- taskSums.getConsumedSum() - taskSums.getTodoSum();
					value = StringHelper.hundredthToEntry(delta);
					break;
				case (COMMENT_COLUMN_IDX):
					value = task.getComment() != null ? task.getComment() : ""; //$NON-NLS-1$
					break;
				default:
					throw new UITechException(
							Strings.getString("TasksUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				// Retour du r�sultat
				return value;
			}
		};
		// Ex�cution
		return safeRunner.run(parent.getShell());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object,
	 * java.lang.String, java.lang.Object)
	 */
	public void modify(Object element, String property, final Object value) {
		log.debug("ICellModifier.modify(" + element + ", " + property + ", " + value + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		final TreeItem item = (TreeItem) element;
		final Task task = (Task) item.getData();
		final int columnIdx = treeColsMgr.getColumnIndex(property);
		final IBaseLabelProvider labelProvider = this;
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				boolean parentsMustBeRefreshed = false;
				switch (columnIdx) {
				case (NAME_COLUMN_IDX):
					task.setName((String) value);
					break;
				case (CODE_COLUMN_IDX):
					task.setCode((String) value);
					break;
				case (INITIAL_FUND_COLUMN_IDX):
					long newInitialFund = StringHelper
							.entryToHundredth((String) value);
					task.setBudget(newInitialFund);
					parentsMustBeRefreshed = true;
					break;
				case (INITIALLY_CONSUMED_COLUMN_IDX):
					long newInitiallyConsumed = StringHelper
							.entryToHundredth((String) value);
					task.setInitiallyConsumed(newInitiallyConsumed);
					parentsMustBeRefreshed = true;
					break;
				case (TODO_COLUMN_IDX):
					long newTodo = StringHelper
							.entryToHundredth((String) value);
					task.setTodo(newTodo);
					parentsMustBeRefreshed = true;
					break;
				case (COMMENT_COLUMN_IDX):
					String comment = (String) value;
					if (comment != null)
						comment = comment.trim();
					// Si le commentaire est vide, il devient nul
					if ("".equals(comment)) //$NON-NLS-1$
						comment = null;
					task.setComment((String) value);
					break;
				case (CONSUMED_COLUMN_IDX):
				case (DELTA_COLUMN_IDX):
					throw new UITechException(
							Strings.getString("TasksUI.errros.READ_ONLY_COLUMN")); //$NON-NLS-1$
				default:
					throw new UITechException(
							Strings.getString("TasksUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				// Mise � jour en base
				ModelMgr.updateTask(task);
				// Mise � jour des labels
				if (parentsMustBeRefreshed) {
					// Mise � jour des sommes des taches parentes
					updateBranchItemsSums(item);
				} else {
					// Notification de la mise � jour uniquement pour la tache
					notifyLabelProviderListener(new LabelProviderChangedEvent(
							labelProvider, new Object[] { task }));
				}
				// Notification de la mise � jour de la tache pour les listeners
				notifyTaskUpdated(task);
				return null;
			}
		};
		// Ex�cution
		safeRunner.run(parent.getShell());
	}

	/**
	 * Met � jour les sommes associ�s aux taches de la branche associ�e � l'item
	 * sp�cifi�.
	 * 
	 * @param item
	 *            l'item de tableau.
	 * @throws ModelException
	 *             lev�e en cas d'invalidit� associ�e au mod�le.
	 * @throws DbException
	 *             lev� en cas d'incident technique avec la base de donn�es.
	 */
	private void updateBranchItemsSums(TreeItem item) throws ModelException,
			DbException {
		List<Task> list = new ArrayList<Task>();
		// Nettoyage du cache
		TreeItem cursor = item;
		while (cursor != null) {
			Task taskCursor = (Task) cursor.getData();
			log.debug("Update task " + taskCursor.getName()); //$NON-NLS-1$
			tasksSums.remove(taskCursor);
			list.add(0, taskCursor);
			cursor = cursor.getParentItem();
		}
		// Notification de la mise � jour (ce qui recharge automatiquement
		// le cache des sommes de taches)
		notifyLabelProviderListener(new LabelProviderChangedEvent(this,
				list.toArray()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.
	 * Object)
	 */
	public boolean hasChildren(Object element) {
		log.debug("ITreeContentProvider.getChildren(" + element + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		final Task task = (Task) element;
		return task.getSubTasksCount() > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.
	 * Object)
	 */
	public Object[] getChildren(Object parentElement) {
		log.debug("ITreeContentProvider.getChildren(" + parentElement + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		final Task parentTask = (Task) parentElement;
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				Task[] subTasks = ModelMgr.getSubtasks(parentTask);
				return subTasks;
			}
		};
		Object[] result = (Object[]) safeRunner.run(parent.getShell(),
				new Object[] {});
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object
	 * )
	 */
	public Object getParent(Object element) {
		log.debug("ITreeContentProvider.getParent(" + element + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		final Task task = (Task) element;
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				Task parentTask = ModelMgr.getParentTask(task);
				return parentTask == null ? treeViewer.getInput() : parentTask;
			}
		};
		// Ex�cution du traitement
		Object result = (Object) safeRunner.run(parent.getShell());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang
	 * .Object, int)
	 */
	public String getColumnText(Object element, int columnIndex) {
		log.debug("ITableLabelProvider.getColumnText(" + element + ", " + columnIndex + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return (String) getValue(element,
				treeColsMgr.getColumnCode(columnIndex));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITableColorProvider#getBackground(java.lang
	 * .Object, int)
	 */
	public Color getBackground(Object element, int columnIndex) {
		return canModify(element, treeColsMgr.getColumnCode(columnIndex)) ? treeViewer
				.getTree().getBackground() : disabledBGColor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITableColorProvider#getForeground(java.lang
	 * .Object, int)
	 */
	public Color getForeground(Object element, int columnIndex) {
		return canModify(element, treeColsMgr.getColumnCode(columnIndex)) ? treeViewer
				.getTree().getForeground() : disabledFGColor;
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
				TreeItem[] selection = treeViewer.getTree().getSelection();
				// Cas d'une cr�ation (m�me niveau)
				if (newTaskItem.equals(source)) {
					// R�cup�ration du noeud parent
					TreeItem parentItem = selection.length > 0 ? selection[0]
							.getParentItem() : null;
					Task parentTask = parentItem == null ? null
							: (Task) parentItem.getData();
					// Cr�ation de la tache
					Task newTask = newTask(parentTask);
					// Notification des listeners
					notifyTaskAdded(newTask);
				}
				// Cas d'une cr�ation de sous tache
				else if (newSubtaskItem.equals(source)) {
					Task selectedTask = (Task) selection[0].getData();
					Task newTask = newTask(selectedTask);
					// Notification des listeners
					notifyTaskAdded(newTask);
				}
				// Cas d'une demande de d�placement vers le haut
				else if (moveUpItem.equals(source)) {
					Task selectedTask = (Task) selection[0].getData();
					String oldTaskFullpath = selectedTask.getFullPath();
					ModelMgr.moveUpTask(selectedTask);
					// Mise � jour de l'IHM
					treeViewer.refresh(selection[0].getParent().getData(),
							false);
					// Notification des listeners
					notifyTaskMoved(oldTaskFullpath, selectedTask);
				}
				// Cas d'une demande de d�placement vers le haut
				else if (moveDownItem.equals(source)) {
					Task selectedTask = (Task) selection[0].getData();
					String oldTaskFullpath = selectedTask.getFullPath();
					ModelMgr.moveDownTask(selectedTask);
					// Mise � jour de l'IHM
					treeViewer.refresh(selection[0].getParent().getData(),
							false);
					// Notification des listeners
					notifyTaskMoved(oldTaskFullpath, selectedTask);
				}
				// Cas d'une demande de d�placement avant ou apr�s une autre
				// tache
				else if (moveBeforeAnotherTaskItem.equals(source)
						|| moveAfterAnotherTaskItem.equals(source)) {
					Task selectedTask = (Task) selection[0].getData();
					final Task finalSelectedTask = selectedTask;
					// Cr�ation du valideur
					taskChooserDialog.setValidator(new ITaskChooserValidator() {
						public void validateChoosenTask(Task choosenTask)
								throws DialogException {
							if (finalSelectedTask.equals(choosenTask))
								throw new DialogException(
										"Please select another task", null); //$NON-NLS-1$
						}
					});
					taskChooserDialog.setValue(selectedTask);
					if (taskChooserDialog.open() == Dialog.OK) {
						Task chosenTask = (Task) taskChooserDialog.getValue();
						String oldTaskFullpath = selectedTask.getFullPath();
						// Traitement du changement �ventuel de parent
						if (!chosenTask.getPath()
								.equals(selectedTask.getPath())) {
							// D�placement
							ModelMgr.moveTask(selectedTask, chosenTask);
							// Rafraichissement de la tache
							selectedTask = ModelMgr.getTask(selectedTask
									.getId());
						}
						// D�placement de la tache
						int targetNumber = chosenTask.getNumber();
						if (moveBeforeAnotherTaskItem.equals(source)
								&& targetNumber > selectedTask.getNumber())
							targetNumber--;
						else if (moveAfterAnotherTaskItem.equals(source)
								&& targetNumber < selectedTask.getNumber())
							targetNumber++;
						ModelMgr.moveTaskUpOrDown(selectedTask, targetNumber);
						// Notification des listeners
						notifyTaskMoved(oldTaskFullpath, selectedTask);
						// Mise � jour de l'IHM
						treeViewer.refresh();
					}
				}
				// Cas d'une demande de d�placement vers une autre tache
				else if (moveToAnotherTaskItem.equals(source)) {
					Task taskToMove = (Task) selection[0].getData();
					// R�cup�ration du noeud parent
					TreeItem parentItem = selection[0].getParentItem();
					final Task srcParentTask = (parentItem != null) ? (Task) parentItem
							.getData() : null;
					// Cr�ation du valideur
					taskChooserDialog.setValidator(new ITaskChooserValidator() {
						public void validateChoosenTask(Task selectedTask)
								throws DialogException {
							if (srcParentTask != null
									&& srcParentTask.equals(selectedTask))
								throw new DialogException(
										Strings.getString("TasksUI.errors.MOVE_TO_SAME_PARENT"), null); //$NON-NLS-1$
							try {
								ModelMgr.checkAcceptsSubtasks(selectedTask);
							} catch (ModelException e) {
								throw new DialogException(e.getMessage(), null);
							}
							// TODO Ajouter au ITaskChooserValidator la lev�e
							// d'exception techniques pour ne plus avoir ce
							// catch
							catch (DbException e) {
								throw new DialogException(
										Strings.getString(
												"TasksUI.errors.TECHNICAL_ERROR", e.getMessage()), null); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
					});
					// Affichage du popup
					Task newParentTask = null;
					if (taskChooserDialog.open() == Dialog.OK) {
						newParentTask = (Task) taskChooserDialog.getValue();
						log.debug("Selected parent task=" + newParentTask); //$NON-NLS-1$
						String oldTaskFullpath = taskToMove.getFullPath();
						ModelMgr.moveTask(taskToMove, newParentTask);
						// Rafraichir l'ancien et le nouveau parent ne suffit
						// pas
						// dans le cas ou le parent destination change de num�ro
						// (ex : d�placement d'une tache A vers une tache B avec
						// A et B initialement soeurs, A �tant plac� avant B)
						// treeViewer.refresh(newParentTask);
						// treeViewer.refresh(srcParentTask);
						treeViewer.refresh();
						// Notification des listeners
						notifyTaskMoved(oldTaskFullpath, taskToMove);
					}
				}
				// Cas d'une demande de d�placement vers la racine
				else if (moveToRootItem.equals(source)) {
					TreeItem selectedItem = selection[0];
					Task taskToMove = (Task) selectedItem.getData();
					String oldTaskFullpath = taskToMove.getFullPath();
					// D�placement
					ModelMgr.moveTask(taskToMove, null);
					treeViewer.refresh();
					// Notification des listeners
					notifyTaskMoved(oldTaskFullpath, taskToMove);
				}
				// Cas d'une demande de copie des taches s�lectionn�es (on met
				// dans le presse papier
				// le chemin de la tache)
				else if (copyItem.equals(source)) {
					// Impl�mentation en multi s�lection => pour l'instant on ne
					// veut g�rer qu'une seule tache � la fois
					// TreeItem[] selectedItems =
					// treeViewer.getTree().getSelection();
					// Clipboard clipboard = new Clipboard(parent.getDisplay());
					// String[] taskCodePaths = new
					// String[selectedItems.length];
					// Transfer[] transfers = new
					// Transfer[selectedItems.length];
					// for (int i=0; i<selectedItems.length; i++) {
					// Task task = (Task) selectedItems[i].getData();
					// taskCodePaths[i] = ModelMgr.getTaskCodePath(task);
					// transfers[i] = TextTransfer.getInstance();
					// }
					// clipboard.setContents(taskCodePaths, transfers);
					// clipboard.dispose();

					// Impl�mentation en s�lection simple
					if (selection != null && selection.length > 0) {
						TreeItem selectedItem = selection[0];
						Clipboard clipboard = new Clipboard(parent.getDisplay());
						Task task = (Task) selectedItem.getData();
						String taskCodePath = ModelMgr.getTaskCodePath(task);
						clipboard.setContents(new String[] { taskCodePath },
								new Transfer[] { TextTransfer.getInstance() });
						clipboard.dispose();
					}
				}
				// Cas d'une suppression
				else if (removeItem.equals(source)) {
					TreeItem selectedItem = selection[0];
					TreeItem parentItem = selectedItem.getParentItem();
					Task selectedTask = (Task) selectedItem.getData();
					Task parentTask = (parentItem != null) ? (Task) parentItem
							.getData() : null;
					// Suppression
					ModelMgr.removeTask(selectedTask);
					// Suppression dans l'arbre
					treeViewer.remove(selectedTask);
					// Mise � jour des sommes des taches parentes
					updateBranchItemsSums(parentItem);
					// Mise � jour des taches soeurs
					if (parentTask != null)
						treeViewer.refresh(parentTask);
					else
						treeViewer.refresh();
					// Notification des listeners
					notifyTaskRemoved(selectedTask);
				}
				// Cas d'une demande d'expansion d'un noeud
				else if (expandItem.equals(source)) {
					TreeItem selectedItem = treeViewer.getTree().getSelection()[0];
					treeViewer.expandToLevel(selectedItem.getData(),
							AbstractTreeViewer.ALL_LEVELS);
				}
				// Cas d'une demande de fermeture d'un noeud
				else if (collapseItem.equals(source)) {
					TreeItem selectedItem = treeViewer.getTree().getSelection()[0];
					treeViewer.collapseToLevel(selectedItem.getData(),
							AbstractTreeViewer.ALL_LEVELS);
				}
				// Cas d'une demande de liste des contributions
				else if (listTaskContributionsItem.equals(source)) {
					TreeItem selectedItem = selection[0];
					Task selectedTask = (Task) selectedItem.getData();
					contribsViewerDialog.setFilter(selectedTask, null, null,
							null, null);
					// Ouverture du dialogue
					contribsViewerDialog.open();
				}
				// Cas d'une demande de rafraichissement
				else if (refreshItem.equals(source)) {
					// R�cup�ration du noeud parent
					TreeItem selectedItem = selection[0];
					TreeItem parentItem = selectedItem.getParentItem();
					Task parentTask = (parentItem != null) ? (Task) parentItem
							.getData() : null;
					// Mise � jour
					if (parentTask != null)
						treeViewer.refresh(parentTask);
					else
						treeViewer.refresh();
				}
				// Cas d'une demande d'export du tableau
				else if (exportItem.equals(source)) {
					// Export du tableau
					SWTHelper.exportToWorkBook(treeViewer.getTree());
				}
				return null;
			}
		};
		// Ex�cution
		safeRunner.run(parent.getShell());
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

	/**
	 * Ajoute une tache.
	 * 
	 * @param parentTask
	 *            la tache parent ou null pour une tache racine.
	 * @throws DbException
	 *             lev� en cas d'incident associ� � la persistence.
	 * @throws ModelException
	 *             lev� en cas de violation du mod�le de donn�es.
	 * @return la tache cr��e.
	 */
	private Task newTask(Task parentTask) throws DbException, ModelException {
		log.debug("newTask(" + parentTask + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// Cr�ation de la nouvelle tache
		Task newTask = ModelMgr.createNewTask(parentTask);
		// Ajout dans l'arbre et cr�ation en base
		treeViewer.add(parentTask == null ? treeViewer.getInput() : parentTask,
				newTask);
		treeViewer.setSelection(new StructuredSelection(newTask), true);
		return newTask;
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
		TreeItem[] selection = treeViewer.getTree().getSelection();
		boolean emptySelection = selection.length == 0;
		boolean singleSelection = selection.length == 1;
		boolean rootSingleSelection = singleSelection && (selection[0] == null);
		newTaskItem.setEnabled(emptySelection || singleSelection);
		newSubtaskItem.setEnabled(singleSelection);
		moveUpItem.setEnabled(singleSelection);
		moveDownItem.setEnabled(singleSelection);
		moveBeforeAnotherTaskItem.setEnabled(singleSelection);
		moveAfterAnotherTaskItem.setEnabled(singleSelection);
		moveToAnotherTaskItem.setEnabled(singleSelection);
		moveToRootItem.setEnabled(singleSelection && !rootSingleSelection);
		copyItem.setEnabled(!emptySelection);
		removeItem.setEnabled(singleSelection);
		expandItem.setEnabled(singleSelection);
		collapseItem.setEnabled(singleSelection);
		listTaskContributionsItem.setEnabled(singleSelection);
		refreshItem.setEnabled(singleSelection);
		exportItem.setEnabled(true);
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
	 * jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener#databaseOpened()
	 */
	public void databaseOpened() {
		// Cr�ation d'une racine fictive
		treeViewer.setInput(ROOT_NODE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener#databaseClosed()
	 */
	public void databaseClosed() {
		Tree tree = treeViewer.getTree();
		TreeItem[] items = tree.getItems();
		for (int i = 0; i < items.length; i++) {
			items[i].dispose();
		}
		taskChooserDialog.databaseClosed();
	}

	/**
	 * Ajoute un listener.
	 * 
	 * @param listener
	 *            le nouveau listener.
	 */
	public void addTaskListener(ITaskListener listener) {
		listeners.add(listener);
	}

	/**
	 * Ajoute un listener.
	 * 
	 * @param listener
	 *            le nouveau listener.
	 */
	public void removeTaskListener(ITaskListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Notifie les listeners qu'une tache a �t� ajout�e.
	 * 
	 * @param newTask
	 *            la tache ajout�.
	 */
	private void notifyTaskAdded(Task newTask) {
		Iterator<ITaskListener> it = listeners.iterator();
		while (it.hasNext()) {
			ITaskListener listener = it.next();
			listener.taskAdded(newTask);
		}
		// Transfert de la notification au popup de choix de tache
		taskChooserDialog.taskAdded(newTask);
	}

	/**
	 * Notifie les listeners qu'une tache a �t� supprim�e.
	 * 
	 * @param task
	 *            la tache supprim�e.
	 */
	private void notifyTaskRemoved(Task task) {
		Iterator<ITaskListener> it = listeners.iterator();
		while (it.hasNext()) {
			ITaskListener listener = it.next();
			listener.taskRemoved(task);
		}
		// Transfert de la notification au popup de choix de tache
		taskChooserDialog.taskRemoved(task);
	}

	/**
	 * Notifie les listeners qu'une tache a �t� modifi�e.
	 * 
	 * @param task
	 *            la tache modifi�e.
	 */
	private void notifyTaskUpdated(Task task) {
		Iterator<ITaskListener> it = listeners.iterator();
		while (it.hasNext()) {
			ITaskListener listener = it.next();
			listener.taskUpdated(task);
		}
		// Transfert de la notification au popup de choix de tache
		taskChooserDialog.taskUpdated(task);
	}

	/**
	 * Notifie les listeners qu'une tache a �t� d�plac�e.
	 * 
	 * @param task
	 *            la tache modifi�e.
	 */
	private void notifyTaskMoved(String oldTaskPath, Task task) {
		Iterator<ITaskListener> it = listeners.iterator();
		while (it.hasNext()) {
			ITaskListener listener = it.next();
			listener.taskMoved(oldTaskPath, task);
		}
		// Transfert de la notification au popup de choix de tache
		taskChooserDialog.taskMoved(oldTaskPath, task);
	}

	/**
	 * Indique qu'une contribution a �t� ajout�e au r�f�rentiel.
	 * 
	 * @param contribution
	 *            la contribution ajout�e.
	 */
	public void contributionAdded(Contribution contribution) {
		needRefresh = true;
	}

	/**
	 * Indique que des contributions ont �t� supprim�es du r�f�rentiel.
	 * 
	 * @param contributions
	 *            les contributions supprim�es.
	 */
	public void contributionsRemoved(Contribution[] contributions) {
		needRefresh = true;
	}

	/**
	 * Indique que des contributions ont �t� modifi�e dans le r�f�rentiel.
	 * 
	 * @param contributions
	 *            les contributions modifi�es.
	 */
	public void contributionsUpdated(Contribution[] contributions) {
		needRefresh = true;
	}

}
