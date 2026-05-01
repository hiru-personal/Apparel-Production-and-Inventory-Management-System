package org.example.pim_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.Employee;
import org.example.pim_system.repository.EmployeeRepository;
import org.example.pim_system.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerEmployee(@RequestBody Map<String, String> employeeData,
                                                                  HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String employeeId = employeeData.get("employeeId");
            String fullName = employeeData.get("fullName");
            String position = employeeData.get("position");
            String basicSalaryStr = employeeData.get("basicSalary");
            String bankAccount = employeeData.get("bankAccount");
            String startDateStr = employeeData.get("startDate");
            
            // Validate required fields
            if (employeeId == null || employeeId.isEmpty()) {
                response.put("success", false);
                response.put("message", "Employee ID is required!");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (fullName == null || fullName.isEmpty()) {
                response.put("success", false);
                response.put("message", "Full Name is required!");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (position == null || position.isEmpty()) {
                response.put("success", false);
                response.put("message", "Position is required!");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (basicSalaryStr == null || basicSalaryStr.isEmpty()) {
                response.put("success", false);
                response.put("message", "Basic Salary is required!");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (startDateStr == null || startDateStr.isEmpty()) {
                response.put("success", false);
                response.put("message", "Start Date is required!");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if employee ID already exists
            if (employeeRepository.existsByEmployeeId(employeeId)) {
                response.put("success", false);
                response.put("message", "Employee ID already exists!");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create new employee
            Employee employee = new Employee();
            employee.setEmployeeId(employeeId);
            employee.setFullName(fullName);
            employee.setPosition(position);
            employee.setBasicSalary(Double.parseDouble(basicSalaryStr));
            employee.setBankAccount(bankAccount);
            
            // Parse start date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            employee.setStartDate(LocalDate.parse(startDateStr, formatter));
            
            employeeRepository.save(employee);

            auditLogService.log(
                    "EMPLOYEE_CREATED",
                    String.format("Registered new employee: ID=%s, Name=%s, Position=%s, Basic Salary=LKR %s, Start Date=%s",
                            employeeId, fullName, position, basicSalaryStr, startDateStr),
                    request
            );
            
            response.put("success", true);
            response.put("message", "Employee registered successfully!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error registering employee: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAllByOrderByFullNameAsc();
        return ResponseEntity.ok(employees);
    }
}



