package org.activitymgr.ui.web.logic;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;

public interface IConstraintsValidator {
	
	public static interface IStatus {
		
		boolean isError();
		
		String getErrorReason();
	}
	
	public static IStatus OK_STATUS = new IStatus() {
		@Override
		public boolean isError() {
			return false;
		}
		
		@Override
		public String getErrorReason() {
			return null;
		}
	};

	public static class ErrorStatus implements IStatus {
		
		private String message;
		
		public ErrorStatus(String message) {
			this.message = message;
		}
		@Override
		public boolean isError() {
			return true;
		}
		
		@Override
		public String getErrorReason() {
			return message;
		}
	};
	
	/**
	 * Tells whether the given task can accept sub tasks.
	 * @param task the task.
	 * @return <code>true</code> if the task can accept sub tasks.
	 */
	IStatus canCreateSubTaskUnder(Task task) throws ModelException;

}
