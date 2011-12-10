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
package jfb.tools.activitymgr.core.beans;

/**
 * Filtre de recherche de t�che.
 */
public class TaskSearchFilter {

	/** Index du champ 'nom' de tache */
	public static final int TASK_NAME_FIELD_IDX = 0;
	/** Index du champ 'code' de tache */
	public static final int TASK_CODE_FIELD_IDX = 1;

	/** Index du critere de recherche 'est �gal �' */
	public static final int IS_EQUAL_TO_CRITERIA_IDX = 0;
	/** Index du critere de recherche 'commence par' */
	public static final int STARTS_WITH_CRITERIA_IDX = 1;
	/** Index du critere de recherche 'finit par' */
	public static final int ENDS_WITH_CRITERIA_IDX = 2;
	/** Index du critere de recherche 'contient' */
	public static final int CONTAINS_CRITERIA_IDX = 3;

	/** Champ de la tache utilis� pour effectuer la recherche */
	private int fieldIndex;

	/** Type de crit�re utilis� pour la recherche */
	private int criteriaIndex;

	/** Valeur du champ utilis� pour la recherche */
	private String fieldValue;

	/**
	 * Retourne l'index du type de crit�re utilis� pour la recherche.
	 * 
	 * @return l'index du type de crit�re utilis� pour la recherche.
	 */
	public int getCriteriaIndex() {
		return criteriaIndex;
	}

	/**
	 * D�finit l'index du type de crit�re utilis� pour la recherche.
	 * 
	 * @param criteriaIndex
	 *            l'index du type de crit�re utilis� pour la recherche.
	 */
	public void setCriteriaIndex(int criteriaIndex) {
		this.criteriaIndex = criteriaIndex;
	}

	/**
	 * Retourne l'index du champ de la tache utilis� pour effectuer la recherche
	 * 
	 * @return l'index du champ de la tache utilis� pour effectuer la recherche
	 */
	public int getFieldIndex() {
		return fieldIndex;
	}

	/**
	 * D�finit l'index du champ de la tache utilis� pour effectuer la recherche
	 * 
	 * @param fieldIndex
	 *            l'index du champ de la tache utilis� pour effectuer la
	 *            recherche
	 */
	public void setFieldIndex(int fieldIndex) {
		this.fieldIndex = fieldIndex;
	}

	/**
	 * Retourne la valeur du champ utilis� pour la recherche.
	 * 
	 * @return la valeur du champ utilis� pour la recherche.
	 */
	public String getFieldValue() {
		return fieldValue;
	}

	/**
	 * D�finit la valeur du champ utilis� pour la recherche.
	 * 
	 * @param fieldValue
	 *            la valeur du champ utilis� pour la recherche.
	 */
	public void setFieldValue(String fieldValue) {
		this.fieldValue = fieldValue;
	}

}
