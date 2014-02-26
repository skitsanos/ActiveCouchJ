package com.skitsanos.api;

/**
 * Created with IntelliJ IDEA.
 * User: umashankar
 * Date: 25/07/12
 * Time: 23:25
 * To change this template use File | Settings | File Templates.
 */
public class ServerResponse {
    private String contentType;
    private Object data;
    private Long execTime;

    public String contentType() {
        return contentType;
    }

    public void contentType(String contentType) {
        this.contentType = contentType;
    }

    public String data() {
        return (String)data;
    }

    public Byte[] dataBytes() {
        return (Byte[])data;
    }

    public void data(Object data) {
        this.data = data;
    }
    
    public void data(String data) {
        this.data = data;
    }

    public Long execTime() {
        return execTime;
    }

    public void execTime(Long execTime) {
        this.execTime = execTime;
    }
}
