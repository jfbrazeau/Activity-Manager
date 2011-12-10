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
package jfb.tools.activitymgr.ui.util;

import jfb.tools.activitymgr.AbstractException;
import jfb.tools.activitymgr.core.util.Strings;
import jfb.tools.activitymgr.ui.dialogs.ErrorDialog;

import org.apache.log4j.Logger;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;

/**
 * Offre un contexte d'ex�cution s�curis�.
 * 
 * <p>
 * Si une exception est lev�e dans le traitement, elle est attrap�e et un popup
 * d'erreur est affich�.
 * </p>
 * 
 * <p>
 * Exemple d'utilisation :<br>
 * 
 * <pre>
 * // Initialisation du contexte d'ex�cution s�curis�
 * SafeRunner safeRunner = new SafeRunner() {
 * 	public Object runUnsafe() throws Exception {
 * 		// Declare unsafe code...
 * 		return result;
 * 	}
 * };
 * // Ex�cution du traitement
 * Object result = safeRunner.run(parent.getShell(), &quot;&quot;);
 * </pre>
 */
public abstract class SafeRunner {

	/** Logger */
	private static Logger log = Logger.getLogger(SafeRunner.class);

	/**
	 * Classe permettant de stoker le r�sultat du traitement. (sans cet objet il
	 * n'est pas possible de r�cup�rer le r�sultat dans le traitement ex�cut�
	 * dans le Runnable puisqu'il faut passer par une r�f�rence finale).
	 */
	private static class Result {
		public Object value;
	}

	/**
	 * Lance le traitement dans le contexte s�curis�.
	 * 
	 * @param parentShell
	 *            shell parent (peut �tre nul).
	 * @return le r�sultat du traitement.
	 */
	public Object run(Shell parentShell) {
		return run(parentShell, null);
	}

	/**
	 * Lance le traitement dans le contexte s�curis�.
	 * 
	 * @param parentShell
	 *            shell parent (peut �tre nul).
	 * @param defaultValue
	 *            la valeur � retourner par d�faut.
	 * @return le r�sultat du traitement.
	 */
	public Object run(final Shell parentShell, Object defaultValue) {
		log.debug("ParentShell : " + parentShell); //$NON-NLS-1$
		final Result result = new Result();
		result.value = defaultValue;
		// Ex�cution du traitement
		BusyIndicator.showWhile(parentShell.getDisplay(), new Runnable() {
			public void run() {
				try {
					result.value = runUnsafe();
				} catch (AbstractException e) {
					log.info("UI Exception", e); //$NON-NLS-1$
					new ErrorDialog(
							parentShell,
							Strings.getString(
									"SafeRunner.errors.UNABLE_TO_COMPLETE_OPERATION", e.getMessage()), e).open(); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (Throwable t) {
					log.error("Unexpected error", t); //$NON-NLS-1$
					new ErrorDialog(parentShell, Strings
							.getString("SafeRunner.errors.UNEXPECTED_ERROR"), t).open(); //$NON-NLS-1$
				}
			}
		});
		// Retour du r�sultat
		log.debug(" -> result='" + result.value + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		return result.value;
	}

	/**
	 * Traitement potentiellement � risque.
	 * 
	 * <p>
	 * Cette m�thode doit �tre impl�ment�e.
	 * </p>
	 * 
	 * @return le r�sultat du traitement.
	 * @throws Exception
	 *             le traitement peut potentiellement lever n'importe quelle
	 *             exception.
	 */
	protected abstract Object runUnsafe() throws Exception;

}
