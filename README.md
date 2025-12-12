<img width="1758" height="303" alt="Group 144" src="https://github.com/user-attachments/assets/bb4b0055-a3d4-415c-8855-b1114ed4276b" />

<br />

# 🧩 UniBooker Back-end

<br>

## 📘 프로젝트 소개

**UniBooker**는 대규모 예약 신청을 안정적으로 처리하기 위해 설계된 **B2B 클라우드 예약 관리 서비스**입니다. <br/>
기업이나 기관은 별도의 개발 과정 없이 **가입만으로 바로 도입**할 수 있으며,
시설·공간·이벤트 등 다양한 리소스를 손쉽게 등록하고 운영할 수 있습니다.

교육, 헬스케어, 공공기관, 기업 행사 등 여러 산업에서 바로 적용할 수 있도록 설계되었으며,
각 기업의 정책과 운영 방식에 맞춰 **유연하게 규칙을 설정하고 확장 가능한 구조**를 제공하여
기존처럼 각 기업이 개별 시스템을 구축·운영해야 하는 부담을 크게 줄여줍니다.
<br><br><br>

## 👥 팀원 소개
<div align="center">

| <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRKQRnAMmRWI273hiziEH3SDGEIPshU-cRblQ&s" width="100" height="100"/> | <img src="https://i.pinimg.com/236x/c8/5f/d7/c85fd7277915c240c352e0f7b8495d8c.jpg" width="100" height="100"/> | <img src="https://image.zeta-ai.io/profile-image/8f2165b4-df11-41d4-babb-aa27e1f1a420/d470174d-7e96-44c5-adcc-3463975a7146.jpeg?w=3840&q=90&f=webp" width="100" height="100"/> | <img src="https://blog.kakaocdn.net/dna/ddapbS/btsH5PuRPbZ/AAAAAAAAAAAAAAAAAAAAACDpoGlbGghqNR4aWLq9C2NaIXAf5bF6blKSGob_YDBP/img.png?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1764514799&allow_ip=&allow_referer=&signature=i6S%2F%2B8MiAs%2FNXDXkxvCBb8U%2BFco%3D" width="100" height="100"/> | <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQCFhW2il5BFeGVFQH1X6llq9_zOstgaADyuQ&s" width="100" height="100"/> |
| :-------------------------------------------------------------------------------------------------------------------: | :----------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------: |
|                              🍀 **김아영**<br/>[@thay123028](https://github.com/thay123028)                              |                  🍀 **유현경**<br/>[@gaangstar](https://github.com/gaangstar)                 |                    🍀 **윤소민**<br/>[@somminn](https://github.com/somminn)                    |                               🍀 **허정우**<br/>[@JohnHeo81](https://github.com/JohnHeo81)                               |                               🍀 **홍서연**<br/>[@seoyeon22](https://github.com/seoyeon22)                               |

</div>
<br><br>

## 📘 프로젝트 개요
### 🔍 배경

기업·기관의 예약 시스템은 특정 시점에 이용자가 급증하는 구조를 가지고 있습니다.<br/>
수강신청, 티켓팅, 병원 예약, 행정 서비스 등에서는 수천 명이 동시에 접속하는 상황이 반복되지만 이러한 트래픽을 감당하지 못해 아래와 같은 문제가 발생하고 있습니다.

* 동시 접속 폭주로 인한 응답 지연, 서버 다운, 중복 예약
* 잔여석·취소·성공 여부를 실시간으로 확인하기 어려움
* 기업별로 시스템을 따로 구축해야 하므로 비용·운영 인력 부담 증가
* 특정 산업에 맞춘 폐쇄적 구조로 범용성이 부족

이는 운영 효율 저하, 공정성 문제, 민원 증가 등으로 이어질 수 있습니다.

<br/>

### 🧩 솔루션

저희는 위 문제를 해결하기 위해
대규모 예약 신청을 안정적으로 처리할 수 있는 B2B 클라우드 예약 관리 서비스를 목표로 하였습니다.

#### 1) 대규모 트래픽 대응
* **Redis 기반 대기열 시스템**으로 폭주 트래픽 제어
* **Hold(임시 점유) 시스템**으로 중복 선택 방지
* Redis 분산 락으로 중복 예약 방지

#### 2) 실시간 동기화
* **WebSocket(STOMP)** 기반 실시간 좌석/시간 상태 동기화
* 잔여석, Hold 상태, 예약 완료를 실시간으로 브로드캐스트
* 운영자가 즉각적으로 문제를 파악하고 대응 가능

#### 3) 다양한 예약 유형 지원
* **RESERVATION**: 시간대 단위 예약 (회의실, 스터디룸 등)
* **SEAT**: 좌석 단위 예약 (영화관, 공연장, 강의실 등)
* **EVENT**: 이벤트/행사 신청

#### 4) 시스템 구축/운영 부담 감소
* 클라우드 SaaS 기반으로 별도 개발·서버 구축 필요 없음
* 멀티테넌시 지원으로 기업별 독립 운영
* 유지보수 비용 절감 및 운영 효율 향상

<br/>


## 🌟 주요 기능

### 🍀 대기열 시스템
* **Redis ZSet 기반** 선착순 대기열 구현
* 실시간 대기 순번 및 예상 대기 시간 제공
* 입장 토큰 발급 및 TTL 기반 자동 만료
* WebSocket을 통한 실시간 대기 상태 알림

### 🍀 Hold(임시 점유) 시스템
* 시간/좌석 선택 시 **Redis TTL 기반 임시 점유**
* 다른 사용자의 중복 선택 차단
* **페이지 TTL과 동기화**되어 자동 해제
* WebSocket 브로드캐스트로 실시간 Hold 상태 공유

### 🍀 예약 관리
* **RESERVATION**: 시간대 단위 예약 생성/취소
* **SEAT**: 좌석 단위 예약 (행/열 좌표 기반)
* **EVENT**: 이벤트 신청 (선착순/추첨)
* 예약 겹침·동시성 제어로 안정적인 예약 처리
* 예약 완료 시 Hold 자동 삭제 및 WebSocket 알림

### 🍀 리소스(예약 서비스) 관리
* 시설·공간·장비 등 다양한 자원을 그룹별로 구조화하여 관리
* 리소스별 운영 시간, 예약 가능 조건 설정
* 좌석형 리소스의 행/열 구성 설정
* 특정 날짜/시간에 예약을 막는 예외 시간대 설정

### 🍀 실시간 WebSocket 통신
* **STOMP 프로토콜** 기반 양방향 통신
* Hold 생성/해제/예약완료 이벤트 브로드캐스트
* 리소스별 토픽 구독 (`/topic/hold/{resourceId}`)
* 사용자별 알림 큐 (`/user/queue/notifications`)

### 🍀 멀티테넌시 지원
* 기업별 독립 데이터 분리 (Company 기반)
* 커스텀 도메인 Slug 지원 (`/c/{company-slug}/...`)
* 기업별 관리자/매니저 권한 분리

### 🍀 통계 및 리포트
* 누적 예약/취소 건수
* 리소스 그룹별 예약 수 통계
* 시간대별 이용량 조회

<br><br>

## 🌐 접속 주소

### [플랫폼 관리자 바로가기](https://www.unibooker.n-e.kr/super/login)
- ID : super@unibooker.com
- PW : super1234

### [기업 관리자 바로가기](https://www.unibooker.n-e.kr/admin/login)
- ID : admin@hanwha.com
- PW : Admin1234!

### [고객 바로가기](https://www.unibooker.n-e.kr/c/hanwha-systems)
- ID : user.jeon@hanwha.com
- PW : User1234!

<br><br>

## 🧰 기술 스택

### Backend
<div>
  <img src="https://img.shields.io/badge/Java_17-007396?style=for-the-badge&logo=java&logoColor=white">
  <img src="https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white">
  <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white">
</div>
<div>
  <img src="https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white">
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
  <img src="https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socket.io&logoColor=white">
  <img src="https://img.shields.io/badge/JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white">
</div>

### Infra
<div>
  <img src="https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white">
  <img src="https://img.shields.io/badge/AWS_RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white">
  <img src="https://img.shields.io/badge/Upstash_Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
  <img src="https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white">
</div>

### Tools
<div>
  <img src="https://img.shields.io/badge/git-F05032?style=for-the-badge&logo=git&logoColor=white">
  <img src="https://img.shields.io/badge/github-181717?style=for-the-badge&logo=github&logoColor=white">
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black">
  <img src="https://img.shields.io/badge/discord-5865F2?style=for-the-badge&logo=discord&logoColor=white">
</div>
<br><br>

## 🏗️ 시스템 아키텍처
### V1
![3. 시스템아키텍처_v1](https://github.com/beyond-sw-camp/be17-fin-LinkVerse-UniBooker-BE/blob/develop/docs/3.%20시스템%20아키텍처_v1.png)

### V2
![4. 시스템아키텍처_v2](https://github.com/beyond-sw-camp/be17-fin-LinkVerse-UniBooker-BE/blob/develop/docs/3.%20시스템%20아키텍처_v2.png)

<br><br>

## 🛢️ ERD
![5. ERD](https://github.com/user-attachments/assets/0b21618e-43e3-4f0f-97cf-3f959b31c888)

<br><br>

## 📡 API 명세

### 대기열 API
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/queue/{resourceId}/join` | 대기열 참여 |
| GET | `/api/queue/{resourceId}/status` | 대기 상태 조회 |
| POST | `/api/queue/{resourceId}/consume` | 대기열 토큰 소비 |

### Hold API
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/hold/{resourceId}` | Hold 생성 |
| DELETE | `/api/hold/{resourceId}/release` | Hold 해제 |
| GET | `/api/hold/{resourceId}/status` | Hold 상태 조회 |

### 예약 API
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/reservations/{resourceId}` | 예약 생성 |
| GET | `/api/reservations/{id}` | 예약 상세 조회 |
| DELETE | `/api/reservations/{id}` | 예약 취소 |

### WebSocket
| Topic | Description |
|-------|-------------|
| `/topic/hold/{resourceId}` | Hold 상태 브로드캐스트 |
| `/user/queue/notifications` | 개인 알림 |

<br><br>

## 🖥 Swagger
> [Swagger-UI 바로가기](https://api.unibooker.n-e.kr/webjars/swagger-ui/index.html)

<br><br>

## 📺 기능 테스트
> [기능 테스트 보러가기](https://github.com/beyond-sw-camp/be17-fin-LinkVerse-UniBooker-BE/wiki/7.-%EA%B8%B0%EB%8A%A5-%ED%85%8C%EC%8A%A4%ED%8A%B8)

<br><br>

## 📄 프로젝트 상세 문서
#### 📌 프로젝트 기획서
> [프로젝트 기획서 보러가기](https://github.com/beyond-sw-camp/be17-fin-LinkVerse-UniBooker-BE/wiki/1.-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%EA%B8%B0%ED%9A%8D%EC%84%9C)
#### 📌 요구사항 정의서
> [요구사항 정의서 보러가기](https://github.com/beyond-sw-camp/be17-fin-LinkVerse-UniBooker-BE/wiki/2.-%EC%9A%94%EA%B5%AC%EC%82%AC%ED%95%AD-%EC%A0%95%EC%9D%98%EC%84%9C)
#### 📌 WBS
> [WBS 보러가기](https://github.com/beyond-sw-camp/be17-fin-LinkVerse-UniBooker-BE/wiki/4.-WBS)