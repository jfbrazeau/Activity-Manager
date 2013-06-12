package org.activitymgr.ui.web.view.util;

import org.activitymgr.ui.web.logic.ITextFieldLogic;

import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class TextFieldView extends TextField implements ITextFieldLogic.View {
	
	private ITextFieldLogic logic;
	
	@Override
	public void registerLogic(ITextFieldLogic newLogic) {
		this.logic = newLogic;
		setImmediate(true);
		addStyleName("amount");
		addBlurListener(new FieldEvents.BlurListener() {
			@Override
			public void blur(BlurEvent event) {
				logic.onValueChanged(getValue());
			}
		});
	}
	
}
