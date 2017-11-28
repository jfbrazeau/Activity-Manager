package org.activitymgr.ui.web.view.impl.internal;

import java.util.Map;

import org.activitymgr.ui.web.logic.IContributionTaskChooserLogic;
import org.activitymgr.ui.web.view.impl.dialogs.AbstractTaskChooserDialog;
import org.activitymgr.ui.web.view.impl.internal.util.MapBasedDatasource;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ContributionTaskChooserDialog extends
		AbstractTaskChooserDialog<IContributionTaskChooserLogic> implements
		IContributionTaskChooserLogic.View {

	private ListSelect recentTasksSelect;
	private CheckBox newSubTaskCheckbox;
	private TextField newSubTaskCodeField;
	private TextField newSubTaskNameField;
	private VerticalLayout newTaskFormPanel;
	private ComboBox newSubTaskCreationPatternField;

	public ContributionTaskChooserDialog() {
        super();
        
        recentTasksSelect.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
						getLogic().onRecentTaskClicked(
								(Long) recentTasksSelect.getValue());
			}
		});
        newSubTaskCheckbox.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
						getLogic().onNewTaskCheckboxClicked();
			}
		});
        newSubTaskNameField.setTextChangeEventMode(TextChangeEventMode.EAGER);
        newSubTaskNameField.addTextChangeListener(new FieldEvents.TextChangeListener() {
			@Override
			public void textChange(TextChangeEvent event) {
						getLogic().onNewTaskNameChanged(event.getText());
			}
		});
        newSubTaskCodeField.setTextChangeEventMode(TextChangeEventMode.EAGER);
        newSubTaskCodeField.addTextChangeListener(new FieldEvents.TextChangeListener() {
			@Override
			public void textChange(TextChangeEvent event) {
						getLogic().onNewTaskCodeChanged(event.getText());
			}
		});
    }

	@Override
	protected Component createBody() {
		HorizontalLayout bodyLayout = new HorizontalLayout();
        bodyLayout.setSizeFull();

        // Task tree
        Component leftContainerPanel = super.createBody();
        bodyLayout.addComponent(leftContainerPanel);

        // Recent tasks
        VerticalLayout rightContainerPanel = new VerticalLayout();
        rightContainerPanel.setSizeFull();
        // Left margin
        rightContainerPanel.setMargin(new MarginInfo(false, false, false, true));
        bodyLayout.addComponent(rightContainerPanel);
        recentTasksSelect = new ListSelect("Recent :");
        recentTasksSelect.setSizeFull();
        recentTasksSelect.setImmediate(true);
        recentTasksSelect.setNullSelectionAllowed(false);
        rightContainerPanel.addComponent(recentTasksSelect);
        newTaskFormPanel = new VerticalLayout();
        rightContainerPanel.addComponent(newTaskFormPanel);
        newSubTaskCheckbox = new CheckBox("Create a new task");
        newSubTaskCheckbox.setImmediate(true);
        newTaskFormPanel.addComponent(newSubTaskCheckbox);
        newTaskFormPanel.addComponent(new Label("Task attributes"));
        HorizontalLayout newTaskLayout = new HorizontalLayout();
        newTaskFormPanel.addComponent(newTaskLayout);
        newSubTaskCodeField = new TextField();
        newSubTaskCodeField.setWidth("60px");
        newSubTaskCodeField.setImmediate(true);
        newSubTaskCodeField.setInputPrompt("Code");
        newTaskLayout.addComponent(newSubTaskCodeField);
        newSubTaskNameField = new TextField();
        newSubTaskNameField.setWidth("150px");
        newSubTaskNameField.setImmediate(true);
        newSubTaskNameField.setInputPrompt("Name (required)");
        newTaskLayout.addComponent(newSubTaskNameField);
        
        // Pattern
		newSubTaskCreationPatternField = new ComboBox("Creation pattern");
        newSubTaskCreationPatternField.setNullSelectionAllowed(false);
        newSubTaskCreationPatternField.setImmediate(true);
        newSubTaskCreationPatternField.setTextInputAllowed(false);
        newSubTaskCreationPatternField.setVisible(false); // Hidden by default
        newTaskFormPanel.addComponent(newSubTaskCreationPatternField);
        
        // Set expand ratios for right container
        rightContainerPanel.setExpandRatio(recentTasksSelect, 60);
        rightContainerPanel.setExpandRatio(newTaskFormPanel, 40);

        // Set expand ratios for body itself
        bodyLayout.setExpandRatio(leftContainerPanel, 40);
        bodyLayout.setExpandRatio(rightContainerPanel, 60);

        return bodyLayout;
	}

    @Override
    public void setCreationPatterns(Map<String, String> patterns) {
    	newSubTaskCreationPatternField.setVisible(true);
    	MapBasedDatasource<String> datasource = new MapBasedDatasource<String>(patterns);
    	newSubTaskCreationPatternField.setContainerDataSource(datasource);
    	newSubTaskCreationPatternField.setItemCaptionPropertyId(MapBasedDatasource.LABEL_PROPERTY_ID);
    	newSubTaskCreationPatternField.setValue(patterns.keySet().iterator().next());
    }

    @Override
    public void setRecentTasks(Map<Long, String> recentTasks) {
    	MapBasedDatasource<Long> datasource = new MapBasedDatasource<Long>(recentTasks);
    	recentTasksSelect.setContainerDataSource(datasource);
    	recentTasksSelect.setItemCaptionPropertyId(MapBasedDatasource.LABEL_PROPERTY_ID);
    }
    
	@Override
	public void setNewTaskFieldsEnabled(boolean enabled) {
		newSubTaskCodeField.setEnabled(enabled);
		newSubTaskNameField.setEnabled(enabled);
		newSubTaskCreationPatternField.setEnabled(enabled);
	}

	@Override
	public String getSelectedTaskCreationPatternId() {
		return (String) newSubTaskCreationPatternField.getValue();
	}

}