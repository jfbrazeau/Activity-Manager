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
package jfb.tools.activitymgr.report;

import org.apache.log4j.Logger;

/**
 * Classe offrant des services de comparaison et de tests pour la g�n�ration de
 * rapports avec Velocity.
 */
public class TestHelper {

	/** Logger */
	private static Logger log = Logger.getLogger(TestHelper.class);

	/**
	 * Teste si la valeur1 est sup�rieure stricte � la valeur2.
	 * 
	 * @param val1
	 *            valeur 1.
	 * @param val2
	 *            valeur 2.
	 * @return un bool�en indiquant si val1 &gt; val2.
	 */
	public boolean gt(double val1, double val2) {
		log.debug("gt(" + val1 + ", " + val2 + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return val1 > val2;
	}

	/**
	 * Teste si la valeur1 est �gale � la valeur2.
	 * 
	 * @param val1
	 *            valeur 1.
	 * @param val2
	 *            valeur 2.
	 * @return un bool�en indiquant si val1 == val2.
	 */
	public boolean eq(double val1, double val2) {
		log.debug("gt(" + val1 + ", " + val2 + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return val1 == val2;
	}
}
