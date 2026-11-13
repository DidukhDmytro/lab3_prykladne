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
import java.time.Clock;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class RepairOrderService {
    private RepairOrderRepository repairOrderRepository;
    private CarRepository carRepository;
    private MechanicRepository mechanicRepository;
    private PricingPolicy pricingPolicy;
    private GiftPolicy giftPolicy;
    private Clock clock;


    public RepairOrderService(
            RepairOrderRepository repairOrderRepository,
            CarRepository carRepository,
            MechanicRepository mechanicRepository,
            PricingPolicy pricingPolicy,
            GiftPolicy giftPolicy,
            Clock clock) {
        if (repairOrderRepository == null
                || carRepository == null
                || mechanicRepository == null
                || pricingPolicy == null
                || giftPolicy == null
                || clock == null) {
            throw new InvalidArgumentException(ServiceMessages.serviceDependenciesRequired);
        }
        this.repairOrderRepository = repairOrderRepository;
        this.carRepository = carRepository;
        this.mechanicRepository = mechanicRepository;
        this.pricingPolicy = pricingPolicy;
        this.giftPolicy = giftPolicy;
        this.clock = clock;
    }


    public RepairOrder createOrder(String orderId, String vin) {
        requireIdentifier(orderId);
        requireIdentifier(vin);
        if (repairOrderRepository.findById(orderId).isPresent()) {
            throw new EntityAlreadyExistsException(
                    String.format(ServiceMessages.duplicateRepairOrder, orderId));
        }
        Car car = carRepository.findByVin(vin)
                .orElseThrow(() -> new EntityNotFoundException(String.format(ServiceMessages.carNotFound, vin)));

        RepairOrder repairOrder = new RepairOrder(orderId, car);

        return repairOrderRepository.save(repairOrder);
    }


    @Transactional(readOnly = true)
    public RepairOrder getOrder(String orderId) {
        return getOrThrow(orderId);
    }


    @Transactional(readOnly = true)
    public Optional<RepairOrder> findOrder(String orderId) {
        requireIdentifier(orderId);
        return repairOrderRepository.findById(orderId);
    }


    public RepairOrder addWork(String orderId, ServiceWork work) {
        requireIdentifier(orderId);
        if (work == null) {
            throw new InvalidArgumentException(ServiceMessages.entityRequired);
        }
        RepairOrder order = getOrThrow(orderId);
        order.addWork(work);
        return repairOrderRepository.save(order);
    }


    public RepairOrder addWorks(String orderId, List<ServiceWork> works) {
        requireIdentifier(orderId);
        if (works == null) {
            throw new InvalidArgumentException(ServiceMessages.workListRequired);
        }
        if (works.isEmpty()) {
            throw new InvalidArgumentException(ServiceMessages.workListMustNotBeEmpty);
        }
        RepairOrder order = getOrThrow(orderId);
        // Keep this operation inline: calling addWork through this instance bypasses the transactional proxy.
        works.forEach(order::addWork);
        return repairOrderRepository.save(order);
    }


    public RepairOrder changePriority(String orderId, OrderPriority priority) {
        if (priority == null) {
            throw new InvalidArgumentException(ServiceMessages.servicePriorityRequired);
        }
        RepairOrder order = getOrThrow(orderId);
        order.changePriority(priority);
        return repairOrderRepository.save(order);
    }


    public RepairOrder diagnose(String orderId) {
        RepairOrder order = getOrThrow(orderId);
        order.diagnose();
        return repairOrderRepository.save(order);
    }


    public RepairOrder approve(String orderId) {
        RepairOrder order = getOrThrow(orderId);
        order.approve();
        return repairOrderRepository.save(order);
    }


    public RepairOrder assignMechanic(String orderId, String mechanicId) {
        requireIdentifier(mechanicId);
        RepairOrder order = getOrThrow(orderId);
        Mechanic mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.mechanicNotFound, mechanicId)));
        order.assignMechanic(mechanic);
        return repairOrderRepository.save(order);
    }


    public RepairOrder startProgress(String orderId) {
        RepairOrder order = getOrThrow(orderId);
        order.startProgress();
        return repairOrderRepository.save(order);
    }


    public RepairOrder completeWork(String orderId, int workIndex) {
        RepairOrder order = getOrThrow(orderId);
        order.completeWork(workIndex);
        return repairOrderRepository.save(order);
    }


    public RepairOrder complete(String orderId) {
        RepairOrder order = getOrThrow(orderId);
        order.ensureCanComplete();
        BigDecimal finalPrice = pricingPolicy.calculateFinalPrice(order);
        Instant completedAt = Instant.now(clock);
        order.complete(finalPrice, completedAt);
        return repairOrderRepository.save(order);
    }


    public RepairOrder cancel(String orderId) {
        RepairOrder order = getOrThrow(orderId);
        order.cancel();
        return repairOrderRepository.save(order);
    }


    @Transactional(readOnly = true)
    public List<RepairOrder> findByStatus(OrderStatus status) {
        if (status == null) {
            throw new InvalidArgumentException(ServiceMessages.serviceStatusRequired);
        }
        return repairOrderRepository.findByStatus(status);
    }


    @Transactional(readOnly = true)
    public BigDecimal calculateBasePrice(String orderId) {
        return pricingPolicy.calculateBasePrice(getOrThrow(orderId));
    }


    @Transactional(readOnly = true)
    public BigDecimal calculateFinalPrice(String orderId) {
        return pricingPolicy.calculateFinalPrice(getOrThrow(orderId));
    }


    @Transactional(readOnly = true)
    public Present createPresent(String orderId) {
        return giftPolicy.createPresent(getOrThrow(orderId));
    }

    private RepairOrder getOrThrow(String orderId) {
        requireIdentifier(orderId);
        Optional<RepairOrder> order = repairOrderRepository.findById(orderId);
        return order.orElseThrow(() -> new EntityNotFoundException(
                String.format(ServiceMessages.repairOrderNotFound, orderId)));
    }

    private void requireIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidArgumentException(ServiceMessages.serviceIdentifierRequired);
        }
    }
}
