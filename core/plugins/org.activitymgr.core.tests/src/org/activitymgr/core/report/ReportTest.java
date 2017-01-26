package org.activitymgr.core.report;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.activitymgr.core.AbstractModelTestCase;
import org.activitymgr.core.dto.report.Report;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.dto.report.ReportItem;
import org.activitymgr.core.model.ModelException;
import org.xml.sax.SAXException;

public class ReportTest extends AbstractModelTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		loadSampleModel();
	}

	private static final String START_PROP = "start";
	private static final String INTERVAL_TYPE_PROP = "intervalType";
	private static final String INTERVAL_COUNT_PROP = "intervalCount";
	private static final String ROOT_TASK_CODE_PATH_PROP = "rootTaskCodePath";
	private static final String TASK_DEPTH_PROP = "taskDepth";
	private static final String BY_CONTRIBUTOR_PROP = "byContributor";
	private static final String ORDER_BY_CONTRIBUTOR_PROP = "orderByContributor";
	
	public void testReportOneDayNoRootTask() throws IOException, ModelException {
		doTestReport();
	}

	public void testReportOneDayWithRootTask() throws IOException, ModelException {
		doTestReport();
	}

	private void doTestReport() throws IOException, ModelException {
		String testName = getName();
		String fileName = testName.substring(4) + ".txt";
		System.out.println(fileName);
		BufferedReader in = new BufferedReader(new InputStreamReader(ReportTest.class.getResourceAsStream(fileName)));
		String line = "";

		Properties props = new Properties();

		while (line != null) {
			// Load properties
			StringWriter propsSw = new StringWriter();
			while (!(line = in.readLine()).startsWith("+-")) {
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
			
			Report report = doBuildReport(
					cal(props.getProperty(START_PROP)), // Start date
					ReportIntervalType.valueOf(props.getProperty(INTERVAL_TYPE_PROP).toUpperCase()), // interval type
					Integer.parseInt(props.getProperty(INTERVAL_COUNT_PROP)), // interval count
					rootTaskId, // root task
					Integer.parseInt(props.getProperty(TASK_DEPTH_PROP)), // Task depth
					Boolean.parseBoolean(props.getProperty(BY_CONTRIBUTOR_PROP)), // By contributor
					Boolean.parseBoolean(props.getProperty(ORDER_BY_CONTRIBUTOR_PROP)) // Order by contributor
					);
			System.out.println(report.toString());
			assertNotNull(report);
			List<ReportItem> items = report.getItems();
			assertNotNull(items);
			assertEquals(expected, report.toString());
		}
	}
	
	private Report doBuildReport(Calendar start, ReportIntervalType intervalType, int intervalCount, Long rootTaskId, int taskDepth, boolean byContributor, boolean orderByContributor) {
		System.out.println("buildReport(");
		System.out.println("  " + start + ", // start");
		System.out.println("  " + intervalType + ", // intervalType");
		System.out.println("  " + intervalCount + ", // intervalCount");
		System.out.println("  " + rootTaskId + ", // rootTaskId");
		System.out.println("  " + taskDepth + ", // taskDepth");
		System.out.println("  " + byContributor + ", // byContributor");
		System.out.println("  " + orderByContributor + ") // orderByContributor");
		return getModelMgr().buildReport(start, intervalType, intervalCount, rootTaskId, taskDepth, byContributor, orderByContributor);
	}

	private Calendar cal(String cal) {
		if (cal.length() != 8) {
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
	}
	
}
