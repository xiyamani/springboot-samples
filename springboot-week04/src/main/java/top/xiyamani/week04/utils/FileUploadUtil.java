package top.xiyamani.week04.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;
import top.xiyamani.week04.exception.BusinessException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class FileUploadUtil {

    // 固定使用绝对路径，避免初始化报错
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/src/main/resources/static/upload/";

    static {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            log.info("上传目录初始化成功: {}", UPLOAD_DIR);
        } catch (Exception e) {
            throw new RuntimeException("创建上传目录失败", e);
        }
    }

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".md", ".csv",
            ".zip", ".rar", ".7z",
            ".json", ".xml"
    );

    public static String upload(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }

        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(suffix)) {
            throw new BusinessException(400, "不支持的文件类型：" + suffix);
        }

        String fileName = UUID.randomUUID() + suffix;
        File dest = new File(UPLOAD_DIR, fileName);
        file.transferTo(dest);
        return fileName;
    }
}