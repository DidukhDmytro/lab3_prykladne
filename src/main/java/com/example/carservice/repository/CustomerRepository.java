package com.example.carservice.repository;

import com.example.carservice.domain.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Provides persistent access to customers.
 *
 * <p>Lookup returns {@link Optional#empty()} when the customer is missing.</p>
 */
public interface CustomerRepository extends Repository<Customer, String> {
    Customer save(Customer customer);

    Optional<Customer> findById(String id);

    List<Customer> findAll();
}
