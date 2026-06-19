package com.aiplus.spring_rag.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aiplus.spring_rag.dto.FileUploadDTO;
import com.aiplus.spring_rag.utils.FileUtils;

@Service
public class FileService {
    
    @Value("${file.storage.base-path}")
    private Path basePath;
    
    @Value("${file.storage.temp-path}")
    private Path tempPath;

    /**
     * 上传文件通用处理
     */
    public void uploadFileHandle(MultipartFile file, FileUploadDTO fileUploadDTO, Integer userId) {
        // 文件基础检查
        // TODO：考虑存在相同文件的情形，即可省略大部分存储过程。
        if (file.isEmpty()) {
            throw new RuntimeException("文件为空！");
        }
        if (!FileUtils.isAllowedFileType(file.getOriginalFilename())) {
            throw new RuntimeException("非法文件类型！");
        }

        // 创建唯一临时文件
        Path tempFilePath = null;
        try {
            // 方法会尝试一定次数自动生成唯一文件名
            tempFilePath = Files.createTempFile(tempPath, userId + "_", ".tmp", null); 
        } catch (IOException e) {
            System.out.println("临时文件创建失败，重点检查重复文件名！");
            e.printStackTrace();
        }
        // 文件临时存到磁盘同时求 SHA-256
        String sha256Hash = null;
        try (InputStream inputStream = file.getInputStream();
            OutputStream outputStream = Files.newOutputStream(tempFilePath)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            sha256Hash = FileUtils.getHexString(hashBytes);
        } catch (IOException e){
            System.out.println("上传文件读取异常！");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("未找到 SHA-256 算法，JDK 异常");
        }

        // 检查文件的合法性
        FileUtils.isExtensionMatchMagicType(tempFilePath.toString());

        // 根据文件 SHA-256 重新存储文件并删除临时文件
        if (sha256Hash != null) {
            // 获取一级、二级目录
            String firstDirectory = sha256Hash.substring(0, 2);
            String secondDirectory = sha256Hash.substring(2, 4);

            // 拼接完整路径
            String fileExtensionName;
            int extensionIdx = file.getOriginalFilename().lastIndexOf(".");
            if (extensionIdx != -1) {
                fileExtensionName = file.getOriginalFilename().substring(extensionIdx);
            } else {
                fileExtensionName = "";
                throw new RuntimeException("非法文件扩展名");
            }
            String realPath = basePath.toString() + "//" + firstDirectory + "//" + secondDirectory
                + "//" + sha256Hash + fileExtensionName;

            // 存储文件
            try (InputStream inputStream = Files.newInputStream(tempFilePath, null);
                    OutputStream outputStream = Files.newOutputStream(Path.of(realPath), null)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                System.out.println("文件存储到本地异常！");
                e.printStackTrace();
            }
            // 删除临时文件
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException e) {
                System.out.println("临时文件删除异常！");
                e.printStackTrace();
            }
        }


        // 文件分块并向量化存储到 Qdrant。TODO: 可以此处可以优化成并行
        
        // 保存记录到 MySQL，需要等待文件存储完成

        // 如果是智能体知识库上传，需要更新智能体 fileIds


    }

    /**
     * 文件存储
     */
    public void fileSave(InputStream inputStream, String filePath) {


        

    }

}
