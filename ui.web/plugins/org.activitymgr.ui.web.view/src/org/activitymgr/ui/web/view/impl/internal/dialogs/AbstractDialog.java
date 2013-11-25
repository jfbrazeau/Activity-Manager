package org.activitymgr.ui.web.view.impl.internal.dialogs;

import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class AbstractDialog extends Window {

	private IResourceCache resourceCache;

	public AbstractDialog(IResourceCache resourceCache, String title) {
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

	protected IResourceCache getResourceCache() {
		return resourceCache;
	}

}
