package com.example.carservice.service;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumerate.OrderStatus;
import com.example.carservice.domain.enumerate.OrderPriority;
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
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class RepairOrderService {
    private final RepairOrderRepository repairOrderRepository;
    private final CarRepository carRepository;
    private final MechanicRepository mechanicRepository;
    private final PricingPolicy pricingPolicy;
    private final GiftPolicy giftPolicy;
    private final Clock clock;


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
            throw new InvalidArgumentException(ServiceMessages.SERVICE_DEPENDENCIES_REQUIRED);
        }
        this.repairOrderRepository = repairOrderRepository;
        this.carRepository = carRepository;
        this.mechanicRepository = mechanicRepository;
        this.pricingPolicy = pricingPolicy;
        this.giftPolicy = giftPolicy;
        this.clock = clock;
    }


    public RepairOrder createOrder(UUID orderId, String vin) {
        requireIdentifier(orderId);
        requireIdentifier(vin);
        if (repairOrderRepository.findById(orderId).isPresent()) {
            throw new EntityAlreadyExistsException(
                    String.format(ServiceMessages.DUPLICATE_REPAIR_ORDER, orderId));
        }
        Car car = carRepository.findByVin(vin)
                .orElseThrow(() -> new EntityNotFoundException(String.format(ServiceMessages.CAR_NOT_FOUND, vin)));

        RepairOrder repairOrder = new RepairOrder(orderId, car);

        return repairOrderRepository.save(repairOrder);
    }


    @Transactional(readOnly = true)
    public Optional<RepairOrder> findOrder(UUID orderId) {
        requireIdentifier(orderId);
        return repairOrderRepository.findById(orderId);
    }


    public RepairOrder addWork(UUID orderId, ServiceWork work) {
        requireIdentifier(orderId);
        if (work == null) {
            throw new InvalidArgumentException(ServiceMessages.ENTITY_REQUIRED);
        }
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        order.addWork(work);
        return repairOrderRepository.save(order);
    }


    public RepairOrder addWorks(UUID orderId, List<ServiceWork> works) {
        requireIdentifier(orderId);
        if (works == null) {
            throw new InvalidArgumentException(ServiceMessages.WORK_LIST_REQUIRED);
        }
        if (works.isEmpty()) {
            throw new InvalidArgumentException(ServiceMessages.WORK_LIST_MUST_NOT_BE_EMPTY);
        }
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        // Keep this operation inline: calling addWork through this instance bypasses the transactional proxy.
        works.forEach(order::addWork);
        return repairOrderRepository.save(order);
    }


    public RepairOrder changePriority(UUID orderId, OrderPriority priority) {
        requireIdentifier(orderId);
        if (priority == null) {
            throw new InvalidArgumentException(ServiceMessages.SERVICE_PRIORITY_REQUIRED);
        }
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        order.changePriority(priority);
        return repairOrderRepository.save(order);
    }


    public RepairOrder diagnose(UUID orderId) {
        requireIdentifier(orderId);
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        order.diagnose();
        return repairOrderRepository.save(order);
    }


    public RepairOrder approve(UUID orderId) {
        requireIdentifier(orderId);
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        order.approve();
        return repairOrderRepository.save(order);
    }


    public RepairOrder assignMechanic(UUID orderId, UUID mechanicId) {
        requireIdentifier(orderId);
        requireIdentifier(mechanicId);
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        Mechanic mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.MECHANIC_NOT_FOUND, mechanicId)));
        order.assignMechanic(mechanic);
        return repairOrderRepository.save(order);
    }


    public RepairOrder startProgress(UUID orderId) {
        requireIdentifier(orderId);
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        order.startProgress();
        return repairOrderRepository.save(order);
    }


    public RepairOrder completeWork(UUID orderId, int workIndex) {
        requireIdentifier(orderId);
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        order.completeWork(workIndex);
        return repairOrderRepository.save(order);
    }


    public RepairOrder complete(UUID orderId) {
        requireIdentifier(orderId);
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        order.ensureCanComplete();
        BigDecimal finalPrice = pricingPolicy.calculateFinalPrice(order);
        Instant completedAt = Instant.now(clock);
        order.complete(finalPrice, completedAt);
        return repairOrderRepository.save(order);
    }


    public RepairOrder cancel(UUID orderId) {
        requireIdentifier(orderId);
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        order.cancel();
        return repairOrderRepository.save(order);
    }


    @Transactional(readOnly = true)
    public List<RepairOrder> findByStatus(OrderStatus status) {
        if (status == null) {
            throw new InvalidArgumentException(ServiceMessages.SERVICE_STATUS_REQUIRED);
        }
        return repairOrderRepository.findByStatus(status);
    }


    @Transactional(readOnly = true)
    public BigDecimal calculateBasePrice(UUID orderId) {
        requireIdentifier(orderId);
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        return pricingPolicy.calculateBasePrice(order);
    }


    @Transactional(readOnly = true)
    public BigDecimal calculateFinalPrice(UUID orderId) {
        requireIdentifier(orderId);
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        return pricingPolicy.calculateFinalPrice(order);
    }


    @Transactional(readOnly = true)
    public Present createPresent(UUID orderId) {
        requireIdentifier(orderId);
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.REPAIR_ORDER_NOT_FOUND, orderId)));
        return giftPolicy.createPresent(order);
    }

    private void requireIdentifier(UUID identifier) {
        if (identifier == null) {
            throw new InvalidArgumentException(ServiceMessages.SERVICE_IDENTIFIER_REQUIRED);
        }
    }

    private void requireIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidArgumentException(ServiceMessages.SERVICE_IDENTIFIER_REQUIRED);
        }
    }
}
