/*
 * Copyright (c) 2004-2010, Jean-François Brazeau. All rights reserved.
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Duration;
import jfb.tools.activitymgr.core.util.StringFormatException;
import jfb.tools.activitymgr.core.util.StringHelper;
import jfb.tools.activitymgr.core.util.Strings;
import jfb.tools.activitymgr.ui.DatabaseUI.IDbStatusListener;
import jfb.tools.activitymgr.ui.images.ImagesDatas;
import jfb.tools.activitymgr.ui.util.AbstractTableMgr;
import jfb.tools.activitymgr.ui.util.SWTHelper;
import jfb.tools.activitymgr.ui.util.SafeRunner;
import jfb.tools.activitymgr.ui.util.TableOrTreeColumnsMgr;
import jfb.tools.activitymgr.ui.util.UITechException;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * IHM de gestion des durées.
 */
public class DurationsUI extends AbstractTableMgr implements IDbStatusListener, ICellModifier, SelectionListener, MenuListener {

	/** Logger */
	private static Logger log = Logger.getLogger(DurationsUI.class);

	/** Constantes associées aux colonnes */
	public static final int IS_ACTIVE_COLUMN_IDX =  0;
	public static final int DURATION_COLUMN_IDX = 1;
	private static TableOrTreeColumnsMgr tableColsMgr;

	/**
	 * Interface utilisée pour permettre l'écoute de la suppression ou de
	 * l'ajout de durées.
	 */
	public static interface IDurationListener {
		
		/**
		 * Indique qu'une durée a été ajoutée au référentiel.
		 * @param duration la durée ajoutée.
		 */
		public void durationAdded(Duration duration);
		
		/**
		 * Indique qu'une durée a été supprimée du référentiel.
		 * @param duration la durée supprimée.
		 */
		public void durationRemoved(Duration duration);

		/**
		 * Indique qu'une durée a été modifiée dans le référentiel.
		 * @param oldDuration la durée modifiée.
		 * @param newDuration la nouvelle durée.
		 */
		public void durationUpdated(Duration oldDuration, Duration newDuration);

		/**
		 * Indique que l'état d'activation d'une durée a été désactivée dans le référentiel.
		 * @param duration la durée modifiée.
		 */
		public void durationActivationStatusChanged(Duration duration);
	}
	
	/** Viewer */
	private TableViewer tableViewer;

	/** Items de menu */
	private MenuItem newItem;
	private MenuItem removeItem;
	private MenuItem exportItem;
	
	/** Composant parent */
	private Composite parent;

	/** Listeners */
	private ArrayList listeners = new ArrayList();

	/** Icone utilisé pour marquer les durées actifs */
	private Image checkedIcon;
	
	/** Icone utilisé pour les durées non actifs */
	private Image uncheckedIcon;
	
	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 * @param tabItem item parent.
	 */
	public DurationsUI(TabItem tabItem) {
		this(tabItem.getParent());
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par défaut.
	 * @param parentComposite composant parent.
	 */
	public DurationsUI(Composite parentComposite) {
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
		table.setToolTipText(Strings.getString("DurationsUI.table.TOOL_TIP")); //$NON-NLS-1$

		// Création du viewer
		tableViewer = new TableViewer(table);
		tableViewer.setCellModifier(this);
		tableViewer.setContentProvider(this);
		tableViewer.setLabelProvider(this);

		// Configuration des colonnes
		tableColsMgr = new TableOrTreeColumnsMgr();
		tableColsMgr.addColumn("IS_ACTIVE", "!", 20, SWT.CENTER); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr.addColumn("DURATION", Strings.getString("DurationsUI.columns.DURATION"), 100, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr.configureTable(tableViewer);

		// Configuration des éditeurs de cellules
		CellEditor[] editors = new CellEditor[9];
		editors[IS_ACTIVE_COLUMN_IDX] = new CheckboxCellEditor(table);
		editors[DURATION_COLUMN_IDX] = new TextCellEditor(table);
		tableViewer.setCellEditors(editors);

		// Configuration du menu popup
		final Menu menu = new Menu(table);
		menu.addMenuListener(this);
		newItem = new MenuItem(menu, SWT.CASCADE);
		newItem.setText(Strings.getString("DurationsUI.7")); //$NON-NLS-1$
		newItem.addSelectionListener(this);
		removeItem = new MenuItem(menu, SWT.CASCADE);
		removeItem.setText(Strings.getString("DurationsUI.menuitems.REMOVE")); //$NON-NLS-1$
		removeItem.addSelectionListener(this);
		exportItem = new MenuItem(menu, SWT.CASCADE);
		exportItem.setText(Strings.getString("DurationsUI.menuitems.EXPORT")); //$NON-NLS-1$
		exportItem.addSelectionListener(this);
		table.setMenu(menu);
		
		// Chargement des icones
		checkedIcon = new Image(parentComposite.getDisplay(), ImagesDatas.CHECKED_ICON);
		uncheckedIcon = new Image(parentComposite.getDisplay(), ImagesDatas.UNCHECKED_ICON);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		// Chargement des données
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				return ModelMgr.getDurations();
			}
		};
		// Exécution
		return (Object[]) safeRunner.run(parent.getShell());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
	 */
	public boolean canModify(Object element, String property) {
		log.debug("ICellModifier.canModify(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
	 */
	public Object getValue(final Object element, final String property) {
		log.debug("ICellModifier.getValue(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				Duration duration = (Duration) element;
				int columnIndex = tableColsMgr.getColumnIndex(property);
				Object value = null;
				switch (columnIndex) {
					case IS_ACTIVE_COLUMN_IDX :
						value = duration.getIsActive() ? Boolean.TRUE : Boolean.FALSE;
						break;
					case (DURATION_COLUMN_IDX) :
						value = String.valueOf(new BigDecimal(duration.getId()).movePointLeft(2));
						break;
					default : throw new Error(Strings.getString("DurationsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				return value;
			}
		};
		// Exécution
		return safeRunner.run(parent.getShell(), ""); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	public void modify(final Object element, String property, final Object value) {
		log.debug("ICellModifier.modify(" + element + ", " + property + ", " + value + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		TableItem item = (TableItem) element;
		final Duration duration = (Duration) item.getData();
		final IBaseLabelProvider labelProvider = this;
		final int columnIndex = tableColsMgr.getColumnIndex(property);
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				// Création d'un clone dans le cas ou il s'agît 
				// d'une modification de la valeur de la durée
				Duration oldDuration = new Duration();
				oldDuration.setId(duration.getId());
				oldDuration.setIsActive(duration.getIsActive());
				// Booléens indiquant quelles notifications doivent être émises
				boolean mustNotifyUpdateEvent = false;
				boolean mustNotifyActivationStatusChangeEvent = false;
				switch (columnIndex) {
					case (IS_ACTIVE_COLUMN_IDX) :
						Boolean isActive = (Boolean) value;
						duration.setIsActive(isActive.booleanValue());
						ModelMgr.updateDuration(duration);
						mustNotifyActivationStatusChangeEvent = true;
						break;
					case (DURATION_COLUMN_IDX) :
						// Mise à jour en base
						Duration newDuration = new Duration();
						newDuration.setId(StringHelper.entryToHundredth((String) value));
						newDuration.setIsActive(duration.getIsActive());
						newDuration = ModelMgr.updateDuration(oldDuration, newDuration);
						// Mise à jour dans le modèle
						duration.setId(newDuration.getId());
						mustNotifyUpdateEvent = true;
						// Tri des données
						sortDurations();
						break;
					default : throw new UITechException(Strings.getString("DurationsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				// Notification des listeners
				notifyLabelProviderListener(new LabelProviderChangedEvent(labelProvider, duration));
				if (mustNotifyUpdateEvent)
					notifyDurationUpdated(oldDuration, duration);
				if (mustNotifyActivationStatusChangeEvent)
					notifyDurationActivationStatusChanged(duration);
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
		log.debug("ITableLabelProvider.getColumnText(" + element + ", " + columnIndex + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				Duration duration = (Duration) element;
				String text = null;
				switch (columnIndex) {
					case IS_ACTIVE_COLUMN_IDX :
						text = ""; // colonne indiquant si la durée est active ou non //$NON-NLS-1$
						break;
					case (DURATION_COLUMN_IDX) :
						text = String.valueOf(new BigDecimal(duration.getId()).movePointLeft(2));
						break;
					default : throw new Error(Strings.getString("DurationsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				return text;
			}
		};
		// Exécution
		return (String) safeRunner.run(parent.getShell(), ""); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.util.AbstractTableMgr#getColumnImage(java.lang.Object, int)
	 */
	public Image getColumnImage(final Object element, final int columnIndex) {
		log.debug("ITableLabelProvider.getColumnImage(" + element + ", " + columnIndex + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				Duration duration = (Duration) element;
				Image image = null;
				switch (columnIndex) {
					case (IS_ACTIVE_COLUMN_IDX) :
						image = duration.getIsActive() ? checkedIcon : uncheckedIcon;
						break;
					case (DURATION_COLUMN_IDX) :
						image = null;
						break;
					default : throw new Error(Strings.getString("DurationsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				return image;
			}
		};
		// Exécution
		return (Image) safeRunner.run(parent.getShell());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetSelected(final SelectionEvent e) {
		log.debug("SelectionListener.widgetSelected(" + e + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		final Object source = e.getSource();
		SafeRunner safeRunner = new SafeRunner() {
			public Object runUnsafe() throws Exception {
				//TableItem[] selection = tableViewer.getTable().getSelection();
				// Cas d'une création
				if (newItem.equals(source)) {
					InputDialog dialog = new InputDialog(
						parent.getShell(), 
						Strings.getString("DurationsUI.titles.INPUT_DIALOG"),  //$NON-NLS-1$
						Strings.getString("DurationsUI.questions.NEW_DURATION"),  //$NON-NLS-1$
						"0",  //$NON-NLS-1$
						new IInputValidator() {
							public String isValid(String newText) {
								String errorMsg = null;
								Duration duration = new Duration();
								try { 
									// Parsing de la saisie et contrôle du format
									duration.setId(StringHelper.entryToHundredth(newText));
									// Vérification de la non existence de la durée
									if (ModelMgr.durationExists(duration))
										errorMsg = Strings.getString("DurationsUI.errors.DURATION_ALREADY_EXIST"); //$NON-NLS-1$
								}
								catch (StringFormatException e) {
									errorMsg = e.getMessage();
								}
								catch (DbException e) {
									errorMsg = Strings.getString("DurationsUI.errors.DATABASE_CONNECTION_FAILURE_WHILE_DURATION_EXISTENCE_CHECK"); //$NON-NLS-1$
								}
								// Retour du résultat
								return errorMsg;
							}
						});
					// Ouverture du dialogue
					if (dialog.open()==Dialog.OK) {
						Duration newDuration = new Duration();
						newDuration.setId(StringHelper.entryToHundredth(dialog.getValue()));
						ModelMgr.createDuration(newDuration);
						newLine(newDuration);
						// Notification des listeners
						notifyDurationAdded(newDuration);
						// Tri des données
						sortDurations();
					}
				}
				// Cas d'une suppression
				else if (removeItem.equals(source)) {
					TableItem[] items = tableViewer.getTable().getSelection();
					for (int i=0; i<items.length; i++) {
						TableItem item = items[i];
						Duration duration = (Duration) item.getData();
						ModelMgr.removeDuration(duration);
						item.dispose();
						// Notification des listeners
						notifyDurationRemoved(duration);
					}
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
	 * @param duration la durée associée à la nouvelle ligne.
	 */
	private void newLine(Duration duration) {
		// Ajout dans l'arbre
		tableViewer.add(duration);
		tableViewer.setSelection(new StructuredSelection(duration), true);
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
		log.debug("menuShown(" + e + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		TableItem[] selection = tableViewer.getTable().getSelection();
		boolean emptySelection = selection.length==0;
		boolean singleSelection = selection.length==1;
		newItem.setEnabled(emptySelection || singleSelection);
		removeItem.setEnabled(!emptySelection);
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
	public void addDurationListener(IDurationListener listener) {
		listeners.add(listener);
	}

	/**
	 * Ajoute un listener.
	 * @param listener le nouveau listener.
	 */
	public void removeDurationListener(IDurationListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Notifie les listeners qu'une durée a été ajoutée.
	 * @param newDuration la durée ajoutée.
	 */
	private void notifyDurationAdded(Duration newDuration) {
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IDurationListener listener = (IDurationListener) it.next();
			listener.durationAdded(newDuration);
		}
	}

	/**
	 * Notifie les listeners qu'une durée a été supprimée.
	 * @param duration la durée supprimée.
	 */
	private void notifyDurationRemoved(Duration duration) {
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IDurationListener listener = (IDurationListener) it.next();
			listener.durationRemoved(duration);
		}
	}

	/**
	 * Notifie les listeners qu'un collaborateur a été modifié.
	 * @param oldDuration la durée modifiée.
	 * @param newDuration la nouvelle durée.
	 */
	private void notifyDurationUpdated(Duration oldDuration, Duration newDuration) {
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IDurationListener listener = (IDurationListener) it.next();
			listener.durationUpdated(oldDuration, newDuration);
		}
	}

	/**
	 * Notifie les listeners que l'état d'activation d'une durée a été modifiée.
	 * @param duration la durée modifiée.
	 */
	private void notifyDurationActivationStatusChanged(Duration duration) {
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IDurationListener listener = (IDurationListener) it.next();
			listener.durationActivationStatusChanged(duration);
		}
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.DatabaseUI.DbStatusListener#databaseOpened()
	 */
	public void databaseOpened() {
		initUI();
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

	/**
	 * Trie les durées.
	 */
	private void sortDurations() {
		// Réinitialisation des donées
		initUI();
	}

	/**
	 * Initialise l'IHM avec les données en base.
	 */
	private void initUI() {
		// Initialisation de la table
		tableViewer.setInput(ROOT_NODE);
	}

}
