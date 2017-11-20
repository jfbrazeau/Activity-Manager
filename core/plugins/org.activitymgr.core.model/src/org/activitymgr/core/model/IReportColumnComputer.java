package org.activitymgr.core.model;

import org.activitymgr.core.dto.report.ReportItem;

public interface IReportColumnComputer {
	
	public abstract class Impl implements IReportColumnComputer {
		
		private String name;
		private boolean isSummable;

		public Impl(String name, boolean isSummable) {
			this.name = name;
			this.isSummable = isSummable;
		}
		
		@Override
		public final String getName() {
			return name;
		}
		
		@Override
		public final boolean isSummable() {
			return isSummable;
		}
		
	}
	
	String getName();
	
	Object compute(ReportItem item);

	boolean isSummable();

}
