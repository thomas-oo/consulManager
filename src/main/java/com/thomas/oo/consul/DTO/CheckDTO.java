package com.thomas.oo.consul.DTO;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;


@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CheckDTO.HTTPCheckDTO.class, name = "http"),
        @JsonSubTypes.Type(value = CheckDTO.ScriptCheckDTO.class, name = "script"),
        @JsonSubTypes.Type(value = CheckDTO.TCPCheckDTO.class, name = "tcp"),
        @JsonSubTypes.Type(value = CheckDTO.TTLCheckDTO.class, name = "ttl")
})
public abstract class CheckDTO {
    String checkId = "";
    int interval;

    public static class HTTPCheckDTO extends CheckDTO {
        String url="";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
    public static class ScriptCheckDTO extends CheckDTO {
        String script="";

        public String getScript() {
            return script;
        }

        public void setScript(String script) {
            this.script = script;
        }
    }
    public static class TCPCheckDTO extends CheckDTO {
        String addressAndPort="";

        public String getAddressAndPort() {
            return addressAndPort;
        }

        public void setAddressAndPort(String addressAndPort) {
            this.addressAndPort = addressAndPort;
        }
    }
    public static class TTLCheckDTO extends CheckDTO {
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }
}