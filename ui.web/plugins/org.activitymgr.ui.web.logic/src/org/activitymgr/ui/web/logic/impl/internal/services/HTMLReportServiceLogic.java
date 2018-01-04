package org.activitymgr.ui.web.logic.impl.internal.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;

import org.activitymgr.core.dto.Collaborator;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class HTMLReportServiceLogic extends AbstractReportServiceLogic {

	private static enum DecimalSeparator {
		COMMA, DOT;
	}

	public static final String DECIMAL_SEPARATOR_PARAMETER = "decimalSeparator";

	@Override
	public String getPath() {
		return "/report/html";
	}

	@Override
	protected void doService(Collaborator connected, Request request,
			Response response, Workbook report) throws IOException {

		// Retrieve decimal separator
		String decimalSeparatorParam = request
				.getParameter(DECIMAL_SEPARATOR_PARAMETER);
		DecimalSeparator decimalSeparator = null;
		if (decimalSeparatorParam != null) {
			decimalSeparator = DecimalSeparator.valueOf(decimalSeparatorParam
					.trim().toUpperCase());
		}

		// Set response content type
		response.setContentType("text/html");

		/*
		 * Prevents EXCEL from caching the page see
		 * https://blogs.technet.microsoft
		 * .com/the_microsoft_excel_support_team_blog
		 * /2011/11/15/cannot-download-
		 */
		response.addHeader("Cache-Control", "max-age=0");
		response.addHeader("Pragma", "public");

		// Output the table
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("<html>");
		pw.println("<head>");
		pw.println("<title>Activity Manager Report</title>");
		/*
		 * Prevents EXCEL from caching the page see
		 * https://blogs.technet.microsoft
		 * .com/the_microsoft_excel_support_team_blog
		 * /2011/11/15/cannot-download-
		 * the-information-you-requested-executing-web-query-from-excel/
		 */
		pw.println("<http-equiv=\"PRAGMA\" content=\"NO-CACHE\">");
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
						String formatted = NumberFormat.getNumberInstance()
								.format(cell.getNumericCellValue());
						if (decimalSeparator != null) {
							switch (decimalSeparator) {
							case COMMA:
								formatted = formatted.replace('.', ',');
								break;
							case DOT:
								formatted = formatted.replace(',', '.');
								break;
							}
						}
						pw.print(formatted);
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
