package org.activitymgr.core.dto.report;

import java.util.Arrays;
import java.util.Collection;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSums;

public class ReportItem {
	
	private final Collaborator contributor;
	
	private final Collection<Task> tasks;
	
	private final TaskSums contributedTask;

	private final long[] contributionSums;
	
	public ReportItem(Report parent, Collaborator contributor,
			TaskSums contributedTask, Task... tasks) {
		contributionSums = new long[parent.getIntervalCount()];
		if (tasks != null && (tasks.length > parent.getTaskDepth())) {
			throw new IllegalStateException("Invalid task count, expected : " + parent.getTaskDepth() + ", actual:" + tasks.length);
		}
		this.tasks = tasks != null ? Arrays.asList(tasks) : null;
		this.contributor = contributor;
		this.contributedTask = contributedTask;
		parent.add(this);
	}
	
	public long getContributionSum(int dateIdx) {
		return contributionSums[dateIdx];
	}
	
	public void addToContributionSum(int dateIdx, long duration) {
		contributionSums[dateIdx] += duration;
	}

	public Collection<Task> getTasks() {
		return tasks;
	}

	public Collaborator getContributor() {
		return contributor;
	}

	public TaskSums getContributedTask() {
		return contributedTask;
	}

}
