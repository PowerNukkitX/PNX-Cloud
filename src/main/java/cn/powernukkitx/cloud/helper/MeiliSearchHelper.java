package cn.powernukkitx.cloud.helper;

import cn.powernukkitx.cloud.bean.RepoDataBean;
import cn.powernukkitx.cloud.config.SearchConfig;
import cn.powernukkitx.cloud.config.SecretConfig;
import cn.powernukkitx.cloud.util.ArrayUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.json.JacksonJsonHandler;
import com.meilisearch.sdk.model.Searchable;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.TypoTolerance;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

@Singleton
public class MeiliSearchHelper {
    private final SecretConfig secretConfig;
    private final SearchConfig searchConfig;
    private final DBHelper dbHelper;
    private final ObjectMapper jacksonMapper;
    private Client meiliClient = null;

    public MeiliSearchHelper(@NotNull SecretConfig secretConfig, @NotNull SearchConfig searchConfig, @NotNull DBHelper dbHelper) {
        this.secretConfig = secretConfig;
        this.searchConfig = searchConfig;
        this.dbHelper = dbHelper;
        this.jacksonMapper = new ObjectMapper();
        this.jacksonMapper.getSerializerProvider().setNullValueSerializer(NullSerializer.instance);
    }

    public @NotNull Client getMeiliClient() {
        if (meiliClient == null) {
            meiliClient = new Client(new Config(secretConfig.getMeiliSearch().getUrl(), secretConfig.getMeiliSearch().getKey(),
                    new JacksonJsonHandler()));
        }
        return meiliClient;
    }

    public void tryInit() throws MeilisearchException {
        var indexes = getMeiliClient().getIndexes();
        if (!ArrayUtil.contains(Arrays.stream(indexes.getResults()).map(Index::getUid).toArray(), "plugins")) {
            var task = getMeiliClient().createIndex("plugins", "id");
            getMeiliClient().waitForTask(task.getTaskUid());
        }
        var pluginIndex = getMeiliClient().index("plugins");
        pluginIndex.updateTypoToleranceSettings(new TypoTolerance().setEnabled(true).setMinWordSizeForTypos(
                new HashMap<>(Map.of("oneTypo", 4, "twoTypos", 7))
        ));
    }

    static Pattern SingleHumpNamingPattern = Pattern.compile("([A-Z][a-z]+)([A-Z][a-z]+[A-Z]?)([A-Z][a-z0-9]+[A-Z]?)?$");

    public int syncPluginData() throws MeilisearchException {
        var repoDataBeans = dbHelper.getRepoDataBeanObjectRepository();
        var index = getMeiliClient().index("plugins");
        var pluginList = repoDataBeans.find().toList();
        var synonyms = new HashMap<String, String[]>();
        try {
            for (var each : pluginList) {
                var matcher = SingleHumpNamingPattern.matcher(each.getName());
                if (matcher.matches()) {
                    var count = matcher.groupCount();
                    var synonymsArray = new ArrayList<String>(3);
                    for (int i = 0; i < count; i++) {
                        var g = matcher.group(i + 1);
                        if (g != null && !g.isBlank())
                            synonymsArray.add(g);
                    }
                    synonyms.put(each.getName(), synonymsArray.toArray(String[]::new));
                    for (var eachSynonym : synonymsArray) {
                        if (eachSynonym == null || eachSynonym.isBlank()) continue;
                        if (synonyms.containsKey(eachSynonym)) {
                            var oldArray = synonyms.get(eachSynonym);
                            var newArray = new String[oldArray.length + 1];
                            System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
                            newArray[oldArray.length] = each.getName();
                            synonyms.put(eachSynonym, newArray);
                        } else {
                            synonyms.put(eachSynonym, new String[]{each.getName()});
                        }
                    }
                }
            }
            for (var entry : searchConfig.getSynonyms().entrySet()) {
                if (entry.getKey().contains("-")) {
                    var key = entry.getKey().replace("-", "");
                    synonyms.put(key, entry.getValue());
                } else {
                    synonyms.put(entry.getKey(), entry.getValue());
                }
            }
            index.updateSynonymsSettings(synonyms);
            return index.updateDocuments(jacksonMapper.writeValueAsString(pluginList
                    .stream().map(RepoDataBean::toSearchFriendlyBean).toArray()), "id").getTaskUid();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Searchable search(@NotNull String keywords, int size, String order) throws MeilisearchException {
        var index = getMeiliClient().index("plugins");
        return index.search(new SearchRequest(keywords).setHitsPerPage(size).setPage(0).setSort(new String[]{
                "qualityScore:" + order, "star:" + order, "lastUpdateAt:" + order
        }));
    }
}
