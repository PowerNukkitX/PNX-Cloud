package cn.powernukkitx.cloud.bean;

import cn.powernukkitx.cloud.config.GHConfig;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.objects.Id;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.Indices;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

@Data
@NoArgsConstructor
@Indices({
        @Index(value = "id", type = IndexType.Unique),
        @Index(value = "owner", type = IndexType.NonUnique),
        @Index(value = "name", type = IndexType.NonUnique),
        @Index(value = "description", type = IndexType.Fulltext),
        @Index(value = "mainLanguage", type = IndexType.NonUnique),
        @Index(value = "topics", type = IndexType.Fulltext),
        @Index(value = "pluginName", type = IndexType.NonUnique),
        @Index(value = "mainClass", type = IndexType.NonUnique),
        @Index(value = "banned", type = IndexType.NonUnique),
        @Index(value = "qualityScore", type = IndexType.NonUnique)
})
public final class RepoDataBean {
    @Id
    String id;
    String owner;
    String name;
    String description;
    String mainLanguage;
    Date lastUpdateAt;
    String topics;
    int star;
    long iconDownloadID;
    String pluginName;
    String mainClass;
    @JsonSerialize(nullsUsing = NullSerializer.class)
    String[] dependencies;
    @JsonSerialize(nullsUsing = NullSerializer.class)
    String[] softDependencies;
    /**
     * 是否被作者或管理员要求强制下架。
     */
    boolean banned;
    /**
     * 品质得分，此项分数越高越优先展示
     */
    int qualityScore;
    /**
     * 编辑推荐得分，可以用来额外更改插件的品质得分
     */
    int editorRecommendScore;
    Date lastFullIndexTime;

    public void calcQualityScore(@NotNull GHConfig config) {
        var qualityScore = 0;
        qualityScore += star * 100;
        int dayAfterLastUpdate = (int) ((new Date().getTime() - lastUpdateAt.getTime()) / 1000 / 60 / 60 / 24);
        qualityScore -= (int) (dayAfterLastUpdate * 7.14); // 相当于半个月掉一个星
        if (!config.getOrganization().equals(owner) && "JavaScript".equals(mainLanguage)) {
            qualityScore -= 100;
        }
        if (name.toLowerCase().contains("test") && name.length() < 8) {
            qualityScore -= 2000;
        }
        if (description == null) {
            qualityScore -= 200;
        }
        if (description != null && description.toLowerCase().contains("test") && description.length() < 16) {
            qualityScore -= 1000;
        }
        if (topics != null && !topics.isEmpty()) {
            qualityScore += 200;
        }
        if (description != null && description.length() > 32) {
            qualityScore += 100;
        }
        if (banned) {
            qualityScore -= 2000000000;
        }
        qualityScore += editorRecommendScore;
        this.qualityScore = qualityScore;
    }
}
