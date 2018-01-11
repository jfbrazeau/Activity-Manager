package org.activitymgr.ui.web.view.impl.dialogs;

import org.activitymgr.ui.web.logic.IGenericCallback;

import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class InputDialog extends AbstractDialog implements Button.ClickListener {

	private IGenericCallback<String> callback;
	private Button ok = new Button("Ok", this);
	private Button cancel = new Button("Cancel", this);
	private TextField text;

	public InputDialog(String caption, String question, String defaultValue,
			final IGenericCallback<String> callback) {
        super(caption);

        setModal(true);

        this.callback = callback;

        VerticalLayout vl = new VerticalLayout();
        setContent(vl);
		vl.setSpacing(true);
		vl.setMargin(true);
        
        if (question != null) {
            vl.addComponent(new Label(question));
        }

		text = new TextField();
		text.setSizeFull();
		if (defaultValue != null) {
			text.setValue(defaultValue);
			text.selectAll();
		}
		vl.addComponent(text);

        HorizontalLayout hl = new HorizontalLayout();
        hl.addComponent(ok);
        hl.setExpandRatio(ok, 1);
		hl.addComponent(cancel);
		hl.setComponentAlignment(cancel, Alignment.MIDDLE_RIGHT);
		hl.setExpandRatio(cancel, 1);
        vl.addComponent(hl);

		// Key listener
		addShortcutListener(new ShortcutListener("OK",
				ShortcutListener.KeyCode.ENTER, new int[] {}) {
			@Override
			public void handleAction(Object sender, Object target) {
				if (getParent() != null) {
					close();
				}
				callback.callback(text.getValue());
			}
		});
		addAttachListener(new AttachListener() {
			@Override
			public void attach(AttachEvent event) {
				text.focus();
				removeAttachListener(this);
			}
		});
    }


	@Override
	public void buttonClick(ClickEvent event) {
        if (getParent() != null) {
            close();
        }
		if (event.getSource() == ok) {
			callback.callback(text.getValue());
		}
	}

}