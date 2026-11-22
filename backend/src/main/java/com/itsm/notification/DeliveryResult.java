package com.itsm.notification;

public record DeliveryResult(boolean success, String status, String detail) {
    public static DeliveryResult sent(String detail) { return new DeliveryResult(true, "SENT", detail); }
    public static DeliveryResult simulated(String detail) { return new DeliveryResult(true, "SIMULATED", detail); }
    public static DeliveryResult failed(String detail) { return new DeliveryResult(false, "FAILED", detail); }
}
