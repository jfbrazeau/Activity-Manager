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
package jfb.tools.activitymgr.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tools.activitymgr.core.util.Strings;
import jfb.tools.activitymgr.ui.DatabaseUI.IDbStatusListener;
import jfb.tools.activitymgr.ui.TasksUI.ITaskListener;
import jfb.tools.activitymgr.ui.util.ITaskSelectionListener;
import jfb.tools.activitymgr.ui.util.SafeRunner;
import jfb.tools.activitymgr.ui.util.TaskFinderPanel;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class TaskChooserTreeWithHistoryDialog extends AbstractDialog implements
		ITaskListener, IDbStatusListener {

	/** Logger */
	private static Logger log = Logger
			.getLogger(TaskChooserTreeWithHistoryDialog.class);

	/** Arbre contenant la liste des t�ches */
	private TaskChooserTree tasksTree;

	/** Tableau contenant les derni�res taches s�lectionn�es */
	private TaskChooserTable previouslySelectedTasksTable;

	/** Valideur */
	private ITaskChooserValidator validator;

	/** Panneau de recherche de tache */
	private TaskFinderPanel taskFinderPanel;

	/** Liste des s�lections pr�c�dentes */
	private List<Task> previouslySelectedTasks = new ArrayList<Task>();

	/**
	 * Constructeur par d�faut.
	 * 
	 * @param parentShell
	 *            shell parent.
	 */
	public TaskChooserTreeWithHistoryDialog(Shell parentShell) {
		super(
				parentShell,
				Strings.getString("TaskChooserTreeWithHistoryDialog.texts.TITLE"), null, null); //$NON-NLS-1$
		setShellStyle(SWT.RESIZE | SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.util.AbstractDialog#validateUserEntry()
	 */
	protected Object validateUserEntry() throws DialogException {
		log.debug("validateUserEntry"); //$NON-NLS-1$
		Task selectedTask = null;
		Tree tree = tasksTree.getTreeViewer().getTree();
		TreeItem[] selection = tree.getSelection();
		if (selection.length > 0)
			selectedTask = (Task) selection[0].getData();
		log.debug("Selected task = " + selectedTask); //$NON-NLS-1$
		if (selectedTask == null)
			throw new DialogException(
					Strings.getString("TaskChooserTreeWithHistoryDialog.errors.TASK_REQUIRED"), null); //$NON-NLS-1$
		if (validator != null)
			validator.validateChoosenTask(selectedTask);
		// Suppression puis enregistrement de la tache dans l'historique
		// (la suppression permet de garantir que la tache soit la premi�re
		// dans l'historique)
		previouslySelectedTasks.remove(selectedTask);
		if (!previouslySelectedTasks.contains(selectedTask))
			previouslySelectedTasks.add(selectedTask);
		// Purge de la liste si + de 20 valeurs
		if (previouslySelectedTasks.size() == 21)
			previouslySelectedTasks.remove(20);
		// Validation du choix de la tache
		return selectedTask;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.util.AbstractDialog#createDialogArea(org.eclipse
	 * .swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite parentComposite = (Composite) super.createDialogArea(parent);

		// Panneau permettant de recherche une tache
		taskFinderPanel = new TaskFinderPanel(parentComposite);
		GridData gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
		gridData.horizontalSpan = 2;
		taskFinderPanel.setLayoutData(gridData);
		taskFinderPanel.addTaskListener(new ITaskSelectionListener() {
			public void taskSelected(Task selectedTask) {
				selectTaskInTree(selectedTask);
			}
		});

		// Montre-t-on le panneau contenant l'hitorique des s�lections ?
		boolean showHistoryPanel = (previouslySelectedTasks.size() != 0);

		// Arbre contenant les taches
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		if (!showHistoryPanel)
			gridData.horizontalSpan = 2;
		Group taskTreeGroup = new Group(parentComposite, SWT.NONE);
		taskTreeGroup
				.setText(Strings
						.getString("TaskChooserTreeWithHistoryDialog.labels.TASK_TREE")); //$NON-NLS-1$
		taskTreeGroup.setLayoutData(gridData);
		taskTreeGroup.setLayout(new FillLayout());
		tasksTree = new TaskChooserTree(taskTreeGroup, null);
		TreeViewer viewer = tasksTree.getTreeViewer();
		viewer.getTree().addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				okPressed();
			}
		});
		Task lastValue = (Task) getValue();
		if (lastValue != null) {
			viewer.setSelection(new StructuredSelection(lastValue));
		}

		// Zone de s�lection des taches pr�c�demment s�lectionn�es
		if (showHistoryPanel) {
			gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			Group lastSelectionsGroup = new Group(parentComposite, SWT.NONE);
			lastSelectionsGroup
					.setText(Strings
							.getString("TaskChooserTreeWithHistoryDialog.labels.PREVIOUS_SELECTION")); //$NON-NLS-1$
			lastSelectionsGroup.setLayoutData(gridData);
			lastSelectionsGroup.setLayout(new FillLayout());
			// R�cup�ration de l'hitorique avec tri invers� (le premier arriv�
			// est affich� en dernier)
			Task[] history = (Task[]) previouslySelectedTasks
					.toArray(new Task[previouslySelectedTasks.size()]);
			Task[] _history = new Task[history.length];
			for (int i = 0; i < history.length; i++)
				_history[history.length - i - 1] = history[i];
			previouslySelectedTasksTable = new TaskChooserTable(
					lastSelectionsGroup, null, _history);
			final Table table = previouslySelectedTasksTable.getTableViewer()
					.getTable();
			table.addMouseListener(new MouseAdapter() {
				public void mouseDoubleClick(MouseEvent e) {
					mouseDown(e);
					StructuredSelection selection = (StructuredSelection) previouslySelectedTasksTable
							.getTableViewer().getSelection();
					if (selection.getFirstElement() != null)
						okPressed();
				}

				public void mouseDown(MouseEvent e) {
					StructuredSelection selection = (StructuredSelection) previouslySelectedTasksTable
							.getTableViewer().getSelection();
					if (selection != null) {
						Task selectedTask = (Task) selection.getFirstElement();
						if (selectedTask != null)
							selectTaskInTree(selectedTask);
					}
				}
			});
		}

		// Retour du composant parent
		return parentComposite;
	}

	/**
	 * S�lectionne la tache sp�cifi�e dans l'arbre des taches.
	 * 
	 * @param selectedTask
	 *            la tache � s�lectionner.
	 */
	private void selectTaskInTree(Task selectedTask) {
		TreeViewer treeViewer = tasksTree.getTreeViewer();
		treeViewer.setSelection(new StructuredSelection(selectedTask));
		treeViewer.getTree().setFocus();
	}

	/**
	 * @param validator
	 *            le nouveau valideur.
	 */
	public void setValidator(ITaskChooserValidator validator) {
		this.validator = validator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.TasksUI.TaskListener#taskAdded(jfb.tools.activitymgr
	 * .core.beans.Task)
	 */
	public void taskAdded(Task task) {
		// Quand une tache est ajout�e, elle ne peut pas �tre pr�sente dans
		// l'historique
		// des taches => donc rien � faire
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
				// Parcours des taches pr�sentes dans le tableau
				int itemIdxToRemove = -1;
				for (int i = 0; i < previouslySelectedTasks.size(); i++) {
					Task currentTask = (Task) previouslySelectedTasks.get(i);
					// Cas ou la tache supprim�e est dans le tableau
					// dans ce cas, on sauvegarde le N� pour effectuer
					// la suppression par la suite
					if (currentTask.getId() == removedTask.getId()) {
						itemIdxToRemove = i;
					}
					// Autre cas : la tache supprim�e est la soeur d'une des
					// taches parent
					// de la tache en cours ; c'est le cas si le chemin de la
					// tache en cours
					// commence par le chemin de la tache qui a �t� supprim�e
					else if (currentTask.getPath().startsWith(
							removedTask.getPath())) {
						String removedTaskFullpath = removedTask.getFullPath();
						String removedTaskSisterFullPath = currentTask
								.getFullPath().substring(0,
										removedTaskFullpath.length());
						// La tache n'est impact�e que si sa tache parent se
						// trouvant �tre la soeur de
						// celle qui a �t� supprim�e poss�de un num�ro sup�rieur
						// � celui de la
						// tache supprim�e
						if (removedTaskSisterFullPath
								.compareTo(removedTaskFullpath) > 0) {
							// Dans ce cas il faut mettre � jour le chemin de la
							// tache
							currentTask = ModelMgr.getTask(currentTask.getId());
							previouslySelectedTasks.set(i, currentTask);
						}
					}
				}
				// Si on a trouv� l'item supprim�, on le supprime
				if (itemIdxToRemove >= 0)
					previouslySelectedTasks.remove(itemIdxToRemove);
				return null;
			}
		}.run(getParentShell());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.TasksUI.TaskListener#taskUpdated(jfb.tools.
	 * activitymgr.core.beans.Task)
	 */
	public void taskUpdated(Task updatedTask) {
		boolean found = false;
		for (int i = 0; !found && i < previouslySelectedTasks.size(); i++) {
			Task currentTask = (Task) previouslySelectedTasks.get(i);
			found = currentTask.getId() == updatedTask.getId();
			if (found)
				previouslySelectedTasks.set(i, updatedTask);
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
				// D�duction de l'ancien chemin de la tache � partir de l'ancien
				// chemin complet
				String oldTaskPath = oldTaskFullpath.substring(0,
						oldTaskFullpath.length() - 2);
				for (int i = 0; i < previouslySelectedTasks.size(); i++) {
					Task currentTask = (Task) previouslySelectedTasks.get(i);
					// Cas ou la tache modifi�e est dans la liste
					if (currentTask.getId() == movedTask.getId()) {
						previouslySelectedTasks.set(i, movedTask);
					}
					// Autre cas : la tache a d�plac�e est une tache
					// parent de la tache en cours
					else if (currentTask.getPath().startsWith(oldTaskFullpath)) {
						currentTask = ModelMgr.getTask(currentTask.getId());
						previouslySelectedTasks.set(i, currentTask);
					}
					// Autre cas : la tache d�plac�e est la soeur d'une des
					// taches parent
					// de la tache en cours
					else if (currentTask.getPath().startsWith(oldTaskPath)) {
						currentTask = ModelMgr.getTask(currentTask.getId());
						previouslySelectedTasks.set(i, currentTask);
					}
				}
				return null;
			}

		}.run(getParentShell());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.DatabaseUI.IDbStatusListener#databaseClosed()
	 */
	public void databaseClosed() {
		previouslySelectedTasks.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.DatabaseUI.IDbStatusListener#databaseOpened()
	 */
	public void databaseOpened() {
	}

}
