Game Flow

┌─────────────────────────────────────────────────────────────────┐                                                                                                                                                                   
│                    FULL GAME FLOW                               │                                                                                                                                                                   
└─────────────────────────────────────────────────────────────────┘

    [대기 중]                                                                                                                                                                                                                           
        │                                                                                                                                                                                                                               
        │  방장: /start  (모든 멤버 접속 필수)                                                                                                                                                                                          
        ▼                                                                                                                                                                                                                               
    ┌──────────┐   10초 카운트다운                                                                                                                                                                                                      
    │ VOLUNTEER │──────────────────────────────────────────┐                                                                                                                                                                            
    │  (10초)   │  유저: /volunteer (팀장 지원)              │                                                                                                                                                                          
    └──────────┘                                          │                                                                                                                                                                             
        │                                                 │                                                                                                                                                                             
        │  10초 경과 (TaskScheduler)                        │                                                                                                                                                                           
        ▼                                                 │                                                                                                                                                                             
    ┌────────────────────────────────┐                     │                                                                                                                                                                            
    │ 지원자 수 판단                   │                     │                                                                                                                                                                          
    │  0명 → 전원 자동 지원자 등록      │                     │                                                                                                                                                                         
    │  1명 → 즉시 팀장 결정 (RESULT)   │─────────┐          │                                                                                                                                                                           
    │  2명+ → GAME_SELECT            │          │          │                                                                                                                                                                            
    └────────────────────────────────┘          │          │                                                                                                                                                                            
        │                                      │          │                                                                                                                                                                             
        ▼                                      │          │                                                                                                                                                                             
    ┌───────────┐                              │          │                                                                                                                                                                             
    │ GAME_SELECT│                              │          │                                                                                                                                                                            
    │            │                              │          │                                                                                                                                                                            
    └───────────┘                              │          │                                                                                                                                                                             
        │                                      │          │                                                                                                                                                                             
        │  방장: /select-game                    │          │                                                                                                                                                                           
        │                                      │          │                                                                                                                                                                             
        ├─── LADDER ──┐                         │          │                                                                                                                                                                            
        │             ▼                         │          │                                                                                                                                                                            
        │   즉시 결과 계산                        │          │                                                                                                                                                                          
        │   broadcast GAME_RESULT ──────────────┤          │                                                                                                                                                                            
        │                                      │          │                                                                                                                                                                             
        ├─── ROULETTE ─┐                        │          │                                                                                                                                                                            
        │              ▼                        │          │                                                                                                                                                                            
        │   즉시 결과 계산                        │          │                                                                                                                                                                          
        │   broadcast GAME_RESULT ──────────────┤          │                                                                                                                                                                            
        │                                      │          │                                                                                                                                                                             
        └─── TIMING ──┐                         │          │                                                                                                                                                                            
                      ▼                         │          │                                                                                                                                                                            
                ┌───────────┐                   │          │                                                                                                                                                                            
                │ GAME_READY │                   │          │                                                                                                                                                                           
                │ (방장 대기)  │                   │          │                                                                                                                                                                         
                └───────────┘                   │          │                                                                                                                                                                            
                      │                         │          │                                                                                                                                                                            
                      │  방장: /start-timing      │          │                                                                                                                                                                          
                      ▼                         │          │                                                                                                                                                                            
                ┌────────────┐  16초 제한         │          │                                                                                                                                                                          
                │ GAME_PLAYING│                   │          │                                                                                                                                                                          
                │  (타이밍)    │                   │          │                                                                                                                                                                         
                └────────────┘                   │          │                                                                                                                                                                           
                      │                         │          │                                                                                                                                                                            
                      │  유저: /timing-stop       │          │                                                                                                                                                                          
                      │  (전원 완료 or 16초 경과)    │          │                                                                                                                                                                       
                      ▼                         │          │                                                                                                                                                                            
                오차 가장 큰 유저 = 팀장             │          │                                                                                                                                                                       
                broadcast GAME_RESULT ──────────┤          │                                                                                                                                                                            
                                               │          │                                                                                                                                                                             
                                               ▼          │                                                                                                                                                                             
                                         ┌──────────┐     │                                                                                                                                                                             
                                         │  RESULT   │◀───┘                                                                                                                                                                             
                                         └──────────┘                                                                                                                                                                                   
                                               │                                                                                                                                                                                        
                                               │  DB 반영:                                                                                                                                                                              
                                               │  - 당첨자 → LEADER                                                                                                                                                                     
                                               │  - HOST → MEMBER                                                                                                                                                                       
                                               │  - Workflow → SETUP                                                                                                                                                                    
                                               │                                                                                                                                                                                        
                                               │  유저: /confirm-result                                                                                                                                                                 
                                               │  (전원 나가면 Redis 정리)                                                                                                                                                              
                                               ▼                                                                                                                                                                                        
                                            [종료]                                                                                                                                                                                      

Role Changes on Result

Before:                          After:                                                                                                                                                                                               
User A  [HOST]     ──────▶    User A  [MEMBER]                                                                                                                                                                                      
User B  [MEMBER]   ──────▶    User B  [LEADER]  (당첨)                                                                                                                                                                              
User C  [MEMBER]   ──────▶    User C  [MEMBER]

Timing Game Logic

Target: 7.777초                                                                                                                                                                                                                       
제한시간: 16초

오차 = |stoppedTime - 7.777|                                                                                                                                                                                                          
미기록 유저 → stoppedTime = 16.0

가장 큰 오차 = 팀장 (벌칙 개념)                                                                                                                                                                                                       
동일 오차 → 랜덤 선택

File Structure

domain/leadergame/                                                                                                                                                                                                                    
├── controller/                                                                                                                                                                                                                       
│   ├── LeaderGameController.java      WebSocket 메시지 핸들러                                                                                                                                                                        
│   └── LeaderGameTestController.java  테스트 데이터 REST API                                                                                                                                                                         
├── dto/                                                                                                                                                                                                                              
│   ├── GameParticipant.java           참가자 정보                                                                                                                                                                                    
│   ├── GameState.java                 전체 게임 상태                                                                                                                                                                                 
│   ├── message/                                                                                                                                                                                                                      
│   │   ├── GameMessage.java           제네릭 메시지 래퍼                                                                                                                                                                             
│   │   ├── GameResultPayload.java     결과 페이로드                                                                                                                                                                                  
│   │   ├── JoinPayload.java           입장 응답                                                                                                                                                                                      
│   │   ├── PhaseChangePayload.java    Phase 변경 알림                                                                                                                                                                                
│   │   └── VolunteerPayload.java      지원 알림                                                                                                                                                                                      
│   └── request/                                                                                                                                                                                                                      
│       ├── SelectGameRequest.java     게임 선택 요청                                                                                                                                                                                 
│       ├── TimingStopRequest.java     타이밍 STOP 요청                                                                                                                                                                               
│       └── VolunteerRequest.java      지원 요청                                                                                                                                                                                      
├── enums/                                                                                                                                                                                                                            
│   ├── GamePhase.java                 VOLUNTEER → GAME_SELECT → ...                                                                                                                                                                  
│   └── GameType.java                  LADDER, ROULETTE, TIMING                                                                                                                                                                       
└── service/                                                                                                                                                                                                                          
├── GameStateRedisService.java     Redis 상태 관리                                                                                                                                                                                
├── LadderGameService.java         사다리 게임 로직                                                                                                                                                                               
├── LeaderGameDbService.java       DB 트랜잭션 처리                                                                                                                                                                               
├── LeaderGameService.java         메인 게임 서비스                                                                                                                                                                               
├── RouletteGameService.java       룰렛 게임 로직                                                                                                                                                                                 
└── TimingGameService.java         타이밍 게임 로직      