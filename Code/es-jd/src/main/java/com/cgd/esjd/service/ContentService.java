package com.cgd.esjd.service;

import com.alibaba.fastjson.JSON;
import com.cgd.esjd.entity.Content;
import com.cgd.esjd.utils.HtmlParseUtil;
import jdk.nashorn.internal.runtime.linker.LinkerCallSite;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.validation.constraints.AssertFalse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Service
public class ContentService {
    @Autowired
    @Qualifier("restHighLevelClient")
    RestHighLevelClient client;

    /**
     * 解析数据 把数据放到集合中
     * @param keywords
     * @return
     */
    public boolean parseContent(String keywords) throws IOException {
        List<Content> contents = HtmlParseUtil.paseJD(keywords);

        //查询的数据放到索引中
        BulkRequest bulkRequest = new BulkRequest();
        //设置超时时间
        //bulkRequest.timeout("10m");
        for (int i = 0; i < contents.size(); i++) {
            bulkRequest.add(new IndexRequest("jd_index").source(JSON.toJSONString(contents.get(i)), XContentType.JSON));
        }
        BulkResponse bulk = client.bulk(bulkRequest, RequestOptions.DEFAULT);



        return !bulk.hasFailures();
    }

    /**
     * 获取数据实现搜索
     * @param keyword
     * @param pageNo
     * @param pageSize
     * @return
     * @throws IOException
     */
    public List<Map<String,Object>> searchPage(String keyword,int pageNo,int pageSize) throws IOException {
        if (pageNo<=1){
            pageNo = 1;
        }
        SearchRequest searchRequest = new SearchRequest("jd_index");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //分页
        sourceBuilder.from(pageNo);
        sourceBuilder.size(pageSize);
        //精准匹配
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("title", keyword);
        sourceBuilder.query(termQueryBuilder);
        sourceBuilder.timeout(new TimeValue(60,TimeUnit.SECONDS));
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        List<Map<String,Object>> list = new CopyOnWriteArrayList<>();
        for (SearchHit documentFields : searchResponse.getHits().getHits()) {
            list.add(documentFields.getSourceAsMap());
        }
        return list;
    }

    /**
     * 高亮
     * @param keyword
     * @param pageNo
     * @param pageSize
     * @return
     * @throws IOException
     */
    public List<Map<String,Object>> searchHighLight(String keyword,int pageNo,int pageSize) throws IOException {
        if (pageNo<=1){
            pageNo = 1;
        }
        SearchRequest searchRequest = new SearchRequest("jd_index");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //分页
        sourceBuilder.from(pageNo);
        sourceBuilder.size(pageSize);
        //精准匹配
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("title", keyword);
        //模糊查询
        WildcardQueryBuilder queryBuilder = QueryBuilders.wildcardQuery("title", "*"+keyword+"*");
       //分词查找
       // BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().should(QueryBuilders.matchQuery("title", keyword).analyzer("ik_smart").operator(Operator.OR));
        sourceBuilder.query(queryBuilder);
        sourceBuilder.timeout(new TimeValue(60,TimeUnit.SECONDS));

        //高亮

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        //是否需要多个字段高亮
        highlightBuilder.requireFieldMatch(true);
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("<span>");
        sourceBuilder.highlighter(highlightBuilder);
        //执行搜索
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        //解析结果
        List<Map<String,Object>> list = new CopyOnWriteArrayList<>();
        for (SearchHit documentFields : searchResponse.getHits().getHits()) {
            //解析高亮的字段
            Map<String, HighlightField> highlightFields = documentFields.getHighlightFields();
            HighlightField title = highlightFields.get("title");
            Map<String, Object> sourceAsMap = documentFields.getSourceAsMap();
            if (title != null){
                Text[] fragments = title.fragments();
                String n_title = "";
                for (Text text : fragments) {
                    n_title+=text;
                }
                sourceAsMap.put("title", n_title);

            }
            list.add(sourceAsMap);
        }
        return list;
    }

}
