package org.activitymgr.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class XlsTest extends AbstractModelTestCase {

	private static final double MYETC = 0.25;
	private static final double MYINITCONS = 0.5;
	private static final double MYBUDGET = 1.5;
	private static final String MYCOMMENT = "MYCOMMENT";
	private static final String MYTASK = "MYTASK";
	private static final String MYCODE = "MYCODE";
	/** Logger */
	//private static Logger log = Logger.getLogger(XlsTest.class);
	
	public void testEmptyWorkbook() throws IOException {
		Workbook wbk = new HSSFWorkbook();
		try {
			getModelMgr().importFromExcel(null, serialize(wbk));
			fail("Empty workbook import should fail");
		}
		catch (ModelException e) {
		}
	}

	public void testEmptySheet() throws IOException {
		Workbook wbk = new HSSFWorkbook();
		wbk.createSheet();
		try {
			getModelMgr().importFromExcel(null, serialize(wbk));
			fail("Empty sheet import should fail");
		}
		catch (ModelException e) {
		}
	}

	public void testSheetOnlyContainingHeader() throws IOException {
		Workbook wbk = new HSSFWorkbook();
		Sheet sheet = wbk.createSheet();
		Row headerRow = sheet.createRow(0);
		headerRow.createCell(0).setCellValue("code");
		headerRow.createCell(1).setCellValue("name");
		try {
			getModelMgr().importFromExcel(null, serialize(wbk));
			fail("Sheet withou content should fail");
		}
		catch (ModelException e) {
		}
	}

	public void testImportWithoutCode() throws IOException, ModelException {
		Workbook wbk = new HSSFWorkbook();
		Sheet sheet = wbk.createSheet();
		Row headerRow = sheet.createRow(0);
		headerRow.createCell(0).setCellValue("name");
		headerRow.createCell(1).setCellValue("comment");
		headerRow.createCell(2).setCellValue("budget");
		headerRow.createCell(3).setCellValue("initiallyConsumed");
		headerRow.createCell(4).setCellValue("todo");
		Row contentRow = sheet.createRow(1);
		contentRow.createCell(0).setCellValue(MYTASK);
		contentRow.createCell(1).setCellValue(MYCOMMENT);
		contentRow.createCell(2).setCellValue(MYBUDGET);
		contentRow.createCell(3).setCellValue(MYINITCONS);
		contentRow.createCell(4).setCellValue(MYETC);
		try {
			getModelMgr().importFromExcel(null, serialize(wbk));
			fail("A sheet without code column should fail");
		}
		catch (ModelException e) {
			
		}
		
	}

	public void testImportWithUnknownPath() throws IOException, ModelException {
		Workbook wbk = new HSSFWorkbook();
		Sheet sheet = wbk.createSheet();
		Row headerRow = sheet.createRow(0);
		headerRow.createCell(0).setCellValue("path");
		headerRow.createCell(1).setCellValue("code");
		headerRow.createCell(2).setCellValue("name");
		headerRow.createCell(3).setCellValue("comment");
		headerRow.createCell(4).setCellValue("budget");
		headerRow.createCell(5).setCellValue("initiallyConsumed");
		headerRow.createCell(6).setCellValue("todo");
		// First row : empty path
		Row contentRow = sheet.createRow(1);
		contentRow.createCell(0).setCellValue(MYCODE); // This task doesn't exist
		contentRow.createCell(1).setCellValue(MYCODE);
		contentRow.createCell(2).setCellValue(MYTASK);
		contentRow.createCell(3).setCellValue(MYCOMMENT);
		contentRow.createCell(4).setCellValue(0);
		contentRow.createCell(5).setCellValue(0);
		contentRow.createCell(6).setCellValue(0);
		try {
			getModelMgr().importFromExcel(null, serialize(wbk));
			fail("A sheet that contains a unknown path should fail");
		}
		catch (ModelException e) {
			
		}
		
	}

	public void testBasicImport() throws IOException, ModelException {
		Workbook wbk = new HSSFWorkbook();
		Sheet sheet = wbk.createSheet();
		Row headerRow = sheet.createRow(0);
		headerRow.createCell(0).setCellValue("code");
		headerRow.createCell(1).setCellValue("name");
		headerRow.createCell(2).setCellValue("comment");
		headerRow.createCell(3).setCellValue("budget");
		headerRow.createCell(4).setCellValue("initiallyConsumed");
		headerRow.createCell(5).setCellValue("todo");
		Row contentRow = sheet.createRow(1);
		contentRow.createCell(0).setCellValue(MYCODE);
		contentRow.createCell(1).setCellValue(MYTASK);
		contentRow.createCell(2).setCellValue(MYCOMMENT);
		contentRow.createCell(3).setCellValue(MYBUDGET);
		contentRow.createCell(4).setCellValue(MYINITCONS);
		contentRow.createCell(5).setCellValue(MYETC);
		getModelMgr().importFromExcel(null, serialize(wbk));
		
		Task[] subtasks = getModelMgr().getSubTasks(null);
		assertNotNull(subtasks);
		assertEquals(1, subtasks.length);
		Task task = subtasks[0];
		assertNotNull(task);
		assertEquals(MYCODE, task.getCode());
		assertEquals(MYTASK, task.getName());
		assertEquals(MYCOMMENT, task.getComment());
		assertEquals((long)(MYBUDGET*100), task.getBudget());
		assertEquals((long)(MYINITCONS*100), task.getInitiallyConsumed());
		assertEquals((long)(MYETC*100), task.getTodo());
	}

	public void testImportWithPath() throws IOException, ModelException {
		Task rootTask = getFactory().newTask();
		rootTask.setCode("ROOT");
		rootTask.setName("The root");
		rootTask = getModelMgr().createTask(null, rootTask);
		
		Workbook wbk = new HSSFWorkbook();
		Sheet sheet = wbk.createSheet();
		Row headerRow = sheet.createRow(0);
		headerRow.createCell(0).setCellValue("path");
		headerRow.createCell(1).setCellValue("code");
		headerRow.createCell(2).setCellValue("name");
		headerRow.createCell(3).setCellValue("comment");
		headerRow.createCell(4).setCellValue("budget");
		headerRow.createCell(5).setCellValue("initiallyConsumed");
		headerRow.createCell(6).setCellValue("todo");
		// First row : empty path
		Row contentRow = sheet.createRow(1);
		contentRow.createCell(1).setCellValue(MYCODE);
		contentRow.createCell(2).setCellValue(MYTASK);
		contentRow.createCell(3).setCellValue(MYCOMMENT);
		// Second row : path using previous task
		contentRow = sheet.createRow(2);
		contentRow.createCell(0).setCellValue(MYCODE);
		contentRow.createCell(1).setCellValue(MYCODE);
		contentRow.createCell(2).setCellValue(MYTASK);
		contentRow.createCell(3).setCellValue(MYCOMMENT);
		contentRow.createCell(4).setCellValue(0);
		contentRow.createCell(5).setCellValue(0);
		contentRow.createCell(6).setCellValue(0);
		// Third row : path using previous task
		contentRow = sheet.createRow(3);
		contentRow.createCell(0).setCellValue(MYCODE + '/' + MYCODE);
		contentRow.createCell(1).setCellValue(MYCODE);
		contentRow.createCell(2).setCellValue(MYTASK);
		contentRow.createCell(3).setCellValue(MYCOMMENT);
		contentRow.createCell(4).setCellValue(MYBUDGET);
		contentRow.createCell(5).setCellValue(MYINITCONS);
		contentRow.createCell(6).setCellValue(MYETC);
		getModelMgr().importFromExcel(rootTask.getId(), serialize(wbk));
		getModelMgr().exportToXML(System.out);
		
		Task[] subtasks = getModelMgr().getSubTasks(rootTask.getId());
		assertNotNull(subtasks);
		assertEquals(1, subtasks.length);
		subtasks = getModelMgr().getSubTasks(subtasks[0].getId());
		assertNotNull(subtasks);
		assertEquals(1, subtasks.length);
	}
	private InputStream serialize(Workbook wbk) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		wbk.write(out);
		InputStream in = new ByteArrayInputStream(out.toByteArray());
		return in;
	}

	
	
}
