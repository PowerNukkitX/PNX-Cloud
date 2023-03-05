package cn.powernukkitx.cloud.helper;

import cn.powernukkitx.cloud.bean.DownloadIDBean;
import cn.powernukkitx.cloud.bean.FileInfoBean;
import cn.powernukkitx.cloud.bean.RepoDataBean;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.fulltext.Languages;
import org.dizitart.no2.fulltext.UniversalTextTokenizer;
import org.dizitart.no2.objects.ObjectRepository;

import java.nio.file.Path;

@Context
@Singleton
public class DBHelper {
    private static DBHelper INSTANCE = null;

    private final Nitrite db;
    private final ObjectRepository<DownloadIDBean> downloadIDBeanObjectRepository;
    private final ObjectRepository<FileInfoBean> fileInfoBeanObjectRepository;
    private final ObjectRepository<RepoDataBean> repoDataBeanObjectRepository;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected DBHelper() {
        var tokenizer = new UniversalTextTokenizer();
        tokenizer.loadLanguage(Languages.English, Languages.Chinese);
        var dbDirPath = Path.of("./data/db");
        if (!dbDirPath.toFile().exists()) {
            dbDirPath.toFile().mkdirs();
        }
        db = Nitrite.builder()
                .compressed()
                .textTokenizer(tokenizer)
                .filePath("./data/db/pnx-cloud.db")
                .openOrCreate();
        downloadIDBeanObjectRepository = db.getRepository(DownloadIDBean.class);
        fileInfoBeanObjectRepository = db.getRepository(FileInfoBean.class);
        repoDataBeanObjectRepository = db.getRepository(RepoDataBean.class);
        INSTANCE = this;
    }

    public Nitrite getRawDb() {
        return db;
    }

    public ObjectRepository<DownloadIDBean> getDownloadIDBeanObjectRepository() {
        return downloadIDBeanObjectRepository;
    }

    public ObjectRepository<FileInfoBean> getFileInfoBeanObjectRepository() {
        return fileInfoBeanObjectRepository;
    }

    public ObjectRepository<RepoDataBean> getRepoDataBeanObjectRepository() {
        return repoDataBeanObjectRepository;
    }

    @PreDestroy
    public void close() {
        if (!db.isClosed())
            db.close();
        if (INSTANCE != null)
            INSTANCE = null;
    }

    public static DBHelper getInstance() {
        return INSTANCE;
    }
}
