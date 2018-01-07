package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.IReportsLogic.View;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

@SuppressWarnings("serial")
public class ReportsTabPanel extends AbstractTabPanel<IReportsTabLogic>
		implements IReportsTabLogic.View {

	private HorizontalLayout bodyComponent;

	@Inject
	public ReportsTabPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	protected Component createBodyComponent() {
		bodyComponent = new HorizontalLayout();
		return bodyComponent;
	}

	@Override
	public void setReportsView(View view) {
		bodyComponent.addComponent((Component) view);
	}

}
