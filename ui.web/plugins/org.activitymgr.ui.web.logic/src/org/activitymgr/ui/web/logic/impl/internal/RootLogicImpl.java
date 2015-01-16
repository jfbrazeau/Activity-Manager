package org.activitymgr.ui.web.logic.impl.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.IButtonLogic;
import org.activitymgr.ui.web.logic.IDownloadButtonLogic;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.IFeatureAccessManager;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.IViewDescriptor;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.DefaultFeatureAccessManagerImpl;
import org.activitymgr.ui.web.logic.impl.LogicContext;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

public class RootLogicImpl implements IRootLogic {

	private LogicContext context;
	private IRootLogic.View view;
	
	public RootLogicImpl(IRootLogic.View rootView, IViewDescriptor viewFactory) {
		// Retrieve the feature access manager if configured
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.featureAccessManager");

		// Feature access manager retrieval
		IFeatureAccessManager accessManager = null;
		if (cfgs.length == 0) {
			accessManager = new DefaultFeatureAccessManagerImpl();
		}
		else {
			if (cfgs.length > 1) {
				System.err.println(
						"More than one feature access manager is provided.\n" +
						"Only one implementation is allowed");
			}
			IConfigurationElement cfg = cfgs[0];
			try {
				accessManager = (IFeatureAccessManager) cfg.createExecutableExtension("class");
			}
			catch (CoreException e) {
				// If an error occurs, a null access manager is instantiated
				// No feature will be available
				handleError(getView(), e);
				accessManager = new NoAccessFeatureAccessManager();
			}
		}
		
		// Once the registry is registered, ModelMgr implementation 
		// can be created
		// TODO externalize connection parameters
		// Create the datasource
		String jdbcDriver = "com.mysql.jdbc.Driver";
		String jdbcUrl = "jdbc:mysql://localhost:3306/taskmgr_db";
		String jdbcUser = "taskmgr_user";
		String jdbcPassword = "secret";
		// Context initialization
		try {
			context = new LogicContext(viewFactory, accessManager, jdbcDriver, jdbcUrl, jdbcUser, jdbcPassword);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}

		// View registration
		this.view = rootView;
		view.registerLogic(this);

		// Model manager retrieval
		// Create authentication logic
		getView().setContentView(new AuthenticationLogicImpl(this, getContext()).getView());
		
		// Event listeners registration
		context.getEventBus().register(CallbackExceptionEvent.class, new IEventListener() {
			@Override
			public void handle(AbstractEvent event) {
				// If an error occurs in a view callback, it shows the error to the user
				handleError(getView(), ((CallbackExceptionEvent) event).getException());
			}
		});
		context.getEventBus().register(ConnectedCollaboratorEvent.class, new IEventListener() {
			@Override
			public void handle(AbstractEvent event) {
				// Create the tab container
				TabFolderLogicImpl tabFolderLogic = new TabFolderLogicImpl(RootLogicImpl.this, getContext());
				getView().setContentView(tabFolderLogic.getView());
				// Iterate over the provided tabs and create it
				IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.tabLogic");
				List<IConfigurationElement> cfgList = new ArrayList<IConfigurationElement>(Arrays.asList(cfgs));
				Collections.reverse(cfgList);
				for (IConfigurationElement cfg : cfgList) {
					String tabId = cfg.getAttribute("id");
					String tabName = cfg.getAttribute("label");
					// Check user access
					if (!getContext().getAccessManager().hasAccessToTab(getContext().getConnectedCollaborator(), tabName)) {
						return;
					}
					try {
						Class<AbstractTabLogicImpl<?>> tabLogicClass = Activator.getDefault().<AbstractTabLogicImpl<?>>loadClass(cfg.getContributor().getName(), cfg.getAttribute("class"));
						AbstractTabLogicImpl<?> tabLogic = getContext().newExtensionInstance(tabLogicClass, AbstractLogicImpl.class, tabFolderLogic);
						tabFolderLogic.addTab(tabName, tabLogic);
						addButtons(tabLogic, tabId);
					} catch (ClassNotFoundException e) {
						throw new IllegalStateException(e);
					}
				}
				
			}

		});
	}
	
	private void addButtons(AbstractTabLogicImpl<?> tabLogic, String tabId) throws InvalidRegistryObjectException, ClassNotFoundException {
		// Create actions
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.button");
		for (IConfigurationElement cfg : cfgs) {
			String targetTabId = cfg.getAttribute("target");
			if (tabId.equals(targetTabId)) {
				String iconId = cfg.getAttribute("iconId");
				String label = cfg.getAttribute("label");
				// Standard button case
				if ("button".equals(cfg.getName())) {
					String kbDesc = cfg.getAttribute("shortcutKey");
					KeyBinding kb = new KeyBinding(kbDesc);
					Class<IButtonLogic> buttonLogicClass = Activator.getDefault().<IButtonLogic>loadClass(cfg.getContributor().getName(), cfg.getAttribute("logic"));
					IButtonLogic buttonLogic = getContext().newExtensionInstance(buttonLogicClass, AbstractLogicImpl.class, tabLogic);
					IButtonLogic.View buttonView = buttonLogic.getView();
					buttonView.setIcon(iconId);
					buttonView.setDescription(label + " <em>"
							+ kbDesc + "</em>");
					tabLogic.getView().addButton(label, kb.getKey(), kb.isCtrl(), kb.isShift(), kb.isAlt(), buttonView);
				}
				// Download button case
				else {
					Class<IDownloadButtonLogic> buttonLogicClass = Activator.getDefault().<IDownloadButtonLogic>loadClass(cfg.getContributor().getName(), cfg.getAttribute("logic"));
					IDownloadButtonLogic buttonLogic = getContext().newExtensionInstance(buttonLogicClass, AbstractLogicImpl.class, tabLogic);
					IDownloadButtonLogic.View buttonView = buttonLogic.getView();
					buttonView.setIcon(iconId);
					buttonView.setDescription(label);
					tabLogic.getView().addDownloadButton(buttonView);
				}
			}
		}
	}
	
	
	@Override
	public ILogic<?> getParent() {
		return null;
	}

	@Override
	public View getView() {
		return view;
	}
	
	public LogicContext getContext() {
		return context;
	}

	public static void handleError(IRootLogic.View rootView, Throwable error) {
		error.printStackTrace();
		// Building message
		String message = error.getMessage();
		if (message == null || "".equals(message.trim())) {
			message = error.getClass().getSimpleName();
		}
		// Generating details
		String details = null;
		Throwable cause = error;
		while ((cause = cause.getCause()) != null) {
			details = cause.getClass().getSimpleName() + " : " + cause.getMessage() + "\n";
		}
		// FIXME transport the error on the event bus ?
		rootView.showErrorNotification(message, details);
	}

}

class NoAccessFeatureAccessManager implements IFeatureAccessManager {

	@Override
	public boolean hasAccessToTab(Collaborator collaborator, String tab) {
		return false;
	}
	
}