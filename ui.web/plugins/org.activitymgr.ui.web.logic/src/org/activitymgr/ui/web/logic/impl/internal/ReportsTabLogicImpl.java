package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activitymgr.core.dto.ReportCfg;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.IGenericCallback;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.logic.IStandardButtonLogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.impl.AbstractSafeGenericCallback;
import org.activitymgr.ui.web.logic.impl.AbstractSafeStandardButtonLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;

import com.google.inject.Inject;

public class ReportsTabLogicImpl extends
		AbstractTabLogicImpl<IReportsTabLogic.View> implements IReportsTabLogic {

	@Inject(optional = true)
	private Set<ITabButtonFactory<IReportsTabLogic>> buttonFactories;
	
	private String tabLabel;

	private String category;

	private List<ReportCfg> reportCfgs = new ArrayList<ReportCfg>();

	private Map<Long, ReportCfg> reportCfgsMap = new HashMap<Long, ReportCfg>();

	private List<ReportCfg> selectedReportCfgs = new ArrayList<ReportCfg>();

	private IStandardButtonLogic newReportButton;

	private IStandardButtonLogic saveReportButton;

	private IStandardButtonLogic duplicateReportButton;

	private IStandardButtonLogic removeReportButton;

	private ReportsLogicImpl reportsLogic;

	private boolean dirty;

	public ReportsTabLogicImpl(ITabFolderLogic parent,
			final boolean advancedMode) {
		super(parent);

		tabLabel = advancedMode ? "Adv. reports" : "My reports";
		category = advancedMode ? "advanced-reports" : "self-reports";

		// Add buttons
		registerButtons(buttonFactories);

		// In advanced mode, the list is longer than in basic mode
		getView().setLongReportsList(advancedMode);

		// Add report configurations buttons
		final IGenericCallback<String> inputDialogCallback = buildTransactionalWrapper(
				new AbstractSafeGenericCallback<String>(
						ReportsTabLogicImpl.this) {
					protected void unsafeCallback(String result)
							throws Exception {
						if ("".equals(result.trim())) {
							getRoot().getView().showNotification(
									"Name cannot be empty");
						} else {
							ReportCfg cfg = new ReportCfg();
							cfg.setCategory(category);
							cfg.setOwnerId(getContext()
									.getConnectedCollaborator().getId());
							cfg.setName(result.trim());
							cfg = getModelMgr().createReportCfg(cfg);
							registerReportCfg(cfg);
							sortReportCfgs();
							getView().addReportCfg(cfg.getId(), cfg.getName(),
									reportCfgs.indexOf(cfg));
							getView().selectReportCfg(cfg.getId());
						}
					}
				}, IGenericCallback.class);
		newReportButton = new AbstractSafeStandardButtonLogicImpl(
				this, "New", "new",
				null) {
			@Override
			protected void unsafeOnClick() throws Exception {
				getRoot().getView().simpleInput(
						"Please enter a name for the new report :", null,
						inputDialogCallback);
			}
		};
		getView().addReportConfigurationButton(newReportButton.getView());
		saveReportButton = new AbstractSafeStandardButtonLogicImpl(this,
				"Save",
				"save", null) {
			@Override
			protected void unsafeOnClick() throws Exception {
				ReportCfg cfgToSave = selectedReportCfgs.iterator().next();
				cfgToSave.setConfiguration(reportsLogic.toJson());
				getModelMgr().updateReportCfg(cfgToSave);
				dirty = false;
				getRoot().getView().showNotification(
						"Report configuration saved");
				updateUI();
			}
		};
		getView().addReportConfigurationButton(saveReportButton.getView());
		final IGenericCallback<String> duplicateReportCfgCallback = buildTransactionalWrapper(
				new AbstractSafeGenericCallback<String>(this) {
					@Override
					protected void unsafeCallback(String result)
							throws Exception {
						if ("".equals(result.trim())) {
							getRoot().getView().showNotification(
									"Name cannot be empty");
						} else {
							ReportCfg cfgToDuplicate = selectedReportCfgs
									.iterator().next();
							ReportCfg cfg = new ReportCfg();
							cfg.setCategory(cfgToDuplicate.getCategory());
							cfg.setOwnerId(cfgToDuplicate.getOwnerId());
							cfg.setName(result.trim());
							cfg.setConfiguration(cfgToDuplicate
									.getConfiguration());
							cfg = getModelMgr().createReportCfg(cfg);
							registerReportCfg(cfg);
							sortReportCfgs();
							getView().addReportCfg(cfg.getId(), cfg.getName(),
									reportCfgs.indexOf(cfg));
							getView().selectReportCfg(cfg.getId());
						}
					}
				}, IGenericCallback.class);
		duplicateReportButton = new AbstractSafeStandardButtonLogicImpl(this,
				"Duplicate", "duplicate", null) {
			@Override
			protected void unsafeOnClick() throws Exception {
				ReportCfg cfgToDuplicate = selectedReportCfgs.iterator().next();
				getRoot().getView().simpleInput(
						"Please enter a name for the new report :",
						cfgToDuplicate.getName() + " (copy)",
						duplicateReportCfgCallback);
			}
		};
		getView().addReportConfigurationButton(duplicateReportButton.getView());
		final IGenericCallback<Boolean> removeReportCfgCallback = buildTransactionalWrapper(
				new AbstractSafeGenericCallback<Boolean>(this) {
					@Override
					protected void unsafeCallback(Boolean okClicked)
							throws Exception {
						if (okClicked) {
							for (ReportCfg cfgRoRemove : selectedReportCfgs) {
								getModelMgr().removeReportCfg(
										cfgRoRemove.getId());
								ReportsTabLogicImpl.this.getView()
										.removeReportCfg(cfgRoRemove.getId());
								ReportsTabLogicImpl.this.reportCfgs.remove(cfgRoRemove);
								reportCfgsMap.remove(cfgRoRemove.getId());
							}
							selectedReportCfgs.clear();
							updateUI();
						}
					}
				}, IGenericCallback.class);
		removeReportButton = new AbstractSafeStandardButtonLogicImpl(this,
				"Remove", "remove", null) {
			@Override
			protected void unsafeOnClick() throws Exception {
				getRoot()
						.getView()
						.showConfirm(
								"Are you sure you want to remove this(these) report(s) ?",
								removeReportCfgCallback);
			}
		};
		getView().addReportConfigurationButton(removeReportButton.getView());
		
		try {
			ReportCfg[] reportCfgs = getModelMgr().getReportCfgs(category,
					getContext().getConnectedCollaborator().getId());
			for (ReportCfg reportCfg : reportCfgs) {
				registerReportCfg(reportCfg);
			}
			sortReportCfgs();
			int idx = 0;
			for (ReportCfg reportCfg : reportCfgs) {
				getView().addReportCfg(reportCfg.getId(), reportCfg.getName(),
						idx++);
			}
		} catch (ModelException e) {
			throw new IllegalStateException(e);
		}
		// Add a report logic
		reportsLogic = new ReportsLogicImpl(this, advancedMode) {
			@Override
			protected void onReportConfigurationChanged(String json) {
				boolean oldDirty = dirty;
				if (selectedReportCfgs.size() > 0) {
					ReportCfg editedCfg = selectedReportCfgs.iterator().next();
					dirty = !json.equals(editedCfg.getConfiguration());
				} else {
					dirty = false;
				}
				if (oldDirty != dirty) {
					updateUI();
				}
			}
		};
		getView().setReportsView(reportsLogic.getView());

		// Auto select first entry
		if (reportCfgs.size() > 0) {
			ReportCfg first = reportCfgs.iterator().next();
			selectedReportCfgs.add(first);
			getView().selectReportCfg(first.getId());
		}

		// Update the UI
		updateUI();
	}

	private void registerReportCfg(ReportCfg reportCfg) {
		this.reportCfgs.add(reportCfg);
		reportCfgsMap.put(reportCfg.getId(), reportCfg);
	}

	@Override
	public String getLabel() {
		return tabLabel;
	}

	private void updateUI() {
		boolean emptySelection = selectedReportCfgs == null
				|| selectedReportCfgs.size() == 0;
		boolean singleSelection = !emptySelection
				&& selectedReportCfgs.size() == 1;
		getView().setReportsPanelEnabled(singleSelection);
		saveReportButton.getView().setEnabled(dirty);
		duplicateReportButton.getView().setEnabled(singleSelection);
		removeReportButton.getView().setEnabled(!emptySelection);
	}

	@Override
	public void onSelectionChanged(final Collection<Long> values) {
		final IGenericCallback<Boolean> changeSelectionCallback = buildTransactionalWrapper(
				new AbstractSafeGenericCallback<Boolean>(this) {
					@Override
					protected void unsafeCallback(Boolean okClicked)
							throws Exception {
						try {
							setViewNotificationsEnabled(false);
							if (okClicked) {
								selectedReportCfgs.clear();
								for (Long value : values) {
									ReportCfg rc = reportCfgsMap.get(value);
									if (rc != null) {
										selectedReportCfgs.add(rc);
									}
								}
								if (selectedReportCfgs.size() != 1) {
									reportsLogic.loadFromJson(null);
								} else {
									ReportCfg cfg = selectedReportCfgs
											.iterator().next();
									reportsLogic.loadFromJson(cfg
											.getConfiguration());
									// Ensure configuration is the same
									// even after UI initialization
									cfg.setConfiguration(reportsLogic.toJson());
								}
								dirty = false;
								updateUI();
							} else {
								ReportCfg cfg = selectedReportCfgs.iterator()
										.next();
								getView().selectReportCfg(cfg.getId());
							}
						} finally {
							setViewNotificationsEnabled(true);
						}
					}
				}, IGenericCallback.class);
		if (dirty) {
			getRoot().getView().showConfirm("Current report is not saved, continue ?", changeSelectionCallback);
		} else {
			// simulate a click on OK
			changeSelectionCallback.callback(true);
		}
	}

	private void sortReportCfgs() {
		Collections.sort(reportCfgs, new Comparator<ReportCfg>() {
			@Override
			public int compare(ReportCfg o1, ReportCfg o2) {
				return o1.getName().toLowerCase()
						.compareTo(o2.getName().toLowerCase());
			}
		});
	}

}

