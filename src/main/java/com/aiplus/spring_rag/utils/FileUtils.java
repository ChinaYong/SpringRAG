package com.aiplus.spring_rag.utils;

import java.util.List;

/**
 * 文件处理相关的工具类
 */
public class FileUtils {
    
    // 允许用户上传的文件类型
    private static List<String> allowedFileTypes = List.of(".txt", ".pdf", ".doc", ".docx",
        ".md"
    ); 

    // 根据文件名获取文件扩展名
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    // 依据文件扩展名检查用户上传的文件是否合法
    public static boolean isAllowedFileType(String fileName) {
        String fileExtension = getFileExtension(fileName);
        return allowedFileTypes.contains(fileExtension.toLowerCase());
    }
}
