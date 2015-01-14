package org.activitymgr.ui.web.view.impl.internal;

import java.util.ArrayList;
import java.util.List;

import org.activitymgr.ui.web.logic.IActionLogic;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.event.ShortcutListener;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public abstract class AbstractPanelWithActions<LOGIC extends IActionLogic<?>> extends VerticalLayout implements IActionLogic.View<LOGIC>{

	private IResourceCache resourceCache;
	private LOGIC logic;
	private VerticalLayout actionsContainer;
	private List<ShortcutListener> actions = new ArrayList<ShortcutListener>();

	public AbstractPanelWithActions(IResourceCache resourceCache) {
		this.resourceCache = resourceCache;
		setSpacing(true);
		setMargin(true);
		
		// Header
		Component header = createHeaderComponent();
		if (header != null) {
			addComponent(header);
		}
		
		// Horizontal layout
		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);
		addComponent(hl);
		
		// Header
		Component leftComponent = createLeftComponent();
		if (leftComponent != null) {
			hl.addComponent(leftComponent);
		}

		// Body
		Component bodyComponent = createBodyComponent();
		hl.addComponent(bodyComponent);

		// Add actions panel
		actionsContainer = new VerticalLayout();
		hl.addComponent(actionsContainer);
	}
	
	protected Component createHeaderComponent() {
		return null;
	}

	protected Component createLeftComponent() {
		return null;
	}

	protected abstract Component createBodyComponent();

	@Override
	public void registerLogic(LOGIC logic) {
		this.logic = logic;
	}

	protected IResourceCache getResourceCache() {
		return resourceCache;
	}
	
	protected LOGIC getLogic() {
		return logic;
	}

	protected List<ShortcutListener> getActions() {
		return actions;
	}

	@Override
	public void addAction(final String actionId, final String label, final String keyBindingDescription, final String iconId, final char key,
			final boolean ctrl, final boolean shift, final boolean alt) {
		// TODO Use standard views ?
		int[] rawModifiers = new int[3];
		int i = 0;
		if (ctrl)
			rawModifiers[i++] = ShortcutListener.ModifierKey.CTRL;
		if (shift)
			rawModifiers[i++] = ShortcutListener.ModifierKey.SHIFT;
		if (alt)
			rawModifiers[i++] = ShortcutListener.ModifierKey.ALT;
		int[] modifiers = new int[i];
		System.arraycopy(rawModifiers, 0, modifiers, 0, i);
		Resource iconResource = getResourceCache().getResource(iconId + ".gif");
		String caption = label + " <em>"
				+ keyBindingDescription + "</em>";
		ShortcutListener newAction = new ShortcutListener(caption,
				iconResource, key, modifiers) {
			@Override
			public void handleAction(Object sender, Object target) {
				logic.onAction(actionId);
			}
		};
		actions.add(newAction);
		addShortcutListener(newAction);
		Button button = new Button();
		button.setIcon(iconResource);
		button.setDescription(caption);
		actionsContainer.addComponent(button);
		button.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				logic.onAction(actionId);
			}
		});
	}
}