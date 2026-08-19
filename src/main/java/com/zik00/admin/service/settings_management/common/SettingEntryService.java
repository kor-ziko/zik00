package com.zik00.admin.service.settings_management.common;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.zik00.admin.domain.settings_management.common.SettingEntry;
import com.zik00.admin.dto.settings_management.common.SettingEntryRequest;
import com.zik00.admin.dto.settings_management.common.SettingEntryResponse;
import com.zik00.admin.repository.settings_management.common.SettingEntryRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class SettingEntryService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{8,20}$");
    private static final Pattern KOREAN_POSTAL_CODE = Pattern.compile("^\\d{5}$");
    private final SettingEntryRepository repository;
    private final ObjectMapper objectMapper;
    public SettingEntryService(SettingEntryRepository repository, ObjectMapper objectMapper){this.repository=repository;this.objectMapper=objectMapper;}
    public List<SettingEntryResponse> findAll(String type){return repository.findByTypeOrderByDisplayOrderAscIdAsc(type).stream().map(this::response).toList();}
    @Transactional public SettingEntryResponse create(String type, SettingEntryRequest request, boolean singleton){
        if(singleton&&!repository.findByTypeOrderByDisplayOrderAscIdAsc(type).isEmpty())throw bad("이미 등록된 설정이 있습니다.");
        validate(type, request);
        validateCode(type, request.code(), 0L);
        return response(repository.save(new SettingEntry(type,clean(request.code()),clean(request.name()),nullable(request.content()),json(request.fields()),request.displayOrder(),request.active())));
    }
    @Transactional public SettingEntryResponse update(String type,long id,SettingEntryRequest request){
        SettingEntry item=find(type,id);validate(type, request);validateCode(type,request.code(),id);
        item.update(type,clean(request.code()),clean(request.name()),nullable(request.content()),json(request.fields()),request.displayOrder(),request.active());return response(item);
    }
    @Transactional public void delete(String type,long id){repository.delete(find(type,id));}
    private SettingEntry find(String type,long id){SettingEntry item=repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"설정을 찾을 수 없습니다."));if(!type.equals(item.getType()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"설정을 찾을 수 없습니다.");return item;}
    private void validateCode(String type,String code,long id){if(repository.existsByTypeAndCodeAndIdNot(type,clean(code),id))throw bad("같은 코드가 이미 사용 중입니다.");}
    private void validate(String type, SettingEntryRequest request) {
        Map<String, String> fields = request.fields() == null ? Map.of() : request.fields();
        switch (type) {
            case "SIGNUP_MAIL" -> {
                required(fields, "subject", "메일 제목");
                required(fields, "senderName", "발신자명");
                email(fields, "replyTo", "회신 이메일", true);
                if (clean(request.content()).isEmpty()) throw bad("메일 본문을 입력해주세요.");
            }
            case "COMPANY_INFO" -> {
                for (String[] field : List.of(
                        new String[]{"representative", "대표자명"},
                        new String[]{"businessNumber", "사업자등록번호"},
                        new String[]{"commerceNumber", "통신판매업 신고번호"},
                        new String[]{"phone", "대표 전화"},
                        new String[]{"email", "대표 이메일"},
                        new String[]{"postalCode", "우편번호"},
                        new String[]{"address", "사업장 주소"})) required(fields, field[0], field[1]);
                phone(fields, "phone", "대표 전화");
                email(fields, "email", "대표 이메일", true);
            }
            case "DEPOSIT_ACCOUNT" -> {
                required(fields, "bankName", "은행명");
                required(fields, "accountNumber", "계좌번호");
                required(fields, "accountHolder", "예금주");
                String usage = required(fields, "usage", "사용 구분");
                if (!Set.of("DEPOSIT", "ORDER", "ALL").contains(usage)) throw bad("사용 구분이 올바르지 않습니다.");
            }
            case "MEMBER_GRADE" -> {
                nonNegative(fields, "minimumPurchaseAmount", "승급 기준 누적 구매금액", null);
                nonNegative(fields, "pointRate", "포인트 적립률", 100d);
                nonNegative(fields, "discountRate", "기본 할인율", 100d);
            }
            case "SHIPPING_ADDRESS" -> {
                String postalCode = required(fields, "postalCode", "우편번호");
                if (!KOREAN_POSTAL_CODE.matcher(postalCode).matches()) throw bad("우편번호는 숫자 5자리여야 합니다.");
                required(fields, "address1", "기본 주소");
                required(fields, "address2", "상세 주소");
                required(fields, "receiverName", "담당자명");
                required(fields, "phone", "연락처");
                phone(fields, "phone", "연락처");
            }
            default -> { }
        }
    }
    private String required(Map<String,String> fields,String key,String label){String value=clean(fields.get(key));if(value.isEmpty())throw bad(label+"을(를) 입력해주세요.");return value;}
    private void email(Map<String,String> fields,String key,String label,boolean required){String value=clean(fields.get(key));if(required&&value.isEmpty())throw bad(label+"을(를) 입력해주세요.");if(!value.isEmpty()&&!EMAIL.matcher(value).matches())throw bad(label+" 형식이 올바르지 않습니다.");}
    private void phone(Map<String,String> fields,String key,String label){String value=required(fields,key,label);if(!PHONE.matcher(value).matches())throw bad(label+" 형식이 올바르지 않습니다.");}
    private void nonNegative(Map<String,String> fields,String key,String label,Double maximum){String value=required(fields,key,label);try{double number=Double.parseDouble(value);if(number<0||(maximum!=null&&number>maximum))throw bad(label+" 값의 범위를 확인해주세요.");}catch(NumberFormatException e){throw bad(label+"에는 숫자만 입력해주세요.");}}
    private SettingEntryResponse response(SettingEntry item){return new SettingEntryResponse(item.getId(),item.getType(),item.getCode(),item.getName(),item.getContent(),fields(item.getFieldData()),item.getDisplayOrder(),item.isActive(),item.getCreatedAt(),item.getUpdatedAt());}
    private String json(Map<String,String> fields){try{return objectMapper.writeValueAsString(fields==null?Map.of():fields);}catch(JacksonException e){throw bad("설정 값을 저장할 수 없습니다.");}}
    private Map<String,String> fields(String value){try{Map<?,?> parsed=objectMapper.readValue(value,Map.class);Map<String,String> result=new LinkedHashMap<>();parsed.forEach((key,item)->result.put(String.valueOf(key),String.valueOf(item)));return result;}catch(JacksonException e){return Map.of();}}
    private String clean(String value){return value==null?"":value.trim();} private String nullable(String value){return value==null||value.isBlank()?null:value.trim();}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
