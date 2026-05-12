package de.t14d3.rapunzellib.network.queue;

import de.t14d3.spool.core.EntityManager;
import de.t14d3.spool.repository.EntityRepository;

/**
 * Repository for persisting and querying {@link NetworkOutboxMessage} entities.
 * <p>
 * Provides standard CRUD operations inherited from {@link EntityRepository},
 * backed by the spool ORM's {@link EntityManager}.
 * </p>
 */
final class NetworkOutboxRepository extends EntityRepository<NetworkOutboxMessage> {
    /**
     * Constructs a new repository for outbox messages.
     *
     * @param entityManager the spool entity manager
     */
    NetworkOutboxRepository(EntityManager entityManager) {
        super(entityManager, NetworkOutboxMessage.class);
    }
}
