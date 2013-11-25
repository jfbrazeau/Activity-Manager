package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.ITabFolderLogic;

import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.themes.Runo;

@SuppressWarnings("serial")
public class TabFolderViewImpl extends TabSheet implements ITabFolderLogic.View {

	@SuppressWarnings("unused")
	private ITabFolderLogic logic;
	
	private boolean moreThanOneTab = false;

	public TabFolderViewImpl() {
		setStyleName(Runo.TABSHEET_SMALL);
		hideTabs(true);
	}

	@Override
	public void registerLogic(ITabFolderLogic logic) {
		this.logic = logic;
	}

	@Override
	public void addTab(String label, IView<?> view) {
		addTab((Component) view, label);
		if (moreThanOneTab) {
			hideTabs(false);
		}
		moreThanOneTab = true;
	}

}
