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
package jfb.tools.activitymgr.ui.dialogs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;

import jfb.tools.activitymgr.core.util.Strings;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialogue affichant une erreur avec la pile associ�e.
 */
public class ErrorDialog extends IconAndMessageDialog {

	/** Logger */
	private static Logger log = Logger.getLogger(ErrorDialog.class);

	/** Bouton d'affichage du d�tail */
	private Button detailsButton;

	/** Bool�en indiquant si le d�tail est affich� */
	private boolean detailsShown = false;

	/** Liste contenant la pile d'erreur */
	private List list;

	/** Exception associ�e � l'erreur */
	private Throwable throwable;

	/**
	 * Constructeur par d�faut.
	 * 
	 * @param parentShell
	 *            shell parent.
	 * @param message
	 *            le message d'erreur.
	 * @param throwable
	 *            l'exception.
	 */
	public ErrorDialog(Shell parentShell, String message, Throwable throwable) {
		super(parentShell);
		this.throwable = throwable;
		this.message = message;
	}

	/*
	 * (non-Javadoc) Method declared in Window.
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(Strings.getString("ErrorDialog.texts.TITLE")); //$NON-NLS-1$
		shell.setImage(getImage());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets
	 * .Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite c = (Composite) super.createDialogArea(parent);
		GridLayout layout = (GridLayout) c.getLayout();
		// Changement du Nb de colonnes du Layout
		layout.numColumns = 2;
		createMessageArea(c);
		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse
	 * .swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		detailsButton = createButton(parent, IDialogConstants.DETAILS_ID,
				IDialogConstants.SHOW_DETAILS_LABEL, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case IDialogConstants.DETAILS_ID:
			Point windowSize = getShell().getSize();
			Point oldSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);

			detailsShown = !detailsShown;
			detailsButton
					.setText(detailsShown ? IDialogConstants.HIDE_DETAILS_LABEL
							: IDialogConstants.SHOW_DETAILS_LABEL);
			if (detailsShown) {
				list = new List((Composite) getContents(), SWT.BORDER
						| SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
				GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL
						| GridData.GRAB_HORIZONTAL
						| GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL);
				data.heightHint = list.getItemHeight() * 10;
				data.horizontalSpan = 2;
				list.setLayoutData(data);

				try {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					throwable.printStackTrace(new PrintStream(out));
					out.close();
					ByteArrayInputStream in = new ByteArrayInputStream(
							out.toByteArray());
					LineNumberReader lin = new LineNumberReader(
							new InputStreamReader(in));
					String line = null;
					while ((line = lin.readLine()) != null) {
						list.add(line.replaceFirst("\t", "    ")); //$NON-NLS-1$ //$NON-NLS-2$
					}
				} catch (IOException e) {
					log.error(
							Strings.getString("ErrorDialog.infos.IO_ERROR_WHILE_PRINTING_STACKTRACE"), e); //$NON-NLS-1$
					list.add(Strings
							.getString("ErrorDialog.infos.IO_ERROR_WHILE_PRINTING_STACKTRACE")); //$NON-NLS-1$
					list.add(Strings
							.getString("ErrorDialog.infos.SEE_LOGS_FOR_MORE_DETAILS")); //$NON-NLS-1$
				}
			} else {
				list.dispose();
			}
			Point newSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
			getShell().setSize(
					new Point(windowSize.x, windowSize.y
							+ (newSize.y - oldSize.y)));
			break;
		default:
			super.buttonPressed(buttonId);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IconAndMessageDialog#getImage()
	 */
	protected Image getImage() {
		return getErrorImage();
	}

}
