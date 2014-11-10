package org.activitymgr.core.orm.query;

public class InStatement extends AbstractStatement {

	private Object[] values;

	public InStatement(Object[] values) {
		this.values = values;
	}

	public Object[] getValues() {
		return values;
	}
	
}
