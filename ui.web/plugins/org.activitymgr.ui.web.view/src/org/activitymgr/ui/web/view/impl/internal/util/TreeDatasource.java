package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Arrays;
import java.util.Collection;

import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.server.Resource;
import com.vaadin.ui.Label;

@SuppressWarnings("serial")
public class TreeDatasource extends
		AbstractContainerDatasource<TreeItem> implements
		Container.Hierarchical {

	public static final String NAME_PROPERTY_ID = "name";
	public static final String ICON_PROPERTY_ID = "icon";
	public static final Collection<String> PROPERTY_IDS = Arrays
			.asList(new String[] { NAME_PROPERTY_ID, ICON_PROPERTY_ID });

	private ITreeContentProviderCallback contentProvider;

	public TreeDatasource(IResourceCache resourceCache,
			ITreeContentProviderCallback contentProvider) {
		super(resourceCache);
		this.contentProvider = contentProvider;
	}

	@Override
	public Collection<?> getChildren(Object itemId) {
		return contentProvider.getChildren((String) itemId);
	}

	@Override
	public Collection<?> rootItemIds() {
		return contentProvider.rootItemIds();
	}

	@Override
	public boolean isRoot(Object itemId) {
		return contentProvider.isRoot((String) itemId);
	}

	@Override
	protected TreeItem createItem(String itemId) {
		return new TreeItem(getResourceCache(),
				contentProvider.getLabelProvider(itemId));
	}

	/*
	 * Property management methods
	 */

	@Override
	public final Collection<?> getContainerPropertyIds() {
		return PROPERTY_IDS;
	}

	@Override
	public final Class<?> getType(Object propertyId) {
		return NAME_PROPERTY_ID.equals(propertyId) ? String.class
				: Resource.class;
	}

	@Override
	public final boolean areChildrenAllowed(Object itemId) {
		return hasChildren(itemId);
	}

	@Override
	public final boolean hasChildren(Object itemId) {
		return getChildren(itemId).size() > 0;
	}

	/*
	 * Not implemented
	 */

	@Override
	public final Object getParent(Object itemId) {
		throw new IllegalStateException(
				"Method is not implemented {getParent(Object itemId)}");
	}

	@Override
	public final Collection<?> getItemIds() {
		throw new IllegalStateException(
				"Method is not implemented {getItemIds()}");
	}

	@Override
	public final int size() {
		throw new IllegalStateException("Method is not implemented {size()}");
	}

	/*
	 * Unsupported operations
	 */

	@Override
	public final boolean setParent(Object itemId, Object newParentId)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean setChildrenAllowed(Object itemId,
			boolean areChildrenAllowed) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}

@SuppressWarnings("serial")
class TreeItem extends AbstractItem {
	
	private ILabelProviderCallback labelProvider;

	public TreeItem(IResourceCache resourceCache, ILabelProviderCallback labelProvider) {
		super(resourceCache);
		this.labelProvider = labelProvider;
	}
	
	@Override
	public Property<?> getItemProperty(Object id) {
		String propertyId = (String) id;
		Property<?> property = null;
		if (TreeDatasource.NAME_PROPERTY_ID.equals(propertyId)) {
			property = new Label(labelProvider.getText());
		}
		else if (TreeDatasource.ICON_PROPERTY_ID.equals(propertyId)) {
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
		return TreeDatasource.PROPERTY_IDS;
	}

}
