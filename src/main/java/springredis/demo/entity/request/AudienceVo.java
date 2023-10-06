package springredis.demo.entity.request;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;


import springredis.demo.utils.LocalDateStringConverter;
import springredis.demo.utils.LocalDateTimeStringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
public class AudienceVo  {

    @DateTimeFormat("yyyy-MM-dd HH:mm:ss.SSSSSS")
    @ExcelProperty(value = "created_at", converter = LocalDateTimeStringConverter.class)
    private LocalDateTime createdAt;

    @ExcelProperty("created_by")
    private String createdBy;

    @DateTimeFormat("yyyy-MM-dd HH:mm:ss.SSSSSS")
    @ExcelProperty(value = "updated_at", converter = LocalDateTimeStringConverter.class)
    private LocalDateTime updatedAt;

    @ExcelProperty("updated_by")
    private String updatedBy;

    @ExcelProperty("id")
    private long id;
    @ExcelProperty("email")
    private String email;
    @ExcelProperty("first_name")
    private String firstName;
    @ExcelProperty("last_name")
    private String lastName;
    @ExcelProperty("phone")
    private String phone;
    @ExcelProperty("address")
    private String address;

    @ExcelProperty(value = "birthday", converter = LocalDateStringConverter.class)
    @DateTimeFormat("yyyy/MM/dd")
    private LocalDate birthday;
    @ExcelProperty("source")
    private String source;

    @ExcelIgnore
    private String gender;


    @ExcelProperty("audience_node_id")
    private Long audienceNodeId;


    @ExcelProperty("user_id")
    private Long userId;


    @ExcelProperty(value = "date_added", converter = LocalDateStringConverter.class)
    @DateTimeFormat("yyyy/MM/dd")
    private LocalDate date_added;

    @ExcelProperty(value = "last_updated_time", converter = LocalDateStringConverter.class)
    @DateTimeFormat("yyyy/MM/dd")
    private LocalDate last_updated_time;


}

