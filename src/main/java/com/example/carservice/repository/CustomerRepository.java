package com.example.carservice.repository;

import com.example.carservice.domain.Customer;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistent access to customers.
 *
 * <p>Lookups return {@link java.util.Optional#empty()} when a customer is missing.</p>
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}
