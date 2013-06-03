package org.activitymgr.ui.web.view.dialogs;

import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.util.ResourceCache;
import org.activitymgr.ui.web.view.util.TreeDatasource;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class TaskChooserDialog extends Window implements Button.ClickListener, ITaskChooserLogic.View {

	private Button ok = new Button("Ok", this);
	private Button cancel = new Button("Cancel", this);
	private ITaskChooserLogic logic;
	private Tree taskTree;
	private ResourceCache resourceCache;
	private Label statusLabel;

	public TaskChooserDialog(ResourceCache resourceCache) {
        super("Choose a task");
        setModal(true);

        setHeight(450, Unit.PIXELS);

        this.resourceCache = resourceCache;
        
        VerticalLayout vl = new VerticalLayout();
        setContent(vl);
        
        statusLabel = new Label("Select a task");
        vl.addComponent(statusLabel);
        
        taskTree = new Tree();
        vl.addComponent(taskTree);
        taskTree.setWidth(400, Unit.PIXELS);
        taskTree.setImmediate(true);

        HorizontalLayout hl = new HorizontalLayout();
        vl.addComponent(hl);
        vl.setComponentAlignment(hl, Alignment.MIDDLE_RIGHT);
        hl.addComponent(ok);
        hl.setExpandRatio(ok, 1);
        hl.addComponent(cancel);
        hl.setComponentAlignment(cancel, Alignment.MIDDLE_RIGHT);
        hl.setExpandRatio(cancel, 1);
        
        // Register listeners
        taskTree.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				logic.onSelectionChanged(taskTree.getValue() == null ? null : Long.parseLong((String) taskTree.getValue()));
			}
		});
    }

	@Override
	public void setTreeContentProviderCallback(
			ITreeContentProviderCallback treeContentProviderCallback) {
		taskTree.setContainerDataSource(new TreeDatasource(resourceCache, treeContentProviderCallback));
		taskTree.setItemCaptionPropertyId(TreeDatasource.NAME_PROPERTY_ID);
		taskTree.setItemIconPropertyId(TreeDatasource.ICON_PROPERTY_ID);
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
}