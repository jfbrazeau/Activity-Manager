package org.activitymgr.ui.web.view.impl.dialogs;

import java.util.Map;
import java.util.Stack;

import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.impl.internal.util.MapBasedDatasource;
import org.activitymgr.ui.web.view.impl.internal.util.TreeTableDatasource;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.sqlcontainer.query.generator.filter.FilterTranslator;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.ShortcutListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class TaskChooserDialog extends AbstractDialog implements Button.ClickListener, ITaskChooserLogic.View {

	private Button ok = new Button("Ok", this);
	private Button cancel = new Button("Cancel", this);
	private ITaskChooserLogic logic;
	private Tree taskTree;
	private Label statusLabel;
	private ListSelect recentTasksSelect;
	private CheckBox newSubTaskCheckbox;
	private TextField newSubTaskCodeField;
	private TextField newSubTaskNameField;
	private VerticalLayout newTaskFormPanel;
	private ComboBox newSubTaskCreationPatternField;
	private TextField filterField;

	public TaskChooserDialog() {
        super("Select a task");
        setModal(true);

        setWidth(700, Unit.PIXELS);
        setHeight(550, Unit.PIXELS);

        // Global layout
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setMargin(new MarginInfo(true, true, true, true));
        setContent(contentLayout);
        
        // Main part 
        HorizontalLayout bodyLayout = createBodyLayout();
        contentLayout.addComponent(bodyLayout);
        
        // Footer containing status & OK / Cancel buttons
        HorizontalLayout footerLayout = createFooterLayout();
        contentLayout.addComponent(footerLayout);
        
        // Set expand ratios
        contentLayout.setExpandRatio(bodyLayout, 95);
        contentLayout.setExpandRatio(footerLayout, 5);
        
        // Register listeners
        filterField.addTextChangeListener(new FieldEvents.TextChangeListener() {
			@Override
			public void textChange(TextChangeEvent event) {
				logic.onTaskFilterChanged(event.getText());
			}
		});
        taskTree.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				logic.onSelectionChanged((Long) taskTree.getValue());
			}
		});
        recentTasksSelect.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				logic.onRecentTaskClicked((Long) recentTasksSelect.getValue());
			}
		});
        newSubTaskCheckbox.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				logic.onNewTaskCheckboxClicked();
			}
		});
        newSubTaskNameField.setTextChangeEventMode(TextChangeEventMode.EAGER);
        newSubTaskNameField.addTextChangeListener(new FieldEvents.TextChangeListener() {
			@Override
			public void textChange(TextChangeEvent event) {
				logic.onNewTaskNameChanged(event.getText());
			}
		});
        newSubTaskCodeField.setTextChangeEventMode(TextChangeEventMode.EAGER);
        newSubTaskCodeField.addTextChangeListener(new FieldEvents.TextChangeListener() {
			@Override
			public void textChange(TextChangeEvent event) {
				logic.onNewTaskCodeChanged(event.getText());
			}
		});
        
        // Key listener
        addShortcutListener(new ShortcutListener("OK", ShortcutListener.KeyCode.ENTER, new int[] {}) {
			@Override
			public void handleAction(Object sender, Object target) {
				if (ok.isEnabled()) {
			        if (getParent() != null) {
			            close();
			        }
		        	logic.onOkButtonClicked((Long) taskTree.getValue());
				}
				else if (target == taskTree && taskTree.getValue() != null) {
					taskTree.expandItem(taskTree.getValue());
				}
			}
		});
    }

	private HorizontalLayout createBodyLayout() {
		HorizontalLayout bodyLayout = new HorizontalLayout();
        bodyLayout.setSizeFull();

        // Task tree
        VerticalLayout leftContainerPanel = new VerticalLayout();
        leftContainerPanel.setSizeFull();
        bodyLayout.addComponent(leftContainerPanel);
        filterField = new TextField();
        filterField.setWidth("100%");
        filterField.setImmediate(true);
        leftContainerPanel.addComponent(filterField);
        filterField.setInputPrompt("Type a text to filter...");
       
        Panel treeContainer = new Panel();
        treeContainer.setSizeFull();
        leftContainerPanel.addComponent(treeContainer);
        leftContainerPanel.setSizeFull();
        taskTree = new Tree();
        taskTree.setNullSelectionAllowed(false);
        treeContainer.setContent(taskTree);
        taskTree.setImmediate(true);
        taskTree.setHtmlContentAllowed(true);

        // Set expand ratios for left container
        leftContainerPanel.setExpandRatio(treeContainer, 93);
        leftContainerPanel.setExpandRatio(filterField, 7);

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

	private HorizontalLayout createFooterLayout() {
		HorizontalLayout footerLayout = new HorizontalLayout();
		footerLayout.setSizeFull();

        statusLabel = new Label();
        footerLayout.addComponent(statusLabel);
        footerLayout.addComponent(ok);
        footerLayout.addComponent(cancel);
        footerLayout.setComponentAlignment(cancel, Alignment.TOP_RIGHT);

        // Set expand ratios
        footerLayout.setExpandRatio(statusLabel, 80);
        footerLayout.setExpandRatio(ok, 10);
        footerLayout.setExpandRatio(cancel, 10);
		return footerLayout;
	}

    public void focus() {
    	taskTree.focus();
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
	public void setTasksTreeProviderCallback(
			ITreeContentProviderCallback<Long> treeContentProviderCallback) {
		TreeTableDatasource<Long> dataSource = new TreeTableDatasource<Long>(getResourceCache(), treeContentProviderCallback);
		taskTree.setContainerDataSource(dataSource);
		taskTree.setItemCaptionPropertyId(treeContentProviderCallback.getPropertyIds().iterator().next());
	}
	
    @Override
    public void setRecentTasks(Map<Long, String> recentTasks) {
    	MapBasedDatasource<Long> datasource = new MapBasedDatasource<Long>(recentTasks);
    	recentTasksSelect.setContainerDataSource(datasource);
    	recentTasksSelect.setItemCaptionPropertyId(MapBasedDatasource.LABEL_PROPERTY_ID);
    }
    
	@Override
	public void buttonClick(ClickEvent event) {
        if (getParent() != null) {
            close();
        }
        if (event.getSource() == ok) {
        	logic.onOkButtonClicked((Long)taskTree.getValue());
        }
	}

	@Override
	public void registerLogic(ITaskChooserLogic logic) {
		this.logic = logic;
	}

	@Override
	public void setOkButtonEnabled(boolean enabled) {
		ok.setEnabled(enabled);
	}

	@Override
	public void setNewTaskFieldsEnabled(boolean enabled) {
		newSubTaskCodeField.setEnabled(enabled);
		newSubTaskNameField.setEnabled(enabled);
		newSubTaskCreationPatternField.setEnabled(enabled);
	}

	@Override
	public void setStatus(String status) {
		statusLabel.setValue(status);
	}
	
	@Override
	public void selectTask(long taskId) {
		taskTree.setValue(taskId);
		expandToTask(taskId);
	}

	@Override
	public void expandToTask(long taskId) {
		Object parent = taskId;
		Stack<Object> parents = new Stack<Object>();
		while ((parent = taskTree.getParent(parent)) != null) {
			parents.push(parent);
		}
		while (!parents.isEmpty()) {
			Object element = parents.pop();
			taskTree.expandItem(element);
		}
	}

	@Override
	public boolean isNewTaskChecked() {
		return newSubTaskCheckbox.getValue();
	}
	
	@Override
	public String getNewTaskName() {
		return newSubTaskNameField.getValue();
	}

	@Override
	public String getNewTaskCode() {
		return newSubTaskCodeField.getValue();
	}

	@Override
	public long getSelectedTaskId() {
		return taskTree.getValue() != null ? (Long)taskTree.getValue() : -1;
	}

	@Override
	public String getSelectedTaskCreationPatternId() {
		return (String) newSubTaskCreationPatternField.getValue();
	}

}