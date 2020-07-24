package uk.nhs.cactus.common.elasticsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ElasticSearchClient {

  @Value("${es.audit}")
  private String endpoint;

  private final ElasticRestClientFactory clientFactory;
  private final ObjectMapper objectMapper;

  public List<SearchHit> search(String index, SearchSourceBuilder source) throws IOException {
    var request = Requests.searchRequest(index).source(source);

    var response = clientFactory.highLevelClient(endpoint)
        .search(request, RequestOptions.DEFAULT);
    return Arrays.asList(response.getHits().getHits());
  }

  public void store(String index, Object source, Map<String, ? extends Serializable> additionalProperties)
      throws IOException {
    var typedReference = new TypeReference<Map<String, Object>>() {};
    var sourceMap = objectMapper.<Map<String, Object>>convertValue(source, typedReference);

    for (var entry : additionalProperties.entrySet()) {
      sourceMap.merge(entry.getKey(), entry.getValue().toString(), (v1, v2) -> v1);
    }

    var request = Requests.indexRequest(index).source(sourceMap);

    clientFactory.highLevelClient(endpoint).index(request, RequestOptions.DEFAULT);
  }
}
