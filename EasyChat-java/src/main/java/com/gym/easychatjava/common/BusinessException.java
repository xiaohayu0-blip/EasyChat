package com.gym.easychatjava.common;

/**
 * 业务异常类
 * 遇到业务上"不能继续"的情况，直接抛出此异常
 */
public class BusinessException extends RuntimeException{

    private int code;

    /**
     * 从枚举中获取code和msg
     * @param resultCode
     */
    public BusinessException(ResultCode resultCode){
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    /**
     * 自定义code和msg
     * @param code
     * @param msg
     */
    public BusinessException(int code,String msg){
        super(msg);
        this.code = code;
    }

    public int getCode(){
        return code;
    }
}
