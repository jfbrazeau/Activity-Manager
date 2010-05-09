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
package jfb.tools.activitymgr.ui.dialogs;

import jfb.tools.activitymgr.core.beans.Task;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;

public class TasksChooserDialog extends AbstractDialog implements MouseListener {

	/** Logger */
	private static Logger log = Logger.getLogger(TasksChooserDialog.class);

	/** Arbre contenant la liste des tâches */
	private TasksChooserTree tasksTree;

	/** Valideur */
	private ITaskChooserValidator validator;
	
	/**
	 * Constructeur par défaut.
	 * @param parentShell shell parent.
	 */
	public TasksChooserDialog(Shell parentShell) {
		super(parentShell, "Choose a task", null, null);
		setShellStyle(SWT.RESIZE | SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.util.AbstractDialog#validateUserEntry()
	 */
	protected Object validateUserEntry() throws DialogException {
		log.debug("validateUserEntry");
		TreeItem[] selection = tasksTree.getTreeViewer().getTree().getSelection();
		Task selectedTask = null;
		if (selection.length>0)
			selectedTask = (Task) selection[0].getData();
		log.debug("Selected task = " + selectedTask);
		if (selectedTask==null)
			throw new DialogException("Please choose a task", null);
		if (validator!=null)
			validator.validateChoosenTask(selectedTask);
		// Validation du choix de la tache
		return selectedTask;
	}
	
	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.util.AbstractDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite c = (Composite) super.createDialogArea(parent);
		tasksTree = new TasksChooserTree(c);
		TreeViewer viewer = tasksTree.getTreeViewer();
		viewer.getTree().addMouseListener(this);
		Task lastValue = (Task) getValue();
		if (lastValue!=null) {
			viewer.setSelection(new StructuredSelection(lastValue));
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseDoubleClick(MouseEvent e) {
		okPressed();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseDown(MouseEvent e) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseUp(MouseEvent e) {
	}

	/**
	 * @param validator le nouveau valideur.
	 */
	public void setValidator(ITaskChooserValidator validator) {
		this.validator = validator;
	}


	
}
