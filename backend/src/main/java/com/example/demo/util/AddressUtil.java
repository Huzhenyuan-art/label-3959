package com.example.demo.util;

import com.example.demo.entity.UserAddress;

public final class AddressUtil {

    private AddressUtil() {
    }

    public static String buildFullAddress(UserAddress address) {
        if (address == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, address.getProvince());
        appendIfPresent(sb, address.getCity());
        appendIfPresent(sb, address.getDistrict());
        sb.append(address.getDetailAddress());
        return sb.toString();
    }

    private static void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(value);
        }
    }
}
