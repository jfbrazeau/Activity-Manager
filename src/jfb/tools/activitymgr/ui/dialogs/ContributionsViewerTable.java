/*
 * Copyright (c) 2004-2006, Jean-François Brazeau. All rights reserved.
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

import java.text.SimpleDateFormat;
import java.util.HashMap;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Collaborator;
import jfb.tools.activitymgr.core.beans.Contribution;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tools.activitymgr.core.util.StringHelper;
import jfb.tools.activitymgr.ui.util.AbstractTableMgr;
import jfb.tools.activitymgr.ui.util.SWTHelper;
import jfb.tools.activitymgr.ui.util.SafeRunner;
import jfb.tools.activitymgr.ui.util.TableOrTreeColumnsMgr;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.TableViewer;
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
import org.eclipse.swt.widgets.Table;

public class ContributionsViewerTable extends AbstractTableMgr 
	implements SelectionListener, MenuListener {

	/** Logger */
	private static Logger log = Logger.getLogger(ContributionsViewerTable.class);

	/** Filtre de recherche */
	private Task task;
	private Collaborator contributor;
	private Integer day;
	private Integer month;
	private Integer year;
	
	/** Constantes associées aux colonnes */
	public static final int DATE_COLUMN_IDX = 0;
	public static final int COLLABORATOR_COLUMN_IDX = 1;
	public static final int TASK_CODE_PATH_COLUMN_IDX = 2;
	public static final int TASK_NAME_COLUMN_IDX = 3;
	public static final int DURATION_COLUMN_IDX = 4;
	private static TableOrTreeColumnsMgr tableColsMgr;
	
	/** Formatteur de date */
	private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	
	/** Viewer */
	private TableViewer tableViewer;

	/** Composant parent */
	private Composite parent;
	
	/** Items de menu */
	private MenuItem exportItem;

	/** Cache de taches */
	private HashMap tasksCache = new HashMap();

	/** Cache de chemins de tache */
	private HashMap taskCodePathsCache = new HashMap();

	/** Cache de collaborateurs */
	private HashMap collaboratorsCache = new HashMap();

	/**
	 * Constructeur par défaut.
	 * @param parentComposite composant parent.
	 * @param layoutData données associées au layout.
	 */
	public ContributionsViewerTable(Composite parentComposite, Object layoutData) {
		log.debug("new ContributionsViewerTable()");
		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayoutData(layoutData);
		parent.setLayout(new GridLayout(1, false));
		
		// Arbre tableau
		final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.HIDE_SELECTION | SWT.MULTI);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 300;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setEnabled(true);

		// Création du viewer
		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(this);
		tableViewer.setLabelProvider(this);

		// Configuration des colonnes
		tableColsMgr = new TableOrTreeColumnsMgr();
		tableColsMgr.addColumn("DATE", "Date", 70, SWT.LEFT);
		tableColsMgr.addColumn("COLLABORATOR", "Collaborator", 100, SWT.LEFT);
		tableColsMgr.addColumn("TASK_NAME", "Task path", 170, SWT.LEFT);
		tableColsMgr.addColumn("TASK_NAME", "Task name", 170, SWT.LEFT);
		tableColsMgr.addColumn("DURATION", "Duration", 50, SWT.LEFT);
		tableColsMgr.configureTable(tableViewer);

		// Configuration du menu popup
		final Menu menu = new Menu(table);
		menu.addMenuListener(this);
		exportItem = new MenuItem(menu, SWT.CASCADE);
		exportItem.setText("Export");
		exportItem.addSelectionListener(this);
		table.setMenu(menu);
	}

	/**
	 * Initialise le filtre de recherche des contributions.
	 * @param task la tache.
	 * @param contributor le collaborateur.
	 * @param year l'année.
	 * @param month le mois.
	 * @param day le jour.
	 */
	public void setFilter(Task task, Collaborator contributor, Integer year, Integer month, Integer day) {
		// Initialisation du filtre de recherche
		this.task = task;
		this.contributor = contributor;
		this.year = year;
		this.month = month;
		this.day = day;
		// Création d'une racine fictive
		tableViewer.setInput(ROOT_NODE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		// Chargement des données
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				// Recherche des collaborateurs 
				return ModelMgr.getContributions(task, contributor, year, month, day);
			}
		};
		// Exécution
		Object result = (Object) safeRunner.run(parent.getShell());
		return (Contribution[]) (result!=null ? result : new Contribution[] {});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	public String getColumnText(final Object element, final int columnIndex) {
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				Contribution c = (Contribution) element;
				String text = null;
				switch (columnIndex) {
					case (DATE_COLUMN_IDX) :
						text = sdf.format(c.getDate().getTime());
						break;
					case (COLLABORATOR_COLUMN_IDX) :
						Collaborator collaborator = getCachedCollaborator(c.getContributorId());
						text = collaborator.getFirstName() + " " + collaborator.getLastName();
						break;
					case (TASK_CODE_PATH_COLUMN_IDX) :
						text = getCachedTaskCodePath(c.getTaskId());
						break;
					case (TASK_NAME_COLUMN_IDX) :
						text = getCachedTask(c.getTaskId()).getName();
						break;
					case (DURATION_COLUMN_IDX) :
						text = StringHelper.hundredthToEntry(c.getDurationId());
						break;
					default : throw new Error("Colonne inconnue");
				}
				return text;
			}
		};
		// Exécution
		return (String) safeRunner.run(parent.getShell(), "");
	}

	/**
	 * Retourne le chemin de la tache associée à l'identifiant spécifié.
	 * @param taskId l'identifiant de la tache.
	 * @return le chemin.
	 * @throws ModelException levé en cas de viloation du modèle.
	 * @throws DbException levé en cas d'incident associé à l'accès à la base de données.
	 */
	private String getCachedTaskCodePath(long taskId) throws ModelException, DbException {
		Long _taskId = new Long(taskId);
		String taskCodePath = (String) taskCodePathsCache.get(_taskId);
		if (taskCodePath==null) {
			log.debug("Registering in cache task code path for taskId=" + taskId);
			Task task = getCachedTask(taskId);
			taskCodePath = ModelMgr.getTaskCodePath(task);
			taskCodePathsCache.put(_taskId, taskCodePath);
		}
		return taskCodePath;
	}
	
	/**
	 * Retourne la tache associée à l'identifiant spécifié.
	 * @param taskId l'identifiant de la tache.
	 * @return la tache.
	 * @throws DbException levé en cas d'incident associé à l'accès à la base de données.
	 */
	private Task getCachedTask(long taskId) throws DbException {
		Long _taskId = new Long(taskId);
		Task task = (Task) tasksCache.get(_taskId);
		if (task==null) {
			log.debug("Registering in cache task for taskId=" + taskId);
			task = ModelMgr.getTask(taskId);
			tasksCache.put(_taskId, task);
		}
		return task;
	}
	
	/**
	 * Retourne le collaborateur associée à l'identifiant spécifié.
	 * @param collaboratorId l'identifiant du collaborateur.
	 * @return le collaborateur.
	 * @throws DbException levé en cas d'incident associé à l'accès à la base de données.
	 */
	private Collaborator getCachedCollaborator(long collaboratorId) throws DbException {
		Long _collaboratorId = new Long(collaboratorId);
		Collaborator collaborator = (Collaborator) collaboratorsCache.get(_collaboratorId);
		if (collaborator==null) {
			log.debug("Registering in cache collaborator for collaboratorId=" + collaboratorId);
			collaborator = ModelMgr.getCollaborator(collaboratorId);
			collaboratorsCache.put(_collaboratorId, collaborator);
		}
		return collaborator;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MenuListener#menuShown(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuShown(MenuEvent e) {
		log.debug("menuShown(" + e + ")");
//		TableItem[] selection = tableViewer.getTable().getSelection();
//		boolean emptySelection = selection.length==0;
//		boolean singleSelection = selection.length==1;
		exportItem.setEnabled(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MenuListener#menuHidden(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuHidden(MenuEvent e) {
		// Do nothing...
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
				// Cas d'une demande d'export du tableau
				if (exportItem.equals(source)) {
					// Export du tableau
					SWTHelper.exportToWorkBook(tableViewer.getTable());
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

}
