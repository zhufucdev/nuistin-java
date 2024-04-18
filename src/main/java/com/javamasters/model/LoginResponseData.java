package com.javamasters.model;

public record LoginResponseData(boolean reauth, String username, String balance, String duration, String outport,
                                String totaltimepsan, String useripadd) {

}
