package com.learn.lld.gramvikash.schemes.service;

import com.learn.lld.gramvikash.schemes.dto.*;
import com.learn.lld.gramvikash.schemes.entity.*;
import com.learn.lld.gramvikash.schemes.repository.SchemeFaqRepository;
import com.learn.lld.gramvikash.schemes.repository.SchemeRepository;
import com.learn.lld.gramvikash.user.entity.Farmer;
import com.learn.lld.gramvikash.user.entity.UserKnownField;
import com.learn.lld.gramvikash.user.repository.FarmerRepository;
import com.learn.lld.gramvikash.user.repository.UserKnownFieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchemeService {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeFaqRepository schemeFaqRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private UserKnownFieldRepository userKnownFieldRepository;

    // ======================== 1. SCHEME CRUD ========================

    @Transactional
    public SchemeDetailResponse createScheme(CreateSchemeRequest request) {
        if (schemeRepository.existsBySchemeCode(request.getSchemeCode())) {
            throw new RuntimeException("Scheme with code '" + request.getSchemeCode() + "' already exists");
        }

        Scheme scheme = new Scheme();
        scheme.setSchemeName(request.getSchemeName());
        scheme.setSchemeCode(request.getSchemeCode());
        scheme.setDescription(request.getDescription());
        scheme.setBenefitDetails(request.getBenefitDetails());
        scheme.setCategory(request.getCategory());
        scheme.setState(request.getState());
        scheme.setCreatedAt(LocalDateTime.now());
        scheme.setUpdatedAt(LocalDateTime.now());

        // Add eligibility groups and rules
        if (request.getEligibilityGroups() != null) {
            for (EligibilityGroupRequest groupReq : request.getEligibilityGroups()) {
                SchemeEligibilityGroup group = new SchemeEligibilityGroup();
                group.setGroupName(groupReq.getGroupName());
                group.setGroupOperator(groupReq.getGroupOperator());
                group.setScheme(scheme);

                if (groupReq.getRules() != null) {
                    for (EligibilityRuleRequest ruleReq : groupReq.getRules()) {
                        SchemeEligibilityRule rule = new SchemeEligibilityRule();
                        rule.setFieldName(ruleReq.getFieldName());
                        rule.setOperator(ruleReq.getOperator());
                        rule.setValue(ruleReq.getValue());
                        rule.setFieldType(ruleReq.getFieldType());
                        rule.setGroup(group);
                        group.getRules().add(rule);
                    }
                }
                scheme.getEligibilityGroups().add(group);
            }
        }

        // Add FAQs
        if (request.getFaqs() != null) {
            for (FaqRequest faqReq : request.getFaqs()) {
                SchemeFaq faq = new SchemeFaq();
                faq.setQuestion(faqReq.getQuestion());
                faq.setAnswer(faqReq.getAnswer());
                faq.setLanguage(faqReq.getLanguage() != null ? faqReq.getLanguage() : "EN");
                faq.setDisplayOrder(faqReq.getDisplayOrder() != null ? faqReq.getDisplayOrder() : 1);
                faq.setScheme(scheme);
                scheme.getFaqs().add(faq);
            }
        }

        Scheme saved = schemeRepository.save(scheme);
        return mapToDetailResponse(saved, null);
    }

    // ======================== 2. BROWSE SCHEMES ========================

    public List<SchemeCardResponse> browseSchemes(String state, String category) {
        List<Scheme> schemes;

        if (state != null && category != null) {
            if ("CENTRAL".equalsIgnoreCase(state)) {
                schemes = schemeRepository.findByStateIsNullAndCategoryAndIsActiveTrue(category);
            } else {
                schemes = schemeRepository.findByStateAndCategoryAndIsActiveTrue(state, category);
            }
        } else if (state != null) {
            if ("CENTRAL".equalsIgnoreCase(state)) {
                schemes = schemeRepository.findByStateIsNullAndIsActiveTrue();
            } else {
                schemes = schemeRepository.findByStateAndIsActiveTrue(state);
            }
        } else if (category != null) {
            schemes = schemeRepository.findByCategoryAndIsActiveTrue(category);
        } else {
            schemes = schemeRepository.findByIsActiveTrue();
        }

        return schemes.stream().map(this::mapToCardResponse).collect(Collectors.toList());
    }

    // ======================== 3. SCHEME DETAIL + FAQ ========================

    public SchemeDetailResponse getSchemeDetail(Long schemeId, String language) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new RuntimeException("Scheme not found with id: " + schemeId));
        return mapToDetailResponse(scheme, language);
    }

    // ======================== 4. SAVE USER KNOWN FIELDS ========================

    @Transactional
    public String saveUserKnownFields(UserFieldRequest request) {
        Farmer farmer = farmerRepository.findById(request.getFarmerId())
                .orElseThrow(() -> new RuntimeException("Farmer not found with id: " + request.getFarmerId()));

        for (UserFieldRequest.FieldEntry entry : request.getFields()) {
            Optional<UserKnownField> existing = userKnownFieldRepository
                    .findByFarmerIdAndFieldName(farmer.getId(), entry.getFieldName());

            if (existing.isPresent()) {
                UserKnownField field = existing.get();
                field.setValue(entry.getValue());
                field.setFieldType(entry.getFieldType());
                userKnownFieldRepository.save(field);
            } else {
                UserKnownField field = new UserKnownField();
                field.setFieldName(entry.getFieldName());
                field.setValue(entry.getValue());
                field.setFieldType(entry.getFieldType());
                field.setFarmer(farmer);
                userKnownFieldRepository.save(field);
            }
        }

        return "Saved " + request.getFields().size() + " known fields for farmer " + farmer.getUserName();
    }

    // ======================== 5. ELIGIBILITY CHECKER (CORE ENGINE) ========================

    public EligibilityResultResponse checkEligibility(EligibilityCheckRequest request) {
        Farmer farmer = farmerRepository.findById(request.getFarmerId())
                .orElseThrow(() -> new RuntimeException("Farmer not found with id: " + request.getFarmerId()));

        String mode = request.getMode() != null ? request.getMode().toUpperCase() : "DISCOVER";

        // Build complete field map from: farmer profile + stored known fields + request additional fields
        Map<String, String> farmerFields = buildFarmerFieldMap(farmer, request.getAdditionalFields());

        List<Scheme> activeSchemes = schemeRepository.findByIsActiveTrue();

        List<EligibilityResultResponse.MatchedScheme> eligible = new ArrayList<>();
        List<EligibilityResultResponse.MatchedScheme> almostEligible = new ArrayList<>();
        List<EligibilityResultResponse.MatchedScheme> ineligible = new ArrayList<>();

        for (Scheme scheme : activeSchemes) {
            SchemeEvaluationResult result = evaluateScheme(scheme, farmerFields, mode);

            EligibilityResultResponse.MatchedScheme matched = EligibilityResultResponse.MatchedScheme.builder()
                    .schemeId(scheme.getId())
                    .schemeName(scheme.getSchemeName())
                    .schemeCode(scheme.getSchemeCode())
                    .category(scheme.getCategory())
                    .benefitSummary(truncate(scheme.getBenefitDetails(), 150))
                    .eligible(result.isEligible())
                    .failedRuleCount(result.getFailedRules().size())
                    .totalRules(result.getTotalRules())
                    .reasonMessage(result.getReasonMessage())
                    .missingFields(result.getMissingFields())
                    .build();

            if (result.isEligible()) {
                eligible.add(matched);
            } else if (result.getFailedRules().size() == 1) {
                // Almost eligible — missed by exactly 1 rule
                SchemeEligibilityRule failedRule = result.getFailedRules().get(0);
                String almostMsg = "You could also qualify for " + scheme.getSchemeName()
                        + " if " + buildSingleRuleHint(failedRule);
                matched.setReasonMessage(almostMsg);
                almostEligible.add(matched);
            } else {
                ineligible.add(matched);
            }
        }

        return EligibilityResultResponse.builder()
                .mode(mode)
                .totalSchemesEvaluated(activeSchemes.size())
                .eligibleSchemes(eligible)
                .almostEligibleSchemes(almostEligible)
                .ineligibleSchemes(ineligible)
                .build();
    }

    // ======================== RULE ENGINE INTERNALS ========================

    private Map<String, String> buildFarmerFieldMap(Farmer farmer, Map<String, String> additionalFields) {
        Map<String, String> fields = new LinkedHashMap<>();

        // Extract from farmer profile — compute age from dob
        if (farmer.getDob() != null) {
            int age = java.time.Period.between(farmer.getDob(), java.time.LocalDate.now()).getYears();
            fields.put("age", String.valueOf(age));
        }
        if (farmer.getState() != null) {
            fields.put("state", farmer.getState().getName());
        }
        if (farmer.getDistrict() != null) {
            fields.put("district", farmer.getDistrict().getName());
        }
        if (farmer.getCrops() != null && !farmer.getCrops().isEmpty()) {
            String cropNames = farmer.getCrops().stream()
                    .map(c -> c.getName())
                    .collect(Collectors.joining(","));
            fields.put("cropType", cropNames);
        }

        // Extract from stored UserKnownFields
        List<UserKnownField> knownFields = userKnownFieldRepository.findByFarmerId(farmer.getId());
        for (UserKnownField kf : knownFields) {
            fields.put(kf.getFieldName(), kf.getValue());
        }

        // Override/add from request additional fields
        if (additionalFields != null) {
            fields.putAll(additionalFields);
        }

        return fields;
    }

    private SchemeEvaluationResult evaluateScheme(Scheme scheme, Map<String, String> farmerFields, String mode) {
        List<SchemeEligibilityRule> allFailedRules = new ArrayList<>();
        List<String> allMissingFields = new ArrayList<>();
        int totalRules = 0;
        boolean allGroupsPass = true;

        for (SchemeEligibilityGroup group : scheme.getEligibilityGroups()) {
            GroupEvaluationResult groupResult = evaluateGroup(group, farmerFields, mode);
            totalRules += groupResult.getTotalRules();
            allFailedRules.addAll(groupResult.getFailedRules());
            allMissingFields.addAll(groupResult.getMissingFields());

            if (!groupResult.isPassed()) {
                allGroupsPass = false;
            }
        }

        String reason = "";
        if (!allGroupsPass && !allFailedRules.isEmpty()) {
            reason = buildNaturalLanguageReason(allFailedRules);
        }

        SchemeEvaluationResult result = new SchemeEvaluationResult();
        result.setEligible(allGroupsPass);
        result.setTotalRules(totalRules);
        result.setFailedRules(allFailedRules);
        result.setMissingFields(allMissingFields);
        result.setReasonMessage(allGroupsPass ? "You are fully eligible for this scheme." : reason);
        return result;
    }

    private GroupEvaluationResult evaluateGroup(SchemeEligibilityGroup group, Map<String, String> farmerFields, String mode) {
        List<SchemeEligibilityRule> failedRules = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();
        int totalRules = group.getRules().size();
        boolean isAnd = "AND".equalsIgnoreCase(group.getGroupOperator());

        boolean groupPassed;
        if (isAnd) {
            // AND: all rules must pass
            groupPassed = true;
            for (SchemeEligibilityRule rule : group.getRules()) {
                String fieldValue = farmerFields.get(rule.getFieldName());
                if (fieldValue == null) {
                    if ("DISCOVER".equalsIgnoreCase(mode)) {
                        // Skip missing fields in discover mode
                        missingFields.add(rule.getFieldName());
                        continue;
                    } else {
                        // VERIFY mode: missing field = fail
                        failedRules.add(rule);
                        groupPassed = false;
                    }
                } else if (!evaluateRule(rule, fieldValue)) {
                    failedRules.add(rule);
                    groupPassed = false;
                }
            }
        } else {
            // OR: at least one rule must pass
            groupPassed = false;
            for (SchemeEligibilityRule rule : group.getRules()) {
                String fieldValue = farmerFields.get(rule.getFieldName());
                if (fieldValue == null) {
                    if ("DISCOVER".equalsIgnoreCase(mode)) {
                        missingFields.add(rule.getFieldName());
                    } else {
                        failedRules.add(rule);
                    }
                } else if (evaluateRule(rule, fieldValue)) {
                    groupPassed = true;
                    break;  // one pass is enough for OR
                } else {
                    failedRules.add(rule);
                }
            }
            // In discover mode, if no rule passed but some were skipped, consider group as tentatively passed
            if (!groupPassed && "DISCOVER".equalsIgnoreCase(mode) && !missingFields.isEmpty()
                    && failedRules.isEmpty()) {
                groupPassed = true;
            }
        }

        GroupEvaluationResult result = new GroupEvaluationResult();
        result.setPassed(groupPassed);
        result.setTotalRules(totalRules);
        result.setFailedRules(failedRules);
        result.setMissingFields(missingFields);
        return result;
    }

    private boolean evaluateRule(SchemeEligibilityRule rule, String fieldValue) {
        String ruleValue = rule.getValue();
        String operator = rule.getOperator();
        String fieldType = rule.getFieldType();

        try {
            switch (fieldType.toUpperCase()) {
                case "NUMBER":
                    double actual = Double.parseDouble(fieldValue);
                    double expected = Double.parseDouble(ruleValue);
                    return switch (operator) {
                        case "=" -> actual == expected;
                        case ">" -> actual > expected;
                        case "<" -> actual < expected;
                        case ">=" -> actual >= expected;
                        case "<=" -> actual <= expected;
                        case "!=" -> actual != expected;
                        default -> false;
                    };

                case "STRING":
                    return switch (operator) {
                        case "=" -> fieldValue.equalsIgnoreCase(ruleValue);
                        case "!=" -> !fieldValue.equalsIgnoreCase(ruleValue);
                        case "IN" -> {
                            // fieldValue can be comma-separated (e.g., crops: "Paddy,Wheat")
                            // ruleValue can also be comma-separated
                            String[] ruleValues = ruleValue.split(",");
                            String[] fieldValues = fieldValue.split(",");
                            boolean found = false;
                            for (String rv : ruleValues) {
                                for (String fv : fieldValues) {
                                    if (fv.trim().equalsIgnoreCase(rv.trim())) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) break;
                            }
                            yield found;
                        }
                        default -> false;
                    };

                case "BOOLEAN":
                    boolean actualBool = Boolean.parseBoolean(fieldValue);
                    boolean expectedBool = Boolean.parseBoolean(ruleValue);
                    return switch (operator) {
                        case "=" -> actualBool == expectedBool;
                        case "!=" -> actualBool != expectedBool;
                        default -> false;
                    };

                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ======================== NATURAL LANGUAGE REASON BUILDER ========================

    private String buildNaturalLanguageReason(List<SchemeEligibilityRule> failedRules) {
        if (failedRules.isEmpty()) return "";

        List<String> reasons = new ArrayList<>();
        for (SchemeEligibilityRule rule : failedRules) {
            reasons.add(buildSingleRuleHint(rule));
        }

        if (reasons.size() == 1) {
            return "You need " + reasons.get(0) + " to qualify.";
        }

        // Join with "and" for multiple reasons
        String joined = String.join(" and ", reasons);
        return "You need " + joined + " to qualify.";
    }

    private String buildSingleRuleHint(SchemeEligibilityRule rule) {
        String fieldLabel = humanizeFieldName(rule.getFieldName());
        String op = rule.getOperator();
        String val = rule.getValue();

        return switch (op) {
            case ">=" -> fieldLabel + " of at least " + formatValue(val, rule.getFieldType());
            case ">" -> fieldLabel + " greater than " + formatValue(val, rule.getFieldType());
            case "<=" -> fieldLabel + " of at most " + formatValue(val, rule.getFieldType());
            case "<" -> fieldLabel + " less than " + formatValue(val, rule.getFieldType());
            case "=" -> fieldLabel + " to be " + formatValue(val, rule.getFieldType());
            case "!=" -> fieldLabel + " not equal to " + formatValue(val, rule.getFieldType());
            case "IN" -> fieldLabel + " to be one of: " + val;
            default -> fieldLabel + " " + op + " " + val;
        };
    }

    private String humanizeFieldName(String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "age" -> "age";
            case "landsize" -> "land size";
            case "income" -> "annual income";
            case "isbpl" -> "BPL status";
            case "minority" -> "minority status";
            case "gender" -> "gender";
            case "croptype" -> "crop type";
            case "state" -> "state";
            case "district" -> "district";
            default -> fieldName;
        };
    }

    private String formatValue(String value, String fieldType) {
        if ("NUMBER".equalsIgnoreCase(fieldType)) {
            try {
                double num = Double.parseDouble(value);
                if (num >= 100000) {
                    return "₹" + String.format("%.1f", num / 100000) + "L";
                } else if (num >= 1000) {
                    return "₹" + String.format("%.0f", num);
                }
                return value;
            } catch (NumberFormatException e) {
                return value;
            }
        }
        return value;
    }

    // ======================== MAPPERS ========================

    private SchemeCardResponse mapToCardResponse(Scheme scheme) {
        return SchemeCardResponse.builder()
                .id(scheme.getId())
                .schemeName(scheme.getSchemeName())
                .schemeCode(scheme.getSchemeCode())
                .category(scheme.getCategory())
                .state(scheme.getState() == null ? "Central" : scheme.getState())
                .benefitSummary(truncate(scheme.getBenefitDetails(), 150))
                .isActive(scheme.getIsActive())
                .build();
    }

    private SchemeDetailResponse mapToDetailResponse(Scheme scheme, String language) {
        // Filter FAQs by language if provided
        List<SchemeFaq> faqs;
        if (language != null && !language.isEmpty()) {
            faqs = schemeFaqRepository.findBySchemeIdAndLanguageAndIsActiveTrueOrderByDisplayOrderAsc(
                    scheme.getId(), language.toUpperCase());
        } else {
            faqs = scheme.getFaqs() != null ? scheme.getFaqs() : List.of();
        }

        return SchemeDetailResponse.builder()
                .id(scheme.getId())
                .schemeName(scheme.getSchemeName())
                .schemeCode(scheme.getSchemeCode())
                .description(scheme.getDescription())
                .benefitDetails(scheme.getBenefitDetails())
                .category(scheme.getCategory())
                .state(scheme.getState() == null ? "Central" : scheme.getState())
                .isActive(scheme.getIsActive())
                .createdAt(scheme.getCreatedAt())
                .eligibilityGroups(scheme.getEligibilityGroups().stream().map(g ->
                        SchemeDetailResponse.EligibilityGroupDetail.builder()
                                .groupName(g.getGroupName())
                                .groupOperator(g.getGroupOperator())
                                .rules(g.getRules().stream().map(r ->
                                        SchemeDetailResponse.RuleDetail.builder()
                                                .fieldName(r.getFieldName())
                                                .operator(r.getOperator())
                                                .value(r.getValue())
                                                .fieldType(r.getFieldType())
                                                .build()
                                ).collect(Collectors.toList()))
                                .build()
                ).collect(Collectors.toList()))
                .faqs(faqs.stream().map(f ->
                        SchemeDetailResponse.FaqDetail.builder()
                                .id(f.getId())
                                .question(f.getQuestion())
                                .answer(f.getAnswer())
                                .language(f.getLanguage())
                                .displayOrder(f.getDisplayOrder())
                                .build()
                ).collect(Collectors.toList()))
                .build();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    // ======================== INTERNAL RESULT CLASSES ========================

    private static class SchemeEvaluationResult {
        private boolean eligible;
        private int totalRules;
        private List<SchemeEligibilityRule> failedRules = new ArrayList<>();
        private List<String> missingFields = new ArrayList<>();
        private String reasonMessage;

        public boolean isEligible() { return eligible; }
        public void setEligible(boolean eligible) { this.eligible = eligible; }
        public int getTotalRules() { return totalRules; }
        public void setTotalRules(int totalRules) { this.totalRules = totalRules; }
        public List<SchemeEligibilityRule> getFailedRules() { return failedRules; }
        public void setFailedRules(List<SchemeEligibilityRule> failedRules) { this.failedRules = failedRules; }
        public List<String> getMissingFields() { return missingFields; }
        public void setMissingFields(List<String> missingFields) { this.missingFields = missingFields; }
        public String getReasonMessage() { return reasonMessage; }
        public void setReasonMessage(String reasonMessage) { this.reasonMessage = reasonMessage; }
    }

    private static class GroupEvaluationResult {
        private boolean passed;
        private int totalRules;
        private List<SchemeEligibilityRule> failedRules = new ArrayList<>();
        private List<String> missingFields = new ArrayList<>();

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public int getTotalRules() { return totalRules; }
        public void setTotalRules(int totalRules) { this.totalRules = totalRules; }
        public List<SchemeEligibilityRule> getFailedRules() { return failedRules; }
        public void setFailedRules(List<SchemeEligibilityRule> failedRules) { this.failedRules = failedRules; }
        public List<String> getMissingFields() { return missingFields; }
        public void setMissingFields(List<String> missingFields) { this.missingFields = missingFields; }
    }
}
