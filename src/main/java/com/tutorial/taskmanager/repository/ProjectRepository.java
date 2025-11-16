package com.tutorial.taskmanager.repository;

import com.tutorial.taskmanager.enums.ProjectStatus;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Project} entity.
 *
 * <p>This repository handles project-related database operations with support for
 * filtering by owner (user), status, and project name.
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li>Find all projects owned by a specific user</li>
 *   <li>Find projects by status (ACTIVE, COMPLETED, etc.)</li>
 *   <li>Search projects by name (exact or partial match)</li>
 *   <li>Combine filters (e.g., active projects for a specific owner)</li>
 * </ul>
 *
 * @author Task Manager Tutorial
 * @see Project
 * @see JpaRepository
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // ==================== FIND BY OWNER ====================

    /**
     * TODO: Add a method to find all projects owned by a specific user.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Project entity has field: <code>@ManyToOne AppUser owner</code></li>
     *   <li>Method name pattern: findBy{RelationshipField}</li>
     *   <li>Return type: List&lt;Project&gt; (one user can own multiple projects)</li>
     *   <li>Parameter type: AppUser</li>
     * </ul>
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;Project&gt; findByOwner(AppUser owner);
     * </pre>
     *
     * <p><b>Generated SQL:</b>
     * <pre>
     * SELECT * FROM projects WHERE owner_id = ?
     * </pre>
     */
    // TODO: Add findByOwner method here

    // ==================== FIND BY STATUS ====================

    /**
     * TODO: Add a method to find all projects with a specific status.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Project has field: <code>ProjectStatus status</code></li>
     *   <li>Return type: List&lt;Project&gt;</li>
     *   <li>Parameter type: ProjectStatus (the enum)</li>
     * </ul>
     *
     * <p><b>Use case:</b> Get all ACTIVE projects, or all COMPLETED projects
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;Project&gt; findByStatus(ProjectStatus status);
     * </pre>
     */
    // TODO: Add findByStatus method here

    // ==================== COMBINED FILTERS ====================

    /**
     * TODO: Add a method to find projects by both owner AND status.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Method name pattern: findBy{Field1}And{Field2}</li>
     *   <li>Combine owner and status filters</li>
     *   <li>Parameters: AppUser owner, ProjectStatus status (order matters!)</li>
     * </ul>
     *
     * <p><b>Use case:</b> Get all ACTIVE projects for a specific user
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;Project&gt; findByOwnerAndStatus(AppUser owner, ProjectStatus status);
     * </pre>
     *
     * <p><b>Generated SQL:</b>
     * <pre>
     * SELECT * FROM projects WHERE owner_id = ? AND status = ?
     * </pre>
     */
    // TODO: Add findByOwnerAndStatus method here

    // ==================== FIND BY NAME ====================

    /**
     * TODO: Add a method to find a project by exact name match.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Project has field: <code>String name</code></li>
     *   <li>Return type: Optional&lt;Project&gt; (name might be unique or used for lookups)</li>
     *   <li>Parameter type: String</li>
     * </ul>
     *
     * <p><b>Note:</b> Project names are not enforced as unique in the database,
     * but you might want to ensure uniqueness per user in your business logic.
     *
     * <p><b>Example:</b>
     * <pre>
     * Optional&lt;Project&gt; findByName(String name);
     * </pre>
     */
    // TODO: Add findByName method here

    /**
     * TODO: Add a method to search projects by partial name match (case-insensitive).
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Keywords: <code>Containing</code> and <code>IgnoreCase</code></li>
     *   <li>Method name: findByName{Keyword}{Keyword}</li>
     *   <li>Return type: List&lt;Project&gt; (multiple matches possible)</li>
     *   <li>Parameter type: String</li>
     * </ul>
     *
     * <p><b>Use case:</b> Search for projects - user types "mobile" and finds
     * "Mobile App", "Mobile Backend", "iOS Mobile Client", etc.
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;Project&gt; findByNameContainingIgnoreCase(String keyword);
     * </pre>
     *
     * <p><b>Generated SQL (approximate):</b>
     * <pre>
     * SELECT * FROM projects WHERE LOWER(name) LIKE LOWER(CONCAT('%', ?, '%'))
     * </pre>
     */
    // TODO: Add findByNameContainingIgnoreCase method here

    // ==================== ADVANCED COMBINED QUERIES ====================

    /**
     * TODO (CHALLENGE): Add a method to search projects by owner and partial name match.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Combine: findByOwnerAndNameContainingIgnoreCase</li>
     *   <li>Keywords: And, Containing, IgnoreCase</li>
     *   <li>Parameters: AppUser owner, String keyword</li>
     *   <li>Return type: List&lt;Project&gt;</li>
     * </ul>
     *
     * <p><b>Use case:</b> User searches their own projects by keyword
     */
    // TODO: Add findByOwnerAndNameContainingIgnoreCase method here

    // ==================== EXISTENCE CHECKS ====================

    /**
     * TODO: Add a method to check if a project with a specific name exists for an owner.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Method name pattern: existsBy{Field1}And{Field2}</li>
     *   <li>Return type: boolean</li>
     *   <li>Parameters: AppUser owner, String name</li>
     * </ul>
     *
     * <p><b>Use case:</b> Prevent duplicate project names for the same user
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean existsByOwnerAndName(AppUser owner, String name);
     * </pre>
     *
     * <p><b>Why check per owner?</b> Different users can have projects with the same name,
     * but you might want to enforce uniqueness per user.
     */
    // TODO: Add existsByOwnerAndName method here

    // ==================== NOTES FOR LATER ====================

    /*
     * SORTING EXAMPLES (add when needed):
     *
     * - findByOwnerOrderByNameAsc(AppUser owner)
     *   Returns projects sorted alphabetically by name
     *
     * - findByStatusOrderByCreatedTimestampDesc(ProjectStatus status)
     *   Returns projects sorted by creation date (newest first)
     *
     * - For dynamic sorting, use:
     *   findByOwner(AppUser owner, Sort sort)
     *   Call with: Sort.by(Sort.Direction.ASC, "name")
     */

    /*
     * PAGINATION EXAMPLES (add when needed):
     *
     * - Page<Project> findByOwner(AppUser owner, Pageable pageable)
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
     * @Query("SELECT p FROM Project p JOIN FETCH p.tasks WHERE p.owner = :owner")
     * List<Project> findByOwnerWithTasks(@Param("owner") AppUser owner);
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
     *    - Use existsByOwnerAndName() before creating new projects
     *
     * 5. **Query method naming is strict**:
     *    - Field names must match exactly (case-sensitive)
     *    - Order of parameters must match order in method name
     *    - Keywords have specific meanings (And, Or, Between, etc.)
     */
}
