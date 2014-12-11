package org.activitymgr.ui.web.view.impl.internal.dialogs;

import org.activitymgr.ui.web.logic.IGenericCallback;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class YesNoDialog extends AbstractDialog implements Button.ClickListener {

	private IGenericCallback<Boolean> callback;
	private Button yes = new Button("Yes", this);
	private Button no = new Button("No", this);

	public YesNoDialog(IResourceCache resourceCache, String caption, String question, IGenericCallback<Boolean> callback) {
        super(resourceCache, caption);

        setModal(true);

        this.callback = callback;

        VerticalLayout vl = new VerticalLayout();
        setContent(vl);
        
        if (question != null) {
            vl.addComponent(new Label(question));
        }

        HorizontalLayout hl = new HorizontalLayout();
        hl.addComponent(yes);
        hl.setExpandRatio(yes, 1);
        hl.addComponent(no);
        hl.setComponentAlignment(no, Alignment.MIDDLE_RIGHT);
        hl.setExpandRatio(no, 1);
        vl.addComponent(hl);
    }


	@Override
	public void buttonClick(ClickEvent event) {
        if (getParent() != null) {
            close();
        }
        callback.callback(event.getSource() == yes);
	}

}