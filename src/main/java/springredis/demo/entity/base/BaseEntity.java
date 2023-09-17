package springredis.demo.entity.base;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import springredis.demo.utils.LocalDateTimeStringConverter;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    @DateTimeFormat("yyyy-MM-dd HH:mm:ss.SSSSSS")
    @ExcelProperty(value = "created_at", converter = LocalDateTimeStringConverter.class)
    @CreatedDate
    @Column(updatable = false)
    @JsonIgnore
    private LocalDateTime createdAt;

    @ExcelProperty("created_by")
    @CreatedBy
    @Column(updatable = false)
    @JsonIgnore
    private String createdBy;

    @DateTimeFormat("yyyy-MM-dd HH:mm:ss.SSSSSS")
    @ExcelProperty(value = "updated_at", converter = LocalDateTimeStringConverter.class)
    @LastModifiedDate
    @Column()
    @JsonIgnore
    private LocalDateTime updatedAt;

    @ExcelProperty("updated_by")
    @LastModifiedBy
    @Column()
    @JsonIgnore
    private String updatedBy;

    public BaseEntity(LocalDateTime createdAt, String createdBy, LocalDateTime updatedAt, String updatedBy) {
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public BaseEntity() {
    }

    public BaseEntity(LocalDateTime createdAt, String createdBy) {
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }
}