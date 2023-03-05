package cn.powernukkitx.cloud.bean;

import cn.powernukkitx.cloud.helper.DBHelper;
import lombok.Getter;
import lombok.Setter;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.objects.Id;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

@Index(value = "url", type = IndexType.Unique)
public class DownloadIDBean {
    @Id
    @Getter
    @Setter
    long id;

    @Getter
    @Setter
    String url;

    @Getter
    @Setter
    String savePath;

    public static long getDownloadId(@NotNull String url, @NotNull ObjectRepository<DownloadIDBean> repo) {
        var result = repo.find(ObjectFilters.eq("url", url));
        if (result.size() != 0) {
            return result.firstOrDefault().getId();
        } else {
            var bean = new DownloadIDBean();
            bean.setUrl(url);
            bean.setId(repo.size() + 1);
            repo.insert(bean);
            return bean.getId();
        }
    }

    public static long getDownloadId(@NotNull String url, @NotNull DBHelper dbHelper) {
        return getDownloadId(url, dbHelper.getDownloadIDBeanObjectRepository());
    }

    public static long getDownloadId(@NotNull URL url, @NotNull ObjectRepository<DownloadIDBean> repo) {
        return getDownloadId(url.toString(), repo);
    }

    public static long getDownloadId(@NotNull URL url, @NotNull DBHelper dbHelper) {
        return getDownloadId(url.toString(), dbHelper.getDownloadIDBeanObjectRepository());
    }
}
