package org.activitymgr.core.orm.query;

public class GreaterThanStatement extends AbstractStatement {

	private Object value;
	private boolean orEquals;

	public GreaterThanStatement(Object value, boolean orEquals) {
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
