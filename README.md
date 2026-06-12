# Auction Platform

Platforma de licitatii full-stack construita cu Spring Boot, PostgreSQL, RabbitMQ, WebSocket/STOMP si Angular 19.

## Workflow de lucru

Fisierul principal pentru backlog si urmatorii pasi este:

- `ROADMAP.md`

Regula de lucru este:

- luam contextul pentru urmatorul feature din `ROADMAP.md`
- implementam feature-ul
- apoi actualizam atat `ROADMAP.md`, cat si acest `README`

## Module

- `auction-api`: API REST, logica de business, persistenta, outbox, WebSocket.
- `auction-shared`: contracte comune pentru evenimente.
- `auction-worker`: consumator RabbitMQ pentru audit si procesari asincrone.
- `auction-ui`: frontend Angular pentru listare, creare, administrare si monitorizare live a licitatiilor.
  Include acum si dashboard operational.

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
- aplica autentificare JWT si authorization pe roluri `USER` / `ADMIN`

### `auction-shared`

Contine contractele comune folosite intre procese:

- `AuctionEventType`
- `AuctionEventEnvelope`
- `BidPlacedEvent`
- `AuctionExtendedEvent`
- `AuctionClosedEvent`
- `NotificationType`

Scopul lui este sa elimine dublarea contractelor de integrare intre API si worker.

### `auction-worker`

Responsabilitati principale:

- consuma evenimentele publicate in RabbitMQ
- deserializeaza payload-ul pe baza `eventType`
- valideaza indirect contractul dintre publisher si consumer
- persista un audit tehnic in tabela `audit_events`
- genereaza notificari in-app in tabela `notifications`
- livreaza email notifications pentru evenimentele eligibile
- reprezinta locul in care pot fi adaugate ulterior notificari, analytics sau alte procese asincrone

### `auction-ui`

Responsabilitati principale:

- afiseaza lista licitatiilor si overview operational
- permite crearea unei licitatii noi din browser
- permite editarea unei licitatii `DRAFT` din browser
- afiseaza pagina de detalii pentru o licitatie
- permite adaugarea unei licitatii in watchlist si gestionarea `My Watchlist`
- ofera zona `My Area` cu pagini separate pentru `My Auctions`, `My Bids` si `My Watchlist`
- ofera pagina `My Profile` pentru date de contact si schimbare parola
- suporta `reserve price` optional si expune starea `reserve met / not met`
- suporta `buy now price` optional si actiune de cumparare imediata din pagina de detalii
- afiseaza dashboard operational cu KPI-uri si breakdown-uri pe categorii
- afiseaza pagina `Fraud Signals` cu pattern-uri suspecte detectate si actiune administrativa de suspendare
- afiseaza notificari in-app si unread badge
- permite start si close din UI pentru fluxul de administrare
- permite plasarea de bid-uri
- consuma evenimente live prin WebSocket/STOMP
- actualizeaza local starea licitatiei pe baza snapshot REST + evenimente live
- gestioneaza galerie de imagini pentru fiecare licitatie prin upload local din browser

## Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- RabbitMQ
- Spring WebSocket
- Angular 19
- PrimeNG 19
- Bootstrap 5
- RxJS
- STOMP.js + SockJS
- Spring Security + JWT

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

Pentru livrare reala de email poti porni worker-ul cu SMTP extern prin variabile de mediu. Exemplu generic:

```powershell
$env:MAIL_HOST="smtp.example.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="your-account@example.com"
$env:MAIL_PASSWORD="your-app-password"
$env:MAIL_SMTP_AUTH="true"
$env:MAIL_SMTP_STARTTLS_ENABLE="true"
$env:MAIL_SMTP_STARTTLS_REQUIRED="true"
$env:MAIL_SMTP_SSL_ENABLE="false"
$env:APP_MAIL_FROM="your-account@example.com"
mvn -pl auction-worker spring-boot:run
```

Pentru servere SMTPS pe port `465`, setezi de regula:

```powershell
$env:MAIL_PORT="465"
$env:MAIL_SMTP_STARTTLS_ENABLE="false"
$env:MAIL_SMTP_SSL_ENABLE="true"
```

Pentru validare imediata a configuratiei SMTP la startup, poti activa:

```powershell
$env:MAIL_TEST_CONNECTION="true"
$env:APP_MAIL_STARTUP_TEST_RECIPIENT="destinatar@example.com"
```

Optional, daca vrei ca worker-ul sa nu porneasca atunci cand SMTP-ul real nu este configurat corect:

```powershell
$env:APP_MAIL_STARTUP_TEST_REQUIRED="true"
```

Pentru troubleshooting detaliat de protocol SMTP:

```powershell
$env:MAIL_DEBUG="true"
```

Exemplu Gmail cu `App Password`:

```powershell
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="your-account@gmail.com"
$env:MAIL_PASSWORD="your-16-digit-app-password"
$env:MAIL_SMTP_AUTH="true"
$env:MAIL_SMTP_STARTTLS_ENABLE="true"
$env:MAIL_SMTP_STARTTLS_REQUIRED="true"
$env:MAIL_SMTP_SSL_ENABLE="false"
$env:MAIL_SMTP_SSL_TRUST="smtp.gmail.com"
$env:MAIL_TEST_CONNECTION="true"
$env:APP_MAIL_FROM="your-account@gmail.com"
$env:APP_MAIL_STARTUP_TEST_RECIPIENT="recipient@example.com"
mvn -pl auction-worker spring-boot:run
```

Exemplu Yahoo cu `App Password`:

```powershell
$env:MAIL_HOST="smtp.mail.yahoo.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="your-account@yahoo.com"
$env:MAIL_PASSWORD="your-app-password"
$env:MAIL_SMTP_AUTH="true"
$env:MAIL_SMTP_STARTTLS_ENABLE="true"
$env:MAIL_SMTP_STARTTLS_REQUIRED="true"
$env:MAIL_SMTP_SSL_ENABLE="false"
$env:MAIL_SMTP_SSL_TRUST="smtp.mail.yahoo.com"
$env:MAIL_TEST_CONNECTION="true"
$env:APP_MAIL_FROM="your-account@yahoo.com"
$env:APP_MAIL_STARTUP_TEST_RECIPIENT="recipient@example.com"
mvn -pl auction-worker spring-boot:run
```

4. Porneste UI-ul:

```powershell
cd auction-ui
npm install
npm start
```

5. API-ul ruleaza implicit la:

- `http://localhost:8080`

6. Worker-ul ruleaza implicit la:

- `http://localhost:8081`

7. UI-ul ruleaza implicit la:

- `http://localhost:4200`

8. RabbitMQ Management UI:

- `http://localhost:15672`
- user: `auctions`
- parola: `auctions`

9. MailHog UI:

- `http://localhost:8025`
- SMTP local: `localhost:1025`

Worker-ul foloseste implicit `MailHog` pentru dezvoltare locala. Daca setezi variabilele `MAIL_*`, aceeasi implementare poate trimite si printr-un provider SMTP real.

## Structura aplicatiei

Codul principal este in `auction-api/src/main/java/org/nedelcu/cosmin/auction/api`.

Zonele importante in `auction-api`:

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

In `auction-ui`, zonele importante sunt:

- `core/models`: contractele TypeScript aliniate cu backend-ul
- `core/services`: integrare REST si WebSocket
- `features/auction-list`: lista licitatiilor, filtre, search, start/close rapid
- `features/auction-create`: formular de creare licitatie
- `features/auction-form`: formular reutilizabil pentru create/edit
- `features/auction-edit`: editare licitatie `DRAFT`
- `features/auction-details`: snapshot REST + live updates + bid flow + countdown + bid history + live events
- `features/my-auctions`: pagina dedicata pentru loturile create de userul curent
- `features/my-bids`: pagina dedicata pentru activitatea de bid a userului curent
- `features/my-watchlist`: pagina dedicata pentru loturile urmarite
- `features/dashboard`: dashboard operational pentru marketplace
- `features/fraud-signals`: review operational pentru semnale suspecte si suspendare administrativa
- `app.routes.ts`: rutele UI

## Ce face aplicatia acum

Backend-ul suporta:

- register / login cu JWT
- pagina de profil pentru userul autentificat, cu update pentru datele de contact
- schimbare parola din contul autentificat
- roluri `USER` si `ADMIN`
- creare licitatie
- editare licitatie `DRAFT`
- listare licitatii
- citire licitatie dupa id
- watch / unwatch pentru licitatii
- listare `My Auctions`
- listare `My Bids`
- listare `My Watchlist`
- reserve price optional in create, edit, close summary si evenimente
- buy now price optional in create, edit, close summary si endpoint dedicat pentru cumparare imediata
- endpoint analytics pentru dashboard operational agregat
- endpoint fraud pentru semnale suspecte calculate on-demand (`bidder pair dominance` si `seller-bidder concentration`)
- endpoint admin pentru suspendarea unei licitatii `RUNNING` cu motiv obligatoriu
- listare notificari pentru userul curent
- unread notifications count
- `mark as read` si `mark all as read`
- email notifications pentru `AUCTION_WON`, `OUTBID`, `AUCTION_CLOSED` si `AUCTION_SUSPENDED`
- suport configurabil pentru SMTP local (`MailHog`) sau SMTP real prin variabile `MAIL_*`
- pornire licitatie
- inchidere licitatie
- inchidere automata a licitatiilor expirate
- plasare bid
- blocare consecutive self-bids atunci cand user-ul este deja highest bidder
- anti-sniping cu extensie automata a duratei licitatiei
- listare bids pentru o licitatie
- salvare evenimente de domeniu in `outbox_events`
- retry pentru publish outbox
- publicare evenimente in RabbitMQ
- broadcast WebSocket pentru evenimente live
- audit al evenimentelor procesate in `audit_events`
- generare asincrona de notificari in-app in `notifications`
- tracking simplu pentru livrarea email direct in tabela `notifications`
- stocare locala pentru imaginile uploadate si servire din `/media/**`
- rezolvarea user-ului curent din token JWT prin `CurrentUserService`

Frontend-ul suporta:

- pagini dedicate `Login` si `Register`
- JWT interceptor si route guards
- listare licitatii la `/auctions`
- filtrare licitatii dupa status: `ALL`, `RUNNING`, `DRAFT`, `ENDED`
- filtrare licitatii dupa categorie
- cautare dupa titlu sau descriere in lista de licitatii
- creare licitatie la `/auctions/new`
- editare licitatie `DRAFT` la `/auctions/:id/edit`
- detalii licitatie la `/auctions/:id`
- watch / unwatch din marketplace si pagina de detalii
- pagina dedicata `My Auctions` la `/my-auctions`
- pagina dedicata `My Bids` la `/my-bids`
- pagina dedicata `My Watchlist` la `/my-watchlist`
- pagina dedicata `Dashboard` la `/dashboard`
- pagina dedicata `Fraud Signals` la `/fraud-signals`
- pagina dedicata `Notifications` la `/notifications`
- afisare `Reserve met`, `Reserve pending` sau `Reserve not met` in paginile relevante
- afisare `Buy Now` in detalii si in listele relevante atunci cand lotul are `buyNowPrice`
- start auction din lista si din pagina de detalii
- close auction din lista si din pagina de detalii
- buy now din pagina de detalii pentru loturile `RUNNING` eligibile
- plasare bid din UI
- blocare bid form pentru owner si pentru userul care este deja in lead
- creare licitatie cu pana la 5 imagini locale prin upload `multipart/form-data`
- countdown reactiv pana la `endTime`
- actualizare live a statusului la `AUCTION_STARTED`
- extensie live a countdown-ului la `AUCTION_EXTENDED`
- actualizare live a pretului si istoricului la `BID_PLACED`
- dezactivare bid form la `AUCTION_CLOSED` sau la expirarea timpului
- toast notifications PrimeNG pentru evenimente live si actiuni cheie din `auction-details`
- afisare rezultat final pentru licitatiile inchise: winner, winning bid, final price, close reason, closed at
- imagine principala in lista si galerie in pagina de detalii
- panou `Bid history` cu cele mai recente bid-uri
- panou `Live events` cu evenimentele WebSocket receptionate in timp real
- taxonomie categorie/subcategorie in create, edit, listare si detalii
- `watchers count` in marketplace, details si watchlist
- control operational pentru `My Auctions`: `Edit`, `Start`, `Close`
- sumar pentru `My Bids`: latest bid, highest bid, total bids, leading/won state
- close outcome corect pentru reserve price: lot vandut doar daca `reservePrice` este atins
- suspendare administrativa pentru licitatii `RUNNING` prin `POST /api/admin/auctions/{id}/suspend`
- afisare `SUSPENDED` si motivul blocarii in paginile relevante
- unread badge in header pentru notificari
- toast global in coltul ecranului pentru notificari noi de tip `AUCTION_WON`
- `mark as read` si `mark all as read` pentru notificari
- afisare conditionata a zonelor admin in functie de rolul user-ului autentificat

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

- `AUCTION_STARTED`
- `BID_PLACED`
- `AUCTION_EXTENDED`
- `AUCTION_CLOSED`
- `AUCTION_SUSPENDED`

### 5. Worker audit

Worker-ul nu se opreste la log-uri. Pentru fiecare eveniment consumat:

- citeste `eventType`
- deserializeaza payload-ul in contractul corect
- extrage `aggregateId` din payload
- persista payload-ul original in `audit_events`
- genereaza notificari in-app relevante pentru userii afectati

Audit-ul are rolul de:

- trasabilitate tehnica
- debugging
- baza pentru analytics sau notificari ulterioare
- dovada ca worker-ul a procesat evenimentul

### 5b. In-app notifications

Notificarile in-app sunt generate in `auction-worker` pe baza evenimentelor deja publicate prin outbox.

Tipuri suportate acum:

- `OUTBID`
- `AUCTION_WON`
- `AUCTION_LOST`
- `AUCTION_CLOSED`
- `AUCTION_SUSPENDED`
- `AUCTION_EXTENDED`
- `NEW_BID_ON_OWN_AUCTION`

Tipurile care trimit email in MVP sunt:

- `AUCTION_WON`
- `OUTBID`
- `AUCTION_CLOSED`
- `AUCTION_SUSPENDED`

Fluxul actual este:

1. `auction-api` scrie evenimentul in `outbox_events`
2. `OutboxPublisher` il publica in RabbitMQ
3. `auction-worker` consuma evenimentul
4. worker-ul il auditeaza in `audit_events`
5. worker-ul creeaza una sau mai multe notificari in `notifications`
6. `auction-ui` citeste notificarile prin REST si afiseaza unread badge + lista completa
7. pentru notificari noi de tip `AUCTION_WON`, UI afiseaza si un toast global in dreapta-sus
8. pentru tipurile eligibile, worker-ul incearca si livrarea email prin SMTP-ul configurat

Schema simplificata a fluxului:

```text
[User in UI]
    |
    | HTTP request
    v
[auction-ui] ------------------------------.
    |                                      |
    | POST /api/auctions/{id}/bids         | WebSocket live event
    v                                      |
[auction-api]                              |
    |                                      |
    | 1. valideaza business rule           |
    | 2. salveaza bid / close / extend     |
    | 3. scrie event in outbox_events      |
    | 4. trimite live event spre UI        |
    v                                      |
[PostgreSQL]                               |
    |                                      |
    | outbox publisher citeste NEW         |
    v                                      |
[RabbitMQ] --------------------------------'
    |
    | message: BID_PLACED / AUCTION_EXTENDED / AUCTION_CLOSED
    v
[auction-worker]
    |
    | 1. consuma mesajul
    | 2. salveaza audit in audit_events
    | 3. genereaza notifications in DB
    | 4. trimite email daca notificarea e eligibila
    v
[notifications table]
    |
    | REST polling
    v
[auction-ui notifications]
    |
    | badge / lista / toast global AUCTION_WON
    v
[User sees notification]

[MailHog / SMTP]
    ^
    |
    '---- email din worker pentru tipurile eligibile
```

### 6. Frontend reactiv

Pagina de detalii foloseste un model hibrid:

- snapshot initial prin REST (`getAuction`, `getBids`)
- stream live prin WebSocket (`watchAuction`)
- state local reactiv cu RxJS (`BehaviorSubject`, `combineLatest`, `timer`)

Scopul este:

- UI-ul sa porneasca dintr-o stare consistenta
- apoi sa reflecte live schimbarile de pret, timp si status fara refresh manual

Lista de licitatii completeaza partea live cu un control operational local:

- filtre rapide pe status
- cautare dupa titlu sau descriere
- actiuni rapide de start si close
- acces direct la licitatia relevanta

## Modelul de date

Tabele principale:

- `users`
- `auctions`
- `auction_images`
- `auction_watchlist`
- `notifications`
- `bids`
- `outbox_events`
- `audit_events`

### `auctions`

Retine starea curenta a licitatiei:

- `status`: `DRAFT`, `RUNNING`, `ENDED`, `SUSPENDED`
- `current_price`
- `end_time`
- `winner_id`
- `winning_bid_id`
- `final_price`
- `closed_at`
- `closed_reason`
- `suspended_at`
- `suspended_by`
- `suspension_reason`
- `reserve_price`
- `reserve_met`
- `anti_sniping_window_sec`
- `anti_sniping_extend_sec`
- `version`

### `bids`

Retine istoricul imutabil al bid-urilor:

- ce licitatie a primit bid-ul
- cine a licitat
- ce suma a fost oferita
- cand s-a plasat bid-ul

### `auction_images`

Retine imaginile asociate unei licitatii:

- `auction_id`
- `image_url`
- `display_order`

`image_url` retine calea media generata de backend dupa upload local. Prima imagine din `display_order` este folosita ca imagine principala in listare, iar toate imaginile sunt afisate in galerie in pagina de detalii.

Persistenta media locala foloseste:

- `auction-api/src/main/resources/static/demo-images/` pentru imaginile demo seed-uite
- `uploads/` pentru imaginile uploadate in runtime
- `scripts/generate_thematic_demo_assets.mjs` pentru generarea asset-urilor demo

### `auction_watchlist`

Retine loturile urmarite de fiecare utilizator:

- `user_id`
- `auction_id`
- `created_at`

Pentru MVP:

- user-ul curent este rezolvat din token-ul JWT
- watchlist-ul este accesibil doar pentru utilizatorul autentificat

### `notifications`

Retine notificarile in-app pentru fiecare utilizator:

- `user_id`
- `auction_id`
- `type`
- `title`
- `message`
- `is_read`
- `created_at`
- `read_at`
- `email_delivery_status`
- `email_sent_at`
- `email_last_attempt_at`
- `email_last_error`

Pentru MVP:

- notificarile sunt generate asincron in `auction-worker`
- API-ul doar expune listarea si actiunile de read state
- user-ul curent este rezolvat din token-ul JWT
- worker-ul trimite email doar pentru tipurile importante, iar statusul livrarii ramane pe notificare

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

Clientul sau UI-ul apeleaza:

- `POST /api/auctions`

Aplicatia:

- valideaza request-ul
- creeaza un `AuctionEntity`
- seteaza statusul initial `DRAFT`
- initializeaza configuratia anti-sniping
- salveaza licitatia in DB

In frontend:

- utilizatorul completeaza formularul de la `/auctions/new`
- optional selecteaza pana la 5 imagini locale prin `p-fileupload`
- UI-ul trimite `POST /api/auctions` ca `multipart/form-data`
- dupa creare, UI-ul redirectioneaza la `/auctions/{id}`

### 1b. Edit draft auction

Clientul sau UI-ul apeleaza:

- `PUT /api/auctions/{id}`

Aplicatia:

- incarca licitatia cu lock
- verifica sa fie `DRAFT`
- actualizeaza metadatele si configuratia licitatiei
- pastreaza imaginile existente
- adauga la final imaginile noi uploadate

In frontend:

- utilizatorul intra pe `/auctions/{id}/edit`
- formularul este prepopulat cu datele licitatiei
- editarea este permisa doar pentru `DRAFT`
- dupa salvare, UI-ul redirectioneaza la `/auctions/{id}`

### 1c. Browse and filter auctions

Utilizatorul intra pe:

- `GET /auctions`

UI-ul:

- incarca lista completa de licitatii
- filtreaza local dupa `ALL`, `RUNNING`, `DRAFT`, `ENDED`
- cauta dupa `title` si `description`
- foloseste prima imagine din galerie ca thumbnail in lista si in sectiunea `Focus now`
- permite actiuni rapide de `Start`, `Close` si `Details`

### 1d. Watch / Unwatch auction

Clientul sau UI-ul apeleaza:

- `POST /api/auctions/{id}/watch`
- `DELETE /api/auctions/{id}/watch`
- `GET /api/auctions/me/watchlist`

Aplicatia:

- adauga sau elimina licitatia din `auction_watchlist`
- calculeaza `watchersCount`
- expune `watchedByCurrentUser` in raspunsurile pentru licitatii
- blocheaza watchlist-ul pentru loturile create de user-ul curent

In frontend:

- utilizatorul poate urmari sau opri urmarirea din marketplace si din pagina de detalii
- exista pagina `My Watchlist`
- UI afiseaza numarul de watchers pentru fiecare lot relevant
- butonul `Watch` nu este afisat pentru propriile loturi

### 1e. Read notifications

Clientul sau UI-ul apeleaza:

- `GET /api/me/notifications`
- `GET /api/me/notifications/unread-count`
- `POST /api/me/notifications/{id}/read`
- `POST /api/me/notifications/read-all`

Aplicatia:

- citeste notificarile user-ului curent in ordine descrescatoare dupa `createdAt`
- returneaza unread count pentru badge-ul din header
- permite marcarea individuala sau bulk ca citite

In frontend:

- unread count este polled periodic pentru header badge
- utilizatorul poate intra in `/notifications`
- fiecare notificare poate fi marcata individual ca citita
- exista si actiunea `Mark all as read`
- daca notificarea are `auctionId`, UI ofera link direct catre pagina licitatiei
- pentru notificari noi de tip `AUCTION_WON`, UI afiseaza automat un toast global, o singura data per notificare

### 1f. Deliver email notification

Cand worker-ul creeaza o notificare eligibila pentru email:

- cauta email-ul user-ului in tabela `users`
- construieste un email simplu cu `title` + `message`
- trimite prin SMTP-ul configurat
- salveaza pe notificare unul dintre statusurile:
  - `PENDING`
  - `SENT`
  - `SKIPPED`
  - `FAILED`

Implicit, acest flux este configurat pentru SMTP local, deci email-urile apar in `MailHog`. Daca setezi variabilele `MAIL_*`, aceleasi notificari pot merge catre inbox-uri reale.

### 2. Start auction

Clientul sau UI-ul apeleaza:

- `POST /api/auctions/{id}/start`

Aplicatia:

- incarca licitatia
- verifica sa fie `DRAFT`
- verifica `endTime` in viitor
- seteaza statusul `RUNNING`
- seteaza `startTime`
- actualizeaza `updatedAt`

In frontend:

- butonul `Start` este disponibil pentru licitatiile `DRAFT`
- dupa raspuns, UI-ul actualizeaza local starea licitatiei

### 3. Place bid

Clientul sau UI-ul apeleaza:

- `POST /api/auctions/{id}/bids`

Aplicatia:

- incarca licitatia
- verifica sa fie `RUNNING`
- verifica sa nu fie expirata
- verifica sa nu fie lotul creatorului curent
- verifica sa nu fie deja highest bidder pe lot
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

In frontend:

- bid form calculeaza `nextMinimumBid = currentPrice + minIncrement`
- formularul este activ doar daca licitatia este `RUNNING` si countdown-ul nu a expirat
- formularul este dezactivat pentru owner si pentru userul care conduce deja licitatia
- UI-ul valideaza local suma minima
- la succes, formularul asteapta confirmarea live
- la `AUCTION_STARTED`, UI-ul actualizeaza statusul local fara refresh
- la `BID_PLACED`, UI-ul actualizeaza `currentPrice` si istoricul local
- la `AUCTION_EXTENDED`, UI-ul actualizeaza `endTime` si countdown-ul
- la `AUCTION_STARTED`, `BID_PLACED`, `AUCTION_EXTENDED`, `AUCTION_CLOSED` si `AUCTION_SUSPENDED`, UI-ul afiseaza toast-uri contextuale
- la actiunile manuale `Start` si `Close`, UI-ul afiseaza toast-uri dedicate pentru confirmare rapida
- panoul `Bid history` afiseaza bid-urile cu cel mai nou element sus
- panoul `Live events` afiseaza cronologic invers evenimentele WebSocket receptionate

### 4. Close auction

Clientul sau UI-ul apeleaza:

- `POST /api/auctions/{id}/close`

Aplicatia:

- incarca licitatia cu `PESSIMISTIC_WRITE`
- verifica sa fie `RUNNING`
- determina bid-ul castigator, daca exista
- verifica daca `reservePrice` a fost atins, daca este configurat
- seteaza `winnerId`, `winningBidId`, `finalPrice`, `closedAt`, `closedReason = MANUAL`
- daca `reservePrice` nu este atins, licitatia se inchide fara winner si cu `reserveMet = false`
- seteaza statusul `ENDED`
- salveaza evenimentul `AUCTION_CLOSED` in `outbox_events`
- trimite `AUCTION_CLOSED` pe WebSocket

In frontend:

- butonul `Close` este disponibil pentru licitatiile `RUNNING`
- dupa inchidere, bid form devine dezactivat
- pagina de detalii afiseaza rezultatul final al licitatiei, inclusiv castigatorul daca exista

### 4b. Buy now

Clientul sau UI-ul apeleaza:

- `POST /api/auctions/{id}/buy-now`

Aplicatia:

- incarca licitatia cu `PESSIMISTIC_WRITE`
- verifica sa fie `RUNNING`
- verifica existenta unui `buyNowPrice`
- blocheaza seller-ul sa isi cumpere propria licitatie
- respinge cererea daca `currentPrice >= buyNowPrice`
- seteaza `currentPrice = buyNowPrice`
- seteaza `winnerId` la userul curent
- seteaza `winningBidId = null`
- seteaza `finalPrice = buyNowPrice`
- seteaza `closedAt`, `closedReason = BUY_NOW`, `status = ENDED`
- recomputa `reserveMet`
- salveaza evenimentul `AUCTION_CLOSED` in `outbox_events`
- trimite `AUCTION_CLOSED` pe WebSocket

In frontend:

- formularul de create/edit accepta `buyNowPrice` optional
- pagina de detalii afiseaza pretul `Buy Now`
- butonul `Buy Now` este disponibil doar pentru loturi `RUNNING` unde `currentPrice < buyNowPrice`
- dupa succes, pagina se actualizeaza ca licitatie inchisa si bid form devine dezactivat

### 5. Auto-close auction

Periodic, scheduler-ul cauta licitatiile `RUNNING` cu `endTime <= now`.

Aplicatia:

- selecteaza ID-urile licitatiilor expirate
- reincarca fiecare licitatie cu `PESSIMISTIC_WRITE`
- determina bid-ul castigator, daca exista
- seteaza `winnerId`, `winningBidId`, `finalPrice`, `closedAt`, `closedReason = EXPIRED`
- o marcheaza `ENDED`
- salveaza evenimentul `AUCTION_CLOSED` in `outbox_events`
- trimite evenimentul `AUCTION_CLOSED` si pe WebSocket

In frontend:

- countdown-ul ajunge la zero
- formularul de bid este blocat imediat in UI
- dupa `AUCTION_CLOSED`, statusul devine `ENDED`
- UI-ul actualizeaza live si sumarul de inchidere

### 5b. Administrative suspension

Clientul sau UI-ul apeleaza:

- `POST /api/admin/auctions/{id}/suspend`

Aplicatia:

- incarca licitatia cu `PESSIMISTIC_WRITE`
- verifica sa fie `RUNNING`
- valideaza `reason` ca text obligatoriu
- seteaza `status = SUSPENDED`
- curata `winnerId`, `winningBidId`, `finalPrice`, `closedAt` si `closedReason`
- seteaza `suspendedAt`, `suspendedBy` si `suspensionReason`
- salveaza evenimentul `AUCTION_SUSPENDED` in `outbox_events`
- trimite `AUCTION_SUSPENDED` pe WebSocket

In frontend:

- pagina `Fraud Signals` poate suspenda direct o licitatie asociata unui semnal
- adminul trebuie sa introduca un motiv in dialogul PrimeNG
- pagina de detalii, listele relevante si notificarile afiseaza statusul `SUSPENDED` si motivul
- o licitatie suspendata nu mai permite `bid`, `buy now`, `close` sau `start`

## Reguli de business

### Reguli de stare

- o licitatie nou creata intra in `DRAFT`
- doar o licitatie `DRAFT` poate fi pornita
- doar o licitatie `RUNNING` accepta bids
- doar o licitatie `RUNNING` poate fi inchisa
- doar o licitatie `RUNNING` poate fi suspendata administrativ
- o licitatie expirata este inchisa automat de scheduler
- o licitatie `SUSPENDED` este terminala pentru MVP

### Reguli de timp

- `endTime` trebuie sa fie in viitor cand licitatia este pornita
- un bid este acceptat doar daca `endTime > now`
- daca un bid intra in fereastra finala configurata, licitatia se extinde
- frontend-ul reflecta extinderea in countdown fara refresh complet

Formula anti-sniping:

- daca `now >= endTime - antiSnipingWindowSec`, atunci:
- `endTime = endTime + antiSnipingExtendSec`

### Reguli de pret

- `currentPrice` porneste din `startPrice`
- un bid nou trebuie sa fie cel putin `currentPrice + minIncrement`
- dupa acceptare, `currentPrice` devine suma bid-ului
- daca exista `reservePrice`, lotul se vinde doar daca highest bid-ul final este `>= reservePrice`
- daca exista `buyNowPrice`, acesta trebuie sa fie `> startPrice`
- daca exista simultan `reservePrice`, atunci `buyNowPrice` trebuie sa fie `>= reservePrice`
- `Buy Now` foloseste un pret fix configurat inainte de start; nu se recalculeaza dinamic in functie de `currentPrice`
- `Buy Now` inchide imediat licitatia la pretul configurat si seteaza `closedReason = BUY_NOW`
- `reserveMet` devine:
  - `null` daca licitatia nu are reserve price
  - `true` daca pretul curent a atins reserve price
  - `false` daca licitatia se inchide fara sa atinga reserve price

### Reguli de integrare

- fiecare eveniment de business este scris in outbox in aceeasi tranzactie
- publicarea in RabbitMQ este asincrona
- worker-ul auditeaza doar evenimentele consumate cu succes
- `AUCTION_SUSPENDED` foloseste acelasi flux outbox -> RabbitMQ -> worker -> notificari/email

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

Se aplica la inchiderea licitatiei, la suspendarea administrativa si la plasarea bid-urilor prin:

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
6. worker-ul creeaza notificari in-app
7. worker-ul trimite email pentru tipurile eligibile si actualizeaza statusul de delivery

Campurile de audit salvate de worker:

- `event_type`
- `aggregate_id`
- `payload`
- `processed_at`
- `source = AUCTION_WORKER`

Audit-ul nu modifica starea licitatiilor. El este strict o persistenta a procesarii mesajelor.

## Event types actuale

In acest moment exista:

- `AUCTION_STARTED`
- `BID_PLACED`
- `AUCTION_EXTENDED`
- `AUCTION_CLOSED`
- `AUCTION_SUSPENDED`

Tipurile de notificari in-app suportate acum sunt:

- `OUTBID`
- `AUCTION_WON`
- `AUCTION_LOST`
- `AUCTION_CLOSED`
- `AUCTION_EXTENDED`
- `NEW_BID_ON_OWN_AUCTION`

Tipurile care trimit email in MVP sunt:

- `AUCTION_WON`
- `OUTBID`
- `AUCTION_CLOSED`
- `AUCTION_SUSPENDED`

## Endpoint-uri actuale

- `GET /api/auctions`
- `GET /api/auctions/{id}`
- `POST /api/auctions`
- `PUT /api/auctions/{id}`
- `POST /api/auctions/{id}/watch`
- `DELETE /api/auctions/{id}/watch`
- `GET /api/auctions/me/watchlist`
- `GET /api/auctions/me/created`
- `GET /api/auctions/me/bids`
- `POST /api/auctions` si `PUT /api/auctions/{id}` accepta si `reservePrice`, `buyNowPrice`
- `GET /api/analytics/dashboard`
- `GET /api/fraud/signals`
- `GET /api/me/notifications`
- `GET /api/me/notifications/unread-count`
- `POST /api/me/notifications/{id}/read`
- `POST /api/me/notifications/read-all`
- `POST /api/auctions/{id}/start`
- `POST /api/auctions/{id}/close`
- `POST /api/auctions/{id}/buy-now`
- `POST /api/auctions/{id}/bids`
- `GET /api/auctions/{id}/bids`

## Rute frontend actuale

- `GET /auctions`
- `GET /dashboard`
- `GET /fraud-signals`
- `GET /auctions/new`
- `GET /my-watchlist`
- `GET /my-auctions`
- `GET /my-bids`
- `GET /notifications`
- `GET /auctions/:id/edit`
- `GET /auctions/:id`

## Contracte UI importante

### `AuctionResponse`

Frontend-ul consuma un model stabil care include:

- `id`
- `title`
- `description`
- `startPrice`
- `currentPrice`
- `minIncrement`
- `status`
- `startTime`
- `endTime`
- `antiSnipingWindowSec`
- `antiSnipingExtendSec`
- `createdBy`
- `winnerId`
- `winningBidId`
- `finalPrice`
- `closedAt`
- `closedReason`
- `images[]`
- `watchersCount`
- `watchedByCurrentUser`
- `version`

### `AuctionImageResponse`

Fiecare imagine expusa de backend include:

- `id`
- `imageUrl`
- `displayOrder`

Backend-ul serveste fisierele incarcate prin:

- `GET /media/**`

### `BidResponse`

Frontend-ul foloseste si campurile:

- `auctionExtended`
- `newEndTime`

pentru a afisa contextul anti-sniping si pentru a marca bid-urile care au extins licitatia.

### `AuctionRealtimeEvent<T>`

Evenimentele live pentru UI au forma:

```ts
export interface AuctionRealtimeEvent<T = AuctionBusinessEvent> {
  type: AuctionEventType;
  payload: T;
  occurredAt: string;
}
```

Tipurile de evenimente folosite in UI sunt:

- `AUCTION_STARTED`
- `BID_PLACED`
- `AUCTION_EXTENDED`
- `AUCTION_CLOSED`
- `AUCTION_SUSPENDED`

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
5. sistemul determina winner-ul si sumarul final daca exista bids
6. se salveaza `AUCTION_CLOSED`
7. evenimentul este publicat in RabbitMQ
8. worker-ul il persista in `audit_events`

### Scenariul 4: creare si administrare completa din UI

1. utilizatorul intra pe `/auctions`
2. apasa `Create auction`
3. completeaza formularul si trimite cererea
4. UI-ul redirectioneaza la pagina de detalii
5. utilizatorul porneste licitatia din `DRAFT`
6. licitatia devine `RUNNING`
7. bid-urile si evenimentele live sunt vizibile in aceeasi pagina

### Scenariul 5: anti-sniping vizibil in UI

1. licitatia se apropie de expirare
2. utilizatorul sau alt client trimite un bid valid
3. backend-ul extinde `endTime`
4. UI-ul primeste `AUCTION_EXTENDED`
5. countdown-ul se prelungeste imediat
6. istoricul bid-urilor marcheaza extensia

### Scenariul 6: filtrare operationala in UI

1. utilizatorul intra pe `/auctions`
2. selecteaza `RUNNING` pentru a vedea doar licitatiile active
3. cauta dupa un cuvant din titlu sau descriere
4. UI-ul filtreaza local lista fara request suplimentar
5. utilizatorul intra direct pe licitatia relevanta sau executa o actiune rapida

### Scenariul 7: notificare de outbid

1. utilizatorul A liciteaza pe o licitatie
2. utilizatorul B trimite ulterior un bid mai mare
3. sistemul accepta bid-ul nou si publica `BID_PLACED`
4. `auction-worker` creeaza notificare `OUTBID` pentru bidderii anteriori afectati
5. UI-ul afiseaza badge unread pentru utilizatorul relevant
6. utilizatorul intra in `/notifications` si merge direct la licitatie

### Scenariul 8: notificare de inchidere

1. licitatia se inchide manual sau automat
2. sistemul publica `AUCTION_CLOSED`
3. `auction-worker` creeaza notificari pentru winner, losing bidders, seller si watcherii relevanti
4. UI-ul actualizeaza unread badge
5. utilizatorii pot marca notificarile ca citite individual sau bulk

### Scenariul 9: castigarea unei licitatii

1. licitatia se inchide cu un winner valid
2. `auction-worker` creeaza notificare `AUCTION_WON` pentru userul castigator
3. polling-ul de notificari detecteaza notificarea noua
4. UI-ul actualizeaza badge-ul unread din header
5. UI-ul afiseaza imediat si un toast global in coltul dreapta-sus

### Scenariul 10: email de outbid

1. utilizatorul A este depasit de un bid mai mare
2. worker-ul creeaza notificarea `OUTBID`
3. worker-ul rezolva email-ul user-ului din tabela `users`
4. worker-ul trimite email prin `MailHog`
5. notificarea este marcata cu `email_delivery_status = SENT`

## Date de test utile

Exemple de useri locali folositi in testare:

- `collector@archivebid.test` / `password`
- `curator@archivebid.test` / `password`
- `dealer@archivebid.test` / `password`
- `admin@archivebid.test` / `password`

## Ce urmeaza tehnic

Backlog-ul si ordinea de implementare se mentin in:

- `ROADMAP.md`

Autentificarea si autorizarea reale au fost integrate.

Urmatorii pasi recomandati sunt:

1. stabilizare finala
   - smoke test end-to-end pentru fluxurile principale
   - verificare manuala pentru scenariile `RUNNING`, `ENDED`, `SUSPENDED`
   - verificare notificari in-app, email, auth si fraud modal
2. curatare pentru livrare
   - revizie finala a textelor din UI
   - verificare consistentei `README.md` si `ROADMAP.md`
   - capturi de ecran pentru lucrare / prezentare
3. pregatire pentru sustinere
   - definirea a 3-5 scenarii demo
   - pregatirea unei prezentari scurte pentru arhitectura si fluxuri
   - notarea limitelor MVP asumate explicit

Feature-uri precum `Reviews / Rating`, `Advanced bid increment rules`, wallet, orders sau shipping raman in afara scope-ului actual.
