package com.tutorial.taskmanager.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Global exception handler for the Task Manager application.
 *
 * <p>This class uses {@code @RestControllerAdvice} to provide centralized exception handling
 * across all {@code @RestController} classes. When an exception is thrown from any controller,
 * Spring looks for a matching {@code @ExceptionHandler} method here.
 *
 * <h2>What is @RestControllerAdvice?</h2>
 * <p>A specialization of {@code @ControllerAdvice} that:
 * <ul>
 *   <li>Applies to all controllers in the application</li>
 *   <li>Combines {@code @ControllerAdvice} + {@code @ResponseBody}</li>
 *   <li>Automatically serializes return values to JSON</li>
 * </ul>
 *
 * <h2>Exception Mapping:</h2>
 * <table border="1">
 *   <tr><th>Exception</th><th>HTTP Status</th><th>Use Case</th></tr>
 *   <tr><td>ResourceNotFoundException</td><td>404 Not Found</td><td>Entity not found by ID/field</td></tr>
 *   <tr><td>ValidationException</td><td>400 Bad Request</td><td>Business rule violations</td></tr>
 *   <tr><td>IllegalArgumentException</td><td>400 Bad Request</td><td>Invalid input parameters</td></tr>
 *   <tr><td>Exception (fallback)</td><td>500 Internal Server Error</td><td>Unexpected errors</td></tr>
 * </table>
 *
 * <h2>Error Response Format:</h2>
 * <pre>
 * {
 *   "timestamp": "2024-01-15T10:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "appUser with id '999' not found"
 * }
 * </pre>
 *
 * @see ResourceNotFoundException
 * @see ValidationException
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";

    // ========================================================================
    // 404 NOT FOUND - Resource not found
    // ========================================================================

    /**
     * Handles ResourceNotFoundException.
     *
     * <p>Thrown when an entity cannot be found by ID or other unique identifier.
     * Returns HTTP 404 Not Found.
     *
     * <p><strong>Example scenarios:</strong>
     * <ul>
     *   <li>GET /api/users/999 - User with ID 999 doesn't exist</li>
     *   <li>GET /api/users/username/unknown - Username doesn't exist</li>
     *   <li>DELETE /api/users/999 - Trying to delete non-existent user</li>
     * </ul>
     *
     * @param ex the ResourceNotFoundException
     * @return ResponseEntity with error details and 404 status
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ========================================================================
    // 401 UNAUTHORIZED - Authentication failures
    // ========================================================================

    /**
     * Handles BadCredentialsException.
     *
     * <p>Thrown when authentication fails due to incorrect password.
     * Returns HTTP 401 Unauthorized.
     *
     * <p><strong>Security Note:</strong> Always return a generic message like
     * "Invalid username or password" - never reveal whether the username
     * or password was incorrect (prevents user enumeration attacks).
     *
     * @param ex the BadCredentialsException
     * @return ResponseEntity with error details and 401 status
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    /**
     * Handles UsernameNotFoundException.
     *
     * <p>Thrown when authentication fails because the user doesn't exist.
     * Returns HTTP 401 Unauthorized.
     *
     * <p><strong>Security Note:</strong> Returns the same generic message as
     * {@link #handleBadCredentials} to prevent user enumeration attacks.
     * An attacker shouldn't be able to determine if a username exists.
     *
     * @param ex the UsernameNotFoundException
     * @return ResponseEntity with error details and 401 status
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UsernameNotFoundException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    // ========================================================================
    // 400 BAD REQUEST - Validation and business rule violations
    // ========================================================================

    /**
     * Handles ValidationException.
     *
     * <p>Thrown when business rules are violated.
     * Returns HTTP 400 Bad Request.
     *
     * <p><strong>Example scenarios:</strong>
     * <ul>
     *   <li>Project name not unique for user</li>
     *   <li>Task assigned to project owned by different user</li>
     * </ul>
     *
     * @param ex the ValidationException
     * @return ResponseEntity with error details and 400 status
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles IllegalArgumentException.
     *
     * <p>Thrown when input parameters are invalid.
     * Returns HTTP 400 Bad Request.
     *
     * <p><strong>Example scenarios:</strong>
     * <ul>
     *   <li>Username already exists</li>
     *   <li>Email already exists</li>
     *   <li>Required field is null or empty</li>
     * </ul>
     *
     * @param ex the IllegalArgumentException
     * @return ResponseEntity with error details and 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ========================================================================
    // 500 INTERNAL SERVER ERROR - Fallback for unexpected errors
    // ========================================================================

    /**
     * Fallback handler for all other exceptions.
     *
     * <p>Catches any exception not handled by specific handlers.
     * Returns HTTP 500 Internal Server Error.
     *
     * <p><strong>Security Note:</strong> In production, avoid exposing internal error details.
     * Log the full exception but return a generic message to the client.
     *
     * @param ex the Exception
     * @return ResponseEntity with error details and 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        LOGGER.error(UNEXPECTED_ERROR_MESSAGE, ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_MESSAGE);
    }

    // ========================================================================
    // RESPONSE ENTITY Helper
    // ========================================================================

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message) {
        ErrorResponse errorResponse = ErrorResponse.of(status, message);
        return ResponseEntity.status(status).body(errorResponse);
    }

    // ========================================================================
    // ERROR RESPONSE DTO (Inner Class)
    // ========================================================================

    /**
     * Standard error response format for all API errors.
     *
     * <p>This record provides a consistent structure for error responses,
     * making it easier for clients to handle errors.
     *
     * <p><strong>Fields:</strong>
     * <ul>
     *   <li>timestamp - When the error occurred</li>
     *   <li>status - HTTP status code (e.g., 404, 400, 500)</li>
     *   <li>error - HTTP status reason phrase (e.g., "Not Found")</li>
     *   <li>message - Detailed error message</li>
     * </ul>
     */
    public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message) {
        /**
         * Factory method to create an ErrorResponse.
         *
         * @param status the HTTP status
         * @param message the error message
         * @return a new ErrorResponse instance
         */
        public static ErrorResponse of(HttpStatus status, String message) {
            return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message);
        }
    }
}
