package com.yamoyo.be.domain.meeting.everytime.strategy;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiEverytimeParsingStrategy 단위 테스트")
class ApiEverytimeParsingStrategyTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ApiEverytimeParsingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ApiEverytimeParsingStrategy(restClient);
    }

    @Nested
    @DisplayName("parse() - 시간표 파싱")
    class ParseTest {

        @Test
        @DisplayName("수업이 없는 시간표는 모든 슬롯이 true")
        void emptyTimetable_AllSlotsTrue() {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <response>
                    <table year="2024" semester="1">
                    </table>
                </response>
                """;
            mockRestClient(xml);

            // when
            Map<DayOfWeek, boolean[]> result = strategy.parse("testuser");

            // then: 모든 요일의 모든 슬롯이 true
            for (DayOfWeek day : DayOfWeek.values()) {
                assertThat(result.get(day)).containsOnly(true);
            }
        }

        @Test
        @DisplayName("수업이 있는 시간에 해당하는 슬롯은 false")
        void classExists_SlotsMarkedFalse() {
            // given: 월요일 09:00~10:30 수업
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <response>
                    <table year="2024" semester="1">
                        <subject>
                            <time value="월1.0-2.5/09:00-10:30">
                                <data day="0" starttime="108" endtime="126" place=""/>
                            </time>
                        </subject>
                    </table>
                </response>
                """;
            mockRestClient(xml);

            // when
            Map<DayOfWeek, boolean[]> result = strategy.parse("testuser");

            // then: 월요일 슬롯 18~20이 false (09:00~10:30)
            boolean[] monday = result.get(DayOfWeek.MON);
            assertThat(monday[17]).isTrue();   // 08:30~09:00 가능
            assertThat(monday[18]).isFalse();  // 09:00~09:30 불가
            assertThat(monday[19]).isFalse();  // 09:30~10:00 불가
            assertThat(monday[20]).isFalse();  // 10:00~10:30 불가
            assertThat(monday[21]).isTrue();   // 10:30~11:00 가능

            // 다른 요일은 영향 없음
            assertThat(result.get(DayOfWeek.TUE)).containsOnly(true);
        }

        @Test
        @DisplayName("여러 수업이 있는 경우 모두 처리")
        void multipleClasses_AllProcessed() {
            // given: 월요일 09:00~10:30, 화요일 14:00~15:30 수업
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <response>
                    <table year="2024" semester="1">
                        <subject>
                            <time value="월1.0-2.5/09:00-10:30">
                                <data day="0" starttime="108" endtime="126" place=""/>
                            </time>
                        </subject>
                        <subject>
                            <time value="화6.0-7.5/14:00-15:30">
                                <data day="1" starttime="168" endtime="186" place=""/>
                            </time>
                        </subject>
                    </table>
                </response>
                """;
            mockRestClient(xml);

            // when
            Map<DayOfWeek, boolean[]> result = strategy.parse("testuser");

            // then: 월요일, 화요일 모두 처리됨
            assertThat(result.get(DayOfWeek.MON)[18]).isFalse();  // 월 09:00~09:30 불가
            assertThat(result.get(DayOfWeek.TUE)[28]).isFalse();  // 화 14:00~14:30 불가
        }

        @Test
        @DisplayName("토/일 수업(day >= 5)은 무시됨")
        void weekendClass_Ignored() {
            // given: 토요일 수업 (day=5)
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <response>
                    <table year="2024" semester="1">
                        <subject>
                            <time value="토1.0-2.5/09:00-10:30">
                                <data day="5" starttime="108" endtime="126" place=""/>
                            </time>
                        </subject>
                    </table>
                </response>
                """;
            mockRestClient(xml);

            // when
            Map<DayOfWeek, boolean[]> result = strategy.parse("testuser");

            // then: 토요일은 전체 true (주말 수업은 무시)
            assertThat(result.get(DayOfWeek.SAT)).containsOnly(true);
        }

        @Test
        @DisplayName("API 호출 실패 시 EVERYTIME_API_ERROR 예외")
        void apiCallFails_ThrowsException() {
            // given
            when(restClient.post()).thenThrow(new RuntimeException("Connection failed"));

            // when & then
            assertThatThrownBy(() -> strategy.parse("testuser"))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> {
                        YamoyoException ye = (YamoyoException) e;
                        assertThat(ye.getErrorCode()).isEqualTo(ErrorCode.EVERYTIME_API_ERROR);
                    });
        }

        @Test
        @DisplayName("잘못된 XML 형식은 EVERYTIME_PARSE_FAILED 예외")
        void invalidXml_ThrowsException() {
            // given
            String invalidXml = "not valid xml <>";
            mockRestClient(invalidXml);

            // when & then
            assertThatThrownBy(() -> strategy.parse("testuser"))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> {
                        YamoyoException ye = (YamoyoException) e;
                        assertThat(ye.getErrorCode()).isEqualTo(ErrorCode.EVERYTIME_PARSE_FAILED);
                    });
        }
    }

    private void mockRestClient(String responseBody) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(responseBody);
    }
}
