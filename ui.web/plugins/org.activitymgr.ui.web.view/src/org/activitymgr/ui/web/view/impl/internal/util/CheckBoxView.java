package org.activitymgr.ui.web.view.impl.internal.util;

import org.activitymgr.ui.web.logic.ICheckBoxFieldLogic;
import org.activitymgr.ui.web.logic.ICheckBoxFieldLogic.View;

import com.vaadin.data.Property;
import com.vaadin.ui.CheckBox;

@SuppressWarnings("serial")
public class CheckBoxView extends CheckBox implements View {

	private ICheckBoxFieldLogic logic;

	public CheckBoxView() {
		addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(Property.ValueChangeEvent event) {
				logic.onValueChanged(getValue());
			}
		});
	}

	@Override
	public void registerLogic(ICheckBoxFieldLogic logic) {
		this.logic = logic;
	}

	@Override
	public void setValue(boolean value) {
		super.setValue(value);
	}

}
