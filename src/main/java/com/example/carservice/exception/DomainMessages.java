package com.example.carservice.exception;

public class DomainMessages {
    public static String invalidVin = "VIN must not be null or blank.";
    public static String invalidPhoneNumber = "Phone number must not be null or blank.";
    public static String invalidCustomerId = "Customer identifier must not be null or blank.";
    public static String invalidCustomerName = "Customer name must not be null or blank.";
    public static String ownerRequired = "Car owner is required.";
    public static String invalidServiceWorkDescription =
            "Service work description must not be null or blank.";
    public static String costsMustBeNonNegative = "Parts and labor costs must not be negative.";
    public static String specializationRequired = "Mechanic specialization is required.";
    public static String invalidRepairOrderId = "Repair order identifier must not be null or blank.";
    public static String carRequired = "A car is required for a repair order.";
    public static String serviceWorkRequired = "Service work is required.";
    public static String invalidMechanicId = "Mechanic identifier must not be null or blank.";
    public static String invalidMechanicName = "Mechanic name must not be null or blank.";
    public static String mechanicRequiredForAssignment = "A mechanic is required for assignment.";
    public static String mechanicNotCompatible = "The mechanic is not compatible with the order work.";
    public static String priorityRequired = "Order priority is required.";
    public static String costsRequired = "Parts and labor costs are required.";
    public static String workAlreadyCompleted = "Service work is already completed.";
    public static String invalidDiagnoseStatus = "Only created orders can be diagnosed.";
    public static String invalidApproveStatus = "Only diagnosed orders can be approved.";
    public static String emptyOrder = "An order cannot be approved without service work.";
    public static String invalidProgressStatus = "Only approved orders can start progress.";
    public static String mechanicRequired = "A mechanic must be assigned before work starts.";
    public static String invalidCompleteWorkStatus = "Work can only be completed on in-progress orders.";
    public static String invalidCompleteStatus = "Only in-progress orders can be completed.";
    public static String workIndexOutOfRange = "Work index is outside the order work list.";
    public static String unfinishedWork = "An order cannot be completed with unfinished work.";
    public static String closedOrder = "A completed or cancelled order cannot be modified.";
    public static String invalidCancelStatus = "Cannot cancel order in status: ";
    public static String giftOrderRequired = "A repair order is required to create a present.";
    public static String completionDetailsRequired = "Final price and completion time are required.";

    private DomainMessages() {
    }
}
