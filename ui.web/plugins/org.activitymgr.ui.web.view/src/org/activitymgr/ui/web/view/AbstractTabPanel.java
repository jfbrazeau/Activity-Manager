package org.activitymgr.ui.web.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.activitymgr.ui.web.logic.IButtonLogic.View;
import org.activitymgr.ui.web.logic.ITabLogic;
import org.activitymgr.ui.web.view.impl.internal.util.StandardButtonView;
import org.activitymgr.ui.web.view.impl.internal.util.TextFieldView;

import com.google.inject.Inject;
import com.vaadin.event.Action;
import com.vaadin.event.LayoutEvents;
import com.vaadin.event.ShortcutListener;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public abstract class AbstractTabPanel<LOGIC extends ITabLogic<?>> extends VerticalLayout implements ITabLogic.View<LOGIC>{

	public static class ButtonBasedShortcutListener extends ShortcutListener {
		
		private StandardButtonView buttonView;

		public ButtonBasedShortcutListener(StandardButtonView buttonView, char key,
				boolean ctrl, boolean shift, boolean alt) {
			super(buttonView.getDescription(), buttonView.getIcon(), key, toModifiers(ctrl, shift, alt));
			this.buttonView = buttonView;
		}
		private static int[] toModifiers(boolean ctrl, boolean shift, boolean alt) {
			// Register a menu and a shortcut
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
			return modifiers;
		}
		public boolean isEnabled() {
			return buttonView.isEnabled();
		}
		@Override
		public void handleAction(Object sender, Object target) {
			((Button)buttonView).click();
		}
	}

	
	@Inject
	private IResourceCache resourceCache;
	private LOGIC logic;
	private VerticalLayout actionsContainer;
	private List<ButtonBasedShortcutListener> orderedActions = new ArrayList<ButtonBasedShortcutListener>();
	private List<ButtonBasedShortcutListener> activeActions = new ArrayList<ButtonBasedShortcutListener>();
	private Component bodyComponent;

	public AbstractTabPanel() {
		setSpacing(true);
		setMargin(true);
		setSizeFull();
		
		// Header
		Component headerComponent = createHeaderComponent();
		if (headerComponent != null) {
			addComponent(headerComponent);
		}
		
		// Horizontal layout
		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);
		hl.setSizeFull();
		addComponent(hl);
		
		// Header
		Component leftComponent = createLeftComponent();
		if (leftComponent != null) {
			hl.addComponent(leftComponent);
		}

		// Body
		bodyComponent = createBodyComponent();
		hl.addComponent(bodyComponent);

		// Add actions panel
		actionsContainer = new VerticalLayout();
		hl.addComponent(actionsContainer);
		
		// Set expand ratios
		if (headerComponent != null) {
			setExpandRatio(headerComponent, 5);
			setExpandRatio(hl, 95);
		}
		if (leftComponent != null) {
			hl.setExpandRatio(leftComponent, 20);
			hl.setExpandRatio(bodyComponent, 75);
			hl.setExpandRatio(actionsContainer, 5);
		}
		else {
			hl.setExpandRatio(bodyComponent, 95);
			hl.setExpandRatio(actionsContainer, 5);
		}
		
		// Click listener
		addLayoutClickListener(new LayoutEvents.LayoutClickListener() {
			@Override
			public void layoutClick(LayoutClickEvent event) {
				if (event.isDoubleClick() && event.getClickedComponent() instanceof TextFieldView) {
					TextFieldView txtField = (TextFieldView) event.getClickedComponent();
					txtField.onDoubleClick();
				}
			}
		});
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
		
		if (bodyComponent instanceof Action.Container) {
			Action.Container actionContainer = (Action.Container) bodyComponent;
			actionContainer.addActionHandler(new Action.Handler() {
				@Override
				public void handleAction(Action action, Object sender, Object target) {
					((ShortcutListener) action).handleAction(sender, target);
				}
				@Override
				public Action[] getActions(Object target, Object sender) {
					return (Action[]) activeActions.toArray(new Action[activeActions.size()]);
				}
			});
		}
	}

	protected IResourceCache getResourceCache() {
		return resourceCache;
	}
	
	protected LOGIC getLogic() {
		return logic;
	}
	
	public void enableShortcut(ButtonBasedShortcutListener shortcutListener) {
		activeActions.add(shortcutListener);
		// Else, we must preserve the initial order when restoring the action
		Collections.sort(activeActions, new Comparator<ButtonBasedShortcutListener>() {
			@Override
			public int compare(ButtonBasedShortcutListener o1,
					ButtonBasedShortcutListener o2) {
				return new Integer(orderedActions.indexOf(o1)).compareTo(orderedActions.indexOf(o2));
			}
		});
		addShortcutListener(shortcutListener);
		forceActionHandlersReload();
	}

	private void forceActionHandlersReload() {
		bodyComponent.markAsDirty(); // Forces action handler cache reload
	}
	
	public void disableShortcut(ButtonBasedShortcutListener shortcutListener) {
		activeActions.remove(shortcutListener);
		removeShortcutListener(shortcutListener);
		forceActionHandlersReload();
	}
	
	@Override
	public void addButton(View<?> buttonView) {
		Button button = (Button) buttonView;
		actionsContainer.addComponent(button);
		if (buttonView instanceof StandardButtonView) {
			ButtonBasedShortcutListener shortcut = ((StandardButtonView) buttonView).getShortcut();
			if (shortcut != null) {
				orderedActions.add(shortcut);
				if (button.isEnabled()) {
					enableShortcut(shortcut);
				}
			}
		}
	}
	
}

