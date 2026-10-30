package com.example.carservice.exception;

public class ServiceMessages {
    public static String entityRequired = "Registered entities must not be null.";
    public static String duplicateEntity = "An entity with identifier '%s' is already registered.";
    public static String duplicateVin = "A car with VIN '%s' is already registered.";
    public static String repositoryOrderRequired = "A repair order is required.";
    public static String repositoryCarRequired = "A car is required.";
    public static String repositoryMechanicRequired = "A mechanic is required.";
    public static String repositoryIdentifierRequired = "An identifier is required.";
    public static String repositoryStatusRequired = "An order status is required.";
    public static String duplicateRepairOrder = "Repair order '%s' already exists.";
    public static String repairOrderNotFound = "Repair order '%s' was not found.";
    public static String carNotFound = "Car with VIN '%s' was not found.";
    public static String customerNotFound = "Customer '%s' was not found.";
    public static String repositoryCustomerRequired = "A customer is required.";
    public static String mechanicNotFound = "Mechanic '%s' was not found.";
    public static String serviceDependenciesRequired = "Service dependencies are required.";
    public static String workListRequired = "A work list is required.";
    public static String workListMustNotBeEmpty = "A work list must not be empty.";
    public static String serviceIdentifierRequired = "A service identifier is required.";
    public static String serviceStatusRequired = "An order status is required.";
    public static String servicePriorityRequired = "An order priority is required.";
    public static String serviceMechanicRequired = "A mechanic is required.";

    private ServiceMessages() {
    }
}
