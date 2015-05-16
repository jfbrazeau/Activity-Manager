package org.activitymgr.ui.web.view.impl.internal.dialogs;

import java.util.Stack;

import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.TableDatasource;
import org.activitymgr.ui.web.view.impl.internal.util.TreeTableDatasource;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
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

	public TaskChooserDialog(IResourceCache resourceCache) {
        super(resourceCache, "Select a task");
        setModal(true);

        setWidth(520, Unit.PIXELS);

        GridLayout gl = new GridLayout(2, 2);
        setContent(gl);
        
        // Task tree
        Panel leftContainerPanel = new Panel();
        gl.addComponent(leftContainerPanel);
        leftContainerPanel.setWidth(200, Unit.PIXELS);
        leftContainerPanel.setHeight(350, Unit.PIXELS);
        taskTree = new Tree();
        leftContainerPanel.setContent(taskTree);
        taskTree.setImmediate(true);
        
        // Recent tasks
        VerticalLayout rightContainerPanel = new VerticalLayout();
        rightContainerPanel.setMargin(true);
        rightContainerPanel.setWidth(300, Unit.PIXELS);
        rightContainerPanel.setHeight(350, Unit.PIXELS);
        gl.addComponent(rightContainerPanel);
        recentTasksSelect = new ListSelect("Recent :");
        recentTasksSelect.setSizeFull();
        recentTasksSelect.setImmediate(true);
        recentTasksSelect.setNullSelectionAllowed(false);
        rightContainerPanel.addComponent(recentTasksSelect);
        VerticalLayout newTaskFormPanel = new VerticalLayout();
        rightContainerPanel.addComponent(newTaskFormPanel);
        newSubTaskCheckbox = new CheckBox("Create a new task");
        newSubTaskCheckbox.setImmediate(true);
        newTaskFormPanel.addComponent(newSubTaskCheckbox);
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

        // Buttons
        HorizontalLayout hl = new HorizontalLayout();
        hl.setSizeFull();
        gl.addComponent(hl, 0, 1, 1, 1);
        //vl.setComponentAlignment(hl, Alignment.MIDDLE_RIGHT);
        
        statusLabel = new Label();
        hl.addComponent(statusLabel);
        hl.setExpandRatio(statusLabel, 1);
        hl.addComponent(ok);
        hl.setExpandRatio(ok, 0);
        hl.addComponent(cancel);
        hl.setComponentAlignment(cancel, Alignment.MIDDLE_RIGHT);
        hl.setExpandRatio(cancel, 0);
        
        // Register listeners
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
				else {
					taskTree.expandItem(taskTree.getValue());
				}
			}
		});
    }

    public void focus() {
    	taskTree.focus();
    }

    @Override
	public void setTreeContentProviderCallback(
			ITreeContentProviderCallback<Long> treeContentProviderCallback) {
		TreeTableDatasource<Long> dataSource = new TreeTableDatasource<Long>(getResourceCache(), treeContentProviderCallback);
		taskTree.setContainerDataSource(dataSource);
		taskTree.setItemCaptionPropertyId(treeContentProviderCallback.getPropertyIds().iterator().next());
	}
	
    @Override
    public void setRecentTasksProviderCallback(ITableCellProviderCallback<Long> callback) {
    	TableDatasource<Long> datasource = new TableDatasource<Long>(getResourceCache(), callback);
    	recentTasksSelect.setContainerDataSource(datasource);
    	recentTasksSelect.setItemCaptionPropertyId(callback.getPropertyIds().iterator().next());
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
	public void setNewTaskFormEnabled(boolean enabled) {
		newSubTaskCheckbox.setEnabled(enabled);
	}

	@Override
	public void setNewTaskFieldsEnabled(boolean enabled) {
		newSubTaskCodeField.setEnabled(enabled);
		newSubTaskNameField.setEnabled(enabled);
	}

	@Override
	public void setStatus(String status) {
		statusLabel.setValue(status);
	}
	
	@Override
	public void selectTask(long taskId) {
		taskTree.setValue(taskId);
		// FIXME maybe useless
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
		return (Long) taskTree.getValue();
	}

}