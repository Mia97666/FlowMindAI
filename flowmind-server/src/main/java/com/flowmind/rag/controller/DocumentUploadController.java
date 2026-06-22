package com.flowmind.rag.controller;

import com.flowmind.rag.entity.KnowledgeChunk;
import com.flowmind.rag.entity.KnowledgeDocument;
import com.flowmind.rag.repository.KnowledgeChunkRepository;
import com.flowmind.rag.repository.KnowledgeDocumentRepository;
import com.flowmind.rag.service.impl.ChunkService;
import com.flowmind.rag.service.impl.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocumentUploadController {

    private final ChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    @Value("${flowmind.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        validateFileType(originalFilename);

        Path uploadPath = Path.of(uploadDir);
        Files.createDirectories(uploadPath);

        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        Path targetPath = uploadPath.resolve(storedFilename);

        file.transferTo(targetPath);

        String content = Files.readString(targetPath, StandardCharsets.UTF_8);

        List<String> chunks = chunkService.split(content);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setOriginalFilename(originalFilename);
        document.setStoredFilename(storedFilename);
        document.setFilePath(targetPath.toString());
        document.setFileSize(file.getSize());
        document.setChunkCount(chunks.size());
        document.setCreatedAt(LocalDateTime.now());

        document = documentRepository.save(document);

        List<String> ids = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            TextSegment segment = TextSegment.from(chunk);
            Embedding embedding = embeddingService.embed(chunk);
            String vectorId = embeddingStore.add(embedding, segment);

            KnowledgeChunk knowledgeChunk = new KnowledgeChunk();
            knowledgeChunk.setDocumentId(document.getId());
            knowledgeChunk.setChunkIndex(i);
            knowledgeChunk.setContent(chunk);
            knowledgeChunk.setVectorId(vectorId);
            knowledgeChunk.setCreatedAt(LocalDateTime.now());

            chunkRepository.save(knowledgeChunk);

            ids.add(vectorId);
        }
        return Map.of(
                "documentId", document.getId(),
                "fileName", originalFilename,
                "storedFileName", storedFilename,
                "chunkCount", chunks.size(),
                "vectorIds", ids,
                "message", "文档上传并入库成功"
        );
    }

    @GetMapping
    public List<KnowledgeDocument> listDocuments() {
        return documentRepository.findAll();
    }

    /**
     * 分页查询知识文档。
     */
    @GetMapping("/page")
    public Page<KnowledgeDocument> pageDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        Specification<KnowledgeDocument> spec = (root, query, cb) -> {
            if (keyword != null && !keyword.isBlank()) {
                return cb.like(cb.lower(root.get("originalFilename")),
                        "%" + keyword.toLowerCase() + "%");
            }
            return null;
        };
        return documentRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{documentId}/chunks")
    public List<KnowledgeChunk> listChunks(@PathVariable Long documentId) {
        return chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
    }

    /**
     * 分页查询文档 Chunk。
     */
    @GetMapping("/{documentId}/chunks/page")
    public Page<KnowledgeChunk> pageChunks(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        Specification<KnowledgeChunk> spec = (root, query, cb) ->
                cb.equal(root.get("documentId"), documentId);
        return chunkRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "chunkIndex")));
    }

    @DeleteMapping("/{documentId}")
    public Map<String, Object> deleteDocument(@PathVariable Long documentId) throws Exception {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("知识文档不存在：" + documentId));

        List<KnowledgeChunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        List<String> vectorIds = chunks.stream()
                .map(KnowledgeChunk::getVectorId)
                .filter(vectorId -> vectorId != null && !vectorId.isBlank())
                .toList();

        if (!vectorIds.isEmpty()) {
            embeddingStore.removeAll(vectorIds);
        }

        chunkRepository.deleteAll(chunks);
        documentRepository.delete(document);

        if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
            Files.deleteIfExists(Path.of(document.getFilePath()));
        }

        return Map.of(
                "documentId", documentId,
                "deletedChunks", chunks.size(),
                "deletedVectors", vectorIds.size(),
                "message", "知识文档已删除"
        );
    }

    private void validateFileType(String filename) {
        String lower = filename.toLowerCase();

        if (!lower.endsWith(".txt") && !lower.endsWith(".md")) {
            throw new IllegalArgumentException("当前仅支持 txt 和 md 文件");
        }
    }
}
