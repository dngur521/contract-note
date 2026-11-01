CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE contract (
    id              BIGSERIAL PRIMARY KEY,
    client_name     VARCHAR(200),
    project_name    VARCHAR(200),
    contract_date   DATE,
    delivery_date   DATE,
    total_amount    BIGINT,
    revision_limit  INTEGER,
    copyright_owner VARCHAR(200),
    withholding     BOOLEAN NOT NULL DEFAULT FALSE,
    status          VARCHAR(20) NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE TABLE document (
    id          BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contract (id) ON DELETE CASCADE,
    file_name   VARCHAR(300) NOT NULL,
    media_type  VARCHAR(100) NOT NULL,
    content     BYTEA NOT NULL,
    transcript  TEXT
);

-- 계약 항목이 어느 기록의 어느 문장에서 나왔는지
CREATE TABLE evidence (
    id          BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contract (id) ON DELETE CASCADE,
    document_id BIGINT REFERENCES document (id) ON DELETE SET NULL,
    field       VARCHAR(50) NOT NULL,
    quote       TEXT NOT NULL
);

-- AI는 규칙(rule_type, fixed_date, offset_days)만 뽑고 실제 날짜는 코드가 계산한다
CREATE TABLE payment (
    id          BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contract (id) ON DELETE CASCADE,
    document_id BIGINT REFERENCES document (id) ON DELETE SET NULL,
    label       VARCHAR(100) NOT NULL,
    amount      BIGINT,
    rule_type   VARCHAR(30) NOT NULL,
    fixed_date  DATE,
    offset_days INTEGER,
    paid        BOOLEAN NOT NULL DEFAULT FALSE,
    quote       TEXT
);

-- RAG 검색용 문장 묶음. 384는 임베딩 모델의 벡터 크기
CREATE TABLE chunk (
    id          BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contract (id) ON DELETE CASCADE,
    document_id BIGINT NOT NULL REFERENCES document (id) ON DELETE CASCADE,
    text        TEXT NOT NULL,
    embedding   VECTOR(384) NOT NULL
);
CREATE INDEX chunk_embedding_idx ON chunk USING hnsw (embedding vector_cosine_ops);
