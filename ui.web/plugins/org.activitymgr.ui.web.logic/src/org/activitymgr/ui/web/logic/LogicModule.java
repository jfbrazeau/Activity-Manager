package org.activitymgr.ui.web.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Savepoint;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Stack;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.CoreModelModule;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.impl.CollaboratorsCellLogicFatory;
import org.activitymgr.ui.web.logic.impl.ContributionsCellLogicFatory;
import org.activitymgr.ui.web.logic.impl.internal.CollaboratorsTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.ContributionsTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.DefaultConstraintsValidator;
import org.activitymgr.ui.web.logic.impl.internal.NewContributionTaskButtonLogic;
import org.activitymgr.ui.web.logic.impl.internal.TasksTabLogicImpl;
import org.activitymgr.ui.web.logic.spi.IAuthenticatorExtension;
import org.activitymgr.ui.web.logic.spi.ICollaboratorsCellLogicFactory;
import org.activitymgr.ui.web.logic.spi.IContributionsCellLogicFactory;
import org.activitymgr.ui.web.logic.spi.IFeatureAccessManager;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;
import org.activitymgr.ui.web.logic.spi.ITabFactory;
import org.activitymgr.ui.web.logic.spi.ITaskCreationPatternHandler;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.PropertyConfigurator;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;

public class LogicModule extends AbstractModule {

	@Override
	protected void configure() {
		// Install core module
		install(new CoreModelModule());
		
		// Load configuration
		Properties props = new Properties();
		try {
			String installArea = new URL(System.getProperty("osgi.install.area")).getFile();
			if (!attempToLoadConfiguration(props, new File(installArea))) {
				attempToLoadConfiguration(props, new File(System.getProperty("activitymgr.config", System.getProperty("user.home"))));
			}
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
		
		// Configure log4j
		PropertyConfigurator.configure(props);
		System.out.println(props);
		// Create the datasource
		String jdbcDriver = props.getProperty("activitymgr.jdbc.driver", "com.mysql.jdbc.Driver");
		String jdbcUrl = props.getProperty("activitymgr.jdbc.url", "jdbc:mysql://localhost:3306/taskmgr_db");
		String jdbcUser = props.getProperty("activitymgr.jdbc.user", "taskmgr");
		String jdbcPassword = props.getProperty("activitymgr.jdbc.password", "taskmgr");
		TransactionManager txManager = new TransactionManager(jdbcDriver, jdbcUrl, jdbcUser, jdbcPassword);

		// Bind TX provider
		bind(Connection.class).toProvider(txManager);
		// and Transactional wrapper builder
		bind(ITransactionalWrapperBuilder.class).toInstance(txManager);
		
		// Default SPI implementations
		bind(IFeatureAccessManager.class).toInstance(new DefaultFeatureAccessManager());
		bind(IAuthenticatorExtension.class).toInstance(new DefaultAuthenticatorExtension());
		bind(ICollaboratorsCellLogicFactory.class).toInstance(new CollaboratorsCellLogicFatory());
		bind(IContributionsCellLogicFactory.class).toInstance(new ContributionsCellLogicFatory());
		
		// Bind tabs
		Multibinder<ITabFactory> tabsBinder = Multibinder.newSetBinder(binder(), ITabFactory.class);
		tabsBinder.addBinding().toInstance(new ITabFactory() {
			@Override
			public String getTabId() {
				return "collaborators";
			}
			@Override
			public ITabLogic<?> create(ITabFolderLogic parent) {
				return new CollaboratorsTabLogicImpl(parent);
			}
		});
		tabsBinder.addBinding().toInstance(new ITabFactory() {
			@Override
			public String getTabId() {
				return "tasks";
			}
			@Override
			public ITabLogic<?> create(ITabFolderLogic parent) {
				return new TasksTabLogicImpl(parent);
			}
		});
		tabsBinder.addBinding().toInstance(new ITabFactory() {
			@Override
			public String getTabId() {
				return "contributions";
			}
			@Override
			public ITabLogic<?> create(ITabFolderLogic parent) {
				return new ContributionsTabLogicImpl(parent);
			}
		});

		// Bind task creation pattern
		MapBinder<String, ITaskCreationPatternHandler> tcpBinder = MapBinder.newMapBinder(binder(), String.class, ITaskCreationPatternHandler.class);
		tcpBinder.addBinding("0_none").to(NoneTaskCreationPatternHandler.class);

		// Bind contribution tab buttons
		Multibinder<ITabButtonFactory<IContributionsTabLogic>> ctlBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<ITabButtonFactory<IContributionsTabLogic>>() {});
		ctlBinder.addBinding().toInstance(new ITabButtonFactory<IContributionsTabLogic>() {
			@Override
			public IButtonLogic<?> create(IContributionsTabLogic parent) {
				return new NewContributionTaskButtonLogic(parent);
			}
		});

		// Bind constraints validator
		Multibinder<IConstraintsValidator> cvBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<IConstraintsValidator>() {});
		cvBinder.addBinding().to(DefaultConstraintsValidator.class);
	}

	private boolean attempToLoadConfiguration(Properties props, File cfgFolder) {
		System.out.println("Trying to load configuration from " + cfgFolder.getAbsolutePath());
		if (cfgFolder.exists() && cfgFolder.isDirectory()) {
			try {
				File[] propFiles = cfgFolder.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".properties");
					}
				});
				for (File propFile : propFiles) {
					System.out.println("Loading " + propFile.getAbsolutePath());
					props.load(new FileInputStream(propFile));
				}
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			return !props.isEmpty();
		}
		return false;
	}

}

class DbTransactionContext {
	Connection tx;
	Stack<Method> calls = new Stack<Method>();
	DbTransactionContext(Connection con) {
		tx = con;
	}
}

class DefaultFeatureAccessManager implements IFeatureAccessManager {
	@Override
	public boolean hasAccessToTab(Collaborator collaborator, String tab) {
		return true;
	}
}

class DefaultAuthenticatorExtension implements IAuthenticatorExtension {
	
	@Inject
	private IModelMgr modelMgr;

	@Override
	public boolean authenticate(String login, String password) {
		return modelMgr.getCollaborator(login) != null;
	}

}

class NoneTaskCreationPatternHandler implements ITaskCreationPatternHandler {
	
	@Override
	public List<Task> handle(ILogicContext context, Task newTask)
			throws ModelException {
		return Collections.emptyList();
	}

	@Override
	public String getLabel() {
		return "None";
	}
}

class TransactionManager implements ITransactionalWrapperBuilder, Provider<Connection> {
	
	private BasicDataSource datasource;
	private ThreadLocal<DbTransactionContext> transactions = new ThreadLocal<DbTransactionContext>();

	TransactionManager(String jdbcDriver, String jdbcUrl, String jdbcUser, String jdbcPassword) {
		datasource = new BasicDataSource();
		datasource.setDriverClassName(jdbcDriver);
		datasource.setUrl(jdbcUrl);
		datasource.setUsername(jdbcUser);
		datasource.setPassword(jdbcPassword);
		datasource.setDefaultAutoCommit(false);
	}

	@Override
	public Connection get() {
		DbTransactionContext txCtx = transactions.get();
		return txCtx != null ? txCtx.tx : null;
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
								if (txCtx != null) {
									txCtx.calls.pop();
									if (txCtx.calls.size() == 0) {
										// Release the transaction
										transactions.remove();
										txCtx.tx.close();
									}
								}
							}
						}
					}
				});
	}

}
