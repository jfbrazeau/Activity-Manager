package org.activitymgr.ui.web.view.impl.internal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.TableDatasource;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.event.Action;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PopupDateField;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ContributionsPanel extends VerticalLayout implements IContributionsTabLogic.View, Button.ClickListener {

	private IContributionsTabLogic logic;
	
	private PopupDateField dateField;

	private Button previousYearButton;

	private Button previousMonthButton;

	private Button previousWeekButton;

	private Button nextWeekButton;

	private Button nextMonthButton;

	private Button nextYearButton;

	private Table contributionsTable;

	private VerticalLayout actionsContainer;

	private Table collaboratorsTable;

	private List<ShortcutListener> actions = new ArrayList<ShortcutListener>();

	private IResourceCache resourceCache;

	private ITableCellProviderCallback<Long> contributionsProvider;

	public ContributionsPanel(IResourceCache resourceCache) {
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

		// Collaborators list, contribution tables & actions container
		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);
		addComponent(hl);

		/*
		 * Actions container
		 */
		collaboratorsTable = new Table();
		hl.addComponent(collaboratorsTable);
		collaboratorsTable.setSelectable(true);
		collaboratorsTable.setImmediate(true);
		collaboratorsTable.setNullSelectionAllowed(false);
		collaboratorsTable.setHeight("500px");

		/*
		 * Contributions table
		 */
		contributionsTable = new Table();
		hl.addComponent(contributionsTable);
		contributionsTable.setFooterVisible(true);
		contributionsTable.setHeight("500px");
		contributionsTable.setWidth("1050px");
		
		/*
		 * Actions container
		 */
		actionsContainer = new VerticalLayout();
		hl.addComponent(actionsContainer);
		
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
				cal.setTime(dateField.getValue()!= null ? dateField.getValue() : new Date());
				logic.onDateChange(cal);
			}
		});
		collaboratorsTable.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				logic.onSelectedCollaboratorChanged((Long) collaboratorsTable.getValue());
			}
		});

		// Register action handler		
		contributionsTable.addActionHandler(new Action.Handler() {
			@Override
			public void handleAction(Action action, Object sender, Object target) {
				((ShortcutListener) action).handleAction(sender, target);
			}
			@Override
			public Action[] getActions(Object target, Object sender) {
				return (Action[]) actions.toArray(new Action[actions.size()]);
			}
		});

		// Register keyboard shortcut listeners
		addShortcutListener(new ShortcutListener("Previous year",
				ShortcutListener.KeyCode.ARROW_LEFT,
				new int[] { ShortcutListener.ModifierKey.CTRL, ShortcutListener.ModifierKey.SHIFT, ShortcutListener.ModifierKey.ALT }) {
			@Override
			public void handleAction(Object sender, Object target) {
				logic.onPreviousYear();
			}
		});
		addShortcutListener(new ShortcutListener("Previous month",
				ShortcutListener.KeyCode.ARROW_LEFT,
				new int[] { ShortcutListener.ModifierKey.CTRL, ShortcutListener.ModifierKey.SHIFT }) {
			@Override
			public void handleAction(Object sender, Object target) {
				logic.onPreviousMonth();
			}
		});
		addShortcutListener(new ShortcutListener("Previous week",
				ShortcutListener.KeyCode.ARROW_LEFT,
				new int[] { ShortcutListener.ModifierKey.CTRL }) {
			@Override
			public void handleAction(Object sender, Object target) {
				logic.onPreviousWeek();
			}
		});
		addShortcutListener(new ShortcutListener("Next week",
				ShortcutListener.KeyCode.ARROW_RIGHT,
				new int[] { ShortcutListener.ModifierKey.CTRL }) {
			@Override
			public void handleAction(Object sender, Object target) {
				logic.onNextWeek();
			}
		});
		addShortcutListener(new ShortcutListener("Next month",
				ShortcutListener.KeyCode.ARROW_RIGHT,
				new int[] { ShortcutListener.ModifierKey.CTRL, ShortcutListener.ModifierKey.SHIFT }) {
			@Override
			public void handleAction(Object sender, Object target) {
				logic.onNextMonth();
			}
		});
		addShortcutListener(new ShortcutListener("Next year",
				ShortcutListener.KeyCode.ARROW_RIGHT,
				new int[] { ShortcutListener.ModifierKey.CTRL, ShortcutListener.ModifierKey.SHIFT, ShortcutListener.ModifierKey.ALT  }) {
			@Override
			public void handleAction(Object sender, Object target) {
				logic.onNextYear();
			}
		});
	}

	@Override
	public void registerLogic(IContributionsTabLogic logic) {
		this.logic = logic;
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
			Integer columnWidth = contributionsProvider.getColumnWidth(propertyId);
			if (columnWidth != null) {
				contributionsTable.setColumnWidth(propertyId, columnWidth);
			}
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
	public void setCollaboratorsProvider(
			final ITableCellProviderCallback<Long> collaboratorsProvider) {
		TableDatasource<Long> dataSource = new TableDatasource<Long>(getResourceCache(), collaboratorsProvider);
		collaboratorsTable.setContainerDataSource(dataSource);
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			collaboratorsTable.addGeneratedColumn(propertyId, new Table.ColumnGenerator() {
				@Override
				public Object generateCell(Table source, Object itemId, Object propertyId) {
					return collaboratorsProvider.getCell((Long) itemId, (String) propertyId);
				}
			});
			Integer columnWidth = collaboratorsProvider.getColumnWidth(propertyId);
			if (columnWidth != null) {
				collaboratorsTable.setColumnWidth(propertyId, columnWidth);
			}
		}
	}

	@Override
	public void selectCollaborator(final long collaboratorId) {
		collaboratorsTable.select(collaboratorId);
		collaboratorsTable.focus();
	}

	@Override
	public void addAction(final String actionId, final String label, final String keyBindingDescription, final String iconId, final char key,
			final boolean ctrl, final boolean shift, final boolean alt) {
		// TODO Use standard views ?
		int[] rawModifiers = new int[3];
		int i = 0;
		if (ctrl)
			rawModifiers[i++] = ShortcutListener.ModifierKey.CTRL;
		if (shift)
			rawModifiers[i++] = ShortcutListener.ModifierKey.SHIFT;
		if (alt)
			rawModifiers[i++] = ShortcutListener.ModifierKey.ALT;
		int[] modifiers = new int[i];
		System.arraycopy(rawModifiers, 0, modifiers, 0, i);
		Resource iconResource = getResourceCache().getResource(iconId + ".gif");
		String caption = label + " <em>"
				+ keyBindingDescription + "</em>";
		ShortcutListener newAction = new ShortcutListener(caption,
				iconResource, key, modifiers) {
			@Override
			public void handleAction(Object sender, Object target) {
				logic.onAction(actionId);
			}
		};
		actions.add(newAction);
		addShortcutListener(newAction);
		Button button = new Button();
		button.setIcon(iconResource);
		button.setDescription(caption);
		actionsContainer.addComponent(button);
		button.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				logic.onAction(actionId);
			}
		});
	}

	protected IResourceCache getResourceCache() {
		return resourceCache;
	}

	@Override
	public void focus() {
		super.focus();
	}

}
