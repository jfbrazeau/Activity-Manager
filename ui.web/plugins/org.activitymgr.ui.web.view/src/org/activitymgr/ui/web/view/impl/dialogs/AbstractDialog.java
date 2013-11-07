package org.activitymgr.ui.web.view.impl.dialogs;

import org.activitymgr.ui.web.view.util.ResourceCache;

import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class AbstractDialog extends Window {

	private ResourceCache resourceCache;

	public AbstractDialog(ResourceCache resourceCache, String title) {
		super(title);
		this.resourceCache = resourceCache;
		// Register escape key binding
		setCloseShortcut(ShortcutListener.KeyCode.ESCAPE);
		// Trigger a focus as soon as it is possible (the dialog mus be attached
		// first)
		addAttachListener(new AttachListener() {
			@Override
			public void attach(AttachEvent event) {
				focus();
			}
		});
	}

	protected ResourceCache getResourceCache() {
		return resourceCache;
	}

}
