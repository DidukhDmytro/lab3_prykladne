package com.example.carservice.exception;

public class DomainMessages {
    private static String invalidVin = "VIN must not be null or blank.";
    private static String invalidPhoneNumber = "Phone number must not be null or blank.";
    private static String invalidCustomerId = "Customer identifier must not be null or blank.";
    private static String invalidCustomerName = "Customer name must not be null or blank.";
    private static String ownerRequired = "Car owner is required.";
    private static String invalidServiceWorkDescription =
            "Service work description must not be null or blank.";
    private static String costsMustBeNonNegative = "Parts and labor costs must not be negative.";
    private static String specializationRequired = "Mechanic specialization is required.";
    private static String invalidRepairOrderId = "Repair order identifier must not be null or blank.";
    private static String carRequired = "A car is required for a repair order.";
    private static String serviceWorkRequired = "Service work is required.";
    private static String invalidMechanicId = "Mechanic identifier must not be null or blank.";
    private static String invalidMechanicName = "Mechanic name must not be null or blank.";
    private static String mechanicRequiredForAssignment = "A mechanic is required for assignment.";
    private static String mechanicNotCompatible = "The mechanic is not compatible with the order work.";
    private static String priorityRequired = "Order priority is required.";
    private static String costsRequired = "Parts and labor costs are required.";
    private static String workAlreadyCompleted = "Service work is already completed.";
    private static String invalidDiagnoseStatus = "Only created orders can be diagnosed.";
    private static String invalidApproveStatus = "Only diagnosed orders can be approved.";
    private static String emptyOrder = "An order cannot be approved without service work.";
    private static String invalidProgressStatus = "Only approved orders can start progress.";
    private static String mechanicRequired = "A mechanic must be assigned before work starts.";
    private static String invalidCompleteWorkStatus = "Work can only be completed on in-progress orders.";
    private static String invalidCompleteStatus = "Only in-progress orders can be completed.";
    private static String workIndexOutOfRange = "Work index is outside the order work list.";
    private static String unfinishedWork = "An order cannot be completed with unfinished work.";
    private static String closedOrder = "A completed or cancelled order cannot be modified.";
    private static String invalidCancelStatus = "Cannot cancel order in status: ";
    private static String giftOrderRequired = "A repair order is required to create a present.";

    public static String invalidVin() { return invalidVin; }
    public static String invalidPhoneNumber() { return invalidPhoneNumber; }
    public static String invalidCustomerId() { return invalidCustomerId; }
    public static String invalidCustomerName() { return invalidCustomerName; }
    public static String ownerRequired() { return ownerRequired; }
    public static String invalidServiceWorkDescription() { return invalidServiceWorkDescription; }
    public static String costsMustBeNonNegative() { return costsMustBeNonNegative; }
    public static String specializationRequired() { return specializationRequired; }
    public static String invalidRepairOrderId() { return invalidRepairOrderId; }
    public static String carRequired() { return carRequired; }
    public static String serviceWorkRequired() { return serviceWorkRequired; }
    public static String invalidMechanicId() { return invalidMechanicId; }
    public static String invalidMechanicName() { return invalidMechanicName; }
    public static String mechanicRequiredForAssignment() { return mechanicRequiredForAssignment; }
    public static String mechanicNotCompatible() { return mechanicNotCompatible; }
    public static String priorityRequired() { return priorityRequired; }
    public static String costsRequired() { return costsRequired; }
    public static String workAlreadyCompleted() { return workAlreadyCompleted; }
    public static String invalidDiagnoseStatus() { return invalidDiagnoseStatus; }
    public static String invalidApproveStatus() { return invalidApproveStatus; }
    public static String emptyOrder() { return emptyOrder; }
    public static String invalidProgressStatus() { return invalidProgressStatus; }
    public static String mechanicRequired() { return mechanicRequired; }
    public static String invalidCompleteWorkStatus() { return invalidCompleteWorkStatus; }
    public static String invalidCompleteStatus() { return invalidCompleteStatus; }
    public static String workIndexOutOfRange() { return workIndexOutOfRange; }
    public static String unfinishedWork() { return unfinishedWork; }
    public static String closedOrder() { return closedOrder; }
    public static String invalidCancelStatus() { return invalidCancelStatus; }
    public static String giftOrderRequired() { return giftOrderRequired; }

    private DomainMessages() {
    }
}
