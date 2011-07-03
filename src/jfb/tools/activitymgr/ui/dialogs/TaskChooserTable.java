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

import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tools.activitymgr.core.util.Strings;
import jfb.tools.activitymgr.ui.util.AbstractTableMgr;
import jfb.tools.activitymgr.ui.util.SafeRunner;
import jfb.tools.activitymgr.ui.util.TableOrTreeColumnsMgr;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * Composant g�rant l'historique des t�che adffich� dans le dialogue de choix
 * d'un t�che.
 */
public class TaskChooserTable extends AbstractTableMgr {

	/** Logger */
	private static Logger log = Logger.getLogger(TaskChooserTree.class);

	/** Constantes associ�es aux colonnes */
	public static final int TASK_PATH_COLUMN_IDX = 0;
	public static final int TASK_COLUMN_IDX = 1;
	private static TableOrTreeColumnsMgr tableColsMgr;

	/** Viewer */
	private TableViewer tableViewer;

	/** Composant parent */
	private Composite parent;

	/**
	 * Constructeur par d�faut.
	 * 
	 * @param parentComposite
	 *            composant parent.
	 * @param layoutData
	 *            donn�es du layout.
	 * @param tasks
	 *            la liste des taches � afficher.
	 */
	public TaskChooserTable(Composite parentComposite, Object layoutData,
			Task[] tasks) {
		// Cr�ation du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayoutData(layoutData);
		parent.setLayout(new GridLayout());

		// Table
		final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER
				| SWT.HIDE_SELECTION | SWT.SINGLE);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 300;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setEnabled(true);

		// Cr�ation du viewer
		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(this);
		tableViewer.setLabelProvider(this);

		// Configuration des colonnes
		tableColsMgr = new TableOrTreeColumnsMgr();
		tableColsMgr
				.addColumn(
						"TASK_PATH", Strings.getString("TaskChooserTable.columns.TASK_PATH"), 150, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr
				.addColumn(
						"TASK", Strings.getString("TaskChooserTable.columns.TASK_NAME"), 200, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr.configureTable(tableViewer);

		// Initialisation du tableau
		tableViewer.setInput(tasks);
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
				Task task = (Task) element;
				String text = null;
				switch (columnIndex) {
				case (TASK_PATH_COLUMN_IDX):
					text = ModelMgr.getTaskCodePath(task);
					break;
				case (TASK_COLUMN_IDX):
					text = task.getName();
					break;
				default:
					throw new Error(
							Strings.getString("TaskChooserTable.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				return text;
			}
		};
		// Ex�cution
		return (String) safeRunner.run(parent.getShell(), ""); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java
	 * .lang.Object)
	 */
	public Object[] getElements(final Object inputElement) {
		return (Task[]) inputElement;
	}

	/**
	 * Retourne le viewer associ� au tableau.
	 * 
	 * @return le viewer associ� au tableau.
	 */
	public TableViewer getTableViewer() {
		return tableViewer;
	}

}
