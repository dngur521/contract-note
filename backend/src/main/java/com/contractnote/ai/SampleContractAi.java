package com.contractnote.ai;

import com.contractnote.ai.ExtractionResult.FieldEvidence;
import com.contractnote.ai.ExtractionResult.PaymentTerm;
import com.contractnote.ai.ExtractionResult.Transcript;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * API 키가 없을 때 쓰는 가짜 AI. 올린 파일을 읽지 않고 samples/의 가상 고객 결과를 그대로 돌려준다.
 * 화면 개발용이며, 화면에 "샘플 AI"로 표시되므로 시연 영상에는 쓰지 않는다.
 */
public class SampleContractAi implements ContractAi {

    private static final String QUOTE_TRANSCRIPT = """
            견적서 (가상 자료) 2026-10-02
            의뢰인: 하늘디자인 / 프로젝트: 브랜드 소개 영상 편집
            총 금액 3,000,000원 / 계약금 900,000원 (계약 시 지급) / 잔금 2,100,000원 (납품 후 14일 내)
            수정 3회 포함 / 납품 예정일 2026-10-30
            완성 영상의 저작권은 잔금 지급 후 의뢰인에게 넘어갑니다.
            3.3% 원천징수 후 지급""";

    private static final String KAKAO_TRANSCRIPT = """
            2026-10-05 하늘디자인 박담당: 견적 확인했습니다. 다만 납품을 10월 20일로 당길 수 있을까요?
            2026-10-05 나: 가능합니다. 대신 수정은 3번 말고 2번까지만 괜찮을까요?
            2026-10-05 하늘디자인 박담당: 네 좋아요. 수정은 2번까지로 하고 잔금은 납품 후 7일 내에 드릴게요.
            2026-10-05 나: 감사합니다. 계약금 입금 확인되면 시작하겠습니다.""";

    @Override
    public String mode() {
        return "sample";
    }

    @Override
    public ExtractionResult extract(List<UploadedFile> files) {
        int quoteDoc = 1;
        int kakaoDoc = Math.min(2, files.size());
        List<Transcript> transcripts = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            UploadedFile file = files.get(i);
            String text = file.mediaType().startsWith("text/")
                    ? new String(file.bytes(), StandardCharsets.UTF_8)
                    : (i == 0 ? QUOTE_TRANSCRIPT : KAKAO_TRANSCRIPT);
            transcripts.add(new Transcript(i + 1, text));
        }
        return new ExtractionResult(
                "하늘디자인",
                "브랜드 소개 영상 편집",
                "2026-10-05",
                "2026-10-20",
                "",
                3_000_000,
                2,
                "완성 영상의 저작권은 잔금 지급 후 의뢰인에게 넘어감",
                true,
                List.of(
                        new FieldEvidence("clientName", quoteDoc, "의뢰인: 하늘디자인"),
                        new FieldEvidence("projectName", quoteDoc, "프로젝트: 브랜드 소개 영상 편집"),
                        new FieldEvidence("contractDate", kakaoDoc, "네 좋아요. 수정은 2번까지로 하고 잔금은 납품 후 7일 내에 드릴게요."),
                        new FieldEvidence("deliveryDate", kakaoDoc, "납품을 10월 20일로 당길 수 있을까요?"),
                        new FieldEvidence("totalAmount", quoteDoc, "총 금액 3,000,000원"),
                        new FieldEvidence("revisionLimit", kakaoDoc, "수정은 2번까지로 하고"),
                        new FieldEvidence("copyrightOwner", quoteDoc, "완성 영상의 저작권은 잔금 지급 후 의뢰인에게 넘어갑니다."),
                        new FieldEvidence("withholding", quoteDoc, "3.3% 원천징수 후 지급")),
                List.of(
                        new PaymentTerm("계약금", 900_000, "DAYS_AFTER_CONTRACT", "", 0, quoteDoc, "계약금 900,000원 (계약 시 지급)"),
                        new PaymentTerm("잔금", 2_100_000, "DAYS_AFTER_DELIVERY", "", 7, kakaoDoc, "잔금은 납품 후 7일 내에 드릴게요.")),
                List.of(
                        "수정 횟수가 견적서의 3회에서 카카오톡 합의로 2회로 바뀌었습니다. 맞는지 확인해 주세요.",
                        "납품일이 견적서의 10월 30일에서 카카오톡 합의로 10월 20일로 바뀌었습니다. 맞는지 확인해 주세요.",
                        "잔금 지급 기한이 견적서의 납품 후 14일에서 카카오톡 합의로 7일로 바뀌었습니다. 맞는지 확인해 주세요."),
                transcripts);
    }

    @Override
    public AnswerResult answer(String question, List<SourceChunk> sources) {
        return new AnswerResult(
                true,
                "[샘플 AI] 실제 답변을 만들지 않고, 검색으로 가장 가까운 기록을 그대로 보여 줍니다: " + sources.get(0).text(),
                List.of(1));
    }
}
