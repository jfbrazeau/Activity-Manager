package org.activitymgr.ui.web.view.util;

import com.vaadin.data.Property;

@SuppressWarnings("serial")
// TODO use property generic type ?
public class SimpleProperty implements Property<Object> {

	private Object value;
	private Class<?> type;
	
	public SimpleProperty(Object value, Class<?> type) {
		this.value = value;
		this.type = type;
	}
	
	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public void setValue(Object newValue) throws ReadOnlyException {
		throw new ReadOnlyException();
	}

	@Override
	public Class<?> getType() {
		return type;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public void setReadOnly(boolean newStatus) {
	}

}
