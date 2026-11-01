package com.contractnote.rag;

import com.contractnote.ai.SourceChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** pgvector 연산자(<=>: 코사인 거리)를 써야 해서 JPA 대신 SQL을 직접 쓴다. */
@Repository
public class ChunkRepository {

    private final JdbcClient jdbc;

    public ChunkRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(long contractId, long documentId, String text, float[] embedding) {
        jdbc.sql("INSERT INTO chunk (contract_id, document_id, text, embedding) VALUES (?, ?, ?, ?::vector)")
                .params(contractId, documentId, text, toVectorLiteral(embedding))
                .update();
    }

    /** 질문과 가장 가까운 문장 묶음을 찾는다. contractId가 있으면 그 계약 안에서만 찾는다. */
    public List<SourceChunk> search(float[] query, Long contractId, int limit) {
        String vector = toVectorLiteral(query);
        return jdbc.sql("""
                        SELECT ch.id, ch.contract_id, ch.document_id, co.client_name, d.file_name, ch.text,
                               1 - (ch.embedding <=> ?::vector) AS score
                        FROM chunk ch
                        JOIN contract co ON co.id = ch.contract_id
                        JOIN document d ON d.id = ch.document_id
                        WHERE (?::bigint IS NULL OR ch.contract_id = ?::bigint)
                        ORDER BY ch.embedding <=> ?::vector
                        LIMIT ?
                        """)
                .params(vector, contractId, contractId, vector, limit)
                .query((rs, n) -> new SourceChunk(rs.getLong("id"), rs.getLong("contract_id"), rs.getLong("document_id"),
                        rs.getString("client_name"), rs.getString("file_name"), rs.getString("text"), rs.getDouble("score")))
                .list();
    }

    /** 찾은 문장의 앞뒤 문장까지 합쳐 한 덩어리로 만든다. 문장 하나만으로는 맥락이 끊기기 때문이다. */
    public List<SourceChunk> withNeighbors(List<SourceChunk> hits, int window) {
        List<SourceChunk> passages = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (SourceChunk hit : hits) {
            if (used.contains(hit.id())) {
                continue;
            }
            List<Row> rows = jdbc.sql("""
                            SELECT id, text FROM chunk
                            WHERE document_id = ? AND id BETWEEN ? AND ?
                            ORDER BY id
                            """)
                    .params(hit.documentId(), hit.id() - window, hit.id() + window)
                    .query((rs, n) -> new Row(rs.getLong("id"), rs.getString("text")))
                    .list();
            StringBuilder text = new StringBuilder();
            for (Row row : rows) {
                if (used.add(row.id())) {
                    text.append(text.isEmpty() ? "" : "\n").append(row.text());
                }
            }
            passages.add(new SourceChunk(hit.id(), hit.contractId(), hit.documentId(), hit.clientName(), hit.fileName(),
                    text.toString(), hit.score()));
        }
        // 문서 안의 원래 순서대로 읽도록 정렬한다.
        passages.sort(Comparator.comparingLong(SourceChunk::documentId).thenComparingLong(SourceChunk::id));
        return passages;
    }

    private record Row(long id, String text) {}

    private static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
