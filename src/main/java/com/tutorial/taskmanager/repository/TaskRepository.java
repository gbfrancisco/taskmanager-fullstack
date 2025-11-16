package com.tutorial.taskmanager.repository;

import com.tutorial.taskmanager.enums.TaskStatus;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.model.Project;
import com.tutorial.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    // ==================== FIND BY RELATIONSHIP ====================

    /**
     * TODO: Add a method to find all tasks assigned to a specific user.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Method name pattern: findBy{RelationshipField}</li>
     *   <li>The Task entity has a field: <code>@ManyToOne AppUser user</code></li>
     *   <li>Return type: List&lt;Task&gt; (multiple tasks per user)</li>
     *   <li>Parameter type: AppUser (pass the entire user object)</li>
     * </ul>
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;Task&gt; findByUser(AppUser user);
     * </pre>
     *
     * <p><b>Generated SQL:</b>
     * <pre>
     * SELECT * FROM tasks WHERE user_id = ?
     * </pre>
     */
    // TODO: Add findByUser method here

    /**
     * TODO: Add a method to find all tasks belonging to a specific project.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Similar to findByUser</li>
     *   <li>Task has field: <code>@ManyToOne Project project</code></li>
     *   <li>Return type: List&lt;Task&gt;</li>
     *   <li>Parameter type: Project</li>
     * </ul>
     */
    // TODO: Add findByProject method here

    // ==================== FIND BY STATUS ====================

    /**
     * TODO: Add a method to find all tasks with a specific status.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Task has field: <code>TaskStatus status</code></li>
     *   <li>Return type: List&lt;Task&gt;</li>
     *   <li>Parameter type: TaskStatus (the enum)</li>
     * </ul>
     *
     * <p><b>Use case:</b> Get all TODO tasks, or all COMPLETED tasks
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;Task&gt; findByStatus(TaskStatus status);
     * </pre>
     */
    // TODO: Add findByStatus method here

    // ==================== COMBINED FILTERS ====================

    /**
     * TODO: Add a method to find tasks by both user AND status.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Method name pattern: findBy{Field1}And{Field2}</li>
     *   <li>Spring combines conditions with AND in SQL</li>
     *   <li>Return type: List&lt;Task&gt;</li>
     *   <li>Parameters: AppUser user, TaskStatus status (order matters!)</li>
     * </ul>
     *
     * <p><b>Use case:</b> Get all TODO tasks for a specific user
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;Task&gt; findByUserAndStatus(AppUser user, TaskStatus status);
     * </pre>
     *
     * <p><b>Generated SQL:</b>
     * <pre>
     * SELECT * FROM tasks WHERE user_id = ? AND status = ?
     * </pre>
     */
    // TODO: Add findByUserAndStatus method here

    /**
     * TODO: Add a method to find tasks by both project AND status.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Similar to findByUserAndStatus</li>
     *   <li>Combine Project and TaskStatus filters</li>
     * </ul>
     *
     * <p><b>Use case:</b> Get all IN_PROGRESS tasks for a specific project
     */
    // TODO: Add findByProjectAndStatus method here

    // ==================== DATE-BASED QUERIES ====================

    /**
     * TODO: Add a method to find tasks due before a specific date.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Keyword: <code>Before</code> (e.g., findByDueDateBefore)</li>
     *   <li>Task has field: <code>LocalDate dueDate</code></li>
     *   <li>Return type: List&lt;Task&gt;</li>
     *   <li>Parameter type: LocalDate</li>
     * </ul>
     *
     * <p><b>Use case:</b> Find overdue tasks
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;Task&gt; findByDueDateBefore(LocalDate date);
     * </pre>
     *
     * <p><b>Generated SQL:</b>
     * <pre>
     * SELECT * FROM tasks WHERE due_date &lt; ?
     * </pre>
     */
    // TODO: Add findByDueDateBefore method here

    /**
     * TODO: Add a method to find tasks due between two dates.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Keyword: <code>Between</code> (e.g., findByDueDateBetween)</li>
     *   <li>Parameters: LocalDate start, LocalDate end (inclusive range)</li>
     *   <li>Return type: List&lt;Task&gt;</li>
     * </ul>
     *
     * <p><b>Use case:</b> Find tasks due this week or this month
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;Task&gt; findByDueDateBetween(LocalDate start, LocalDate end);
     * </pre>
     *
     * <p><b>Generated SQL:</b>
     * <pre>
     * SELECT * FROM tasks WHERE due_date BETWEEN ? AND ?
     * </pre>
     */
    // TODO: Add findByDueDateBetween method here

    // ==================== ADVANCED COMBINED QUERIES ====================

    /**
     * TODO (CHALLENGE): Add a method to find overdue tasks for a specific user
     * that are not completed or cancelled.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Combine: findByUserAndDueDateBeforeAndStatusNot</li>
     *   <li>Keywords: And, Before, Not</li>
     *   <li>Parameters: AppUser user, LocalDate date, TaskStatus status</li>
     *   <li>You'll call this with status = COMPLETED to exclude completed tasks</li>
     * </ul>
     *
     * <p><b>Alternative approach:</b>
     * You could also use <code>StatusNotIn</code> with a collection of statuses:
     * <pre>
     * findByUserAndDueDateBeforeAndStatusNotIn(
     *     AppUser user,
     *     LocalDate date,
     *     Collection&lt;TaskStatus&gt; statuses
     * )
     * </pre>
     *
     * <p><b>Try implementing the simpler version first!</b>
     */
    // TODO: Add method to find overdue incomplete tasks here

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
