package com.example.demo.dto.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class Result implements Serializable{


    private Boolean success;    //是否成功
    private String Message;

    //data可能为list 或者 object
    private Object data;
    private Long total;

    private String code;


    private String requestId; //请求ID


    //无参构造
    public static Result ok(){
        return new Result().setSuccess(true).setMessage("操作成功").setCode("200");
    }
    //有参构造
    public static Result ok(Object data){
        return new Result().setSuccess(true).setMessage("操作成功").setData(data).setCode("200");
    }
    //有参构造 list
    public static Result ok(List<?> data, Long total){
        return new Result().setSuccess(true).setMessage("操作成功").setData(data).setTotal(total).setCode("200");
    }
    //失败
    public static Result fail(String errorMsg, String code){
        return new Result().setSuccess(false).setMessage(errorMsg).setCode(code);
    }
}
