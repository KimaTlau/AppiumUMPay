package com.umpay.mobile.utility;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Reads a test data workbook out of TestData/.
 *
 * The same reader the UMPay web suite uses, kept deliberately identical so a workbook
 * can be opened by either project and a reader who knows one knows the other. Each
 * module has its own .xlsx, the feature file names the workbook, the sheet and the row,
 * and the step definition knows which columns of that sheet it wants.
 *
 * Row 0 is the header, so row 1 is the first row of data.
 */
public class ExcelDataProvider {

	XSSFWorkbook wb;

	/**
	 * The sheet is named again on every read, so it is accepted and ignored here. The
	 * constructor takes it only so a caller can write the workbook and sheet together,
	 * the way the feature file names them.
	 */
	public ExcelDataProvider(String fileName, String sheetName) {
		this(fileName);
	}

	public ExcelDataProvider(String fileName) {

		if (!fileName.endsWith(".xlsx")) {
			fileName = fileName + ".xlsx";
		}
		File src = new File("./TestData/" + fileName);

		try {
			FileInputStream fis = new FileInputStream(src);

			wb = new XSSFWorkbook(fis);
		} catch (IOException e) {
			System.out.println("Error in reading excel file" + e.getMessage());
		}
	}

	public String getStringData(String sheetName, int row, int col) {
		org.apache.poi.ss.usermodel.Cell cell = wb.getSheet(sheetName).getRow(row).getCell(col);
		if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {

			double value = cell.getNumericCellValue();

			/*
			 * Account and phone numbers run past the range of an int, and casting
			 * one clamps it to 2147483647 without any error. Whole numbers go
			 * through long, and anything with a fraction is printed in plain
			 * notation so an amount never arrives as 6.5E10.
			 */
			if (value == Math.rint(value) && !Double.isInfinite(value)) {
				return String.valueOf((long) value);
			}
			return new BigDecimal(Double.toString(value)).stripTrailingZeros().toPlainString();
		}
		return cell.getStringCellValue();
	}

	public double getNumericData(String sheetName, int row, int col) {
		return wb.getSheet(sheetName).getRow(row).getCell(col).getNumericCellValue();
	}
}
