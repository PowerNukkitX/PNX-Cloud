package cn.powernukkitx.cloud.bean;

import cn.powernukkitx.cloud.util.FileUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.Indices;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Indices({
        @Index(value = "path", type = IndexType.Unique),
        @Index(value = "fileName", type = IndexType.NonUnique),
        @Index(value = "md5", type = IndexType.NonUnique)
})
public final class FileInfoBean {
    @NotNull String path;
    @NotNull String fileName;
    long size;
    long lastUpdateTime;
    @NotNull String md5;
    long downloadID;

    public FileInfoBean(@NotNull Path baseDir, @NotNull Path filePath) throws IOException {
        var file = filePath.toFile();
        this.path = filePath.toAbsolutePath().toString().replace('\\', '/');
        this.fileName = baseDir.relativize(filePath).toString().replace('\\', '/');
        this.size = file.length();
        this.lastUpdateTime = file.lastModified();
        this.md5 = FileUtil.getMD5(file);
    }

    @Contract(" -> new")
    public @NotNull NoSecretFileInfoBean toNoSecretFileInfoBean() {
        return new NoSecretFileInfoBean(fileName, size, lastUpdateTime, md5, downloadID);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NoSecretFileInfoBean {
        @NotNull String fileName;
        long size;
        long lastUpdateTime;
        @NotNull String md5;
        long downloadID;
    }
}
