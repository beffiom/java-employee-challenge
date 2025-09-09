package com.reliaquest.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllEmployees_shouldReturnListOfEmployees() throws Exception {
        // Arrange
        List<Employee> employees =
                Arrays.asList(createEmployee("1", "John Doe", 50000), createEmployee("2", "Jane Smith", 60000));
        when(employeeService.getAllEmployees()).thenReturn(employees);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].salary").value(50000))
                .andExpect(jsonPath("$[1].name").value("Jane Smith"))
                .andExpect(jsonPath("$[1].salary").value(60000));
    }

    @Test
    void getAllEmployees_whenServiceThrowsTooManyRequests_shouldReturn429() throws Exception {
        // Arrange
        when(employeeService.getAllEmployees()).thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        // Act & Assert
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Service temporarily unavailable due to rate limiting"));
    }

    @Test
    void getEmployeesByNameSearch_shouldReturnMatchingEmployees() throws Exception {
        // Arrange
        List<Employee> employees = Arrays.asList(createEmployee("1", "John Doe", 50000));
        when(employeeService.searchEmployeesByName("john")).thenReturn(employees);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employee/search/john"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("John Doe"));
    }

    @Test
    void getEmployeeById_withValidId_shouldReturnEmployee() throws Exception {
        // Arrange
        Employee employee = createEmployee("1", "John Doe", 50000);
        when(employeeService.getEmployeeById("1")).thenReturn(employee);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employee/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.salary").value(50000));
    }

    @Test
    void getEmployeeById_withInvalidId_shouldReturn400() throws Exception {
        // Arrange
        when(employeeService.getEmployeeById("invalid"))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        mockMvc.perform(get("/api/v1/employee/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid employee ID or request data"));
    }

    @Test
    void getHighestSalaryOfEmployees_shouldReturnHighestSalary() throws Exception {
        // Arrange
        when(employeeService.getHighestSalary()).thenReturn(75000);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employee/highestSalary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("75000"));
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_shouldReturnTopEarners() throws Exception {
        // Arrange
        List<String> topEarners = Arrays.asList("Employee1", "Employee2", "Employee3");
        when(employeeService.getTopTenHighestEarningEmployeeNames()).thenReturn(topEarners);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employee/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Employee1"))
                .andExpect(jsonPath("$[1]").value("Employee2"))
                .andExpect(jsonPath("$[2]").value("Employee3"));
    }

    @Test
    void createEmployee_withValidRequest_shouldReturnCreatedEmployee() throws Exception {
        // Arrange
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .name("New Employee")
                .salary(55000)
                .age(28)
                .title("Developer")
                .build();

        Employee createdEmployee = createEmployee("3", "New Employee", 55000);
        when(employeeService.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(createdEmployee);

        // Act & Assert
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("3"))
                .andExpect(jsonPath("$.name").value("New Employee"))
                .andExpect(jsonPath("$.salary").value(55000));
    }

    @Test
    void createEmployee_whenServiceThrowsTooManyRequests_shouldReturn429() throws Exception {
        // Arrange
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .name("New Employee")
                .salary(55000)
                .age(28)
                .title("Developer")
                .build();

        when(employeeService.createEmployee(any(CreateEmployeeRequest.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        // Act & Assert
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Service temporarily unavailable due to rate limiting"));
    }

    @Test
    void deleteEmployeeById_withValidId_shouldReturnEmployeeName() throws Exception {
        // Arrange
        when(employeeService.deleteEmployeeById("1")).thenReturn("John Doe");

        // Act & Assert
        mockMvc.perform(delete("/api/v1/employee/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("\"John Doe\"")); // JSON string response
    }

    @Test
    void deleteEmployeeById_withInvalidId_shouldReturn400() throws Exception {
        // Arrange
        when(employeeService.deleteEmployeeById("invalid"))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        mockMvc.perform(delete("/api/v1/employee/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid employee ID or request data"));
    }

    @Test
    void deleteEmployeeById_whenServiceThrowsTooManyRequests_shouldReturn429() throws Exception {
        // Arrange
        when(employeeService.deleteEmployeeById("1"))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        // Act & Assert
        mockMvc.perform(delete("/api/v1/employee/1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Service temporarily unavailable due to rate limiting"));
    }

    private Employee createEmployee(String id, String name, Integer salary) {
        return Employee.builder()
                .id(id)
                .name(name)
                .salary(salary)
                .age(30)
                .title("Developer")
                .email(name.toLowerCase().replace(" ", "") + "@company.com")
                .build();
    }
}
