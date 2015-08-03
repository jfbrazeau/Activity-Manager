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
package org.activitymgr.core.dto.misc;

import org.activitymgr.core.dto.Task;

/**
 * Sommes associées à une tache et ses sous-taches.
 */
public class TaskSums {
	
	/** The task */
	private Task task;

	/** Somme des budgets */
	private long budgetSum;

	/** Somme des consommés initiaux */
	private long initiallyConsumedSum;

	/** Somme des reste à faire */
	private long todoSum;
	
	/** Tells wether this task is a leaf task */
	private boolean isLeaf;

	/** Task contributions sums */
	private TaskContributionsSums contributionsSums;

	/**
	 * @return the task.
	 */
	public Task getTask() {
		return task;
	}
	
	/**
	 * Sets the task.
	 * @param task the new task.
	 */
	public void setTask(Task task) {
		this.task = task;
	}
	
	/**
	 * @return la somme des bugets.
	 */
	public long getBudgetSum() {
		return budgetSum;
	}

	/**
	 * Définit la somme des budgets.
	 * 
	 * @param budgetSum
	 *            la nouvelle somme.
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
	 * 
	 * @param initiallyConsumedSum
	 *            la nouvelle somme.
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
	 * 
	 * @param todoSum
	 *            la nouvelle somme.
	 */
	public void setTodoSum(long todoSum) {
		this.todoSum = todoSum;
	}

	/**
	 * @return the contributions sums.
	 */
	public TaskContributionsSums getContributionsSums() {
		return contributionsSums;
	}
	
	/**
	 * @param contributionsSums the new contributions sums.
	 */
	public void setContributionsSums(TaskContributionsSums contributionsSums) {
		this.contributionsSums = contributionsSums;
	}

	/**
	 * @param isLeaf <code>true</code> if this task is a leaf task.
	 */
	public void setLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
	}
	
	/**
	 * @return <code>true</code> if this task is a leaf task.
	 */
	public boolean isLeaf() {
		return isLeaf;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean equals = false;
		if (obj instanceof TaskSums) {
			equals = getTask().equals(((TaskSums) obj).getTask());
		}
		return equals;
	}

	@Override
	public String toString() {
		return getTask().toString();
	}
}
