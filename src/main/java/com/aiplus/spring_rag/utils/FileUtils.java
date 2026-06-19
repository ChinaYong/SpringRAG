package com.aiplus.spring_rag.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件处理相关的工具类
 */
public class FileUtils {
    
    // 允许用户上传的文件类型
    private static List<String> allowedFileTypes = List.of(".txt", ".md", ".csv", ".json", ".xml",
        ".log", ".yaml", ".java", ".py", ".c", ".cpp", ".pdf", ".png", ".jpg"
    ); 

    /**
     * 允许用户上传的魔数类型"25504446", "FFD8", "89504E47"
     * 按照声明顺序：pdf、jpg、png
     */ 
    private static Map<String, String> allowedMagicTypes = Map.of(
        ".pdf", "25504446",
        ".jpg", "FFD8",
        ".png", "89504E47"
    );

    // UTF-8、UTF-16 LE、 UTF-16 BE
    private static List<String> utfHead = List.of("EFBBBF", "FFFE", "FEFF");

    // 十六进制转换辅助字符数组
    private static char[] HEX = "0123456789abcdef".toCharArray();

    // 文本编码名
    private static List<String> textEncodings = List.of("UTF-8", "UTF-16LE", "UTF-16BE", "GBK");

    // 根据文件名获取包括"."的文件扩展名
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    // 获取不包括文件扩展名的文件名
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

    // 检查文件扩展名的扩展名与魔数是否匹配
    public static boolean isExtensionMatchMagicType(String filePath) {
        String fileExtension = getFileExtension(filePath);

        // 对不同文件类型进行判断
        switch (fileExtension) {
            case ".txt":
            case ".md":
            case ".csv":
            case ".json":
            case ".xml":
            case ".log":
            case ".yaml":
            case ".java":
            case ".py":
            case ".c":
            case ".cpp":
                return isTextFile(filePath);
            case ".pdf":
                return isPdfFile(filePath);

            case ".jpg":
                return isJpgFile(filePath);
            case ".png":
                return isPngFile(filePath);
            default:
                throw new RuntimeException("不支持的文件类型！");

        }
        
    }
        

    // 判断文件是否是文本文件
    public static boolean isTextFile(String filePath) {
        
        Path path = Path.of(filePath);

        byte[] readBuffer = new byte[4096];
        int n = 0;
        // 先判断是否是 UTF-8 BOM、UTF-16 LE、 UTF-16 BE
        try (InputStream textFileInputStream = Files.newInputStream(path, null)) {
            StringBuilder sb = new StringBuilder();
            if ((n = textFileInputStream.read(readBuffer)) != -1) {
                int i = 0;
                if (n >= 2) {
                    for (; i < 2; i++) {
                        sb.append(HEX[readBuffer[i] >>> 4]);
                        sb.append(HEX[readBuffer[i] & 0x0f]);
                    }
                    if (utfHead.contains(sb.toString())) {
                        return true;
                    }
                } else if (n > 2) {
                    sb.append(HEX[readBuffer[i] >>> 4])
                        .append(HEX[readBuffer[i] & 0x0f]);
                    if (utfHead.contains(sb.toString())) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("判断是否为文本文件时读取异常！");
            e.printStackTrace();
        }

        // 零字节排除
        for (int i = 0; i < n; i++) { // byte 默认值为0，所以只能根据被实际填充的元素进行判断
            if (readBuffer[i] == 0) {
                return false;
            }
        }

        // 编码合法性校验
        for (String encoding : textEncodings) {
            if (isValidEncoding(encoding, readBuffer)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPdfFile(String filePath) {
        if (filePath == null) {
            throw new RuntimeException("文件路径为空！");
        }

        Path path = Path.of(filePath);
        byte[] bytes = new byte[4];

        return "25504446".equals(getHexString(bytes));
    }

    public static boolean isJpgFile(String filePath) {
        if (filePath == null) {
            throw new RuntimeException("文件路径为空");
        }

        Path path = Path.of(filePath);
        byte[] bytes = new byte[2];

        return "FFD8".equals(getHexString(bytes));
    }

    public static boolean isPngFile(String filePath) {
        if (filePath == null) {
            throw new RuntimeException("文件路径为空");
        }

        Path path = Path.of(filePath);
        byte[] bytes = new byte[4];

        return "89504E47".equals(getHexString(bytes));
    }

    public static boolean isValidEncoding(String encoding, byte[] readBuffer) {

        try {
            CharsetDecoder decoder = Charset.forName(encoding).newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT)  // 不符合编码格式时报错
                //.onUnmappableCharacter(CodingErrorAction.REPORT)    // 符合某种编码但与指定编码不符时报错
                .decode(ByteBuffer.wrap(readBuffer));
            return true;
        } catch (CharacterCodingException e) {
            System.out.println("字符集解码异常！");
            e.printStackTrace();
            return false;
        }
    }

    public static  String getHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(HEX[b >>> 4]);
            sb.append(HEX[b & 0x0f]);
        }

        return sb.toString();
    }
}
