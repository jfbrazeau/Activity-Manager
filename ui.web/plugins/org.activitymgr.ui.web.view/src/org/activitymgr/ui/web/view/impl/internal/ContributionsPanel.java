package org.activitymgr.ui.web.view.impl.internal;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.dialogs.PopupDateFieldWithParser;
import org.activitymgr.ui.web.view.impl.internal.util.AlignHelper;
import org.activitymgr.ui.web.view.impl.internal.util.TableDatasource;

import com.google.inject.Inject;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PopupDateField;
import com.vaadin.ui.Table;

@SuppressWarnings("serial")
public class ContributionsPanel extends AbstractTabPanel<IContributionsTabLogic> implements IContributionsTabLogic.View, Button.ClickListener {

	private PopupDateField dateField;

	private Button selectMeButton;

	private Button todayButton;

	private Button previousYearButton;

	private Button previousMonthButton;

	private Button previousWeekButton;

	private Button nextWeekButton;

	private Button nextMonthButton;

	private Button nextYearButton;

	private Table contributionsTable;

	private Table collaboratorsTable;

	private ITableCellProviderCallback<Long> contributionsProvider;

	@Inject
	public ContributionsPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	protected Component createHeaderComponent() {
		GridLayout controlsContainer = new GridLayout(12, 1);
		addComponent(controlsContainer);
		selectMeButton = new Button("Me");
		controlsContainer.addComponent(selectMeButton);

		appendEmptyLabel(controlsContainer, 200);
		
		previousYearButton = new Button("<<< Year");
		previousYearButton.setDescription("Ctrl+Shift+Alt+Left");
		controlsContainer.addComponent(previousYearButton);
		previousMonthButton = new Button("<< Month");
		previousMonthButton.setDescription("Ctrl+Shift+Left");
		controlsContainer.addComponent(previousMonthButton);
		previousWeekButton = new Button("< Week");
		previousWeekButton.setDescription("Ctrl+Left");
		controlsContainer.addComponent(previousWeekButton);

		appendEmptyLabel(controlsContainer, 20);
		
		todayButton = new Button("Today");
		controlsContainer.addComponent(todayButton);
		dateField = new PopupDateFieldWithParser();
		dateField.setImmediate(true);
		dateField.setDateFormat("E dd/MM/yyyy");
		dateField.setShowISOWeekNumbers(true);
		dateField.setStyleName("monday-date-field");
		controlsContainer.addComponent(dateField);
		
		appendEmptyLabel(controlsContainer, 20);

		nextWeekButton = new Button("Week >");
		nextWeekButton.setDescription("Ctrl+Right");
		controlsContainer.addComponent(nextWeekButton);
		nextMonthButton = new Button("Month >>");
		nextMonthButton.setDescription("Ctrl+Shift+Right");
		controlsContainer.addComponent(nextMonthButton);
		nextYearButton = new Button("Year >>>");
		nextYearButton.setDescription("Ctrl+Shift+Alt+Right");
		controlsContainer.addComponent(nextYearButton);
		return controlsContainer;
	}

	private void appendEmptyLabel(GridLayout container, int width) {
		Label emptyLabel = new Label();
		emptyLabel.setWidth(width, Unit.PIXELS);
		container.addComponent(emptyLabel);
	}
	
	@Override
	protected Component createLeftComponent() {
		/*
		 * Collaborators table
		 */
		collaboratorsTable = new Table();
		collaboratorsTable.setSelectable(true);
		collaboratorsTable.setImmediate(true);
		collaboratorsTable.setNullSelectionAllowed(false);
		collaboratorsTable.setSizeFull();
		return collaboratorsTable;
	}

	@Override
	protected Component createBodyComponent() {
		/*
		 * Contributions table
		 */
		contributionsTable = new Table();
		contributionsTable.setFooterVisible(true);
		contributionsTable.setSizeFull();
		return contributionsTable;
	}

	@Override
	public void registerLogic(final IContributionsTabLogic logic) {
		super.registerLogic(logic);
		registerListeners();
	}

	private void registerListeners() {
		collaboratorsTable.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				getLogic().onSelectedCollaboratorChanged((Long) collaboratorsTable.getValue());
			}
		});
		todayButton.addClickListener(this);
		selectMeButton.addClickListener(this);
		previousYearButton.addClickListener(this);
		previousMonthButton.addClickListener(this);
		previousWeekButton.addClickListener(this);
		nextWeekButton.addClickListener(this);
		nextMonthButton.addClickListener(this);
		nextYearButton.addClickListener(this);
		dateField.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				Calendar cal = new GregorianCalendar();
				cal.setTime(dateField.getValue()!= null ? dateField.getValue() : new Date());
				getLogic().onDateChange(cal);
			}
		});
		addShortcutListener(new ShortcutListener("Previous year",
				ShortcutListener.KeyCode.ARROW_LEFT,
				new int[] { ShortcutListener.ModifierKey.CTRL, ShortcutListener.ModifierKey.SHIFT, ShortcutListener.ModifierKey.ALT }) {
			@Override
			public void handleAction(Object sender, Object target) {
				getLogic().onPreviousYear();
			}
		});
		addShortcutListener(new ShortcutListener("Previous month",
				ShortcutListener.KeyCode.ARROW_LEFT,
				new int[] { ShortcutListener.ModifierKey.CTRL, ShortcutListener.ModifierKey.SHIFT }) {
			@Override
			public void handleAction(Object sender, Object target) {
				getLogic().onPreviousMonth();
			}
		});
		addShortcutListener(new ShortcutListener("Previous week",
				ShortcutListener.KeyCode.ARROW_LEFT,
				new int[] { ShortcutListener.ModifierKey.CTRL }) {
			@Override
			public void handleAction(Object sender, Object target) {
				getLogic().onPreviousWeek();
			}
		});
		addShortcutListener(new ShortcutListener("Next week",
				ShortcutListener.KeyCode.ARROW_RIGHT,
				new int[] { ShortcutListener.ModifierKey.CTRL }) {
			@Override
			public void handleAction(Object sender, Object target) {
				getLogic().onNextWeek();
			}
		});
		addShortcutListener(new ShortcutListener("Next month",
				ShortcutListener.KeyCode.ARROW_RIGHT,
				new int[] { ShortcutListener.ModifierKey.CTRL, ShortcutListener.ModifierKey.SHIFT }) {
			@Override
			public void handleAction(Object sender, Object target) {
				getLogic().onNextMonth();
			}
		});
		addShortcutListener(new ShortcutListener("Next year",
				ShortcutListener.KeyCode.ARROW_RIGHT,
				new int[] { ShortcutListener.ModifierKey.CTRL, ShortcutListener.ModifierKey.SHIFT, ShortcutListener.ModifierKey.ALT  }) {
			@Override
			public void handleAction(Object sender, Object target) {
				getLogic().onNextYear();
			}
		});
	}

	@Override
	public void setContributionsProvider(
			final ITableCellProviderCallback<Long> contributionsProvider) {
		this.contributionsProvider = contributionsProvider;
		TableDatasource<Long> dataSource = new TableDatasource<Long>(getResourceCache(), contributionsProvider);
		contributionsTable.setContainerDataSource(dataSource);
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			contributionsTable.addGeneratedColumn(propertyId, new Table.ColumnGenerator() {
				@Override
				public Object generateCell(Table source, Object itemId, Object propertyId) {
					return contributionsProvider.getCell((Long) itemId, (String) propertyId);
				}
			});
			int columnWidth = contributionsProvider.getColumnWidth(propertyId);
			contributionsTable.setColumnWidth(propertyId, columnWidth);
			contributionsTable.setColumnAlignment(propertyId, AlignHelper.toVaadinAlign(contributionsProvider.getColumnAlign(propertyId)));
		}
	}
	
	@Override
	public void reloadContributionTableItems() {
		contributionsTable.refreshRowCache();
		reloadContributionTableFooter();
	}

	@Override
	public void reloadContributionTableFooter() {
		for (String columnId : contributionsProvider.getPropertyIds()) {
			contributionsTable.setColumnFooter(columnId, contributionsProvider.getFooter(columnId));
		}
	}

	@Override
	public void setDate(Calendar date) {
		dateField.setValue(date.getTime());
	}

	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getSource() == selectMeButton) {
			getLogic().onSelectMe();
		} else if (event.getSource() == todayButton) {
			getLogic().onToday();
		} else if (event.getSource() == previousYearButton) {
			getLogic().onPreviousYear();
		} else if (event.getSource() == previousMonthButton) {
			getLogic().onPreviousMonth();
		} else if (event.getSource() == previousWeekButton) {
			getLogic().onPreviousWeek();
		} else if (event.getSource() == nextWeekButton) {
			getLogic().onNextWeek();
		} else if (event.getSource() == nextMonthButton) {
			getLogic().onNextMonth();
		} else if (event.getSource() == nextYearButton) {
			getLogic().onNextYear();
		}
		else {
			throw new IllegalArgumentException("Unexpected button click");
		}
	}

	@Override
	public void setCollaboratorsProvider(
			final ITableCellProviderCallback<Long> collaboratorsProvider) {
		TableDatasource<Long> dataSource = new TableDatasource<Long>(getResourceCache(), collaboratorsProvider);
		collaboratorsTable.setContainerDataSource(dataSource);
		int tableWidth = 10;
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			collaboratorsTable.addGeneratedColumn(propertyId, new Table.ColumnGenerator() {
				@Override
				public Object generateCell(Table source, Object itemId, Object propertyId) {
					return collaboratorsProvider.getCell((Long) itemId, (String) propertyId);
				}
			});
			int columnWidth = collaboratorsProvider.getColumnWidth(propertyId);
			tableWidth += columnWidth + 10;
			collaboratorsTable.setColumnWidth(propertyId, columnWidth);
			collaboratorsTable.setColumnAlignment(propertyId, AlignHelper.toVaadinAlign(collaboratorsProvider.getColumnAlign(propertyId)));
		}
		collaboratorsTable.setWidth(tableWidth + "px");
	}

	@Override
	public void selectCollaborator(final long collaboratorId) {
		collaboratorsTable.select(collaboratorId);
		collaboratorsTable.focus();
	}

	@Override
	public void focus() {
		super.focus();
	}

	@Override
	public void setColumnTitle(String propertyId, String title) {
		contributionsTable.setColumnHeader(propertyId, title);
	}

}
