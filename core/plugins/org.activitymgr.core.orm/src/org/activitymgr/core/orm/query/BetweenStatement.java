package org.activitymgr.core.orm.query;

public class BetweenStatement extends AbstractStatement {

	private Object low;
	private Object high;

	public BetweenStatement(Object low, Object high) {
		this.low = low;
		this.high = high;
	}

	public Object getHigh() {
		return high;
	}

	public Object getLow() {
		return low;
	}

}
