package springredis.demo.utils;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.DataFormatData;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateStringConverter implements Converter<LocalDate> {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public Class<?> supportJavaTypeKey() {
        return LocalDate.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }


    @Override
    public LocalDate convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        String dateStr = cellData.getStringValue();
        if (StringUtils.hasLength(dateStr)){
            DateTimeFormatter formatter = getFormatter(contentProperty);
            return LocalDate.parse(dateStr, formatter);
        }else {
            return null;
        }
    }

    @Override
    public WriteCellData<LocalDate> convertToExcelData(LocalDate value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        String dateStr = value.format(getFormatter(contentProperty));
        return new WriteCellData<>(dateStr);
    }

    private DateTimeFormatter getFormatter(ExcelContentProperty contentProperty) {
        DateTimeFormat dateTimeFormat = contentProperty.getField().getAnnotation(DateTimeFormat.class);
        if (dateTimeFormat != null) {
            return DateTimeFormatter.ofPattern(dateTimeFormat.pattern());
        }
        return DEFAULT_FORMATTER;
    }
}
