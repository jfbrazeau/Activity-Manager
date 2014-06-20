package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Arrays;
import java.util.Collection;

import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.data.Property;
import com.vaadin.server.Resource;
import com.vaadin.ui.Label;

@SuppressWarnings("serial")
public class BasicItem extends AbstractItem {
	
	private ILabelProviderCallback labelProvider;
	public static final String ICON_PROPERTY_ID = "icon";
	public static final String NAME_PROPERTY_ID = "name";
	public static final Collection<String> PROPERTY_IDS = Arrays
	.asList(new String[] { NAME_PROPERTY_ID, ICON_PROPERTY_ID });

	public BasicItem(IResourceCache resourceCache, ILabelProviderCallback labelProvider) {
		super(resourceCache);
		this.labelProvider = labelProvider;
	}
	
	@Override
	public Property<?> getItemProperty(Object id) {
		String propertyId = (String) id;
		Property<?> property = null;
		if (NAME_PROPERTY_ID.equals(propertyId)) {
			property = new Label(labelProvider.getText());
		}
		else if (ICON_PROPERTY_ID.equals(propertyId)) {
			Resource icon = getResourceCache().getIconResource(labelProvider.getIcon());
			property = new SimpleProperty(icon, Resource.class);
		}
		else {
			throw new IllegalStateException("Unexpected property id " + propertyId + ")");
		}
		return property;
	}
	
	@Override
	public Collection<?> getItemPropertyIds() {
		return PROPERTY_IDS;
	}

}