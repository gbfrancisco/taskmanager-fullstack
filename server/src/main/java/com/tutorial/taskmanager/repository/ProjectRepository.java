package com.tutorial.taskmanager.repository;

import com.tutorial.taskmanager.enums.ProjectStatus;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.model.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Project} entity.
 *
 * <p>This repository handles project-related database operations with support for
 * filtering by user (appUser), status, and project name.
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li>Find all projects belonging to a specific user</li>
 *   <li>Find projects by status (ACTIVE, COMPLETED, etc.)</li>
 *   <li>Search projects by name (exact or partial match)</li>
 *   <li>Combine filters (e.g., active projects for a specific user)</li>
 * </ul>
 *
 * @author Task Manager Tutorial
 * @see Project
 * @see JpaRepository
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // ==================== FIND BY APP USER ====================

    List<Project> findByAppUser(AppUser appUser);

    List<Project> findByAppUserId(Long appUserId);

    // ==================== FIND BY STATUS ====================

    List<Project> findByStatus(ProjectStatus status);

    // ==================== COMBINED FILTERS ====================

    List<Project> findByAppUserAndStatus(AppUser appUser, ProjectStatus status);

    List<Project> findByAppUserIdAndStatus(Long appUserId, ProjectStatus status);

    // ==================== FIND BY NAME ====================

    Optional<Project> findByName(String name);

    List<Project> findByNameContainingIgnoreCase(String name);

    // ==================== ADVANCED COMBINED QUERIES ====================

    List<Project> findByAppUserAndNameContainingIgnoreCase(AppUser appUser, String name);

    List<Project> findByAppUserIdAndNameContainingIgnoreCase(Long appUserId, String name);

    // ==================== EXISTENCE CHECKS ====================

    boolean existsByAppUserAndName(AppUser appUser, String name);

    boolean existsByAppUserIdAndName(Long appUserId, String name);

    // ==================== ENTITY GRAPH METHODS ====================
    // These methods fetch the appUser relationship in a single query
    // to avoid N+1 problems when mapping to DTOs with embedded user info.
    // See docs/10-jpa-entity-graph.md for details.

    /**
     * Find all projects with appUser eagerly fetched.
     * Use this for list views that display owner info.
     */
    @EntityGraph(attributePaths = {"appUser"})
    List<Project> findAllWithOwner();

    /**
     * Find project by ID with appUser eagerly fetched.
     * Use this for detail views.
     */
    @EntityGraph(attributePaths = {"appUser"})
    Optional<Project> findWithOwnerById(Long id);

    /**
     * Find projects by user ID with owner eagerly fetched.
     */
    @EntityGraph(attributePaths = {"appUser"})
    List<Project> findWithOwnerByAppUserId(Long appUserId);

    /**
     * Find projects by status with owner eagerly fetched.
     */
    @EntityGraph(attributePaths = {"appUser"})
    List<Project> findWithOwnerByStatus(ProjectStatus status);

    /**
     * Find projects by user ID and status with owner eagerly fetched.
     */
    @EntityGraph(attributePaths = {"appUser"})
    List<Project> findWithOwnerByAppUserIdAndStatus(Long appUserId, ProjectStatus status);

    /**
     * Search projects by name with owner eagerly fetched.
     */
    @EntityGraph(attributePaths = {"appUser"})
    List<Project> findWithOwnerByNameContainingIgnoreCase(String name);

    /**
     * Search projects by user ID and name with owner eagerly fetched.
     */
    @EntityGraph(attributePaths = {"appUser"})
    List<Project> findWithOwnerByAppUserIdAndNameContainingIgnoreCase(Long appUserId, String name);

    // ==================== NOTES FOR LATER ====================

    /*
     * SORTING EXAMPLES (add when needed):
     *
     * - findByAppUserOrderByNameAsc(AppUser appUser)
     *   Returns projects sorted alphabetically by name
     *
     * - findByStatusOrderByCreatedTimestampDesc(ProjectStatus status)
     *   Returns projects sorted by creation date (newest first)
     *
     * - For dynamic sorting, use:
     *   findByAppUser(AppUser appUser, Sort sort)
     *   Call with: Sort.by(Sort.Direction.ASC, "name")
     */

    /*
     * PAGINATION EXAMPLES (add when needed):
     *
     * - Page<Project> findByAppUser(AppUser appUser, Pageable pageable)
     *   Supports pagination + sorting together
     *
     * - Call with: PageRequest.of(0, 10, Sort.by("name"))
     *   (page 0, 10 items per page, sorted by name)
     */

    /*
     * ADVANCED QUERIES WITH @Query (add when needed):
     *
     * - Custom JPQL for complex business logic
     * - Native SQL for database-specific features
     * - JOIN FETCH to avoid N+1 query problems
     *
     * Example:
     * @Query("SELECT p FROM Project p JOIN FETCH p.tasks WHERE p.appUser = :appUser")
     * List<Project> findByAppUserWithTasks(@Param("appUser") AppUser appUser);
     */

    /*
     * KEY LEARNING POINTS:
     *
     * 1. **String search keywords**:
     *    - Containing: partial match (LIKE %keyword%)
     *    - StartingWith: prefix match (LIKE keyword%)
     *    - EndingWith: suffix match (LIKE %keyword)
     *    - IgnoreCase: case-insensitive comparison
     *
     * 2. **Existence checks** are more efficient than find + isPresent()
     *    - existsByOwnerAndName() vs findByOwnerAndName().isPresent()
     *    - Generates COUNT query instead of SELECT *
     *
     * 3. **Optional vs List**:
     *    - Use Optional<Project> when expecting single result (or none)
     *    - Use List<Project> when expecting multiple results (even if 0)
     *
     * 4. **Uniqueness constraints**:
     *    - Database doesn't enforce unique project names
     *    - Can enforce uniqueness per user in service layer
     *    - Use existsByAppUserAndName() before creating new projects
     *
     * 5. **Query method naming is strict**:
     *    - Field names must match exactly (case-sensitive)
     *    - Order of parameters must match order in method name
     *    - Keywords have specific meanings (And, Or, Between, etc.)
     *
     * 6. **Parameter names don't affect Spring Data JPA**:
     *    - Spring only looks at method name and parameter POSITION, not parameter NAMES
     *    - findByDueDateBefore(LocalDateTime x) same as findByDueDateBefore(LocalDateTime dateTimeToCompare)
     *    - Use descriptive parameter names for code readability, but Spring maps by position
     *    - Example: In findByAppUserAndStatus(AppUser a, TaskStatus b):
     *      - 1st param maps to "appUser" field (because method says "AppUser")
     *      - 2nd param maps to "status" field (because method says "Status")
     */
}
