
package cn.kaziki.nft.turbo.collection.infrastructure.config;

import cn.kaziki.nft.turbo.collection.domain.entity.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.elasticsearch.enable", havingValue = "true")
public class EsIndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    public EsIndexInitializer(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(Collection.class);

            if (!indexOps.exists()) {
                log.info("Elasticsearch index 'nfturbo_collection' does not exist, creating...");
                boolean created = indexOps.createWithMapping();
                if (created) {
                    log.info("Elasticsearch index 'nfturbo_collection' created successfully");
                } else {
                    log.error("Failed to create Elasticsearch index 'nfturbo_collection'");
                }
            } else {
                log.info("Elasticsearch index 'nfturbo_collection' already exists");
            }
        } catch (Exception e) {
            log.error("Error initializing Elasticsearch index", e);
        }
    }
}