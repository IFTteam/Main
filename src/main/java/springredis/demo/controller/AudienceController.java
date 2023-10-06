package springredis.demo.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springredis.demo.Service.AudienceService;
import springredis.demo.entity.Audience;
import springredis.demo.entity.Node;
import springredis.demo.entity.User;
import springredis.demo.entity.request.AudienceVo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/audience")
public class AudienceController {

    @Autowired
    private AudienceService audienceService;

    @PostMapping("/add")
    public ResponseEntity<Object> add(@RequestParam("file") MultipartFile file) throws IOException {
        List<AudienceVo> dataList = EasyExcel.read(file.getInputStream()).head(AudienceVo.class).sheet().doReadSync();
        List<Audience> audiences = new ArrayList<>();
        for (AudienceVo audienceVo : dataList) {
            Audience audience = new Audience();
            BeanUtils.copyProperties(audienceVo, audience);
            if (audienceVo.getUserId() != null){
                User user = new User();
                user.setId(audienceVo.getUserId());
                audience.setUser(user);
            }
            if (audienceVo.getAudienceNodeId() != null){
                Node node = new Node();
                node.setId(audienceVo.getAudienceNodeId());
                audience.setNode(node);
            }
            audiences.add(audience);
        }
        List<Audience> audienceResult = audienceService.saveBatch(audiences);
        return new ResponseEntity<>(audienceResult, HttpStatus.CREATED);
    }

}
