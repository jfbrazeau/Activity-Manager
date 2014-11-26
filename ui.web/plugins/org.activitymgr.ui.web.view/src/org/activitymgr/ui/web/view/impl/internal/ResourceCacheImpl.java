package org.activitymgr.ui.web.view.impl.internal;

import java.util.HashMap;
import java.util.Map;

import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;

public class ResourceCacheImpl implements IResourceCache {
	
	public static final String ONE_PIXEL_ICON = "one-pixel.gif";

	private Map<String, Resource> cache = new HashMap<String, Resource>();
	
	public Resource getResource(String path) {
		Resource result = cache.get(path);
		if (result == null) {
			result = new ThemeResource("icons/" + path);
			cache.put(path, result);
		}
		return result;
	}

	public Resource getIconResource(String icon) {
		return icon == null ? null : getResource(icon);
	}

}
