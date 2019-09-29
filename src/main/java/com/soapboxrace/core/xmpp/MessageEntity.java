package com.soapboxrace.core.xmpp;

public class MessageEntity {
    private String from;
    private String body;
    private String subject;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void calculateHash(String to) {
        Long hash = SubjectCalc.calculateHash(to.toCharArray(), body.toCharArray());
        this.subject = hash.toString();
    }
}
