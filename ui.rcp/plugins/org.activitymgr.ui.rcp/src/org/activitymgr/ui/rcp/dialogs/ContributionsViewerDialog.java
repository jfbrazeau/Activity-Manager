/*
 * Copyright (c) 2004-2017, Jean-Francois Brazeau. All rights reserved.
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

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.util.Strings;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialogue permettant d'afficher une liste de contriutions.
 */
public class ContributionsViewerDialog extends AbstractDialog {

	/** Logger */
	private static Logger log = Logger
			.getLogger(ContributionsViewerDialog.class);

	/** Table contenant les données */
	private ContributionsViewerTable contributionsTable;

	/**
	 * Filtre de recherche (stocké dans cet objet en attendant l'invocation du
	 * createDialogArea)
	 */
	private Task task;
	private Collaborator contributor;

	/** Model manager */
	private IModelMgr modelMgr;

	/**
	 * Constructeur par défaut.
	 * 
	 * @param parentShell
	 *            shell parent.
	 * @param modelMgr
	 *            the model manager.
	 */
	public ContributionsViewerDialog(Shell parentShell, IModelMgr modelMgr) {
		super(parentShell, Strings
				.getString("ContributionsViewerDialog.texts.TITLE"), null, null); //$NON-NLS-1$
		setShellStyle(SWT.RESIZE | SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		this.modelMgr = modelMgr;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jfb.tools.activitymgr.ui.util.AbstractDialog#createDialogArea(org.eclipse
	 * .swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		log.debug("createDialogArea"); //$NON-NLS-1$
		Composite c = (Composite) super.createDialogArea(parent);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		contributionsTable = new ContributionsViewerTable(c, gridData, modelMgr);
		contributionsTable.setFilter(task, contributor);
		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jfb.tools.activitymgr.ui.util.AbstractDialog#validateUserEntry()
	 */
	protected Object validateUserEntry() throws DialogException {
		// Ce dialogue n'a pas pour but d'effectuer une saisie.
		return null;
	}

	/**
	 * Initialise le filtre de recherche des contributions.
	 * 
	 * @param task
	 *            la tache.
	 * @param contributor
	 *            le collaborateur.
	 */
	public void setFilter(Task task, Collaborator contributor) {
		this.task = task;
		this.contributor = contributor;
	}

}
