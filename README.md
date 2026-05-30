# Auction Platform Backend

Backend pentru o platforma de licitatii construit cu Spring Boot, PostgreSQL si RabbitMQ.

## Module

- `auction-api`: API REST, logica de business, persistenta, outbox, WebSocket.
- `auction-shared`: contracte comune pentru evenimente.
- `auction-worker`: consumator RabbitMQ pentru audit si procesari asincrone.

## Rolul fiecarui modul

### `auction-api`

Responsabilitati principale:

- expune endpoint-urile REST pentru licitatii si bids
- aplica regulile de business pentru creare, start, bid si close
- persista starea in PostgreSQL
- scrie evenimente de domeniu in `outbox_events`
- publica evenimentele din outbox in RabbitMQ
- trimite evenimente live prin WebSocket
- inchide automat licitatiile expirate

### `auction-shared`

Contine contractele comune folosite intre procese:

- `AuctionEventType`
- `AuctionEventEnvelope`
- `BidPlacedEvent`
- `AuctionExtendedEvent`
- `AuctionClosedEvent`

Scopul lui este sa elimine dublarea contractelor de integrare intre API si worker.

### `auction-worker`

Responsabilitati principale:

- consuma evenimentele publicate in RabbitMQ
- deserializeaza payload-ul pe baza `eventType`
- valideaza indirect contractul dintre publisher si consumer
- persista un audit tehnic in tabela `audit_events`
- reprezinta locul in care pot fi adaugate ulterior notificari, analytics sau alte procese asincrone

## Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- RabbitMQ
- Spring WebSocket

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
- `auction/scheduler`: job-uri periodice pentru business
- `auction/repository`: acces JPA la baza de date
- `auction/entity`: entitati persistente
- `auction/dto`: request/response DTO
- `auction/event`: event payload-uri specifice API/WebSocket
- `common/exception`: exception handling global
- `common/outbox`: outbox pattern

Contractele de evenimente partajate intre API si worker sunt in:

- `auction-shared/.../event`

In worker, zonele importante sunt:

- `messaging`: RabbitMQ config si consumer
- `audit`: persistenta auditului de procesare

## Ce face aplicatia acum

Momentan backend-ul suporta:

- creare licitatie
- listare licitatii
- citire licitatie dupa id
- pornire licitatie
- inchidere licitatie
- inchidere automata a licitatiilor expirate
- plasare bid
- anti-sniping cu extensie automata a duratei licitatiei
- listare bids pentru o licitatie
- salvare evenimente de domeniu in `outbox_events`
- retry pentru publish outbox
- publicare evenimente in RabbitMQ
- broadcast WebSocket pentru evenimente live
- audit al evenimentelor procesate in `audit_events`

## Componente cheie

### 1. REST API

API-ul gestioneaza ciclul de viata al licitatiei:

- creare
- pornire
- plasare bid
- inchidere manuala
- listare stare si istoric bids

### 2. Scheduler de business

Exista doua job-uri periodice:

- `OutboxPublisher`: publica evenimentele `NEW` in RabbitMQ
- `AuctionScheduler`: inchide automat licitatiile expirate

### 3. Outbox

Outbox-ul separa tranzactia de business de integrarea cu RabbitMQ.

Aplicatia nu publica direct in broker in tranzactia principala. In schimb:

- scrie in DB starea licitatiei
- scrie evenimentul in `outbox_events`
- lasa publisher-ul asincron sa il trimita ulterior

### 4. WebSocket broadcaster

Evenimentele importante pentru UI sunt trimise imediat si pe canal live:

- `BID_PLACED`
- `AUCTION_EXTENDED`
- `AUCTION_CLOSED`

### 5. Worker audit

Worker-ul nu se opreste la log-uri. Pentru fiecare eveniment consumat:

- citeste `eventType`
- deserializeaza payload-ul in contractul corect
- extrage `aggregateId` din payload
- persista payload-ul original in `audit_events`

Audit-ul are rolul de:

- trasabilitate tehnica
- debugging
- baza pentru analytics sau notificari ulterioare
- dovada ca worker-ul a procesat evenimentul

## Modelul de date

Tabele principale:

- `users`
- `auctions`
- `bids`
- `outbox_events`
- `audit_events`

### `auctions`

Retine starea curenta a licitatiei:

- `status`: `DRAFT`, `RUNNING`, `ENDED`
- `current_price`
- `end_time`
- `anti_sniping_window_sec`
- `anti_sniping_extend_sec`
- `version`

### `bids`

Retine istoricul imutabil al bid-urilor:

- ce licitatie a primit bid-ul
- cine a licitat
- ce suma a fost oferita
- cand s-a plasat bid-ul

### `outbox_events`

Retine evenimentele de integrare generate de `auction-api`.

Campuri importante:

- `event_type`
- `aggregate_id`
- `payload`
- `status`
- `retry_count`
- `last_error`
- `published_at`

### `audit_events`

Retine auditul de procesare din `auction-worker`.

Campuri importante:

- `event_type`
- `aggregate_id`
- `payload`
- `processed_at`
- `source`

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
- initializeaza configuratia anti-sniping
- salveaza licitatia in DB

### 2. Start auction

Clientul apeleaza:

- `POST /api/auctions/{id}/start`

Aplicatia:

- incarca licitatia
- verifica sa fie `DRAFT`
- verifica `endTime` in viitor
- seteaza statusul `RUNNING`
- seteaza `startTime`
- actualizeaza `updatedAt`

### 3. Place bid

Clientul apeleaza:

- `POST /api/auctions/{id}/bids`

Aplicatia:

- incarca licitatia
- verifica sa fie `RUNNING`
- verifica sa nu fie expirata
- verifica suma minima acceptata: `currentPrice + minIncrement`
- blocheaza licitatia cu `PESSIMISTIC_WRITE`
- actualizeaza `currentPrice`
- daca bid-ul intra in fereastra de anti-sniping, extinde `endTime`
- salveaza bid-ul in `bids`
- salveaza evenimentul `BID_PLACED` in `outbox_events`
- daca licitatia a fost extinsa, salveaza si `AUCTION_EXTENDED`
- trimite evenimentele live si pe WebSocket

Response-ul de bid expune si:

- `auctionExtended`
- `newEndTime`

### 4. Close auction

Clientul apeleaza:

- `POST /api/auctions/{id}/close`

Aplicatia:

- incarca licitatia cu `PESSIMISTIC_WRITE`
- verifica sa fie `RUNNING`
- seteaza statusul `ENDED`
- salveaza evenimentul `AUCTION_CLOSED` in `outbox_events`
- trimite `AUCTION_CLOSED` pe WebSocket

### 5. Auto-close auction

Periodic, scheduler-ul cauta licitatiile `RUNNING` cu `endTime <= now`.

Aplicatia:

- selecteaza ID-urile licitatiilor expirate
- reincarca fiecare licitatie cu `PESSIMISTIC_WRITE`
- o marcheaza `ENDED`
- salveaza evenimentul `AUCTION_CLOSED` in `outbox_events`
- trimite evenimentul `AUCTION_CLOSED` si pe WebSocket

## Reguli de business

### Reguli de stare

- o licitatie nou creata intra in `DRAFT`
- doar o licitatie `DRAFT` poate fi pornita
- doar o licitatie `RUNNING` accepta bids
- doar o licitatie `RUNNING` poate fi inchisa
- o licitatie expirata este inchisa automat de scheduler

### Reguli de timp

- `endTime` trebuie sa fie in viitor cand licitatia este pornita
- un bid este acceptat doar daca `endTime > now`
- daca un bid intra in fereastra finala configurata, licitatia se extinde

Formula anti-sniping:

- daca `now >= endTime - antiSnipingWindowSec`, atunci:
- `endTime = endTime + antiSnipingExtendSec`

### Reguli de pret

- `currentPrice` porneste din `startPrice`
- un bid nou trebuie sa fie cel putin `currentPrice + minIncrement`
- dupa acceptare, `currentPrice` devine suma bid-ului

### Reguli de integrare

- fiecare eveniment de business este scris in outbox in aceeasi tranzactie
- publicarea in RabbitMQ este asincrona
- worker-ul auditeaza doar evenimentele consumate cu succes

## Locking si concurenta

Aplicatia foloseste doua mecanisme diferite:

### Optimistic locking

`AuctionEntity` pastreaza si un camp:

```java
@Version
private Long version;
```

Acesta ofera protectie suplimentara la update-uri concurente si ramane parte din modelul persistent.

### Pessimistic locking

Se aplica la inchiderea licitatiei si la plasarea bid-urilor prin:

- `findByIdForUpdate(...)`
- `@Lock(LockModeType.PESSIMISTIC_WRITE)`

Ideea:

- blocam randul licitatiei pe durata tranzactiei
- evitam bid-uri concurente incoerente
- evitam inchideri concurente pe aceeasi licitatie

In practica, fluxurile critice actuale se bazeaza in principal pe `findByIdForUpdate(...)`.

## Outbox Pattern

### Problema pe care o rezolva

Nu vrem sa publicam direct in RabbitMQ in mijlocul tranzactiei de business, pentru ca pot aparea inconsistente:

- DB commit reuseste, dar publish in broker esueaza
- sau mesajul pleaca, dar tranzactia cade

### Solutia

In aceeasi tranzactie cu logica de business, salvam un rand in `outbox_events`.

Exemple:

- la bid salvam `BID_PLACED`
- la extensie salvam `AUCTION_EXTENDED`
- la close salvam `AUCTION_CLOSED`

Campuri importante:

- `aggregate_type`
- `aggregate_id`
- `event_type`
- `payload`
- `status`

Statusul initial este:

- `NEW`

Campuri suplimentare pentru retry:

- `retry_count`
- `last_error`
- `created_at`
- `published_at`

### Publisher-ul outbox

Aplicatia are acum si un publisher programat care ruleaza periodic.

Ce face:

1. citeste evenimentele `NEW` din `outbox_events`
2. construieste un `AuctionEventEnvelope(eventType, payload)`
3. publica envelope-ul in RabbitMQ
4. marcheaza evenimentele `PUBLISHED`
5. daca publish-ul esueaza, creste `retry_count`
6. dupa 3 incercari, marcheaza evenimentul `FAILED`

Statusuri posibile:

- `NEW`
- `PUBLISHED`
- `FAILED`

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

Mesajul publicat in broker are forma:

```json
{
  "eventType": "BID_PLACED",
  "payload": "{...json payload...}"
}
```

### Worker-ul RabbitMQ

Modulul `auction-worker` consuma mesajele publicate de `auction-api`.

Fluxul este:

1. `auction-api` scrie evenimentul in `outbox_events`
2. `OutboxPublisher` publica mesajul in RabbitMQ
3. `auction-worker` consuma mesajul din `auction.events.queue`
4. worker-ul deserializeaza payload-ul in functie de `eventType`
5. worker-ul salveaza un audit in `audit_events`

Campurile de audit salvate de worker:

- `event_type`
- `aggregate_id`
- `payload`
- `processed_at`
- `source = AUCTION_WORKER`

Audit-ul nu modifica starea licitatiilor. El este strict o persistenta a procesarii mesajelor.

## Event types actuale

In acest moment exista:

- `BID_PLACED`
- `AUCTION_EXTENDED`
- `AUCTION_CLOSED`

## Endpoint-uri actuale

- `GET /api/auctions`
- `GET /api/auctions/{id}`
- `POST /api/auctions`
- `POST /api/auctions/{id}/start`
- `POST /api/auctions/{id}/close`
- `POST /api/auctions/{id}/bids`
- `GET /api/auctions/{id}/bids`

## Exemple de scenarii reale

### Scenariul 1: bid normal

1. licitatia este `RUNNING`
2. clientul trimite un bid valid
3. `currentPrice` este actualizat
4. se salveaza `BID_PLACED` in outbox
5. evenimentul merge in WebSocket
6. `OutboxPublisher` il trimite in RabbitMQ
7. `auction-worker` il salveaza in `audit_events`

### Scenariul 2: bid in fereastra anti-sniping

1. licitatia este aproape de expirare
2. clientul trimite un bid valid
3. sistemul extinde `endTime`
4. se salveaza `BID_PLACED`
5. se salveaza `AUCTION_EXTENDED`
6. ambele pot fi publicate si auditate separat

### Scenariul 3: licitatie expirata fara close manual

1. licitatia este `RUNNING`
2. `endTime` trece
3. `AuctionScheduler` detecteaza licitatia expirata
4. sistemul o marcheaza `ENDED`
5. se salveaza `AUCTION_CLOSED`
6. evenimentul este publicat in RabbitMQ
7. worker-ul il persista in `audit_events`

## Date de test utile

Exemple de useri locali folositi in testare:

- creator: `id = 1`
- bidder: `id = 2`

## Ce urmeaza tehnic

Directia fireasca din punctul actual:

1. teste de integrare end-to-end pentru API + RabbitMQ + worker
2. dead-letter / retry strategy pe consumer side
3. notificari reale sau analytics peste `audit_events`
4. securizare si autentificare pentru endpoint-uri
