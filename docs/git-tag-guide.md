# Git 태그 가이드 (Staging 배포 버전 관리)

## 태그 네이밍 규칙

```
v{major}.{minor}.{patch}-staging

예시:
v1.0.0-staging   # staging 첫 배포
v1.1.0-staging   # 기능 추가 배포
v1.1.1-staging   # 버그픽스 배포
v2.0.0-staging   # 대규모 변경 배포
```

| 구분 | 올릴 때 |
|------|--------|
| major | 대규모 변경, 호환성 깨지는 변경 |
| minor | 기능 추가 |
| patch | 버그 수정, 핫픽스 |

---

## 자주 쓰는 명령어

### 태그 생성

```bash
# 현재 커밋에 태그 (메시지 포함, 권장)
git tag -a v1.0.0-staging -m "staging 배포: JWT sid 추가 + 부하테스트 API"

# 특정 커밋에 태그
git tag -a v1.0.0-staging abc1234 -m "staging 배포: 초기 배포"
```

### 태그 push

```bash
# 특정 태그 push
git push origin v1.0.0-staging

# 모든 태그 push
git push origin --tags
```

### 태그 조회

```bash
# 전체 태그 목록
git tag -l

# staging 태그만 필터
git tag -l "*staging*"

# 태그 상세 정보 (커밋, 메시지, 날짜)
git show v1.0.0-staging

# 태그 포함된 로그
git log --oneline --decorate
```

### 태그 삭제

```bash
# 로컬 삭제
git tag -d v1.0.0-staging

# 원격 삭제
git push origin --delete v1.0.0-staging
```

### 태그 기준 diff

```bash
# 두 버전 사이 변경 사항
git diff v1.0.0-staging..v1.1.0-staging

# 두 버전 사이 커밋 목록
git log v1.0.0-staging..v1.1.0-staging --oneline
```

---

## 배포 플로우 예시

```bash
# 1. dev 브랜치에서 작업 완료 후 staging에 배포할 커밋 확인
git log --oneline -5

# 2. 태그 생성
git tag -a v1.2.0-staging -m "JWT sid 추가, 부하테스트 API, 주문서 경기장 고정"

# 3. 태그 push
git push origin v1.2.0-staging

# 4. 나중에 "staging에 뭐가 배포되어있지?" 확인
git show v1.2.0-staging
```