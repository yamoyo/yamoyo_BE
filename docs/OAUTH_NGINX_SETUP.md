# OAuth2 & Nginx 설정 가이드

> 최종 수정일: 2026-01-14

## 1. 현재 설정 (Nginx 미사용)

### 아키텍처
```
브라우저 → Spring Boot (8080)
```

### 변경된 파일

| 파일 | 변경 내용 |
|------|----------|
| `application.yml` | Google/Kakao redirect-uri를 8080 포트로 명시 |
| `.env` | `KAKAO_REDIRECT_URI` 포트 80 → 8080 변경 |
| `.env.example` | Nginx 유무에 따른 설정 가이드 추가 |
| `JwtTokenProvider.java:47` | `Base64.getDecoder()` → `Base64.getUrlDecoder()` 변경 |

### OAuth Provider Console 설정

| Provider | Redirect URI |
|----------|--------------|
| Kakao | `http://localhost:8080/login/oauth2/code/kakao` |
| Google | `http://localhost:8080/login/oauth2/code/google` |

---

## 2. Nginx 도입 시 변경 사항

### 아키텍처
```
브라우저 → Nginx (80) → Spring Boot (8080)
```

### 2-1. `application.yml` 수정

```diff
# Google
- redirect-uri: http://localhost:8080/login/oauth2/code/google
+ redirect-uri: http://localhost/login/oauth2/code/google

# Kakao
- redirect-uri: http://localhost:8080/login/oauth2/code/kakao
+ redirect-uri: http://localhost/login/oauth2/code/kakao
```

### 2-2. `.env` 수정

```diff
- KAKAO_REDIRECT_URI=http://localhost:8080/login/oauth2/code/kakao
+ KAKAO_REDIRECT_URI=http://localhost/login/oauth2/code/kakao
```

### 2-3. OAuth Provider Console 수정

| Provider | Console URL | Redirect URI |
|----------|-------------|--------------|
| Kakao | https://developers.kakao.com/ | `http://localhost/login/oauth2/code/kakao` |
| Google | https://console.cloud.google.com/ | `http://localhost/login/oauth2/code/google` |

### 2-4. docker-compose.yml에 Nginx 서비스 추가

```yaml
services:
  # ... 기존 서비스들 ...

  nginx:
    image: nginx:alpine
    container_name: yamoyo-nginx
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - backend
    networks:
      - yamoyo-network
```

### 2-5. nginx/nginx.conf 파일 생성

```nginx
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server backend:8080;
    }

    server {
        listen 80;
        server_name localhost;

        location / {
            proxy_pass http://backend;
            proxy_http_version 1.1;

            # Forwarded Headers (Spring Boot가 원본 요청 정보 인식)
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header X-Forwarded-Host $host;
            proxy_set_header X-Forwarded-Port $server_port;
        }
    }
}
```

### 2-6. application.yml 확인

아래 설정이 이미 되어 있어야 합니다 (현재 설정됨):

```yaml
server:
  port: 8080
  forward-headers-strategy: framework  # Nginx의 X-Forwarded-* 헤더 인식
```

---

## 3. Nginx 도입 체크리스트

- [ ] `application.yml`의 Google/Kakao redirect-uri를 80 포트로 변경
- [ ] `.env` 파일의 `KAKAO_REDIRECT_URI` 포트 제거 (80 사용)
- [ ] `nginx/nginx.conf` 파일 생성
- [ ] `docker-compose.yml`에 nginx 서비스 추가
- [ ] Kakao Developers Console에서 Redirect URI 변경
- [ ] Google Cloud Console에서 Redirect URI 변경
- [ ] `docker compose down && docker compose --profile full up -d --build` 실행

---

## 4. JWT Secret Key 생성 방법

URL-safe Base64 인코딩된 키 생성:

```bash
openssl rand -base64 32 | tr '+/' '-_' | tr -d '='
```

생성된 값을 `.env` 파일의 `JWT_SECRET`에 설정합니다.

---

## 5. 트러블슈팅

### "localhost가 연결을 거부했습니다" 오류

**원인**: OAuth 콜백 URL 포트와 실제 서버 포트 불일치

| 상황 | 해결 방법 |
|------|----------|
| Nginx 미사용인데 80 포트로 설정됨 | `.env`와 Provider Console을 8080으로 변경 |
| Nginx 사용인데 8080 포트로 설정됨 | `.env`와 Provider Console을 80으로 변경 |

### "Illegal base64 character" 오류

**원인**: JWT_SECRET이 올바른 Base64 형식이 아님

**해결**:
1. URL-safe Base64 값 생성 (위 명령어 참고)
2. `.env`에 따옴표 없이 설정
