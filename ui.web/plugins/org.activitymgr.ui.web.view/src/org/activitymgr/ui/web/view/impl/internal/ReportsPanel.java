package org.activitymgr.ui.web.view.impl.internal;

import java.util.Date;

import org.activitymgr.ui.web.logic.IDownloadButtonLogic;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.logic.ITwinSelectLogic.View;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.dialogs.PopupDateFieldWithParser;

import com.google.inject.Inject;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
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

	@Inject
	public ReportsPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	protected Component createBodyComponent() {
		bodyComponent = new GridLayout(2, 16);
		bodyComponent.setSpacing(true);
		bodyComponent.setWidth("850px");
		
		createIntervalConfigurationPanel(bodyComponent);
		createScopeConfigurationPanel(bodyComponent);
		createColumnsConfigurationPanel(bodyComponent);
		createStatusPanel();

		return bodyComponent;
	}

	private void createIntervalConfigurationPanel(GridLayout gl) {
		addTitle(gl, "Interval configuration", 0);

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
		intervalBoundsPanel.addComponent(new Label("(ignored if automatic mode is selected)"));

		// Register listeners
		intervalUnitGroup.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				getLogic().onIntervalTypeChanged(event.getProperty().getValue());
			}
		});
		intervalBoundsModeGroup.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				getLogic().onIntervalBoundsModeChanged(event.getProperty().getValue());
			}
		});
		ValueChangeListener dateBoundsChangeListener = new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				getLogic().onIntervalBoundsChanged(startDateField.getValue(), endDateField.getValue());
			}
		};
		startDateField.addValueChangeListener(dateBoundsChangeListener);
		endDateField.addValueChangeListener(dateBoundsChangeListener);
	}

	private void createScopeConfigurationPanel(GridLayout gl) {
		addTitle(gl, "Scope configuration", 4);

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

		gl.addComponent(new Label("Task tree depth :"));
		HorizontalLayout taskDepthLayout = new HorizontalLayout();
		gl.addComponent(taskDepthLayout);
		Button decreaseTaskDepthButton = new Button("-");
		decreaseTaskDepthButton.setImmediate(true);
		taskDepthLayout.addComponent(decreaseTaskDepthButton);
		taskDepthTextField = new TextField();
		taskDepthTextField.setImmediate(true);
		taskDepthTextField.setWidth("40px");
		taskDepthTextField.addStyleName("center");
		taskDepthLayout.addComponent(taskDepthTextField);
		Button increaseTaskDepthButton = new Button("+");
		increaseTaskDepthButton.setImmediate(true);
		taskDepthLayout.addComponent(increaseTaskDepthButton);
		taskDepthLayout.addComponent(new Label(
				"(deeper contributions will be aggregated)"));

		gl.addComponent(new Label("Collaborators :"));
		selectedCollaboratorsComponent = new Label("");
		gl.addComponent(selectedCollaboratorsComponent);

		// Register listeners
		browseTaskButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				getLogic().onBrowseTaskButtonCLicked();
			}
		});
		rootTaskTextField
				.addValueChangeListener(new Property.ValueChangeListener() {
					@Override
					public void valueChange(ValueChangeEvent event) {
						getLogic().onTaskScopePathChanged(
								(String) event.getProperty().getValue());
					}
				});
		decreaseTaskDepthButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				increaseOrDecreaseTaskTreeDepth(-1);
			}
		});
		taskDepthTextField
				.addValueChangeListener(new Property.ValueChangeListener() {
					@Override
					public void valueChange(ValueChangeEvent event) {
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
	}

	private void createColumnsConfigurationPanel(GridLayout gl) {
		addTitle(gl, "Columns configuration", 8);
		gl.addComponent(new Label(""));
		selectedColumnsComponent = new Label("");
		gl.addComponent(selectedColumnsComponent);
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

	private void addTitle(GridLayout gl, String caption, int row) {
		Label label = new Label("<b>" + caption + "</b><hr>", ContentMode.HTML);
		label.setWidth("100%");
		gl.addComponent(label, 0, row, 1, row);
		
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
		intervalUnitGroup.setValue(id);
	}

	@Override
	public void addIntervalBoundsModeRadioButton(Object id, String label) {
		addCheckboxToGroup(intervalBoundsModeGroup, id, label);
	}

	@Override
	public void selectIntervalBoundsModeButton(Object id) {
		intervalBoundsModeGroup.setValue(id);
	}

	private static void addCheckboxToGroup(OptionGroup group, Object id, String label) {
		group.addItem(id);
		group.setItemCaption(id, label);
	}

	@Override
	public void setIntervalBoundsModeEnablement(boolean startDateEnablement, boolean endDateEnablement) {
		startDateField.setEnabled(startDateEnablement);
		endDateField.setEnabled(endDateEnablement);
	}
 
	@Override
	public void setIntervalBounds(Date startDate, Date endDate) {
		startDateField.setValue(startDate);
		endDateField.setValue(endDate);
	}

	@Override
	public void setTaskScopePath(String path) {
		rootTaskTextField.setValue(path);
	}

	@Override
	public void setTaskTreeDepth(int i) {
		taskDepthTextField.setValue(String.valueOf(i));
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

}
