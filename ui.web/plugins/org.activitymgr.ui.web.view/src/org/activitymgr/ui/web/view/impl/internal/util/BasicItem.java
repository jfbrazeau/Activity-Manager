package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Collection;

import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.data.Property;
import com.vaadin.ui.Label;

@SuppressWarnings("serial")
public class BasicItem extends AbstractItem {
	
	private ILabelProviderCallback<Object> labelProvider;
	private Object value;

	@SuppressWarnings("unchecked")
	public BasicItem(IResourceCache resourceCache, Object value, ILabelProviderCallback<?> labelProvider) {
		super(resourceCache);
		this.labelProvider = (ILabelProviderCallback<Object>) labelProvider;
		this.value = value;
	}
	
	@Override
	public Property<?> getItemProperty(Object id) {
		String propertyId = (String) id;
		return new Label(labelProvider.getText(value, propertyId));
	}
	
	@Override
	public Collection<?> getItemPropertyIds() {
		return labelProvider.getPropertyIds();
	}

}