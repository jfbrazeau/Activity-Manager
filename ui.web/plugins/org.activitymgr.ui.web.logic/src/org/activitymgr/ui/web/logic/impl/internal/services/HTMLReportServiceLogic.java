package org.activitymgr.ui.web.logic.impl.internal.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.activitymgr.core.dto.Collaborator;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class HTMLReportServiceLogic extends AbstractReportServiceLogic {

	@Override
	public String getPath() {
		return "/report/html";
	}

	@Override
	protected void doService(Collaborator connected, Request request,
			Response response, Workbook report) throws IOException {
		response.setContentType("text/html");
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("<html>");
		pw.println("<head>");
		pw.println("<title>Export global contributions</title>");
		pw.println("<meta charset='UTF-8'>");
		pw.println("</head>");
		pw.println("<body>");
		pw.println("<table border='1' cellspacing='0' cellpadding='0'>");
		Sheet sheet = report.getSheetAt(0);
		for (int r = 0; r <= sheet.getLastRowNum(); r++) {
			Row row = sheet.getRow(r);
			pw.println("<tr>");
			for (int c = 0; c < row.getLastCellNum(); c++) {
				Cell cell = row.getCell(c);
				if (cell == null) {
					pw.println("  <td></td>");
				} else {
					CellStyle style = cell.getCellStyle();
					pw.print("  <td");
					HSSFColor bgColor = (HSSFColor) style
							.getFillForegroundColorColor();
					if (bgColor != null
							&& !HSSFColor.AUTOMATIC.getInstance().equals(
									bgColor)) {
						pw.print(" bgcolor='#");
						pw.print(bgColor.getHexString());
						pw.print("'");
					}
					pw.print(" align='");
					switch (style.getAlignment()) {
					case CellStyle.ALIGN_CENTER:
						pw.print("center");
						break;
					case CellStyle.ALIGN_RIGHT:
						pw.print("right");
						break;
					default:
						pw.print("left");
					}
					pw.print("'");
					pw.print(">");
					int cellType = cell.getCellType();
					if (cellType == Cell.CELL_TYPE_FORMULA) {
						cellType = cell.getCachedFormulaResultType();
					}
					switch (cellType) {
					case Cell.CELL_TYPE_BLANK:
						break;
					case Cell.CELL_TYPE_BOOLEAN:
						pw.print(cell.getBooleanCellValue());
						break;
					case Cell.CELL_TYPE_ERROR:
						pw.print("#ERROR");
						break;
					case Cell.CELL_TYPE_NUMERIC:
						pw.print(String.valueOf(cell.getNumericCellValue()));
						break;
					case Cell.CELL_TYPE_STRING:
						String str = cell.getStringCellValue();
						if (str != null) {
							str = str.replaceAll("<", "&lt;");
							str = str.replaceAll(">", "&gt;");
						}
						pw.print(str);
					}
					pw.println("</td>");
				}
			}
			pw.println("</tr>");
		}
		pw.println("</table>");
		pw.println("</body>");
		pw.println("</html>");
		response.getOutputStream().write(sw.toString().getBytes("UTF-8"));
	}
}
