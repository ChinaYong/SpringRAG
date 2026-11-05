$ErrorActionPreference = 'Stop'

if (-not (Test-Path '.\mvnw.cmd')) {
    throw '请先切换到 SpringRAG 项目根目录运行。'
}

$classpathFile = Join-Path $env:TEMP "qdrant-grpc-cp-$PID.txt"
$sourceFile = Join-Path $env:TEMP "QdrantGrpcPermissionCheck-$PID.java"
$secureKey = Read-Host '输入 QDRANT_API_KEY' -AsSecureString
$pointer = [IntPtr]::Zero
$previousKey = $env:QDRANT_API_KEY
$javaExit = 1

$javaSource = @'
import io.grpc.StatusRuntimeException;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import java.util.concurrent.TimeUnit;

public class QdrantGrpcPermissionCheck {
    public static void main(String[] args) {
        String key = System.getenv("QDRANT_API_KEY");
        if (key == null || key.isBlank()) {
            System.err.println("QDRANT_API_KEY 未提供");
            System.exit(1);
        }

        String host = "8f6c49b8-202c-466c-b1a6-56c881420ed1.us-west-1-0.aws.cloud.qdrant.io";
        String collection = "grpc-permission-check-" +
                java.util.UUID.randomUUID().toString().replace("-", "");
        QdrantGrpcClient grpc = QdrantGrpcClient
                .newBuilder(host, 6334, true)
                .withApiKey(key)
                .build();
        QdrantClient client = new QdrantClient(grpc);

        boolean created = false;
        String stage = "gRPC listCollections";
        Exception failure = null;

        try {
            var collections = client.listCollectionsAsync().get(20, TimeUnit.SECONDS);
            System.out.println("通过：gRPC 可以列出 collections，共 " + collections.size() + " 个。");

            stage = "gRPC createCollection";
            client.createCollectionAsync(
                    collection,
                    VectorParams.newBuilder().setSize(3).setDistance(Distance.Cosine).build()
            ).get(20, TimeUnit.SECONDS);
            created = true;
            System.out.println("通过：gRPC 可以创建 collection（" + collection + "）。");
        } catch (Exception e) {
            failure = e;
        } finally {
            if (created) {
                try {
                    client.deleteCollectionAsync(collection).get(20, TimeUnit.SECONDS);
                    System.out.println("清理完成：临时 collection 已删除。");
                } catch (Exception e) {
                    System.err.println("清理失败，请在 Qdrant 控制台手动删除：" + collection);
                }
            }
            client.close();
        }

        if (failure != null) {
            Throwable cause = failure;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof StatusRuntimeException grpcError) {
                System.err.println("失败 [" + stage + "]：gRPC " +
                        grpcError.getStatus().getCode() + " - " +
                        grpcError.getStatus().getDescription());
            } else {
                System.err.println("失败 [" + stage + "]：" + cause);
            }
            System.exit(1);
        }
    }
}
'@

try {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
    $env:QDRANT_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)

    $invalidChars = @(
        for ($i = 0; $i -lt $env:QDRANT_API_KEY.Length; $i++) {
            $code = [int][char]$env:QDRANT_API_KEY[$i]
            if ($code -lt 32 -or $code -gt 126) {
                "位置 $i：U+$($code.ToString('X4'))"
            }
        }
    )
    if ($invalidChars.Count -gt 0) {
        throw "API Key 含有 gRPC 不接受的非 ASCII 字符：$($invalidChars -join ', ')。请重新输入；密钥内容不会被输出。"
    }
    if ($env:QDRANT_API_KEY -ne $env:QDRANT_API_KEY.Trim()) {
        throw 'API Key 前后包含空白字符，请重新输入；密钥内容不会被输出。'
    }
    Write-Host "Key 字符检查通过（长度 $($env:QDRANT_API_KEY.Length)，未显示密钥）。"

    [IO.File]::WriteAllText(
        $sourceFile,
        $javaSource,
        [Text.UTF8Encoding]::new($false)
    )

    & .\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=$classpathFile"
    if ($LASTEXITCODE -ne 0) {
        throw '生成项目依赖 classpath 失败。'
    }

    $classpath = (Get-Content $classpathFile -Raw).Trim()
    if ([string]::IsNullOrWhiteSpace($classpath)) {
        throw '项目依赖 classpath 为空。'
    }

    & java -cp $classpath $sourceFile
    $javaExit = $LASTEXITCODE
}
catch {
    Write-Host "检查脚本失败：$($_.Exception.Message)" -ForegroundColor Red
    $javaExit = 1
}
finally {
    if ($null -eq $previousKey) {
        Remove-Item Env:QDRANT_API_KEY -ErrorAction SilentlyContinue
    } else {
        $env:QDRANT_API_KEY = $previousKey
    }

    if ($pointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
    Remove-Item $sourceFile, $classpathFile -ErrorAction SilentlyContinue
}

if ($javaExit -eq 0) {
    Write-Host '检查完成：gRPC 权限验证通过。' -ForegroundColor Green
} else {
    Write-Host "检查完成：失败，退出码 $javaExit。" -ForegroundColor Red
}
Read-Host '按 Enter 返回 PowerShell'