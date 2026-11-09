package com.example.statementvalidator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StatementPipeline {
    private final Client client;
    private final ObjectMapper mapper;
    private final String model;

    public StatementPipeline(Client client, ObjectMapper mapper, @Value("${vertex.model}") String model) {
        this.client = client;
        this.mapper = mapper;
        this.model = model;
    }

    public Map<String, Object> process(MultipartFile file, String expectedAccount, String expectedOwner, String expectedPeriod) throws Exception {
        byte[] bytes = file.getBytes();
        String mime = file.getContentType();
        Part document = Part.fromBytes(bytes, mime);
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> fileCheck = ask(document,
                "Stage 1 — file validation. Decide whether this document is a genuine trading/brokerage account statement. " +
                "Do not accept generic financial documents, invoices, bank statements, or unrelated files. " +
                "Return only JSON: {\"valid\": boolean, \"reason\": string, \"evidence\": string}.");
        result.put("fileValidation", fileCheck);
        if (!yes(fileCheck, "valid")) return stopped(result, "File is not a recognized trading statement; later stages were skipped.");

        String accountPrompt = "Stage 2 — account validation. Read the statement's account number/identifier, account owner, " +
                "and statement period. Compare it to the expected account information below. Mask identifiers in your response " +
                "except for the last four characters. Mark valid=true only when at least one provided expected value can be matched to the statement; otherwise valid=false. " +
                "Expected account: " + safe(expectedAccount) + ". Expected owner: " + safe(expectedOwner) + ". Expected statement period: " + safe(expectedPeriod) + ". " +
                "Return only JSON: {\"valid\": boolean, \"accountNumberMasked\": string|null, \"owner\": string|null, " +
                "\"period\": string|null, \"reason\": string}.";
        Map<String, Object> accountCheck = ask(document, accountPrompt);
        result.put("accountValidation", accountCheck);
        if (!yes(accountCheck, "valid")) return stopped(result, "Account validation failed; trade identification and extraction were skipped.");

        Map<String, Object> tradeCheck = ask(document,
                "Stage 3 — trade identification. Determine whether this statement contains one or more executed trades " +
                "for the account. Do not count open positions, balances, cash movements, or orders that were not executed. " +
                "Return only JSON: {\"hasTrades\": boolean, \"tradeCountEstimate\": integer, \"reason\": string}.");
        result.put("tradeIdentification", tradeCheck);
        if (!yes(tradeCheck, "hasTrades")) return stopped(result, "No executed trades were identified; extraction was skipped.");

        Map<String, Object> extraction = ask(document,
                "Stage 4 — extract the account details and every executed trade visible in this statement. " +
                "Keep source values as printed; use null for missing values and never infer. Include dates, instrument/symbol, " +
                "side, quantity, price, fees, currency, and trade/settlement date when available. " +
                "Return only JSON with this shape: {\"account\": {\"accountNumberMasked\": string|null, " +
                "\"owner\": string|null, \"period\": string|null, \"currency\": string|null}, " +
                "\"trades\": [{\"tradeDate\": string|null, \"settlementDate\": string|null, " +
                "\"instrument\": string|null, \"symbol\": string|null, \"side\": string|null, " +
                "\"quantity\": number|null, \"price\": number|null, \"fees\": number|null, " +
                "\"currency\": string|null, \"sourceText\": string|null}], " +
                "\"warnings\": [string]}.");
        result.put("extraction", extraction);
        result.put("status", "completed");
        return result;
    }

    private Map<String, Object> ask(Part document, String prompt) throws Exception {
        Content content = Content.fromParts(document, Part.fromText(prompt));
        GenerateContentResponse response = client.models.generateContent(model, content, null);
        String text = response.text();
        if (text == null || text.isBlank()) throw new IllegalStateException("Vertex returned an empty response.");
        String json = text.trim().replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
        return mapper.readValue(json, new TypeReference<>() {});
    }

    private boolean yes(Map<String, Object> map, String key) { return Boolean.TRUE.equals(map.get(key)); }
    private String safe(String s) { return s == null || s.isBlank() ? "not provided" : s.replaceAll("[\\r\\n]", " "); }
    private Map<String, Object> stopped(Map<String, Object> result, String message) {
        result.put("status", "stopped"); result.put("message", message); return result;
    }
}


