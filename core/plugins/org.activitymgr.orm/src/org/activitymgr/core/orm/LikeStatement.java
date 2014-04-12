package org.activitymgr.core.orm;

public class LikeStatement extends AbstractStatement {

	private Object value;

	public LikeStatement(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

}
