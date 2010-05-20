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
package jfb.tools.activitymgr.core.beans;

/**
 * Sommes associées à une tache et ses sous-taches.
 */
public class TaskSums {

	/** Somme des budgets */
	private long budgetSum;
	
	/** Somme des consommés initiaux */
	private long initiallyConsumedSum;
	
	/** Somme des reste à faire */
	private long todoSum;
	
	/** Somme des consommés */
	private long consumedSum;

	/** Nombre de contributions */
	private long contributionsNb;
	
	/**
	 * @return la somme des bugets.
	 */
	public long getBudgetSum() {
		return budgetSum;
	}

	/**
	 * Définit la somme des budgets.
	 * @param budgetSum la nouvelle somme.
	 */
	public void setBudgetSum(long budgetSum) {
		this.budgetSum = budgetSum;
	}

	/**
	 * @return la somme des consommés initiaux.
	 */
	public long getInitiallyConsumedSum() {
		return initiallyConsumedSum;
	}

	/**
	 * Définit la somme des consommés initiaux.
	 * @param initiallyConsumedSum la nouvelle somme.
	 */
	public void setInitiallyConsumedSum(long initiallyConsumedSum) {
		this.initiallyConsumedSum = initiallyConsumedSum;
	}

	/**
	 * @return la somme des reste à faire.
	 */
	public long getTodoSum() {
		return todoSum;
	}

	/**
	 * Définit la somme des reste à faire.
	 * @param todoSum la nouvelle somme.
	 */
	public void setTodoSum(long todoSum) {
		this.todoSum = todoSum;
	}

	/**
	 * @return la somme des consommés.
	 */
	public long getConsumedSum() {
		return consumedSum;
	}

	/**
	 * Définit la somme des consommés.
	 * @param consumed la nouvelle somme.
	 */
	public void setConsumedSum(long consumed) {
		this.consumedSum = consumed;
	}

	/**
	 * Retourne le nombre de contributions.
	 * @return le nombre de contributions.
	 */
	public long getContributionsNb() {
		return contributionsNb;
	}

	/**
	 * Définit le nombre de contributions.
	 * @param contributionsNb le nombre de contributions.
	 */
	public void setContributionsNb(long contributionsNb) {
		this.contributionsNb = contributionsNb;
	}

}
