package org.activitymgr.ui.web.logic.impl;

import java.lang.reflect.Constructor;
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

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.model.CoreModelModule;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.IFeatureAccessManager;
import org.activitymgr.ui.web.logic.IViewDescriptor;
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

	private IViewDescriptor viewDescriptor;
	private IEventBus eventBus = new EventBusImpl();
	private Collaborator connectedCollaborator;
	// TODO datasource musn't be declined by logic context
	private BasicDataSource datasource;
	private ThreadLocal<DbTransactionContext> transactions;
	private Injector injector;
	private IFeatureAccessManager accessManager;

	public LogicContext(IViewDescriptor viewDescriptor, IFeatureAccessManager accessManager, String jdbcDriver, String jdbcUrl, String jdbcUser, String jdbcPassword) throws SQLException {
		this.viewDescriptor = viewDescriptor;
		this.accessManager = accessManager;

		List<AbstractModule> modules = new ArrayList<AbstractModule>();
		modules.add(new CoreModelModule());
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Connection.class).toProvider(
						new Provider<Connection>() {
							@Override
							public Connection get() {
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

		// Initialize the database
		transactions = new ThreadLocal<DbTransactionContext>();
		Connection con = datasource.getConnection();
		transactions.set(new DbTransactionContext(con));

		// Create Guice injector
		try {
			injector = Guice.createInjector(modules);
			injector.getInstance(IModelMgr.class);
			con.commit();
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

	public IViewDescriptor getViewDescriptor() {
		return viewDescriptor;
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

	public <EXTENSION_TYPE, CONSTRUCTOR_ARG_TYPE> EXTENSION_TYPE getSingletonExtension(
			String extensionPointId, Class<EXTENSION_TYPE> defaultType,
			Class<CONSTRUCTOR_ARG_TYPE> constructorArgType,
			CONSTRUCTOR_ARG_TYPE constructorArg) {
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor(extensionPointId);
		Class<EXTENSION_TYPE> type = defaultType;
		if (cfgs.length > 0) {
			if (cfgs.length > 1) {
				System.err.println(
						"More than one '" + extensionPointId + "' is provided.\n" +
						"Only the first occurence will be used");
			}
			IConfigurationElement cfg = cfgs[0];
			try {
				type = Activator.getDefault().<EXTENSION_TYPE>loadClass(cfg.getContributor().getName(), cfg.getAttribute("class"));
				
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}
		return newExtensionInstance(type, constructorArgType, constructorArg);
	}

//	public <EXTENSION_TYPE, CONSTRUCTOR_ARG_TYPE> List<EXTENSION_TYPE> getExtensions(
//			String extensionPointId, Class<CONSTRUCTOR_ARG_TYPE> constructorArgType,
//			CONSTRUCTOR_ARG_TYPE constructorArg) {
//		List<EXTENSION_TYPE> result = new ArrayList<EXTENSION_TYPE>();
//		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor(extensionPointId);
//		for (IConfigurationElement cfg : cfgs) {
//			try {
//				Class<EXTENSION_TYPE> type = Activator.getDefault().<EXTENSION_TYPE>loadClass(cfg.getContributor().getName(), cfg.getAttribute("class"));
//				result.add(newExtensionInstance(type, constructorArgType, constructorArg));
//				
//			} catch (ClassNotFoundException e) {
//				throw new IllegalStateException(e);
//			}
//		}
//		return result;
//	}
//
	public <EXTENSION_TYPE, CONSTRUCTOR_ARG_TYPE> EXTENSION_TYPE newExtensionInstance(Class<EXTENSION_TYPE> type,
			Class<CONSTRUCTOR_ARG_TYPE> constructorArgType,
			CONSTRUCTOR_ARG_TYPE constructorArg) {
		try {
			// The type is supposed whether to have :
			// * a declared constructor accepting an abstract logic as parent
			// * or a declared constructor with two parameters : parent logic / logic context
			try {
				// First attempt
				Constructor<EXTENSION_TYPE> constructor = type.getDeclaredConstructor(constructorArgType, LogicContext.class);
				return constructor.newInstance(constructorArg, this);
			}
			catch (NoSuchMethodException e) {
				// Second attempt
				Constructor<EXTENSION_TYPE> constructor = type.getDeclaredConstructor(constructorArgType);
				return constructor.newInstance(constructorArg);
			}
			
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T buildTransactionalWrapper(final T wrapped, final Class<?> interfaceToWrapp) {
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

	public IDTOFactory getBeanFactory() {
		return injector.getInstance(IDTOFactory.class);
	}
}

class DbTransactionContext {
	Connection tx;
	Stack<Method> calls = new Stack<Method>();
	DbTransactionContext(Connection con) {
		tx = con;
	}
}