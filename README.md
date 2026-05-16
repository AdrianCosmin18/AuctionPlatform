X# Auction Platform Backend

Backend pentru o platforma de licitatii construit cu Spring Boot, PostgreSQL si RabbitMQ.

## Module

- `auction-api`: API-ul principal REST, logica de business, persistenta, outbox.
- `auction-shared`: cod comun intre servicii.
- `auction-worker`: consumator RabbitMQ pentru procesari asincrone.

## Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- RabbitMQ

## Cum porneste local

1. Porneste infrastructura:

```powershell
docker compose up -d
```

2. Porneste API-ul:

```powershell
mvn -pl auction-api spring-boot:run
```

3. Porneste worker-ul:

```powershell
mvn -pl auction-worker spring-boot:run
```

4. API-ul ruleaza implicit la:

- `http://localhost:8080`

5. Worker-ul ruleaza implicit la:

- `http://localhost:8081`

6. RabbitMQ Management UI:

- `http://localhost:15672`
- user: `auctions`
- parola: `auctions`

## Structura aplicatiei

Codul principal este in `auction-api/src/main/java/org/nedelcu/cosmin/auction/api`.

Zonele importante:

- `auction/controller`: endpoint-uri REST
- `auction/service`: logica de business
- `auction/repository`: acces JPA la baza de date
- `auction/entity`: entitati persistente
- `auction/dto`: request/response DTO
- `auction/event`: event payload-uri de domeniu
- `common/exception`: exception handling global
- `common/outbox`: outbox pattern

## Ce face aplicatia acum

Momentan backend-ul suporta:

- creare licitatie
- listare licitatii
- citire licitatie dupa id
- pornire licitatie
- inchidere licitatie
- plasare bid
- listare bids pentru o licitatie
- salvare evenimente de domeniu in `outbox_events`

## Modelul de date

Tabele principale:

- `users`
- `auctions`
- `bids`
- `outbox_events`

Cheile primare folosesc `bigint` cu secvente Postgres:

- `users_seq`
- `auctions_seq`
- `bids_seq`
- `outbox_events_seq`

## Fluxul unei licitatii

### 1. Create auction

Clientul apeleaza:

- `POST /api/auctions`

Aplicatia:

- valideaza request-ul
- creeaza un `AuctionEntity`
- seteaza statusul initial `DRAFT`
- salveaza licitatia in DB

### 2. Start auction

Clientul apeleaza:

- `POST /api/auctions/{id}/start`

Aplicatia:

- incarca licitatia
- verifica sa fie `DRAFT`
- verifica `endTime` in viitor
- seteaza statusul `RUNNING`

### 3. Place bid

Clientul apeleaza:

- `POST /api/auctions/{id}/bids`

Aplicatia:

- incarca licitatia
- verifica sa fie `RUNNING`
- verifica sa nu fie expirata
- verifica suma minima acceptata: `currentPrice + minIncrement`
- actualizeaza `currentPrice`
- salveaza bid-ul in `bids`
- salveaza evenimentul `BID_PLACED` in `outbox_events`

### 4. Close auction

Clientul apeleaza:

- `POST /api/auctions/{id}/close`

Aplicatia:

- incarca licitatia cu `PESSIMISTIC_WRITE`
- verifica sa fie `RUNNING`
- seteaza statusul `ENDED`
- salveaza evenimentul `AUCTION_CLOSED` in `outbox_events`

## Locking si concurenta

Aplicatia foloseste doua mecanisme diferite:

### Optimistic locking

Se aplica pe `AuctionEntity` prin:

```java
@Version
private Long version;
```

Este folosit in special pentru bidding.

Ideea:

- doua request-uri pot citi aceeasi licitatie
- doar unul poate salva cu versiunea veche
- celalalt primeste conflict

Rezultatul pentru client:

- `409 Conflict` la update concurent

### Pessimistic locking

Se aplica la inchiderea licitatiei prin:

- `findByIdForUpdate(...)`
- `@Lock(LockModeType.PESSIMISTIC_WRITE)`

Ideea:

- blocam randul licitatiei pe durata tranzactiei de close
- evitam inchideri concurente pe aceeasi licitatie

## Outbox Pattern

### Problema pe care o rezolva

Nu vrem sa publicam direct in RabbitMQ in mijlocul tranzactiei de business, pentru ca pot aparea inconsistente:

- DB commit reuseste, dar publish in broker esueaza
- sau mesajul pleaca, dar tranzactia cade

### Solutia

In aceeasi tranzactie cu logica de business, salvam un rand in `outbox_events`.

Exemple:

- la bid salvam `BID_PLACED`
- la close salvam `AUCTION_CLOSED`

Campuri importante:

- `aggregate_type`
- `aggregate_id`
- `event_type`
- `payload`
- `status`

Statusul initial este:

- `NEW`

### Publisher-ul outbox

Aplicatia are acum si un publisher programat care ruleaza periodic.

Ce face:

1. citeste evenimentele `NEW` din `outbox_events`
2. publica `payload` in RabbitMQ
3. marcheaza evenimentele `PUBLISHED`
4. daca publish-ul esueaza, marcheaza evenimentul `FAILED`

Practic:

- tranzactia de business scrie in DB
- publisher-ul asincron impinge mesajele in broker
- worker-ele sau websocket broadcaster-ele pot consuma ulterior evenimentele

### RabbitMQ

Configuratia actuala foloseste:

- exchange: `auction.events.exchange`
- queue: `auction.events.queue`
- routing key: `auction.events`

Publisher-ul este implementat ca job cu `@Scheduled(fixedDelay = 2000)`.

### Worker-ul RabbitMQ

Modulul `auction-worker` consuma mesajele publicate de `auction-api`.

Fluxul este:

1. `auction-api` scrie evenimentul in `outbox_events`
2. `OutboxPublisher` publica mesajul in RabbitMQ
3. `auction-worker` consuma mesajul din `auction.events.queue`
4. worker-ul poate fi extins ulterior pentru:
   - notificari
   - audit
   - websocket bridge
   - analytics

Momentan worker-ul doar scrie mesajul in log:

- `Received auction event: {...}`

## Event types actuale

In acest moment exista:

- `BID_PLACED`
- `AUCTION_CLOSED`

Exista si `AUCTION_EXTENDED` pregatit in cod, dar nu este folosit inca, pentru ca logica de anti-sniping nu este implementata momentan.

## Endpoint-uri actuale

- `GET /api/auctions`
- `GET /api/auctions/{id}`
- `POST /api/auctions`
- `POST /api/auctions/{id}/start`
- `POST /api/auctions/{id}/close`
- `POST /api/auctions/{id}/bids`
- `GET /api/auctions/{id}/bids`

## Date de test utile

Exemple de useri locali folositi in testare:

- creator: `id = 1`
- bidder: `id = 2`

## Ce urmeaza tehnic

Directia fireasca din punctul actual:

1. consumatori sau worker pentru procesare evenimente
2. websocket broadcast pentru UI live
3. anti-sniping logic
4. teste de integrare pentru concurenta si outbox
