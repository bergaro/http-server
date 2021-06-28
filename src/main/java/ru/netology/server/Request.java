package ru.netology.server;

import java.util.HashMap;
import java.util.Map;

public class Request {
    private String msgType;
    private String requestPath;
    private String requestHeader;
    private String requestBody;
    private Map<String, String> queryParamPare = new HashMap<>();

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRequestHeader() {
        return requestHeader;
    }

    public void setRequestHeader(String requestHeader) {
        this.requestHeader = requestHeader;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Map<String, String> getQueryParamPare() {
        return queryParamPare;
    }

    public void setQueryParamPare(Map<String, String> queryParamPare) {
        this.queryParamPare = queryParamPare;
    }

    @Override
    public String toString() {
        return "Request{" +
                "msgType='" + msgType + '\'' +
                ", requestPath='" + requestPath + '\'' +
                ", requestHeader='" + requestHeader + '\'' +
                ", requestBody='" + requestBody + '\'' +
                '}';
    }
}
