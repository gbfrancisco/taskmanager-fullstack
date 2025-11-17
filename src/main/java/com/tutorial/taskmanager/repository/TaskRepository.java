package com.tutorial.taskmanager.repository;

import com.tutorial.taskmanager.enums.TaskStatus;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.model.Project;
import com.tutorial.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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
