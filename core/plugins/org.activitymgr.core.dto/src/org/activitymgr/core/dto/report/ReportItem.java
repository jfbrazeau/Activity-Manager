package org.activitymgr.core.dto.report;

import java.util.Arrays;
import java.util.Collection;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;

public class ReportItem {
	
	private final Collaborator contributor;
	
	private final Collection<Task> tasks;
	
	private final Task contributedTask;

	private final long[] contributionSums;
	
	public ReportItem(Report parent, Collaborator contributor, Task... tasks) {
		contributionSums = new long[parent.getIntervalCount()];
		if (tasks != null && (tasks.length > parent.getTaskDepth())) {
			throw new IllegalStateException("Invalid task count, expected : " + parent.getTaskDepth() + ", actual:" + tasks.length);
		}
		this.tasks = tasks != null ? Arrays.asList(tasks) : null;
		this.contributor = contributor;
		this.contributedTask = (tasks != null && tasks.length > 0) ? tasks[tasks.length - 1] : null;
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

	public Task getContributedTask() {
		return contributedTask;
	}

}
