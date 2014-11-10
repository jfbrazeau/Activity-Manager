package org.activitymgr.core.orm.query;

public class LowerThanStatement extends AbstractStatement {

	private Object value;
	private boolean orEquals;

	public LowerThanStatement(Object value, boolean orEquals) {
		this.value = value;
		this.orEquals = orEquals;
	}

	public boolean getOrEquals() {
		return orEquals;
	}

	public Object getValue() {
		return value;
	}

}
