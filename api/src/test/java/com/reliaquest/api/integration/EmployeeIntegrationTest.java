package com.reliaquest.api.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"mock.server.url=http://localhost:8112/api/v1/employee"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmployeeIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/employee";

        // Add small delay between tests to reduce rate limiting
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @BeforeAll
    static void checkMockServerAvailability() {
        // This test assumes the mock server is running on port 8112
        // In a real scenario, you might start/stop the mock server programmatically
        // or use a test container
        System.out.println(
                "IMPORTANT: Ensure mock server is running on http://localhost:8112 before running integration tests");
        System.out.println("Run './start-test-env.sh start' to start the mock server automatically");

        // Try to verify mock server is accessible
        try {
            java.net.URL url = new java.net.URL("http://localhost:8112/api/v1/employee");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("WARNING: Mock server returned status " + responseCode);
            } else {
                System.out.println("Mock server is accessible");
            }
        } catch (Exception e) {
            System.err.println("WARNING: Could not reach mock server: " + e.getMessage());
            System.err.println("Integration tests may fail if mock server is not running");
        }
    }

    @Test
    @Order(1)
    void getAllEmployees_shouldReturnEmployeeList() {
        // Act
        ResponseEntity<List<Employee>> response = restTemplate.exchange(
                baseUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<Employee>>() {});

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() > 0);

        Employee firstEmployee = response.getBody().get(0);
        assertNotNull(firstEmployee.getId());
        assertNotNull(firstEmployee.getName());
        assertNotNull(firstEmployee.getSalary());
        assertTrue(firstEmployee.getSalary() > 0);
    }

    @Test
    @Order(2)
    void getEmployeeById_withValidId_shouldReturnEmployee() {
        // Arrange - First get all employees to get a valid ID
        ResponseEntity<List<Employee>> allEmployeesResponse = restTemplate.exchange(
                baseUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<Employee>>() {});

        assumeTrue(
                allEmployeesResponse.getBody() != null
                        && !allEmployeesResponse.getBody().isEmpty(),
                "At least one employee should exist for this test");

        String validId = allEmployeesResponse.getBody().get(0).getId();

        // Act
        ResponseEntity<Employee> response = restTemplate.getForEntity(baseUrl + "/" + validId, Employee.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(validId, response.getBody().getId());
        assertNotNull(response.getBody().getName());
        assertNotNull(response.getBody().getSalary());
    }

    @Test
    @Order(3)
    void getEmployeeById_withInvalidId_shouldReturn400() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/nonexistent-id-12345", String.class);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid employee ID or request data", response.getBody());
    }

    @Test
    @Order(4)
    void getEmployeeById_withEmptyId_shouldReturn400() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/ ", // Space will be trimmed to empty
                String.class);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid employee ID or request data", response.getBody());
    }

    @Test
    @Order(5)
    void getEmployeesByNameSearch_shouldReturnMatchingEmployees() {
        // Arrange - Use a common name fragment
        String searchString = "a"; // Most employee names should contain 'a'

        // Act
        ResponseEntity<List<Employee>> response = restTemplate.exchange(
                baseUrl + "/search/" + searchString,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Employee>>() {});

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify all returned employees contain the search string (case-insensitive)
        response.getBody()
                .forEach(employee -> assertTrue(
                        employee.getName().toLowerCase().contains(searchString.toLowerCase()),
                        "Employee " + employee.getName() + " should contain '" + searchString + "'"));
    }

    @Test
    @Order(6)
    void getHighestSalary_shouldReturnValidSalary() {
        // Act
        ResponseEntity<Integer> response = restTemplate.getForEntity(baseUrl + "/highestSalary", Integer.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() > 0, "Highest salary should be greater than 0");

        // Additional verification - the highest salary should be among all employee salaries
        ResponseEntity<List<Employee>> allEmployeesResponse = restTemplate.exchange(
                baseUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<Employee>>() {});

        if (allEmployeesResponse.getBody() != null) {
            Integer calculatedHighest = allEmployeesResponse.getBody().stream()
                    .mapToInt(Employee::getSalary)
                    .max()
                    .orElse(0);
            assertEquals(
                    calculatedHighest, response.getBody(), "Returned highest salary should match calculated highest");
        }
    }

    @Test
    @Order(7)
    void getTopTenHighestEarningEmployeeNames_shouldReturnUpTo10Names() {
        // Act
        ResponseEntity<List<String>> response = restTemplate.exchange(
                baseUrl + "/topTenHighestEarningEmployeeNames",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {});

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() <= 10, "Should return at most 10 names");
        assertTrue(response.getBody().size() > 0, "Should return at least 1 name");

        // Verify all entries are valid names (not null or empty)
        response.getBody().forEach(name -> {
            assertNotNull(name, "Employee name should not be null");
            assertFalse(name.trim().isEmpty(), "Employee name should not be empty");
        });
    }

    @Test
    @Order(8)
    void createEmployee_withValidRequest_shouldCreateAndReturnEmployee() {
        // Arrange
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .name("Integration Test User")
                .salary(75000)
                .age(30)
                .title("Test Engineer")
                .build();

        // Act with retry logic for rate limiting
        ResponseEntity<Employee> response =
                retryOnRateLimit(() -> restTemplate.postForEntity(baseUrl, request, Employee.class));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Integration Test User", response.getBody().getName());
        assertEquals(75000, response.getBody().getSalary());
        assertEquals(30, response.getBody().getAge());
        assertEquals("Test Engineer", response.getBody().getTitle());
        assertNotNull(response.getBody().getId(), "Created employee should have an ID");
        assertNotNull(response.getBody().getEmail(), "Created employee should have an email");
    }

    @Test
    @Order(9)
    void deleteEmployee_fullLifecycle_shouldCreateThenDelete() {
        // Step 1: Create an employee
        CreateEmployeeRequest createRequest = CreateEmployeeRequest.builder()
                .name("To Be Deleted User")
                .salary(60000)
                .age(25)
                .title("Temporary Employee")
                .build();

        ResponseEntity<Employee> createResponse =
                retryOnRateLimit(() -> restTemplate.postForEntity(baseUrl, createRequest, Employee.class));

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        String employeeId = createResponse.getBody().getId();
        String employeeName = createResponse.getBody().getName();

        // Step 2: Verify employee exists
        ResponseEntity<Employee> getResponse = restTemplate.getForEntity(baseUrl + "/" + employeeId, Employee.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());

        // Step 3: Delete the employee
        ResponseEntity<String> deleteResponse =
                restTemplate.exchange(baseUrl + "/" + employeeId, HttpMethod.DELETE, null, String.class);

        // Assert deletion was successful
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        assertEquals("\"" + employeeName + "\"", deleteResponse.getBody());

        // Step 4: Verify employee no longer exists
        ResponseEntity<String> verifyDeletedResponse =
                restTemplate.getForEntity(baseUrl + "/" + employeeId, String.class);
        assertEquals(
                HttpStatus.BAD_REQUEST,
                verifyDeletedResponse.getStatusCode(),
                "Employee should no longer exist after deletion");
    }

    @Test
    @Order(10)
    void deleteEmployee_withInvalidId_shouldReturn400() {
        // Act with retry logic for rate limiting
        ResponseEntity<String> response = retryOnRateLimit(() ->
                restTemplate.exchange(baseUrl + "/nonexistent-delete-id-54321", HttpMethod.DELETE, null, String.class));

        // Assert - should be 400 for invalid ID, but might be 429 due to rate limiting
        assertTrue(
                response.getStatusCode() == HttpStatus.BAD_REQUEST
                        || response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS,
                "Expected 400 (Bad Request) or 429 (Too Many Requests), got: " + response.getStatusCode());

        if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {
            assertEquals("Invalid employee ID or request data", response.getBody());
        } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            assertEquals("Service temporarily unavailable due to rate limiting", response.getBody());
        }
    }

    private void assumeTrue(boolean condition, String message) {
        if (!condition) {
            Assumptions.assumeTrue(false, message);
        }
    }

    private <T> ResponseEntity<T> retryOnRateLimit(java.util.function.Supplier<ResponseEntity<T>> operation) {
        int maxRetries = 5;
        int baseDelay = 2000; // 2 second base delay

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && attempt < maxRetries - 1) {
                    System.out.println("Rate limited on attempt " + (attempt + 1) + ", retrying in "
                            + (baseDelay * (attempt + 1)) + "ms");
                    try {
                        Thread.sleep(baseDelay * (attempt + 1)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Test interrupted", ie);
                    }
                    continue;
                }
                throw e;
            } catch (Exception e) {
                // Handle JSON parsing errors that might be due to error responses
                if (e.getMessage() != null
                        && (e.getMessage().contains("JSON parse error")
                                || e.getMessage().contains("Service temporarily"))
                        && attempt < maxRetries - 1) {
                    System.out.println(
                            "JSON parse error on attempt " + (attempt + 1) + " (likely rate limited), retrying in "
                                    + (baseDelay * (attempt + 1))
                                    + "ms");
                    try {
                        Thread.sleep(baseDelay * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Test interrupted", ie);
                    }
                    continue;
                }
                throw e;
            }
        }

        throw new RuntimeException("Max retries exceeded");
    }
}
