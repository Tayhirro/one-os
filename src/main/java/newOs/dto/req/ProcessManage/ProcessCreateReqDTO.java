package newOs.dto.req.ProcessManage;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProcessCreateReqDTO {
    private String processName;
    private String[] instructions;
}
