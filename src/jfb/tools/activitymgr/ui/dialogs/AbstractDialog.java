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

import jfb.tools.activitymgr.core.util.Strings;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialogue père des dialogues asociés à la page de
 * configuration.
 */
public abstract class AbstractDialog extends Dialog {

	/** Logger */
	private static Logger log = Logger.getLogger(AbstractDialog.class);

	/** Titre du dialogue */
	private String title;
	
	/** Icone du dialogue */
	private Image icon;
	
	/** Valeur initiale */
	private Object initialValue;
	
	/** Valeur validée */
	private Object value;

	/**
	 * Constructeur par défaut.
	 * @param parentShell le shell parent.
	 * @param title titre du dialogue.
	 * @param icon icone du dialogue.
	 * @param initialValue valeur initiale du dialogue.
	 */
	protected AbstractDialog(Shell parentShell, String title, Image icon, Object initialValue) {
		super(parentShell);
		this.title = title;
		this.initialValue = initialValue;
		this.icon = icon;
	}

	/**
	 * Retourne la valeur saisie au travers du dialogue.
	 * @return la valeur saisie au travers du dialogue.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Définit la valeur sélectionnée par le dialogue.
	 * @param value la nouvelle valeur.
	 */
	public void setValue(Object value) {
		this.value = value;
	}
	
	/**
	 * Retourne la valeur initiale du dialogue.
	 * @return la valeur initiale du dialogue.
	 */
	public Object getInitialValue() {
		return initialValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		try {
			log.debug("Ok pressed"); //$NON-NLS-1$
			value = validateUserEntry();
			super.okPressed();
		}
		catch (DialogException e) {
			Control control = e.getControl();
			if (control instanceof Text) {
				Text text = (Text) control;
				text.setFocus();
				text.selectAll();
			}
			// Affichage du message d'erreur
			MessageDialog.openWarning(getShell(), Strings.getString("AbstractDialog.dialog.TITLE"), e.getMessage()); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
	 */
	protected void cancelPressed() {
		log.debug("Cancel pressed"); //$NON-NLS-1$
		super.cancelPressed();
	}

	/**
	 * Valide la saisie de l'utilisateur.
	 * @return la nouvelle valeur du dialogue.
	 * @throws DialogException levé en cas de détection d'anomalie
	 *     dans la saisie de l'utilisateur.
	 */
	protected abstract Object validateUserEntry() throws DialogException;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite c = (Composite) super.createDialogArea(parent);
		Shell shell = c.getShell();
		if (title!=null) shell.setText(title);
		if (icon!=null) shell.setImage(icon);
		
		// Mise à jour du Layout
		GridLayout defaultLayout = (GridLayout) c.getLayout();
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = defaultLayout.marginHeight;
		layout.marginWidth = defaultLayout.marginWidth;
		layout.verticalSpacing = defaultLayout.verticalSpacing;
		layout.horizontalSpacing = defaultLayout.horizontalSpacing;
		c.setLayout(layout);

		// Retour du résultat
		return c;
	}
	
}
