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
package org.activitymgr.ui.rcp.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.CollaboratorsUI.ICollaboratorListener;
import org.activitymgr.ui.rcp.DatabaseUI.IDbStatusListener;
import org.activitymgr.ui.rcp.images.ImagesDatas;
import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * IHM de gestion des collaborateurs.
 */
public class SelectableCollaboratorPanel extends AbstractTableMgr implements
		IDbStatusListener, ICollaboratorListener {

	/** Logger */
	private static Logger log = Logger
			.getLogger(SelectableCollaboratorPanel.class);

	/** Constantes associées aux colonnes */
	public static final int FIRST_NAME_COLUMN_IDX = 0;
	public static final int LAST_NAME_COLUMN_IDX = 1;
	private static TableOrTreeColumnsMgr tableColsMgr;

	/** Viewer */
	private TableViewer tableViewer;

	/** Composant parent */
	private Composite parent;

	/** Index de la colonne utilisé pour trier les collaborateurs */
	private int sortColumnIndex = LAST_NAME_COLUMN_IDX;

	/** Icone utilisé pour marquer le collaborateur sélectionné */
	private Image selectedItemIcon;

	/** Icone utilisé pour les collaborateurs non sélectionnés */
	private Image unselectedItemIcon;

	/** Liste des listeners */
	private List<ICollaboratorSelectionListener> listeners = new ArrayList<ICollaboratorSelectionListener>();

	/** Collaborateur sélectionné */
	private Collaborator selectedCollaborator;

	private IModelMgr modelMgr;

	/**
	 * Constructeur par défaut.
	 * 
	 * @param parentComposite
	 *            composant parent.
	 * @param modelMgr
	 *            the model manager.
	 */
	public SelectableCollaboratorPanel(Composite parentComposite,
			Object layoutData, IModelMgr modelMgr) {
		parent = parentComposite;
		this.modelMgr = modelMgr;

		// Table
		final Table table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION
				| SWT.BORDER | SWT.HIDE_SELECTION);
		table.setLayoutData(layoutData);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setEnabled(true);
		table.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				Iterator<ICollaboratorSelectionListener> it = listeners
						.iterator();
				while (it.hasNext()) {
					Collaborator lastSelectedCollaborator = selectedCollaborator;
					StructuredSelection selection = (StructuredSelection) tableViewer
							.getSelection();
					if (selection != null && !selection.isEmpty())
						selectedCollaborator = (Collaborator) selection
								.getFirstElement();
					ICollaboratorSelectionListener listener = it.next();
					listener.collaboratorSelected(selectedCollaborator);
					tableViewer.refresh(selectedCollaborator);
					if (lastSelectedCollaborator != null)
						tableViewer.refresh(lastSelectedCollaborator);
				}
			}
		});

		// Création du viewer
		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(this);
		tableViewer.setLabelProvider(this);

		// Configuration des colonnes
		tableColsMgr = new TableOrTreeColumnsMgr();
		tableColsMgr
				.addColumn(
						"FIRST_NAME", Strings.getString("SelectableCollaboratorPanel.columns.FIRST_NAME"), 100, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr
				.addColumn(
						"LAST_NAME", Strings.getString("SelectableCollaboratorPanel.columns.LAST_NAME"), 100, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr.configureTable(tableViewer);

		// Ajout du listener de gestion du tri des colonnes
		// Add sort indicator and sort data when column selected
		Listener sortListener = new Listener() {
			public void handleEvent(Event e) {
				log.debug("handleEvent(" + e + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				TableColumn previousSortColumn = table.getSortColumn();
				TableColumn newSortColumn = (TableColumn) e.widget;
				int dir = table.getSortDirection();
				if (previousSortColumn == newSortColumn) {
					dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
				} else {
					table.setSortColumn(newSortColumn);
					dir = SWT.UP;
				}
				table.setSortDirection(dir);
				sortColumnIndex = Arrays.asList(table.getColumns()).indexOf(
						newSortColumn);
				// Rafraichissement des données
				tableViewer.refresh();
			}
		};
		table.getColumns()[FIRST_NAME_COLUMN_IDX].addListener(SWT.Selection,
				sortListener);
		table.getColumns()[LAST_NAME_COLUMN_IDX].addListener(SWT.Selection,
				sortListener);
		table.setSortColumn(table.getColumns()[sortColumnIndex]);
		table.setSortDirection(SWT.UP);

		// Chargement des icones
		selectedItemIcon = new Image(parentComposite.getDisplay(),
				ImagesDatas.SELECTED_ITEM_ICON);
		unselectedItemIcon = new Image(parentComposite.getDisplay(),
				ImagesDatas.UNSELECTED_ITEM_ICON);

	}

	/**
	 * Retourne le nombre de collaborateurs présentés dans le tableau.
	 * 
	 * @return le nombre de collaborateurs présentés dans le tableau.
	 */
	public int getCollaboratorsCount() {
		return tableViewer.getTable().getItemCount();
	}

	/**
	 * Définit le collaborateur sélectionné.
	 * 
	 * @param idx
	 *            index du collaborateur sélectionné.
	 */
	public void setSelectedIndex(int idx) {
		Collaborator c = (Collaborator) tableViewer.getElementAt(idx);
		setSelectedCollaborator(c);
	}

	/**
	 * Définit le collaborateur sélectionné.
	 * 
	 * @param collaborator
	 *            le collaborateur.
	 */
	public void setSelectedCollaborator(Collaborator collaborator) {
		Collaborator lastSelectedCollaborator = selectedCollaborator;
		selectedCollaborator = collaborator;
		if (collaborator != null) {
			tableViewer.setSelection(new StructuredSelection(
					selectedCollaborator));
			tableViewer.refresh(selectedCollaborator);
		}
		if (lastSelectedCollaborator != null)
			tableViewer.refresh(lastSelectedCollaborator);
	}

	/**
	 * Retourne le collaborateur sélectionné.
	 * 
	 * @return le collaborateur sélectionné.
	 */
	public Collaborator getSelectedCollaborator() {
		return selectedCollaborator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java
	 * .lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		// Chargement des données
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				// Recherche des collaborateurs
				int orderByFieldIndex = -1;
				switch (sortColumnIndex) {
				case FIRST_NAME_COLUMN_IDX:
					orderByFieldIndex = Collaborator.FIRST_NAME_FIELD_IDX;
					break;
				case LAST_NAME_COLUMN_IDX:
				default:
					orderByFieldIndex = Collaborator.LAST_NAME_FIELD_IDX;
					break;
				}
				// Récupération
				return modelMgr.getActiveCollaborators(orderByFieldIndex,
						tableViewer.getTable().getSortDirection() == SWT.UP);
			}
		};
		// Exécution
		Object result = (Object) safeRunner.run(parent.getShell());
		return (Collaborator[]) (result != null ? result
				: new Collaborator[] {});
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
				Collaborator collaborator = (Collaborator) element;
				String text = null;
				switch (columnIndex) {
				case (FIRST_NAME_COLUMN_IDX):
					text = collaborator.getFirstName();
					break;
				case (LAST_NAME_COLUMN_IDX):
					text = collaborator.getLastName();
					break;
				default:
					throw new Error(
							Strings.getString("SelectableCollaboratorPanel.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				return text;
			}
		};
		// Exécution
		return (String) safeRunner.run(parent.getShell(), ""); //$NON-NLS-1$
	}

	public Image getColumnImage(Object element, int columnIndex) {
		Image image = null;
		if (columnIndex == 0) {
			Collaborator collaborator = (Collaborator) element;
			image = collaborator.equals(selectedCollaborator) ? selectedItemIcon
					: unselectedItemIcon;
		}
		return image;
	}

	/**
	 * Initialise l'IHM.
	 */
	public void initialize() {
		// Création d'une racine fictive
		tableViewer.setInput(ROOT_NODE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener#databaseOpened()
	 */
	public void databaseOpened() {
		initialize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener#databaseClosed()
	 */
	public void databaseClosed() {
		Table table = tableViewer.getTable();
		TableItem[] items = table.getItems();
		for (int i = 0; i < items.length; i++) {
			items[i].dispose();
		}
		selectedCollaborator = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.CollaboratorsUI.ICollaboratorListener#
	 * collaboratorAdded(jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorAdded(Collaborator collaborator) {
		tableViewer.refresh();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.CollaboratorsUI.ICollaboratorListener#
	 * collaboratorRemoved(jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorRemoved(Collaborator collaborator) {
		// Si le collaborateur supprimé est celui qui est actuellement
		// sélectionné => on supprime la sélection
		if (collaborator.equals(selectedCollaborator))
			selectedCollaborator = null;
		tableViewer.refresh();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.CollaboratorsUI.ICollaboratorListener#
	 * collaboratorUpdated(jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorUpdated(Collaborator collaborator) {
		tableViewer.refresh(collaborator);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.CollaboratorsUI.ICollaboratorListener#
	 * collaboratorActivationStatusChanged
	 * (jfb.tools.activitymgr.core.beans.Collaborator)
	 */
	public void collaboratorActivationStatusChanged(Collaborator collaborator) {
		// C'est soit comme une suppression ou comme un ajout
		if (collaborator.getIsActive())
			collaboratorAdded(collaborator);
		else
			collaboratorRemoved(collaborator);
	}

	/**
	 * Ajoute un listener de sélection.
	 * 
	 * @param listener
	 *            le nouveau listener.
	 */
	public void addSelectionListener(ICollaboratorSelectionListener listener) {
		listeners.add(listener);
	}

	/**
	 * Supprime un listener de sélection.
	 * 
	 * @param listener
	 *            le listener.
	 */
	public void removeSelectionListener(SelectionListener listener) {
		listeners.remove(listener);
	}

}
