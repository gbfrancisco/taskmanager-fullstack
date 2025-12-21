package com.tutorial.taskmanager.exception;

import com.tutorial.taskmanager.exception.GlobalExceptionHandler.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GlobalExceptionHandler.
 *
 * <p>Tests the exception handling logic in isolation without loading Spring context.
 * Each test verifies that the correct HTTP status code and error response are returned
 * for different exception types.
 *
 * <p><strong>Testing Strategy:</strong>
 * <ul>
 *   <li>Test each exception handler method directly</li>
 *   <li>Verify HTTP status codes</li>
 *   <li>Verify ErrorResponse structure and content</li>
 *   <li>No Spring context needed - pure unit tests</li>
 * </ul>
 */
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    // ========================================================================
    // ResourceNotFoundException Tests - 404 Not Found
    // ========================================================================

    @Nested
    @DisplayName("ResourceNotFoundException Handler Tests")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("Should return 404 Not Found for ResourceNotFoundException")
        void handleResourceNotFoundException_Returns404() {
            // Arrange
            ResourceNotFoundException ex = new ResourceNotFoundException("appUser", 999L);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().error()).isEqualTo("Not Found");
            assertThat(response.getBody().message()).isEqualTo("appUser with id '999' not found");
            assertThat(response.getBody().timestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should return 404 with custom message for ResourceNotFoundException")
        void handleResourceNotFoundException_CustomMessage_Returns404() {
            // Arrange
            ResourceNotFoundException ex = new ResourceNotFoundException("appUser with username 'unknown' not found");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().message()).isEqualTo("appUser with username 'unknown' not found");
        }
    }

    // ========================================================================
    // ValidationException Tests - 400 Bad Request
    // ========================================================================

    @Nested
    @DisplayName("ValidationException Handler Tests")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Should return 400 Bad Request for ValidationException")
        void handleValidationException_Returns400() {
            // Arrange
            ValidationException ex = new ValidationException("Project name must be unique for user");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().error()).isEqualTo("Bad Request");
            assertThat(response.getBody().message()).isEqualTo("Project name must be unique for user");
            assertThat(response.getBody().timestamp()).isNotNull();
        }
    }

    // ========================================================================
    // IllegalArgumentException Tests - 400 Bad Request
    // ========================================================================

    @Nested
    @DisplayName("IllegalArgumentException Handler Tests")
    class IllegalArgumentExceptionTests {

        @Test
        @DisplayName("Should return 400 Bad Request for IllegalArgumentException")
        void handleIllegalArgumentException_Returns400() {
            // Arrange
            IllegalArgumentException ex = new IllegalArgumentException("username already exists");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().error()).isEqualTo("Bad Request");
            assertThat(response.getBody().message()).isEqualTo("username already exists");
            assertThat(response.getBody().timestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should return 400 for email already exists")
        void handleIllegalArgumentException_EmailExists_Returns400() {
            // Arrange
            IllegalArgumentException ex = new IllegalArgumentException("email already exists");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).isEqualTo("email already exists");
        }

        @Test
        @DisplayName("Should return 400 for null id")
        void handleIllegalArgumentException_NullId_Returns400() {
            // Arrange
            IllegalArgumentException ex = new IllegalArgumentException("id cannot be null");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).isEqualTo("id cannot be null");
        }
    }

    // ========================================================================
    // Generic Exception Tests - 500 Internal Server Error
    // ========================================================================

    @Nested
    @DisplayName("Generic Exception Handler Tests")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should return 500 Internal Server Error for unexpected exceptions")
        void handleGenericException_Returns500() {
            // Arrange
            Exception ex = new RuntimeException("Database connection failed");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
            // Note: Generic message - don't expose internal error details
            assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().timestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should not expose internal error details in response")
        void handleGenericException_HidesInternalDetails() {
            // Arrange - Simulate a detailed internal error
            Exception ex = new NullPointerException("Sensitive internal error: null value at line 42");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

            // Assert - Should return generic message, not the sensitive details
            assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().message()).doesNotContain("Sensitive");
            assertThat(response.getBody().message()).doesNotContain("line 42");
        }
    }

    // ========================================================================
    // ErrorResponse Record Tests
    // ========================================================================

    @Nested
    @DisplayName("ErrorResponse Factory Method Tests")
    class ErrorResponseTests {

        @Test
        @DisplayName("Should create ErrorResponse with factory method")
        void errorResponseOf_CreatesCorrectResponse() {
            // Act
            ErrorResponse response = ErrorResponse.of(HttpStatus.NOT_FOUND, "User not found");

            // Assert
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.error()).isEqualTo("Not Found");
            assertThat(response.message()).isEqualTo("User not found");
            assertThat(response.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should create ErrorResponse for different status codes")
        void errorResponseOf_DifferentStatusCodes() {
            // Test various status codes
            ErrorResponse badRequest = ErrorResponse.of(HttpStatus.BAD_REQUEST, "Invalid input");
            ErrorResponse serverError = ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "Server error");

            assertThat(badRequest.status()).isEqualTo(400);
            assertThat(badRequest.error()).isEqualTo("Bad Request");

            assertThat(serverError.status()).isEqualTo(500);
            assertThat(serverError.error()).isEqualTo("Internal Server Error");
        }
    }
}
