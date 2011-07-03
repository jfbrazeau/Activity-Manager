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
package jfb.tools.activitymgr.ui.util;

import jfb.tools.activitymgr.core.beans.Contribution;
import jfb.tools.activitymgr.core.beans.Task;

/**
 * Contributions associ�es � une tache donn�e sur une semaine.
 */
public class WeekContributions {

	/** La tache associ�e � la liste de contributions */
	private Task task;

	/** La liste des contributions */
	private Contribution[] contributions = new Contribution[7];

	/**
	 * Retourne la contribution du jour de la semaine sp�cifi�.
	 * 
	 * @param day
	 *            le jour de la semaine.
	 * @return la contribution du jour de la semaine sp�cifi�.
	 */
	public Contribution getContribution(int day) {
		return contributions[day];
	}

	/**
	 * D�finit la contribution d'un jour de la semaine.
	 * 
	 * @param day
	 *            le num�ro du jour de la semaine.
	 * @param contribution
	 *            les nouvelles contributions.
	 */
	public void setContribution(int day, Contribution contribution) {
		contributions[day] = contribution;
	}

	/**
	 * @return la tache pour cette contribution sur une semaine.
	 */
	public Task getTask() {
		return task;
	}

	/**
	 * D�finit la tache pour cette contribution sur une semaine.
	 * 
	 * @param task
	 *            la nouvelle tache.
	 */
	public void setTask(Task task) {
		this.task = task;
	}

	/**
	 * @return les contributions de la semaine.
	 */
	public Contribution[] getContributions() {
		return contributions;
	}

	/**
	 * D�finit les contributions de la semaine.
	 * 
	 * @param contributions
	 *            les nouvelles contributions.
	 */
	public void setContributions(Contribution[] contributions) {
		this.contributions = contributions;
	}

}
