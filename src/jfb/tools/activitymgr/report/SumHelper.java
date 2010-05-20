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
package jfb.tools.activitymgr.report;

import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * Classe offrant des services de calcul de sommes
 * pour la génération de rapports avec Velocity.
 */
public class SumHelper {

	/** Logger */
	private static Logger log = Logger.getLogger(SumHelper.class);

	/** Table de stockage des cumuls */
	private HashMap sumMap = new HashMap();
	
	/**
	 * Définit la valeur d'une somme nommée.
	 * @param sumName le nom de la somme.
	 * @param value la valeur de la somme.
	 */
	public void set(String sumName, double value) {
		log.debug("sh.set(" + sumName + ", " + value + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sumMap.put(sumName, new Double(value));
	}

	/**
	 * Retourne la valeur d'une somme nommée.
	 * @param sumName le nom de la somme.
	 * @return la valeur de la somme.
	 */
	public double get(String sumName) {
		log.debug("sh.get(" + sumName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		Double sum = (Double) sumMap.get(sumName);
		double _sum = 0;
		if (sum!=null) {
			_sum = sum.doubleValue();
		}
		return _sum;
	}
	
	/**
	 * Ajoute une valeur à une somme nommée.
	 * @param sumName le nom de la somme.
	 * @param value la valeur à ajouter.
	 */
	public void add(String sumName, double value) {
		log.debug("sh.add(" + sumName + ", " + value + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		double sum = get(sumName);
		set(sumName, sum + value);
	}

	/**
	 * Soustrait une valeur à une somme nommée.
	 * @param sumName le nom de la somme.
	 * @param value la valeur à soustraire.
	 */
	public void sub(String sumName, double value) {
		add(sumName, -value);
	}

}
