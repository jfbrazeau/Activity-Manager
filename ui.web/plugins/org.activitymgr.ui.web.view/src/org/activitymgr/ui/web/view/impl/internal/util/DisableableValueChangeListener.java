package org.activitymgr.ui.web.view.impl.internal.util;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;

@SuppressWarnings("serial")
public abstract class DisableableValueChangeListener implements
		ValueChangeListener {

	private ThreadLocal<Boolean> disabled = new ThreadLocal<Boolean>();

	public void setEnabled(boolean enabled) {
		if (enabled) {
			this.disabled.remove();
		} else {
			this.disabled.set(Boolean.TRUE);
		}
	}

	@Override
	public void valueChange(ValueChangeEvent event) {
		if (disabled.get() == null) {
			doValueChange(event);
		}
	}

	public abstract void doValueChange(ValueChangeEvent event);
}
