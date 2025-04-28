package com.finsense.util;

import java.util.UUID;

public class PaymentIdGenerator {

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
