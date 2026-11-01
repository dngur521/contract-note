-- 검수일: "검수 후 N일" 지급 조건의 기준 날짜
ALTER TABLE contract ADD COLUMN acceptance_date DATE;
