package com.example.carservice.exception;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ServiceMessages {
    public static final String ENTITY_REQUIRED = "Registered entities must not be null.";
    public static final String DUPLICATE_ENTITY = "An entity with identifier '%s' is already registered.";
    public static final String DUPLICATE_VIN = "A car with VIN '%s' is already registered.";
    public static final String REPOSITORY_CAR_REQUIRED = "A car is required.";
    public static final String DUPLICATE_REPAIR_ORDER = "Repair order '%s' already exists.";
    public static final String REPAIR_ORDER_NOT_FOUND = "Repair order '%s' was not found.";
    public static final String CAR_NOT_FOUND = "Car with VIN '%s' was not found.";
    public static final String CUSTOMER_NOT_FOUND = "Customer '%s' was not found.";
    public static final String MECHANIC_NOT_FOUND = "Mechanic '%s' was not found.";
    public static final String SERVICE_DEPENDENCIES_REQUIRED = "Service dependencies are required.";
    public static final String WORK_LIST_REQUIRED = "A work list is required.";
    public static final String WORK_LIST_MUST_NOT_BE_EMPTY = "A work list must not be empty.";
    public static final String SERVICE_IDENTIFIER_REQUIRED = "A service identifier is required.";
    public static final String SERVICE_STATUS_REQUIRED = "An order status is required.";
    public static final String SERVICE_PRIORITY_REQUIRED = "An order priority is required.";
    public static final String SERVICE_MECHANIC_REQUIRED = "A mechanic is required.";

}
