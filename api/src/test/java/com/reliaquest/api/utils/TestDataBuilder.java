package com.reliaquest.api.utils;

import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.MockEmployee;
import com.reliaquest.api.model.MockEmployeeResponse;
import com.reliaquest.api.model.MockServerResponse;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for building test data objects.
 * Provides consistent test data creation across unit and integration tests.
 */
public class TestDataBuilder {

    public static Employee createEmployee(String id, String name, Integer salary) {
        return Employee.builder()
                .id(id != null ? id : UUID.randomUUID().toString())
                .name(name)
                .salary(salary)
                .age(30)
                .title("Developer")
                .email(name.toLowerCase().replace(" ", "") + "@company.com")
                .build();
    }

    public static Employee createEmployee(String name, Integer salary) {
        return createEmployee(null, name, salary);
    }

    public static MockEmployee createMockEmployee(String id, String name, Integer salary) {
        return MockEmployee.builder()
                .id(id != null ? id : UUID.randomUUID().toString())
                .employeeName(name)
                .employeeSalary(salary)
                .employeeAge(30)
                .employeeTitle("Developer")
                .employeeEmail(name.toLowerCase().replace(" ", "") + "@company.com")
                .build();
    }

    public static MockEmployee createMockEmployee(String name, Integer salary) {
        return createMockEmployee(null, name, salary);
    }

    public static CreateEmployeeRequest createEmployeeRequest(String name, Integer salary, Integer age, String title) {
        return CreateEmployeeRequest.builder()
                .name(name)
                .salary(salary)
                .age(age != null ? age : 30)
                .title(title != null ? title : "Developer")
                .build();
    }

    public static CreateEmployeeRequest createEmployeeRequest(String name, Integer salary) {
        return createEmployeeRequest(name, salary, null, null);
    }

    public static MockServerResponse createMockServerResponse(List<MockEmployee> employees) {
        return MockServerResponse.builder()
                .data(employees)
                .status("Successfully processed request.")
                .build();
    }

    public static MockServerResponse createMockServerResponse(MockEmployee... employees) {
        return createMockServerResponse(Arrays.asList(employees));
    }

    public static MockEmployeeResponse createMockEmployeeResponse(MockEmployee employee) {
        return MockEmployeeResponse.builder()
                .data(employee)
                .status("Successfully processed request.")
                .build();
    }

    public static List<Employee> createEmployeeList() {
        return Arrays.asList(
                createEmployee("1", "John Doe", 50000),
                createEmployee("2", "Jane Smith", 75000),
                createEmployee("3", "Bob Wilson", 60000),
                createEmployee("4", "Alice Johnson", 85000),
                createEmployee("5", "Charlie Brown", 55000));
    }

    public static List<MockEmployee> createMockEmployeeList() {
        return Arrays.asList(
                createMockEmployee("1", "John Doe", 50000),
                createMockEmployee("2", "Jane Smith", 75000),
                createMockEmployee("3", "Bob Wilson", 60000),
                createMockEmployee("4", "Alice Johnson", 85000),
                createMockEmployee("5", "Charlie Brown", 55000));
    }

    public static List<MockEmployee> createLargeMockEmployeeList() {
        return Arrays.asList(
                createMockEmployee("1", "Employee1", 100000),
                createMockEmployee("2", "Employee2", 95000),
                createMockEmployee("3", "Employee3", 90000),
                createMockEmployee("4", "Employee4", 85000),
                createMockEmployee("5", "Employee5", 80000),
                createMockEmployee("6", "Employee6", 75000),
                createMockEmployee("7", "Employee7", 70000),
                createMockEmployee("8", "Employee8", 65000),
                createMockEmployee("9", "Employee9", 60000),
                createMockEmployee("10", "Employee10", 55000),
                createMockEmployee("11", "Employee11", 50000),
                createMockEmployee("12", "Employee12", 45000));
    }
}
