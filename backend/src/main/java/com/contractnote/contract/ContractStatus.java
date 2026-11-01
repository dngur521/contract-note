package com.contractnote.contract;

public enum ContractStatus {
    /** AI가 정리했고 사용자 확인을 기다리는 상태 */
    DRAFT,
    /** 사용자가 확인한 상태. 이때부터 일정에 나타난다 */
    CONFIRMED
}
