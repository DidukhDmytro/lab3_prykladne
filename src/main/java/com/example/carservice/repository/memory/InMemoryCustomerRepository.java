package com.example.carservice.repository.memory;

import com.example.carservice.domain.Customer;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.ServiceMessages;
import com.example.carservice.repository.CustomerRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCustomerRepository implements CustomerRepository {
    private Map<String, Customer> customers = new HashMap<>();

    @Override
    public Customer save(Customer customer) {
        if (customer == null) {
            throw new InvalidArgumentException(ServiceMessages.repositoryCustomerRequired());
        }
        customers.put(customer.getId(), customer);
        return customer;
    }

    @Override
    public Optional<Customer> findById(String id) {
        if (id == null) {
            throw new InvalidArgumentException(ServiceMessages.repositoryIdentifierRequired());
        }
        return Optional.ofNullable(customers.get(id));
    }

    @Override
    public List<Customer> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(customers.values()));
    }
}
