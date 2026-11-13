package com.example.carservice.app;

import com.example.carservice.config.AppConstants;
import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumerate.MechanicSpecialization;
import com.example.carservice.domain.enumerate.OrderPriority;
import com.example.carservice.domain.enumerate.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.enumerate.WorkStatus;
import com.example.carservice.pricing.PriceCalculator;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.pricing.rule.FixedDiscountRule;
import com.example.carservice.pricing.rule.PercentageDiscountRule;
import com.example.carservice.service.RegistrationService;
import com.example.carservice.service.RepairOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CarServiceDemoRunner implements CommandLineRunner {
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final String CUSTOMER_NAME = "Alex Morgan";
    private static final String CUSTOMER_PHONE = "+1-555-0100";
    private static final String CAR_VIN = "DEMO-VIN-001";
    private static final String CAR_MAKE = "Example Motors";
    private static final String CAR_MODEL = "Roadster";
    private static final int CAR_YEAR = 2010;
    private static final int CURRENT_YEAR = 2026;
    private static final int OLD_CAR_AGE_LIMIT = 10;
    private static final UUID MECHANIC_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final String MECHANIC_NAME = "Taylor General";
    private static final UUID DEMO_ORDER_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000005");
    private static final UUID NORMAL_PRICING_ORDER_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000006");
    private static final UUID URGENT_PRICING_ORDER_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000007");
    private static final String RESTART_INSTRUCTION =
            "Run the application again to load and complete this order.";
    private static final String REGISTRATION_FORMAT = "%s -> %s";
    private static final String LOADED_ORDER_FORMAT = "%s loaded from database -> status: %s";
    private static final String MECHANIC_FORMAT = "Assigned mechanic: %s";
    private static final String ORDER_STATUS_FORMAT = "Order status: %s";
    private static final String NO_MECHANIC_LABEL = "none";
    private static final String WORK_FORMAT = "Work: %s | status: %s | parts: %s | labor: %s";
    private static final String COMPLETION_FORMAT =
            "Completed order -> final price: %s; status: %s; completed at: %s";
    private static final String PRICE_FORMAT = "%s -> base: %s; final: %s";
    private static final String CREATED_ORDER_LABEL = "Created demo order";
    private static final String CUSTOMER_REGISTERED_LABEL = "Customer registered";
    private static final String CAR_REGISTERED_LABEL = "Car registered";
    private static final String MECHANIC_REGISTERED_LABEL = "Mechanic registered";
    private static final String NORMAL_PRICING_LABEL = "Normal pricing";
    private static final String URGENT_PRICING_LABEL = "Urgent surcharge";
    private static final String FIXED_THEN_PERCENT_LABEL = "Fixed discount then percentage discount";
    private static final String PERCENT_THEN_FIXED_LABEL = "Percentage discount then fixed discount";
    private static final String OLD_CAR_PRICING_LABEL = "Old-car discount";
    private static final String CUSTOM_SURCHARGE_LABEL = "Custom surcharge before percentage discount";
    private static final String ENGINE_DESCRIPTION = "Engine service";
    private static final String BRAKES_DESCRIPTION = "Brake service";
    private static final BigDecimal ENGINE_PARTS_COST = new BigDecimal("200.00");
    private static final BigDecimal ENGINE_LABOR_COST = new BigDecimal("85.00");
    private static final BigDecimal BRAKES_PARTS_COST = new BigDecimal("60.00");
    private static final BigDecimal BRAKES_LABOR_COST = new BigDecimal("45.00");
    private static final BigDecimal DISCOUNT_PERCENT = new BigDecimal("10");
    private static final BigDecimal OLD_CAR_DISCOUNT_PERCENT = new BigDecimal("5");
    private static final BigDecimal FIXED_DISCOUNT_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal CUSTOM_SURCHARGE_AMOUNT = new BigDecimal("25.00");
    private static final int FIRST_WORK_INDEX = 0;

    private RegistrationService registrationService;
    private RepairOrderService repairOrderService;
    private PricingPolicy pricingPolicy;

    public CarServiceDemoRunner(
            RegistrationService registrationService,
            RepairOrderService repairOrderService,
            PricingPolicy pricingPolicy) {
        this.registrationService = registrationService;
        this.repairOrderService = repairOrderService;
        this.pricingPolicy = pricingPolicy;
    }

    @Override
    public void run(String... args) {
        Customer customer = ensureCustomer();
        Car car = ensureCar(customer);
        Mechanic mechanic = ensureMechanic(
                MECHANIC_ID,
                MECHANIC_NAME,
                MechanicSpecialization.GENERAL);
        Optional<RepairOrder> existingOrder = repairOrderService.findOrder(DEMO_ORDER_ID);
        if (existingOrder.isPresent()) {
            handleExistingDemoOrder(existingOrder.get());
        } else {
            RepairOrder order = createDemoOrder(car, mechanic);
            printOrderDetails(order);
            System.out.println(RESTART_INSTRUCTION);
        }
        runPricingDemonstrations(car);
    }

    private Customer ensureCustomer() {
        Optional<Customer> existing = registrationService.findCustomer(CUSTOMER_ID);
        if (existing.isPresent()) {
            return existing.get();
        }
        Customer customer = new Customer(CUSTOMER_ID, CUSTOMER_NAME, CUSTOMER_PHONE);
        registrationService.registerCustomer(customer);
        System.out.println(String.format(REGISTRATION_FORMAT, CUSTOMER_REGISTERED_LABEL, CUSTOMER_ID));
        return customer;
    }

    private Car ensureCar(Customer customer) {
        Optional<Car> existing = registrationService.findCar(CAR_VIN);
        if (existing.isPresent()) {
            return existing.get();
        }
        Car car = new Car(CAR_VIN, CAR_MAKE, CAR_MODEL, CAR_YEAR, customer);
        registrationService.registerCar(car);
        System.out.println(String.format(REGISTRATION_FORMAT, CAR_REGISTERED_LABEL, CAR_VIN));
        return car;
    }

    private Mechanic ensureMechanic(
            UUID id,
            String name,
            MechanicSpecialization specialization) {
        Optional<Mechanic> existing = registrationService.findMechanic(id);
        if (existing.isPresent()) {
            return existing.get();
        }
        Mechanic mechanic = new Mechanic(
                id,
                name,
                specialization);
        registrationService.registerMechanic(mechanic);
        System.out.println(String.format(REGISTRATION_FORMAT, MECHANIC_REGISTERED_LABEL, id));
        return mechanic;
    }

    private RepairOrder createDemoOrder(Car car, Mechanic mechanic) {
        repairOrderService.createOrder(DEMO_ORDER_ID, car.getVin());
        repairOrderService.addWorks(DEMO_ORDER_ID, List.of(createEngineWork(), createBrakesWork()));
        repairOrderService.diagnose(DEMO_ORDER_ID);
        repairOrderService.approve(DEMO_ORDER_ID);
        repairOrderService.assignMechanic(DEMO_ORDER_ID, mechanic.getId());
        repairOrderService.startProgress(DEMO_ORDER_ID);
        RepairOrder createdOrder = repairOrderService.findOrder(DEMO_ORDER_ID).orElseThrow();
        System.out.println(String.format(REGISTRATION_FORMAT, CREATED_ORDER_LABEL, DEMO_ORDER_ID));
        return createdOrder;
    }

    private void handleExistingDemoOrder(RepairOrder order) {
        System.out.println(String.format(LOADED_ORDER_FORMAT, DEMO_ORDER_ID, order.getStatus()));
        printOrderDetails(order);
        if (order.getStatus() == OrderStatus.IN_PROGRESS) {
            completeDemoOrder(order);
        } else if (order.getStatus() == OrderStatus.COMPLETED) {
            printCompletion(order);
        }
    }

    private void printOrderDetails(RepairOrder order) {
        System.out.println(String.format(ORDER_STATUS_FORMAT, order.getStatus()));
        Mechanic mechanic = order.getAssignedMechanic();
        System.out.println(String.format(
                MECHANIC_FORMAT,
                mechanic == null ? NO_MECHANIC_LABEL : mechanic.getFullName()));
        order.getWorks().stream()
                .map(work -> String.format(
                        WORK_FORMAT,
                        work.getDescription(),
                        work.getStatus(),
                        work.getPartsCost(),
                        work.getLaborCost()))
                .forEach(System.out::println);
    }

    private void completeDemoOrder(RepairOrder order) {
        List<ServiceWork> works = order.getWorks();
        IntStream.range(FIRST_WORK_INDEX, works.size())
                .filter(index -> works.get(index).getStatus() == WorkStatus.PENDING)
                .forEach(index -> repairOrderService.completeWork(DEMO_ORDER_ID, index));
        repairOrderService.complete(DEMO_ORDER_ID);
        printCompletion(repairOrderService.findOrder(DEMO_ORDER_ID).orElseThrow());
    }

    private void printCompletion(RepairOrder order) {
        System.out.println(String.format(
                COMPLETION_FORMAT,
                order.getFinalPrice(),
                order.getStatus(),
                order.getCompletedAt()));
    }

    private void runPricingDemonstrations(Car car) {
        RepairOrder normalOrder = createPricingOrder(NORMAL_PRICING_ORDER_ID, car, false);
        printPrice(NORMAL_PRICING_LABEL, normalOrder);
        RepairOrder urgentOrder = createPricingOrder(URGENT_PRICING_ORDER_ID, car, true);
        printPrice(URGENT_PRICING_LABEL, urgentOrder);

        PriceCalculator fixedThenPercentCalculator = new PriceCalculator(
                List.of(
                        new FixedDiscountRule(FIXED_DISCOUNT_AMOUNT),
                        new PercentageDiscountRule(DISCOUNT_PERCENT)),
                AppConstants.MINIMUM_PRICE);
        PriceCalculator percentThenFixedCalculator = new PriceCalculator(
                List.of(
                        new PercentageDiscountRule(DISCOUNT_PERCENT),
                        new FixedDiscountRule(FIXED_DISCOUNT_AMOUNT)),
                AppConstants.MINIMUM_PRICE);
        System.out.println(String.format(
                PRICE_FORMAT,
                FIXED_THEN_PERCENT_LABEL,
                fixedThenPercentCalculator.calculateBasePrice(normalOrder),
                fixedThenPercentCalculator.calculateFinalPrice(normalOrder)));
        System.out.println(String.format(
                PRICE_FORMAT,
                PERCENT_THEN_FIXED_LABEL,
                percentThenFixedCalculator.calculateBasePrice(normalOrder),
                percentThenFixedCalculator.calculateFinalPrice(normalOrder)));
        PriceCalculator customStageCalculator = new PriceCalculator(
                List.of(
                        new PercentageDiscountRule(DISCOUNT_PERCENT),
                        new DemonstrationSurchargeRule(CUSTOM_SURCHARGE_AMOUNT)),
                AppConstants.MINIMUM_PRICE);
        System.out.println(String.format(
                PRICE_FORMAT,
                CUSTOM_SURCHARGE_LABEL,
                customStageCalculator.calculateBasePrice(normalOrder),
                customStageCalculator.calculateFinalPrice(normalOrder)));

        PriceCalculator oldCarCalculator = new PriceCalculator(
                List.of(new OldCarDiscountRule(
                        CURRENT_YEAR,
                        OLD_CAR_AGE_LIMIT,
                        OLD_CAR_DISCOUNT_PERCENT)),
                AppConstants.MINIMUM_PRICE);
        System.out.println(String.format(
                PRICE_FORMAT,
                OLD_CAR_PRICING_LABEL,
                oldCarCalculator.calculateBasePrice(normalOrder),
                oldCarCalculator.calculateFinalPrice(normalOrder)));
    }

    private void printPrice(String label, RepairOrder order) {
        System.out.println(String.format(
                PRICE_FORMAT,
                label,
                pricingPolicy.calculateBasePrice(order),
                pricingPolicy.calculateFinalPrice(order)));
    }

    private RepairOrder createPricingOrder(UUID id, Car car, boolean urgent) {
        RepairOrder order = new RepairOrder(id, car);
        order.addWork(createEngineWork());
        order.addWork(createBrakesWork());
        if (urgent) {
            order.changePriority(OrderPriority.URGENT);
        }
        return order;
    }

    private ServiceWork createEngineWork() {
        return new ServiceWork(
                ENGINE_DESCRIPTION,
                ENGINE_PARTS_COST,
                ENGINE_LABOR_COST,
                MechanicSpecialization.ENGINE);
    }

    private ServiceWork createBrakesWork() {
        return new ServiceWork(
                BRAKES_DESCRIPTION,
                BRAKES_PARTS_COST,
                BRAKES_LABOR_COST,
                MechanicSpecialization.BRAKES);
    }

}
