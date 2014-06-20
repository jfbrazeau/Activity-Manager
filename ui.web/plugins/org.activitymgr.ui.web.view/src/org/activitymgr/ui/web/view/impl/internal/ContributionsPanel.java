package org.activitymgr.ui.web.view.impl.internal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.IContributionsTabLogic.ICollaborator;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.impl.IContributionCellLogicProviderExtension;
import org.activitymgr.ui.web.view.IContributionColumnViewProviderExtension;
import org.activitymgr.ui.web.view.IResourceCache;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

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
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ContributionsPanel extends VerticalLayout implements IContributionsTabLogic.View, Button.ClickListener {

	private IContributionsTabLogic logic;
	
	private IResourceCache resourceCache;

	private PopupDateField dateField;

	private Button previousYearButton;

	private Button previousMonthButton;

	private Button previousWeekButton;

	private Button nextWeekButton;

	private Button nextMonthButton;

	private Button nextYearButton;

	private List<IContributionColumnViewProviderExtension> viewProviders = new ArrayList<IContributionColumnViewProviderExtension>();;

	private Table contributionsTable;

	private VerticalLayout actionsContainer;

	private Table collaboratorsTable;

	private List<ShortcutListener> actions = new ArrayList<ShortcutListener>();

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
		collaboratorsTable.addContainerProperty("FNAME", String.class, null);
		collaboratorsTable.setColumnHeader("FNAME", "Fist name");
		collaboratorsTable.setColumnWidth("FNAME", 70);
		collaboratorsTable.addContainerProperty("LNAME", String.class, null);
		collaboratorsTable.setColumnHeader("LNAME", "Last name");
		collaboratorsTable.setColumnWidth("LNAME", 70);
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
		
		// Register the default column view provider
		viewProviders.add(new DefaultColumnProvider());
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.view.contributionColumnViewProvider");
		for (IConfigurationElement cfg : cfgs) {
			try {
				viewProviders.add((IContributionColumnViewProviderExtension) cfg.createExecutableExtension("class"));
			} catch (CoreException e) {
				throw new IllegalStateException("Unable to load view provider '" + cfg.getAttribute("class") + "'", e);
			}
		}

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
				logic.onSelectedCollaboratorChanged((String) collaboratorsTable.getValue());
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
	public void setDate(Calendar date) {
		dateField.setValue(date.getTime());
	}

	@Override
	public void addWeekContribution(long taskId, List<ILogic.IView<?>> cellViews) {
		contributionsTable.addItem(cellViews.toArray(), taskId);
		// TODO enhance sort management
		contributionsTable.sort(new Object[] { IContributionCellLogicProviderExtension.PATH_COLUMN_ID }, new boolean[] { true });
	}

	@Override
	public void removeAllWeekContributions() {
		contributionsTable.removeAllItems();
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
	public void setColumnIdentifiers(List<String> ids) {
		if (contributionsTable.getContainerPropertyIds().size() > 0) {
			throw new IllegalStateException("The contribution table cannot be initialized more than once");
		}
		for (String id : ids) {
			IContributionColumnViewProviderExtension provider = getProvider(id);
			contributionsTable.addContainerProperty(id, provider.getColumnType(id), null);
			contributionsTable.setColumnHeader(id, provider.getLabel(id));
			contributionsTable.setColumnWidth(id, provider.getColumnWidth(id));
		}
	}
	
	private IContributionColumnViewProviderExtension getProvider(String columnId) {
		for (IContributionColumnViewProviderExtension provider : viewProviders) {
			if (provider.isProviderFor(columnId)) {
				return provider;
			}
		}
		throw new IllegalStateException("No view provider for column '" + columnId + "'");
	}

	@Override
	public void setColumnFooter(String id, String value) {
		contributionsTable.setColumnFooter(id, value);
	}

	@Override
	public void setCollaborators(List<ICollaborator> collaborators) {
		collaboratorsTable.removeAllItems();
		for (ICollaborator col : collaborators) {
			collaboratorsTable.addItem(new Object[] { col.getFirstName(), col.getLastName() }, col.getLogin());
		}
	}

	@Override
	public void selectCollaborator(String login) {
		collaboratorsTable.select(login);
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
		Resource iconResource = resourceCache.getResource(iconId + ".gif");
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

	@Override
	public void focus() {
		super.focus();
	}
}

class DefaultColumnProvider implements IContributionColumnViewProviderExtension {

	private static final Map<String, String> DEFAULT_COLUMN_NAMES = new HashMap<String, String>();
	static {
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProviderExtension.PATH_COLUMN_ID, "Path");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProviderExtension.NAME_COLUMN_ID, "Name");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProviderExtension.MON_COLUMN_ID, "MON");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProviderExtension.TUE_COLUMN_ID, "TUE");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProviderExtension.WED_COLUMN_ID, "WED");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProviderExtension.THU_COLUMN_ID, "THU");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProviderExtension.FRI_COLUMN_ID, "FRI");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProviderExtension.SAT_COLUMN_ID, "SAT");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProviderExtension.SUN_COLUMN_ID, "SUN");
		DEFAULT_COLUMN_NAMES.put(IContributionCellLogicProviderExtension.TOTAL_COLUMN_ID, "Total");
	}
	
	private static final Map<String, Class<?>> DEFAULT_COLUMN_TYPES = new HashMap<String, Class<?>>();
	static {
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProviderExtension.PATH_COLUMN_ID, Label.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProviderExtension.NAME_COLUMN_ID, Label.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProviderExtension.MON_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProviderExtension.TUE_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProviderExtension.WED_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProviderExtension.THU_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProviderExtension.FRI_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProviderExtension.SAT_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProviderExtension.SUN_COLUMN_ID, TextField.class);
		DEFAULT_COLUMN_TYPES.put(IContributionCellLogicProviderExtension.TOTAL_COLUMN_ID, Label.class);
	}

	private static final int DAY_COLUMN_WIDTH = 40;
	private static final Map<String, Integer> DEFAULT_COLUMN_WIDTHS = new HashMap<String, Integer>();
	static {
		DEFAULT_COLUMN_WIDTHS.put(IContributionCellLogicProviderExtension.PATH_COLUMN_ID, 250);
		DEFAULT_COLUMN_WIDTHS.put(IContributionCellLogicProviderExtension.NAME_COLUMN_ID, 150);
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