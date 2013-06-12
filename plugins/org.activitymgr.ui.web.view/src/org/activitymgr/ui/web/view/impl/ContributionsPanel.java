package org.activitymgr.ui.web.view.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.ui.web.logic.IActionLogic.View;
import org.activitymgr.ui.web.logic.IContributionCellLogicProvider;
import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.view.IContributionColumnViewProvider;
import org.activitymgr.ui.web.view.util.ActionView;
import org.activitymgr.ui.web.view.util.ResourceCache;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.DateField;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ContributionsPanel extends VerticalLayout implements IContributionsLogic.View, Button.ClickListener {

	private IContributionsLogic logic;
	
	@SuppressWarnings("unused")
	private ResourceCache resourceCache;

	private DateField dateField;

	private Button previousYearButton;

	private Button previousMonthButton;

	private Button previousWeekButton;

	private Button nextWeekButton;

	private Button nextMonthButton;

	private Button nextYearButton;

	private List<IContributionColumnViewProvider> viewProviders = new ArrayList<IContributionColumnViewProvider>();;

	private Table contributionsTable;

	private VerticalLayout actionsContainer;

	public ContributionsPanel(ResourceCache resourceCache) {
		this.resourceCache = resourceCache;

		setSpacing(true);
		setMargin(true);
		
		/*
		 * Controls
		 */
		GridLayout controlsContainer = new GridLayout(8, 1);
		addComponent(controlsContainer);
		Label emptyLabel = new Label();
		emptyLabel.setWidth(250, Unit.PIXELS);
		controlsContainer.addComponent(emptyLabel);
		previousYearButton = new Button("<<< Year");
		controlsContainer.addComponent(previousYearButton);
		previousMonthButton = new Button("<< Month");
		controlsContainer.addComponent(previousMonthButton);
		previousWeekButton = new Button("< Week");
		controlsContainer.addComponent(previousWeekButton);
		
		dateField = new DateField();
		dateField.setDateFormat("EEE dd/MM/yyyy");
		dateField.setStyleName("monday-date-field");
		controlsContainer.addComponent(dateField);
		
		nextWeekButton = new Button("Week >");
		controlsContainer.addComponent(nextWeekButton);
		nextMonthButton = new Button("Month >>");
		controlsContainer.addComponent(nextMonthButton);
		nextYearButton = new Button("Year >>>");
		controlsContainer.addComponent(nextYearButton);

		// Contribution tables & actions container
		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);
		addComponent(hl);

		/*
		 * Actions container
		 */
		actionsContainer = new VerticalLayout();
		hl.addComponent(actionsContainer);
		
		/*
		 * Contributions table
		 */
		contributionsTable = new Table();
		contributionsTable.setFooterVisible(true);
		contributionsTable.setImmediate(true);
		hl.addComponent(contributionsTable);
		
		// Register the default column view provider
		viewProviders.add(new DefaultColumnProvider());
		// TODO add extension point

		// Register listeners
		previousYearButton.addClickListener(this);
		previousMonthButton.addClickListener(this);
		previousWeekButton.addClickListener(this);
		nextWeekButton.addClickListener(this);
		nextMonthButton.addClickListener(this);
		nextYearButton.addClickListener(this);
		dateField.setImmediate(true);
		dateField.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				Calendar cal = new GregorianCalendar();
				cal.setTime(dateField.getValue());
				logic.onDateChange(cal);
			}
		});
	}

	@Override
	public void registerLogic(IContributionsLogic logic) {
		this.logic = logic;
	}

	@Override
	public void setDate(Calendar date) {
		dateField.setValue(date.getTime());
	}

	@Override
	public void addWeekContribution(long taskId, List<ILogic.IView<?>> cellViews) {
		contributionsTable.addItem(cellViews.toArray(), taskId);
	}

	@Override
	public void removeAllWeekContributions() {
		contributionsTable.removeAllItems();
	}

// TODO delete
//	@Override
//	public void setTaskWeekPrevision(String taskCodePath, String previsionalWeekDuration) {
//		int row = findRow(taskCodePath);
//		Label label = (Label) contributionsContainer.getComponent(10, row);
//		label.setValue(previsionalWeekDuration);
//	}

	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getSource() == previousYearButton) {
			logic.onPreviousYear();
		} else if (event.getSource() == previousMonthButton) {
			logic.onPreviousMonth();
		} else if (event.getSource() == previousWeekButton) {
			logic.onPreviousWeek();
		} else if (event.getSource() == nextWeekButton) {
			logic.onNextWeek();
		} else if (event.getSource() == nextMonthButton) {
			logic.onNextMonth();
		} else if (event.getSource() == nextYearButton) {
			logic.onNextYear();
		}
		else {
			throw new IllegalArgumentException("Unexpected button click");
		}
	}

	@Override
	public void setColumnIdentifiers(List<String> ids) {
		if (contributionsTable.getContainerPropertyIds().size() > 0) {
			throw new IllegalStateException("The contribution table cannot be initialized more than once");
		}
		for (String id : ids) {
			IContributionColumnViewProvider provider = getProvider(id);
			contributionsTable.addContainerProperty(id, provider.getColumnType(id), null);
			contributionsTable.setColumnHeader(id, provider.getLabel(id));
			contributionsTable.setColumnWidth(id, provider.getColumnWidth(id));
		}
	}
	
	private IContributionColumnViewProvider getProvider(String columnId) {
		for (IContributionColumnViewProvider provider : viewProviders) {
			if (provider.isProviderFor(columnId)) {
				return provider;
			}
		}
		throw new IllegalStateException("No provider for column '" + columnId + "'");
	}

	@Override
	public void setColumnFooter(String id, String value) {
		contributionsTable.setColumnFooter(id, value);
	}

	@Override
	public void addAction(View actionView) {
		actionsContainer.addComponent((ActionView) actionView);
	}
}

class DefaultColumnProvider implements IContributionColumnViewProvider {

	private static final Map<String, String> DEFAULT_COLUMN_NAMES = new HashMap<String, String>();
	static {
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProvider.PATH_COLUMN_ID, "Path");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProvider.NAME_COLUMN_ID, "Name");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProvider.MON_COLUMN_ID, "MON");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProvider.TUE_COLUMN_ID, "TUE");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProvider.WED_COLUMN_ID, "WED");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProvider.THU_COLUMN_ID, "THU");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProvider.FRI_COLUMN_ID, "FRI");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProvider.SAT_COLUMN_ID, "SAT");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProvider.SUN_COLUMN_ID, "SUN");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProvider.TOTAL_COLUMN_ID, "Total");
	}
	
	private static final Map<String, Class<?>> DEFAULT_COLUMN_TYPES = new HashMap<String, Class<?>>();
	static {
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProvider.PATH_COLUMN_ID, Label.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProvider.NAME_COLUMN_ID, Label.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProvider.MON_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProvider.TUE_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProvider.WED_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProvider.THU_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProvider.FRI_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProvider.SAT_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProvider.SUN_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProvider.TOTAL_COLUMN_ID, Label.class);
	}

	private static final int DAY_COLUMN_WIDTH = 50;
	private static final Map<String, Integer> DEFAULT_COLUMN_WIDTHS = new HashMap<String, Integer>();
	static {
		DEFAULT_COLUMN_WIDTHS.put(IContributionCellLogicProvider.PATH_COLUMN_ID, 250);
		DEFAULT_COLUMN_WIDTHS.put(IContributionCellLogicProvider.NAME_COLUMN_ID, 250);
	}

	@Override
	public boolean isProviderFor(String columnId) {
		return DEFAULT_COLUMN_NAMES.containsKey(columnId);
	}

	@Override
	public String getLabel(String columnId) {
		return DEFAULT_COLUMN_NAMES.get(columnId);
	}

	@Override
	public Class<?> getColumnType(String columnId) {
		return DEFAULT_COLUMN_TYPES.get(columnId);
	}

	@Override
	public int getColumnWidth(String id) {
		return DEFAULT_COLUMN_WIDTHS.containsKey(id) ? DEFAULT_COLUMN_WIDTHS.get(id) : DAY_COLUMN_WIDTH;
	}
	
	
}