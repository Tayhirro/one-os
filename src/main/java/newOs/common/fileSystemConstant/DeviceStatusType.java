package newOs.common.fileSystemConstant;


import lombok.Getter;

@Getter
public enum DeviceStatusType {
    FREE(0, "空闲"),
    BUSY(1, "忙碌"),
    ERROR(2, "错误");

    private int code;
    private String desc;        // 描述

    DeviceStatusType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
