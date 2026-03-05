package top.xiyamani.hellosample.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.xiyamani.hellosample.vo.ResultVO;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public ResultVO<String> hello() {
        return ResultVO.success("Hello Spring Boot");
    }
}
