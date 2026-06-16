package __PACKAGE_NAME__.domain.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SampleEntity {

    private Long id;

    private String name;

    private String description;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
