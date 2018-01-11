package org.activitymgr.ui.web.view.impl.internal;

import java.util.Collection;

import org.activitymgr.ui.web.logic.IReportsLogic.View;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ReportsTabPanel extends AbstractTabPanel<IReportsTabLogic>
		implements IReportsTabLogic.View {

	private VerticalLayout leftComponent;

	private HorizontalLayout bodyComponent;

	private ListSelect reportsList;

	private HorizontalLayout reportCfgsButtonsPanel;

	private IndexedContainer resportsListDatasource;

	private View reportsView;

	@Inject
	public ReportsTabPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	protected Component createLeftComponent() {
		leftComponent = new VerticalLayout();
		leftComponent.setSpacing(true);
		leftComponent.setMargin(new MarginInfo(false, true, false, false));

		reportCfgsButtonsPanel = new HorizontalLayout();
		leftComponent.addComponent(reportCfgsButtonsPanel);

		reportsList = new ListSelect();
		reportsList.setImmediate(true);
		reportsList.setMultiSelect(true);
		reportsList.setNullSelectionAllowed(false);
		reportsList.setWidth(100, Unit.PERCENTAGE);
		resportsListDatasource = (IndexedContainer) reportsList
				.getContainerDataSource();
		reportsList.select("Projet EDF");
		leftComponent.addComponent(reportsList);

		leftComponent.setExpandRatio(reportCfgsButtonsPanel, 10);
		leftComponent.setExpandRatio(reportsList, 90);

		reportsList.addValueChangeListener(new ValueChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void valueChange(ValueChangeEvent event) {
				getLogic().onSelectionChanged(
						(Collection<Long>) reportsList.getValue());
			}
		});

		return leftComponent;
	}

	@Override
	public void removeReportCfg(long id) {
		resportsListDatasource.removeItem(id);
	}

	@Override
	public void addReportCfg(long id, String name, int idx) {
		resportsListDatasource.addItemAt(idx, id);
		reportsList.setItemCaption(id, name);
	}

	@Override
	public void setLongReportsList(boolean longList) {
		reportsList.setHeight(longList ? 530 : 230, Unit.PIXELS);
	}

	@Override
	protected Component createBodyComponent() {
		bodyComponent = new HorizontalLayout();
		return bodyComponent;
	}

	@Override
	public void setReportsView(View view) {
		this.reportsView = view;
		bodyComponent.addComponent((Component) view);
	}

	@Override
	public void addReportConfigurationButton(
			org.activitymgr.ui.web.logic.IStandardButtonLogic.View view) {
		reportCfgsButtonsPanel.addComponent((Component) view);
	}

	@Override
	public void setReportsPanelEnabled(boolean b) {
		((Component) reportsView).setEnabled(b);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void selectReportCfg(long id) {
		for (Long selected : (Collection<Long>) reportsList.getValue()) {
			reportsList.unselect(selected);
		}
		reportsList.select(id);
	}

}
