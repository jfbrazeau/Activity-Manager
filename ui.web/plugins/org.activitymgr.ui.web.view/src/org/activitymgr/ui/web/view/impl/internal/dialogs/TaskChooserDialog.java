package org.activitymgr.ui.web.view.impl.internal.dialogs;

import java.util.Collection;

import org.activitymgr.ui.web.logic.IListContentProviderCallback;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.BasicItem;
import org.activitymgr.ui.web.view.impl.internal.util.BasicListDatasource;
import org.activitymgr.ui.web.view.impl.internal.util.BasicTreeDatasource;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Panel;
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

	public TaskChooserDialog(IResourceCache resourceCache) {
        super(resourceCache, "Select a task");
        setModal(true);

        setWidth(420, Unit.PIXELS);

        GridLayout gl = new GridLayout(2, 2);
        setContent(gl);
        
        // Task tree
        Panel taskTreeContainerPanel = new Panel();
        gl.addComponent(taskTreeContainerPanel);
        taskTreeContainerPanel.setWidth(200, Unit.PIXELS);
        taskTreeContainerPanel.setHeight(350, Unit.PIXELS);
        taskTree = new Tree();
        taskTreeContainerPanel.setContent(taskTree);
        //taskTree.setWidth(400, Unit.PIXELS);
        taskTree.setSizeUndefined();
        taskTree.setImmediate(true);
        taskTree.setSizeFull();
        
        // Recent tasks
        VerticalLayout recentTasksContainerPanel = new VerticalLayout();
        recentTasksContainerPanel.setHeight(350, Unit.PIXELS);
        recentTasksContainerPanel.setWidth(200, Unit.PIXELS);
        recentTasksContainerPanel.setHeight(350, Unit.PIXELS);
        gl.addComponent(recentTasksContainerPanel);
        recentTasksSelect = new ListSelect("Recent :");
        recentTasksSelect.setSizeFull();
        recentTasksSelect.setImmediate(true);
        recentTasksSelect.setNullSelectionAllowed(false);
        recentTasksContainerPanel.addComponent(recentTasksSelect);
        recentTasksContainerPanel.addComponent(new Label("dd"));
        
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
				logic.onSelectionChanged(taskTree.getValue() == null ? null : Long.parseLong((String) taskTree.getValue()));
			}
		});
        taskTree.setNewItemHandler(new AbstractSelect.NewItemHandler() {
			@Override
			public void addNewItem(String newItemCaption) {
				System.out.println("addNewItem(" + newItemCaption + ")");
			}
		});
        recentTasksSelect.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				logic.onRecentTaskClicked(recentTasksSelect.getValue() == null ? null : Long.parseLong((String) recentTasksSelect.getValue()));
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
		        	logic.onTaskChosen(Long.parseLong((String) taskTree.getValue()));
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
			ITreeContentProviderCallback treeContentProviderCallback) {
		BasicTreeDatasource dataSource = new BasicTreeDatasource(getResourceCache(), treeContentProviderCallback);
		taskTree.setContainerDataSource(dataSource);
		taskTree.setItemCaptionPropertyId(BasicItem.NAME_PROPERTY_ID);
		taskTree.setItemIconPropertyId(BasicItem.ICON_PROPERTY_ID);
		// TODO preselect another node ?
		Collection<?> rootItemIds = dataSource.rootItemIds();
		if (!rootItemIds.isEmpty()) {
			Object rootItemId = rootItemIds.iterator().next();
			taskTree.select(rootItemId);
		}
	}
	
    @Override
    public void setRecentTasksProviderCallback(IListContentProviderCallback callback) {
    	BasicListDatasource datasource = new BasicListDatasource(getResourceCache(), callback);
    	recentTasksSelect.setContainerDataSource(datasource);
    	recentTasksSelect.setItemCaptionPropertyId(BasicItem.NAME_PROPERTY_ID);
    	recentTasksSelect.setItemIconPropertyId(BasicItem.ICON_PROPERTY_ID);
    }
    
	@Override
	public void buttonClick(ClickEvent event) {
        if (getParent() != null) {
            close();
        }
        if (event.getSource() == ok) {
        	logic.onTaskChosen(Long.parseLong((String) taskTree.getValue()));
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
	public void setStatus(String status) {
		statusLabel.setValue(status);
	}
	
	@Override
	public void expandTask(long taskId) {
		taskTree.expandItem(String.valueOf(taskId));
	}

	@Override
	public void selectTask(long taskId) {
		taskTree.setValue(String.valueOf(taskId));
	}

	@Override
	public void expandTasks(Collection<Long> taskIds) {
		for (Long taskId : taskIds) {
			expandTask(taskId);
		}
	}

}