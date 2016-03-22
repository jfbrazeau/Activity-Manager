package org.activitymgr.ui.web.logic.impl.internal;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Stack;

public class DbTransactionContext {
	
	private Connection tx;
	
	private Stack<Method> calls = new Stack<Method>();
	
	public DbTransactionContext(Connection con) {
		tx = con;
	}
	
	public Connection getTx() {
		return tx;
	}
	
	public Stack<Method> getCalls() {
		return calls;
	}

}