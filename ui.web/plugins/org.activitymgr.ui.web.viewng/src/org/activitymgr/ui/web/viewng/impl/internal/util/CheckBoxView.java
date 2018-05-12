package org.activitymgr.ui.web.viewng.impl.internal.util;

import org.activitymgr.ui.web.logic.ICheckBoxFieldLogic;
import org.activitymgr.ui.web.logic.ICheckBoxFieldLogic.View;

import com.vaadin.ui.CheckBox;

@SuppressWarnings("serial")
public class CheckBoxView extends CheckBox implements View {

	@SuppressWarnings("unused")
	private ICheckBoxFieldLogic logic;

	@Override
	public void registerLogic(final ICheckBoxFieldLogic logic) {
		this.logic = logic;
		addValueChangeListener(new ValueChangeListener<Boolean>() {
			@Override
			public void valueChange(ValueChangeEvent<Boolean> event) {
				logic.onValueChanged(getValue());
			}
		});
	}

	@Override
	public void setValue(Boolean value) {
		super.setValue(value);
	}

}
