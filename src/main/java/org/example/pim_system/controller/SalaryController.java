package org.example.pim_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.Employee;
import org.example.pim_system.model.MonthlySalary;
import org.example.pim_system.repository.EmployeeRepository;
import org.example.pim_system.repository.MonthlySalaryRepository;
import org.example.pim_system.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/salary")
public class SalaryController {

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private MonthlySalaryRepository monthlySalaryRepository;
    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateSalary(@RequestBody Map<String, Object> payload,
                                                                 HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String employeeId = (String) payload.get("employeeId");
            Double overtimeHours = payload.get("overtimeHours") != null
                    ? Double.valueOf(payload.get("overtimeHours").toString()) : 0.0;
            Double allowances = payload.get("allowances") != null
                    ? Double.valueOf(payload.get("allowances").toString()) : 0.0;
            Double advances = payload.get("advances") != null
                    ? Double.valueOf(payload.get("advances").toString()) : 0.0;
            boolean applyEpf = payload.get("applyEpf") != null
                    && Boolean.parseBoolean(payload.get("applyEpf").toString());
            String month = payload.get("month") != null ? payload.get("month").toString() : null;

            if (employeeId == null || employeeId.isEmpty()) {
                response.put("success", false);
                response.put("message", "Employee ID is required");
                return ResponseEntity.badRequest().body(response);
            }
            if (month == null || !month.matches("^\\d{4}-\\d{2}$")) {
                response.put("success", false);
                response.put("message", "Valid month is required (format: YYYY-MM)");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<Employee> optionalEmployee = employeeRepository.findByEmployeeId(employeeId);
            if (optionalEmployee.isEmpty()) {
                response.put("success", false);
                response.put("message", "Employee not found");
                return ResponseEntity.badRequest().body(response);
            }

            Employee employee = optionalEmployee.get();
            double basicSalary = employee.getBasicSalary() != null ? employee.getBasicSalary() : 0.0;

            // Simple OT calculation: hourly = basic / (8 hours * 26 working days), OT at 1.5x
            double hourlyRate = basicSalary / (8 * 26.0);
            double overtimeAmount = overtimeHours * hourlyRate * 1.5;

            double grossSalary = basicSalary + overtimeAmount + allowances;
            boolean isPermanentEmployee = employee.getPosition() != null
                    && employee.getPosition().toLowerCase().contains("permanent");
            double epfDeduction = (applyEpf && isPermanentEmployee) ? grossSalary * 0.08 : 0.0;
            double netSalary = grossSalary - advances - epfDeduction;

            MonthlySalary monthlySalary = monthlySalaryRepository
                    .findByEmployeeAndSalaryMonth(employee, month)
                    .orElseGet(MonthlySalary::new);
            monthlySalary.setEmployee(employee);
            monthlySalary.setSalaryMonth(month);
            monthlySalary.setBasicSalary(basicSalary);
            monthlySalary.setOvertimeHours(overtimeHours);
            monthlySalary.setOvertimeAmount(overtimeAmount);
            monthlySalary.setAllowances(allowances);
            monthlySalary.setAdvances(advances);
            monthlySalary.setGrossSalary(grossSalary);
            monthlySalary.setNetSalary(netSalary);
            monthlySalaryRepository.save(monthlySalary);

            auditLogService.log(
                    "SALARY_CALCULATED",
                    String.format("Salary processed for %s (%s) - Month: %s. Basic=LKR %.2f, OT Hours=%.1f, OT Amount=LKR %.2f, Allowances=LKR %.2f, Advances=LKR %.2f, EPF=LKR %.2f, Net Salary=LKR %.2f",
                            employee.getFullName(), employeeId, month,
                            basicSalary, overtimeHours, overtimeAmount, allowances, advances, epfDeduction, netSalary),
                    request
            );

            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("basicSalary", basicSalary);
            breakdown.put("overtimeHours", overtimeHours);
            breakdown.put("overtimeAmount", overtimeAmount);
            breakdown.put("allowances", allowances);
            breakdown.put("advances", advances);
            breakdown.put("epfDeduction", epfDeduction);
            breakdown.put("epfApplied", applyEpf && isPermanentEmployee);
            breakdown.put("grossSalary", grossSalary);
            breakdown.put("netSalary", netSalary);

            response.put("success", true);
            response.put("employeeName", employee.getFullName());
            response.put("employeeId", employee.getEmployeeId());
            response.put("month", month);
            response.put("breakdown", breakdown);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error calculating salary: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/payslips")
    public ResponseEntity<Map<String, Object>> generatePayslips(@RequestParam(required = false) String month) {
        Map<String, Object> response = new HashMap<>();

        // Placeholder implementation: in a real system, you would generate PDFs or export files here.
        response.put("success", true);
        response.put("message", "Payslips generated successfully for " + (month != null ? month : "the selected period") + ".");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly-details")
    public ResponseEntity<Map<String, Object>> getMonthlySalaryDetails(@RequestParam String month) {
        Map<String, Object> response = new HashMap<>();

        if (month == null || !month.matches("^\\d{4}-\\d{2}$")) {
            response.put("success", false);
            response.put("message", "Valid month is required (format: YYYY-MM)");
            return ResponseEntity.badRequest().body(response);
        }

        List<MonthlySalary> monthlySalaries = monthlySalaryRepository.findBySalaryMonthOrderByEmployee_FullNameAsc(month);

        List<Map<String, Object>> rows = new ArrayList<>();
        double totalNetSalary = 0.0;
        double totalMonthlyPayroll = 0.0;
        int processedCount = 0;

        for (MonthlySalary saved : monthlySalaries) {
            Employee emp = saved.getEmployee();
            if (emp == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("employeeId", emp.getEmployeeId());
            row.put("fullName", emp.getFullName());
            row.put("position", emp.getPosition());
            row.put("month", month);
            row.put("basicSalary", saved.getBasicSalary());
            row.put("overtimeHours", saved.getOvertimeHours());
            row.put("overtimeAmount", saved.getOvertimeAmount());
            row.put("allowances", saved.getAllowances());
            row.put("advances", saved.getAdvances());
            row.put("grossSalary", saved.getGrossSalary());
            row.put("netSalary", saved.getNetSalary());
            row.put("status", "Processed");
            processedCount++;
            double netSalary = saved.getNetSalary() != null ? saved.getNetSalary() : 0.0;
            totalNetSalary += netSalary;
            totalMonthlyPayroll += netSalary;
            rows.add(row);
        }

        response.put("success", true);
        response.put("month", month);
        response.put("totalEmployees", processedCount);
        response.put("processedEmployees", processedCount);
        response.put("totalNetSalary", totalNetSalary);
        response.put("totalMonthlyPayroll", totalMonthlyPayroll);
        response.put("rows", rows);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payslips/download")
    public ResponseEntity<byte[]> downloadPayslips(@RequestParam(required = false) String month) {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Month,Employee ID,Full Name,Position,Basic Salary (LKR),Overtime Hours,Overtime Amount (LKR),Allowances (LKR),Advances (LKR),Gross Salary (LKR),Net Salary (LKR)\n");

        if (month != null && !month.isEmpty()) {
            List<MonthlySalary> monthlySalaries = monthlySalaryRepository.findBySalaryMonthOrderByEmployee_FullNameAsc(month);
            for (MonthlySalary salary : monthlySalaries) {
                Employee emp = salary.getEmployee();
                csvBuilder
                        .append(salary.getSalaryMonth()).append(',')
                        .append(emp.getEmployeeId() != null ? emp.getEmployeeId() : "").append(',')
                        .append(emp.getFullName() != null ? emp.getFullName() : "").append(',')
                        .append(emp.getPosition() != null ? emp.getPosition() : "").append(',')
                        .append(salary.getBasicSalary()).append(',')
                        .append(salary.getOvertimeHours()).append(',')
                        .append(salary.getOvertimeAmount()).append(',')
                        .append(salary.getAllowances()).append(',')
                        .append(salary.getAdvances()).append(',')
                        .append(salary.getGrossSalary()).append(',')
                        .append(salary.getNetSalary())
                        .append('\n');
            }
        } else {
            List<Employee> employees = employeeRepository.findAll();
            for (Employee emp : employees) {
                csvBuilder
                        .append("N/A").append(',')
                        .append(emp.getEmployeeId() != null ? emp.getEmployeeId() : "").append(',')
                        .append(emp.getFullName() != null ? emp.getFullName() : "").append(',')
                        .append(emp.getPosition() != null ? emp.getPosition() : "").append(',')
                        .append(emp.getBasicSalary() != null ? emp.getBasicSalary() : 0.0).append(',')
                        .append("0.0,0.0,0.0,0.0,")
                        .append(emp.getBasicSalary() != null ? emp.getBasicSalary() : 0.0).append(',')
                        .append(emp.getBasicSalary() != null ? emp.getBasicSalary() : 0.0)
                        .append('\n');
            }
        }

        byte[] fileBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        String filename = "payslips-" + (month != null && !month.isEmpty() ? month : "current") + ".csv";
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }
}


