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

import java.util.ArrayList;
import java.util.HashMap;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tools.activitymgr.core.beans.TaskSums;
import jfb.tools.activitymgr.core.util.StringHelper;
import jfb.tools.activitymgr.ui.dialogs.ContributionsViewerDialog;
import jfb.tools.activitymgr.ui.dialogs.DialogException;
import jfb.tools.activitymgr.ui.dialogs.ITaskChooserValidator;
import jfb.tools.activitymgr.ui.dialogs.TasksChooserDialog;
import jfb.tools.activitymgr.ui.util.SWTHelper;
import jfb.tools.activitymgr.ui.util.SafeRunner;
import jfb.tools.activitymgr.ui.util.TableMgrBase;
import jfb.tools.activitymgr.ui.util.TableOrTreeColumnsMgr;
import jfb.tools.activitymgr.ui.util.UITechException;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * IHM de gestion des tâches.
 */
public class TasksUI extends TableMgrBase implements ICellModifier, SelectionListener, MenuListener, ITreeContentProvider {

	/** Logger */
	private static Logger log = Logger.getLogger(TasksUI.class);

	/** Constantes associées aux colonnes */
	public static final int NAME_COLUMN_IDX = 0;
	public static final int CODE_COLUMN_IDX = 1;
	public static final int INITIAL_FUND_COLUMN_IDX = 2;
	public static final int INITIALLY_CONSUMED_COLUMN_IDX = 3;
	public static final int TODO_COLUMN_IDX = 4;
	public static final int CONSUMED_COLUMN_IDX = 5;
	public static final int DELTA_COLUMN_IDX = 6;
	private static TableOrTreeColumnsMgr treeColsMgr;
	
	/** Viewer */
	private TreeViewer treeViewer;

	/** Items de menu */
	private MenuItem newTaskItem;
	private MenuItem newSubtaskItem;
	private MenuItem moveUpItem;
	private MenuItem moveDownItem;
	private MenuItem moveToAnotherTaskItem;
	private MenuItem moveToRootItem;
	private MenuItem listTaskContributionsItem;
	private MenuItem removeItem;
	private MenuItem refreshItem;
	private MenuItem expandItem;
	private MenuItem collapseItem;
	private MenuItem exportItem;
	
	/** Composant parent */
	private Composite parent;

	/** Table contenant les sommes associées aux taches */
	private HashMap tasksSums = new HashMap();
	
	/** Popup permettant de choisir une tache */
	private TasksChooserDialog taskChooserDialog;

	/** Popup permettant de lister les contributions d'une tache */
	private ContributionsViewerDialog contribsViewerDialog;
	
	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 * @param tabItem item parent.
	 */
	public TasksUI(TabItem tabItem) {
		this(tabItem.getParent());
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par défaut.
	 * @param parentComposite composant parent.
	 */
	public TasksUI(Composite parentComposite) {
		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));

		// Arbre tableau
		final Tree tree = new Tree(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.HIDE_SELECTION);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
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

		// Configuration des colonnes
		treeColsMgr = new TableOrTreeColumnsMgr();
		treeColsMgr.addColumn("NAME", "Task name", 200, SWT.LEFT);
		treeColsMgr.addColumn("CODE", "Task code", 70, SWT.LEFT);
		treeColsMgr.addColumn("BUDGET", "Budget", 70, SWT.RIGHT);
		treeColsMgr.addColumn("INI_CONS", "Initially consumed", 70, SWT.RIGHT);
		treeColsMgr.addColumn("TODO", "Todo", 70, SWT.RIGHT);
		treeColsMgr.addColumn("CONSUMED", "Consumed", 70, SWT.RIGHT);
		treeColsMgr.addColumn("DELTA", "Delta", 70, SWT.RIGHT);
		treeColsMgr.configureTree(treeViewer);

		// Configuration des éditeurs de cellules
		CellEditor[] editors = new CellEditor[7];
		editors[NAME_COLUMN_IDX] = new TextCellEditor(tree);
		editors[CODE_COLUMN_IDX] = new TextCellEditor(tree);
		editors[INITIAL_FUND_COLUMN_IDX] = new TextCellEditor(tree);
		editors[INITIALLY_CONSUMED_COLUMN_IDX] = new TextCellEditor(tree);
		editors[TODO_COLUMN_IDX] = new TextCellEditor(tree);
		editors[CONSUMED_COLUMN_IDX] = null;
		editors[DELTA_COLUMN_IDX] = null;
		treeViewer.setCellEditors(editors);
		
		// Initialisation des popups
		taskChooserDialog = new TasksChooserDialog(parent.getShell());
		contribsViewerDialog = new ContributionsViewerDialog(parent.getShell());
		
		// Configuration du menu popup
		final Menu menu = new Menu(tree);
		menu.addMenuListener(this);
		// Sous-menu 'Nouveau'
		MenuItem newItem = new MenuItem(menu, SWT.CASCADE);
		newItem.setText("New");
		Menu newMenu = new Menu(newItem);
		newItem.setMenu(newMenu);
		newTaskItem = new MenuItem(newMenu, SWT.CASCADE);
		newTaskItem.setText("task (same level)");
		newTaskItem.addSelectionListener(this);
		newSubtaskItem = new MenuItem(newMenu, SWT.CASCADE);
		newSubtaskItem.setText("subtask");
		newSubtaskItem.addSelectionListener(this);
		// Sous-menu 'Déplacer'
		MenuItem moveToItem = new MenuItem(menu, SWT.CASCADE);
		moveToItem.setText("Move");
		Menu moveToMenu = new Menu(moveToItem);
		moveToItem.setMenu(moveToMenu);
		moveUpItem = new MenuItem(moveToMenu, SWT.CASCADE);
		moveUpItem.setText("up");
		moveUpItem.addSelectionListener(this);
		moveDownItem = new MenuItem(moveToMenu, SWT.CASCADE);
		moveDownItem.setText("down");
		moveDownItem.addSelectionListener(this);
		moveToAnotherTaskItem = new MenuItem(moveToMenu, SWT.CASCADE);
		moveToAnotherTaskItem.setText("under another task");
		moveToAnotherTaskItem.addSelectionListener(this);
		moveToRootItem = new MenuItem(moveToMenu, SWT.CASCADE);
		moveToRootItem.setText("under root");
		moveToRootItem.addSelectionListener(this);
		listTaskContributionsItem = new MenuItem(menu, SWT.CASCADE);
		listTaskContributionsItem.setText("List contrib.");
		listTaskContributionsItem.addSelectionListener(this);
		removeItem = new MenuItem(menu, SWT.CASCADE);
		removeItem.setText("Remove");
		removeItem.addSelectionListener(this);
		refreshItem = new MenuItem(menu, SWT.CASCADE);
		refreshItem.setText("Refresh");
		refreshItem.addSelectionListener(this);
		expandItem = new MenuItem(menu, SWT.CASCADE);
		expandItem.setText("Expand all");
		expandItem.addSelectionListener(this);
		collapseItem = new MenuItem(menu, SWT.CASCADE);
		collapseItem.setText("Collapse all");
		collapseItem.addSelectionListener(this);
		exportItem = new MenuItem(menu, SWT.CASCADE);
		exportItem.setText("Export");
		exportItem.addSelectionListener(this);
		tree.setMenu(menu);

		log.debug("UI initialization done");
		// TODO Implémenter le KeyLitener
//		tree.addKeyListener(new KeyListener() {
//			public void keyPressed(KeyEvent e) {
//			}
//			public void keyReleased(KeyEvent e) {
//				if ((e.keyCode==SWT.ARROW_DOWN) && (e.stateMask==SWT.CTRL))
//					JOptionPane.showMessageDialog(null, "Down!");
//			}
//		});
	}

	/**
	 * Initialise l'IHM avec les données en base.
	 */
	public void initUI() {
		// Création d'une racine fictive
		treeViewer.setInput(ROOT_NODE);
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(null);
	}
	
	/**
	 * Retourne les sommes associées à la tache spécifiée.
	 * @param task la tache pour laquelle on désire connaître les cumuls.
	 * @return les sommes associées à la tache.
	 * @throws ModelException levé en cas de détection d'incohérence au niveau
	 *     du modèle. 
	 * @throws DbException levé en cas d'incident technique d'accès à la base
	 *     de données.
	 */
	private TaskSums getTasksSums(Task task) throws ModelException, DbException {
		// 1° lecture dans le cache
		TaskSums taskSums = (TaskSums) tasksSums.get(task);
		if (taskSums==null) {
			synchronized (tasksSums) {
				// 2° lecture dans le cache (synchronisée)
				taskSums = (TaskSums) tasksSums.get(task);
				if (taskSums==null) {
					taskSums = ModelMgr.getTaskSums(task);
					// Dépot dans le cache
					tasksSums.put(task, taskSums);
				}				
			}
		}
		return taskSums;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
	 */
	public boolean canModify(Object element, String property) {
		log.debug("ICellModifier.canModify(" + element + ", " + property + ")");
		final Task task = (Task) element;
		final int propertyIdx = treeColsMgr.getColumnIndex(property);
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				boolean hasChilds = (task.getSubTasksCount()>0);
				boolean canModify = false;
				switch (propertyIdx) {
					case (NAME_COLUMN_IDX) :
					case (CODE_COLUMN_IDX) :
						canModify = true;
						break;
					case (INITIAL_FUND_COLUMN_IDX) :
					case (INITIALLY_CONSUMED_COLUMN_IDX) :
					case (TODO_COLUMN_IDX) :
						canModify = !hasChilds;
						break;
					case (CONSUMED_COLUMN_IDX) :
					case (DELTA_COLUMN_IDX) :
						canModify = false;
						break;
					default : 
						throw new UITechException("Colonne inconnue");
				}
				return canModify ? Boolean.TRUE : Boolean.FALSE;
			}
		};
		// Retour du résultat
		return ((Boolean)safeRunner.run(parent.getShell())).booleanValue();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
	 */
	public Object getValue(Object element, String property) {
		log.debug("ICellModifier.getValue(" + element + ", " + property + ")");
		final Task task = (Task) element;
		final int propertyIdx = treeColsMgr.getColumnIndex(property);
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				TaskSums taskSums = getTasksSums(task);
				boolean hasChilds = (task.getSubTasksCount()>0);
				String value = null;
				switch (propertyIdx) {
					case (NAME_COLUMN_IDX) :
						value = task.getName();
						break;
					case (CODE_COLUMN_IDX) :
						value = task.getCode();
						break;
					case (INITIAL_FUND_COLUMN_IDX) :
						value = StringHelper.hundredthToEntry(!hasChilds ? task.getBudget() : taskSums.getBudgetSum());
						break;
					case (INITIALLY_CONSUMED_COLUMN_IDX) :
						value = StringHelper.hundredthToEntry(!hasChilds ? task.getInitiallyConsumed() : taskSums.getInitiallyConsumedSum());
						break;
					case (TODO_COLUMN_IDX) :
						value = StringHelper.hundredthToEntry(!hasChilds ? task.getTodo() : taskSums.getTodoSum());
						break;
					case (CONSUMED_COLUMN_IDX) :
						value = StringHelper.hundredthToEntry(taskSums.getConsumedSum()+taskSums.getInitiallyConsumedSum());
						break;
					case (DELTA_COLUMN_IDX) :
						long delta = taskSums.getBudgetSum()
							- taskSums.getInitiallyConsumedSum()
							- taskSums.getConsumedSum()
							- taskSums.getTodoSum();
						value = StringHelper.hundredthToEntry(delta);
						break;
					default : 
						throw new UITechException("Colonne inconnue");
				}
				// Retour du résultat
				return value;
			}
		};
		// Exécution
		return safeRunner.run(parent.getShell());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	public void modify(Object element, String property, final Object value) {
		log.debug("ICellModifier.modify(" + element + ", " + property + ", " + value + ")");
		final TreeItem item = (TreeItem) element;
		final Task task = (Task) item.getData();
		final int columnIdx = treeColsMgr.getColumnIndex(property);
		final IBaseLabelProvider labelProvider = this;
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				boolean parentsMustBeRefreshed = false;
				switch (columnIdx) {
					case (NAME_COLUMN_IDX) :
						task.setName((String)value);
						break;
					case (CODE_COLUMN_IDX) :
						task.setCode((String)value);
						break;
					case (INITIAL_FUND_COLUMN_IDX) :
						long newInitialFund = StringHelper.entryToHundredth((String)value);
						task.setBudget(newInitialFund);
						parentsMustBeRefreshed = true;
						break;
					case (INITIALLY_CONSUMED_COLUMN_IDX) :
						long newInitiallyConsumed = StringHelper.entryToHundredth((String)value);
						task.setInitiallyConsumed(newInitiallyConsumed);
						parentsMustBeRefreshed = true;
						break;
					case (TODO_COLUMN_IDX) :
						long newTodo = StringHelper.entryToHundredth((String)value);
						task.setTodo(newTodo);
						parentsMustBeRefreshed = true;
						break;
					case (CONSUMED_COLUMN_IDX) :
					case (DELTA_COLUMN_IDX) :
						throw new UITechException("Cette colonne ne peut pas être modifiée");
					default : 
						throw new UITechException("Colonne inconnue");
				}
				// Mise à jour en base
				ModelMgr.updateTask(task);
				// Mise à jour des labels
				if (parentsMustBeRefreshed) {
					// Mise à jour des sommes des taches parentes
					updateBranchItemsSums(item);
				}
				else {
					// Notification de la mise à jour uniquement pour la tache
					notifyLabelProviderListener(new LabelProviderChangedEvent(labelProvider, new Object[] { task }));
				}
				return null;
			}
		};
		// Exécution
		safeRunner.run(parent.getShell());
	}

	/**
	 * Met à jour les sommes associés aux taches de la branche associée à l'item spécifié.
	 * @param item l'item de tableau.
	 * @throws ModelException levée en cas d'invalidité associée au modèle.
	 * @throws DbException levé en cas d'incident technique avec la base de données.
	 */
	private void updateBranchItemsSums(TreeItem item) 
			throws ModelException, DbException {
		ArrayList list = new ArrayList();
		// Nettoyage du cache
		TreeItem cursor = item;
		while (cursor!=null) {
			Task taskCursor = (Task) cursor.getData();
			log.debug("Update task " + taskCursor.getName());
			tasksSums.remove(taskCursor);
			list.add(0, taskCursor);
			cursor = cursor.getParentItem();
		}
		// Notification de la mise à jour (ce qui recharge automatiquement
		// le cache des sommes de taches)
		notifyLabelProviderListener(new LabelProviderChangedEvent(this, list.toArray()));
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		log.debug("ITreeContentProvider.getChildren(" + element + ")");
		final Task task = (Task) element;
		return task.getSubTasksCount()>0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		log.debug("ITreeContentProvider.getChildren(" + parentElement + ")");
		final Task parentTask = (Task) parentElement;
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				Task[] subTasks = ModelMgr.getSubtasks(parentTask);
				return subTasks;
			}
		};
		Object[] result = (Object[]) safeRunner.run(parent.getShell(), new Object[] {});
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		log.debug("ITreeContentProvider.getParent(" + element + ")");
		final Task task = (Task) element;
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				Task parentTask = ModelMgr.getParentTask(task);
				return parentTask==null? treeViewer.getInput() : parentTask;
			}
		};
		// Exécution du traitement
		Object result = (Object) safeRunner.run(parent.getShell());
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	public String getColumnText(Object element, int columnIndex) {
		log.debug("ITableLabelProvider.getColumnText(" + element + ", " + columnIndex + ")");
		return (String) getValue(element, treeColsMgr.getColumnCode(columnIndex));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetSelected(final SelectionEvent e) {
		log.debug("SelectionListener.widgetSelected(" + e + ")");
		final Object source = e.getSource();
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				TreeItem[] selection = treeViewer.getTree().getSelection();
				// Cas d'une création (même niveau)
				if (newTaskItem.equals(source)) {
					// Récupération du noeud parent
					TreeItem parentItem = selection.length>0 ? selection[0].getParentItem() : null;
					Task parentTask = parentItem==null ? null : (Task) parentItem.getData();
					// Création de la tache
					newTask(parentTask);
				}
				// Cas d'une création de sous tache
				else if (newSubtaskItem.equals(source)) {
					Task selectedTask = (Task) selection[0].getData();
					newTask(selectedTask);
				}
				// Cas d'une demande de déplacement vers le haut
				else if (moveUpItem.equals(source)) {
					Task selectedTask = (Task) selection[0].getData();
					ModelMgr.moveUpTask(selectedTask);
					// Mise à jour de l'IHM
					treeViewer.refresh(selection[0].getParent().getData(), true);
				}
				// Cas d'une demande de déplacement vers le haut
				else if (moveDownItem.equals(source)) {
					Task selectedTask = (Task) selection[0].getData();
					ModelMgr.moveDownTask(selectedTask);
					// Mise à jour de l'IHM
					treeViewer.refresh(selection[0].getParent().getData(), true);
				}
				// Cas d'une demande de déplacement vers une autre tache
				else if (moveToAnotherTaskItem.equals(source)) {
					Task taskToMove = (Task) selection[0].getData();
					// Récupération du noeud parent
					TreeItem parentItem = selection[0].getParentItem();
					final Task srcParentTask = (parentItem!=null) ? (Task) parentItem.getData() : null;
					// Création du valideur
					taskChooserDialog.setValidator(new ITaskChooserValidator() {
						public void validateChoosenTask(Task selectedTask) throws DialogException {
							if (srcParentTask!=null && srcParentTask.equals(selectedTask))
								throw new DialogException("This parent task is the source parent task.", null);
							try { ModelMgr.checkAcceptsSubtasks(selectedTask); }
							catch (ModelException e) {
								throw new DialogException(e.getMessage(), null);
							}
							// TODO Ajouter au ITaskChooserValidator la levée d'exception techniques pour ne plus avoir ce catch
							catch (DbException e) {
								throw new DialogException("Incident technique : '" + e.getMessage() + "'", null);
							}
						}
					});
					// Affichage du popup
					Task newParentTask = null;
					if (taskChooserDialog.open()==Dialog.OK) {
						newParentTask = (Task) taskChooserDialog.getValue();
						log.debug("Selected parent task=" + newParentTask);
						ModelMgr.moveTask(taskToMove, newParentTask);
						// Rafraichir l'ancien et le nouveau parent ne suffit pas
						// dans le cas ou le parent destination change de numéro 
						// (ex : déplacement d'une tache A vers une tache B avec
						// A et B initialement soeurs, A étant placé avant B)
						//treeViewer.refresh(newParentTask);
						//treeViewer.refresh(srcParentTask);
						treeViewer.refresh();
					}
				}
				// Cas d'une demande de déplacement vers la racine
				else if (moveToRootItem.equals(source)) {
					TreeItem selectedItem = selection[0];
					Task taskToMove = (Task) selectedItem.getData();
					// Déplacement
					ModelMgr.moveTask(taskToMove, null);
					treeViewer.refresh();
				}
				// Cas d'une demande de liste des contributions
				else if (listTaskContributionsItem.equals(source)) {
					TreeItem selectedItem = selection[0];
					Task selectedTask = (Task) selectedItem.getData();
					contribsViewerDialog.setFilter(selectedTask, null, null, null, null);
					// Ouverture du dialogue
					contribsViewerDialog.open();
				}
				// Cas d'une suppression
				else if (removeItem.equals(source)) {
					TreeItem selectedItem = selection[0];
					TreeItem parentItem = selectedItem.getParentItem();
					Task selectedTask = (Task) selectedItem.getData();
					Task parentTask = (parentItem!=null) ? (Task) parentItem.getData() : null;
					// Suppression
					ModelMgr.removeTask(selectedTask);
					// Suppression dans l'arbre
					treeViewer.remove(selectedTask);
					// Mise à jour des sommes des taches parentes
					updateBranchItemsSums(parentItem);
					// Mise à jour des taches soeurs 
					if (parentTask != null)
						treeViewer.refresh(parentTask);
					else
						treeViewer.refresh();
				}
				// Cas d'une demande de rafraichissement
				else if (refreshItem.equals(source)) {
					// Récupération du noeud parent
					TreeItem selectedItem = selection[0];
					TreeItem parentItem = selectedItem.getParentItem();
					Task parentTask = (parentItem!=null) ? (Task) parentItem.getData() : null;
					// Mise à jour
					if (parentTask != null)
						treeViewer.refresh(parentTask);
					else
						treeViewer.refresh();
				}
				// Cas d'une demande d'expansion d'un noeud
				else if (expandItem.equals(source)) {
					TreeItem selectedItem = treeViewer.getTree().getSelection()[0];
					treeViewer.expandToLevel(
							selectedItem.getData(), AbstractTreeViewer.ALL_LEVELS);
				}
				// Cas d'une demande de fermeture d'un noeud
				else if (collapseItem.equals(source)) {
					TreeItem selectedItem = treeViewer.getTree().getSelection()[0];
					treeViewer.collapseToLevel(
							selectedItem.getData(), AbstractTreeViewer.ALL_LEVELS);
				}
				// Cas d'une demande d'export du tableau
				else if (exportItem.equals(source)) {
					// Export du tableau
					SWTHelper.exportToWorkBook(treeViewer.getTree());
				}
				return null;
			}
		};
		// Exécution
		safeRunner.run(parent.getShell());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent e) {
		widgetSelected(e);
	}

	/**
	 * Ajoute une tache.
	 * @param parentTask la tache parent ou null pour une tache racine.
	 * @param code le code de tache.
	 * @param name le nom de la tache.
	 * @throws DbException levé en cas d'incident associé à la persistence.
	 * @throws ModelException levé en cas de violation du modèle de données.
	 */
	private void newTask(Task parentTask) throws DbException, ModelException {
		log.debug("newTask(" + parentTask + ")");
		// Création de la nouvelle tache
		Task newTask = ModelMgr.createNewTask(parentTask);
		// Ajout dans l'arbre et création en base
		treeViewer.add(parentTask==null ? treeViewer.getInput() : parentTask, newTask);
		treeViewer.setSelection(new StructuredSelection(newTask), true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MenuListener#menuShown(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuShown(MenuEvent e) {
		log.debug("menuShown(" + e + ")");
		TreeItem[] selection = treeViewer.getTree().getSelection();
		boolean emptySelection = selection.length==0;
		boolean singleSelection = selection.length==1;
		boolean rootSingleSelection = singleSelection && (selection[0]==null);
		newTaskItem.setEnabled(emptySelection || singleSelection);
		newSubtaskItem.setEnabled(singleSelection);
		moveUpItem.setEnabled(singleSelection);
		moveDownItem.setEnabled(singleSelection);
		moveToAnotherTaskItem.setEnabled(singleSelection);
		moveToRootItem.setEnabled(singleSelection && !rootSingleSelection);
		listTaskContributionsItem.setEnabled(singleSelection);
		removeItem.setEnabled(singleSelection);
		refreshItem.setEnabled(singleSelection);
		expandItem.setEnabled(singleSelection);
		collapseItem.setEnabled(singleSelection);
		exportItem.setEnabled(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MenuListener#menuHidden(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuHidden(MenuEvent e) {
		// Do nothing...
	}

}
