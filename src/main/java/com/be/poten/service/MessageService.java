package com.be.poten.service;

import com.be.poten.domain.Message;
import com.be.poten.dto.ClovaRequestDto.ClovaRequest;
import com.be.poten.dto.ClovaRequestDto.ClovaRequestMessage;
import com.be.poten.dto.message.MessageRequestDto;
import com.be.poten.mapper.MessageMapper;
import com.be.poten.mapper.TestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;

@Service
@Transactional
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    @Transactional
    public String executeAndGetMessage(MessageRequestDto message) {
        String clovaContent = transMessageToClovaContent(message);
        String result = postClova(message, clovaContent);
        insertMessage(Message.MessageOf(message, result));
        return result;
    }

    /**
     * message dto -> 하나의 문장으로 변환 (클로바 전달용)
     */
    private String transMessageToClovaContent(MessageRequestDto message) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n- 대상자: " + message.getTargetType());
        sb.append("\n- 대상자 이름: " + message.getTargetName());

        if(!"기타".equals(message.getRelationship())) {
            sb.append("\n- 대상자를 지칭하는 관계: " + message.getRelationship());
        }

        sb.append("\n- 축사자 이름: " + message.getUserName());
        sb.append("\n- 축사 분위기: " + message.getConcept());
        sb.append("\n- 축사에 들어갈 스토리텔링: " + message.getStory());
        sb.append("\n- 말투: " + message.getSpeechType());
        sb.append("\n- 마지막으로 해주고 싶은 말: " + message.getLastComment());
        sb.append("\n- 축사 진행 시간: " + message.getMinute());
        sb.append("\n- 최대 글자수: " + "1000자");

        return sb.toString();
    }

    private String postClova(MessageRequestDto message, String clovaContent) {
        RestTemplate template = new RestTemplate();

        // request data
        ArrayList<ClovaRequestMessage> messageList = new ArrayList();
        messageList.add(new ClovaRequestMessage("system", "항목을 입력하면 대상자에게 전달할 \\b결혼식 축사 문장을 추천 해드립니다.\\n(지정한 대상자의 이름과 지정한 축사자의 이름이 내용에 들어가야 함)"));
        messageList.add(new ClovaRequestMessage("user", clovaContent));

        ClovaRequest clovaRequest = ClovaRequest.builder()
                .messages(messageList)
                .topP(0.8)
                .topK(0)
                .maxTokens(2000)
                .temperature(0.5)
                .repeatPenalty(5.0)
                .stopBefore(new ArrayList<>())
                .includeAiFilters(true)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NCP-CLOVASTUDIO-API-KEY", "NTA0MjU2MWZlZTcxNDJiYzQNPMHi/Vt8f/jUw+uD7CxTg8NfmaRlPXQoigyeublY7eVnylTxPL1vEyeFjZDwRkkqeiMSeAXfdB1q9+QTAs9Z4BA6CjvC9odNIFkcQ2l6GU2dPPD8l70WoKJ7nOewsdU6Key8/+plx3IVU/6g6hv0JXRjZf89oC384tCoyzfO+IcQ6Mjw00pJLaaBRSr53bQ0lNhdj3GWf4bDr7u1aV0=");
        headers.set("X-NCP-APIGW-API-KEY", "CNRSgHB8UsB0ReR29fU3NqjG8XRfWvifgKOhu6JY");
        headers.set("X-NCP-CLOVASTUDIO-REQUEST-ID", "20b7dae169554243b9c6c44cd718a309");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity formEntity = new HttpEntity<>(clovaRequest, headers);

        String apiUrl = "https://clovastudio.stream.ntruss.com/testapp/v1/chat-completions/HCX-002";

        LinkedHashMap resMap = template.postForObject(apiUrl, formEntity, LinkedHashMap.class);

        String code = (String) ((LinkedHashMap)resMap.get("status")).get("code");
        String content = "";

        if("20000".equals(code)) {
            content = (String) ((LinkedHashMap)((LinkedHashMap)resMap.get("result")).get("message")).get("content");

            /* 데이터 후처리 */
            // [축사자 이름], [대상자 이름] replace
            content = content.replaceAll("\\[축사자 이름\\]", message.getUserName());
            content = content.replaceAll("\\[대상자 이름\\]", message.getTargetName());

        }else {
            content = "문장을 열심히 학습중이예요. 다음에 다시 시도 해주세요.";
        }

        System.out.println(content);

        return content;
    }

    private void insertMessage(Message message) {

        messageMapper.insertMessage(message);

    }

}