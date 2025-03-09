package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

public class GenerateJar {
	public static void main(String[] args) throws IOException {

		String basePath = System.getProperty("user.dir") + File.separator;

		// ขอให้ผู้ใช้กรอกไฟล์ CSV
//        System.out.print("Enter the path to the CSV file: ");
//        String csvFilePath = scanner.nextLine();
//
//        // ขอให้ผู้ใช้กรอกไฟล์ XML Template
//        System.out.print("Enter the path to the XML template file: ");
//        String xmlTemplatePath = scanner.nextLine();
//
//        // ขอให้ผู้ใช้กรอก output directory
//        System.out.print("Enter the output directory: ");
//        String outputDir = scanner.nextLine();

//        String csvFilePath = "D:\\develop\\wachirawitMerge\\input\\MakeData_Source _Boarding_Arrival.csv";
//        String csvFilePath = "D:\\develop\\wachirawitMerge\\input\\MakeData_Source _Boarding_Departure.csv";
//        String csvFilePath = "D:\\develop\\wachirawitMerge\\input\\MakeData_Source _Boarding_Transit.csv";
//        String csvFilePath = "D:\\develop\\wachirawitMerge\\input\\MakeData_Source _Boarding_Transfer.csv";
//        String csvFilePath = "C:\\Users\\User01\\Desktop\\wachirawitMerge\\input\\datafromPTang.csv";
//        String csvFilePath = basePath + "input\\data.csv";

		List<String> csvFilePath = getCsvFiles(basePath);
		System.out.println("---- ALL csvFilePath ---: " + csvFilePath);
//        String outputDir = "C:\\Users\\User01\\Desktop\\wachirawitMerge\\output";
//        String outputDir = basePath + "output";

		for (String eachCsvPath : csvFilePath) {
			try {

				List<Map<String, String>> csvData = readCsv(eachCsvPath);
				
				System.out.println("csvDatacsvData: " + csvData.get(0));

				boolean hasTransitColumn = csvData.get(0).containsKey("Transit port")
						|| csvData.get(0).containsKey("Transfer Port");
				System.out.println("has Transit or Transfer Column: " + hasTransitColumn);
				
				boolean hasTransferColumn = csvData.get(0).containsKey("Transfer Port");
				
				int countValidData = 0;
				for (Map<String, String> data : csvData) {
					if(data.get("First name") != null) {
						countValidData++;
					}
				}
				
				String xmlTemplatePath = generateTemplate(csvData, hasTransitColumn, basePath);
				String outputDir = getOutputDestination(csvData, hasTransitColumn, basePath);

				System.out.println("CSV File Path: " + csvFilePath);
				System.out.println("XML Template Path: " + xmlTemplatePath);
				System.out.println("outputDir: " + outputDir);

				String validReference = "";

				String xmlTemplate = new String(Files.readAllBytes(Paths.get(xmlTemplatePath)));
				int index = 0;
				int fileIndex = 1;
				int validOutPutSuccess = 0;
//                List<String> generatedFiles = new ArrayList<>();
				Map<String, String> arrivalDateTime = generateCurrentDateTimePlusFiveHours();
				for (Map<String, String> rowData : csvData) {
					String formattedIndex = String.format("%03d", fileIndex);
					rowData.put("headerDateTime", generateCurrentDateTime());
					rowData.put("TransactionId", generateTransactionId(fileIndex));
					rowData.put("departureDate", generateCurrentDate());
					rowData.put("departureTime", generateCurrentTime());
					rowData.put("arrivalDate", arrivalDateTime.get("date"));
					rowData.put("arrivalTime", arrivalDateTime.get("time"));
					rowData.put("expectedId0", generateTransactionId(fileIndex));
					rowData.put("sequenceNumber", formattedIndex);
					rowData.put("Date of birth", convertCSVDateTotemplateDate(csvData.get(index).get("Date of birth")));
					rowData.put("Expiry date", convertCSVDateTotemplateDate(csvData.get(index).get("Expiry date")));
					
					String isHaveCarrier = rowData.get("Carrier");
					
					if(isHaveCarrier == null) {
						System.out.println("----- Carrier input is null ----- ");
						String carrier = extractPrefix(rowData.get("Flight no."));
						rowData.put("Carrier", carrier);
					}

					String populatedXml = populateTemplate(xmlTemplate, rowData);

					validReference = rowData.get("Reference");

					if (validReference != null && !validReference.equals("")) {
						String outputFile = validReference + ".xml";
						if(!hasTransferColumn) {
							writeToFile(outputDir, outputFile , populatedXml);
							validOutPutSuccess++;
						} else {
							splitAndWriteFiles(populatedXml, outputDir, validReference);
							validOutPutSuccess++;
						}
					} else {
						System.out.println("----- Reference is null -----");
					}

					index++;
					fileIndex++;
					
//                    if (validReference == null) {
//                        return;
//                    } else if (!validReference.equals("")) {
//                        String outputFile = outputDir + File.separator + validReference + ".xml";
//                        Files.write(Paths.get(outputFile), populatedXml.getBytes());
//                        generatedFiles.add(outputFile);
//                    }
				}
				System.out.println("---------------------------------");
				System.out.println("---- outputDir ----: " + outputDir);
				System.out.println("---- Size of This CSV ----: " + csvData.size());
				System.out.println("---- count Valid Input Data ----: " + countValidData);
				System.out.println("---- count Generate OutPut Success ----: " + validOutPutSuccess);
				System.out.println("---------------------------------");
				if (validOutPutSuccess == 0) {
				    showMessage("Failed to generate any output files.\nPlease check the input data or output directory." + outputDir, "Fail", JOptionPane.ERROR_MESSAGE);
				} else if (validOutPutSuccess == countValidData) {
				    showMessage("All files were successfully generated and saved\nat: " + outputDir, "Success", JOptionPane.INFORMATION_MESSAGE);
				} else {
				    showMessage("Some files could not be generated.\nPartial output has been saved at: " + outputDir, "Warning", JOptionPane.WARNING_MESSAGE);
				}
				

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// เรียกใช้งานโค้ดหลัก

	}
	
    public static void splitAndWriteFiles(String populatedXml, String outputDir, String validReference) {
        String[] parts = populatedXml.split("-----", 2); // แยกข้อความเป็น 2 ส่วน
        if (parts.length < 2) {
            System.out.println("ไม่มีเครื่องหมาย --- หรือมีเพียงส่วนเดียว");
            return;
        }

        writeToFile(outputDir, validReference + "_1.xml", parts[0]); // ไฟล์ที่ 1
        writeToFile(outputDir, validReference + "_2.xml", parts[1]); // ไฟล์ที่ 2
    }
    
    private static void writeToFile(String outputDir, String fileName, String content) {
        String outputFile = outputDir + File.separator + fileName;
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(content);
        } catch (IOException e) {
            System.out.println("เกิดข้อผิดพลาด: " + e);
        }
    }
	
	public static String extractPrefix(String input) {
	    if (input == null || input.trim().isEmpty()) {
	        System.out.println("Invalid Service Number input: null or empty string");
	        return ""; // หรือจะใช้ return null; ถ้าต้องการให้ผลลัพธ์เป็น null
	    }
	    
	    return input.replaceAll("\\d", ""); // ลบตัวเลขออก
	}
	
	private static void showMessage(String message, String title, int messageType) {
	    JOptionPane.showMessageDialog(null, message, title, messageType);
	    System.out.println(message);
	}

	public static List<String> getCsvFiles(String directoryPath) {
		List<String> csvFiles = new ArrayList<>();
		try {
			// Traverse all subdirectories in the specified directory
			try (Stream<Path> files = Files.walk(Paths.get(directoryPath))) {
				files.filter(path -> !Files.isDirectory(path)) // Exclude directories
						.filter(path -> path.toString().endsWith(".csv")) // Only .csv files
						.forEach(path -> csvFiles.add(path.toString())); // Add file path to the list
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return csvFiles;
	}

	public static String getOutputDestination(List<Map<String, String>> csvData, boolean hasTransitColumn,
			String basePath) {
		Map<String, String> outputPaths = new HashMap<>();

		String baseOutputPath = basePath + "output";

//      developing before build
//        if (hasTransitColumn) {
//            templatePaths = Map.of(
//                "Arrival", "D:\\develop\\wachirawitMerge\\template\\transit_template.xml",
//                "Departure", "D:\\develop\\wachirawitMerge\\template\\transfer_template.xml"
//            );
//        } else {
//            templatePaths = Map.of(
//                "Arrival", "D:\\develop\\wachirawitMerge\\template\\arrival_template.xml",
//                "Departure", "D:\\develop\\wachirawitMerge\\template\\departure_template.xml"
//            );
//        }

//      Below Java 8
		if (hasTransitColumn) {
			outputPaths.put("Arrival", baseOutputPath + File.separator + "transfer");
			outputPaths.put("Departure", baseOutputPath + File.separator + "transit");
		} else {
			outputPaths.put("Arrival", baseOutputPath + File.separator + "arrival");
			outputPaths.put("Departure", baseOutputPath + File.separator + "departure");
		}

//      More than java 8
//        if (hasTransitColumn) {
//            templatePaths = Map.of(
//                "Arrival", baseTemplatePath + "transfer_template.xml", // transfer
//                "Departure", baseTemplatePath + "transit_template.xml" // transit
//            );
//        } else {
//            templatePaths = Map.of(
//                "Arrival", baseTemplatePath+ "arrival_template.xml",
//                "Departure", baseTemplatePath + "departure_template.xml"
//            );
//        }

		for (Map<String, String> rowData : csvData) {
			String flightDirection = rowData.get("Flight direction");
			if (outputPaths.containsKey(flightDirection)) {
				return outputPaths.get(flightDirection);
			}
		}

		return null;
	}

	public static String generateTemplate(List<Map<String, String>> csvData, boolean hasTransitColumn,
			String basePath) {
		Map<String, String> templatePaths = new HashMap<>();

		String baseTemplatePath = basePath + "template";

//      developing before build
//        if (hasTransitColumn) {
//            templatePaths = Map.of(
//                "Arrival", "D:\\develop\\wachirawitMerge\\template\\transit_template.xml",
//                "Departure", "D:\\develop\\wachirawitMerge\\template\\transfer_template.xml"
//            );
//        } else {
//            templatePaths = Map.of(
//                "Arrival", "D:\\develop\\wachirawitMerge\\template\\arrival_template.xml",
//                "Departure", "D:\\develop\\wachirawitMerge\\template\\departure_template.xml"
//            );
//        }

//      Below Java 8
		if (hasTransitColumn) {
			templatePaths.put("Arrival", baseTemplatePath + File.separator + "transfer_template.xml");
			templatePaths.put("Departure", baseTemplatePath + File.separator + "transit_template.xml");
		} else {
			templatePaths.put("Arrival", baseTemplatePath + File.separator + "arrival_template.xml");
			templatePaths.put("Departure", baseTemplatePath + File.separator + "departure_template.xml");
		}

//      More than java 8
//        if (hasTransitColumn) {
//            templatePaths = Map.of(
//                "Arrival", baseTemplatePath + "transfer_template.xml", // transfer
//                "Departure", baseTemplatePath + "transit_template.xml" // transit
//            );
//        } else {
//            templatePaths = Map.of(
//                "Arrival", baseTemplatePath+ "arrival_template.xml",
//                "Departure", baseTemplatePath + "departure_template.xml"
//            );
//        }

		for (Map<String, String> rowData : csvData) {
			String flightDirection = rowData.get("Flight direction");
			if (templatePaths.containsKey(flightDirection)) {
				return templatePaths.get(flightDirection);
			}
		}

		return null;
	}

    public static String convertCSVDateTotemplateDate(String inputDate) {
        System.out.println("Input Date: " + inputDate);

        if (inputDate == null || inputDate.trim().isEmpty()) {
            System.out.println("Invalid input: null or empty string");
            return ""; 
        }

        // ✅ ตรวจสอบว่า input ตรงกับ yyyy-MM-dd format หรือไม่
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        isoFormat.setLenient(false); // ป้องกัน parsing ผิดพลาด

        try {
            Date parsedDate = isoFormat.parse(inputDate);
            System.out.println("Input is already in yyyy-MM-dd format: " + inputDate + ", parsedDate: " + parsedDate);
            return inputDate; // ✅ ถ้า input ถูกต้องอยู่แล้ว return ค่าเดิม
        } catch (ParseException e) {
            System.out.println("Input is not in yyyy-MM-dd format, attempting to convert...");
        }

        // ✅ รูปแบบวันที่ที่รองรับ
        SimpleDateFormat inputFormat1 = new SimpleDateFormat("dd-MM-yyyy", Locale.UK);
        SimpleDateFormat inputFormat2 = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);

        inputFormat1.setLenient(false);
        inputFormat2.setLenient(false);

        SimpleDateFormat[] inputFormats = {inputFormat1, inputFormat2};
        Date date = null;

        for (SimpleDateFormat format : inputFormats) {
            try {
                date = format.parse(inputDate);
                break;
            } catch (ParseException e) {
                System.out.println("Failed to parse with format: " + format.toPattern());
            }
        }

        if (date != null) {
            String formattedDate = outputFormat.format(date);
            System.out.println("Formatted Date: " + formattedDate);
            return formattedDate;
        } else {
            System.out.println("Invalid date format");
            return null;
        }
    }

	public static boolean isValidCSVDateFormat(String inputDate, SimpleDateFormat outputFormat) {
	    // Define the desired date format
	    outputFormat.setLenient(false); // ให้ตรวจสอบว่าเป็นวันที่ที่ถูกต้อง

	    try {
	        // Try to parse the input date
	        outputFormat.parse(inputDate); // ถ้าผ่านจะถือว่าเป็น format ที่ถูกต้อง
	        return true;
	    } catch (ParseException e) {
	        // ถ้าไม่สามารถ parse ได้ แสดงว่าไม่ใช่ format ที่ต้องการ
	        return false;
	    }
	}
	@SuppressWarnings("deprecation")
	public static String generateCurrentDate() {
		Locale ukLocale = new Locale("uk", "UK");
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", ukLocale);
		return dateFormatter.format(new Date());
	}

	@SuppressWarnings("deprecation")
	public static String generateCurrentTime() {
		Locale ukLocale = new Locale("uk", "UK");
		SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:00", ukLocale);
		return timeFormatter.format(new Date());
	}

	// ฟังก์ชันสร้าง headerDateTime
	public static String generateCurrentDateTime() {
		return generateCurrentDate() + "T" + generateCurrentTime();
	}

	@SuppressWarnings("deprecation")
	public static Map<String, String> generateCurrentDateTimePlusFiveHours() {
		Locale ukLocale = new Locale("uk", "UK");

		// ใช้ GregorianCalendar แทน Calendar.getInstance()
		Calendar calendar = new GregorianCalendar(ukLocale);

		calendar.add(Calendar.HOUR_OF_DAY, 5); // เพิ่ม 5 ชั่วโมง

		// สร้าง SimpleDateFormat สำหรับการแปลงเป็นวันที่และเวลา
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", ukLocale);
		SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:00", ukLocale);

		// แปลงวันที่และเวลา
		String date = dateFormatter.format(calendar.getTime());
		String time = timeFormatter.format(calendar.getTime());

		// สร้าง Map เพื่อเก็บ date และ time แยกกัน
		Map<String, String> arrivalDateTime = new HashMap<>();
		arrivalDateTime.put("date", date);
		arrivalDateTime.put("time", time);

		return arrivalDateTime; // คืนค่า Map ที่เก็บ date และ time
	}

	public static String generateTransactionId(int fileIndex) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH);
		String timestamp = formatter.format(new Date());
		String formattedIndex = String.format("%02d", fileIndex);
		return timestamp + formattedIndex;
	}

	public static String generateEngDateFromThaiDate(String inputDate) {

		SimpleDateFormat inputFormat = new SimpleDateFormat("dMMMyyyy", Locale.of("th"));

		// ตัวแปลงรูปแบบสำหรับวันที่ที่ต้องการ (18-Jan-1989)
		SimpleDateFormat outputFormat = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
		String formattedDate = null;
		try {
			// แปลงจากสตริงเป็น Date
			Date date = inputFormat.parse(inputDate);

			// แปลงจาก Date เป็นรูปแบบที่ต้องการ
			formattedDate = outputFormat.format(date);

		} catch (ParseException e) {
			System.err.println("Invalid date format: {}" + e.getMessage());
		}

		return formattedDate;
	}

	// ฟังก์ชันอ่าน CSV
	public static List readCsv(String csvFilePath) {

		List csvData = new ArrayList();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(csvFilePath));
			String headerLine = br.readLine();
			if (headerLine == null) {
				return csvData;
			}

			String[] headers = headerLine.split(",");
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				Map rowData = new HashMap();

				for (int i = 0; i < headers.length && i < values.length; i++) {
					rowData.put(headers[i].trim(), values[i].trim());
				}

				csvData.add(rowData);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return csvData;

//        List<Map<String, String>> csvData = new ArrayList<>();
//        BufferedReader br = null;
//
//        try {
//            br = new BufferedReader(new FileReader(csvFilePath));
//            String headerLine = br.readLine();
//            if (headerLine == null) {
//                return csvData;
//            }
//
//            String[] headers = headerLine.split(",");
//            String line;
//            while ((line = br.readLine()) != null) {
//                String[] values = line.split(",");
//                Map<String, String> rowData = new HashMap<>();
//
//                for (int i = 0; i < headers.length && i < values.length; i++) {
//                    rowData.put(headers[i].trim(), values[i].trim());
//                }
//
//                csvData.add(rowData);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (br != null) {
//                try {
//                    br.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        return csvData;
	}

	// ฟังก์ชันแทนค่าข้อมูลใน XML Template
	public static String populateTemplate(String template, Map<String, String> data) {
		String result = template;
		for (Map.Entry<String, String> entry : data.entrySet()) {
			String placeholder = "\\{\\{" + entry.getKey() + "\\}\\}";
			String replacement = entry.getValue() != null ? entry.getValue() : ""; // Handle null values
			try {
				result = result.replaceAll(placeholder, replacement);
			} catch (IllegalArgumentException e) {
				System.out.println("Invalid placeholder: " + placeholder);
				e.printStackTrace();
			}
		}
		return result;
	}

}