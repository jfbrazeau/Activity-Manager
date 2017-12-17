package org.activitymgr.ui.web.view.impl.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.activitymgr.ui.web.logic.IDownloadButtonLogic;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.logic.ITwinSelectFieldLogic.View;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.dialogs.PopupDateFieldWithParser;
import org.activitymgr.ui.web.view.impl.internal.util.DisableableValueChangeListener;

import com.google.inject.Inject;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.GridLayout.Area;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class ReportsPanel extends AbstractTabPanel<IReportsTabLogic> implements IReportsTabLogic.View {

	private GridLayout bodyComponent;
	private OptionGroup intervalUnitGroup;
	private OptionGroup intervalBoundsModeGroup;
	private PopupDateFieldWithParser startDateField;
	private PopupDateFieldWithParser endDateField;
	private TextField rootTaskTextField;
	private Button browseTaskButton;
	private TextField taskDepthTextField;
	private Component selectedCollaboratorsComponent;
	private Component selectedColumnsComponent;
	private Label statusLabel;
	private Image warningIcon;
	private HorizontalLayout statusLayout;
	private Button buildReportButton;
	private CheckBox onlyKeepTasksWithContribsCheckbox;
	private Button decreaseTaskDepthButton;
	private Button increaseTaskDepthButton;
	private Label taskDepthLayoutDocLabel;
	private OptionGroup collaboratorsModeUnitGroup;
	private Label intervalCountLabel;
	private TextField intervalCountTextField;

	@Inject
	public ReportsPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	protected Component createBodyComponent() {
		bodyComponent = new GridLayout(2, 16);
		bodyComponent.setSpacing(true);
		bodyComponent.setWidth("850px");

		return bodyComponent;
	}

	@Override
	public void initialize(boolean advancedMode) {
		createIntervalConfigurationPanel(bodyComponent);
		createScopeConfigurationPanel(bodyComponent, advancedMode);
		createHeaderColumnsContentConfigurationPanel(bodyComponent,
				advancedMode);
		createRowsContentConfigurationPanel(bodyComponent,
				advancedMode);
		createStatusPanel();
	}

	private void createIntervalConfigurationPanel(GridLayout gl) {
		addTitle(gl, "Interval configuration");

		gl.addComponent(new Label("Interval unit :"));
		intervalUnitGroup = new OptionGroup();
		intervalUnitGroup.setImmediate(true);
		intervalUnitGroup.setStyleName("horizontal");
		gl.addComponent(intervalUnitGroup);
		
		gl.addComponent(new Label("Interval bounds mode :"));
		intervalBoundsModeGroup = new OptionGroup();
		intervalBoundsModeGroup.setImmediate(true);
		intervalBoundsModeGroup.setStyleName("horizontal");
		gl.addComponent(intervalBoundsModeGroup);

		gl.addComponent(new Label("Interval bounds :"));
		HorizontalLayout intervalBoundsPanel = new HorizontalLayout();
		gl.addComponent(intervalBoundsPanel);
		startDateField = newDateField();
		intervalBoundsPanel.addComponent(startDateField);
		endDateField = newDateField();
		intervalBoundsPanel.addComponent(endDateField);
		intervalBoundsPanel
				.addComponent(new Label("(&nbsp;", ContentMode.HTML));
		intervalCountTextField = new TextField();
		intervalCountTextField.setImmediate(true);
		intervalCountTextField.setWidth("40px");
		intervalCountTextField.addStyleName("center");
		intervalBoundsPanel.addComponent(intervalCountTextField);
		intervalCountLabel = new Label();
		intervalBoundsPanel.addComponent(new Label("&nbsp;", ContentMode.HTML));
		intervalBoundsPanel.addComponent(intervalCountLabel);
		intervalBoundsPanel.addComponent(new Label(")"));

		// Register listeners
		intervalUnitGroup
				.addValueChangeListener(new DisableableValueChangeListener() {
			@Override
					public void doValueChange(ValueChangeEvent event) {
				getLogic().onIntervalTypeChanged(event.getProperty().getValue());
			}
		});
		intervalBoundsModeGroup
				.addValueChangeListener(new DisableableValueChangeListener() {
			@Override
					public void doValueChange(ValueChangeEvent event) {
				getLogic().onIntervalBoundsModeChanged(event.getProperty().getValue());
			}
		});
		ValueChangeListener dateBoundsChangeListener = new DisableableValueChangeListener() {
			@Override
			public void doValueChange(ValueChangeEvent event) {
				getLogic().onIntervalBoundsChanged(startDateField.getValue(), endDateField.getValue());
			}
		};
		startDateField.addValueChangeListener(dateBoundsChangeListener);
		endDateField.addValueChangeListener(dateBoundsChangeListener);
		intervalCountTextField
				.addValueChangeListener(new DisableableValueChangeListener() {
					@Override
					public void doValueChange(ValueChangeEvent event) {
						try {
							getLogic().onIntervalCountChanged(
									Integer.parseInt(intervalCountTextField
											.getValue()));
						} catch (NumberFormatException e) {
							getLogic().onIntervalCountChanged(1);
						}
					}
				});
	}

	private void createScopeConfigurationPanel(GridLayout gl,
			boolean advancedMode) {
		addTitle(gl, "Scope configuration");

		gl.addComponent(new Label("Root task :"));
		HorizontalLayout rootTaskPanel = new HorizontalLayout();
		gl.addComponent(rootTaskPanel);
		rootTaskTextField = new TextField();
		rootTaskTextField.setImmediate(true);
		rootTaskTextField.setWidth("300px");
		rootTaskPanel.addComponent(rootTaskTextField);
		browseTaskButton = new Button("...");
		browseTaskButton.setImmediate(true);
		rootTaskPanel.addComponent(browseTaskButton);

		collaboratorsModeUnitGroup = new OptionGroup();
		collaboratorsModeUnitGroup.setImmediate(true);
		collaboratorsModeUnitGroup.setStyleName("horizontal");
		if (advancedMode) {
			gl.addComponent(new Label("Collaborators :"));
			gl.addComponent(collaboratorsModeUnitGroup);
		}

		selectedCollaboratorsComponent = new Label("");
		if (advancedMode) {
			gl.addComponent(new Label(""));
			gl.addComponent(selectedCollaboratorsComponent);
		}

		// Register listeners
		browseTaskButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				getLogic().onBrowseTaskButtonCLicked();
			}
		});
		rootTaskTextField
				.addValueChangeListener(new DisableableValueChangeListener() {
					@Override
					public void doValueChange(ValueChangeEvent event) {
						getLogic().onTaskScopePathChanged(
								(String) event.getProperty().getValue());
					}
				});
		collaboratorsModeUnitGroup
				.addValueChangeListener(new DisableableValueChangeListener() {
					@Override
					public void doValueChange(ValueChangeEvent event) {
						getLogic().onCollaboratorsSelectionModeChanged(
								event.getProperty().getValue());
					}
				});
	}

	private void createHeaderColumnsContentConfigurationPanel(GridLayout gl,
			boolean advancedMode) {
		if (advancedMode) {
			addTitle(gl, "Header columns content configuration");
		}
		selectedColumnsComponent = new Label("");
		if (advancedMode) {
			gl.addComponent(new Label("Fields :"));
			gl.addComponent(selectedColumnsComponent);
		}
	}

	private void createRowsContentConfigurationPanel(GridLayout gl,
			boolean advancedMode) {
		addTitle(gl, "Rows content configuration");

		gl.addComponent(new Label("Task tree depth :"));
		HorizontalLayout taskDepthLayout = new HorizontalLayout();
		gl.addComponent(taskDepthLayout);
		decreaseTaskDepthButton = new Button("-");
		decreaseTaskDepthButton.setImmediate(true);
		taskDepthLayout.addComponent(decreaseTaskDepthButton);
		taskDepthTextField = new TextField();
		taskDepthTextField.setImmediate(true);
		taskDepthTextField.setWidth("40px");
		taskDepthTextField.addStyleName("center");
		taskDepthLayout.addComponent(taskDepthTextField);
		increaseTaskDepthButton = new Button("+");
		increaseTaskDepthButton.setImmediate(true);
		taskDepthLayout.addComponent(increaseTaskDepthButton);
		taskDepthLayoutDocLabel = new Label(
				" (from root ; deeper contributions will be aggregated)");
		taskDepthLayout.addComponent(taskDepthLayoutDocLabel);
		taskDepthLayout.setComponentAlignment(taskDepthLayoutDocLabel, Alignment.MIDDLE_LEFT);

		onlyKeepTasksWithContribsCheckbox = new CheckBox(
				"Don't show rows for task that have no contribution");
		if (advancedMode) {
			gl.addComponent(new Label("Filter empty tasks rows :"));
			gl.addComponent(onlyKeepTasksWithContribsCheckbox);
		}

		decreaseTaskDepthButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				increaseOrDecreaseTaskTreeDepth(-1);
			}
		});
		taskDepthTextField
				.addValueChangeListener(new DisableableValueChangeListener() {
					@Override
					public void doValueChange(ValueChangeEvent event) {
						try {
							getLogic().onTaskTreeDepthChanged(
									Integer.parseInt(taskDepthTextField
											.getValue()));
						} catch (NumberFormatException e) {
							getLogic().onTaskTreeDepthChanged(0);
						}
					}
				});
		increaseTaskDepthButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				increaseOrDecreaseTaskTreeDepth(1);
			}
		});
		onlyKeepTasksWithContribsCheckbox
				.addValueChangeListener(new DisableableValueChangeListener() {
					@Override
					public void doValueChange(ValueChangeEvent event) {
						getLogic()
								.onOnlyKeepTaskWithContributionsCheckboxChanged(
										onlyKeepTasksWithContribsCheckbox
												.getValue());
					}
				});
	}

	private void createStatusPanel() {
		bodyComponent.addComponent(new Label("")); // Empty cell
		statusLayout = new HorizontalLayout();
		bodyComponent.addComponent(statusLayout); // Empty cell
		warningIcon = new Image(null, getResourceCache().getResource(
				"warning.gif"));
		statusLayout.addComponent(warningIcon);
		warningIcon.setVisible(false);
		statusLayout.setComponentAlignment(warningIcon, Alignment.MIDDLE_RIGHT);
		statusLabel = new Label("");
		statusLayout.addComponent(statusLabel);
		statusLayout.setComponentAlignment(statusLabel, Alignment.MIDDLE_RIGHT);
	}

	private void increaseOrDecreaseTaskTreeDepth(int amount) {
		try {
			int actual = Integer.parseInt(taskDepthTextField.getValue());
			taskDepthTextField.setValue(String.valueOf(actual + amount));
		} catch (NumberFormatException e) {
			taskDepthTextField.setValue("");
		}
	}

	@Override
	public void setColumnSelectionView(View view) {
		Component newComponent = (Component) view;
		substituteBodyComponent(selectedColumnsComponent, newComponent);
		selectedColumnsComponent = newComponent;
	}

	@Override
	public void setCollaboratorsSelectionView(View view) {
		Component newComponent = (Component) view;
		substituteBodyComponent(selectedCollaboratorsComponent,
				newComponent);
		selectedCollaboratorsComponent = newComponent;
	}

	@Override
	public void setBuildReportButtonView(IDownloadButtonLogic.View view) {
		buildReportButton = (Button) view;
		statusLayout.addComponent(buildReportButton, 0);
	}

	private void substituteBodyComponent(Component componentToSubstitute,
			Component newComponent) {
		Area area = bodyComponent.getComponentArea(componentToSubstitute);
		bodyComponent.removeComponent(componentToSubstitute);
		bodyComponent.addComponent(newComponent,
				area.getColumn1(), area.getRow1(), area.getColumn2(),
				area.getRow2());
	}

	private void addTitle(GridLayout gl, String caption) {
		Label label = new Label("<b>" + caption + "</b><hr>", ContentMode.HTML);
		label.setWidth("100%");
		addComponentWithHorizontalSpan(gl, label);
	}

	private void addComponentWithHorizontalSpan(GridLayout gl,
			Component component) {
		gl.addComponent(component);
		Area area = gl.getComponentArea(component);
		gl.removeComponent(component);
		gl.addComponent(component, area.getColumn1(), area.getRow1(),
				area.getColumn2() + 1, area.getRow2());
	}

	private PopupDateFieldWithParser newDateField() {
		PopupDateFieldWithParser startDateField = new PopupDateFieldWithParser();
		startDateField.setImmediate(true);
		startDateField.setDateFormat("E dd/MM/yyyy");
		startDateField.setShowISOWeekNumbers(true);
		startDateField.setStyleName("monday-date-field");
		startDateField.setValue(new Date());
		return startDateField;
	}

	@Override
	public void addIntervalTypeRadioButton(Object id, String label) {
		addCheckboxToGroup(intervalUnitGroup, id, label);
	}
	
	@Override
	public void selectIntervalTypeRadioButton(Object id) {
		setFieldValueSilently(intervalUnitGroup, id);
		updateIntervalCountLabel();
	}

	private void updateIntervalCountLabel() {
		boolean plural = false;
		try {
			plural = Integer.parseInt(intervalCountTextField.getValue()) > 1;
		} catch (NumberFormatException ignored) {
		}
		intervalCountLabel.setValue(" "
				+ intervalUnitGroup.getValue().toString().toLowerCase()
				+ (plural ? "s" : ""));
	}

	@Override
	public void addIntervalBoundsModeRadioButton(Object id, String label) {
		addCheckboxToGroup(intervalBoundsModeGroup, id, label);
	}

	@Override
	public void selectIntervalBoundsModeButton(Object id) {
		setFieldValueSilently(intervalBoundsModeGroup, id);
	}

	@Override
	public void addCollaboratorsSelectionModeRadioButton(Object id, String label) {
		addCheckboxToGroup(collaboratorsModeUnitGroup, id, label);
	}

	private static void addCheckboxToGroup(OptionGroup group, Object id, String label) {
		group.addItem(id);
		group.setItemCaption(id, label);
	}

	@Override
	public void setIntervalBoundsModeEnablement(boolean startDateEnablement, boolean endDateEnablement) {
		startDateField.setEnabled(startDateEnablement);
		endDateField.setEnabled(endDateEnablement);
		intervalCountTextField.setEnabled(endDateEnablement);
	}
 
	@Override
	public void setIntervalBounds(Date startDate, Date endDate) {
		setFieldValueSilently(startDateField, startDate);
		setFieldValueSilently(endDateField, endDate);
	}

	@Override
	public void setIntervalCount(int intervalCount) {
		setFieldValueSilently(intervalCountTextField,
				String.valueOf(intervalCount));
		updateIntervalCountLabel();
	}

	@Override
	public void setTaskScopePath(String path) {
		setFieldValueSilently(rootTaskTextField, path);
	}

	@Override
	public void setTaskTreeDepth(int i) {
		setFieldValueSilently(taskDepthTextField, String.valueOf(i));
	}

	@Override
	public void setBuildReportButtonEnabled(boolean enabled) {
		buildReportButton.setEnabled(enabled);
	}

	@Override
	public void setErrorMessage(String message) {
		if (message != null && !"".equals(message.trim())) {
			warningIcon.setVisible(true);
			statusLabel.setValue(message);
		} else {
			warningIcon.setVisible(false);
			statusLabel.setValue("");
		}
	}

	@Override
	public void setRowContentConfigurationEnabled(boolean enabled) {
		decreaseTaskDepthButton.setEnabled(enabled);
		taskDepthTextField.setEnabled(enabled);
		increaseTaskDepthButton.setEnabled(enabled);
		taskDepthLayoutDocLabel.setEnabled(enabled);
		onlyKeepTasksWithContribsCheckbox.setEnabled(enabled);
	}

	@Override
	public void setCollaboratorsSelectionUIEnabled(boolean enabled) {
		selectedCollaboratorsComponent.setEnabled(enabled);
	}

	@Override
	public void selectCollaboratorsSelectionModeRadioButton(Object newValue) {
		setFieldValueSilently(collaboratorsModeUnitGroup, newValue);
	}

	private <T> void setFieldValueSilently(AbstractField<T> field, T newValue) {
		Collection<?> listeners = field.getListeners(ValueChangeEvent.class);
		Collection<DisableableValueChangeListener> disabledListeners = new ArrayList<DisableableValueChangeListener>();
		try {
			// Disable listeners
			for (Object listener : listeners) {
				if (listener instanceof DisableableValueChangeListener) {
					DisableableValueChangeListener dListener = (DisableableValueChangeListener) listener;
					dListener.setEnabled(false);
					disabledListeners.add(dListener);
				}
			}
			field.setValue(newValue);
		} finally {
			// Reanable listeners
			for (DisableableValueChangeListener disabledListener : disabledListeners) {
				disabledListener.setEnabled(true);
			}
		}
	}

}
