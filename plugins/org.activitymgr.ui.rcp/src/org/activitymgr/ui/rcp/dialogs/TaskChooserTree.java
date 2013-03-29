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
package org.activitymgr.ui.rcp.dialogs;


import org.activitymgr.core.ModelMgr;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.util.AbstractTableMgr;
import org.activitymgr.ui.rcp.util.SafeRunner;
import org.activitymgr.ui.rcp.util.TableOrTreeColumnsMgr;
import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

public class TaskChooserTree extends AbstractTableMgr implements
		ITreeContentProvider {

	/** Logger */
	private static Logger log = Logger.getLogger(TaskChooserTree.class);

	/** Constantes associées aux colonnes */
	public static final int NAME_COLUMN_IDX = 0;
	public static final int CODE_COLUMN_IDX = 1;
	private static TableOrTreeColumnsMgr treeColsMgr;

	/** Viewer */
	private TreeViewer treeViewer;

	/** Composant parent */
	private Composite parent;

	/**
	 * Constructeur par défaut.
	 * 
	 * @param parentComposite
	 *            composant parent.
	 * @param layoutData
	 *            données du layout.
	 */
	public TaskChooserTree(Composite parentComposite, Object layoutData) {
		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayoutData(layoutData);
		parent.setLayout(new GridLayout());

		// Arbre tableau
		final Tree tree = new Tree(parent, SWT.FULL_SELECTION | SWT.BORDER
				| SWT.HIDE_SELECTION);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 300;
		tree.setLayoutData(gridData);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		tree.setEnabled(true);

		// Création du viewer
		treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(this);
		treeViewer.setLabelProvider(this);

		// Configuration des colonnes
		treeColsMgr = new TableOrTreeColumnsMgr();
		treeColsMgr
				.addColumn(
						"NAME", Strings.getString("TaskChooserTree.columns.TASK_NAME"), 200, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		treeColsMgr
				.addColumn(
						"CODE", Strings.getString("TaskChooserTree.columns.TASK_CODE"), 70, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		treeColsMgr.configureTree(treeViewer);

		// Création d'une racine fictive
		treeViewer.setInput(ROOT_NODE);
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
	 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object,
	 * java.lang.String)
	 */
	public Object getValue(Object element, String property) {
		log.debug("ICellModifier.getValue(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		Task task = (Task) element;
		String value = null;
		int propertyIdx = treeColsMgr.getColumnIndex(property);
		switch (propertyIdx) {
		case (NAME_COLUMN_IDX):
			value = task.getName();
			break;
		case (CODE_COLUMN_IDX):
			value = task.getCode();
			break;
		default:
			throw new Error(
					Strings.getString("TaskChooserTree.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
		}
		return value;
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
				return ModelMgr.getSubtasks(parentTask);
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
		Task task = (Task) element;
		String text = null;
		switch (columnIndex) {
		case NAME_COLUMN_IDX:
			text = task.getName();
			break;
		case CODE_COLUMN_IDX:
			text = task.getCode();
			break;
		default:
			throw new Error(
					Strings.getString("TaskChooserTree.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
		}
		return text;
	}

	/**
	 * Retourne le viewer associé à l'arbre.
	 * 
	 * @return le viewer associé à l'arbre.
	 */
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
}
