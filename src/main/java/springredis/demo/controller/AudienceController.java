package springredis.demo.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springredis.demo.Service.AudienceService;
import springredis.demo.entity.Audience;
import springredis.demo.entity.Node;
import springredis.demo.entity.User;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/audience")
public class AudienceController {

    @Autowired
    private AudienceService audienceService;

    @PostMapping("/add")
    public ResponseEntity<Object> add(@RequestParam("file") MultipartFile file) throws IOException {
        List<Audience> dataList = EasyExcel.read(file.getInputStream()).head(Audience.class).sheet().doReadSync();
        for (Audience audience : dataList) {
            if (audience.getUserId() != null){
                User user = new User();
                user.setId(audience.getUserId());
                audience.setUser(user);
            }
            if (audience.getAudienceNodeId() != null){
                Node node = new Node();
                node.setId(audience.getAudienceNodeId());
                audience.setNode(node);
            }
        }
        List<Audience> audienceResult = audienceService.saveBatch(dataList);
        return new ResponseEntity<>(audienceResult, HttpStatus.CREATED);
    }

}
