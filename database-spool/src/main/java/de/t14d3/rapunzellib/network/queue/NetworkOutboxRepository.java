package de.t14d3.rapunzellib.network.queue;

import de.t14d3.spool.core.EntityManager;
import de.t14d3.spool.repository.EntityRepository;

final class NetworkOutboxRepository extends EntityRepository<NetworkOutboxMessage> {
    NetworkOutboxRepository(EntityManager entityManager) {
        super(entityManager, NetworkOutboxMessage.class);
    }
}

