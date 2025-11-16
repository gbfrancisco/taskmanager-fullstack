package com.tutorial.taskmanager.repository;

import com.tutorial.taskmanager.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AppUser} entity.
 *
 * <p>This interface extends {@link JpaRepository} which provides:
 * <ul>
 *   <li>CRUD operations (save, findById, findAll, delete, etc.)</li>
 *   <li>Pagination and sorting support</li>
 *   <li>Batch operations (saveAll, deleteAll)</li>
 *   <li>Query derivation from method names</li>
 * </ul>
 *
 * <p><b>How Spring Data JPA Works:</b>
 * <ol>
 *   <li>Spring scans for interfaces extending Repository (or JpaRepository)</li>
 *   <li>At runtime, Spring creates a proxy implementation of this interface</li>
 *   <li>Method names are parsed to generate queries automatically</li>
 *   <li>No implementation code needed - Spring handles it!</li>
 * </ol>
 *
 * <p><b>Naming Conventions for Query Methods:</b>
 * <ul>
 *   <li><code>findBy{FieldName}</code> - finds entities by a field (e.g., findByUsername)</li>
 *   <li><code>existsBy{FieldName}</code> - checks if entity exists by a field</li>
 *   <li><code>countBy{FieldName}</code> - counts entities by a field</li>
 *   <li><code>deleteBy{FieldName}</code> - deletes entities by a field</li>
 * </ul>
 *
 * @author Task Manager Tutorial
 * @see AppUser
 * @see JpaRepository
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // ==================== BASIC QUERY METHODS ====================

    /**
     * TODO: Add a method to find a user by username.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Method name pattern: findBy{FieldName}</li>
     *   <li>Username is unique, so return Optional&lt;AppUser&gt;</li>
     *   <li>Spring will generate: SELECT * FROM app_users WHERE username = ?</li>
     * </ul>
     *
     * <p><b>Example:</b>
     * <pre>
     * Optional&lt;AppUser&gt; findByUsername(String username);
     * </pre>
     *
     * <p><b>Why Optional?</b> The user might not exist, Optional handles null safely.
     */
    // TODO: Add findByUsername method here

    /**
     * TODO: Add a method to find a user by email.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Similar to findByUsername</li>
     *   <li>Email is also unique</li>
     *   <li>Return type: Optional&lt;AppUser&gt;</li>
     * </ul>
     */
    // TODO: Add findByEmail method here

    // ==================== EXISTENCE CHECK METHODS ====================

    /**
     * TODO: Add a method to check if a username already exists.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Method name pattern: existsBy{FieldName}</li>
     *   <li>Return type: boolean (true if exists, false otherwise)</li>
     *   <li>More efficient than findByUsername().isPresent()</li>
     *   <li>Spring generates: SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM...</li>
     * </ul>
     *
     * <p><b>Use case:</b> Validate username uniqueness before registration
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean existsByUsername(String username);
     * </pre>
     */
    // TODO: Add existsByUsername method here

    /**
     * TODO: Add a method to check if an email already exists.
     *
     * <p><b>Hints:</b>
     * <ul>
     *   <li>Similar to existsByUsername</li>
     *   <li>Return type: boolean</li>
     * </ul>
     *
     * <p><b>Use case:</b> Validate email uniqueness before registration
     */
    // TODO: Add existsByEmail method here

    // ==================== NOTES FOR LATER ====================

    /*
     * ADVANCED QUERY METHODS (we'll add these when needed):
     *
     * - findByUsernameContainingIgnoreCase(String keyword) - search users by partial username
     * - findByEmailContaining(String domain) - find users by email domain
     * - @Query annotation for custom JPQL queries
     * - @Query with native SQL for complex queries
     * - Pagination: findAll(Pageable pageable)
     * - Sorting: findAll(Sort sort)
     *
     * For now, focus on the basic methods above!
     */

    /*
     * KEY LEARNING POINTS:
     *
     * 1. **No @Repository needed** (but we use it for clarity and exception translation)
     * 2. **No implementation required** - Spring generates it at runtime
     * 3. **Type safety** - JpaRepository<AppUser, Long> means:
     *    - Entity type: AppUser
     *    - ID type: Long
     * 4. **Inherited methods** from JpaRepository (you get these for free):
     *    - save(AppUser user) - insert or update
     *    - findById(Long id) - find by primary key
     *    - findAll() - get all users
     *    - delete(AppUser user) - delete entity
     *    - count() - count all users
     *    - And many more!
     * 5. **Query derivation** - Spring parses method names to generate queries
     * 6. **Optional return types** - handle non-existent results safely
     */
}
