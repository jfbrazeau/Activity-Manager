package org.activitymgr.ui.web.view.impl.internal.util;

import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.data.Item;
import com.vaadin.data.Property;

@SuppressWarnings("serial")
public abstract class AbstractItem implements Item {
	
	private IResourceCache resourceCache;
	
	public AbstractItem(IResourceCache resourceCache) {
		this.resourceCache = resourceCache;
	}
	
	public IResourceCache getResourceCache() {
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
