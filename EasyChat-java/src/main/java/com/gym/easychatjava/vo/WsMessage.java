package com.gym.easychatjava.vo;

import lombok.Data;

@Data
public class WsMessage<T> {

    private String type;

    private T data;
}
