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
package org.activitymgr.core.dto;

import org.activitymgr.core.dto.converters.TaskNumberConverter;
import org.activitymgr.core.orm.annotation.Column;
import org.activitymgr.core.orm.annotation.ColumnNamePrefix;
import org.activitymgr.core.orm.annotation.Converter;
import org.activitymgr.core.orm.annotation.Table;
import org.activitymgr.core.util.StringHelper;

/**
 * Tâche.
 */
@Table("TASK")
@ColumnNamePrefix("TSK_")
public class Task extends SimpleIdentityBean {

	/** Chemin de la tache */
	private String path;

	/** Numéro de la tâche */
	@Converter(TaskNumberConverter.class)
	private byte number;

	/** Code de la tache */
	private String code;

	/** Nom de la tache */
	private String name;

	/** Budget */
	private long budget;

	/** Consommation initiale */
	@Column("INITIAL_CONS")
	private long initiallyConsumed;

	/** Reste à faire */
	private long todo;

	/** Commentaire sur la tache */
	private String comment;

	/**
	 * Default constructor.
	 * <p>
	 * This constructor is protected as this class is not supposed to be
	 * instantiated directly. One is supposed to use the {@link org.activitymgr.core.IDTOFactory
	 * factory} instead.
	 * </p>
	 * @see IDTOFactory
	 */
	protected Task() {
	}

	/**
	 * @return le code de la tache.
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @return le budget alloué à la tache.
	 */
	public long getBudget() {
		return budget;
	}

	/**
	 * @return le consommé initial de la tache.
	 */
	public long getInitiallyConsumed() {
		return initiallyConsumed;
	}

	/**
	 * @return le nom de la tâche.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return le numéro de la tache.
	 */
	public byte getNumber() {
		return number;
	}

	/**
	 * @return le numéro de la tache.
	 */
	public String getNumberAsHex() {
		return StringHelper.toHex(number);
	}

	/**
	 * @return le chemin de la tache.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return le reste à faire associé à la tâche.
	 */
	public long getTodo() {
		return todo;
	}

	/**
	 * Définit le code de la tâche.
	 * 
	 * @param code
	 *            le nouveau code.
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Définit le budget alloué à la tache
	 * 
	 * @param budget
	 *            le nouveau budget.
	 */
	public void setBudget(long budget) {
		this.budget = budget;
	}

	/**
	 * Définit le consommé initial de la tache.
	 * 
	 * @param initiallyConsumed
	 *            le nouveau consommé initial.
	 */
	public void setInitiallyConsumed(long initiallyConsumed) {
		this.initiallyConsumed = initiallyConsumed;
	}

	/**
	 * Définit le nom de la tache.
	 * 
	 * @param name
	 *            le nouveau nom.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Définit le numéro de la tache.
	 * 
	 * @param number
	 *            le nouveau numéro.
	 */
	public void setNumber(byte number) {
		this.number = number;
	}

	/**
	 * Définit le chemin de la tache.
	 * 
	 * @param path
	 *            le nouveau chemin.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Définit le reste à faire associé à la tache.
	 * 
	 * @param todo
	 *            le nouveau reste à faire.
	 */
	public void setTodo(long todo) {
		this.todo = todo;
	}

	/**
	 * Construit le chemin complet de la tâche.
	 * 
	 * @return le chemin complet de la tâche.
	 */
	public String getFullPath() {
		StringBuffer result = new StringBuffer(path != null ? path : ""); //$NON-NLS-1$
		result.append(getNumberAsHex());
		return result.toString();
	}

	/**
	 * Retourne le commentaire associé à la tache.
	 * 
	 * @return le commentaire associé à la tache.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Définit le commentaire de la tache.
	 * 
	 * @param comment
	 *            le nouveau commentaire.
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new StringBuffer("Task(") //$NON-NLS-1$
				.append(getId()).append(", ") //$NON-NLS-1$
				.append(code).append(", ") //$NON-NLS-1$
				.append(name).append(",") //$NON-NLS-1$
				.append(path).append(",") //$NON-NLS-1$
				.append(number).append(",") //$NON-NLS-1$
				.append(getFullPath()).append(")") //$NON-NLS-1$
				.toString();
	}

}
