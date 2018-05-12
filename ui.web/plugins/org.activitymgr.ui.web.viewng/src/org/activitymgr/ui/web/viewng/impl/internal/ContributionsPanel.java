package org.activitymgr.ui.web.viewng.impl.internal;

import java.util.Calendar;

import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.viewng.AbstractTabPanel;
import org.activitymgr.ui.web.viewng.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

@SuppressWarnings("serial")
public class ContributionsPanel extends
		AbstractTabPanel<IContributionsTabLogic> implements
		IContributionsTabLogic.View {

	@Inject
	public ContributionsPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	public void setCollaboratorsProvider(
			ITableCellProviderCallback<Long> collaboratorsProvider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectCollaborator(long collaboratorId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setContributionsProvider(
			ITableCellProviderCallback<Long> contributionsProvider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDate(Calendar lastMonday) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reloadContributionTableItems() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reloadContributionTableFooter() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setColumnTitle(String propertyId, String title) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Component createBodyComponent() {
		return new Label("Fake contributions panel");
	}

}
