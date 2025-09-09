package com.reliaquest.api.service;

import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.CreateMockEmployeeRequest;
import com.reliaquest.api.model.DeleteMockEmployeeResponse;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.MockEmployee;
import com.reliaquest.api.model.MockEmployeeResponse;
import com.reliaquest.api.model.MockServerResponse;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final RestTemplate restTemplate;

    @Value("${mock.server.url:http://localhost:8112/api/v1/employee}")
    private String mockServerUrl;

    @Retryable(
            retryFor = HttpClientErrorException.TooManyRequests.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<Employee> getAllEmployees() {
        log.info("Fetching all employees from mock server");
        MockServerResponse response = restTemplate.getForObject(mockServerUrl, MockServerResponse.class);

        if (response == null || response.getData() == null) {
            log.warn("Received null response or data from mock server");
            return List.of();
        }

        log.info("Successfully fetched {} employees", response.getData().size());
        return response.getData().stream().map(this::convertToEmployee).collect(Collectors.toList());
    }

    public Employee getEmployeeById(String id) {
        log.info("Fetching employee with id: {}", id);

        // Input validation - all invalid employee IDs return 400
        if (id == null || id.trim().isEmpty()) {
            log.warn("Empty or null employee ID provided");
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        }

        // Get all employees and find the one with matching ID
        List<Employee> allEmployees = getAllEmployees();

        Employee employee = allEmployees.stream()
                .filter(emp -> emp.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Employee with id {} does not exist", id);
                    return new HttpClientErrorException(HttpStatus.BAD_REQUEST); // 400, not 404
                });

        log.info("Successfully found employee: {}", employee.getName());
        return employee;
    }

    @Retryable(
            retryFor = HttpClientErrorException.TooManyRequests.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public Employee createEmployee(CreateEmployeeRequest request) {
        log.info("Creating new employee: {}", request.getName());

        CreateMockEmployeeRequest mockRequest = CreateMockEmployeeRequest.builder()
                .name(request.getName())
                .salary(request.getSalary())
                .age(request.getAge())
                .title(request.getTitle())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateMockEmployeeRequest> entity = new HttpEntity<>(mockRequest, headers);

        MockEmployeeResponse response = restTemplate.postForObject(mockServerUrl, entity, MockEmployeeResponse.class);

        if (response == null || response.getData() == null) {
            log.error("Failed to create employee: {}", request.getName());
            throw new RuntimeException("Failed to create employee");
        }

        log.info("Successfully created employee: {}", response.getData().getEmployeeName());
        return convertToEmployee(response.getData());
    }

    @Retryable(
            retryFor = HttpClientErrorException.TooManyRequests.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public String deleteEmployeeById(String id) {
        log.info("Deleting employee with id: {}", id);

        // First get employee to find their name (since mock server deletes by name)
        Employee employee = getEmployeeById(id);
        String employeeName = employee.getName();

        String deleteUrl = mockServerUrl + "/" + employeeName;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, DeleteMockEmployeeResponse.class);

        log.info("Successfully deleted employee: {}", employeeName);
        return employeeName;
    }

    public List<Employee> searchEmployeesByName(String searchString) {
        log.info("Searching employees by name containing: {}", searchString);
        List<Employee> allEmployees = getAllEmployees();

        List<Employee> filteredEmployees = allEmployees.stream()
                .filter(emp -> emp.getName().toLowerCase().contains(searchString.toLowerCase()))
                .collect(Collectors.toList());

        log.info("Found {} employees matching search: {}", filteredEmployees.size(), searchString);
        return filteredEmployees;
    }

    public Integer getHighestSalary() {
        log.info("Finding highest salary among all employees");
        List<Employee> employees = getAllEmployees();

        Integer highestSalary =
                employees.stream().mapToInt(Employee::getSalary).max().orElse(0);

        log.info("Highest salary found: {}", highestSalary);
        return highestSalary;
    }

    public List<String> getTopTenHighestEarningEmployeeNames() {
        log.info("Finding top 10 highest earning employee names");
        List<Employee> employees = getAllEmployees();

        List<String> topTenNames = employees.stream()
                .sorted(Comparator.comparingInt(Employee::getSalary).reversed())
                .limit(10)
                .map(Employee::getName)
                .collect(Collectors.toList());

        log.info("Found {} top earning employees", topTenNames.size());
        return topTenNames;
    }

    private Employee convertToEmployee(MockEmployee mockEmployee) {
        return Employee.builder()
                .id(mockEmployee.getId())
                .name(mockEmployee.getEmployeeName())
                .salary(mockEmployee.getEmployeeSalary())
                .age(mockEmployee.getEmployeeAge())
                .title(mockEmployee.getEmployeeTitle())
                .email(mockEmployee.getEmployeeEmail())
                .build();
    }

    @Recover
    public List<Employee> recoverGetAllEmployees(HttpClientErrorException.TooManyRequests ex) {
        log.error("Max retries exceeded for getAllEmployees due to rate limiting");
        throw ex; // Let GlobalExceptionHandler handle the response
    }

    @Recover
    public Employee recoverCreateEmployee(HttpClientErrorException.TooManyRequests ex, CreateEmployeeRequest request) {
        log.error("Max retries exceeded for createEmployee({}) due to rate limiting", request.getName());
        throw ex; // Let GlobalExceptionHandler handle the response
    }

    @Recover
    public String recoverDeleteEmployeeById(HttpClientErrorException.TooManyRequests ex, String id) {
        log.error("Max retries exceeded for deleteEmployeeById({}) due to rate limiting", id);
        throw ex; // Let GlobalExceptionHandler handle the response
    }
}
