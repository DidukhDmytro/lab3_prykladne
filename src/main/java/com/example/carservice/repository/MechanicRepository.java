package com.example.carservice.repository;

import com.example.carservice.domain.Mechanic;
import java.util.Optional;

/**
 * Stores mechanics by identifier.
 *
 * <p>Saving a mechanic with an existing identifier replaces the stored mechanic. Lookup returns
 * {@link Optional#empty()} when the identifier is not present and never throws merely because a
 * mechanic is missing.</p>
 */
public interface MechanicRepository {
    Mechanic save(Mechanic mechanic);

    Optional<Mechanic> findById(String id);
}
