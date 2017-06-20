package com.thomas.oo.consul.DTO;

public class ServiceDTO {
    String address = "";
    int port;
    String serviceName = "";
    String serviceId = "";
    String[] tags = new String[0];

    public ServiceDTO(){}

    public ServiceDTO(String address, int port, String serviceName, String serviceId, String... tags) {
        this.address = address;
        this.port = port;
        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.tags = tags;
    }

    public ServiceDTO(int port, String serviceName, String serviceId, String... tags) {
        this.port = port;
        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.tags = tags;
    }

    public ServiceDTO(int port, String serviceId){
        this.port = port;
        this.serviceId = serviceId;
    }

    public ServiceDTO(int port, String serviceId, String[] tags){
        this.port = port;
        this.serviceId = serviceId;
        this.tags = tags;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServiceName() { return serviceName; }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }
}
