package org.activitymgr.ui.web.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
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
import org.activitymgr.ui.web.logic.impl.TasksCellLogicFatory;
import org.activitymgr.ui.web.logic.impl.internal.CollaboratorsTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.ConfigurationImpl;
import org.activitymgr.ui.web.logic.impl.internal.ContributionsTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.DefaultConstraintsValidator;
import org.activitymgr.ui.web.logic.impl.internal.NewContributionTaskButtonLogic;
import org.activitymgr.ui.web.logic.impl.internal.ReportsTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.TasksTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.ThreadLocalizedDbTransactionProviderImpl;
import org.activitymgr.ui.web.logic.impl.internal.services.RESTServicesModule;
import org.activitymgr.ui.web.logic.spi.IAuthenticatorExtension;
import org.activitymgr.ui.web.logic.spi.ICollaboratorsCellLogicFactory;
import org.activitymgr.ui.web.logic.spi.IContributionsCellLogicFactory;
import org.activitymgr.ui.web.logic.spi.IFeatureAccessManager;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;
import org.activitymgr.ui.web.logic.spi.ITabFactory;
import org.activitymgr.ui.web.logic.spi.ITaskCreationPatternHandler;
import org.activitymgr.ui.web.logic.spi.ITasksCellLogicFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.PropertyConfigurator;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;

public class LogicModule extends AbstractModule {

	@Override
	protected void configure() {
		// Load configuration
		Properties props = new Properties();
		try {
			String installArea = new URL(
					System.getProperty("osgi.install.area")).getFile();
			if (!attempToLoadConfiguration(props, new File(installArea))) {
				attempToLoadConfiguration(
						props,
						new File(System.getProperty("activitymgr.config",
								System.getProperty("user.home"))));
			}
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}

		// Configure log4j
		PropertyConfigurator.configure(props);

		// Bind configuration
		ConfigurationImpl cfg = new ConfigurationImpl(props);
		bind(IConfiguration.class).toInstance(cfg);

		// Install core module
		install(new CoreModelModule());
		
		// Create the datasource
		BasicDataSource datasource = new BasicDataSource();
		IConfiguration jdbcCfg = cfg.getScoped("activitymgr.jdbc",
				null);
		datasource.setDriverClassName(jdbcCfg.get("driver",
				"com.mysql.jdbc.Driver"));
		datasource.setUrl(jdbcCfg.get("url",
				"jdbc:mysql://localhost:3306/taskmgr_db"));
		datasource.setUsername(jdbcCfg.get("user", "taskmgr"));
		datasource.setPassword(jdbcCfg
				.get("password", "taskmgr"));
		datasource.setDefaultAutoCommit(false);
		final ThreadLocalizedDbTransactionProviderImpl dbTxProvider = new ThreadLocalizedDbTransactionProviderImpl(datasource);
		bind(ThreadLocalizedDbTransactionProviderImpl.class).toInstance(dbTxProvider);
		bind(Connection.class).toProvider(new Provider<Connection>() {
			@Override
			public Connection get() {
				return dbTxProvider.get().getTx();
			}
		});
		
		// Default SPI implementations
		bind(IFeatureAccessManager.class).toInstance(new DefaultFeatureAccessManager());
		bind(IAuthenticatorExtension.class).to(
				DefaultAuthenticatorExtension.class).in(Singleton.class);
		bind(ICollaboratorsCellLogicFactory.class).toInstance(new CollaboratorsCellLogicFatory());
		bind(IContributionsCellLogicFactory.class).toInstance(new ContributionsCellLogicFatory());
		bind(ITasksCellLogicFactory.class).toInstance(new TasksCellLogicFatory());
		
		// Install REST services
		install(new RESTServicesModule());

		// Bind tabs
		Multibinder<ITabFactory> tabsBinder = Multibinder.newSetBinder(binder(), ITabFactory.class);
		tabsBinder.addBinding().toInstance(new ITabFactory() {
			@Override
			public int getTabOrderPriority() {
				return 60;
			}
			@Override
			public String getTabId() {
				return ICollaboratorsTabLogic.ID;
			}
			@Override
			public ITabLogic<?> create(ITabFolderLogic parent) {
				return new CollaboratorsTabLogicImpl(parent);
			}
		});
		tabsBinder.addBinding().toInstance(new ITabFactory() {
			@Override
			public int getTabOrderPriority() {
				return 40;
			}
			@Override
			public String getTabId() {
				return ITasksTabLogic.ID;
			}
			@Override
			public ITabLogic<?> create(ITabFolderLogic parent) {
				return new TasksTabLogicImpl(parent);
			}
		});
		tabsBinder.addBinding().toInstance(new ITabFactory() {
			@Override
			public int getTabOrderPriority() {
				return 20;
			}
			@Override
			public String getTabId() {
				return IContributionsTabLogic.ID;
			}
			@Override
			public ITabLogic<?> create(ITabFolderLogic parent) {
				return new ContributionsTabLogicImpl(parent);
			}
		});
		tabsBinder.addBinding().toInstance(new ITabFactory() {
			@Override
			public int getTabOrderPriority() {
				return 70;
			}
			@Override
			public String getTabId() {
				return IReportsTabLogic.ADVANCED_REPORTS_ID;
			}

			@Override
			public ITabLogic<?> create(ITabFolderLogic parent) {
				return new ReportsTabLogicImpl(parent, true);
			}
		});
		tabsBinder.addBinding().toInstance(new ITabFactory() {
			@Override
			public int getTabOrderPriority() {
				return 71;
			}

			@Override
			public String getTabId() {
				return IReportsTabLogic.MY_REPORTS_ID;
			}
			@Override
			public ITabLogic<?> create(ITabFolderLogic parent) {
				return new ReportsTabLogicImpl(parent, false);
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
		System.out.println("Trying to load configuration from "
				+ cfgFolder.getAbsolutePath());
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

	@Override
	public boolean canUpdateContributions(Collaborator connected,
			Collaborator contributor) {
		return true;
	}

}

class DefaultAuthenticatorExtension implements IAuthenticatorExtension {
	
	@Inject
	private IModelMgr modelMgr;

	private IConfiguration passwords;

	@Inject
	public DefaultAuthenticatorExtension(IConfiguration cfg) {
		passwords = cfg.getScoped("users", "password");
	}

	@Override
	public boolean authenticate(String login, String password) {
		if (!passwords.isEmpty()) {
			String pwd = passwords.get(login);
			return pwd != null && pwd.equals(password)
					&& modelMgr.getCollaborator(login) != null;
		} else {
			// If there is no password configured, simply check that the user
			// exists in the database
			return modelMgr.getCollaborator(login) != null;
		}
	}

}

class NoneTaskCreationPatternHandler implements ITaskCreationPatternHandler {
	
	@Override
	public List<Task> handle(IUILogicContext context, Task newTask)
			throws ModelException {
		return Collections.emptyList();
	}

	@Override
	public String getLabel() {
		return "None";
	}
}
