package com.itsm.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stand-in for a vendor SMS SDK (e.g. Twilio/MSG91). Note its API is deliberately
 * different from ours (msisdn, payload, numeric priority flag, vendor response type) -
 * that mismatch is exactly what {@link SmsNotificationChannel} adapts.
 */
@Component
public class ThirdPartySmsGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartySmsGatewayClient.class);

    public record GatewayResponse(int code, String messageSid) {}

    public GatewayResponse dispatch(String msisdn, String payload, int priorityFlag) {
        if (msisdn == null || msisdn.isBlank()) {
            return new GatewayResponse(400, null);
        }
        String sid = "SM" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[SMS simulated] to={} flag={} sid={} text=\"{}\"", msisdn, priorityFlag, sid, payload);
        return new GatewayResponse(202, sid);
    }
}
