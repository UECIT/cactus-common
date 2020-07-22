package uk.nhs.cactus.common.elasticsearch;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Profile("!dev")
@RequiredArgsConstructor
public class ElasticSearchClient {

  @Value("${es.audit}")
  private String endpoint;

  private final ElasticRestClientFactory clientFactory;

  public List<SearchHit> search(String index, SearchSourceBuilder source) throws IOException {
    var request = new SearchRequest()
        .indices(index)
        .source(source);

    log.info("Sending ElasticSearch request to index " + index + ":");
    log.info(request.toString());

    var response = clientFactory.highLevelClient(endpoint)
        .search(request, RequestOptions.DEFAULT);
    return Arrays.asList(response.getHits().getHits());
  }
}
