package org.activitymgr.core;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.ReportCfg;
import org.activitymgr.core.model.ModelException;

public class ReportConfigurationsTest extends AbstractModelTestCase {

	public void testCreateUpdateAnRemove() throws ModelException {
		Collaborator collaborator = createSampleCollaborator();

		// Insert
		ReportCfg cfg = new ReportCfg();
		cfg.setCategory("category");
		cfg.setOwnerId(collaborator.getId());
		cfg.setName("name");
		cfg.setConfiguration("configuration");
		assertEquals(0, cfg.getId());
		cfg = getModelMgr().createReportCfg(cfg);
		assertTrue(cfg.getId() > 0);

		// Update
		cfg.setCategory("coucou");
		getModelMgr().updateReportCfg(cfg);
		ReportCfg loadedCfg = getModelMgr().getReportCfg(cfg.getId());
		assertEquals(cfg.getCategory(), loadedCfg.getCategory());

		// Remove
		getModelMgr().removeReportCfg(cfg.getId());
		loadedCfg = getModelMgr().getReportCfg(cfg.getId());
		assertNull(loadedCfg);
	}

	public void testCreateWithoutCategory() throws ModelException {
		Collaborator collaborator = createSampleCollaborator();

		// Insert
		ReportCfg cfg = new ReportCfg();
		cfg.setCategory(null); // NULL CATEGORY
		cfg.setOwnerId(collaborator.getId());
		cfg.setName("name");
		cfg.setConfiguration("configuration");
		assertEquals(0, cfg.getId());
		try {
			cfg = getModelMgr().createReportCfg(cfg);
			fail("New report configuration without category should fail");
		} catch (ModelException e) {
		}
	}

	public void testCreateWithoutName() throws ModelException {
		Collaborator collaborator = createSampleCollaborator();

		// Insert
		ReportCfg cfg = new ReportCfg();
		cfg.setCategory("category");
		cfg.setOwnerId(collaborator.getId());
		cfg.setName(null); // NULL NAME
		cfg.setConfiguration("configuration");
		assertEquals(0, cfg.getId());
		try {
			cfg = getModelMgr().createReportCfg(cfg);
			fail("New report configuration without category should fail");
		} catch (ModelException e) {
		}
	}

	/*
	 * No owner id must be permitted
	 */
	public void testCreateWithoutOwnerId() throws ModelException {
		// Insert
		ReportCfg cfg = new ReportCfg();
		cfg.setCategory("category");
		cfg.setOwnerId(null); // NULL owner
		cfg.setName("Name");
		cfg.setConfiguration("configuration");
		assertEquals(0, cfg.getId());
		cfg = getModelMgr().createReportCfg(cfg);
		

		ReportCfg[] reportCfgs = getModelMgr().getReportCfgs("category", null);
		assertNotNull(reportCfgs);
		assertEquals(1, reportCfgs.length);
		ReportCfg newCfg = reportCfgs[0];
		assertEquals(cfg.getId(), newCfg.getId());
		assertNull(newCfg.getOwnerId());
	}

	/*
	 * No configuration id must be permitted
	 */
	public void testCreateWithoutConfiguration() throws ModelException {
		Collaborator collaborator = createSampleCollaborator();
		// Insert
		ReportCfg cfg = new ReportCfg();
		cfg.setCategory("category");
		cfg.setOwnerId(collaborator.getId());
		cfg.setName("Name");
		cfg.setConfiguration(null);
		assertEquals(0, cfg.getId());
		cfg = getModelMgr().createReportCfg(cfg);

		ReportCfg[] reportCfgs = getModelMgr().getReportCfgs("category",
				collaborator.getId());
		assertNotNull(reportCfgs);
		assertEquals(1, reportCfgs.length);
		ReportCfg newCfg = reportCfgs[0];
		assertEquals(cfg.getId(), newCfg.getId());
		assertNull(newCfg.getConfiguration());
	}

	private Collaborator createSampleCollaborator() throws ModelException {
		// Create a collaborator
		Collaborator collaborator = getFactory().newCollaborator();
		collaborator.setFirstName("First name");
		collaborator.setLastName("Last name");
		collaborator.setLogin("login");
		collaborator.setIsActive(true);
		collaborator = getModelMgr().createCollaborator(collaborator);
		assertTrue(collaborator.getId() > 0);
		return collaborator;
	}

}
