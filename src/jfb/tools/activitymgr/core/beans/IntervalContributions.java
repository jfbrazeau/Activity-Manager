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

import java.util.Calendar;

/**
 * Contains the contributions for a given task list and a date interval.
 * @author jbrazeau
 */
public class IntervalContributions {
	
	/** Start date of the interval */
	private Calendar fromDate;
	
	/** End date of the interval */
	private Calendar toDate;
	
	/** The task contributions */
	private TaskContributions[] taskContributions;

	/**
	 * @return the start date of the interval
	 */
	public Calendar getFromDate() {
		return fromDate;
	}

	/**
	 * @param fromDate the start date of the interval
	 */
	public void setFromDate(Calendar fromDate) {
		this.fromDate = fromDate;
	}

	/**
	 * @return the end date of the interval
	 */
	public Calendar getToDate() {
		return toDate;
	}

	/**
	 * @param toDate the end date of the interval
	 */
	public void setToDate(Calendar toDate) {
		this.toDate = toDate;
	}

	/**
	 * @return the taskContributions
	 */
	public TaskContributions[] getTaskContributions() {
		return taskContributions;
	}

	/**
	 * @param taskContributions the taskContributions to set
	 */
	public void setTaskContributions(TaskContributions[] taskContributions) {
		this.taskContributions = taskContributions;
	}
	

}

