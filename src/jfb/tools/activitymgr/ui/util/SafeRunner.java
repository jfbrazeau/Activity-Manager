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
package jfb.tools.activitymgr.ui.util;

import jfb.tools.activitymgr.AbstractApplicationException;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Offre un contexte d'exécution sécurisé.
 * 
 * <p>Si une exception est levée dans le traitement, elle est attrapée
 * et un popup d'erreur est affiché.</p>
 * 
 * <p>Exemple d'utilisation :<br>
 * <pre>
 * // Initialisation du contexte d'exécution sécurisé
 * SafeRunner safeRunner = new SafeRunner() {
 * &nbsp;&nbsp;public Object runUnsafe() throws Exception {
 * &nbsp;&nbsp;&nbsp;&nbsp;// Declare unsafe code...
 * &nbsp;&nbsp;&nbsp;&nbsp;return result;
 * &nbsp;&nbsp;}
 * };
 * // Exécution du traitement
 * Object result = safeRunner.run(parent.getShell(), "");
 * </pre>
 */
public abstract class SafeRunner {

	/** Logger */
	private static Logger log = Logger.getLogger(SafeRunner.class);

	/**
	 * Lance le traitement dans le contexte sécurisé.
	 * @param parentShell shell parent (peut être nul).
	 * @return le résultat du traitement.
	 */
	public Object run(Shell parentShell) {
		return run(parentShell, null);
	}

	/**
	 * Lance le traitement dans le contexte sécurisé.
	 * @param parentShell shell parent (peut être nul).
	 * @param defaultValue la valeur à retourner par défaut.
	 * @return le résultat du traitement.
	 */
	public Object run(Shell parentShell, Object defaultValue) {
		log.debug("ParentShell : " + parentShell);
		Object result = defaultValue;
		// Changement du curseur
		Cursor waitCursor = parentShell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT);
		parentShell.setCursor(waitCursor);
		// Exécution du traitement
		try { result = runUnsafe();	}
		catch (AbstractApplicationException e) {
			log.info("UI Exception", e);
			Shell parent = Display.getCurrent().getActiveShell();
			MessageDialog.openError(parent, "Error", "Unable to complete operation : '" + e.getMessage() + "'");
		}
		catch (Throwable t) {
			log.error("Unexpected error", t);
			Shell parent = Display.getCurrent().getActiveShell();
			MessageDialog.openError(parent, "Error", "An error occured. See logs for more details.");
		}
		finally {
			// Retour du curseur normal
			Cursor normalCursor = parentShell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
			parentShell.setCursor(normalCursor);
		}
		// Retour du résultat
		log.debug(" -> result='" + result + "'");
		return result;
	}

	/**
	 * Traitement potentiellement à risque.
	 * 
	 * <p>Cette méthode doit être implémentée.</p>
	 * 
	 * @return le résultat du traitement.
	 * @throws Exception le traitement peut potentiellement lever n'importe
	 *     quelle exception.
	 */
	protected abstract Object runUnsafe() throws Exception;

}
