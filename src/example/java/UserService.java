package airhacks.example;

import java.time.Instant;
import java.util.Optional;

/// User-facing service for managing accounts.
///
/// `UserService` is the boundary through which callers create, look up,
/// and deactivate [User] records. It is backed by an in-memory store in
/// this example, but the contract is written so that a database-backed
/// implementation could replace it without changing the API.
///
/// This class demonstrates the full range of JavaDoc constructs that
/// jMarkDoc renders, including standard block tags, inline code and links,
/// and the custom contract tags that feed the Agent Notes section.
///
/// @see User
/// @see UserService.NotFoundException
public class UserService {

    /// Logger used for diagnostic output.
    ///
    /// Shared across all instances; never reassigned after class
    /// initialization.
    private static final System.Logger LOGGER =
            System.getLogger(UserService.class.getName());

    /// Maximum number of accounts a single service instance will hold.
    ///
    /// Exposed as a constant so callers can size their batches accordingly.
    public static final int MAX_ACCOUNTS = 10_000;

    /// Backing store mapping a user id to the stored [User].
    private final java.util.Map<Long, User> store;

    /// Creates a service with an empty, mutable account store.
    ///
    /// The resulting service starts with no users and is ready to accept
    /// [#create(String)] calls immediately.
    ///
    /// @threadsafety The constructor is safe to call from any thread; each
    ///               instance owns an independent store.
    public UserService() {
        this(new java.util.concurrent.ConcurrentHashMap<>());
    }

    /// Creates a service backed by the supplied store.
    ///
    /// This constructor exists primarily for testing, allowing a
    /// pre-populated or instrumented map to be injected.
    ///
    /// @param store the backing map from user id to [User]; must not be `null`
    /// @precondition `store` must not be `null`.
    /// @postcondition The service holds a reference to the supplied store; no
    ///                defensive copy is made.
    public UserService(java.util.Map<Long, User> store) {
        this.store = java.util.Objects.requireNonNull(store, "store");
    }

    /// Looks up a user by id.
    ///
    /// The lookup is a constant-time read against the backing store and does
    /// not mutate any state.
    ///
    /// @param id the user identifier; expected to be a positive value
    /// @return the matching [User], never `null`
    /// @throws NotFoundException when no user exists for the supplied id
    /// @see #findOptional(long)
    /// @precondition `id` must be positive.
    /// @postcondition The returned user reflects the latest stored state for `id`.
    /// @sideeffect None; this is a pure read.
    /// @idempotency Repeated calls with the same id return an equal result while
    ///              the store is unchanged.
    /// @threadsafety Safe for concurrent use; reads observe a consistent
    ///               snapshot of the store.
    /// @errorhandling Signals absence by throwing [NotFoundException]
    ///                rather than returning `null`.
    public User findById(long id) {
        User user = store.get(id);
        if (user == null) {
            throw new NotFoundException(id);
        }
        return user;
    }

    /// Looks up a user by id without throwing when absent.
    ///
    /// @param id the user identifier
    /// @return an [Optional] containing the user, or empty when none exists
    /// @see #findById(long)
    /// @sideeffect None; this is a pure read.
    /// @threadsafety Safe for concurrent use.
    public Optional<User> findOptional(long id) {
        return Optional.ofNullable(store.get(id));
    }

    /// Creates and stores a new active user.
    ///
    /// A fresh identifier is allocated and the user is recorded with the
    /// current timestamp as its creation instant.
    ///
    /// @param name the display name for the new account; must be non-blank
    /// @return the newly created [User]
    /// @throws IllegalArgumentException when `name` is blank
    /// @throws IllegalStateException when the store already holds
    ///         [#MAX_ACCOUNTS] users
    /// @precondition `name` must be non-null and non-blank.
    /// @postcondition The returned user is present in the store and is active.
    /// @sideeffect Inserts a new entry into the backing store.
    /// @idempotency Not idempotent; each call allocates a new id and entry.
    /// @authorization Caller must hold the `accounts:write` permission.
    /// @transactions Executes as a single atomic put against the store.
    /// @concurrency Concurrent creates are serialized by the backing map.
    /// @errorhandling Validates input before mutating state, so a rejected
    ///                request leaves the store unchanged.
    /// @requirement R1.1 create and store a new active account
    public User create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be non-blank");
        }
        if (store.size() >= MAX_ACCOUNTS) {
            throw new IllegalStateException("account limit reached");
        }
        long id = store.size() + 1L;
        User user = new User(id, name, true, Instant.now());
        store.put(id, user);
        LOGGER.log(System.Logger.Level.INFO, "created user {0}", id);
        return user;
    }

    /// Deactivates the user with the given id.
    ///
    /// @param id the identifier of the user to deactivate
    /// @throws NotFoundException when no user exists for the supplied id
    /// @precondition A user with `id` must exist.
    /// @postcondition The stored user for `id` is marked inactive.
    /// @sideeffect Replaces the stored user entry with an inactive copy.
    /// @idempotency Idempotent; deactivating an already-inactive user has no
    ///              additional effect.
    /// @authorization Caller must hold the `accounts:write` permission.
    /// @requirement R2.1 deactivate an existing account
    public void deactivate(long id) {
        User existing = findById(id);
        store.put(id, existing.deactivated());
    }

    /// Removes all users from the store.
    ///
    /// @deprecated Bulk deletion bypasses per-account auditing. Use
    ///             [#deactivate(long)] per account instead.
    @Deprecated
    public void purge() {
        store.clear();
    }

    /// An account record managed by [UserService].
    ///
    /// Records are immutable; state transitions such as deactivation produce
    /// a new instance via [#deactivated()].
    ///
    /// @param id the unique account identifier
    /// @param name the display name of the account holder
    /// @param active whether the account may currently authenticate
    /// @param createdAt the instant at which the account was created
    public record User(long id, String name, boolean active, Instant createdAt) {

        /// Validates record components on construction.
        ///
        /// @throws IllegalArgumentException when `name` is blank
        /// @precondition `name` must be non-blank.
        public User {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name must be non-blank");
            }
        }

        /// Returns a copy of this user marked inactive.
        ///
        /// @return an inactive [User] with all other fields preserved
        /// @sideeffect None; returns a new instance and leaves this one unchanged.
        /// @idempotency Idempotent; calling on an inactive user yields an equal
        ///              user.
        public User deactivated() {
            return new User(id, name, false, createdAt);
        }
    }

    /// Thrown when a requested user cannot be found.
    ///
    /// @see UserService#findById(long)
    public static final class NotFoundException extends RuntimeException {

        /// Serialization version identifier for this exception type.
        private static final long serialVersionUID = 1L;

        /// The id that could not be resolved.
        private final long id;

        /// Creates the exception for a missing user id.
        ///
        /// @param id the identifier that was not found
        public NotFoundException(long id) {
            super("no user for id " + id);
            this.id = id;
        }

        /// Returns the unresolved user id.
        ///
        /// @return the id that was not found
        public long id() {
            return id;
        }
    }
}
