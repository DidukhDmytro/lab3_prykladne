package com.example.carservice.exception;

public class ServiceMessages {
    private static String entityRequired = "Registered entities must not be null.";
    private static String duplicateEntity = "An entity with identifier '%s' is already registered.";
    private static String duplicateVin = "A car with VIN '%s' is already registered.";
    private static String repositoryOrderRequired = "A repair order is required.";
    private static String repositoryCarRequired = "A car is required.";
    private static String repositoryMechanicRequired = "A mechanic is required.";
    private static String repositoryIdentifierRequired = "An identifier is required.";
    private static String repositoryStatusRequired = "An order status is required.";
    private static String duplicateRepairOrder = "Repair order '%s' already exists.";
    private static String repairOrderNotFound = "Repair order '%s' was not found.";
    private static String carNotFound = "Car with VIN '%s' was not found.";
    private static String customerNotFound = "Customer '%s' was not found.";
    private static String repositoryCustomerRequired = "A customer is required.";
    private static String mechanicNotFound = "Mechanic '%s' was not found.";
    private static String serviceDependenciesRequired = "Service dependencies are required.";

    public static String entityRequired() { return entityRequired; }
    public static String duplicateEntity() { return duplicateEntity; }
    public static String duplicateVin() { return duplicateVin; }
    public static String repositoryOrderRequired() { return repositoryOrderRequired; }
    public static String repositoryCarRequired() { return repositoryCarRequired; }
    public static String repositoryMechanicRequired() { return repositoryMechanicRequired; }
    public static String repositoryIdentifierRequired() { return repositoryIdentifierRequired; }
    public static String repositoryStatusRequired() { return repositoryStatusRequired; }
    public static String duplicateRepairOrder() { return duplicateRepairOrder; }
    public static String repairOrderNotFound() { return repairOrderNotFound; }
    public static String carNotFound() { return carNotFound; }
    public static String customerNotFound() { return customerNotFound; }
    public static String repositoryCustomerRequired() { return repositoryCustomerRequired; }
    public static String mechanicNotFound() { return mechanicNotFound; }
    public static String serviceDependenciesRequired() { return serviceDependenciesRequired; }

    private ServiceMessages() {
    }
}
