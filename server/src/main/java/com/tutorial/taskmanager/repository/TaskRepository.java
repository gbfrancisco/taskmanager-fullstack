package com.tutorial.taskmanager.repository;

import com.tutorial.taskmanager.enums.TaskStatus;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.model.Project;
import com.tutorial.taskmanager.model.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Task} entity.
 *
 * <p>This repository handles task-related database operations with support for
 * filtering by user, project, status, and due dates.
 *
 * <p><b>Relationship Queries:</b>
 * When querying by related entities (AppUser, Project), Spring Data JPA can navigate
 * relationships using method names. For example:
 * <ul>
 *   <li><code>findByUser</code> - uses the @ManyToOne relationship field "user"</li>
 *   <li><code>findByProject</code> - uses the @ManyToOne relationship field "project"</li>
 *   <li><code>findByUser_Username</code> - navigates to user, then accesses username field</li>
 * </ul>
 *
 * @author Task Manager Tutorial
 * @see Task
 * @see JpaRepository
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // ==================== FIND BY APP USER ====================

    List<Task> findByAppUser(AppUser appUser);

    List<Task> findByAppUserId(Long appUserId);

    // ==================== FIND BY PROJECT ====================

    List<Task> findByProject(Project project);

    List<Task> findByProjectId(Long projectId);

    // ==================== COUNT BY PROJECT ====================

    /**
     * Count tasks for a single project.
     * Generates: SELECT COUNT(*) FROM task WHERE project_id = ?
     */
    long countByProjectId(Long projectId);

    /**
     * Batch count tasks for multiple projects.
     * Returns a list of [projectId, count] pairs.
     * Avoids N+1 queries when fetching task counts for many projects.
     *
     * Usage:
     * List<Object[]> counts = taskRepository.countByProjectIds(projectIds);
     * // counts = [[1L, 5L], [2L, 3L], ...] where first element is projectId, second is count
     */
    @Query("SELECT t.project.id, COUNT(t) FROM Task t WHERE t.project.id IN :projectIds GROUP BY t.project.id")
    List<Object[]> countByProjectIds(@Param("projectIds") List<Long> projectIds);

    // ==================== FIND BY STATUS ====================

    List<Task> findByStatus(TaskStatus status);

    // ==================== COMBINED FILTERS ====================

    List<Task> findByAppUserAndStatus(AppUser appUser, TaskStatus status);

    List<Task> findByAppUserIdAndStatus(Long appUserId, TaskStatus status);

    List<Task> findByProjectAndStatus(Project project, TaskStatus status);

    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

    // ==================== DATE-BASED QUERIES ====================

    List<Task> findByDueDateBefore(LocalDateTime dateTimeToCompare);

    List<Task> findByDueDateBetween(LocalDateTime start, LocalDateTime end);

    // ==================== ADVANCED COMBINED QUERIES ====================

    List<Task> findByAppUserAndDueDateBeforeAndStatusNotIn(
        AppUser appUser,
        LocalDateTime dateTimeToCompare,
        Collection<TaskStatus> excludedStatuses
    );

    List<Task> findByAppUserIdAndDueDateBeforeAndStatusNotIn(
        Long appUserId,
        LocalDateTime dateTimeToCompare,
        Collection<TaskStatus> excludedStatuses
    );

    List<Task> findByDueDateBeforeAndStatusNotIn(
        LocalDateTime dateTimeToCompare,
        Collection<TaskStatus> excludedStatuses
    );

    // ==================== ENTITY GRAPH METHODS ====================
    // These methods fetch related entities (appUser, project) in a single query
    // to avoid N+1 problems when mapping to DTOs with embedded relationships.
    // See docs/10-jpa-entity-graph.md for details.
    //
    // NOTE: Methods without query criteria (like findAll) need @Query because
    // Spring Data can't derive a query from names like "findAllWithAppUserAndProject".
    // Methods WITH criteria (like findByStatus) work because Spring Data
    // understands "ByStatus" as a WHERE clause.

    /**
     * Find all tasks with appUser and project eagerly fetched.
     * Use this for list views that display user and project info.
     */
    @EntityGraph(attributePaths = {"appUser", "project"})
    @Query("SELECT t FROM Task t")
    List<Task> findAllWithAppUserAndProject();

    /**
     * Find task by ID with appUser and project eagerly fetched.
     * Use this for detail views.
     */
    @EntityGraph(attributePaths = {"appUser", "project"})
    Optional<Task> findWithAppUserAndProjectById(Long id);

    /**
     * Find tasks by user ID with relationships eagerly fetched.
     */
    @EntityGraph(attributePaths = {"appUser", "project"})
    List<Task> findWithAppUserAndProjectByAppUserId(Long appUserId);

    /**
     * Find tasks by project ID with relationships eagerly fetched.
     */
    @EntityGraph(attributePaths = {"appUser", "project"})
    List<Task> findWithAppUserAndProjectByProjectId(Long projectId);

    /**
     * Find tasks by status with relationships eagerly fetched.
     */
    @EntityGraph(attributePaths = {"appUser", "project"})
    List<Task> findWithAppUserAndProjectByStatus(TaskStatus status);

    /**
     * Find overdue tasks (before date, excluding certain statuses) with relationships.
     */
    @EntityGraph(attributePaths = {"appUser", "project"})
    List<Task> findWithAppUserAndProjectByDueDateBeforeAndStatusNotIn(
        LocalDateTime dateTimeToCompare,
        Collection<TaskStatus> excludedStatuses
    );

    // ==================== NOTES FOR LATER ====================

    /*
     * MORE QUERY METHOD KEYWORDS (for future reference):
     *
     * Comparison:
     * - LessThan, LessThanEqual, GreaterThan, GreaterThanEqual
     * - After, Before (for dates)
     *
     * String matching:
     * - Containing, StartingWith, EndingWith
     * - IgnoreCase (case-insensitive matching)
     *
     * Collections:
     * - In (e.g., findByStatusIn(Collection<TaskStatus> statuses))
     * - NotIn
     *
     * Null checks:
     * - IsNull, IsNotNull (e.g., findByDueDateIsNull)
     *
     * Sorting:
     * - OrderBy{Field}Asc, OrderBy{Field}Desc
     * - Example: findByUserOrderByDueDateAsc(AppUser user)
     *
     * For complex queries, we can use @Query annotation with JPQL or native SQL!
     */

    /*
     * KEY LEARNING POINTS:
     *
     * 1. **Navigation through relationships** - Use field names from the entity
     * 2. **Combining keywords** - And, Or, Between, etc.
     * 3. **Return types**:
     *    - List<Task> for multiple results
     *    - Optional<Task> for single result that might not exist
     *    - Task for single result (throws exception if not found)
     * 4. **Parameter order matters** - must match the method name order
     * 5. **Type safety** - parameters must match field types (LocalDate, TaskStatus, etc.)
     * 6. **Lazy loading consideration** - relationships are LAZY, may need @EntityGraph later
     */
}
