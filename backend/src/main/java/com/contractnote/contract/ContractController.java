package com.contractnote.contract;

import com.contractnote.ai.ContractAi;
import com.contractnote.ai.UploadedFile;
import com.contractnote.common.BadRequestException;
import com.contractnote.common.NotFoundException;
import com.contractnote.contract.ContractDtos.*;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ContractController {

    private static final List<String> ALLOWED = List.of("application/pdf", "image/png", "image/jpeg", "text/plain");

    private final ContractService service;
    private final DocumentRepository documents;
    private final ContractAi ai;

    public ContractController(ContractService service, DocumentRepository documents, ContractAi ai) {
        this.service = service;
        this.documents = documents;
        this.ai = ai;
    }

    @GetMapping("/meta")
    public Map<String, String> meta() {
        return Map.of("aiMode", ai.mode());
    }

    @PostMapping("/contracts/extract")
    public ContractView extract(@RequestParam("files") List<MultipartFile> files) throws IOException {
        List<UploadedFile> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            String type = file.getContentType() == null ? "" : file.getContentType().split(";")[0];
            if (!ALLOWED.contains(type)) {
                throw new BadRequestException("PDF, PNG, JPG, TXT 파일만 올릴 수 있습니다: " + file.getOriginalFilename());
            }
            if (file.isEmpty()) {
                throw new BadRequestException("빈 파일은 올릴 수 없습니다: " + file.getOriginalFilename());
            }
            uploaded.add(new UploadedFile(file.getOriginalFilename(), type, file.getBytes()));
        }
        if (uploaded.isEmpty()) {
            throw new BadRequestException("파일을 한 개 이상 올려 주세요.");
        }
        return service.extract(uploaded);
    }

    @GetMapping("/contracts")
    public List<ContractView> list() {
        return service.list();
    }

    @GetMapping("/contracts/{id}")
    public ContractView get(@PathVariable long id) {
        return service.get(id);
    }

    @PutMapping("/contracts/{id}")
    public ContractView update(@PathVariable long id, @Valid @RequestBody ContractUpdate request) {
        return service.update(id, request);
    }

    @DeleteMapping("/contracts/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/payments/{id}")
    public ResponseEntity<Void> setPaid(@PathVariable long id, @RequestBody PaidUpdate request) {
        service.setPaid(id, request.paid());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/schedule")
    public List<ScheduleItem> schedule() {
        return service.schedule();
    }

    @GetMapping("/documents/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable long id) {
        ContractDocument document = documents.findById(id)
                .orElseThrow(() -> new NotFoundException("문서를 찾을 수 없습니다."));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMediaType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(document.getContent());
    }
}
