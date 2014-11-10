package org.activitymgr.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.activitymgr.core.util.DbHelper;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

public abstract class AbstractModelTestCase extends TestCase implements
		Provider<DbTransaction> {

	/** Logger */
	private static Logger log = Logger.getLogger(AbstractModelTestCase.class);

	private static BasicDataSource datasource;
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					Connection con = datasource.getConnection();
					if (DbHelper.isEmbeddedHsqlOrH2(con, datasource.getUrl())) {
						DbHelper.shutdowHsqlOrH2(con);
					}
					datasource.close();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});

		// Initialisation des logs et chargement de la config
		PropertyConfigurator.configure(AbstractModelTestCase.class
				.getResource("tests.properties"));
		Properties props = new Properties();
		InputStream in = AbstractModelTestCase.class
				.getResourceAsStream("tests.properties");
		try {
			props.load(in);
			in.close();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		// Préfixe de config à utiliser
		String cfg = props.getProperty("dbconfig");
		// Initialisation de la connexion à la base de données
		String jdbcDriver = props.getProperty(cfg + "." + "driver");
		String jdbcUrl = props.getProperty(cfg + "." + "url");
		String jdbcUser = props.getProperty(cfg + "." + "user");
		String jdbcPassword = props.getProperty(cfg + "." + "password");

		// Database connection
		datasource = new BasicDataSource();
		datasource.setDriverClassName(jdbcDriver);
		datasource.setUrl(jdbcUrl);
		datasource.setUsername(jdbcUser);
		datasource.setPassword(jdbcPassword);
		datasource.setDefaultAutoCommit(false);
	}

	/** Model manager */
	private IModelMgr modelMgr;

	/** The test transaction */
	private DbTransaction tx;

	/** Guice injector */
	private Injector injector;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		tx = new DbTransaction(datasource.getConnection());

		// Create Guice injector
		List<Module> modules = getGuiceModules();
		injector = Guice.createInjector(modules);

		// Retrieve model manager instance
		final IModelMgr modelMgr = injector.getInstance(IModelMgr.class);
		this.modelMgr = (IModelMgr) Proxy.newProxyInstance(
				AbstractModelTestCase.class.getClassLoader(),
				new Class<?>[] { IModelMgr.class }, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						try {
							Object result = method.invoke(modelMgr, args);
							tx.getConnection().commit();
							return result;
						} catch (InvocationTargetException t) {
							t.getCause().printStackTrace();
							tx.getConnection().rollback();
							throw t.getCause();
						}
					}
				});

		// If tables don't exist, create it
		getModelMgr().createTables();
	}

	/**
	 * 
	 * @return
	 */
	protected List<Module> getGuiceModules() {
		ArrayList<Module> modules = new ArrayList<Module>();
		modules.add(new CoreModule());
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				bind(DbTransaction.class).toProvider(
						AbstractModelTestCase.this);
			}
		});
		return modules;
	}

	/**
	 * @return the injector instance.
	 */
	protected Injector getInjector() {
		return injector;
	}

	@Override
	public DbTransaction get() {
		return tx;
	}

	/**
	 * @see junit.framework.Test#run(junit.framework.TestResult)
	 */
	public void run(final TestResult testResult) {
		log.error("");
		log.error("");
		log.error("");
		log.error("**********************************************************");
		log.error("*** STARTING TEST : '" + getName() + "'");
		log.error("**********************************************************");
		try {
			super.run(testResult);
		} finally {
			log.error("Test '" + getName() + "' done.");
		}
	}

	protected void tearDown() throws Exception {
		tx.getConnection().close();
	}

	protected IModelMgr getModelMgr() {
		return modelMgr;
	}
}
