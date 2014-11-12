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
package org.activitymgr.core.beans;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.activitymgr.core.orm.annotation.Column;
import org.activitymgr.core.orm.annotation.ColumnNamePrefix;
import org.activitymgr.core.orm.annotation.PrimaryKey;

/**
 * Contribution d'un collaborateur à une tache.
 */
@ColumnNamePrefix("CTB_")
public class Contribution {
	
	/** Année */
	@PrimaryKey
	private int year;

	/** Mois */
	@PrimaryKey
	private int month;

	/** Jour */
	@PrimaryKey
	private int day;

	/** Identifiant du collaborateur */
	@PrimaryKey
	@Column("CONTRIBUTOR")
	private long contributorId;

	/** Identifiant de la tache */
	@PrimaryKey
	@Column("TASK")
	private long taskId;

	/** Durée */
	@Column("DURATION")
	private long durationId;

	/**
	 * @return l'identifiant du collaborateur.
	 */
	public long getContributorId() {
		return contributorId;
	}

	/**
	 * Définit l'identifiant du collaborateur.
	 * 
	 * @param contributorId
	 *            le nouvel identifiant.
	 */
	public void setContributorId(long contributorId) {
		this.contributorId = contributorId;
	}

	/**
	 * @return la durée.
	 */
	public long getDurationId() {
		return durationId;
	}

	/**
	 * Définit la durée.
	 * 
	 * @param duration
	 *            la nouvelle durée.
	 */
	public void setDurationId(long duration) {
		this.durationId = duration;
	}

	/**
	 * @return l'identifiant de la tache.
	 */
	public long getTaskId() {
		return taskId;
	}

	/**
	 * Définit l'identifiant de la tache.
	 * 
	 * @param taskId
	 *            le nouvel identifiant.
	 */
	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	/**
	 * @return le jour de la contribution.
	 */
	public int getDay() {
		return day;
	}

	/**
	 * Définit le jour de la contribution.
	 * 
	 * @param day
	 *            le nouveau jour.
	 */
	public void setDay(int day) {
		this.day = day;
	}

	/**
	 * @return le mois de la contribution.
	 */
	public int getMonth() {
		return month;
	}

	/**
	 * Définit le mois de la contribution.
	 * 
	 * @param month
	 *            le nouveau mois.
	 */
	public void setMonth(int month) {
		this.month = month;
	}

	/**
	 * @return l'année de la contribution.
	 */
	public int getYear() {
		return year;
	}

	/**
	 * Définit l'année de la contribution.
	 * 
	 * @param year
	 *            la nouvelle année.
	 */
	public void setYear(int year) {
		this.year = year;
	}

	/**
	 * Définit la date associée à la contribution.
	 * 
	 * @param date
	 *            la nouvelle date.
	 */
	public void setDate(Calendar date) {
		setYear(date.get(Calendar.YEAR));
		setMonth(date.get(Calendar.MONTH) + 1);
		setDay(date.get(Calendar.DAY_OF_MONTH));
	}

	/**
	 * @return la date associée à la contribution.
	 */
	public Calendar getDate() {
		GregorianCalendar cal = new GregorianCalendar(year, month - 1, day);
		return cal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		boolean equals = false;
		if (obj != null && obj instanceof Contribution) {
			Contribution other = (Contribution) obj;
			equals = (other.year == year);
			equals &= (other.month == month);
			equals &= (other.day == day);
			equals &= (other.contributorId == contributorId);
			equals &= (other.taskId == taskId);
		}
		return equals;
	}

}
