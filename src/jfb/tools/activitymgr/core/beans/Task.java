/*
 * Copyright (c) 2004-2010, Jean-Fran�ois Brazeau. All rights reserved.
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

import jfb.tools.activitymgr.core.util.StringHelper;

/**
 * T�che.
 */
public class Task extends SimpleIdentityBean {

	/** Code de la tache */
	private String code;

	/** Nom de la tache */
	private String name;

	/** Chemin de la tache */
	private String path;

	/** Num�ro de la t�che */
	private byte number;

	/** Nombre de taches filles */
	private int subTasksCount;

	/** Budget */
	private long budget;

	/** Consommation initiale */
	private long initiallyConsumed;

	/** Reste � faire */
	private long todo;

	/** Commentaire sur la tache */
	private String comment;

	/**
	 * @return le code de la tache.
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @return le budget allou� � la tache.
	 */
	public long getBudget() {
		return budget;
	}

	/**
	 * @return le consomm� initial de la tache.
	 */
	public long getInitiallyConsumed() {
		return initiallyConsumed;
	}

	/**
	 * @return le nom de la t�che.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return le num�ro de la tache.
	 */
	public byte getNumber() {
		return number;
	}

	/**
	 * @return le chemin de la tache.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return le nombre de t�ches filles.
	 */
	public int getSubTasksCount() {
		return subTasksCount;
	}

	/**
	 * @return le reste � faire associ� � la t�che.
	 */
	public long getTodo() {
		return todo;
	}

	/**
	 * D�finit le code de la t�che.
	 * 
	 * @param code
	 *            le nouveau code.
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * D�finit le budget allou� � la tache
	 * 
	 * @param budget
	 *            le nouveau budget.
	 */
	public void setBudget(long budget) {
		this.budget = budget;
	}

	/**
	 * D�finit le consomm� initial de la tache.
	 * 
	 * @param initiallyConsumed
	 *            le nouveau consomm� initial.
	 */
	public void setInitiallyConsumed(long initiallyConsumed) {
		this.initiallyConsumed = initiallyConsumed;
	}

	/**
	 * D�finit le nom de la tache.
	 * 
	 * @param name
	 *            le nouveau nom.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * D�finit le num�ro de la tache.
	 * 
	 * @param number
	 *            le nouveau num�ro.
	 */
	public void setNumber(byte number) {
		this.number = number;
	}

	/**
	 * D�finit le chemin de la tache.
	 * 
	 * @param path
	 *            le nouveau chemin.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * D�finiti le nombre de taches filles.
	 * 
	 * @param subTasksCount
	 *            le nouveau nombre de taches filles.
	 */
	public void setSubTasksCount(int subTasksCount) {
		this.subTasksCount = subTasksCount;
	}

	/**
	 * D�finit le reste � faire associ� � la tache.
	 * 
	 * @param todo
	 *            le nouveau reste � faire.
	 */
	public void setTodo(long todo) {
		this.todo = todo;
	}

	/**
	 * Construit le chemin complet de la t�che.
	 * 
	 * @return le chemin complet de la t�che.
	 */
	public String getFullPath() {
		StringBuffer result = new StringBuffer(path != null ? path : ""); //$NON-NLS-1$
		result.append(StringHelper.toHex(number));
		return result.toString();
	}

	/**
	 * Retourne le commentaire associ� � la tache.
	 * 
	 * @return le commentaire associ� � la tache.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * D�finit le commentaire de la tache.
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
