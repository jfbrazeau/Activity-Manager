package org.activitymgr.core.report;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.activitymgr.core.AbstractModelTestCase;
import org.activitymgr.core.dao.IContributionDAO;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.report.Report;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.dto.report.ReportItem;
import org.activitymgr.core.model.ModelException;
import org.apache.poi.util.HexDump;
import org.xml.sax.SAXException;

public class ReportTest extends AbstractModelTestCase {
	
	private static final int SAMPLE_DATA_WEEK_COUNT = 10;
	private static final String START_PROP = "start";
	private static final String INTERVAL_TYPE_PROP = "intervalType";
	private static final String INTERVAL_COUNT_PROP = "intervalCount";
	private static final String ROOT_TASK_CODE_PATH_PROP = "rootTaskCodePath";
	private static final String TASK_DEPTH_PROP = "taskDepth";
	private static final String BY_CONTRIBUTOR_PROP = "byContributor";
	private static final String ORDER_BY_CONTRIBUTOR_PROP = "orderByContributor";
	
	private static class ContribDef {
		final int dayIdx;
		final Collaborator contributor;
		final Task task;
		final long durationId;
		ContribDef(int dayIdx, Collaborator contributor, Task task, long durationId) {
			this.dayIdx = dayIdx;
			this.contributor = contributor;
			this.task = task;
			this.durationId = durationId;
		}
	}
	
	private Collaborator jdoe;

	private Collaborator wsmith;
	
	private Calendar sampleDataStart;
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		loadSampleModel();
	}

	public void testReportWithoutStartNorContributions() throws IOException {
		try {
			IContributionDAO cDAO = getInjector().getInstance(IContributionDAO.class);
			// Remove all contributions
			cDAO.delete(null, null);
			// Launch a report that is expected to fail
			doBuildReport(
					null, // no start date 
					ReportIntervalType.MONTH, // month
					3, // 3 months
					null, // No root task
					0, // No depth 
					true, // By contributor
					true // Order by contributor
					);
			fail("A model exception should be raised !");
		}
		catch (ModelException e) {
			
		}
	}
	
	public void testReportWithoutStartNorInterval() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportWithoutStart() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportWithoutInterval() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportOneDayNoRootTask() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportOneDayWithRootTask() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportTwoDaysNoRootTask() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportTwoDaysWithRootTask() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportOneWeekNoRootTask() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportOneWeekWithRootTask() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportTwoWeeksNoRootTask() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportTwoWeeksWithRootTask() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportThreeMonthsNoRootTask() throws IOException, ModelException {
		doTestReport();
	}
	
	public void testReportOneMonthWithRootTask() throws IOException, ModelException {
		doTestReport();
	}
	
	public void testReportTwoYearsNoRootTask() throws IOException, ModelException {
		doTestReport();
	}
	
	private void doTestReport() throws IOException, ModelException {
		String testName = getName();
		String fileName = testName.substring(4) + ".txt";
		System.out.println(fileName);
		BufferedReader in = new BufferedReader(new InputStreamReader(ReportTest.class.getResourceAsStream(fileName), Charset.forName("UTF-8")));
		String line = "";

		Properties props = new Properties();

		while (line != null) {
			// Load properties
			StringWriter propsSw = new StringWriter();
			while ((line = in.readLine())!=null && !line.startsWith("+-")) {
				propsSw.append(line);
				propsSw.append('\n');
			}
			props.load(new StringReader(propsSw.toString()));
	
			// Load expected result
			StringWriter expectedSw = new StringWriter();
			expectedSw.append(line);
			expectedSw.append('\n');
			while ((line = in.readLine()) != null && !line.startsWith("#")) {
				line = line.trim();
				if (!"".equals(line)) {
					expectedSw.append(line);
					expectedSw.append('\n');
				}
			}
			String expected = expectedSw.toString();
			
			Long rootTaskId = null;
			String rootTaskCodePath = props.getProperty(ROOT_TASK_CODE_PATH_PROP);
			if (rootTaskCodePath != null) {
				rootTaskCodePath = rootTaskCodePath.trim();
				if (!"".equals(rootTaskCodePath)) {
					rootTaskId = getModelMgr().getTaskByCodePath(rootTaskCodePath).getId();
				}
			}
			
			String intervalCountProperty = props.getProperty(INTERVAL_COUNT_PROP);
			Report report = doBuildReport(
					cal(props.getProperty(START_PROP)), // Start date
					ReportIntervalType.valueOf(props.getProperty(INTERVAL_TYPE_PROP).toUpperCase()), // interval type
					intervalCountProperty != null ? Integer.parseInt(intervalCountProperty) : null, // interval count
					rootTaskId, // root task
					Integer.parseInt(props.getProperty(TASK_DEPTH_PROP)), // Task depth
					Boolean.parseBoolean(props.getProperty(BY_CONTRIBUTOR_PROP)), // By contributor
					Boolean.parseBoolean(props.getProperty(ORDER_BY_CONTRIBUTOR_PROP)) // Order by contributor
					);
			assertNotNull(report);
			List<ReportItem> items = report.getItems();
			assertNotNull(items);
			assertEquals(expected, report.toString());
		}
	}
	
	private Report doBuildReport(Calendar start, ReportIntervalType intervalType, Integer intervalCount, Long rootTaskId, int taskDepth, boolean byContributor, boolean orderByContributor) throws ModelException {
		System.out.println("buildReport(");
		System.out.println("  "
				+ (start != null ? (start.get(Calendar.YEAR) + "/"
						+ (start.get(Calendar.MONTH) + 1) + "/" + start
						.get(Calendar.DATE)) : null) + ", // start");
		System.out.println("  " + intervalType + ", // intervalType");
		System.out.println("  " + intervalCount + ", // intervalCount");
		System.out.println("  " + rootTaskId + ", // rootTaskId");
		System.out.println("  " + taskDepth + ", // taskDepth");
		System.out.println("  " + byContributor + ", // byContributor");
		System.out.println("  " + orderByContributor + ") // orderByContributor");
		return getModelMgr().buildReport(start, intervalType, intervalCount, rootTaskId, taskDepth, byContributor, orderByContributor);
	}

	private Calendar cal(String cal) {
		if (cal == null) {
			return null;
		}
		else if (cal.length() != 8) {
			throw new IllegalArgumentException("Invalide date, must match ddMMyyyy");
		}
		return cal(Integer.parseInt(cal.substring(4, 8)), Integer.parseInt(cal.substring(2, 4)), Integer.parseInt(cal.substring(0, 2)));
	}
	private Calendar cal(int year, int month, int day) {
		//System.out.println("cal(" + year + ", "+ month + ", " + day +  ")");
		Calendar start = Calendar.getInstance(Locale.FRANCE);
		start.set(Calendar.YEAR, year);
		start.set(Calendar.MONTH, month - 1);
		start.set(Calendar.DAY_OF_MONTH, day);
		return start;
	}

	private void loadSampleModel() throws FileNotFoundException, IOException,
			ParserConfigurationException, SAXException, ModelException {
		// Create sample data start date
		sampleDataStart = cal(2016, 12, 5);
		sampleDataStart.set(Calendar.HOUR_OF_DAY, 12);
		sampleDataStart.set(Calendar.MINUTE, 0);
		sampleDataStart.set(Calendar.SECOND, 0);
		sampleDataStart.set(Calendar.MILLISECOND, 0);
		
		// Ouverture du fichier de test
		String filename = "ReportTest.xml";
		InputStream in = ReportTest.class.getResourceAsStream(filename);
		if (in==null) {
			throw new FileNotFoundException(filename);
		}
		// Importation des données
		getModelMgr().importFromXML(in);
		// Fermeture du flux
		in.close();
		
		// Collaborators retrieval
		jdoe = getModelMgr().getCollaborator("jdoe");
		wsmith = getModelMgr().getCollaborator("wsmith");
		
		// Tasks retrieval
		Task specificationA = getModelMgr().getTaskByCodePath("/PR/PA/SPE");
		Task moduleDevA = getModelMgr().getTaskByCodePath("/PR/PA/DEV/MA");
		Task moduleDevB = getModelMgr().getTaskByCodePath("/PR/PA/DEV/MB");
		Task testA = getModelMgr().getTaskByCodePath("/PR/PA/TST");
		Task specificationB = getModelMgr().getTaskByCodePath("/PR/PB/SPE");
		Task moduleDevC = getModelMgr().getTaskByCodePath("/PR/PB/DEV/MC");
		Task moduleDevD = getModelMgr().getTaskByCodePath("/PR/PB/DEV/MD");
		Task testB = getModelMgr().getTaskByCodePath("/PR/PB/TST");
		Task vacations = getModelMgr().getTaskByCodePath("/ABS/VAC");
		Task illness = getModelMgr().getTaskByCodePath("/ABS/ILL");

		// Typical week definition
		List<ContribDef> weekDef = new ArrayList<ReportTest.ContribDef>();
		weekDef.add(new ContribDef(0, jdoe, specificationA, 25));
		weekDef.add(new ContribDef(0, jdoe, testA, 25));
		weekDef.add(new ContribDef(0, jdoe, moduleDevA, 50));
		weekDef.add(new ContribDef(1, jdoe, moduleDevA, 100));
		weekDef.add(new ContribDef(2, jdoe, moduleDevB, 100));
		weekDef.add(new ContribDef(3, jdoe, moduleDevA, 25));
		weekDef.add(new ContribDef(3, jdoe, testA, 75));
		weekDef.add(new ContribDef(4, jdoe, vacations, 100));
		
		weekDef.add(new ContribDef(0, wsmith, specificationA, 25));
		weekDef.add(new ContribDef(0, wsmith, illness, 25));
		weekDef.add(new ContribDef(0, wsmith, specificationB, 50));
		weekDef.add(new ContribDef(1, wsmith, moduleDevC, 100));
		weekDef.add(new ContribDef(2, wsmith, specificationB, 25));
		weekDef.add(new ContribDef(2, wsmith, moduleDevC, 50));
		weekDef.add(new ContribDef(2, wsmith, testB, 25));
		weekDef.add(new ContribDef(3, wsmith, testB, 100));
		weekDef.add(new ContribDef(4, wsmith, testB, 100));
		
		// Repeat this week 10 times
		for (int week = 0; week<SAMPLE_DATA_WEEK_COUNT; week++) {
			for (ContribDef contribDef : weekDef) {
				Contribution ctb = getFactory().newContribution();
				Calendar c = (Calendar) sampleDataStart.clone();
				c.add(Calendar.DATE, week*7);
				c.add(Calendar.DATE, contribDef.dayIdx);
				ctb.setDate(c);
				ctb.setContributorId(contribDef.contributor.getId());
				ctb.setDurationId(contribDef.durationId);
				ctb.setTaskId(contribDef.task.getId());
				getModelMgr().createContribution(ctb, false);
			}
		}
		
		
	}
	
}
