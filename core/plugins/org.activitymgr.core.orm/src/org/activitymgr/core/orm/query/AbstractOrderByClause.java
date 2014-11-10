package org.activitymgr.core.orm.query;

public abstract class AbstractOrderByClause {
	
	private String attributeName;
	
	protected AbstractOrderByClause(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getAttributeName() {
		return attributeName;
	}

}
