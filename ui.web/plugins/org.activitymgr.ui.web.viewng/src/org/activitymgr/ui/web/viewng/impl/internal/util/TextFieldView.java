package org.activitymgr.ui.web.viewng.impl.internal.util;

import org.activitymgr.ui.web.logic.ITextFieldLogic;

import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class TextFieldView extends TextField implements ITextFieldLogic.View {
	
	private ITextFieldLogic logic;
	
	@Override
	public void registerLogic(ITextFieldLogic newLogic) {
		this.logic = newLogic;
		addBlurListener(new FieldEvents.BlurListener() {
			@Override
			public void blur(BlurEvent event) {
				logic.onValueChanged(getValue());
			}
		});
		addShortcutListener(new ShortcutListener("", null, KeyCode.ENTER) {
			@Override
			public void handleAction(Object sender, Object target) {
				logic.onEnterKeyPressed();
			}
		});
	}

	@Override
	public void setNumericFieldStyle() {
		setStyleName("amount");
	}
	
	@Override
	public void setWarningStyle() {
		addStyleName("warning");
	}

	@Override
	public void setValue(String newValue) {
		super.setValue(newValue != null ? newValue : "");
	}

	public void onDoubleClick() {
		logic.onDoubleClick();
	}
	
	@Override
	public void blur() {
		// No blur method, so we simply focus on the root
		getUI().focus();
	}

	@Override
	public void setTooltip(String text) {
		setDescription(text);
	}

	@Override
	public void setPlaceholder(String text) {
		setPlaceholder(text);
	}

}
