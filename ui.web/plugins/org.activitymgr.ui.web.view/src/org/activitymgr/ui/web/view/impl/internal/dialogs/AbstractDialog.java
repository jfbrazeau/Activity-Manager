package org.activitymgr.ui.web.view.impl.internal.dialogs;

import org.activitymgr.ui.web.view.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class AbstractDialog extends Window {

	@Inject
	private IResourceCache resourceCache;

	public AbstractDialog(String title) {
		super(title);
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
