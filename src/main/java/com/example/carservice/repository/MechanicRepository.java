package com.example.carservice.repository;

import com.example.carservice.domain.Mechanic;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistent access to mechanics by identifier.
 *
 * <p>Lookups return {@link java.util.Optional#empty()} when a mechanic is missing.</p>
 */
public interface MechanicRepository extends JpaRepository<Mechanic, UUID> {
}
