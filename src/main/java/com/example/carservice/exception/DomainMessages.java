package com.example.carservice.exception;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DomainMessages {
    public static final String INVALID_VIN = "VIN must not be null or blank.";
    public static final String INVALID_PHONE_NUMBER = "Phone number must not be null or blank.";
    public static final String INVALID_CUSTOMER_ID = "Customer identifier must not be null.";
    public static final String INVALID_CUSTOMER_NAME = "Customer name must not be null or blank.";
    public static final String OWNER_REQUIRED = "Car owner is required.";
    public static final String INVALID_SERVICE_WORK_DESCRIPTION =
            "Service work description must not be null or blank.";
    public static final String COSTS_MUST_BE_NON_NEGATIVE = "Parts and labor costs must not be negative.";
    public static final String SPECIALIZATION_REQUIRED = "Mechanic specialization is required.";
    public static final String INVALID_REPAIR_ORDER_ID = "Repair order identifier must not be null.";
    public static final String CAR_REQUIRED = "A car is required for a repair order.";
    public static final String SERVICE_WORK_REQUIRED = "Service work is required.";
    public static final String INVALID_MECHANIC_ID = "Mechanic identifier must not be null.";
    public static final String INVALID_MECHANIC_NAME = "Mechanic name must not be null or blank.";
    public static final String MECHANIC_REQUIRED_FOR_ASSIGNMENT = "A mechanic is required for assignment.";
    public static final String MECHANIC_NOT_COMPATIBLE = "The mechanic is not compatible with the order work.";
    public static final String PRIORITY_REQUIRED = "Order priority is required.";
    public static final String COSTS_REQUIRED = "Parts and labor costs are required.";
    public static final String WORK_ALREADY_COMPLETED = "Service work is already completed.";
    public static final String INVALID_DIAGNOSE_STATUS = "Only created orders can be diagnosed.";
    public static final String INVALID_APPROVE_STATUS = "Only diagnosed orders can be approved.";
    public static final String EMPTY_ORDER = "An order cannot be approved without service work.";
    public static final String INVALID_PROGRESS_STATUS = "Only approved orders can start progress.";
    public static final String MECHANIC_REQUIRED = "A mechanic must be assigned before work starts.";
    public static final String INVALID_COMPLETE_WORK_STATUS = "Work can only be completed on in-progress orders.";
    public static final String INVALID_COMPLETE_STATUS = "Only in-progress orders can be completed.";
    public static final String WORK_INDEX_OUT_OF_RANGE = "Work index is outside the order work list.";
    public static final String UNFINISHED_WORK = "An order cannot be completed with unfinished work.";
    public static final String CLOSED_ORDER = "A completed or cancelled order cannot be modified.";
    public static final String INVALID_CANCEL_STATUS = "Cannot cancel order in status: ";
    public static final String GIFT_ORDER_REQUIRED = "A repair order is required to create a present.";
    public static final String COMPLETION_DETAILS_REQUIRED = "Final price and completion time are required.";

}
