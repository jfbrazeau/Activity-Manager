package org.activitymgr.ui.web.logic.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITwinSelectLogic;

public class TwinSelectLogic<DTO> extends
		AbstractLogicImpl<ITwinSelectLogic.View>
		implements ITwinSelectLogic {

	public static interface IDTOInfosProvider<DTO> {

		String getLabel(DTO dto);

		String getId(DTO dto);

	}

	private Map<String, DTO> dtosById = new HashMap<String, DTO>();

	private Collection<String> selectedDTOIds = new ArrayList<String>();

	private IDTOInfosProvider<DTO> dtoInfoProvider;

	public TwinSelectLogic(ILogic<?> parent, boolean ordered,
			IDTOInfosProvider<DTO> dtoInfoProvider, DTO... dtos) {
		super(parent);
		this.dtoInfoProvider = dtoInfoProvider;
		for (DTO dto : dtos) {
			String id = dtoInfoProvider.getId(dto);
			dtosById.put(id, dto);
			getView().addAvailableEntry(id, dtoInfoProvider.getLabel(dto));
		}
		if (ordered) {
			getView().showOrderButton();
		}
	}

	@Override
	public void onValueChangedChanged(Collection<String> itemIds) {
		selectedDTOIds = itemIds;
	}

	public List<DTO> getValue() {
		List<DTO> result = new ArrayList<DTO>();
		if (selectedDTOIds != null) {
			for (String dtoId : selectedDTOIds) {
				result.add(dtosById.get(dtoId));
			}
		}
		return result;
	}

	public void selectAll() {
		getView().selectAll();
	}

	public void select(DTO... dtos) {
		for (DTO dto : dtos) {
			getView().select(dtoInfoProvider.getId(dto));
		}
	}

}
