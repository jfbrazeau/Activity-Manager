package org.activitymgr.ui.web.view.util;

import com.vaadin.data.Item;
import com.vaadin.data.Property;

@SuppressWarnings("serial")
public abstract class AbstractItem implements Item {
	
	private ResourceCache resourceCache;
	
	public AbstractItem(ResourceCache resourceCache) {
		this.resourceCache = resourceCache;
	}
	
	public ResourceCache getResourceCache() {
		return resourceCache;
	}

	@Override
	public boolean addItemProperty(Object id, @SuppressWarnings("rawtypes") Property property) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeItemProperty(Object id) {
		throw new UnsupportedOperationException();
	}
	
}
