package org.activitymgr.core.dto.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSums;

public class ReportItem {
	
	private final Collaborator contributor;
	
	private final Collection<Task> tasks = new ArrayList<Task>();
	
	private final TaskSums contributedTask;

	private final long[] contributionSums;
	
	public ReportItem(Report parent, Collaborator contributor,
			TaskSums contributedTask, Task... parentTasks) {
		contributionSums = new long[parent.getIntervalCount()];
		if (parentTasks != null && (parentTasks.length > parent.getTaskDepth())) {
			throw new IllegalStateException("Invalid task count, expected : " + parent.getTaskDepth() + ", actual:" + parentTasks.length);
		}
		if (parentTasks != null) {
			this.tasks.addAll(Arrays.asList(parentTasks));
		}
		if (contributedTask != null) {
			this.tasks.add(contributedTask.getTask());
		}
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
