﻿# SNS

- SNS 백엔드 프로젝트
- 단순 기능 구현 뿐만이 아닌 확장성, 장애 대처, 유지보수성을 고려하여 구현하는 것을 목표로 개발하였습니다.



## ✅ 사용 기술 및 개발 환경

Java, Spring Boot, MySQL, JPA, Redis 등

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

### 1. 인프라의 확장성 👉[Click](http://www.google.co.kr).

API 서버는 단순히 트래픽을 처리할 수 있는 인프라를 증설하는 것으로 충분하지만, DB의 경우에는 모든 DB 서버가 동일한 데이터를 저장하고 조회 시에도 동일한 결과를 반환해야 하는 데이터 정합성이 핵심이라고 생각했습니다.

이에 데이터가 저장되는 주체와, 저장한 데이터를 받아와 동기화시키는 master - slave 구조를 이용하기로 하였습니다.
쓰기 작업은 master 서버에서 이루어지고, 읽기 작업은 slave 서버에서 이루어지게 하여 부하를 효율적으로 분산시킬 수 있기 때문입니다.

더불어 쓰기 작업은 단일 master 서버에서 이루어지기 때문에 단일 장애 지점이 될 수 있었고, Orchestrator를 통해 자동 장애 복구 기능을 구현하여 고가용성을 확보하고자 하였습니다.

복제 타입 중 바이너리 로그 위치 기반 복제의 경우, 각 slave 서버가 master 서버의 로그 파일명과 position을 기준으로 복제를 수행하기 때문에, 새로운 master 서버 승격 시 모든 slave 서버의 복제 설정을 수동으로 변경해야 했습니다. 또한 장애 발생 시점의 정확한 position을 찾지 못하면 데이터 정합성이 깨질 수 있는 위험이 있었습니다.

반면 GTID 기반 복제는 트랜잭션마다 고유 ID를 부여하여 데이터베이스 간 복제 상태를 정확하게 추적합니다. master 서버 장애 발생으로 새로운 master 서버가 선정되었을 때, 각 slave 서버는 자신이 어디까지 읽었는지 정확히 알고 있어 누락이나 중복 없이 복제를 이어갈 수 있습니다. 이러한 명확한 트랜잭션 추적 기능은 자동 페일오버 도구인 Orchestrator와 결합했을 때 더욱 빛을 발했습니다. Orchestrator가 각 데이터베이스의 복제 상태를 정확하게 파악하고 장애 상황에서 자동으로 master 서버 승격 작업을 수행하여 복제 토폴로지 변경이 자유롭다는 장점이 있어 복제 타입으로 채택하였습니다.

복제 동기화 방식으로서는 비동기 복제 방식을 택하였습니다. 반동기 복제 방식은 master 서버가 slave 서버로부터 트랜잭션 수신 확인을 받을 때까지 대기하여 데이터 정합성은 높일 수 있지만, 그만큼 master 서버의 성능이 저하될 수 있다는 단점이 있었습니다.

송금이나 결제 등 강력한 데이터 정합성을 요구하는 금융 서비스는 반동기 복제 방식이 적합할 수 있겠지만, SNS 서비스 특성상 실시간 상호작용이 빈번하게 발생하기 때문에 성능을 최우선으로 고려하여 비동기 방식을 선택하였습니다.

분산 데이터베이스의 고가용성은 Orchestrator를 통해 확보하였습니다.

docker-compose를 통해 master 서버 컨테이너 1대, slave 서버 컨테이너 2대, Orchestrator 컨테이너 1대를 통해 복제 토폴로지를 관리하는 아키텍처를 로컬 환경에서 구현하였습니다. 또한 RecoverMasterClusterFilters 설정을 통해 실제 자동 장애 복구 기능을 활성화하였고, 임의로 컨테이너를 중지시키며 failover 테스트를 진행하였습니다.

다음은 master - slave 구조의 DB를 API 서버와 연동하는 방법을 찾아봤습니다.

AbstractRoutingDataSource을 상속해 커넥션 시 사용할 DataSource를 결정하는 방법과, 처음부터 master 서버와 slave 서버 DataSource를 분리하여 각각 해당하는 EntityManagerFactory, TransactionManager를 만들고, Repository 패키지를 분리하고 명시하여 사용하는 방법이 있었습니다.

Repository 패키지 분리 방식은 컴파일 타임에 잘못된 DB 접근을 방지하고 코드만으로도 DB 접근 의도 파악이 명확하며 각 DB에 최적화된 설정을 적용할 수 있지만, Repository 코드 중복이 발생하고, 읽기 - 쓰기 데이터소스마다 EntityManagerFactory와 TransactionManager를 별도로 설정해야 했기에 빈 객체가 늘어나면서 메모리 사용량이 증가할 수 있었습니다.

반면 AbstractRoutingDataSource를 상속받고, TransactionSynchronizationManager.isCurrentTransactionReadOnly()를 통해 DataSource를 결정하는 방법은 @Transactional을 명시해야 하므로 트랜잭션 속성 누락 시 의도치 않은 master 서버 DB 접근이 가능하고 코드만으로는 어떤 DB를 사용하는지 파악이 어려우며 런타임에서야 DB 접근 오류를 발견할 수 있다는 단점이 있었지만, 불필요한 코드 중복이 없고 적은 빈 객체를 관리하여 유지보수가 쉽고, @Transactional의 readonly 속성을 통한 JPA의 스냅샷 미생성, 더티 체킹 스킵으로 성능 개선 효과를 자연스럽게 활용할 수 있다는 장점이 명확하였습니다.

AbstractRoutingDataSource를 상속받아 ReplicationRoutingDataSource를 구현하고, AtomicInteger를 활용하여 Thread-Safe한 라운드 로빈 알고리즘으로 라우팅을 구현하였습니다. 또한, ReplicationRoutingDataSource를 매개변수로 사용하여 LazyConnectionDataSourceProxy를 최종 DataSource 객체로 반환함으로써 불필요한 커넥션 낭비를 방지하고, 시스템 자원을 효율적으로 관리할 수 있도록 설계했습니다.

서비스 클래스에 기본적으로 @Transactional 어노테이션을 설정하되, SELECT 쿼리를 사용하는 메서드에는 @Transactional(readonly = true) 속성을 부여하여 사용하는 DataSoruce를 분리하게 하였습니다.

문제는 자동 장애 복구 시 장애가 발생한 기존 master DB, master로 승격한 기존 slave DB 등의 정보를 파악하여 targetDataSources와 DefaultTargetDataSource를 변경해 줘야 한다는 점이었습니다.

장애가 생긴 master 서버는 다운된 상태에서, 승격된 slave 서버가 master 서버의 포트로 restart 한다면 애플리케이션 레벨에서 DB의 서버 가용 상태에 종속되지 않고 낮은 결합도로 유연하게 운용할 수 있겠다고 생각했지만, 승격된 slave 서버가 완전히 가동되기 전에 요청이 전달되는 경우 데이터의 무결성이 깨질 수 있다는 문제가 있었습니다.

그렇다고 애플리케이션 레벨에서 헬스 체크를 통해 DB 가용 상태를 파악하고 DataSource를 초기화하자니, 헬스 체크 주기가 길어질 경우 복구 지연이 발생하고, 주기를 짧게 설정하면 시스템 부하가 증가할 수 있다는 문제가 있었습니다.

Orchestrator의 Webhook 기능과 이벤트 리스너를 통해 이벤트를 감지하고 DataSource를 초기화하자니, 분산 애플리케이션 환경에서 네트워크 지연이나 이벤트 누락으로 인해 초기화 타이밍이 어긋날 가능성이 있다는 문제가 있었습니다.

무엇보다 애플리케이션이 DB 상태에 종속되어 있다는 점이 근본적인 문제점이었습니다. 이는 애플리케이션이 DB의 가용 상태를 지속해서 추적해야 하며, 장애나 복구 상황에 따라 추가적인 로직을 관리해야 하는 부담으로 이어졌습니다. 이로 인해 코드 작성이 늘어나면서 시스템 복잡도가 증가하고, 장애 상황에서 신속하고 안정적인 복구가 어렵다는 문제가 있었습니다.

nginx의 로드 밸런싱 개념에서 착안하여 프록시 계층을 도입하고자 하였습니다.

API 서버와 DB 서버 사이 프록시 서버를 둬, API 서버는 DB를 동적으로 연결하지 않고, 프록시 서버만 바라보게 한 다음, 프록시 서버에게 DB 연결에 대한 책임을 위임하고자 하였습니다.

프록시 서버가 트랜잭션 속성을 파악하고, failover를 감지하여 적절한 DB에 연결하게 한다면 애플리케이션 레벨에서 복잡한 로직을 관리하지 않아도 되어, 시스템의 유연성을 크게 향상할 수 있다고 보았습니다.

단점으로서는 프록시 서버를 경유하기 때문에 추가적인 네트워크 통신이 발생하여, 요청-응답 간 네트워크 오버헤드가 우려되었습니다.
또한, 단순히 네트워크 오버헤드뿐만 아니라, 프록시가 트랜잭션을 분석하고 라우팅 방식을 결정하는 과정에서도 오버헤드가 발생할 수 있으며, 이는 곧 응답 시간의 저하로 이어질 가능성이 있었습니다.
프록시 서버의 단일 장애 지점을 극복하기 위해 이중화를 설계하고 구현하는 추가적인 인프라 고려 사항도 필요했습니다.

기존에는 가장 단순하게 구현할 수 있는 라운드 로빈 알고리즘을 라우팅 알고리즘으로 택하였지만, DB 서버의 상태를 고려하지 않고 무조건 순차적으로 분배하고 있어 부하의 불균형이 발생할 수 있는 문제가 있었고, 다른 알고리즘을 구현하기 위해선 추가적인 로직이 필요하였습니다.

이런 상황에선 프록시 서버의 단점보다 애플리케이션 레벨에서 동적 DB 라우팅과 복잡한 failover 로직을 직접 구현할 필요가 없어진다는 장점이 매우 매력적으로 다가왔습니다.

실제로 검색을 해보고 나니 ProxySQL이라는 오픈소스 프록시가 존재했고, 제 요구사항을 충족하는 다양한 기능이 있어 이용해 보고자 하였습니다.

docker를 통해 ProxySQL 서버 컨테이너 1대를 띄우고, sessions_sort=true 설정을 통해 가장 부하가 적은 서버에 우선으로 연결하도록 라우팅 알고리즘을 설정하였습니다. master 서버 1대는 host group 10으로, salve 서버 2대는 host gorup 20으로 설정한 후,
쓰기 쿼리가 실행될 경우 10으로 라우팅 되게, 읽기 쿼리가 실행될 경우 20으로 라우팅 되게 설정하였습니다.

애플리케이션의 DataSource는 이제 더 이상 AbstractRoutingDataSource을 상속받아 구현하지 않아도 됐고, 단순히 jdbcUrl만을 ProxySQL 서버의 주소로 설정하게 하여 HikariDataSource 자체를 매개변수로 전달하게 하였습니다.

master DB 컨테이너를 임의로 중지시키고, Orchestrator의 auto failover 시 slave DB 1이 master로 승격하고, read_only 속성을 ProxySQL이 감지해 새로 Writer 호스트 그룹으로 등록해 mysql_servers를 업데이트하는 과정을 확인할 수 있었습니다.

이로써 Orchestrator를 통해 고가용성을 확보하고, ProxySQL를 통해 쿼리를 동적으로 라우팅하여 부하를 분산하는 아키텍처를 구성하였습니다.

# In-memory 데이터베이스

서비스의 규모가 확장됨에 따라, 읽기 부하 분산을 위해 slave 서버를 추가로 확장할 수 있습니다.

그러나 master 서버는 각 slave 서버의 요청에 따라 지속해서 바이너리 로그를 읽고 이를 전달해야 하는 구조로 인해 slave 서버의 수가 증가할수록 master 서버의 복제 부하가 누적되어, 결과적으로 master 서버에 가해지는 부담이 커지는 문제가 발생하게 되고, 현실적으로는 slave 서버를 무한정 늘릴 수 없다는 문제가 있었습니다.

이에 자주 조회되는 데이터는 메모리에 캐시하여 디스크 I/O 작업을 줄이고, 데이터를 메모리에서 처리함으로써 성능을 개선하는 방법을 고려하였습니다.

저는 API 서버의 분산 환경 상황에서의 저장소 중 로컬 캐시와 글로벌 캐시를 비교하였습니다.

로컬 캐시는 각 서버의 메모리에 데이터를 저장하여 빠른 응답 속도를 제공하지만, 서버 간 데이터 동기화가 어렵고 다수의 서버에서 동일한 데이터가 중복으로 저장되어 메모리가 비효율적으로 사용되는 문제가 있었습니다.

글로벌 캐시는 중앙화된 캐시 서버에서 데이터를 관리하여 데이터 일관성을 보장하고 메모리를 효율적으로 사용할 수 있으며, 캐시 서버 확장을 통한 처리량 증가가 가능하다는 장점이 있어 선택하게 되었습니다.

글로벌 캐시 중 Redis와 Memcached를 비교했을 때, Memcached는 write 시 성능이 좋다는 장점이 있었지만, Redis는 다양한 자료구조, 데이터 영속성을 위한 백업 기능, pub/sub 기능 등 다양한 기능을 지원할 뿐만 아니라 커뮤니티가 활발하고 시중에 나와 있는 서적도 많아 정보를 얻기 쉬웠습니다.

이에 글로벌 캐시로 Redis를 선택하게 되었습니다.

### 2. Redis 기반 피드 관리 전략 👉[Click](http://www.google.co.kr).
피드를 저장할 저장소를 선택하는 과정에서 깊이 고민했습니다. 

피드는 실시간 응답이 중요하고 최신 게시글이 상단에 위치해야 하며, 조회된 게시글은 삭제되는 특징이 있습니다.
RDB는 디스크 I/O로 인한 응답 지연이 있고 영속성에 초점이 맞춰져 있어 피드라는 데이터 특성과 맞지 않았습니다. 
결국 메모리 기반으로 빠른 응답이 가능한 Redis를 저장소로 선택했고, 이후 다음과 같은 고민을 하였습니다.

- ### 피드 데이터와 캐시 데이터 분리
팔로워는 하나의 피드를 가집니다. 
게시글 데이터가 피드 자체에 저장된다면 중복된 데이터가 유저마다 저장되어 메모리 사용량이 증가할 수 있는 문제가 있습니다. 
이에 피드에 저장되는 값은 게시글의 id만 저장하되, 게시글 캐시는 따로 저장하여 메모리를 효율적으로 관리하였습니다.
피드는 Sorted Set 자료구조를 활용하여 postId를 score로 사용함으로써 자동 내림차순 정렬되도록 구현했습니다.

![스크린샷 2024-11-21 124351](https://github.com/user-attachments/assets/254ccda4-ffc9-4156-beb3-e2bb66508360)

- ### 파이프라이닝 활용 Redis 추가 통신 및 응답 지연 개선

Redis 기반 피드 시스템 운영 중 네트워크 통신으로 인한 성능 저하 문제가 발생했습니다. 

String 자료구조에서는 MSET 명령어를 통해 클러스터 환경에서도 해시 태그를 사용하지 않고도 Lettuce 클라이언트가 단일 명령어로 처리하여 각 슬롯에 여러 키를 저장할 수 있었습니다.
그러나 Sorted Set 자료구조에는 여러 키를 한 번에 저장할 수 있는 멀티 명령어가 제공되지 않았습니다.

게시글 작성 시에는 작성자의 팔로워 목록을 조회하고, 각 팔로워의 피드에 새 게시글을 저장하는 과정에서 팔로워 수만큼의 Redis 통신이 발생했습니다.

TCP 모델을 사용하는 Redis는 네트워크 요청이 발생할 때마다 3-Way 핸드셰이크를 거치기 때문에 이 과정에서 네트워크 요청이 많아지면
각 요청에 대한 커넥션 과정에서 지연이 발생하게 되고, RTT가 증가하면서 네트워크 병목이 발생할 수 있습니다.

또한, Redis 내부에서 데이터를 읽고 쓰기 위한 소켓 I/O 작업 시 호출하는 시스템 콜 오버헤드로 인해 자원이 불필요하게 소모될 수 있으며, 
높은 요청 처리량이 요구되는 환경에서는 이러한 오버헤드가 병목으로 작용하여 성능 저하를 초래할 수 있습니다.

이 문제를 해결하기 위해 Redis의 파이프라이닝 기능을 활용한 일괄 처리 방식을 도입했습니다. 
게시글 작성 시에는 모든 팔로워의 피드 업데이트 명령을 파이프라인에 누적했다가 한 번에 실행하는 방식으로 변경했습니다. 

이러한 개선을 통해 Redis와의 네트워크 통신 횟수를 대폭 줄일 수 있었습니다.

    // 10000명의 팔로워를 가진 유저
        List<Long> followerIds = followRepository.findFollowerUserIds(1000220L);

        long start = System.currentTimeMillis();
        for (Long followerId : followerIds) {
            stringRedisTemplate.opsForZSet().add("feed:userId" + followerId, "1000:2024111111:0", 1000);
        }

        System.out.println("파이프라이닝 X 총 피드 생성 소요 시간 :" + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();

        List<Object> result = stringRedisTemplate.executePipelined(
                new RedisCallback<Object>() {
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                        for (long followerId : followerIds) {
                            stringRedisConn.zAdd("feed:userId" + followerId, 1000, "1000:2024111111:0");
                        }
                        return null;
                    }
                });

        System.out.println("파이프라이닝 O 총 피드 생성 소요 시간 :" + (System.currentTimeMillis() - start) + "ms");

![스크린샷 2024-11-20 203829](https://github.com/user-attachments/assets/7cb93f5e-0816-419f-a18b-34c0fd6f1905)

- ### 비동기 피드 생성

기존에는 게시글 등록과 피드 생성이 동기적으로 처리되고 있었습니다.
하지만 게시글 등록과 피드 생성은 서로 다른 책임을 가지고 있으며, 게시글의 생명주기가 피드의 전파 성공 여부에 종속될 이유가 없었습니다.
결과적으로 피드 생성이 게시글 등록에 영향을 주지 않도록 할 필요가 있었습니다.

비동기 처리 방식으로 @Async, WebFlux, 메시지 큐 등을 검토했습니다. WebFlux는 전체 시스템을 리액티브로 구성해야 했으며, 메시지 큐는 모놀리틱 아키텍
처에서 불필요한 인프라 비용이 발생했습니다.

이에 따라 코드의 간결성과 관리의 용이성을 위해 @Async를 사용하여 피드 생성 작업을 비동기 처리하기로 하였습니다. 
이때 톰캣의 기본 스레드 풀이 비동기 처리에 영향을 미치지 않도록 독립된 스레드 풀을 구성하여, 비동기 작업이 별도의 스레드에서 실행될 수 있도록 했습니다.

결과적으로 게시글 등록과 피드 생성의 책임을 분리하여 사용자는 피드 생성 작업의 결과 여부와 관계없이
즉시 게시글 등록 완료 응답을 받을 수 있게 되어 응답성이 개선되었습니다.

- ### 인플루언서 게시글로 인한 대규모 피드 생성 문제 개선
많은 팔로워를 가진 인플루언서가 게시글을 등록 시 팔로워의 수만큼 피드를 저장해 발생하는 메모리 사용량 급증 문제를 해결하기 위한 최적화를 시도했습니다.
모든 팔로워의 피드에 게시글을 즉시 저장하는 대신, 게시글은 DB에만 저장하고 팔로워가 피드를 조회할 때 피드 내 가장 낮은 postId보다 높은 postId를 가진
게시글만 가져와 병합하는 방식을 고안했습니다.

하지만 이 과정에서 두 가지 문제가 발생했습니다. 
첫째, 피드가 비어있을 때의 게시글 조회 기준을 새로 정의해야 했고, 둘째, 만료된 게시글의 중복 조회를 방지하기 위한 조회 이력 관리가 필요했습니다.
조회 이력을 DB에 저장하면 데이터가 과도하게 증가하고, Redis에 저장하면 여전히 메모리 효율이 떨어지는 딜레마에 빠졌습니다
.
문제 해결에 몰두하던 중, 실제 시나리오를 분석해보니 이 케이스는 '인플루언서만 팔로우하는 사용자가 있고, 
그 인플루언서들의 게시글 작성 주기가 긴 경우'라는 매우 드문 상황을 위한 불필요한 최적화였음을 깨달았습니다.
결과적으로 배보다 배꼽이 더 커진 상황이 되었습니다.

이 경험을 통해 두 가지 중요한 교훈을 얻었습니다. 극단적인 케이스를 위한 과도한 최적화가 오히려 전체 시스템 구조를 망칠 수 있다는 점과, 성능 개선과 시스템
복잡도 사이의 트레이드오프를 신중히 고려해야 한다는 점입니다.

### 3. Redis 활용 성능 개선 👉[Click](http://www.google.co.kr).

게시글에 좋아요를 누르면 post_likes 테이블에 유저 ID와 게시글 ID가 저장됩니다.
사용자가 게시글 정보를 요청할 때는 좋아요 테이블에서 좋아요 개수를 COUNT하여 응답하는데, 이 방식은 성능에 부담을 줄 수 있습니다. 
이를 해결하기 위해, 게시글의 좋아요 개수는 like_count 컬럼에 저장하여 관리하고 있습니다.

많은 팔로워를 가진 인플루언서의 게시글은 동시에 대규모 좋아요 요청을 받을 가능성이 높습니다.
이때, like_count 컬럼을 업데이트 경합 도중 락으로 인한 성능 저하가 발생할 수 있는 잠재적인 문제가 있습니다.

이 문제를 해결하기 위해 Redis를 활용하기로 했습니다.
처음에는 Redis의 String 자료구조를 사용하여 INCR 연산으로 좋아요 개수를 관리하려 했으나, 
이는 어떤 유저가 어떤 게시글에 좋아요를 눌렀는지에 대한 정보를 저장할 수 없다는 단점이 있었습니다.

이에 Redis의 Set 자료구조를 사용하기로 했습니다. 
Set은 중복된 값을 허용하지 않기 때문에 좋아요 여부를 쉽게 관리할 수 있고, SCARD 명령어를 사용하여 좋아요 개수를 파악할 수 있습니다.

모든 게시글의 좋아요를 관리해 데이터가 무한히 증가하는 문제를 방지하기 위해, 인플루언서의 최근 게시글만 Redis에 저장하게 하였습니다.

특정 게시글이 뉴스 기사 등 알고리즘을 타 예상치 못한 대규모 트래픽을 받을 확률이 아예 없다고 할 수는 없지만, 
최근 게시글에 한해 좋아요 요청이 집중될 가능성이 높다는 점과 시스템 복잡도를 고려하여 최근 게시글만 관리하는 방식을 선택했습니다.

인플루언서가 게시글을 작성할 때마다 Redis에 post:{postId}:likes라는 키를 생성하고, 사용자가 좋아요를 누르면 해당 게시글에 대해 유저의 ID를 값으로 저장합니다.
또한 주기적으로 스케줄러를 활용하여 좋아요 정보를 데이터베이스에 Flush하는 Write Back 패턴을 활용하였습니다.

Redis에서 좋아요 정보를 가져올 때, KEYS 명령어는 블로킹 방식으로 작동하여 Redis의 성능에 부담을 줄 수 있고, 
대량의 데이터를 한 번에 로드하면 API 서버에도 과부하가 발생할 위험이 있습니다.
이러한 성능 저하를 방지하기 위해, SCAN 명령어를 사용하여 점진적으로 데이터를 조회하도록 개선하였습니다.



결과적으로 인플루언서가 최근 작성한 게시글에 대한 좋아요를 Redis에서 관리하게 하여 락으로 인한 성능 저하 문제를 개선하였습니다.

# Redis 기반 데이터 캐시

게시글, 댓글 등 단순 캐시 데이터는 Look Aside 방식을 기본 읽기 전략으로 선택하되, 수정이 빈번하지 않기에 Write Though 방식을 통해 데이터 정합성을 지켰습니다.

메모리 관리를 위해 Redis의 maxmemory-policy를 volatile-lru로 설정하였습니다.
별도 타임스탬프로 관리되는 피드와, 주기적으로 DB와 병합되는 좋아요 데이터를 제외한 TTL이 설정된 키 중 최근에 사용되지 않은 키를 먼저 제거하도록 하였습니다.

### 4. Redis 장애 대처 👉[Click](http://www.google.co.kr).

- ### Redis Cluster Replication
Redis를 단일 노드로 운영할 경우 해당 노드 장애 시 서비스 중단이 불가피하고, 단일 노드의 메모리 한계로 인한 확장성 제약이 있었습니다.
피드 같은 경우 기본적으로 Redis에 저장되고, 실패 피드 복구 작업으로 인한 시스템 부하가 증가할 염려가 있었습니다.
좋아요 데이터 같은 경우 Write Back 패턴으로 운영되기에 서버 장애 시 데이터 유실에 취약했습니다. 

이를 해결하기 위해 Redis의 고가용성 확보를 위한 Sentinel과 Cluster 방식을 검토했습니다.

Sentinel 방식은 마스터 노드 장애 시 자동 페일오버를 제공하고 구성이 비교적 단순하지만,
데이터가 단일 마스터 노드에 집중되어 확장성에 제약이 있고 메모리 한계를 극복하기 어렵다는 단점이 있습니다.

Cluster 방식은 데이터를 여러 마스터 노드에 분산 저장하는 샤딩을 통해 수평적 확장이 용이하고, 각 마스터 노드에 슬레이브를 둠으로써 가용성도 확보할 수 있었습니다. 
시스템의 미래 확장성과 대용량 데이터 처리를 고려할 때, 초기 구성의 복잡성을 감수하더라도 Cluster 방식이 장기적으로 더 유연한 아키텍처를 제공할 것으로 판단했습니다.

또한 Lettuce 클라이언트의 ReadFrom.REPLICA_PREFERRED 속성을 통해 레플리카 노드에 readonly 속성을 부여함으로써 읽기 부하를 분산하였습니다.

![스크린샷 2024-12-01 143926](https://github.com/user-attachments/assets/4836ba82-db03-4747-879b-db286c79b216)


- ### Resilience4j 활용 재시도 로직 및 서킷 브레이커 구현

Redis 서버와 통신이 불가하여 예외가 발생할 경우
서버는 Redis 액션(조회/저장) 요청마다
(redis connection wait timeout 시간 * redis connection retry 횟수) 만큼 시간을 낭비하고 그만큼 응답 시간은 증가하게 됩니다.

사용자는 늘어난 응답 시간 만큼 의미 없는 대기를 할 잠재적인 이슈가 있었습니다.

재시도 로직을 구현하기 위해 RetryTemplate, Resilience4j, Retryable 등을 검토했습니다.
재시도 로직으로 인한 시스템 부하를 고려하여 Circuit Breaker 패턴을 활용할 수 있는 Resilience4j 라이브러리를 선택하였습니다.

재시도 전략으로는 지수 백오프를 선택했습니다. 즉시 재시도는 Redis 장애 상황에서 부하를 가중시킬 수 있었고, 고정 지연은 최적 대기 시간 설정이 어려웠습니다.
반면 지수 백오프는 2초, 4초, 8초로 간격을 늘려가며 시스템 회복 시간을 확보하고 급격한 재시도를 방지할 수 있었습니다. 

재시도 로직으로 인한 시스템 부하를 고려하여 Circuit Breaker 패턴을 적용했습니다. 
1분간 50% 이상의 실패율이 발생하면 Circuit이 Open 되어 Redis 호출을 차단하였습니다. 
이를 통해 Redis 장애 상황에서 불필요한 재시도를 방지하여 시스템을 보호할 수 있었습니다.

- ### 생성 실패 피드 복구 작업 구현

Redis 서버에 장애가 생긴 상황에서, 재시도 최대 횟수를 초과하거나 서킷 브레이커가 OPEN 상태인 경우, 피드 생성 작업을 큐에 추가하여 복구 작업을 진행하게 하였습니다.
큐에 작업이 쌓여 메모리 사용량이 무한히 늘어나는 상황을 방지하기 위해 최대 사이즈를 설정하였습니다.
서버에 장애가 생기면 스케줄러는 서버에 주기적으로 PING을 보내며, 정상적으로 서버가 복구될 경우엔 복구 작업을 진행합니다.
피드 생성 자체가 하나의 파이프라인 메서드이기 떄문에, 별도의 배치 사이즈 없이 메서드 자체를 복구 작업의 단위로 설정하였습니다.
이 때 기존의 API 커넥션과 더불어 피드 복구 작업이 Redis에 가하는 부하가 너무 커지는 것을 방지하기 위해, 지연 시간을 두고 복구 작업을 진행하게 하였습니다.

결과적으로 게시글 작성자의 팔로워는 모든 피드가 팔로워들에게 전파될 수 있도록 하여 사용자 경험을 개선하였습니다.

- ### Redis 장애 상황 피드 조회 시 fallback 구현

피드 데이터는 Redis에만 저장되기 때문에 Redis에 대한 의존성이 매우 강합니다.
이로 인해 Redis 서버에 장애가 발생하면, 사용자는 핵심 비즈니스 로직인 피드 조회 서비스를 이용할 수 없게 되는 문제가 발생할 수 있습니다.

이를 해결하기 위해, Redis 서버 장애 발생 시 데이터베이스에서 임의로 게시글을 선정하여 피드로 반환하는 fallback 메서드를 구현하였습니다. 
이 메서드는 Redis에 장애가 있을 경우, 피드 조회 요청을 데이터베이스에서 임시로 처리하여 서비스의 가용성을 유지하도록 도와줍니다.

결과적으로 Redis 장애 상황에서도 사용자에게 일정한 품질의 피드 조회 서비스를 제공할 수 있게 되었습니다. 
Redis 장애 시에도 서비스 중단 없이 안정적으로 피드를 제공하며, 사용자 경험을 향상시킬 수 있었습니다.

### 5. AOP 활용 중복 코드 최소화 및 관심사 분리

- ### 파이프라이닝 로직 분리

파이프라인을 활용한 메서드에는 팔로워 수만큼 피드를 생성하거나, 피드에서 조회한 postId로 캐시를 조회하는 등의 작업이 포함됩니다.
이러한 과정에서 파이프라인 코드가 중복되어 유지보수에 어려움이 있었습니다.

    // 피드 생성
        List<Object> result = stringRedisTemplate.executePipelined(
                new RedisCallback<Object>() {
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                        for (long followerId : followerIds) {
                            stringRedisConn.zAdd(generateFeedKey(followerId), postId, feedValue);
                        }
                        return null;
                    }
                });
                
    // 캐시 조회
        List<Object> result = stringRedisTemplate.executePipelined(
                new RedisCallback<Object>() {
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                        for (Long postId : postIds) {
                            stringRedisConn.get("postId:" + postId);
                        }
                        return null;
                    }
                });
    

이를 해결하기 위해, AOP를 활용하여 중복 코드를 최소화하고 횡단 관심사를 분리하는 방법을 채택했습니다.

@RedisPipeline이라는 커스텀 어노테이션을 정의하여, 해당 어노테이션이 적용된 메서드에서 파이프라인 처리를 자동으로 수행하도록 했습니다.

AOP 로직은 파이프라인을 열고, 커넥션을 ThreadLocal을 이용해 connectionHolder에 저장합니다. 메서드 실행 시, 커넥션은 connectionHolder에서 가져와 명령어를 실행합니다.

    // AOP 로직
    @Around("@annotation(RedisPipeline)")
    private Object executeWithPipeline(ProceedingJoinPoint joinPoint) {
        List<Object> results = stringRedisTemplate.executePipelined(
                new RedisCallback<Object>() {
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                        try {
                            RedisPipelineContext.setConnection(stringRedisConn);
                            return joinPoint.proceed();
                        } catch (Throwable throwable) {
                            throw new RuntimeException("Pipeline execution failed", throwable);
                        } finally {
                            RedisPipelineContext.clear();
                        }
                    }
                });
        return results;
    }

    // 피드 생성
    @RedisPipeline
    public void addFeedInRedisPipeLine(List<Long> followerIds, long postId, String feedValue) {
        StringRedisConnection connection = RedisPipelineContext.getConnection();
        for (Long followerId : followerIds) {
            connection.zAdd(generateFeedKey(followerId), postId, feedValue);
        }
    }

    // 캐시 조회
    @RedisPipeline
    public List<Object> getCachedPosts(List<Long> postIds){
        StringRedisConnection connection = RedisPipelineContext.getConnection();
        for (Long postId : postIds) {
             connection.get("postId:"+postId);
        }
        return null;
    }

![스크린샷 2024-11-30 221021](https://github.com/user-attachments/assets/9796669d-711b-4257-879e-8e0551712996)

    
이로써 파이프라이닝 처리가 필요한 로직을 재사용 가능하게 만들고, 중복 코드를 제거하여 코드의 유지보수성을 높였습니다.

- ### 복구 작업 추가 로직 분리

기존에는 재시도와 서킷 브레이커 AOP 로직에서 최종 실패 시, joinPoint의 메서드명을 확인하여 복구 작업을 추가하고자 하는 메서드에만 이를 적용했습니다.

대표적으로 피드 생성 실패 시 복구 작업을 추가하도록 작성하였습니다.
하지만 서비스가 커져감에 따라 복구 작업을 추가하고자 하는 메서드가 늘어날 수 있다는 문제를 인식했습니다.
재시도 로직과 복구 작업 추가 로직이 강하게 결합되었고, 복구해야 할 메서드가 늘어날수록 if 문이 무한히 증가하여 유지보수가 어려워지는 문제가 발생했습니다.

이를 해결하기 위해 복구 작업을 수행하는 어노테이션을 작성하고, AOP 로직을 분리하여 복구가 필요한 메서드에만 어노테이션을 설정하도록 변경했습니다.

또한, 복구 작업 추가, 재시도, 파이프라이닝 등 여러 AOP를 활용할 때에는 @Order 어노테이션을 설정하여 AOP 실행 순서를 제어하였습니다.
결과적으로 코드의 확장성과 유지보수성이 크게 향상되었으며, 새로운 복구 작업을 추가하거나 AOP 로직을 수정하는 과정이 훨씬 수월해졌습니다.


    // 강하게 결합된 기존 코드
    @Around("@annotation(RetryCircuit)")
    public Object retryCircuit(ProceedingJoinPoint joinPoint) {
        try {
            return retry.executeSupplier(() ->
                    circuitBreaker.executeSupplier(() -> {
                        try {
                            return joinPoint.proceed();
                        } catch (Throwable e) {
                            if (e instanceof RedisConnectionFailureException) {
                                throw (RedisConnectionFailureException) e;
                            }
                            throw new RuntimeException(e);
                        }
                    })
            );
        } catch (RedisConnectionFailureException e) {
            if (isSpecificMethod(joinPoint)) {
                log.info("피드 생성 작업입니다. 작업을 큐에 추가합니다.");
                redisWorkQueue.enqueue(joinPoint);
            } else {
                log.info("피드 생성 작업이 아닙니다. 작업을 큐에 추가하지 않습니다.");
            }
            return null;
        }
    }

    // 분리된 재시도 로직과 복구 작업 추가 로직
    
    @Around("@annotation(RetryCircuit)")
    public Object retryCircuit(ProceedingJoinPoint joinPoint) {
        try {
            return retry.executeSupplier(() ->
                    circuitBreaker.executeSupplier(() -> {
                        try {
                            return joinPoint.proceed();
                        } catch (Throwable e) {
                            if (e instanceof RedisConnectionFailureException) {
                                throw (RedisConnectionFailureException) e;
                            }
                            throw new RuntimeException(e);
                        }
                    })
            );
        } catch (RedisConnectionFailureException e) {
            throw e;
        }
    }

    @Around("@annotation(RedisRecovery)")
    public Object recoveryWorkEnqueue(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            if (e instanceof RedisConnectionFailureException) {
                redisWorkQueue.enqueue(joinPoint);
            }
        }
        return null;
    }


    // 피드 생성 메서드
    @RetryCircuit
    @RedisRecovery
    @RedisPipeline
    public void addFeedInRedisPipeLine(List<Long> followerIds, long postId, String feedValue) {
        StringRedisConnection connection = RedisPipelineContext.getConnection();
        for (Long followerId : followerIds) {
            connection.zAdd(generateFeedKey(followerId), postId, feedValue);
        }
    }



### 6. AWS S3 기반 PreSigned URL 활용 파일 업로드 로직 개선 👉[Click](http://www.google.co.kr).
AWS S3를 활용한 파일 업로드 로직 구현 과정에서 성능 개선이 필요했습니다. 
기존 파일 업로드 로직은 단일 스레드가 동기적으로 작업을 처리하고 있어 응답 시간이 길어지고,
대용량 파일 업로드 시 스레드가 장시간 점유되어 다른 API에 영향을 줄 수 있었습니다.

초기에는 CompletableFuture 기능의 별도 스레드 풀을 활용한 병렬 처리 방식을 고려했습니다.
S3 파일 업로드는 스레드의 생명주기 대부분이 BLOCKED 상태인 I/O 바운드 작업이기에 context switching 비용이 적고, 
톰캣 기본 스레드 풀과는 분리된 전용 스레드 풀을 사용하면 다른 API 영향도 최소화할 수 있다고 판단했습니다.

하지만 실제 테스트 과정에서 이상적인 스레드 수를 설정하기 어려운 여러 환경 요인이 발견되었습니다. 로컬 환경과 배포 환경의 테스트 환경 차이, 네트워크 대역
폭 제한으로 인한 병목 현상, S3 API 요청 제한 횟수 등 다양한 환경 요인을 고려해야 했고, 실제 사용자들의 다양한 파일 크기와 동시 업로드 패턴을 고려한 테스
트 시나리오 작성이 필요했으며, 프리티어 사용으로 인해 테스트에도 한계가 있었습니다.

피드 생성의 경우 Redis에 postId만 저장하는 간단한 작업이라 CompletableFuture를 활용한 비동기 처리가 효과적이었지만,
파일 업로드는 예측 불가능한 크기의 파일을 다루고 긴 네트워크 지연이 발생하는 등 다양한 외부 요인과 제약사항으로 인해 이상적인 비동기 처리 구현에 어려움이 있었습니다.
적은 트래픽에서는 여러 작업을 병렬 처리하여 응답 시간과 처리량이 당연히 개선되겠지만, 대규모 트래픽 상황에서는 file-size-threshold 이하의 파일이 메모리
에 직접 로드되어 스레드 점유 파일 바이너리로 인해 메모리 사용량이 급증하는 문제, 최대 스레드 생성/삭제 오버헤드 등의 리소스 문제가 발생했습니다. 

또한 요청 큐가 가득 찰 시 요청 제한 로직 구현, 업로드 재시도 구현, 파일 업로드 후 게시글 등록과 피드 생성 작업 간 데이터 일관성을 위한 분산 트랜잭션 구현 등 복잡
한 로직을 다수 구현해야 했고 시스템 복잡도가 점점 커졌습니다.

이를 해결하기 위해 업로드 서버를 분리하여 메시지 큐 기반 비동기 통신으로 API 서버의 리소스 부담을 줄이고자 했지만,
동일한 문제가 반복되었고 대용량 데이터가 메시지 큐에 부담을 주지 않기 위해 업로드 API를 먼저 호출할 경우 
사용자 인증 및 처리량 제한 등 추가 로직 구현으로 인해 업로드 서버의 책임이 오염될 우려가 있었습니다.

무엇보다 클라이언트 -> 서버 -> S3로 이어지는 네트워크 흐름에서 대규모 파일 데이터가 불필요하게 전송되는 부담이 컸습니다. 
또한 클라이언트에게 업로드 진행률을 실시간으로 제공하기 위해 SseEmitter를 사용했는데, 
이로 인해 서버와 클라이언트 간 지속적인 connection이 유지되어야 했고 주기적인 이벤트 전송으로 인한 추가적인 네트워크 비용이 발생했습니다. 
파일 업로드 시 서버 측 부담을 줄여줄 필요가 있었습니다.

결국 클라이언트에서 S3로 직접 업로드하는 방식을 통해 서버 측 부하를 줄이기로 결정했습니다. 
인증 정보 노출 없이 안전하게 업로드할 수 있는 PreSigned URL을 활용하기로 했습니다. 
PreSigned URL은 서버에서 임시 서명된 URL을 생성하여 클라이언트에 전달하고, 
클라이언트는 이 URL을 통해 직접 S3에 업로드할수 있어 서버 부하를 줄이면서도 보안을 유지할 수 있었습니다. 
또한 클라이언트에서 직접 S3 SDK를 통해 업로드 진행률을 확인할 수 있어 서버의 SseEmitter로 인한 추가적인 네트워크 비용도 절감할 수 있었습니다.

### 7. 서버 모니터링 및 경고 알림 👉[Click](http://www.google.co.kr).
안정적인 서비스 운영을 위해 모니터링 시스템을 구축했습니다.

Spring Actuator를 통해 애플리케이션의 상태, 메트릭, 트래픽, GC 현황, 스레드 상태 등 API 서버의 메트릭을 수집했습니다.
MySQL Exporter를 통해서는 커넥션 풀 현황, 쿼리 처리량, 슬로우 쿼리, 디스크 사용량 등 데이터베이스 관련 메트릭을, 
Redis Exporter를 통해서는 메모리 사용량, 캐시 적중률, 연결 클라이언트 수 등 캐시 서버 관련 메트릭을 수집하였습니다.

Prometheus를 통해 이러한 메트릭들을 주기적으로 수집하고 저장했으며, 수집된 데이터는 Grafana로 시각화하여 직관적인 대시보드를 구성했고, 
CPU, 메모리 사용량, API 응답 시간, 에러율, JVM 힙 메모리, 스레드 수 등 주요 지표들을 실시간으로 모니터링할 수 있게 되었습니다.

특히 AlertManager를 연동하여 크리티컬한 이슈 발생 시 즉각적인 대응이 가능하게 하였습니다. 
CPU 사용률이 임계치를 초과하거나, 힙 메모리 부족, 높은 에러율 발생, 데이터베이스 커넥션 풀 고갈, Redis 메모리 부족, 서버 다운 등 미리 정의한 alert_rules 에 따라 Gmail을 통해 자동으로 경고 알림을 받을 수 있도록 구성했습니다.
이를 통해 장애 상황을 사전에 감지하고 빠르게 대응할 수 있는 체계를 마련했으며, 문제 발생 시 효율적인 원인 분석이 가능해졌습니다.

![스크린샷 2024-11-21 122711](https://github.com/user-attachments/assets/a09bb03c-3511-4ecb-9bf2-1d5d09832eae)
![스크린샷 2024-11-21 122632](https://github.com/user-attachments/assets/555fe17d-b1cf-47a6-846c-3af291d8b3bb)
