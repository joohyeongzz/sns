# SNS

- SNS 백엔드 프로젝트
- 단순 기능 구현 뿐만이 아닌 확장성, 장애 대처, 유지보수성을 고려하여 구현하는 것을 목표로 개발하였습니다.

## ✅ 사용 기술 및 개발 환경

Java, Spring Boot, MySQL, JPA, Redis, Docker 등

## ✅ Architecture

- ### MySQL Architecture
  
![스크린샷 2024-12-19 125432](https://github.com/user-attachments/assets/f84fce70-f977-4d90-9797-9a0a6d932f76)


- ### Redis Architecture
  
![스크린샷 2024-11-21 123352](https://github.com/user-attachments/assets/b52c7731-3d37-4538-94bf-8bb71c8054a6)


- ### Monitoring Architecture
  
![스크린샷 2024-11-21 120914](https://github.com/user-attachments/assets/dee07ddf-c186-44a8-aafd-c9876fce83c4)


## ✅ 주요 기능

1. 게시글 CRUD
2. 댓글 CRUD
3. 게시글 좋아요
4. 피드 생성 및 조회
5. 유저 간 팔로잉


## ✅ 주요 고려 사항

### 1. 인프라의 확장성 👉[Click](https://github.com/joohyeongzz/SNS/wiki/1.-%EC%9D%B8%ED%94%84%EB%9D%BC%EC%9D%98-%ED%99%95%EC%9E%A5%EC%84%B1).

### 2. Redis 기반 피드 관리 전략 👉[Click](https://github.com/joohyeongzz/SNS/wiki/2.-Redis-%EA%B8%B0%EB%B0%98-%ED%94%BC%EB%93%9C-%EA%B4%80%EB%A6%AC-%EC%A0%84%EB%9E%B5).

### 3. Redis 활용 성능 개선 👉[Click](https://github.com/joohyeongzz/SNS/wiki/3.-Redis-%ED%99%9C%EC%9A%A9-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0).

### 4. Redis 장애 대처 👉[Click](https://github.com/joohyeongzz/SNS/wiki/4.-Redis-%EC%9E%A5%EC%95%A0-%EB%8C%80%EC%B2%98).

### 5. AOP 활용 중복 코드 최소화 및 관심사 분리 👉[Click](https://github.com/joohyeongzz/SNS/wiki/5.-AOP-%ED%99%9C%EC%9A%A9-%EC%A4%91%EB%B3%B5-%EC%BD%94%EB%93%9C-%EC%B5%9C%EC%86%8C%ED%99%94-%EB%B0%8F-%EA%B4%80%EC%8B%AC%EC%82%AC-%EB%B6%84%EB%A6%AC).

### 6. AWS S3 기반 PreSigned URL 활용 파일 업로드 로직 개선 👉[Click](https://github.com/joohyeongzz/SNS/wiki/6.-AWS-S3-%EA%B8%B0%EB%B0%98-PreSigned-URL-%ED%99%9C%EC%9A%A9-%ED%8C%8C%EC%9D%BC-%EC%97%85%EB%A1%9C%EB%93%9C-%EB%A1%9C%EC%A7%81-%EA%B0%9C%EC%84%A0).

### 7. 서버 모니터링 및 경고 알림 👉[Click](https://github.com/joohyeongzz/SNS/wiki/7.-%EC%84%9C%EB%B2%84-%EB%AA%A8%EB%8B%88%ED%84%B0%EB%A7%81-%EB%B0%8F-%EA%B2%BD%EA%B3%A0-%EC%95%8C%EB%A6%BC).
