package com.soapboxrace.jaxb;

import com.soapboxrace.jaxb.http.CommerceResultStatus;

public class CommerceException extends Exception {
    private CommerceResultStatus status;

    public CommerceException(CommerceResultStatus status) {
        this.status = status;
    }

    public CommerceResultStatus getStatus() {
        return status;
    }
}
