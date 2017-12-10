/*
 * Copyright (c) 2004-2017, Jean-Francois Brazeau. All rights reserved.
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

import org.activitymgr.core.orm.annotation.Column;
import org.activitymgr.core.orm.annotation.ColumnNamePrefix;
import org.activitymgr.core.orm.annotation.Table;

/**
 * Durée.
 */
@Table("DURATION")
@ColumnNamePrefix("DUR_")
public class Duration extends SimpleIdentityBean {

	/** Booléen indiquant si la durée est active ou non */
	@Column("IS_ACTIVE")
	private boolean isActive = true;

	/**
	 * Default constructor.
	 * <p>
	 * This constructor is protected as this class is not supposed to be
	 * instantiated directly. One is supposed to use the {@link org.activitymgr.core.IDTOFactory
	 * factory} instead.
	 * </p>
	 * @see IDTOFactory
	 */
	protected Duration() {
	}
	
	/**
	 * Retourne un booléen indiquant si la durée est active ou non.
	 * 
	 * @return un booléen indiquant si la durée est active ou non.
	 */
	public boolean getIsActive() {
		return isActive;
	}

	/**
	 * Définit si la durée est active ou non.
	 * 
	 * @param isActive
	 *            un booléen indiquant si la durée est active ou non.
	 */
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}

}
