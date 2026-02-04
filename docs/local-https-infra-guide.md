# 📘 Local HTTPS 테스트 환경 가이드 (Infra)

## 목적
- 운영과 동일한 구조로 로컬 HTTPS 환경에서 테스트 가능하도록 세팅
- OAuth2 테스트를 위한 기반 환경 제공
- 이 문서는 환경 세팅까지만 다룸 (OAuth 로직은 별도)

---

## 1. 사전 준비 (최초 1회)

### 1-1. hosts 설정
```bash
sudo nano /etc/hosts
```

아래 한 줄 추가:
```
127.0.0.1 local.yamoyo.test
```
추가한 후 순서대로
1. 파일 저장 : Ctrl⌃ + O
2. Enter⏎
3. 파일 닫기 : Ctrl⌃ + X


### 1-2. mkcert 설치 및 로컬 인증서 생성
```bash
# macOS
brew install mkcert nss

# 로컬 CA 설치
mkcert -install

# 프로젝트 루트에서 인증서 생성
cd infra/local/certs
mkcert local.yamoyo.test
```

⚠️ **인증서는 각자 PC에서 생성해야 하며, git에 올라가지 않습니다.**

---

## 2. 로컬 HTTPS 환경 실행

```bash
# 기존 컨테이너가 띄워져있을 경우
# docker compose down
docker compose -f docker-compose.local.yml up -d
```
- 기존 컨테이너가 띄워져있을 경우 정지하고 진행해주세요.

**포함된 서비스:**
- Nginx (HTTPS, 프록시)
- Backend (Spring Boot)
- Redis
- MySQL (로컬 전용)

---

## 3. 정상 동작 확인

### 3-1. HTTPS 접속
```
https://local.yamoyo.test
```

**확인 사항:**
- 🔒 자물쇠 표시
- 프론트 화면 정상 노출

### 3-2. API 프록시 확인
```
https://local.yamoyo.test/api/health
```

응답 예:
```json
{"status":"UP"}
```

---

## 4. OAuth2 테스트 관련 안내

**OAuth2 Redirect URI 기준:**
```
https://local.yamoyo.test/login/oauth2/code/{provider}
```

- OAuth2 로직/Provider 설정은 애플리케이션 담당 영역
- .env.local.example을 .env.local로 수정해서 사용해주세요!
---

## ⚠️ 주의사항

- Docker 환경 실행 중에는 로컬에서 Spring Boot를 따로 실행하지 않습니다  
  (8080 포트 충돌 발생)
- 인증서(`infra/local/certs/`)와 환경변수(`.env.local`)만 gitignore 대상입니다.
- 프론트 빌드 결과물은 이미 레포지토리에 포함되어 있습니다. (수정되었다면 빌드 후 다시 복사해주세요.)

---

## 📁 디렉토리 구조

```
backend/
├── docker-compose.local.yml           # 로컬 HTTPS 환경 설정
├── infra/
│   └── local/
│       ├── nginx/
│       │   └── nginx.conf             # Nginx 설정
│       ├── certs/                     # 🔒 gitignore (각자 생성)
│       │   ├── local.yamoyo.test.pem
│       │   └── local.yamoyo.test-key.pem
│       └── frontend/                  # ✅ 프론트 빌드 결과물 (커밋됨)
│           ├── index.html
│           └── assets/
└── .env.local                         # 🔒 gitignore
```

---

## 🔧 문제 해결

### 인증서 오류
```bash
# 인증서 재생성
cd infra/local/certs
rm *.pem
mkcert local.yamoyo.test
```

### 포트 충돌
```bash
# 기존 컨테이너 정리
docker compose -f docker-compose.local.yml down
```

### 프론트 화면이 안 보일 때
```bash
# 컨테이너 재시작
docker compose -f docker-compose.local.yml restart nginx
```