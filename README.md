# SNS

- SNS 백엔드 프로젝트
- 단순 기능 구현 뿐만이 아닌 확장성, 장애 대처, 유지보수성을 고려하여 구현하는 것을 목표로 개발하였습니다.



## ✅ 사용 기술 및 개발 환경

Java, Spring Boot, MySQL, JPA, Redis 등

## ✅ Architecture

- ### MySQL Architecture
  
![스크린샷 2024-11-20 185129](https://github.com/user-attachments/assets/1e72af5b-c7b6-492c-9ba2-043228431b40)

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


### ✅ 주요 고려 사항

# MySQL master - slave
대규모 서비스에서 단일 데이터베이스의 한계를 극복하기 위한 방법으로, 쓰기 작업은 마스터 DB가 담당하고 읽기 작업은 여러 슬레이브 DB로 분산하는 구조를
구성했습니다. 또한 라운드로빈 방식으로 로드밸런싱하여 읽기 요청 부하를 분산하였습니다.

JPA와 DB를 연동할 때 master-slave 요청을 구분하기 위해 두 가지 방식을 고민했습니다. 

일반적으로 사용되는 Transactional(readonly=true/false) 방식은 구현이 단순하고 코드 중복이 없으며, 적은 수의 빈으로 메모리가 효율적이고 JPA 영속성 컨텍스트 최적화가 자동으로 적용되는 장점이 있지만, 트랜잭션 속성 누락 시 의도치 않은 Master DB 접근이 가능하고 코드만으로는 어떤 DB를 사용하는지 파악이 어려우며 런타임에서야 DB 접근 오류를 발견할 수 있다는 단점이 있습니다. 

반면 Repository 물리적 분리 방식은 컴파일 타임에 잘못된 DB 접근을 방지하고 코드만으로도 DB 접근 의도 파악이 명확하며 각 DB에 최적화된 설정을 적용할 수 있지만, Repository 코드 중복이 발생하고,
읽기 - 쓰기 데이터소스마다 EntityManagerFactory와 TransactionManager를 별도로 설정해야 했기에 빈 객체가 늘어나면서 메모리 사용량이 증가될 수 있었습니다.

저는 Transactional(readonly=true/false) 방식으로 요청을 구분하기로 결정했습니다. 코드 중복이 없어 유지보수가 쉽고 휴먼 에러 가능성이 작으며, JPA의
읽기 전용 최적화(스냅샷 미생성, 더티 체킹 스킵)를 자연스럽게 활용할 수 있다는 장점이 명확했습니다.

Repository 분리 방식은 명시성이라는 장점이 있지만, 실제로 트랜잭션 속성 누락으로 인한 문제는 테스트 단계에서 충분히 발견할 수 있고, CI/CD 파이프라
인에서의 테스트 자동화로 런타임 오류를 사전에 방지할 수 있으며, 모니터링과 로깅을 통해 DB 접근 패턴을 추적할 수 있습니다. 결과적으로 Repository 분리는
실제 문제를 해결하기 위한 비용이 이점보다 크고, 패키지 구조 복잡화와 빈 증가로 인한 개발 생산성 저하가 발생하므로, "과도한 엔지니어링은 좋은 엔지니어링
이 아니다"라는 관점에서 Transactional(readonly=true/false) 방식이 더 현명한 선택이라고 생각했습니다.

# In-memory 데이터베이스
현대 웹 서버는 RESTful API를 통해 클라이언트의 요청을 처리하며, 대부분의 요청이 데이터베이스 작업을 수반합니다.
이 과정에서 DB와의 네트워크 통신 지연과 디스크 I/O로 인해 스레드가 장시간 BLOCKED 상태에 머무르게 되어,
대규모 트래픽 상황에서는 데이터베이스 작업이 주요 병목 지점이 될 수 있습니다.

SNS 서비스는 읽기 비율이 높다는 점을 감안했을 때, 적절한 인덱스 설정과 쿼리 튜닝 등의 기본적인 최적화가 되었다는 가정 하에,
slave 서버를 늘리는 것이 한 가지 해결책이 될 수 있습니다. 그러나 slave 서버를 추가할수록 master 서버에 걸리는 복제 부하가 증가하기 때문에, 결국에는 한계가 존재할 수 있습니다.

이러한 문제를 해결하기 위해 Redis와 같은 인메모리 데이터베이스를 도입하여 빈번한 조회 데이터를 캐싱하거나, 응답성이 중요한 데이터를 저장해야 할 경우 
디스크 I/O를 메모리 접근으로 대체하여 병목 지점을 해결해야할 필요가 있습니다.

저는 분산 환경 상황에서의 저장소 중 로컬 캐시와 글로벌 캐시를 비교하였습니다.

로컬 캐시는 각 서버의 메모리에 데이터를 저장하여 빠른 응답 속도를 제공하지만, 
서버 간 데이터 동기화가 어렵고 다수의 서버에서 동일한 데이터가 중복으로 저장되어 메모리가 비효율적으로 사용되는 문제가 있었습니다.

글로벌 캐시는 중앙화된 캐시 서버에서 데이터를 관리하여 데이터 일관성을 보장하고 메모리를 효율적으로 사용할 수 있으며, 
캐시 서버 확장을 통한 처리량 증가가 가능하다는 장점이 있어 선택하게 되었습니다.

글로벌 캐시 중 Redis와 Memcached를 비교했을 때, Memcached는 write시 성능이 좋다는 장점이 있었지만, Redis는 다양한 자료구조, 데이터 영속성을 위한 백업 기능,
pub/sub 기능 등 다양한 기능을 지원할 뿐만 아니라 커뮤니티가 활발하고 시중에 나와 있는 서적도 많아 정보를 얻기 쉬웠습니다.

이에 글로벌 캐시로 Redis를 선택하게 되었습니다.

# Redis 기반 피드 관리 전략
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


- ### 피드 생명주기 관리 방법
피드 내 게시글은 조회된 순간에 삭제할지, 일정 시간 후 삭제할지 고민이 되었습니다.
사용자 입장에서 피드를 다시 조회해도 일정 시간은 게시글이 남아 있는 게 좋겠다고 생각했습니다.
이를 위해 '게시글 별 TTL 설정'과 '키 별 TTL 설정' 두 가지 접근법을 검토했고, 최종적으로 게시글 별 TTL 설정 방식을 채택했습니다.

키 별 TTL 설정 방식은 Redis의 기본 TTL 기능을 활용할 수 있고, 메타데이터가 없어 메모리 사용량이 적다는 장점이 있습니다. 
하지만 키 생성 시점에 TTL을 설정하면 최신 게시글이 TTL 만료와 함께 조기 삭제될 수 있고, 
게시글이 추가될 때 또는 피드를 조회할 때 TTL을 갱신하면 데이터가 무한히 증가하는 문제가 있습니다. 
또한 조회 여부와 관계없이 각 게시글이 동일한 TTL이 적용된다는 한계가 있습니다. 
만료 이벤트 리스너를 통한 미조회 게시글 DB 백업도 고려했으나, 키의 value를 보존할 수 없거나 메모리를 두 배로 사용해야 하는 문제가 있었습니다.

게시글 별 TTL 설정 같은 경우 'postId:timestamp:isViewed' 형식으로 저장해 게시글의 수명과 조회 상태를 통합 관리하는 방식입니다. 
게시글은 등록 시점의 timestamp로 기본 만료 시간이 설정되며, 조회 시에는 timestamp를 갱신해 더 빠른 만료를 유도합니다.
이에 사용자가 이미 조회한 게시글이더라도 일정 시간만큼은 피드에 남게 합니다. 조회 여부를 설정함으로써 한번 조회된 게시글의 timestamp를 갱신하지 않도록 하였고,
페이지 교체 알고리즘의 reference bit에서 착안한 0/1 플래그로 저장해 메모리 사용을 최적화하였습니다.

피드 조회는 페이징 방식을 사용하였습니다. 조회된 게시글과 미조회 게시글의 TTL이 다르게 설정되어 있어, 
미조회 게시글은 다음 요청 시에 피드 상단에 남아있을 수 있습니다. 이는 사용자가 놓친 게시글을 자연스럽게 다시 볼 수 있게 합니다.

이 방식은 캐시 미스 시 발생하는 DB 조회에도 긍정적인 영향을 미칩니다.
조회된 게시글이 먼저 만료되어 IN 절에 포함되는 데이터의 양이 줄어들기 때문에 DB 조회 성능 개선 효과도 얻을 수 있습니다.

만료 처리는 수동적, 능동적 두 가지 방식으로 구현했습니다. 수동적 방식은 피드 조회 시점에 만료된 게시글을 삭제하고
사용자가 피드를 조회하지 않는 한 데이터가 쌓이기 때문에 능동적 방식으로 스케줄러를 통해 피드를 검사하여 만료된 게시글이 있다면 삭제하게 하였습니다.

![스크린샷 2024-11-20 153325](https://github.com/user-attachments/assets/13d39909-4452-4304-b64c-590fcd2cb94e)

![스크린샷 2024-11-20 153559](https://github.com/user-attachments/assets/1897e5b2-9dca-4024-8e2d-dac80acc6edf)


- ### 파이프라이닝 활용 Redis 추가 통신 및 응답 지연 개선
Redis 기반 피드 시스템 운영 중 네트워크 통신으로 인한 성능 저하 문제가 발생했습니다. 

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

# Redis 기반 데이터 캐시
좋아요 수와 같이 빈번한 업데이트가 발생하는 카운터성 데이터는 Redis의 INCR 연산을 활용해 메모리상에서 빠르게 처리한 후 
주기적으로 DB에 동기화하는 Write Back 방식을 적용하여 잦은 DB 업데이트로 인한 성능 저하를 방지했습니다. 

게시글 같은경우 Look Aside 방식을 기본 읽기 전략으로 선택하되, 수정이 빈번하지 않기에 Write Though 방식을 통해 데이터 정합성을 지켰습니다.

메모리 관리를 위해 Redis의 maxmemory-policy를 volatile-lru로 설정하였습니다.
별도 타임스탬프로 관리되는 피드를 제외한 TTL이 설정된 키 중 최근에 사용되지 않은 키를 먼저 제거하도록 하였습니다.

# Redis 장애 대처

- ### Redis Cluster Replication
Redis를 단일 노드로 운영할 경우 해당 노드 장애 시 서비스 중단이 불가피하고, 단일 노드의 메모리 한계로 인한 확장성 제약이 있었습니다.
피드 같은 경우 기본적으로 Redis에 저장되고, 실패 피드 복구 작업으로 인한 시스템 부하가 증가할 염려가 있었습니다.
좋아요 카운터 같은 경우 Write Back 형식으로 운영되기에 서버 장애 시 데이터 유실에 취약했습니다. 
Write Through 패턴을 사용하자니 성능 이슈가 생길 염려가 있었습니다.

이를 해결하기 위해 Redis의 고가용성 확보를 위한 Sentinel과 Cluster 방식을 검토했습니다.

Sentinel 방식은 마스터 노드 장애 시 자동 페일오버를 제공하고 구성이 비교적 단순하지만,
데이터가 단일 마스터 노드에 집중되어 확장성에 제약이 있고 메모리 한계를 극복하기 어렵다는 단점이 있습니다.

Cluster 방식은 데이터를 여러 마스터 노드에 분산 저장하는 샤딩을 통해 수평적 확장이 용이하고, 각 마스터 노드에 슬레이브를 둠으로써 가용성도 확보할 수 있었습니다. 
시스템의 미래 확장성과 대용량 데이터 처리를 고려할 때, 초기 구성의 복잡성을 감수하더라도 Cluster 방식이 장기적으로 더 유연한 아키텍처를 제공할 것으로 판단했습니다.

또한 Lettuce 클라이언트의 ReadFrom.REPLICA_PREFERRED 속성을 통해 레플리카 노드에 readonly 속성을 부여함으로써 읽기 부하를 분산하였습니다.

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

# AOP 활용 중복 코드 최소화 및 관심사 분리

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

- ### 재시도 + 서킷 브레이커 패턴 결합
Resilience4j의 재시도 + 서킷 브레이커 로직은 코드에 명시하여 작성하거나, 어노테이션을 통해 추상화할 수 있었습니다.
하지만, 재시도 어노테이션과 서킷 브레이커 어노테이션을 각각 독립적으로 사용해야 했고, ~~






# AWS S3 기반 PreSigned URL 활용 파일 업로드 로직 개선
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

# 서버 모니터링 및 경고 알림
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

### ✅ 기타 고려 사항

- 조회 쿼리의 실행 계획 확인, 적절한 인덱스를 통한 조회 성능 개선
- Fetch join을 통한 JPA의 N+1 문제 해결
- @Valid 어노테이션을 통한 @RequestBody 유효성 검증
- 전역 예외 핸들러와 커스텀 에러 코드 및 예외를 활용하여 일관된 에러 응답을 반환
