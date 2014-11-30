package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.misc.TaskContributions;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.impl.event.DurationChangedEvent;
import org.activitymgr.ui.web.logic.impl.internal.ContributionsTabLogicImpl;

public class DefaultContributionCellLogicProvider implements IContributionCellLogicProviderExtension {
	
	@Override
	public ILogic<?> getCellLogic(final AbstractContributionLogicImpl parent, final Collaborator contributor, final String columnId,
			final TaskContributions weekContributions) {
		if (DAY_COLUMNS_IDENTIFIERS.contains(columnId)) {
			final int dayOfWeek = DAY_COLUMNS_IDENTIFIERS.indexOf(columnId);
			Contribution c = weekContributions.getContributions()[dayOfWeek];
			String duration = (c == null) ? "" : StringHelper.hundredthToEntry(c.getDurationId());
			ITextFieldLogic textFieldLogic = new AbstractTextFieldLogicImpl((ContributionsTabLogicImpl) parent, duration) {
				@Override
				public void onValueChanged(String newValue) {
					parent.getContext().getEventBus().fire(new DurationChangedEvent(parent, weekContributions, dayOfWeek, newValue, this));
				}
			};
			textFieldLogic.getView().setNumericFieldStyle();
			return textFieldLogic;
		}
		else if (IContributionCellLogicProviderExtension.PATH_COLUMN_ID.equals(columnId)) {
			return new LabelLogicImpl((ContributionsTabLogicImpl) parent, weekContributions.getTaskCodePath());
		}
		else if (IContributionCellLogicProviderExtension.NAME_COLUMN_ID.equals(columnId)) {
			return new LabelLogicImpl((ContributionsTabLogicImpl) parent, weekContributions.getTask().getName());
		}
		else if (IContributionCellLogicProviderExtension.TOTAL_COLUMN_ID.equals(columnId)) {
			return new LabelLogicImpl((ContributionsTabLogicImpl) parent, "");
		}
		else {
			throw new IllegalArgumentException("Unexpected column identifier '" + columnId + "'");
		}
	}

}