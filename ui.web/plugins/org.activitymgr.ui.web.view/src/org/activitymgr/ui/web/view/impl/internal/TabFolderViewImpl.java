package org.activitymgr.ui.web.view.impl.internal;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.ITabFolderLogic;

import com.vaadin.server.VaadinService;
import com.vaadin.server.ClientConnector.AttachEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.themes.Runo;

@SuppressWarnings("serial")
public class TabFolderViewImpl extends TabSheet implements ITabFolderLogic.View {

	private ITabFolderLogic logic;
	
	private boolean moreThanOneTab = false;
	
	private Map<Component, String> tabIdsByComponentsMap = new IdentityHashMap<Component, String>();

	private Map<String, Component> componentsByTabIdsMap = new HashMap<String, Component>();

	public TabFolderViewImpl() {
		setStyleName(Runo.TABSHEET_SMALL);
		setTabsVisible(false);
		setSizeFull();
		addSelectedTabChangeListener(new TabSheet.SelectedTabChangeListener() {
			@Override
			public void selectedTabChange(SelectedTabChangeEvent event) {
				if (VaadinService.getCurrentRequest() != null) {
					String tabId = tabIdsByComponentsMap.get(getSelectedTab());
					if (tabId != null) {
						logic.onSelectedTabChanged(tabId);
					}
				}
			}
		});
	}

	@Override
	public void registerLogic(ITabFolderLogic logic) {
		this.logic = logic;
	}

	@Override
	public void addTab(String id, String label, IView<?> view) {
		Component component = (Component) view;
		addTab(component, label);
		tabIdsByComponentsMap.put(component, id);
		componentsByTabIdsMap.put(id, component);
		if (moreThanOneTab) {
			setTabsVisible(true);
		}
		moreThanOneTab = true;
	}

	@Override
	public void setSelectedTab(String id) {
		Component c = componentsByTabIdsMap.get(id);
		if (c != null) {
			setSelectedTab(c);
		}
	}
	
}
