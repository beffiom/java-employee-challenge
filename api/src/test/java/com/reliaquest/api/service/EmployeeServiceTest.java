package com.reliaquest.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.MockEmployee;
import com.reliaquest.api.model.MockEmployeeResponse;
import com.reliaquest.api.model.MockServerResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EmployeeService employeeService;

    private final String mockServerUrl = "http://localhost:8112/api/v1/employee";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(employeeService, "mockServerUrl", mockServerUrl);
    }

    @Test
    void getAllEmployees_shouldReturnListOfEmployees() {
        // Arrange
        List<MockEmployee> mockEmployees =
                Arrays.asList(createMockEmployee("1", "John Doe", 50000), createMockEmployee("2", "Jane Smith", 60000));
        MockServerResponse mockResponse = new MockServerResponse(mockEmployees, "success");
        when(restTemplate.getForObject(mockServerUrl, MockServerResponse.class)).thenReturn(mockResponse);

        // Act
        List<Employee> result = employeeService.getAllEmployees();

        // Assert
        assertEquals(2, result.size());
        assertEquals("John Doe", result.get(0).getName());
        assertEquals(50000, result.get(0).getSalary());
        assertEquals("Jane Smith", result.get(1).getName());
        assertEquals(60000, result.get(1).getSalary());
        verify(restTemplate).getForObject(mockServerUrl, MockServerResponse.class);
    }

    @Test
    void getAllEmployees_withNullResponse_shouldReturnEmptyList() {
        // Arrange
        when(restTemplate.getForObject(mockServerUrl, MockServerResponse.class)).thenReturn(null);

        // Act
        List<Employee> result = employeeService.getAllEmployees();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void getEmployeeById_withValidId_shouldReturnEmployee() {
        // Arrange
        List<MockEmployee> mockEmployees =
                Arrays.asList(createMockEmployee("1", "John Doe", 50000), createMockEmployee("2", "Jane Smith", 60000));
        MockServerResponse mockResponse = new MockServerResponse(mockEmployees, "success");
        when(restTemplate.getForObject(mockServerUrl, MockServerResponse.class)).thenReturn(mockResponse);

        // Act
        Employee result = employeeService.getEmployeeById("1");

        // Assert
        assertEquals("1", result.getId());
        assertEquals("John Doe", result.getName());
        assertEquals(50000, result.getSalary());
    }

    @Test
    void getEmployeeById_withEmptyId_shouldThrowBadRequest() {
        // Act & Assert
        HttpClientErrorException exception =
                assertThrows(HttpClientErrorException.class, () -> employeeService.getEmployeeById(""));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void getEmployeeById_withNullId_shouldThrowBadRequest() {
        // Act & Assert
        HttpClientErrorException exception =
                assertThrows(HttpClientErrorException.class, () -> employeeService.getEmployeeById(null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void getEmployeeById_withNonExistentId_shouldThrowBadRequest() {
        // Arrange
        List<MockEmployee> mockEmployees = Arrays.asList(createMockEmployee("1", "John Doe", 50000));
        MockServerResponse mockResponse = new MockServerResponse(mockEmployees, "success");
        when(restTemplate.getForObject(mockServerUrl, MockServerResponse.class)).thenReturn(mockResponse);

        // Act & Assert
        HttpClientErrorException exception =
                assertThrows(HttpClientErrorException.class, () -> employeeService.getEmployeeById("999"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createEmployee_withValidRequest_shouldReturnCreatedEmployee() {
        // Arrange
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .name("New Employee")
                .salary(55000)
                .age(28)
                .title("Developer")
                .build();

        MockEmployee createdMockEmployee = createMockEmployee("3", "New Employee", 55000);
        MockEmployeeResponse mockResponse = new MockEmployeeResponse(createdMockEmployee, "success");
        when(restTemplate.postForObject(eq(mockServerUrl), any(), eq(MockEmployeeResponse.class)))
                .thenReturn(mockResponse);

        // Act
        Employee result = employeeService.createEmployee(request);

        // Assert
        assertEquals("3", result.getId());
        assertEquals("New Employee", result.getName());
        assertEquals(55000, result.getSalary());
        verify(restTemplate).postForObject(eq(mockServerUrl), any(), eq(MockEmployeeResponse.class));
    }

    @Test
    void searchEmployeesByName_shouldReturnMatchingEmployees() {
        // Arrange
        List<MockEmployee> mockEmployees = Arrays.asList(
                createMockEmployee("1", "John Doe", 50000),
                createMockEmployee("2", "Jane Smith", 60000),
                createMockEmployee("3", "John Johnson", 55000));
        MockServerResponse mockResponse = new MockServerResponse(mockEmployees, "success");
        when(restTemplate.getForObject(mockServerUrl, MockServerResponse.class)).thenReturn(mockResponse);

        // Act
        List<Employee> result = employeeService.searchEmployeesByName("john");

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(emp -> emp.getName().equals("John Doe")));
        assertTrue(result.stream().anyMatch(emp -> emp.getName().equals("John Johnson")));
    }

    @Test
    void getHighestSalary_shouldReturnMaxSalary() {
        // Arrange
        List<MockEmployee> mockEmployees = Arrays.asList(
                createMockEmployee("1", "John Doe", 50000),
                createMockEmployee("2", "Jane Smith", 75000),
                createMockEmployee("3", "Bob Wilson", 45000));
        MockServerResponse mockResponse = new MockServerResponse(mockEmployees, "success");
        when(restTemplate.getForObject(mockServerUrl, MockServerResponse.class)).thenReturn(mockResponse);

        // Act
        Integer result = employeeService.getHighestSalary();

        // Assert
        assertEquals(75000, result);
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_shouldReturnTopEarners() {
        // Arrange - Create more than 10 employees
        List<MockEmployee> mockEmployees = Arrays.asList(
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
        MockServerResponse mockResponse = new MockServerResponse(mockEmployees, "success");
        when(restTemplate.getForObject(mockServerUrl, MockServerResponse.class)).thenReturn(mockResponse);

        // Act
        List<String> result = employeeService.getTopTenHighestEarningEmployeeNames();

        // Assert
        assertEquals(10, result.size());
        assertEquals("Employee1", result.get(0)); // Highest salary
        assertEquals("Employee10", result.get(9)); // 10th highest
        assertFalse(result.contains("Employee11")); // Should not be included
    }

    @Test
    void deleteEmployeeById_withValidId_shouldReturnEmployeeName() {
        // Arrange
        List<MockEmployee> mockEmployees = Arrays.asList(createMockEmployee("1", "John Doe", 50000));
        MockServerResponse mockResponse = new MockServerResponse(mockEmployees, "success");
        when(restTemplate.getForObject(mockServerUrl, MockServerResponse.class)).thenReturn(mockResponse);
        when(restTemplate.exchange(
                        eq(mockServerUrl + "/John Doe"),
                        eq(org.springframework.http.HttpMethod.DELETE),
                        any(),
                        eq(com.reliaquest.api.model.DeleteMockEmployeeResponse.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(
                        new com.reliaquest.api.model.DeleteMockEmployeeResponse(true, "success")));

        // Act
        String result = employeeService.deleteEmployeeById("1");

        // Assert
        assertEquals("John Doe", result);
        verify(restTemplate)
                .exchange(
                        eq(mockServerUrl + "/John Doe"),
                        eq(org.springframework.http.HttpMethod.DELETE),
                        any(),
                        eq(com.reliaquest.api.model.DeleteMockEmployeeResponse.class));
    }

    @Test
    void deleteEmployeeById_withInvalidId_shouldThrowBadRequest() {
        // Arrange
        List<MockEmployee> mockEmployees = Arrays.asList(createMockEmployee("1", "John Doe", 50000));
        MockServerResponse mockResponse = new MockServerResponse(mockEmployees, "success");
        when(restTemplate.getForObject(mockServerUrl, MockServerResponse.class)).thenReturn(mockResponse);

        // Act & Assert
        HttpClientErrorException exception =
                assertThrows(HttpClientErrorException.class, () -> employeeService.deleteEmployeeById("999"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private MockEmployee createMockEmployee(String id, String name, Integer salary) {
        return MockEmployee.builder()
                .id(id)
                .employeeName(name)
                .employeeSalary(salary)
                .employeeAge(30)
                .employeeTitle("Developer")
                .employeeEmail(name.toLowerCase().replace(" ", "") + "@company.com")
                .build();
    }
}
