package com.example.carservice.repository;

import com.example.carservice.domain.Mechanic;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Provides persistent access to mechanics by identifier.
 *
 * <p>Lookup returns {@link Optional#empty()} when the mechanic is missing.</p>
 */
public interface MechanicRepository extends Repository<Mechanic, String> {
    Mechanic save(Mechanic mechanic);

    Optional<Mechanic> findById(String id);
}
