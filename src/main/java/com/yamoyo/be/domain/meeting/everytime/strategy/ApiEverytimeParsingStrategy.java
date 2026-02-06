package com.yamoyo.be.domain.meeting.everytime.strategy;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.everytime.util.EverytimeTimeSlotConverter;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Map;

/**
 * Everytime API를 통한 시간표 파싱 전략.
 *
 * <p>Everytime API 엔드포인트에 직접 요청하여 XML 응답을 파싱한다.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class ApiEverytimeParsingStrategy implements EverytimeParsingStrategy {

    private final RestClient everytimeRestClient;

    @Override
    public Map<DayOfWeek, boolean[]> parse(String identifier) {
        String xml = fetchTimetableXml(identifier);
        return parseXmlToAvailability(xml);
    }

    private String fetchTimetableXml(String identifier) {
        try {
            String response = everytimeRestClient.post()
                    .uri("/find/timetable/table/friend")
                    .body("identifier=" + identifier + "&friendInfo=true")
                    .retrieve()
                    .body(String.class);

            log.debug("Everytime API 응답 수신 - identifier: {}", identifier);
            return response;
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                log.error("Everytime API 타임아웃 - identifier: {}", identifier, e);
                throw new YamoyoException(ErrorCode.EVERYTIME_TIMEOUT);
            }
            log.error("Everytime API 호출 실패 - identifier: {}", identifier, e);
            throw new YamoyoException(ErrorCode.EVERYTIME_API_ERROR);
        } catch (Exception e) {
            log.error("Everytime API 호출 실패 - identifier: {}", identifier, e);
            throw new YamoyoException(ErrorCode.EVERYTIME_API_ERROR);
        }
    }

    private Map<DayOfWeek, boolean[]> parseXmlToAvailability(String xml) {
        Map<DayOfWeek, boolean[]> availability = EverytimeTimeSlotConverter.createFullAvailabilityMap();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // XXE 공격 방지
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList subjects = doc.getElementsByTagName("subject");

            for (int i = 0; i < subjects.getLength(); i++) {
                Element subject = (Element) subjects.item(i);
                NodeList dataElements = subject.getElementsByTagName("data");

                for (int j = 0; j < dataElements.getLength(); j++) {
                    Element data = (Element) dataElements.item(j);
                    processDataElement(data, availability);
                }
            }

            log.info("시간표 파싱 완료 - 수업 수: {}", subjects.getLength());
            return availability;

        } catch (YamoyoException e) {
            throw e;
        } catch (Exception e) {
            log.error("XML 파싱 실패", e);
            throw new YamoyoException(ErrorCode.EVERYTIME_PARSE_FAILED);
        }
    }

    private void processDataElement(Element data, Map<DayOfWeek, boolean[]> availability) {
        try {
            int day = Integer.parseInt(data.getAttribute("day"));
            int startTime = Integer.parseInt(data.getAttribute("starttime"));
            int endTime = Integer.parseInt(data.getAttribute("endtime"));

            // day 범위 검증 (0=월 ~ 4=금)
            if (day < 0 || day > 4) {
                return;
            }

            DayOfWeek dayOfWeek = EverytimeTimeSlotConverter.toDayOfWeek(day);
            int startMinutes = EverytimeTimeSlotConverter.toMinutes(startTime);
            int endMinutes = EverytimeTimeSlotConverter.toMinutes(endTime);

            boolean[] slots = availability.get(dayOfWeek);
            EverytimeTimeSlotConverter.markBusy(slots, startMinutes, endMinutes);

            log.debug("수업 슬롯 처리 - day: {}, {}분 ~ {}분", dayOfWeek, startMinutes, endMinutes);

        } catch (NumberFormatException e) {
            log.warn("data 요소 파싱 실패 - 속성값이 유효하지 않음", e);
        }
    }
}
