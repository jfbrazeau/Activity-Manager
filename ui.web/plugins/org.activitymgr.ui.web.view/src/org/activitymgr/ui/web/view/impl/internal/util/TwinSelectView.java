package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.activitymgr.ui.web.logic.ITwinSelectFieldLogic;
import org.activitymgr.ui.web.logic.ITwinSelectFieldLogic.View;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class TwinSelectView extends HorizontalLayout implements View {

	private ITwinSelectFieldLogic logic;
	private ListSelect leftSelect;
	private ListSelect rightSelect;
	private Button moveAllRightButton;
	private Button moveRightButton;
	private Button moveLeftButton;
	private Button moveAllLeftButton;
	private List<String> availableItemIds = new ArrayList<String>();
	Map<String, String> labels = new HashMap<String, String>();
	private Button moveUpButton;
	private Button moveDownButton;
	private boolean orderMode;
	private boolean logicNotificationEnabled = false;

	public TwinSelectView() {
		super();
		// Left select
		leftSelect = newSelect();
		addComponent(leftSelect);

		// Middle buttons
		VerticalLayout middleButtonsLayout = new VerticalLayout();
		middleButtonsLayout.setMargin(true);
		middleButtonsLayout.setMargin(new MarginInfo(false, true));
		addComponent(middleButtonsLayout);
		moveAllRightButton = addButton(middleButtonsLayout, ">>");
		moveRightButton = addButton(middleButtonsLayout, ">");
		moveLeftButton = addButton(middleButtonsLayout, "<");
		moveAllLeftButton = addButton(middleButtonsLayout, "<<");

		// Create order buttons (but don't add it to the UI)
		moveUpButton = new Button("Up");
		moveUpButton.setImmediate(true);
		moveDownButton = new Button("Down");
		moveDownButton.setImmediate(true);

		// Right select
		rightSelect = newSelect();
		addComponent(rightSelect);

		ValueChangeListener listener = new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				updateButtonsEnablement();
			}
		};
		leftSelect.addValueChangeListener(listener);
		rightSelect.addValueChangeListener(listener);

		moveAllRightButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				selectAll();
			}
		});
		moveRightButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				moveSelectedItems(leftSelect, rightSelect);
			}
		});
		moveLeftButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				moveSelectedItems(rightSelect, leftSelect);
			}
		});
		moveAllLeftButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				deselectAll();
			}
		});
		moveUpButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				// Moving up a joined group of items is equivalent to move the
				// item that is just before the group, after the group
				List<Object> rightItemIds = new ArrayList<Object>(rightSelect
						.getItemIds());
				List<String> rightSelectedItems = getRightSelectedItemIds();

				// Retrieve the item that is before the group
				String firstSelectedItemId = rightSelectedItems.get(0);
				int idx = rightItemIds.indexOf(firstSelectedItemId) - 1;
				String itemIdToMove = (String) rightItemIds.get(idx);

				// Move the item after the group
				int newIdx = idx + rightSelectedItems.size();
				moveRightSelectItemUpOrDown(itemIdToMove, newIdx);
			}

		});
		moveDownButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				// Moving down a joined group of items is equivalent to move the
				// item that is just after the group, before the group
				List<Object> rightItemIds = new ArrayList<Object>(rightSelect
						.getItemIds());
				List<String> rightSelectedItems = getRightSelectedItemIds();

				// Retrieve the item that is after the group
				String lastSelectedItemId = rightSelectedItems
						.get(rightSelectedItems.size() - 1);
				int idx = rightItemIds.indexOf(lastSelectedItemId) + 1;
				String itemIdToMove = (String) rightItemIds.get(idx);

				// Move the item after the group
				int newIdx = idx - rightSelectedItems.size();
				moveRightSelectItemUpOrDown(itemIdToMove, newIdx);
			}
		});
		logicNotificationEnabled = true;
	}

	private void moveRightSelectItemUpOrDown(String itemId, int newIdx) {
		IndexedContainer container = ((IndexedContainer) rightSelect
				.getContainerDataSource());
		container.removeItem(itemId);
		container.addItemAt(newIdx, itemId);
		rightSelect.setItemCaption(itemId, labels.get(itemId));

		// Update buttons
		updateButtonsEnablement();

		notifyChangeToLogic();
	}

	@Override
	public void showOrderButton() {
		this.orderMode = true;
		// Right buttons
		VerticalLayout orderButtonsLayout = new VerticalLayout();
		orderButtonsLayout.setMargin(true);
		orderButtonsLayout.setMargin(new MarginInfo(false, true));
		addComponent(orderButtonsLayout);
		orderButtonsLayout.addComponent(moveUpButton);
		orderButtonsLayout.addComponent(moveDownButton);
	}

	@Override
	public void addAvailableEntry(String id, String label) {
		availableItemIds.add(id);
		labels.put(id, label);
		leftSelect.addItem(id);
		leftSelect.setItemCaption(id, label);
		updateButtonsEnablement();
	}

	private void updateButtonsEnablement() {
		List<Object> rightItemIds = new ArrayList<Object>(
				rightSelect.getItemIds());
		List<String> rightSelectedItems = getRightSelectedItemIds();

		// Middle buttons enablement
		moveAllRightButton.setEnabled(leftSelect.getItemIds().size() > 0);
		moveRightButton.setEnabled(((Collection<?>) leftSelect.getValue())
				.size() > 0);
		moveLeftButton.setEnabled(rightSelectedItems
				.size() > 0);
		moveAllLeftButton.setEnabled(rightItemIds.size() > 0);

		// Up and down buttons are enabled only if at least one item is selected
		// and if all selected items are joined and if it's not the last items
		// for down button and not the first items for up button
		moveUpButton.setEnabled(false);
		moveDownButton.setEnabled(false);
		if (rightSelectedItems.size() > 0) {
			Iterator<String> rightSelectedItemIdsIterator = rightSelectedItems
					.iterator();
			String firstSelectedItemId = rightSelectedItemIdsIterator.next();
			int firstIdx = rightItemIds.indexOf(firstSelectedItemId);
			boolean joined = true;
			int idx = firstIdx + 1;
			while (rightSelectedItemIdsIterator.hasNext()) {
				String nextSelectedItemId = rightSelectedItemIdsIterator.next();
				String nextItemId = (String) rightItemIds.get(idx++);
				if (!nextSelectedItemId.equals(nextItemId)) {
					joined = false;
					break;
				}
			}
			if (joined) {
				moveUpButton.setEnabled(firstIdx != 0);
				boolean isAtLast = firstIdx + rightSelectedItems.size() == rightItemIds
						.size();
				moveDownButton.setEnabled(!isAtLast);
			}
		}
	}

	private List<String> getRightSelectedItemIds() {
		final List<Object> rightItemIds = new ArrayList<Object>(
				rightSelect.getItemIds());
		@SuppressWarnings("unchecked")
		List<String> rightSelectedItems = new ArrayList<String>(
				(Collection<String>) rightSelect.getValue());
		Collections.sort(rightSelectedItems, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				return rightItemIds.indexOf((String) o1)
						- rightItemIds.indexOf((String) o2);
			}
		});
		return rightSelectedItems;
	}

	private Button addButton(VerticalLayout middleButtonsLayout, String title) {
		Button button = new Button(title);
		button.setImmediate(true);
		middleButtonsLayout.addComponent(button);
		middleButtonsLayout.setComponentAlignment(button,
				Alignment.MIDDLE_CENTER);
		button.setEnabled(false);
		return button;
	}

	private ListSelect newSelect() {
		ListSelect select = new ListSelect();
		select.setImmediate(true);
		select.setMultiSelect(true);
		select.setWidth("130px");
		select.setHeight("100px");
		return select;
	}

	@Override
	public void registerLogic(ITwinSelectFieldLogic logic) {
		this.logic = logic;
	}

	private void moveAllItemTo(ListSelect select) {
		// Clear both selects
		leftSelect.removeAllItems();
		rightSelect.removeAllItems();
		// Add all entries to the given select
		for (String id : availableItemIds) {
			select.addItem(id);
			select.setItemCaption(id, labels.get(id));
		}
		// Update buttons
		updateButtonsEnablement();
		notifyChangeToLogic();
	}

	@SuppressWarnings("unchecked")
	private void moveSelectedItems(ListSelect from, ListSelect destination) {
		// If we move from left to right, and if up and down buttons are shown,
		// simply append the items at the end. Otherwise, preserve the original
		// order
		boolean doNotPreserveOriginalOrder = (destination == rightSelect && orderMode);
		IndexedContainer destinationContainer = ((IndexedContainer) destination
				.getContainerDataSource());
		Collection<String> itemIdsToMove = (Collection<String>) from.getValue();
		for (String itemIdToMove : itemIdsToMove) {
			if (!doNotPreserveOriginalOrder) {
				int globalItemIdToMoveIndex = availableItemIds
						.indexOf(itemIdToMove);
				int idx = 0;
				Collection<String> actualItemIds = (Collection<String>) destination
						.getItemIds();
				for (String actualItemId : actualItemIds) {
					int globalActualItemId = availableItemIds
							.indexOf(actualItemId);
					if (globalItemIdToMoveIndex < globalActualItemId) {
						break;
					} else {
						idx++;
					}
				}
				destinationContainer.addItemAt(idx, itemIdToMove);
			} else {
				destinationContainer.addItem(itemIdToMove);
			}
			destination.setItemCaption(itemIdToMove, labels.get(itemIdToMove));
			from.removeItem(itemIdToMove);
		}
		// Update buttons
		updateButtonsEnablement();
		// Notify the logic
		notifyChangeToLogic();
	}

	@Override
	public void selectAll() {
		moveAllItemTo(rightSelect);
	}

	private void deselectAll() {
		moveAllItemTo(leftSelect);
	}

	@Override
	public void setValue(Collection<String> value) {
		logicNotificationEnabled = false;
		try {
			if (orderMode) {
				// Remove elements from the right
				List<Object> itemsToRemove = new ArrayList<Object>();
				for (Object itemId : rightSelect.getItemIds()) {
					if (!value.contains(itemId)) {
						itemsToRemove.add(itemId);
					}
				}
				for (Object itemId : itemsToRemove) {
					rightSelect.removeItem(itemId);
					leftSelect.addItem(itemId);
					leftSelect.setItemCaption(itemId, labels.get(itemId));
				}

				// In order mode, simply move elements
				// (don't use move function, or value order
				// will not be preserved)
				for (String itemId : value) {
					if (leftSelect.containsId(itemId)) {
						leftSelect.removeItem(itemId);
						rightSelect.addItem(itemId);
						rightSelect.setItemCaption(itemId, labels.get(itemId));
					}
				}
				// Notify update
				notifyChangeToLogic();
			} else {
				// Remove elements from the right
				for (Object itemId : rightSelect.getItemIds()) {
					if (!value.contains(itemId)) {
						rightSelect.select(itemId);
					}
				}

				moveSelectedItems(rightSelect, leftSelect);
				// In non order mode, reuse move function
				// (so that elements will be sorted)
				for (String itemId : value) {
					if (leftSelect.containsId(itemId)) {
						leftSelect.select(itemId);
					}
				}
				moveSelectedItems(leftSelect, rightSelect);
			}
		} finally {
			logicNotificationEnabled = true;
		}
	}

	private void notifyChangeToLogic() {
		if (logicNotificationEnabled) {
			@SuppressWarnings("unchecked")
			Collection<String> itemIds = (Collection<String>) rightSelect
					.getItemIds();
			logic.onValueChanged(itemIds);
		}
	}

	@Override
	public void focus() {
		super.focus();
	}
}
