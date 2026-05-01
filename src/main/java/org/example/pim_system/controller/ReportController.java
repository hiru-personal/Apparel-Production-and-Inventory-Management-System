package org.example.pim_system.controller;

import org.example.pim_system.model.BrokenNeedle;
import org.example.pim_system.model.Employee;
import org.example.pim_system.model.InventoryItem;
import org.example.pim_system.model.MissingNeedle;
import org.example.pim_system.model.Product;
import org.example.pim_system.model.ProductionPlanningPlan;
import org.example.pim_system.model.SupportTicket;
import org.example.pim_system.repository.BrokenNeedleRepository;
import org.example.pim_system.repository.EmployeeRepository;
import org.example.pim_system.repository.InventoryItemRepository;
import org.example.pim_system.repository.MissingNeedleRepository;
import org.example.pim_system.repository.ProductRepository;
import org.example.pim_system.repository.ProductionPlanningPlanRepository;
import org.example.pim_system.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductionPlanningPlanRepository productionPlanningPlanRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private BrokenNeedleRepository brokenNeedleRepository;
    @Autowired
    private MissingNeedleRepository missingNeedleRepository;
    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> queryReport(
            @RequestParam("source") String source,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "category", required = false) String category
    ) {
        try {
            LocalDate fromDate = parseDateOrNull(from);
            LocalDate toDate = parseDateOrNull(to);
            ReportData reportData = buildReportData(source, fromDate, toDate, category);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("source", source);
            body.put("columns", reportData.columns);
            body.put("rows", reportData.rows);
            body.put("summary", reportData.summary);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", "Error generating report: " + e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    @GetMapping({"/export.xls", "/export.xlsx"})
    public ResponseEntity<byte[]> exportReportExcel(
            @RequestParam("source") String source,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "category", required = false) String category
    ) {
        try {
            LocalDate fromDate = parseDateOrNull(from);
            LocalDate toDate = parseDateOrNull(to);
            ReportData reportData = buildReportData(source, fromDate, toDate, category);

            byte[] bytes = buildWorkbookBytes(reportData, source, fromDate, toDate, category);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.ms-excel"));
            String fileName = "report-" + sanitizeFileName(source) + "-" + LocalDate.now() + ".xls";
            headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Failed to export report: " + e.getMessage()).getBytes());
        }
    }

    private ReportData buildReportData(String source, LocalDate fromDate, LocalDate toDate, String category) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Source is required.");
        }

        String normalized = source.trim().toLowerCase();
        ReportData data = new ReportData();

        switch (normalized) {
            case "production":
                buildProductionData(data, fromDate, toDate, category);
                break;
            case "production-planning":
                buildProductionPlanningData(data, fromDate, toDate, category);
                break;
            case "inventory":
                buildInventoryData(data, fromDate, toDate, category);
                break;
            case "payroll":
                buildPayrollData(data, fromDate, toDate, category);
                break;
            case "needle-usage":
                buildNeedleUsageData(data, fromDate, toDate, category);
                break;
            case "submitted-tickets":
                buildSubmittedTicketsData(data, fromDate, toDate, category);
                break;
            default:
                throw new IllegalArgumentException("Unknown report source: " + source);
        }
        return data;
    }

    private void buildProductionData(ReportData data, LocalDate fromDate, LocalDate toDate, String category) {
        data.columns = List.of(
                "Product Code", "Product Name", "Current Qty", "Target", "Completed Qty", "Remaining", "Status",
                "Start Date", "End Date", "Created At"
        );
        List<Product> products = productRepository.findAllByOrderByProductCodeAsc();
        int totalCurrent = 0;
        int totalTarget = 0;

        for (Product p : products) {
            if (!inDateRange(p.getCreatedAt(), fromDate, toDate)) continue;
            if (!containsAny(p.getProductCode(), p.getProductName(), p.getStatus(), category)) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Product Code", nv(p.getProductCode()));
            row.put("Product Name", nv(p.getProductName()));
            row.put("Current Qty", nzInt(p.getCurrentQuantity()));
            row.put("Target", nzInt(p.getTarget()));
            row.put("Completed Qty", nzInt(p.getCompletedQuantity()));
            row.put("Remaining", nzInt(p.getRemainingQuantity()));
            row.put("Status", nv(p.getStatus()));
            row.put("Start Date", p.getStartingDate() != null ? p.getStartingDate().toString() : "");
            row.put("End Date", p.getEndingDate() != null ? p.getEndingDate().toString() : "");
            row.put("Created At", fmtDateTime(p.getCreatedAt()));
            data.rows.add(row);

            totalCurrent += nzInt(p.getCurrentQuantity());
            totalTarget += nzInt(p.getTarget());
        }

        data.summary.put("Rows", data.rows.size());
        data.summary.put("Total Current Qty", totalCurrent);
        data.summary.put("Total Target", totalTarget);
    }

    private void buildProductionPlanningData(ReportData data, LocalDate fromDate, LocalDate toDate, String category) {
        data.columns = List.of(
                "Plan ID", "Product Code", "Product Name", "Quantity", "End Date", "Assigned Machine", "Status", "Created At"
        );
        List<ProductionPlanningPlan> plans = productionPlanningPlanRepository.findAllByOrderByCreatedAtDesc();
        int totalQty = 0;
        int completeCount = 0;

        for (ProductionPlanningPlan p : plans) {
            if (!inDateRange(p.getCreatedAt(), fromDate, toDate)) continue;
            if (!containsAny(p.getProductCode(), p.getProductName(), p.getAssignedMachine(), p.getStatus(), category)) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Plan ID", p.getId());
            row.put("Product Code", nv(p.getProductCode()));
            row.put("Product Name", nv(p.getProductName()));
            row.put("Quantity", nzInt(p.getQuantity()));
            row.put("End Date", p.getEndDate() != null ? p.getEndDate().toString() : "");
            row.put("Assigned Machine", nv(p.getAssignedMachine()));
            row.put("Status", nv(p.getStatus()));
            row.put("Created At", fmtDateTime(p.getCreatedAt()));
            data.rows.add(row);

            totalQty += nzInt(p.getQuantity());
            if ("complete".equalsIgnoreCase(p.getStatus())) completeCount++;
        }

        data.summary.put("Rows", data.rows.size());
        data.summary.put("Total Planned Qty", totalQty);
        data.summary.put("Completed Plans", completeCount);
    }

    private void buildInventoryData(ReportData data, LocalDate fromDate, LocalDate toDate, String category) {
        data.columns = List.of("Item Code", "Item Name", "Category", "Current Stock", "Min Stock", "Unit", "Status", "Created At");
        List<InventoryItem> items = inventoryItemRepository.findAllByOrderByItemNameAsc();
        int lowStock = 0;

        for (InventoryItem i : items) {
            if (!inDateRange(i.getCreatedAt(), fromDate, toDate)) continue;
            if (!containsAny(i.getCategory(), i.getItemCode(), i.getItemName(), category)) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Item Code", nv(i.getItemCode()));
            row.put("Item Name", nv(i.getItemName()));
            row.put("Category", nv(i.getCategory()));
            row.put("Current Stock", nzInt(i.getCurrentStock()));
            row.put("Min Stock", nzInt(i.getMinimumStockLevel()));
            row.put("Unit", nv(i.getUnit()));
            row.put("Status", i.getStatus());
            row.put("Created At", fmtDateTime(i.getCreatedAt()));
            data.rows.add(row);

            if (i.isLowStock()) lowStock++;
        }

        data.summary.put("Rows", data.rows.size());
        data.summary.put("Low Stock Items", lowStock);
    }

    private void buildPayrollData(ReportData data, LocalDate fromDate, LocalDate toDate, String category) {
        data.columns = List.of("Employee ID", "Full Name", "Position", "Basic Salary", "Start Date", "Created At");
        List<Employee> employees = employeeRepository.findAllByOrderByFullNameAsc();
        double totalBasic = 0.0;

        for (Employee e : employees) {
            LocalDate dateToCheck = e.getStartDate();
            if (!inDateRange(dateToCheck, fromDate, toDate)) continue;
            if (!containsAny(e.getEmployeeId(), e.getFullName(), e.getPosition(), category)) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Employee ID", nv(e.getEmployeeId()));
            row.put("Full Name", nv(e.getFullName()));
            row.put("Position", nv(e.getPosition()));
            row.put("Basic Salary", nzDouble(e.getBasicSalary()));
            row.put("Start Date", e.getStartDate() != null ? e.getStartDate().toString() : "");
            row.put("Created At", fmtDateTime(e.getCreatedAt()));
            data.rows.add(row);

            totalBasic += nzDouble(e.getBasicSalary());
        }

        data.summary.put("Rows", data.rows.size());
        data.summary.put("Total Basic Salary", totalBasic);
    }

    private void buildNeedleUsageData(ReportData data, LocalDate fromDate, LocalDate toDate, String category) {
        data.columns = List.of("Type", "Date/Time", "Employee", "Machine / Needle ID", "Needle Type / Status", "Notes");
        int brokenCount = 0;
        int missingCount = 0;

        List<BrokenNeedle> broken = brokenNeedleRepository.findAllByOrderByReportedAtDesc();
        for (BrokenNeedle b : broken) {
            if (!inDateRange(b.getReportedAt(), fromDate, toDate)) continue;
            if (!containsAny(b.getEmployee(), b.getMachineNumber(), b.getNeedleType(), category)) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Type", "Broken");
            row.put("Date/Time", fmtDateTime(b.getReportedAt()));
            row.put("Employee", nv(b.getEmployee()));
            row.put("Machine / Needle ID", nv(b.getMachineNumber()));
            row.put("Needle Type / Status", nv(b.getNeedleType()));
            row.put("Notes", nv(b.getBreakageReason()));
            data.rows.add(row);
            brokenCount++;
        }

        List<MissingNeedle> missing = missingNeedleRepository.findAllByOrderByReportedAtDesc();
        for (MissingNeedle m : missing) {
            if (!inDateRange(m.getReportedAt(), fromDate, toDate)) continue;
            String machineOrNeedleId = (m.getMachineNumber() != null && !m.getMachineNumber().isBlank())
                    ? m.getMachineNumber()
                    : m.getNeedleId();
            if (!containsAny(m.getLastSeenBy(), machineOrNeedleId, m.getNeedleType(), m.getStatus(), category)) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Type", "Missing");
            row.put("Date/Time", fmtDateTime(m.getReportedAt()));
            row.put("Employee", nv(m.getLastSeenBy()));
            // Missing needle records now primarily track machine number.
            row.put("Machine / Needle ID", nv(machineOrNeedleId));
            String needleTypeStatus = (m.getNeedleType() != null && !m.getNeedleType().isBlank())
                    ? m.getNeedleType() + " / " + m.getStatus()
                    : m.getStatus();
            row.put("Needle Type / Status", nv(needleTypeStatus));
            row.put("Notes", nv(m.getSearchActionsTaken()));
            data.rows.add(row);
            missingCount++;
        }

        data.summary.put("Rows", data.rows.size());
        data.summary.put("Broken Reports", brokenCount);
        data.summary.put("Missing Reports", missingCount);
    }

    private void buildSubmittedTicketsData(ReportData data, LocalDate fromDate, LocalDate toDate, String category) {
        data.columns = List.of("Ticket ID", "Title", "Department", "Status", "Created By", "Created At", "Reply");
        List<SupportTicket> tickets = supportTicketRepository.findAllByOrderByCreatedAtDesc();
        int openCount = 0;
        int resolvedCount = 0;

        for (SupportTicket t : tickets) {
            if (!inDateRange(t.getCreatedAt(), fromDate, toDate)) continue;
            if (!containsAny(t.getDepartment(), t.getTitle(), t.getCreatedBy(), t.getStatus(), category)) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Ticket ID", t.getId());
            row.put("Title", nv(t.getTitle()));
            row.put("Department", nv(t.getDepartment()));
            row.put("Status", nv(t.getStatus()));
            row.put("Created By", nv(t.getCreatedBy()));
            row.put("Created At", fmtDateTime(t.getCreatedAt()));
            row.put("Reply", nv(t.getAdminReply()));
            data.rows.add(row);

            if ("resolved".equalsIgnoreCase(t.getStatus())) {
                resolvedCount++;
            } else {
                openCount++;
            }
        }

        data.summary.put("Rows", data.rows.size());
        data.summary.put("Open Tickets", openCount);
        data.summary.put("Resolved Tickets", resolvedCount);
    }

    private byte[] buildWorkbookBytes(ReportData data, String source, LocalDate fromDate, LocalDate toDate, String category) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>")
                .append("<?mso-application progid=\"Excel.Sheet\"?>")
                .append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ")
                .append("xmlns:o=\"urn:schemas-microsoft-com:office:office\" ")
                .append("xmlns:x=\"urn:schemas-microsoft-com:office:excel\" ")
                .append("xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\" ")
                .append("xmlns:html=\"http://www.w3.org/TR/REC-html40\">");

        sb.append("<Worksheet ss:Name=\"Report\"><Table>");
        appendTextRowXml(sb, "Source", source);
        appendTextRowXml(sb, "From", fromDate != null ? fromDate.toString() : "-");
        appendTextRowXml(sb, "To", toDate != null ? toDate.toString() : "-");
        appendTextRowXml(sb, "Category", category != null && !category.isBlank() ? category : "-");
        sb.append("<Row></Row>");

        sb.append("<Row>");
        for (String col : data.columns) {
            sb.append("<Cell><Data ss:Type=\"String\">").append(xmlEscape(col)).append("</Data></Cell>");
        }
        sb.append("</Row>");

        for (Map<String, Object> rowData : data.rows) {
            sb.append("<Row>");
            for (String key : data.columns) {
                Object val = rowData.get(key);
                if (val instanceof Number) {
                    sb.append("<Cell><Data ss:Type=\"Number\">").append(val).append("</Data></Cell>");
                } else {
                    sb.append("<Cell><Data ss:Type=\"String\">")
                            .append(xmlEscape(val != null ? String.valueOf(val) : ""))
                            .append("</Data></Cell>");
                }
            }
            sb.append("</Row>");
        }
        sb.append("</Table></Worksheet>");

        sb.append("<Worksheet ss:Name=\"Summary\"><Table>");
        for (Map.Entry<String, Object> entry : data.summary.entrySet()) {
            appendTextRowXml(sb, entry.getKey(), entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
        }
        sb.append("</Table></Worksheet>");
        sb.append("</Workbook>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value.trim());
    }

    private boolean inDateRange(LocalDateTime dateTime, LocalDate from, LocalDate to) {
        if (dateTime == null) return true;
        return inDateRange(dateTime.toLocalDate(), from, to);
    }

    private boolean inDateRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return true;
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }

    private boolean containsAny(String a, String b, String c, String category) {
        return containsAny(new String[]{a, b, c}, category);
    }

    private boolean containsAny(String a, String b, String c, String d, String category) {
        return containsAny(new String[]{a, b, c, d}, category);
    }

    private boolean containsAny(String[] values, String category) {
        if (category == null || category.isBlank()) return true;
        String q = category.trim().toLowerCase();
        for (String value : values) {
            if (value != null && value.toLowerCase().contains(q)) return true;
        }
        return false;
    }

    private String fmtDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private int nzInt(Integer n) {
        return n != null ? n : 0;
    }

    private double nzDouble(Double n) {
        return n != null ? n : 0.0;
    }

    private String nv(String s) {
        return s != null ? s : "";
    }

    private String sanitizeFileName(String s) {
        if (s == null || s.isBlank()) return "report";
        return s.replaceAll("[^a-zA-Z0-9-_]+", "-");
    }

    private void appendTextRowXml(StringBuilder sb, String label, String value) {
        sb.append("<Row>")
                .append("<Cell><Data ss:Type=\"String\">").append(xmlEscape(label)).append("</Data></Cell>")
                .append("<Cell><Data ss:Type=\"String\">").append(xmlEscape(value)).append("</Data></Cell>")
                .append("</Row>");
    }

    private String xmlEscape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static class ReportData {
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();
    }
}

