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


import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSearchFilter;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.util.AbstractTableMgr;
import org.activitymgr.ui.rcp.util.SafeRunner;
import org.activitymgr.ui.rcp.util.TableOrTreeColumnsMgr;
import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
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

	/** Model manager */
	private IModelMgr modelMgr;

	private Text filterText;

	/**
	 * Constructeur par défaut.
	 * 
	 * @param parentComposite
	 *            composant parent.
	 * @param layoutData
	 *            données du layout.
	 * @param modelMgr
	 *            the model manager.
	 */
	public TaskChooserTree(Composite parentComposite, Object layoutData, final IModelMgr modelMgr) {
		this.modelMgr = modelMgr;
		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayoutData(layoutData);
		parent.setLayout(new GridLayout());
		
		filterText = new Text(parent, SWT.BORDER);
		filterText.setMessage(Strings.getString("TaskChooserTree.labels.PLACEHOLDER"));
		GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		filterText.setLayoutData(gd);
		final Display display = filterText.getShell().getDisplay();
		filterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				final String value = filterText.getText();
				new Thread() {
					public void run() {
						try {
							Thread.sleep(350);
							// Only refresh if the entry has not changed since 350 ms
							display.syncExec(new Runnable() {
								@Override
								public void run() {
									if (filterText.getText().equals(value)) {
										treeViewer.refresh();
										if (treeViewer.getTree().getItems().length > 0) {
											Task firstTaskMatching = modelMgr.getFirstTaskMatching(value);
											treeViewer.reveal(firstTaskMatching);
										}
									}
								}
							});
						} catch (InterruptedException e) {
						}
						
					};
				}.start();
			}
		});


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

		// Configure styled texts
        TreeViewerColumn nameColumn = new TreeViewerColumn(treeViewer, tree.getColumns()[0]);
        nameColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new StyledLabelProvider(filterText) {
        	@Override
        	protected String getDisplayedProperty(Task task) {
        		return task.getName();
        	}
        }));
        TreeViewerColumn codeColumn = new TreeViewerColumn(treeViewer, tree.getColumns()[1]);
        codeColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new StyledLabelProvider(filterText) {
        	@Override
        	protected String getDisplayedProperty(Task task) {
        		return task.getCode();
        	}
        }));

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
		return modelMgr.getSubTasksCount(task.getId()) > 0;
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
				return modelMgr.getSubTasks(parentTask != null ? parentTask.getId() : null, filterText.getText());
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
				Task parentTask = modelMgr.getParentTask(task);
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

abstract class StyledLabelProvider extends BaseLabelProvider implements IStyledLabelProvider {
	
	private Text filterText;

	StyledLabelProvider(Text filterText) {
		this.filterText = filterText;
	}
	
	@Override
	public StyledString getStyledText(Object element) {
		Task task = (Task) element;
		String text = getDisplayedProperty(task);
		String textToLC = text.toLowerCase();
		StyledString str = new StyledString(text);
		String filterLC = filterText.getText().toLowerCase();
		int filterLength = filterLC.length();
		if (filterLength > 0) {
		int indexOf = 0;
			while ((indexOf = textToLC.indexOf(filterLC, indexOf)) >= 0) {
				str.setStyle(indexOf, filterLength, StyledString.QUALIFIER_STYLER);
				indexOf += filterLength;
			}
		}
		return str;
	}

	protected abstract String getDisplayedProperty(Task task);
	
	@Override
	public Image getImage(Object element) {
		return null;
	}

}