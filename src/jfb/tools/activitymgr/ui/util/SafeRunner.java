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
package jfb.tools.activitymgr.ui.util;

import jfb.tools.activitymgr.AbstractException;
import jfb.tools.activitymgr.core.util.Strings;
import jfb.tools.activitymgr.ui.dialogs.ErrorDialog;

import org.apache.log4j.Logger;
import org.eclipse.swt.custom.BusyIndicator;
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
	 * Classe permettant de stoker le résultat du traitement.
	 * (sans cet objet il n'est pas possible de récupérer le résultat
	 * dans le traitement exécuté dans le Runnable puisqu'il faut
	 * passer par une référence finale).
	 */
	private static class Result {
		public Object value;
	}
	
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
// TODO Supprimer ce bout de code si il n'y a pas de pb
//	public Object run(Shell parentShell, Object defaultValue) {
//		log.debug("ParentShell : " + parentShell);
//		Object result = defaultValue;
//		// Changement du curseur
//		Cursor waitCursor = parentShell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT);
//		parentShell.setCursor(waitCursor);
//		// Exécution du traitement
//		try { result = runUnsafe(); }
//		catch (AbstractException e) {
//			log.info("UI Exception", e);
//			Shell parent = Display.getCurrent().getActiveShell();
//			new ErrorDialog(parent, "Unable to complete operation : '" + e.getMessage() + "'", e).open();
//		}
//		catch (Throwable t) {
//			log.error("Unexpected error", t);
//			Shell parent = Display.getCurrent().getActiveShell();
//			new ErrorDialog(parent, "Unexpected error", t).open();
//		}
//		finally {
//			// Retour du curseur normal
//			Cursor normalCursor = parentShell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
//			parentShell.setCursor(normalCursor);
//		}
//		// Retour du résultat
//		log.debug(" -> result='" + result + "'");
//		return result;
//	}
	public Object run(final Shell parentShell, Object defaultValue) {
		log.debug("ParentShell : " + parentShell); //$NON-NLS-1$
		final Result result = new Result();
		result.value = defaultValue;
		// Exécution du traitement
		BusyIndicator.showWhile(parentShell.getDisplay(), new Runnable() {
			public void run() {
				try { result.value = runUnsafe(); }
				catch (AbstractException e) {
					log.info("UI Exception", e); //$NON-NLS-1$
					new ErrorDialog(parentShell, Strings.getString("SafeRunner.errors.UNABLE_TO_COMPLETE_OPERATION", e.getMessage()), e).open(); //$NON-NLS-1$ //$NON-NLS-2$
				}
				catch (Throwable t) {
					log.error("Unexpected error", t); //$NON-NLS-1$
					new ErrorDialog(parentShell, Strings.getString("SafeRunner.errors.UNEXPECTED_ERROR"), t).open(); //$NON-NLS-1$
				}
			}
		});
		// Retour du résultat
		log.debug(" -> result='" + result.value + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		return result.value;
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
