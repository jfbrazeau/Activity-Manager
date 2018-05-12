package org.activitymgr.ui.web.viewng.impl.internal;

import org.activitymgr.ui.web.logic.IReportsLogic.View;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.viewng.AbstractTabPanel;
import org.activitymgr.ui.web.viewng.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

@SuppressWarnings("serial")
public class ReportsTabPanel extends AbstractTabPanel<IReportsTabLogic>
		implements IReportsTabLogic.View {

	@Inject
	public ReportsTabPanel(IResourceCache resourceCache) {
		super(resourceCache);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void setReportsView(View view) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setLongReportsList(boolean longList) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addReportConfigurationButton(
			org.activitymgr.ui.web.logic.IStandardButtonLogic.View view) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addReportCfg(long id, String name, int position) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setReportsPanelEnabled(boolean b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectReportCfg(long id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeReportCfg(long id) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Component createBodyComponent() {
		return new Label("Fake reports panel");
	}

}
