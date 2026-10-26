# video-server

GitHub collaborator 또는 관리자가 발급한 임시 접근 코드로 저장소별 영상을 재생하는 서버입니다.

## 필수 환경 변수

```powershell
$env:GITHUB_TOKEN="GitHub fine-grained PAT"
$env:VIDEO_ADMIN_PASSWORD="관리자 비밀번호"
$env:VIDEO_ROOT="D:\share"
```

GitHub 토큰에는 설정된 저장소의 collaborator를 조회할 수 있는 권한이 필요합니다. 비밀값은 `application.yml`에 기록하지 않습니다.

## 영상 폴더

`VIDEO_ROOT` 아래의 폴더 이름은 `application.yml`의 `custom.github.repositories`와 같아야 합니다.

```text
D:\share\
├─ kh14\
├─ kh15\
├─ kh16\
├─ kh17-be\
└─ the01\
```

## 빌드와 실행

```powershell
cd video
mvn clean package
java -jar target\video-0.0.1.jar
```

Cloudflare Tunnel은 `https://video-api.sysout.co.kr`을 `http://localhost:30000`으로 연결합니다. 애플리케이션 자체 인증서나 PKCS12 파일은 사용하지 않습니다.

## 추가 환경 변수

| 이름 | 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `30000` | 백엔드 포트 |
| `GITHUB_OWNER` | `hiphop5782` | 저장소 소유자 |
| `GITHUB_ENABLED` | `true` | GitHub collaborator 조회 활성화 |
| `FIREBASE_API_KEY` | 기존 Firebase 프로젝트 키 | Firebase ID 토큰 확인 |
| `ACCESS_DB_URL` | `jdbc:h2:file:./data/video-access...` | 접근 권한 DB 경로 |
| `VIEWER_SESSION_HOURS` | `12` | 시청 세션 유지 시간 |
| `ADMIN_SESSION_HOURS` | `2` | 관리자 세션 유지 시간 |
| `COOKIE_SECURE` | `true` | HTTPS 쿠키 사용 여부; 로컬 HTTP 테스트에서만 `false` |

임시 접근 권한은 관리자 API/화면에서 생성하고 삭제합니다. 권한을 삭제하면 그 권한으로 발급된 기존 시청 세션도 즉시 무효화됩니다.
