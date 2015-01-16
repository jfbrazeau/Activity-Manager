package org.activitymgr.ui.web.view.impl.internal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.TableDatasource;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.ContainerOrderedWrapper;
import com.vaadin.data.util.converter.Converter;
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

	private Button previousYearButton;

	private Button previousMonthButton;

	private Button previousWeekButton;

	private Button nextWeekButton;

	private Button nextMonthButton;

	private Button nextYearButton;

	private Table contributionsTable;

	private Table collaboratorsTable;

	private ITableCellProviderCallback<Long> contributionsProvider;

	public ContributionsPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	protected Component createHeaderComponent() {
		GridLayout controlsContainer = new GridLayout(8, 1);
		addComponent(controlsContainer);
		Label emptyLabel = new Label();
		emptyLabel.setWidth(250, Unit.PIXELS);
		controlsContainer.addComponent(emptyLabel);
		previousYearButton = new Button("<<< Year");
		previousYearButton.setDescription("Ctrl+Shift+Alt+Left");
		controlsContainer.addComponent(previousYearButton);
		previousMonthButton = new Button("<< Month");
		previousMonthButton.setDescription("Ctrl+Shift+Left");
		controlsContainer.addComponent(previousMonthButton);
		previousWeekButton = new Button("< Week");
		previousWeekButton.setDescription("Ctrl+Left");
		controlsContainer.addComponent(previousWeekButton);
		
		dateField = new PopupDateField() {
			@Override
			protected Date handleUnparsableDateString(String dateString)
		            throws Converter.ConversionException {
				try {
					int idx = dateString.indexOf(' ');
					if (idx > 0) {
						dateString = dateString.substring(idx);
					}
					SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
					return sdf.parse(dateString);
				} catch (ParseException ignore) {
					return new Date();
				}
		    }
		};
		dateField.setImmediate(true);
		dateField.setDateFormat("E dd/MM/yyyy");
		dateField.setStyleName("monday-date-field");
		controlsContainer.addComponent(dateField);
		
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
	
	@Override
	protected Component createLeftComponent() {
		/*
		 * Collaborators table
		 */
		collaboratorsTable = new Table();
		collaboratorsTable.setSelectable(true);
		collaboratorsTable.setImmediate(true);
		collaboratorsTable.setNullSelectionAllowed(false);
		return collaboratorsTable;
	}

	@Override
	protected Component createBodyComponent() {
		/*
		 * Contributions table
		 */
		contributionsTable = new Table();
		contributionsTable.setFooterVisible(true);
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
		int tableWidth = 20;
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			contributionsTable.addGeneratedColumn(propertyId, new Table.ColumnGenerator() {
				@Override
				public Object generateCell(Table source, Object itemId, Object propertyId) {
					return contributionsProvider.getCell((Long) itemId, (String) propertyId);
				}
			});
			int columnWidth = contributionsProvider.getColumnWidth(propertyId);
			tableWidth += columnWidth + 10;
			contributionsTable.setColumnWidth(propertyId, columnWidth);
		}
		contributionsTable.setWidth(tableWidth + "px");
	}
	
	@Override
	public void reloadContributionTableItems() {
		((ContainerOrderedWrapper)contributionsTable.getContainerDataSource()).updateOrderWrapper();
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
		if (event.getSource() == previousYearButton) {
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
		int tableWidth = 20;
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

}
