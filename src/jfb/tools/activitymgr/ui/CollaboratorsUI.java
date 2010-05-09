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
import java.util.Iterator;

import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Collaborator;
import jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener;
import jfb.tools.activitymgr.ui.dialogs.ContributionsViewerDialog;
import jfb.tools.activitymgr.ui.util.SWTHelper;
import jfb.tools.activitymgr.ui.util.SafeRunner;
import jfb.tools.activitymgr.ui.util.AbstractTableMgr;
import jfb.tools.activitymgr.ui.util.TableOrTreeColumnsMgr;
import jfb.tools.activitymgr.ui.util.UITechException;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * IHM de gestion des collaborateurs.
 */
public class CollaboratorsUI extends AbstractTableMgr implements DbStatusListener, ICellModifier, SelectionListener, MenuListener {

	/** Logger */
	private static Logger log = Logger.getLogger(CollaboratorsUI.class);

	/** Constantes associées aux colonnes */
	public static final int IDENTIFIER_COLUMN_IDX =      0;
	public static final int FIRST_NAME_COLUMN_IDX = 1;
	public static final int LAST_NAME_COLUMN_IDX =  2;
	private static TableOrTreeColumnsMgr tableColsMgr;

	/**
	 * Interface utilisée pour permettre l'écoute de la suppression ou de
	 * l'ajout de collaborateurs.
	 */
	public static interface CollaboratorListener {
		
		/**
		 * Indique qu'un collaborateur a été ajouté au référentiel.
		 * @param collaborator le collaborateur ajouté.
		 */
		public void collaboratorAdded(Collaborator collaborator);
		
		/**
		 * Indique qu'un collaborateur a été supprimé du référentiel.
		 * @param collaborator le collaborateur supprimé.
		 */
		public void collaboratorRemoved(Collaborator collaborator);

		/**
		 * Indique qu'un collaborateur a été modifié duans le référentiel.
		 * @param collaborator le collaborateur modifié.
		 */
		public void collaboratorUpdated(Collaborator collaborator);
	}
	
	/** Viewer */
	private TableViewer tableViewer;

	/** Items de menu */
	private MenuItem newItem;
	private MenuItem removeItem;
	private MenuItem listTaskContributionsItem;
	private MenuItem exportItem;
	
	/** Composant parent */
	private Composite parent;

	/** Popup permettant de lister les contributions d'une tache */
	private ContributionsViewerDialog contribsViewerDialog;
	
	/** Listeners */
	private ArrayList listeners = new ArrayList();

	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 * @param tabItem item parent.
	 */
	public CollaboratorsUI(TabItem tabItem) {
		this(tabItem.getParent());
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par défaut.
	 * @param parentComposite composant parent.
	 */
	public CollaboratorsUI(Composite parentComposite) {
		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));

		// Table
		final Table table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.HIDE_SELECTION);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
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
		tableColsMgr.addColumn("IDENTIFIER", "Identifier", 100, SWT.LEFT);
		tableColsMgr.addColumn("FIRST_NAME", "First name", 100, SWT.LEFT);
		tableColsMgr.addColumn("LAST_NAME", "Last name", 100, SWT.LEFT);
		tableColsMgr.configureTable(tableViewer);

		// Configuration des éditeurs de cellules
		CellEditor[] editors = new CellEditor[9];
		editors[IDENTIFIER_COLUMN_IDX] = new TextCellEditor(table);
		editors[FIRST_NAME_COLUMN_IDX] = new TextCellEditor(table);
		editors[LAST_NAME_COLUMN_IDX] = new TextCellEditor(table);
		tableViewer.setCellEditors(editors);

		// Initialisation des popups
		contribsViewerDialog = new ContributionsViewerDialog(parent.getShell());
		
		// Configuration du menu popup
		final Menu menu = new Menu(table);
		menu.addMenuListener(this);
		newItem = new MenuItem(menu, SWT.CASCADE);
		newItem.setText("New collaborator");
		newItem.addSelectionListener(this);
		removeItem = new MenuItem(menu, SWT.CASCADE);
		removeItem.setText("Remove");
		removeItem.addSelectionListener(this);
		listTaskContributionsItem = new MenuItem(menu, SWT.CASCADE);
		listTaskContributionsItem.setText("List contrib.");
		listTaskContributionsItem.addSelectionListener(this);
		exportItem = new MenuItem(menu, SWT.CASCADE);
		exportItem.setText("Export");
		exportItem.addSelectionListener(this);
		table.setMenu(menu);
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		// Chargement des données
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				// Recherche des collaborateurs 
				return ModelMgr.getCollaborators();
			}
		};
		// Exécution
		Object result = (Object) safeRunner.run(parent.getShell());
		return (Collaborator[]) (result!=null ? result : new Collaborator[] {});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
	 */
	public boolean canModify(Object element, String property) {
		log.debug("ICellModifier.canModify(" + element + ", " + property + ")");
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
	 */
	public Object getValue(final Object element, final String property) {
		log.debug("ICellModifier.getValue(" + element + ", " + property + ")");
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				Collaborator collaborator = (Collaborator) element;
				String text = null;
				int columnIndex = tableColsMgr.getColumnIndex(property);
				switch (columnIndex) {
					case (IDENTIFIER_COLUMN_IDX) :
						text = collaborator.getLogin();
						break;
					case (FIRST_NAME_COLUMN_IDX) :
						text = collaborator.getFirstName();
						break;
					case (LAST_NAME_COLUMN_IDX) :
						text = collaborator.getLastName();
						break;
					default : throw new Error("Colonne inconnue");
				}
				return text;
			}
		};
		// Exécution
		return safeRunner.run(parent.getShell(), "");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	public void modify(final Object element, String property, final Object value) {
		log.debug("ICellModifier.modify(" + element + ", " + property + ", " + value + ")");
		TableItem item = (TableItem) element;
		final Collaborator collaborator = (Collaborator) item.getData();
		final IBaseLabelProvider labelProvider = this;
		final int columnIndex = tableColsMgr.getColumnIndex(property);
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				switch (columnIndex) {
					case (IDENTIFIER_COLUMN_IDX) :
						String newIdentifier = (String) value;
						if (!collaborator.getLogin().equals(newIdentifier)) {
							collaborator.setLogin(newIdentifier);
						}
						break;
					case (FIRST_NAME_COLUMN_IDX) :
						collaborator.setFirstName((String) value);
						break;
					case (LAST_NAME_COLUMN_IDX) :
						collaborator.setLastName((String) value);
						break;
					default : throw new UITechException("Colonne inconnue");
				}
				// Mise à jour en base
				ModelMgr.updateCollaborator(collaborator);
				// Notification des listeners
				notifyLabelProviderListener(new LabelProviderChangedEvent(labelProvider, collaborator));
				notifyCollaboratorUpdated(collaborator);
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
				Collaborator collaborator = (Collaborator) element;
				String text = null;
				switch (columnIndex) {
					case (IDENTIFIER_COLUMN_IDX) :
						text = collaborator.getLogin();
						break;
					case (FIRST_NAME_COLUMN_IDX) :
						text = collaborator.getFirstName();
						break;
					case (LAST_NAME_COLUMN_IDX) :
						text = collaborator.getLastName();
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
				TableItem[] selection = tableViewer.getTable().getSelection();
				// Cas d'une création
				if (newItem.equals(source)) {
					Collaborator newCollaborator = ModelMgr.createNewCollaborator();
					newLine(newCollaborator);
					// Notification des listeners
					notifyCollaboratorAdded(newCollaborator);
				}
				// Cas d'une suppression
				else if (removeItem.equals(source)) {
					TableItem[] items = tableViewer.getTable().getSelection();
					for (int i=0; i<items.length; i++) {
						TableItem item = items[i];
						Collaborator collaborator = (Collaborator) item.getData();
						ModelMgr.removeCollaborator(collaborator);
						item.dispose();
						// Notification des listeners
						notifyCollaboratorRemoved(collaborator);
					}
				}
				// Cas d'une demande de liste des contributions
				else if (listTaskContributionsItem.equals(source)) {
					Collaborator selectedCollaborator = (Collaborator) selection[0].getData();
					contribsViewerDialog.setFilter(null, selectedCollaborator, null, null, null);
					// Ouverture du dialogue
					contribsViewerDialog.open();
				}
				// Cas d'une demande d'export
				else if (exportItem.equals(source)) {
					SWTHelper.exportToWorkBook(tableViewer.getTable());
				}
				return null;
			}
		};
		// Exécution
		safeRunner.run(parent.getShell());
	}

	/**
	 * Ajoute une ligne dans le tableau.
	 * @param collaborator le collaborateur associé à la nouvelle ligne.
	 */
	private void newLine(Collaborator collaborator) {
		// Ajout dans l'arbre
		tableViewer.add(collaborator);
		tableViewer.setSelection(new StructuredSelection(collaborator), true);
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
		removeItem.setEnabled(!emptySelection);
		listTaskContributionsItem.setEnabled(singleSelection);
		exportItem.setEnabled(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MenuListener#menuHidden(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuHidden(MenuEvent e) {
		// Do nothing...
	}

	/**
	 * Ajoute un listener.
	 * @param listener le nouveau listener.
	 */
	public void addCollaboratorListener(CollaboratorListener listener) {
		listeners.add(listener);
	}

	/**
	 * Ajoute un listener.
	 * @param listener le nouveau listener.
	 */
	public void removeCollaboratorListener(CollaboratorListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Notifie les listeners qu'un collaborateur a été ajouté.
	 * @param newCollaborator le collaborateur ajouté.
	 */
	private void notifyCollaboratorAdded(Collaborator newCollaborator) {
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			CollaboratorListener listener = (CollaboratorListener) it.next();
			listener.collaboratorAdded(newCollaborator);
		}
	}

	/**
	 * Notifie les listeners qu'un collaborateur a été supprimé.
	 * @param collaborator le collaborateur supprimé.
	 */
	private void notifyCollaboratorRemoved(Collaborator collaborator) {
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			CollaboratorListener listener = (CollaboratorListener) it.next();
			listener.collaboratorRemoved(collaborator);
		}
	}

	/**
	 * Notifie les listeners qu'un collaborateur a été modifié.
	 * @param collaborator le collaborateur modifié.
	 */
	private void notifyCollaboratorUpdated(Collaborator collaborator) {
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			CollaboratorListener listener = (CollaboratorListener) it.next();
			listener.collaboratorUpdated(collaborator);
		}
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener#databaseOpened()
	 */
	public void databaseOpened() {
		// Création d'une racine fictive
		tableViewer.setInput(ROOT_NODE);
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener#databaseClosed()
	 */
	public void databaseClosed() {
		Table table = tableViewer.getTable();
		TableItem[] items = table.getItems();
		for (int i=0; i<items.length; i++) {
			items[i].dispose();
		}
	}

}
