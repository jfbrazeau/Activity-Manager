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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskContributionsSums;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.ContributionsUI.IContributionListener;
import org.activitymgr.ui.rcp.DatabaseUI.IDbStatusListener;
import org.activitymgr.ui.rcp.dialogs.ContributionsViewerDialog;
import org.activitymgr.ui.rcp.dialogs.DialogException;
import org.activitymgr.ui.rcp.dialogs.ITaskChooserValidator;
import org.activitymgr.ui.rcp.dialogs.TaskChooserTreeWithHistoryDialog;
import org.activitymgr.ui.rcp.util.AbstractTableMgr;
import org.activitymgr.ui.rcp.util.ITaskSelectionListener;
import org.activitymgr.ui.rcp.util.SWTHelper;
import org.activitymgr.ui.rcp.util.SafeRunner;
import org.activitymgr.ui.rcp.util.TableOrTreeColumnsMgr;
import org.activitymgr.ui.rcp.util.TaskFinderPanel;
import org.activitymgr.ui.rcp.util.UITechException;
import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/**
 * IHM de gestion des tâches.
 */
public class TasksUI extends AbstractTableMgr implements IDbStatusListener,
		ICellModifier, SelectionListener, MenuListener, ITreeContentProvider,
		ITableColorProvider, IContributionListener {

	/** Logger */
	private static Logger log = Logger.getLogger(TasksUI.class);

	/** Constantes associées aux colonnes */
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
	 * Interface utilisée pour permettre l'écoute de la suppression ou de
	 * l'ajout de taches.
	 */
	public static interface ITaskListener {

		/**
		 * Indique qu'une tache a été ajoutée au référentiel.
		 * 
		 * @param task
		 *            la tache ajoutée.
		 */
		public void taskAdded(Task task);

		/**
		 * Indique qu'une tache a été supprimée du référentiel.
		 * 
		 * @param task
		 *            la tache supprimée.
		 */
		public void taskRemoved(Task task);

		/**
		 * Indique qu'une tache a été modifiée duans le référentiel.
		 * 
		 * @param task
		 *            la tache modifiée.
		 */
		public void taskUpdated(Task task);

		/**
		 * Indique qu'une tache a été déplacée duans le référentiel.
		 * 
		 * @param oldTaskFullpath
		 *            ancien chemin de la tache.
		 * @param task
		 *            la tache déplacée.
		 */
		public void taskMoved(String oldTaskFullpath, Task task);
	}

	/** Model manager */
	private IModelMgr modelMgr;
	
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
	private MenuItem xlsExportItem;
	private MenuItem xlsImportItem;
	private MenuItem xlsSnapshotExportItem;

	/** Composant parent */
	private Composite parent;

	/** Popup permettant de choisir une tache */
	private TaskChooserTreeWithHistoryDialog taskChooserDialog;

	/** Popup permettant de lister les contributions d'une tache */
	private ContributionsViewerDialog contribsViewerDialog;

	/** Panneau de recherche de tache */
	private TaskFinderPanel taskFinderPanel;

	/** Couleur de police de caractère utilisée pour les zones non modifiables */
	private Color disabledFGColor;

	/**
	 * Booléen permettant de savoir si un refresh doit être exécuté lors du
	 * prochain paint
	 */
	private boolean needRefresh = false;

	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 * 
	 * @param tabItem
	 *            item parent.
	 * @param modelMgr
	 *            the model manager instance.
	 */
	public TasksUI(TabItem tabItem, IModelMgr modelMgr) {
		this(tabItem.getParent(), modelMgr);
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par défaut.
	 * 
	 * @param parentComposite
	 *            composant parent.
	 */
	public TasksUI(Composite parentComposite, final IModelMgr modelMgr) {
		this.modelMgr = modelMgr;

		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));

		// Panneau permettant de recherche une tache
		taskFinderPanel = new TaskFinderPanel(parent, modelMgr);
		GridData gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
		taskFinderPanel.setLayoutData(gridData);
		taskFinderPanel.addTaskListener(new ITaskSelectionListener() {
			public void taskSelected(final Task selectedTask) {
				SafeRunner safeRunner = new SafeRunner() {
					@Override
					protected Object runUnsafe() throws Exception {
						TaskSums selectedElement = null;
						for (Object element : treeViewer.getExpandedElements()) {
							TaskSums sums = (TaskSums) element;
							if (sums.getTask().equals(selectedTask)) {
								selectedElement = sums;
								break;
							}
						}
						if (selectedElement == null) {
							selectedElement = modelMgr.getTaskSums(selectedTask.getId(), null, null);
						}
						return selectedElement;
					}
				};
				TaskSums selectedElement = (TaskSums) safeRunner.run(parent.getShell());
				if (selectedElement != null) {
					treeViewer.setSelection(new StructuredSelection(selectedElement));
					treeViewer.getTree().setFocus();
				}
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

		// Création du viewer
		treeViewer = new TreeViewer(tree);
		treeViewer.setCellModifier(this);
		treeViewer.setContentProvider(this);
		treeViewer.setLabelProvider(this);
		
	    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
	    int operations = DND.DROP_MOVE;
	    final DragSource source = new DragSource(tree, operations);
	    source.setTransfer(types);		
	    source.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				event.doit =  (selection.size() == 1);
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				TaskSums firstElement = (TaskSums) ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
				event.data = String.valueOf(firstElement.getTask().getId());
			}
		});
		DropTarget target = new DropTarget(tree, operations);
		target.setTransfer(types);
		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(final DropTargetEvent event) {
				new SafeRunner() {
					@Override
					protected Object runUnsafe() throws Exception {
						Task taskToMove = modelMgr.getTask(Long.parseLong((String)event.data));
						TreeItem item = (TreeItem)event.item;
						if (item == null) {
							// If we move the task to outside of the tree (at the bottom), simply
							// move the task under root
							doMoveToAnotherTask(taskToMove, null);
						}
						else {
							Task destTask = ((TaskSums) item.getData()).getTask();
							Point pt = tree.getDisplay().map(null, tree, event.x, event.y);
							Rectangle bounds = item.getBounds();
							if (pt.y < bounds.y + bounds.height/3) {
								doMoveBeforeOrAfter(taskToMove, destTask, true);
							} else if (pt.y > bounds.y + 2*bounds.height/3) {
								doMoveBeforeOrAfter(taskToMove, destTask, false);
							} else {
								treeViewer.expandToLevel(event.item.getData(), 1);
								doMoveToAnotherTask(taskToMove, destTask);
							}
						}
						return null;
					}
				}.run(parent.getShell());
			}
			@Override
			public void dragOver(DropTargetEvent event) {
				event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
				if (event.item != null) {
					TreeItem item = (TreeItem)event.item;
					Point pt = tree.getDisplay().map(null, tree, event.x, event.y);
					Rectangle bounds = item.getBounds();
					if (pt.y < bounds.y + bounds.height/3) {
						event.feedback |= DND.FEEDBACK_INSERT_BEFORE;
					} else if (pt.y > bounds.y + 2*bounds.height/3) {
						event.feedback |= DND.FEEDBACK_INSERT_AFTER;
					} else {
						event.feedback |= DND.FEEDBACK_SELECT;
					}
				}
			}
		});
		
		// Création des polices de caractère
		disabledFGColor = tree.getDisplay().getSystemColor(
				SWT.COLOR_TITLE_INACTIVE_FOREGROUND);

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

		// Configuration des éditeurs de cellules
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
				parent.getShell(), modelMgr);
		contribsViewerDialog = new ContributionsViewerDialog(parent.getShell(), modelMgr);

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
		// Sous-menu 'Déplacer'
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

		MenuItem exportItem = new MenuItem(menu, SWT.CASCADE);
		exportItem.setText(Strings.getString("TasksUI.menuitems.EXPORT_IMPORT")); //$NON-NLS-1$
		Menu exportMenu = new Menu(exportItem);
		exportItem.setMenu(exportMenu);

		xlsExportItem = new MenuItem(exportMenu, SWT.CASCADE);
		xlsExportItem.setText(Strings.getString("TasksUI.menuitems.XLS_EXPORT")); //$NON-NLS-1$
		xlsExportItem.addSelectionListener(this);

		xlsImportItem = new MenuItem(exportMenu, SWT.CASCADE);
		xlsImportItem.setText(Strings.getString("TasksUI.menuitems.XLS_IMPORT")); //$NON-NLS-1$
		xlsImportItem.addSelectionListener(this);

		xlsSnapshotExportItem = new MenuItem(exportMenu, SWT.CASCADE);
		xlsSnapshotExportItem.setText(Strings.getString("TasksUI.menuitems.XLS_SNAPSHOT_EXPORT")); //$NON-NLS-1$
		xlsSnapshotExportItem.addSelectionListener(this);
		tree.setMenu(menu);

		log.debug("UI initialization done"); //$NON-NLS-1$
		// Ajout de KeyListeners pour faciliter le déplacement vers le bas et
		// vers le haut des taches
		// (Rq: les accélérateurs sont ignorés dans les menus contextuels)
		KeyListener keyListener = new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				Widget simulatedWidget = null;
				if (e.stateMask == SWT.CTRL /* || e.stateMask == SWT.COMMAND */) {
					if (e.keyCode == SWT.ARROW_UP)
						simulatedWidget = moveUpItem;
					else if (e.keyCode == SWT.ARROW_DOWN)
						simulatedWidget = moveDownItem;
					else if (e.keyCode == 'c')
						simulatedWidget = copyItem;
				}
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

		// Ajout d'un listener permettant de détecter lorsque le
		// composant est affiché (passage d'un onglet à l'autre)
		parent.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent paintevent) {
				if (needRefresh) {
					needRefresh = false;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object,
	 * java.lang.String)
	 */
	public boolean canModify(Object element, String property) {
		log.debug("ICellModifier.canModify(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final TaskSums taskSums = (TaskSums) element;
		final int propertyIdx = treeColsMgr.getColumnIndex(property);
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				boolean canModify = false;
				switch (propertyIdx) {
				case (NAME_COLUMN_IDX):
				case (CODE_COLUMN_IDX):
					canModify = true;
					break;
				case (INITIAL_FUND_COLUMN_IDX):
				case (INITIALLY_CONSUMED_COLUMN_IDX):
				case (TODO_COLUMN_IDX):
					canModify = taskSums.isLeaf();
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
		// Retour du résultat
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
		final TaskSums taskSums = (TaskSums) element;
		final int propertyIdx = treeColsMgr.getColumnIndex(property);
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				String value = null;
				switch (propertyIdx) {
				case (NAME_COLUMN_IDX):
					value = taskSums.getTask().getName();
					break;
				case (CODE_COLUMN_IDX):
					value = taskSums.getTask().getCode();
					break;
				case (INITIAL_FUND_COLUMN_IDX):
					value = StringHelper.hundredthToEntry(taskSums.getBudgetSum());
					break;
				case (INITIALLY_CONSUMED_COLUMN_IDX):
					value = StringHelper.hundredthToEntry(taskSums.getInitiallyConsumedSum());
					break;
				case (CONSUMED_COLUMN_IDX):
					value = StringHelper.hundredthToEntry(taskSums
							.getContributionsSums().getConsumedSum()
							+ taskSums.getInitiallyConsumedSum());
					break;
				case (TODO_COLUMN_IDX):
					value = StringHelper.hundredthToEntry(taskSums.getTodoSum());
					break;
				case (DELTA_COLUMN_IDX):
					long delta = taskSums.getBudgetSum()
							- taskSums.getInitiallyConsumedSum()
							- taskSums.getContributionsSums().getConsumedSum() - taskSums.getTodoSum();
					value = StringHelper.hundredthToEntry(delta);
					break;
				case (COMMENT_COLUMN_IDX):
					value = taskSums.getTask().getComment() != null ? taskSums.getTask().getComment() : ""; //$NON-NLS-1$
					break;
				default:
					throw new UITechException(
							Strings.getString("TasksUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				// Retour du résultat
				return value;
			}
		};
		// Exécution
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
		final TaskSums taskSums = (TaskSums) item.getData();
		final Task task = taskSums.getTask();
		final int columnIdx = treeColsMgr.getColumnIndex(property);
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				boolean parentsMustBeRefreshed = false;
				switch (columnIdx) {
				case (NAME_COLUMN_IDX):
					task.setName((String) value);
					break;
				case (CODE_COLUMN_IDX):
					task.setCode(((String) value).trim());
					break;
				case (INITIAL_FUND_COLUMN_IDX):
					long newInitialFund = StringHelper
							.entryToHundredth((String) value);
					task.setBudget(newInitialFund);
					taskSums.setBudgetSum(newInitialFund);
					parentsMustBeRefreshed = true;
					break;
				case (INITIALLY_CONSUMED_COLUMN_IDX):
					long newInitiallyConsumed = StringHelper
							.entryToHundredth((String) value);
					task.setInitiallyConsumed(newInitiallyConsumed);
					taskSums.setInitiallyConsumedSum(newInitiallyConsumed);
					parentsMustBeRefreshed = true;
					break;
				case (TODO_COLUMN_IDX):
					long newTodo = StringHelper
							.entryToHundredth((String) value);
					task.setTodo(newTodo);
					taskSums.setTodoSum(newTodo);
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
				// Mise à jour en base
				modelMgr.updateTask(task);
				// Mise à jour des labels
				if (parentsMustBeRefreshed) {
					// Mise à jour des sommes des taches parentes
					updateParentSums(item);
				} else {
					// Notification de la mise à jour uniquement pour la tache
					notifyLabelProviderListener(taskSums);
				}
				// Notification de la mise à jour de la tache pour les listeners
				notifyTaskUpdated(task);
				return null;
			}
		};
		// Exécution
		safeRunner.run(parent.getShell());
	}

	protected void notifyLabelProviderListener(TaskSums... sums) {
		super.notifyLabelProviderListener(new LabelProviderChangedEvent(
				this, sums));
	}
	
	/**
	 * Met à jour les sommes associés aux taches de la branche associée à l'item
	 * spécifié.
	 * 
	 * @param item
	 *            l'item de tableau.
	 * @throws ModelException
	 *             levée en cas d'invalidité associée au modèle.
	 */
	private void updateParentSums(TreeItem item) throws ModelException {
		List<TaskSums> list = new ArrayList<TaskSums>();
		// Nettoyage du cache
		TreeItem cursor = item;
		while (cursor != null) {
			TaskSums taskCursor = (TaskSums) cursor.getData();
			TaskSums newTaskSums = modelMgr.getTaskSums(taskCursor.getTask().getId(), null, null);
			cursor.setData(newTaskSums);
			log.debug("Update task " + newTaskSums.getTask().getName()); //$NON-NLS-1$
			list.add(0, newTaskSums);
			cursor = cursor.getParentItem();
		}
		// Notification de la mise à jour (ce qui recharge automatiquement
		// le cache des sommes de taches)
		notifyLabelProviderListener((TaskSums[]) list.toArray(new TaskSums[list.size()]));
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
		TaskSums sums = (TaskSums) element;
		return !sums.isLeaf();
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
		final TaskSums parentTaskSums = (TaskSums) parentElement;
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				List<TaskSums> subTasks = modelMgr.getSubTasksSums(parentTaskSums != null ? parentTaskSums.getTask() : null, null, null);
				return subTasks.toArray();
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
		final TaskSums sums = (TaskSums) element;
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				Task parentTask = modelMgr.getParentTask(sums.getTask());
				return parentTask == null ? treeViewer.getInput() : modelMgr.getTaskSums(parentTask.getId(), null, null);
			}
		};
		// Exécution du traitement
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
		String value = (String) getValue(element,
				treeColsMgr.getColumnCode(columnIndex));
		log.debug("  =>" + value + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITableColorProvider#getBackground(java.lang
	 * .Object, int)
	 */
	public Color getBackground(Object element, int columnIndex) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITableColorProvider#getForeground(java.lang
	 * .Object, int)
	 */
	public Color getForeground(Object element, int columnIndex) {
		return canModify(element, treeColsMgr.getColumnCode(columnIndex)) ? null : disabledFGColor;
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
				// Cas d'une création (même niveau)
				if (newTaskItem.equals(source)) {
					// Récupération du noeud parent
					TreeItem parentItem = selection.length > 0 ? selection[0]
							.getParentItem() : null;
					TaskSums parentSums = parentItem == null ? null
							: (TaskSums) parentItem.getData();
					// Création de la tache
					Task newTask = newTask(parentSums);
					// Notification des listeners
					notifyTaskAdded(newTask);
				}
				// Cas d'une création de sous tache
				else if (newSubtaskItem.equals(source)) {
					TaskSums selected = (TaskSums) selection[0].getData();
					Task newTask = newTask(selected);
					// Then remember that the parent task is not a leaf task
					selected.setLeaf(false);
					// Notification des listeners
					notifyTaskAdded(newTask);
				}
				// Cas d'une demande de déplacement vers le haut
				else if (moveUpItem.equals(source)) {
					TaskSums selected = (TaskSums) selection[0].getData();
					Task selectedTask = selected.getTask();
					String oldTaskFullpath = selectedTask.getFullPath();
					modelMgr.moveUpTask(selectedTask);
					// Mise à jour de l'IHM
					treeViewer.refresh(selection[0].getParent().getData(),
							false);
					// Notification des listeners
					notifyTaskMoved(oldTaskFullpath, selectedTask);
				}
				// Cas d'une demande de déplacement vers le haut
				else if (moveDownItem.equals(source)) {
					TaskSums selected = (TaskSums) selection[0].getData();
					Task selectedTask = selected.getTask();
					String oldTaskFullpath = selectedTask.getFullPath();
					modelMgr.moveDownTask(selectedTask);
					// Mise à jour de l'IHM
					treeViewer.refresh(selection[0].getParent().getData(),
							false);
					// Notification des listeners
					notifyTaskMoved(oldTaskFullpath, selectedTask);
				}
				// Cas d'une demande de déplacement avant ou après une autre
				// tache
				else if (moveBeforeAnotherTaskItem.equals(source)
						|| moveAfterAnotherTaskItem.equals(source)) {
					TaskSums selected = (TaskSums) selection[0].getData();
					Task selectedTask = selected.getTask();
					final Task finalSelectedTask = selectedTask;
					// Création du valideur
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
						boolean before = moveBeforeAnotherTaskItem.equals(source);
						Task chosenTask = (Task) taskChooserDialog.getValue();
						doMoveBeforeOrAfter(selectedTask, chosenTask, before);
					}
				}
				// Cas d'une demande de déplacement vers une autre tache
				else if (moveToAnotherTaskItem.equals(source)) {
					TaskSums selected = (TaskSums) selection[0].getData();
					Task taskToMove = selected.getTask();
					// Récupération du noeud parent
					TreeItem parentItem = selection[0].getParentItem();
					final Task srcParentTask = (parentItem != null) ? ((TaskSums) parentItem
							.getData()).getTask() : null;
					// Création du valideur
					taskChooserDialog.setValidator(new ITaskChooserValidator() {
						public void validateChoosenTask(Task selectedTask)
								throws DialogException {
							if (srcParentTask != null
									&& srcParentTask.equals(selectedTask))
								throw new DialogException(
										Strings.getString("TasksUI.errors.MOVE_TO_SAME_PARENT"), null); //$NON-NLS-1$
							try {
								modelMgr.checkAcceptsSubtasks(selectedTask);
							} catch (ModelException e) {
								throw new DialogException(e.getMessage(), null);
							}
						}
					});
					// Affichage du popup
					if (taskChooserDialog.open() == Dialog.OK) {
						Task newParentTask = (Task) taskChooserDialog.getValue();
						doMoveToAnotherTask(taskToMove, newParentTask);
					}
				}
				// Cas d'une demande de déplacement vers la racine
				else if (moveToRootItem.equals(source)) {
					TaskSums selected = (TaskSums) selection[0].getData();
					Task taskToMove = selected.getTask();
					String oldTaskFullpath = taskToMove.getFullPath();
					// Déplacement
					modelMgr.moveTask(taskToMove, null);
					treeViewer.refresh();
					// Notification des listeners
					notifyTaskMoved(oldTaskFullpath, taskToMove);
				}
				// Cas d'une demande de copie des taches sélectionnées (on met
				// dans le presse papier
				// le chemin de la tache)
				else if (copyItem.equals(source)) {
					// Implémentation en multi sélection => pour l'instant on ne
					// veut gérer qu'une seule tache à la fois
					// TreeItem[] selectedItems =
					// treeViewer.getTree().getSelection();
					// Clipboard clipboard = new Clipboard(parent.getDisplay());
					// String[] taskCodePaths = new
					// String[selectedItems.length];
					// Transfer[] transfers = new
					// Transfer[selectedItems.length];
					// for (int i=0; i<selectedItems.length; i++) {
					// Task task = (Task) selectedItems[i].getData();
					// taskCodePaths[i] = modelMgr.getTaskCodePath(task);
					// transfers[i] = TextTransfer.getInstance();
					// }
					// clipboard.setContents(taskCodePaths, transfers);
					// clipboard.dispose();

					// Implémentation en sélection simple
					if (selection != null && selection.length > 0) {
						TreeItem selectedItem = selection[0];
						Clipboard clipboard = new Clipboard(parent.getDisplay());
						TaskSums selected = (TaskSums) selectedItem.getData();
						String taskCodePath = modelMgr.getTaskCodePath(selected.getTask());
						clipboard.setContents(new String[] { taskCodePath },
								new Transfer[] { TextTransfer.getInstance() });
						clipboard.dispose();
					}
				}
				// Cas d'une suppression
				else if (removeItem.equals(source)) {
					TreeItem selectedItem = selection[0];
					TreeItem parentItem = selectedItem.getParentItem();
					TaskSums selected = (TaskSums) selectedItem.getData();
					Task selectedTask = selected.getTask();
					Task parentTask = (parentItem != null) ? ((TaskSums) parentItem
							.getData()).getTask() : null;
					// Suppression
					modelMgr.removeTask(selectedTask);
					// Suppression dans l'arbre
					treeViewer.remove(selected);
					updateParentSums(parentItem);
					// Mise à jour des taches soeurs
					if (parentTask != null)
						treeViewer.refresh(parentItem.getData());
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
					TaskSums selected = (TaskSums) selection[0].getData();
					Task selectedTask = selected.getTask();
					contribsViewerDialog.setFilter(selectedTask, null);
					// Ouverture du dialogue
					contribsViewerDialog.open();
				}
				// Cas d'une demande de rafraichissement
				else if (refreshItem.equals(source)) {
					// Récupération du noeud parent
					TreeItem selectedItem = selection[0];
					TreeItem parentItem = selectedItem.getParentItem();
					TaskSums parentTaskSums = (parentItem != null) ? (TaskSums) parentItem
							.getData() : null;
					// Mise à jour
					if (parentTaskSums != null)
						treeViewer.refresh(parentTaskSums);
					else
						treeViewer.refresh();
				}
				// Cas d'une demande d'export du tableau
				else if (xlsExportItem.equals(source)) {
					Long parentTaskId = null;
					if (selection.length > 0) {
						TaskSums selected = (TaskSums) selection[0].getData();
						parentTaskId = selected.getTask().getId();
					}
					FileDialog fd = new FileDialog(parent.getShell(), SWT.APPLICATION_MODAL | SWT.SAVE);
					fd.setFilterExtensions(new String[] { "*.xls", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
					fd.setOverwrite(true);
					String fileName = fd.open();
					// Si le nom est spécifié
					if (fileName != null) {
						try {
							// Correction du nom du fichier si besoin
							if (!fileName.endsWith(".xls")) //$NON-NLS-1$
								fileName += ".xls"; //$NON-NLS-1$
							// Sauvegarde du document
							byte[] excel = modelMgr.exportToExcel(parentTaskId);
							FileOutputStream out = new FileOutputStream(fileName);
							out.write(excel);
							out.close();
						} catch (IOException e) {
							log.error("I/O exception", e); //$NON-NLS-1$
							throw new UITechException(
									Strings.getString("SWTHelper.errors.IO_EXCEPTION_WHILE_EXPORTING"), e); //$NON-NLS-1$
						}
					}
				}
				else if (xlsImportItem.equals(source)) {
					Long parentTaskId = null;
					TaskSums selected = null;
					if (selection.length > 0) {
						selected = (TaskSums) selection[0].getData();
						parentTaskId = selected.getTask().getId();
						// Expand the tree
						treeViewer.expandToLevel(selected, 1);
					}
					FileDialog fd = new FileDialog(parent.getShell(), SWT.APPLICATION_MODAL | SWT.OPEN);
					fd.setFilterExtensions(new String[] { "*.xls", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
					String fileName = fd.open();
					// Si le nom est spécifié
					if (fileName != null) {
						try {
							FileInputStream in = new FileInputStream(fileName);
							modelMgr.importFromExcel(parentTaskId, in);
							in.close();
							// Refresh the tree
							treeViewer.refresh();
						} catch (IOException e) {
							log.error("I/O exception", e); //$NON-NLS-1$
							throw new UITechException(
									Strings.getString("SWTHelper.errors.IO_EXCEPTION_WHILE_EXPORTING"), e); //$NON-NLS-1$
						}
					}
				}
				else if (xlsSnapshotExportItem.equals(source)) {
					// Export du tableau
					SWTHelper.exportToWorkBook(treeViewer.getTree());
				}
				return null;
			}


		};
		// Exécution
		safeRunner.run(parent.getShell());
	}

	/**
	 * Move to another task.
	 * @param taskToMove the task to move.
	 * @param newParentTask the new parent task.
	 * @throws ModelException thrown if a model error is detected.
	 */
	private void doMoveToAnotherTask(Task taskToMove, Task newParentTask)
			throws ModelException {
		log.debug("Selected parent task=" + newParentTask); //$NON-NLS-1$
		modelMgr.moveTask(taskToMove, newParentTask);
		// Rafraichir l'ancien et le nouveau parent ne suffit
		// pas
		// dans le cas ou le parent destination change de numéro
		// (ex : déplacement d'une tache A vers une tache B avec
		// A et B initialement soeurs, A étant placé avant B)
		// treeViewer.refresh(newParentTask);
		// treeViewer.refresh(srcParentTask);
		treeViewer.refresh();
		// Notification des listeners
		String oldTaskFullpath = taskToMove.getFullPath();
		notifyTaskMoved(oldTaskFullpath, taskToMove);
	}

	/**
	 * Moves a task before or after another task.
	 * @param taskToMove the task to move.
	 * @param chosenTask the chosen task.
	 * @param before <code>true</code> if the task must be moved before the chosen tasks, <code>false</code> if it must be moved after.
	 * @throws ModelException if a model error occurs.
	 */
	private void doMoveBeforeOrAfter(Task taskToMove, Task chosenTask,
			boolean before) throws ModelException {
		boolean needRefresh = false;
		// Traitement du changement éventuel de parent
		if (!chosenTask.getPath()
				.equals(taskToMove.getPath())) {
			Task destParentTask = modelMgr.getParentTask(chosenTask);
			// Déplacement
			modelMgr.moveTask(taskToMove, destParentTask);
			// Rafraichissement de la tache
			taskToMove = modelMgr.getTask(taskToMove
					.getId());
			needRefresh = true;
		}
		// Déplacement de la tache
		int targetNumber = chosenTask.getNumber();
		if (before
				&& targetNumber > taskToMove.getNumber())
			targetNumber--;
		else if (!before
				&& targetNumber < taskToMove.getNumber())
			targetNumber++;
		if (targetNumber != taskToMove.getNumber()) {
			modelMgr.moveTaskUpOrDown(taskToMove, targetNumber);
			needRefresh = true;
		}
		if (needRefresh) {
			// Notification des listeners
			String oldTaskFullpath = taskToMove.getFullPath();
			notifyTaskMoved(oldTaskFullpath, taskToMove);
			// Mise à jour de l'IHM
			treeViewer.refresh();
		}
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
	 * @param parentSums
	 *            les totaux de la tache parente ou null pour une tache racine.
	 * @throws ModelException
	 *             levé en cas de violation du modèle de données.
	 * @return la tache créée.
	 */
	private Task newTask(TaskSums parentSums) throws ModelException {
		Task parentTask = parentSums != null ? parentSums.getTask() : null;
		log.debug("newTask(" + parentTask + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// Création de la nouvelle tache
		Task newTask = modelMgr.createNewTask(parentTask);
		// Ajout dans l'arbre et création en base
		TaskSums newSums = new TaskSums();
		newSums.setLeaf(true);
		newSums.setTask(newTask);
		newSums.setContributionsSums(new TaskContributionsSums());
		treeViewer.add(parentSums == null ? treeViewer.getInput() : parentSums,
				newSums);
		treeViewer.setSelection(new StructuredSelection(newSums), true);
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
		xlsSnapshotExportItem.setEnabled(true);
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
		// Création d'une racine fictive
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
	 * Notifie les listeners qu'une tache a été ajoutée.
	 * 
	 * @param newTask
	 *            la tache ajouté.
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
	 * Notifie les listeners qu'une tache a été supprimée.
	 * 
	 * @param task
	 *            la tache supprimée.
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
	 * Notifie les listeners qu'une tache a été modifiée.
	 * 
	 * @param task
	 *            la tache modifiée.
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
	 * Notifie les listeners qu'une tache a été déplacée.
	 * 
	 * @param task
	 *            la tache modifiée.
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
	 * Indique qu'une contribution a été ajoutée au référentiel.
	 * 
	 * @param contribution
	 *            la contribution ajoutée.
	 */
	public void contributionAdded(Contribution contribution) {
		needRefresh = true;
	}

	/**
	 * Indique que des contributions ont été supprimées du référentiel.
	 * 
	 * @param contributions
	 *            les contributions supprimées.
	 */
	public void contributionsRemoved(Contribution[] contributions) {
		needRefresh = true;
	}

	/**
	 * Indique que des contributions ont été modifiée dans le référentiel.
	 * 
	 * @param contributions
	 *            les contributions modifiées.
	 */
	public void contributionsUpdated(Contribution[] contributions) {
		needRefresh = true;
	}

}
