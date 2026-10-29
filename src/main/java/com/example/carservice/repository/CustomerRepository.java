package com.example.carservice.repository;

import com.example.carservice.domain.Customer;
import java.util.List;
import java.util.Optional;

/**
 * Stores customers by identifier.
 *
 * <p>Saving a customer with an existing identifier replaces the stored customer. Lookup returns
 * {@link Optional#empty()} when the identifier is not present and never throws merely because a
 * customer is missing.</p>
 */
public interface CustomerRepository {
    Customer save(Customer customer);

    Optional<Customer> findById(String id);

    List<Customer> findAll();
}
