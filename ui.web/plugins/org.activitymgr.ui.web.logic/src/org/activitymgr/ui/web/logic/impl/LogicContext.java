package org.activitymgr.ui.web.logic.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.activitymgr.core.CoreModule;
import org.activitymgr.core.DbException;
import org.activitymgr.core.DbTransaction;
import org.activitymgr.core.IModelMgr;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.IFeatureAccessManager;
import org.activitymgr.ui.web.logic.IViewFactory;
import org.activitymgr.ui.web.logic.impl.event.EventBusImpl;
import org.activitymgr.ui.web.logic.impl.internal.Activator;
import org.apache.commons.dbcp.BasicDataSource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

// TODO Inject ?
public class LogicContext {

	private IViewFactory viewFactory;
	private IEventBus eventBus = new EventBusImpl();
	private Collaborator connectedCollaborator;
	// TODO datasource musn't be declined by logic context
	private BasicDataSource datasource;
	private ThreadLocal<DbTransactionContext> transactions;
	private Injector injector;
	private IFeatureAccessManager accessManager;

	public LogicContext(IViewFactory viewFactory, IFeatureAccessManager accessManager, String jdbcDriver, String jdbcUrl, String jdbcUser, String jdbcPassword) throws SQLException {
		this.viewFactory = viewFactory;
		this.accessManager = accessManager;

		List<AbstractModule> modules = new ArrayList<AbstractModule>();
		modules.add(new CoreModule());
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				bind(DbTransaction.class).toProvider(
						new Provider<DbTransaction>() {
							@Override
							public DbTransaction get() {
								DbTransactionContext txCtx = transactions.get();
								return txCtx != null ? txCtx.tx : null;
							}
						});
			}
		});
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.additionalModules");
		for (IConfigurationElement cfg : cfgs) {
			try {
				modules.add((AbstractModule) cfg.createExecutableExtension("class"));
			} catch (CoreException e) {
				throw new IllegalStateException(e);
			}
		}

		// Create the Datasource (TODO static ?)
		datasource = new BasicDataSource();
		datasource.setDriverClassName(jdbcDriver);
		datasource.setUrl(jdbcUrl);
		datasource.setUsername(jdbcUser);
		datasource.setPassword(jdbcPassword);
		datasource.setDefaultAutoCommit(false);

		// Create Guice injector
		transactions = new ThreadLocal<DbTransactionContext>();
		injector = Guice.createInjector(modules);

		// Initialize the database
		Connection con = datasource.getConnection();
		transactions.set(new DbTransactionContext(con));
		try {
			getComponent(IModelMgr.class).initialize();
			con.commit();
		}
		catch (DbException e) {
			con.rollback();
			throw new IllegalStateException("Couldn't initialize the database access", e);
		}
		finally {
			transactions.remove();
			con.close();
		}
	}

	public IFeatureAccessManager getAccessManager() {
		return accessManager;
	}

	public <T> T getComponent(Class<T> c) {
		return injector.getInstance(c);
	}

	public IViewFactory getViewFactory() {
		return viewFactory;
	}

	public IEventBus getEventBus() {
		return eventBus;
	}

	public Collaborator getConnectedCollaborator() {
		return connectedCollaborator;
	}

	public void setConnectedCollaborator(Collaborator connectedCollaborator) {
		this.connectedCollaborator = connectedCollaborator;
	}

	@SuppressWarnings("unchecked")
	public <T> T buildTransactionalWrapper(final T wrapped, Class<?> interfaceToWrapp) {
		return (T) Proxy.newProxyInstance(
				wrapped.getClass().getClassLoader(),
				// TODO add comments
				new Class<?>[] { interfaceToWrapp }, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						DbTransactionContext txCtx = transactions.get();
						Savepoint sp = null;
						try {
							// Open the transaction if required and push a savepoint
							if (txCtx == null) {
								txCtx = new DbTransactionContext(datasource.getConnection());
								transactions.set(txCtx);
							}
							else {
								sp = txCtx.tx.getConnection().setSavepoint();
							}
							txCtx.calls.push(method);
							//log(txCtx, "START");
							// Call the real model manager
							Object result = method.invoke(wrapped, args);

							// Commit the transaction (or put a save point)
							if (txCtx.calls.size() > 1) {
								sp = txCtx.tx.getConnection().setSavepoint();
							}
							else {
								txCtx.tx.getConnection().commit();
							}
							return result;
						} catch (InvocationTargetException t) {
							// Rollback the transaction in case of failure
							if (txCtx.calls.size() > 1) {
								txCtx.tx.getConnection().rollback(sp);
							}
							else {
								txCtx.tx.getConnection().rollback();
							}
							throw t.getCause();
						} finally {
							//log(txCtx, "END");
							txCtx.calls.pop();
							if (txCtx.calls.size() == 0) {
								// Release the transaction
								transactions.remove();
								txCtx.tx.getConnection().close();
							}
						}
					}
					private void log(DbTransactionContext ctx, String s) {
						Method method = ctx.calls.peek();
						System.out.println(Thread.currentThread() + "-" + (method != null ? method.getName() :"") + "-" + ctx.calls.size() + "-" + s);
					}
				});
	}
}

class DbTransactionContext {
	DbTransactionContext(Connection con) {
		tx = new DbTransaction(con);
	}
	DbTransaction tx;
	Stack<Method> calls = new Stack<Method>();
}