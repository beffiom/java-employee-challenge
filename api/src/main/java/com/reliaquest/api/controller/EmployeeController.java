package com.reliaquest.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController implements IEmployeeController<Employee, CreateEmployeeRequest> {

    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;

    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        log.info("GET /api/v1/employee - Getting all employees");
        List<Employee> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(String searchString) {
        log.info("GET /api/v1/employee/search/{} - Searching employees by name", searchString);
        List<Employee> employees = employeeService.searchEmployeesByName(searchString);
        return ResponseEntity.ok(employees);
    }

    @Override
    public ResponseEntity<Employee> getEmployeeById(String id) {
        log.info("GET /api/v1/employee/{} - Getting employee by ID", id);
        Employee employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        log.info("GET /api/v1/employee/highestSalary - Getting highest salary");
        Integer highestSalary = employeeService.getHighestSalary();
        return ResponseEntity.ok(highestSalary);
    }

    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        log.info(
                "GET /api/v1/employee/topTenHighestEarningEmployeeNames - Getting top 10 highest earning employee names");
        List<String> topTenNames = employeeService.getTopTenHighestEarningEmployeeNames();
        return ResponseEntity.ok(topTenNames);
    }

    @Override
    public ResponseEntity<Employee> createEmployee(CreateEmployeeRequest employeeInput) {
        log.info("POST /api/v1/employee - Creating new employee: {}", employeeInput.getName());
        Employee createdEmployee = employeeService.createEmployee(employeeInput);
        return ResponseEntity.ok(createdEmployee);
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        log.info("DELETE /api/v1/employee/{} - Deleting employee by ID", id);
        String deletedEmployeeName = employeeService.deleteEmployeeById(id);
        try {
            String jsonString = objectMapper.writeValueAsString(deletedEmployeeName);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(jsonString);
        } catch (JsonProcessingException e) {
            log.error("Error serializing employee name to JSON", e);
            return ResponseEntity.ok(deletedEmployeeName);
        }
    }
}
