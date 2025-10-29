package com.couponpop.security.enums;

public enum MemberType {

    OWNER,
    CUSTOMER;

    public String roleName() {
        return "ROLE_" + this.name();
    }
}
