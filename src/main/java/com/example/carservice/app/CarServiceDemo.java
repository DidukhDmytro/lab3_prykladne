package com.example.carservice.app;

import com.example.carservice.config.AppConstants;
import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Gift;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.domain.OrderPriority;
import com.example.carservice.domain.Present;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.exception.DomainException;
import com.example.carservice.pricing.PriceCalculator;
import com.example.carservice.pricing.rule.FixedDiscountRule;
import com.example.carservice.pricing.rule.PercentageDiscountRule;
import com.example.carservice.pricing.rule.UrgentSurchargeRule;
import com.example.carservice.repository.CarRepository;
import com.example.carservice.repository.CustomerRepository;
import com.example.carservice.repository.MechanicRepository;
import com.example.carservice.repository.RepairOrderRepository;
import com.example.carservice.repository.memory.InMemoryCarRepository;
import com.example.carservice.repository.memory.InMemoryCustomerRepository;
import com.example.carservice.repository.memory.InMemoryMechanicRepository;
import com.example.carservice.repository.memory.InMemoryRepairOrderRepository;
import com.example.carservice.service.GiftPolicy;
import com.example.carservice.service.RegistrationService;
import com.example.carservice.service.RepairOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class CarServiceDemo {
    private static String customerId = "customer-001";
    private static String customerName = "Alex Morgan";
    private static String customerPhone = "+1-555-0100";
    private static String carVin = "DEMO-VIN-001";
    private static String carMake = "Example Motors";
    private static String carModel = "Roadster";
    private static int carYear = 2010;
    private static int currentYear = 2026;
    private static int oldCarAgeLimit = 10;
    private static String generalMechanicId = "mechanic-general";
    private static String engineMechanicId = "mechanic-engine";
    private static String brakesMechanicId = "mechanic-brakes";
    private static String generalMechanicName = "Taylor General";
    private static String engineMechanicName = "Jordan Engine";
    private static String brakesMechanicName = "Casey Brakes";
    private static String completedOrderId = "order-complete";
    private static String normalOrderId = "order-normal";
    private static String urgentOrderId = "order-urgent";
    private static String minimumPriceOrderId = "order-minimum";
    private static String emptyOrderId = "order-empty";
    private static String noMechanicOrderId = "order-no-mechanic";
    private static String incompatibleOrderId = "order-incompatible";
    private static String busyOrderId = "order-busy";
    private static String otherBusyOrderId = "order-other-busy";
    private static String forbiddenTransitionOrderId = "order-forbidden-transition";
    private static String missingOrderId = "order-missing";
    private static String duplicateOrderId = "order-duplicate";
    private static String washerFluidGiftOrderId = "order-gift-washer-fluid";
    private static String interiorCleaningGiftOrderId = "order-gift-interior-cleaning";
    private static String discountVoucherGiftOrderId = "order-gift-discount-voucher";
    private static String orderStepFormat = "%s -> status: %s";
    private static String registrationStepFormat = "%s -> registered";
    private static String priceFormat = "%s -> base: %s; final: %s";
    private static String priceScenarioFormat = "%s -> final price: %s";
    private static String giftFormat = "%s -> parts: %s; gift: %s";
    private static String expectedErrorFormat = "%s -> expected error: %s";
    private static String missingExpectedErrorFormat = "%s -> expected error did not occur";
    private static String scenarioHeadingFormat = "\n%s";
    private static String registrationHeading = "1. Registration and repair order lifecycle";
    private static String pricingHeading = "2. Base and final prices";
    private static String giftHeading = "Gift tiers by parts total";
    private static String minimumHeading = "Minimum price after discount";
    private static String extensionHeading = "3. Extensible old-car discount rule";
    private static String orderingHeading = "4. Pricing rule order and stages";
    private static String negativeHeading = "5. Negative scenarios";
    private static String customerRegistered = "Customer registered";
    private static String carRegistered = "Car registered";
    private static String generalMechanicRegistered = "General mechanic registered";
    private static String engineMechanicRegistered = "Engine mechanic registered";
    private static String brakesMechanicRegistered = "Brakes mechanic registered";
    private static String orderCreated = "Order created";
    private static String engineWorkAdded = "Engine work added";
    private static String brakesWorkAdded = "Brakes work added";
    private static String orderDiagnosed = "Order diagnosed";
    private static String orderApproved = "Order approved";
    private static String mechanicAssigned = "Mechanic assigned";
    private static String progressStarted = "Progress started";
    private static String firstWorkCompleted = "First work completed";
    private static String secondWorkCompleted = "Second work completed";
    private static String orderCompleted = "Order completed";
    private static String fetchedOrderStatusFormat = "Fetched completed order status: %s";
    private static String normalLabel = "NORMAL order";
    private static String urgentLabel = "URGENT order";
    private static String washerFluidLabel = "Washer fluid gift tier";
    private static String interiorCleaningLabel = "Interior cleaning gift tier";
    private static String voucherLabel = "Discount voucher gift tier";
    private static String minimumPriceLabel = "100% discount with minimum price";
    private static String oldCarDiscountLabelFormat = "Old-car discount (%s%%)";
    private static String fixedThenPercentLabel = "Fixed discount, then percentage discount";
    private static String percentThenFixedLabel = "Percentage discount, then fixed discount";
    private static String customStageLabel = "Custom surcharge stage before percentage discount";
    private static String duplicateOrderLabel = "Creating a duplicate order";
    private static String duplicateVinLabel = "Duplicate VIN";
    private static String emptyApprovalLabel = "Approving an empty order";
    private static String missingMechanicStartLabel = "Starting without an assigned mechanic";
    private static String incompatibleMechanicLabel = "Assigning an incompatible mechanic";
    private static String busyMechanicLabel = "Assigning a busy mechanic";
    private static String completedModificationLabel = "Modifying a completed order";
    private static String forbiddenTransitionLabel = "Completing an order before progress";
    private static String missingOrderLabel = "Fetching a missing order";
    private static String engineDescription = "Engine service";
    private static String brakesDescription = "Brake service";
    private static String giftWorkDescription = "Parts order";
    private static BigDecimal enginePartsCost = new BigDecimal("200.00");
    private static BigDecimal engineLaborCost = new BigDecimal("85.00");
    private static BigDecimal brakesPartsCost = new BigDecimal("60.00");
    private static BigDecimal brakesLaborCost = new BigDecimal("45.00");
    private static BigDecimal giftLaborCost = new BigDecimal("1.00");
    private static BigDecimal washerFluidPartsTotal = new BigDecimal("250.00");
    private static BigDecimal interiorCleaningPartsTotal = new BigDecimal("550.00");
    private static BigDecimal voucherPartsTotal = new BigDecimal("1111.00");
    private static BigDecimal minimumScenarioPartsCost = new BigDecimal("10.00");
    private static BigDecimal minimumScenarioLaborCost = new BigDecimal("10.00");
    private static BigDecimal discountPercent = new BigDecimal("10");
    private static BigDecimal oldCarDiscountPercent = new BigDecimal("5");
    private static BigDecimal fullDiscountPercent = new BigDecimal("100");
    private static BigDecimal fixedDiscountAmount = new BigDecimal("100.00");
    private static BigDecimal customSurchargeAmount = new BigDecimal("25.00");
    private static int firstWorkIndex = 0;
    private static int secondWorkIndex = 1;

    private CarServiceDemo() {
    }

    public static void main(String[] args) {
        CarRepository carRepository = new InMemoryCarRepository();
        CustomerRepository customerRepository = new InMemoryCustomerRepository();
        MechanicRepository mechanicRepository = new InMemoryMechanicRepository();
        RepairOrderRepository repairOrderRepository = new InMemoryRepairOrderRepository();
        PriceCalculator priceCalculator = new PriceCalculator(
                List.of(new UrgentSurchargeRule(AppConstants.urgentLaborSurchargeFactor())),
                AppConstants.minimumPrice());
        GiftPolicy giftPolicy = new GiftPolicy();
        RegistrationService registrationService =
                new RegistrationService(customerRepository, carRepository, mechanicRepository);
        RepairOrderService repairOrderService = new RepairOrderService(
                repairOrderRepository,
                carRepository,
                mechanicRepository,
                priceCalculator,
                giftPolicy);

        Customer customer = new Customer(customerId, customerName, customerPhone);
        Car car = new Car(carVin, carMake, carModel, carYear, customer);
        Mechanic generalMechanic =
                new Mechanic(generalMechanicId, generalMechanicName, MechanicSpecialization.GENERAL);
        Mechanic engineMechanic =
                new Mechanic(engineMechanicId, engineMechanicName, MechanicSpecialization.ENGINE);
        Mechanic brakesMechanic =
                new Mechanic(brakesMechanicId, brakesMechanicName, MechanicSpecialization.BRAKES);

        RepairOrder completedOrder = runRegistrationAndLifecycle(
                registrationService,
                repairOrderService,
                customer,
                car,
                generalMechanic,
                engineMechanic,
                brakesMechanic);
        RepairOrder normalOrder = runPricingScenarios(repairOrderService, car);
        runRuleExtensionScenario(normalOrder);
        runRuleOrderingScenario(normalOrder);
        runNegativeScenarios(
                registrationService,
                repairOrderService,
                completedOrder,
                car,
                engineMechanic,
                brakesMechanic);
    }

    private static RepairOrder runRegistrationAndLifecycle(
            RegistrationService registrationService,
            RepairOrderService repairOrderService,
            Customer customer,
            Car car,
            Mechanic generalMechanic,
            Mechanic engineMechanic,
            Mechanic brakesMechanic) {
        System.out.println(String.format(scenarioHeadingFormat, registrationHeading));
        registrationService.registerCustomer(customer);
        printRegistration(customerRegistered);
        registrationService.registerCar(car);
        printRegistration(carRegistered);
        registrationService.registerMechanic(generalMechanic);
        printRegistration(generalMechanicRegistered);
        registrationService.registerMechanic(engineMechanic);
        printRegistration(engineMechanicRegistered);
        registrationService.registerMechanic(brakesMechanic);
        printRegistration(brakesMechanicRegistered);

        RepairOrder completedOrder = runCompletedOrder(repairOrderService, generalMechanic);
        System.out.println(String.format(
                fetchedOrderStatusFormat,
                repairOrderService.getOrder(completedOrderId).getStatus()));
        return completedOrder;
    }

    private static RepairOrder runPricingScenarios(
            RepairOrderService repairOrderService,
            Car car) {
        System.out.println(String.format(scenarioHeadingFormat, pricingHeading));
        RepairOrder normalOrder = createPricedOrder(repairOrderService, normalOrderId, car, false);
        printPrice(normalLabel, repairOrderService, normalOrderId);
        createPricedOrder(repairOrderService, urgentOrderId, car, true);
        printPrice(urgentLabel, repairOrderService, urgentOrderId);
        printGifts(repairOrderService, car);

        repairOrderService.createOrder(minimumPriceOrderId, carVin);
        repairOrderService.addWork(
                minimumPriceOrderId,
                new ServiceWork(
                        giftWorkDescription,
                        minimumScenarioPartsCost,
                        minimumScenarioLaborCost,
                        MechanicSpecialization.GENERAL));
        PriceCalculator fullDiscountCalculator = new PriceCalculator(
                List.of(new PercentageDiscountRule(fullDiscountPercent)),
                AppConstants.minimumPrice());
        System.out.println(String.format(
                scenarioHeadingFormat,
                minimumHeading));
        System.out.println(String.format(
                priceScenarioFormat,
                minimumPriceLabel,
                fullDiscountCalculator.calculateFinalPrice(
                        repairOrderService.getOrder(minimumPriceOrderId))));
        return normalOrder;
    }

    private static void runRuleExtensionScenario(RepairOrder normalOrder) {
        System.out.println(String.format(scenarioHeadingFormat, extensionHeading));
        PriceCalculator oldCarCalculator = new PriceCalculator(
                List.of(new OldCarDiscountRule(currentYear, oldCarAgeLimit, oldCarDiscountPercent)),
                AppConstants.minimumPrice());
        System.out.println(String.format(
                priceScenarioFormat,
                String.format(oldCarDiscountLabelFormat, oldCarDiscountPercent),
                oldCarCalculator.calculateFinalPrice(normalOrder)));
    }

    private static void runRuleOrderingScenario(RepairOrder normalOrder) {
        System.out.println(String.format(scenarioHeadingFormat, orderingHeading));
        PriceCalculator fixedThenPercentCalculator = new PriceCalculator(
                List.of(
                        new FixedDiscountRule(fixedDiscountAmount),
                        new PercentageDiscountRule(discountPercent)),
                AppConstants.minimumPrice());
        PriceCalculator percentThenFixedCalculator = new PriceCalculator(
                List.of(
                        new PercentageDiscountRule(discountPercent),
                        new FixedDiscountRule(fixedDiscountAmount)),
                AppConstants.minimumPrice());
        System.out.println(String.format(
                priceScenarioFormat,
                fixedThenPercentLabel,
                fixedThenPercentCalculator.calculateFinalPrice(normalOrder)));
        System.out.println(String.format(
                priceScenarioFormat,
                percentThenFixedLabel,
                percentThenFixedCalculator.calculateFinalPrice(normalOrder)));
        PriceCalculator customStageCalculator = new PriceCalculator(
                List.of(
                        new PercentageDiscountRule(discountPercent),
                        new DemonstrationSurchargeRule(customSurchargeAmount)),
                AppConstants.minimumPrice());
        System.out.println(String.format(
                priceScenarioFormat,
                customStageLabel,
                customStageCalculator.calculateFinalPrice(normalOrder)));
    }

    private static RepairOrder runCompletedOrder(
            RepairOrderService repairOrderService,
            Mechanic mechanic) {
        RepairOrder order = repairOrderService.createOrder(completedOrderId, carVin);
        printOrderStep(orderCreated, order);
        repairOrderService.addWork(completedOrderId, createEngineWork());
        printOrderStep(engineWorkAdded, repairOrderService.getOrder(completedOrderId));
        repairOrderService.addWork(completedOrderId, createBrakesWork());
        printOrderStep(brakesWorkAdded, repairOrderService.getOrder(completedOrderId));
        repairOrderService.diagnose(completedOrderId);
        printOrderStep(orderDiagnosed, repairOrderService.getOrder(completedOrderId));
        repairOrderService.approve(completedOrderId);
        printOrderStep(orderApproved, repairOrderService.getOrder(completedOrderId));
        repairOrderService.assignMechanic(completedOrderId, mechanic.getId());
        printOrderStep(mechanicAssigned, repairOrderService.getOrder(completedOrderId));
        repairOrderService.startProgress(completedOrderId);
        printOrderStep(progressStarted, repairOrderService.getOrder(completedOrderId));
        repairOrderService.completeWork(completedOrderId, firstWorkIndex);
        printOrderStep(firstWorkCompleted, repairOrderService.getOrder(completedOrderId));
        repairOrderService.completeWork(completedOrderId, secondWorkIndex);
        printOrderStep(secondWorkCompleted, repairOrderService.getOrder(completedOrderId));
        repairOrderService.complete(completedOrderId);
        RepairOrder completed = repairOrderService.getOrder(completedOrderId);
        printOrderStep(orderCompleted, completed);
        return completed;
    }

    private static RepairOrder createPricedOrder(
            RepairOrderService repairOrderService,
            String orderId,
            Car car,
            boolean urgent) {
        repairOrderService.createOrder(orderId, car.getVin());
        repairOrderService.addWork(orderId, createEngineWork());
        repairOrderService.addWork(orderId, createBrakesWork());
        if (urgent) {
            repairOrderService.changePriority(orderId, OrderPriority.URGENT);
        }
        return repairOrderService.getOrder(orderId);
    }

    private static void printPrice(
            String label,
            RepairOrderService repairOrderService,
            String orderId) {
        System.out.println(String.format(
                priceFormat,
                label,
                repairOrderService.calculateBasePrice(orderId),
                repairOrderService.calculateFinalPrice(orderId)));
    }

    private static void printGifts(RepairOrderService repairOrderService, Car car) {
        System.out.println(String.format(scenarioHeadingFormat, giftHeading));
        printGiftScenario(
                repairOrderService,
                washerFluidGiftOrderId,
                car,
                washerFluidPartsTotal,
                washerFluidLabel);
        printGiftScenario(
                repairOrderService,
                interiorCleaningGiftOrderId,
                car,
                interiorCleaningPartsTotal,
                interiorCleaningLabel);
        printGiftScenario(
                repairOrderService,
                discountVoucherGiftOrderId,
                car,
                voucherPartsTotal,
                voucherLabel);
    }

    private static void printGiftScenario(
            RepairOrderService repairOrderService,
            String orderId,
            Car car,
            BigDecimal partsTotal,
            String label) {
        repairOrderService.createOrder(orderId, car.getVin());
        repairOrderService.addWork(
                orderId,
                new ServiceWork(
                        giftWorkDescription,
                        partsTotal,
                        giftLaborCost,
                        MechanicSpecialization.GENERAL));
        Present present = repairOrderService.createPresent(orderId);
        Optional<Gift> gift = present.getGift();
        System.out.println(String.format(
                giftFormat,
                label,
                present.getPartsTotal(),
                gift.map(Gift::getName).orElse(AppConstants.noGiftLabel())));
    }

    private static void runNegativeScenarios(
            RegistrationService registrationService,
            RepairOrderService repairOrderService,
            RepairOrder completedOrder,
            Car car,
            Mechanic engineMechanic,
            Mechanic brakesMechanic) {
        System.out.println(String.format(scenarioHeadingFormat, negativeHeading));
        repairOrderService.createOrder(duplicateOrderId, carVin);
        expectError(duplicateOrderLabel, () ->
                repairOrderService.createOrder(duplicateOrderId, carVin));

        expectError(duplicateVinLabel, () -> registrationService.registerCar(
                new Car(carVin, carMake, carModel, carYear, car.getOwner())));

        repairOrderService.createOrder(emptyOrderId, carVin);
        repairOrderService.diagnose(emptyOrderId);
        expectError(emptyApprovalLabel, () -> repairOrderService.approve(emptyOrderId));

        prepareApprovedOrder(repairOrderService, noMechanicOrderId);
        expectError(missingMechanicStartLabel, () -> repairOrderService.startProgress(noMechanicOrderId));

        prepareApprovedOrder(repairOrderService, incompatibleOrderId);
        expectError(incompatibleMechanicLabel, () ->
                repairOrderService.assignMechanic(incompatibleOrderId, brakesMechanic.getId()));

        prepareApprovedOrder(repairOrderService, busyOrderId);
        repairOrderService.assignMechanic(busyOrderId, engineMechanic.getId());
        repairOrderService.startProgress(busyOrderId);
        prepareApprovedOrder(repairOrderService, otherBusyOrderId);
        expectError(busyMechanicLabel, () ->
                repairOrderService.assignMechanic(otherBusyOrderId, engineMechanic.getId()));

        expectError(completedModificationLabel, () ->
                repairOrderService.addWork(completedOrder.getId(), createEngineWork()));
        repairOrderService.createOrder(forbiddenTransitionOrderId, carVin);
        expectError(forbiddenTransitionLabel, () ->
                repairOrderService.complete(forbiddenTransitionOrderId));
        expectError(missingOrderLabel, () -> repairOrderService.getOrder(missingOrderId));
    }

    private static void prepareApprovedOrder(
            RepairOrderService repairOrderService,
            String orderId) {
        repairOrderService.createOrder(orderId, carVin);
        repairOrderService.addWork(orderId, createEngineWork());
        repairOrderService.diagnose(orderId);
        repairOrderService.approve(orderId);
    }

    private static void expectError(String scenario, Runnable action) {
        try {
            action.run();
            System.out.println(String.format(missingExpectedErrorFormat, scenario));
        } catch (DomainException exception) {
            System.out.println(String.format(expectedErrorFormat, scenario, exception.getMessage()));
        }
    }

    private static void printOrderStep(String step, RepairOrder order) {
        System.out.println(String.format(orderStepFormat, step, order.getStatus()));
    }

    private static void printRegistration(String step) {
        System.out.println(String.format(registrationStepFormat, step));
    }

    private static ServiceWork createEngineWork() {
        return new ServiceWork(
                engineDescription,
                enginePartsCost,
                engineLaborCost,
                MechanicSpecialization.ENGINE);
    }

    private static ServiceWork createBrakesWork() {
        return new ServiceWork(
                brakesDescription,
                brakesPartsCost,
                brakesLaborCost,
                MechanicSpecialization.BRAKES);
    }
}
