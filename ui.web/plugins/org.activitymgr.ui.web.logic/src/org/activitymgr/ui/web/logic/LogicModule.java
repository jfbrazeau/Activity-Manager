package org.activitymgr.ui.web.logic;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Savepoint;
import java.util.Stack;

import org.activitymgr.core.model.CoreModelModule;
import org.apache.commons.dbcp.BasicDataSource;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;

public class LogicModule extends AbstractModule implements ITransactionalWrapperBuilder {

	private BasicDataSource datasource;
	private ThreadLocal<DbTransactionContext> transactions = new ThreadLocal<DbTransactionContext>();

	@Override
	protected void configure() {
		// Install core module
		install(new CoreModelModule());
		
		// TODO externalize connection parameters
		// Create the datasource
		String jdbcDriver = "com.mysql.jdbc.Driver";
		String jdbcUrl = "jdbc:mysql://localhost:3306/taskmgr_db";
		String jdbcUser = "taskmgr_user";
		String jdbcPassword = "secret";
		datasource = new BasicDataSource();
		datasource.setDriverClassName(jdbcDriver);
		datasource.setUrl(jdbcUrl);
		datasource.setUsername(jdbcUser);
		datasource.setPassword(jdbcPassword);
		datasource.setDefaultAutoCommit(false);

		// Bind TX provider
		bind(Connection.class).toProvider(
				new Provider<Connection>() {
					@Override
					public Connection get() {
						DbTransactionContext txCtx = transactions.get();
						return txCtx != null ? txCtx.tx : null;
					}
				});

		// Transactional wrapper builder
		bind(ITransactionalWrapperBuilder.class).toInstance(this);

	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T buildTransactionalWrapper(final T wrapped, Class<?> interfaceToWrapp) {
		return (T) Proxy.newProxyInstance(
				wrapped.getClass().getClassLoader(),
				// TODO add comments
				new Class<?>[] { interfaceToWrapp }, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						if (method.getDeclaringClass().equals(Object.class)) {
							return method.invoke(wrapped, args);
						}
						else {
							DbTransactionContext txCtx = transactions.get();
							Savepoint sp = null;
							try {
								// Open the transaction if required and push a savepoint
								if (txCtx == null) {
									txCtx = new DbTransactionContext(datasource.getConnection());
									transactions.set(txCtx);
								}
								else {
									sp = txCtx.tx.setSavepoint();
								}
								txCtx.calls.push(method);
								//log(txCtx, "START");
								// Call the real model manager
								Object result = method.invoke(wrapped, args);
	
								// Commit the transaction (or put a save point)
								if (txCtx.calls.size() > 1) {
									sp = txCtx.tx.setSavepoint();
								}
								else {
									txCtx.tx.commit();
								}
								return result;
							} catch (InvocationTargetException t) {
								// Rollback the transaction in case of failure
								if (txCtx.calls.size() > 1) {
									txCtx.tx.rollback(sp);
								}
								else {
									txCtx.tx.rollback();
								}
								throw t.getCause();
							} finally {
								//log(txCtx, "END");
								txCtx.calls.pop();
								if (txCtx.calls.size() == 0) {
									// Release the transaction
									transactions.remove();
									txCtx.tx.close();
								}
							}
						}
					}
				});
	}

}

class DbTransactionContext {
	Connection tx;
	Stack<Method> calls = new Stack<Method>();
	DbTransactionContext(Connection con) {
		tx = con;
	}
}