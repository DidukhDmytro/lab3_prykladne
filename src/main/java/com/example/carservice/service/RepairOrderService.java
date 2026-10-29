package com.example.carservice.service;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.OrderStatus;
import com.example.carservice.domain.OrderPriority;
import com.example.carservice.domain.Present;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.ServiceMessages;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.repository.CarRepository;
import com.example.carservice.repository.MechanicRepository;
import com.example.carservice.repository.RepairOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates repair-order operations with repositories and pricing and gift policies.
 * Null identifiers and required object arguments are rejected with {@link InvalidArgumentException}.
 */
public class RepairOrderService {
    private RepairOrderRepository repairOrderRepository;
    private CarRepository carRepository;
    private MechanicRepository mechanicRepository;
    private PricingPolicy pricingPolicy;
    private GiftPolicy giftPolicy;

    /**
     * Creates a service using the supplied dependencies.
     * @throws InvalidArgumentException if any dependency is null
     */
    public RepairOrderService(
            RepairOrderRepository repairOrderRepository,
            CarRepository carRepository,
            MechanicRepository mechanicRepository,
            PricingPolicy pricingPolicy,
            GiftPolicy giftPolicy) {
        if (repairOrderRepository == null
                || carRepository == null
                || mechanicRepository == null
                || pricingPolicy == null
                || giftPolicy == null) {
            throw new InvalidArgumentException(ServiceMessages.serviceDependenciesRequired());
        }
        this.repairOrderRepository = repairOrderRepository;
        this.carRepository = carRepository;
        this.mechanicRepository = mechanicRepository;
        this.pricingPolicy = pricingPolicy;
        this.giftPolicy = giftPolicy;
    }

    /**
     * Creates and stores an order for a registered car.
     * @throws EntityAlreadyExistsException if the order id exists; {@link EntityNotFoundException} if the car is missing;
     *         {@link InvalidArgumentException} if an id or VIN is null or the order id is blank
     */
    public RepairOrder createOrder(String orderId, String vin) {
        if (repairOrderRepository.findById(orderId).isPresent()) {
            throw new EntityAlreadyExistsException(
                    String.format(ServiceMessages.duplicateRepairOrder(), orderId));
        }
        Car car = carRepository.findByVin(vin)
                .orElseThrow(() -> new EntityNotFoundException(String.format(ServiceMessages.carNotFound(), vin)));
        return repairOrderRepository.save(new RepairOrder(orderId, car));
    }

    /**
     * Returns the order with the given id.
     * @throws EntityNotFoundException if the order is missing; {@link InvalidArgumentException} if the id is null
     */
    public RepairOrder getOrder(String orderId) {
        return getOrThrow(orderId);
    }

    /**
     * Adds work to an order and saves it.
     * @throws EntityNotFoundException if the order is missing; domain exceptions if work is null or the order cannot be changed
     */
    public RepairOrder addWork(String orderId, ServiceWork work) {
        RepairOrder order = getOrThrow(orderId);
        order.addWork(work);
        return repairOrderRepository.save(order);
    }

    /**
     * Changes an order's priority and saves it.
     * @throws EntityNotFoundException if the order is missing; domain exceptions if priority is null or the order cannot be changed
     */
    public RepairOrder changePriority(String orderId, OrderPriority priority) {
        RepairOrder order = getOrThrow(orderId);
        order.changePriority(priority);
        return repairOrderRepository.save(order);
    }

    /**
     * Moves an order from created to diagnosed and saves it.
     * @throws EntityNotFoundException if the order is missing;
     *         com.example.carservice.exception.InvalidStateTransitionException if its status is not created
     */
    public RepairOrder diagnose(String orderId) {
        RepairOrder order = getOrThrow(orderId);
        order.diagnose();
        return repairOrderRepository.save(order);
    }

    /**
     * Approves a diagnosed order with work and saves it.
     * @throws EntityNotFoundException if the order is missing;
     *         com.example.carservice.exception.InvalidStateTransitionException if it cannot be approved
     */
    public RepairOrder approve(String orderId) {
        RepairOrder order = getOrThrow(orderId);
        order.approve();
        return repairOrderRepository.save(order);
    }

    /**
     * Assigns a mechanic to an order and saves it.
     * @throws EntityNotFoundException if the order or mechanic is missing; domain exceptions if assignment is invalid
     */
    public RepairOrder assignMechanic(String orderId, String mechanicId) {
        RepairOrder order = getOrThrow(orderId);
        Mechanic mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.mechanicNotFound(), mechanicId)));
        order.assignMechanic(mechanic);
        return repairOrderRepository.save(order);
    }

    /**
     * Starts work on an approved order and saves it.
     * @throws EntityNotFoundException if the order is missing;
     *         com.example.carservice.exception.InvalidStateTransitionException if it cannot start
     */
    public RepairOrder startProgress(String orderId) {
        RepairOrder order = getOrThrow(orderId);
        order.startProgress();
        return repairOrderRepository.save(order);
    }

    /**
     * Completes one work item by index and saves the order.
     * @throws EntityNotFoundException if the order is missing; domain exceptions if the index or order state is invalid
     */
    public RepairOrder completeWork(String orderId, int workIndex) {
        RepairOrder order = getOrThrow(orderId);
        order.completeWork(workIndex);
        return repairOrderRepository.save(order);
    }

    /**
     * Completes an order and saves it.
     * @throws EntityNotFoundException if the order is missing;
     *         com.example.carservice.exception.InvalidStateTransitionException if work is unfinished or the state is invalid
     */
    public RepairOrder complete(String orderId) {
        RepairOrder order = getOrThrow(orderId);
        order.complete();
        return repairOrderRepository.save(order);
    }

    /**
     * Cancels an order and saves it.
     * @throws EntityNotFoundException if the order is missing;
     *         com.example.carservice.exception.InvalidStateTransitionException if its state cannot be cancelled
     */
    public RepairOrder cancel(String orderId) {
        RepairOrder order = getOrThrow(orderId);
        order.cancel();
        return repairOrderRepository.save(order);
    }

    /**
     * Returns orders with the given status.
     * @throws InvalidArgumentException if status is null
     */
    public List<RepairOrder> findByStatus(OrderStatus status) {
        return repairOrderRepository.findByStatus(status);
    }

    /**
     * Calculates the base price for an order.
     * @throws EntityNotFoundException if the order is missing;
     *         com.example.carservice.pricing.PricingException if pricing inputs are invalid
     */
    public BigDecimal calculateBasePrice(String orderId) {
        return pricingPolicy.calculateBasePrice(getOrThrow(orderId));
    }

    /**
     * Calculates the final price for an order.
     * @throws EntityNotFoundException if the order is missing;
     *         com.example.carservice.pricing.PricingException if pricing inputs are invalid
     */
    public BigDecimal calculateFinalPrice(String orderId) {
        return pricingPolicy.calculateFinalPrice(getOrThrow(orderId));
    }

    /**
     * Creates a gift present for an order.
     * @throws EntityNotFoundException if the order is missing
     */
    public Present createPresent(String orderId) {
        return giftPolicy.createPresent(getOrThrow(orderId));
    }

    private RepairOrder getOrThrow(String orderId) {
        Optional<RepairOrder> order = repairOrderRepository.findById(orderId);
        return order.orElseThrow(() -> new EntityNotFoundException(
                String.format(ServiceMessages.repairOrderNotFound(), orderId)));
    }
}
