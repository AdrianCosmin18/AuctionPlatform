# Auction Platform Roadmap

Acest fisier este sursa de context pentru pasii urmatori ai proiectului.

Regula de lucru:

1. Inainte de a incepe un feature nou, verificam acest fisier.
2. Alegem urmatorul item din sectiunea `Next`.
3. Dupa fiecare feature implementat, actualizam:
   - statusul din acest fisier
   - `README.md`
4. Daca schimbam ordinea prioritatilor, modificam aici mai intai.

## Current State

Aplicatia suporta acum:

- licitatii cu stari `DRAFT`, `RUNNING`, `ENDED`, `SUSPENDED`
- creare, listare, detalii, start si close pentru licitatii
- editare licitatii `DRAFT` prin reutilizarea formularului de create
- bids cu validare minim next bid
- anti-sniping cu extensie automata
- determinare winner si final price la close
- auto-close pentru licitatiile expirate
- outbox pattern + RabbitMQ + worker audit
- live events prin WebSocket/STOMP
- upload local de imagini pentru licitatii
- galerie imagini in UI
- taxonomy de categorie/subcategorie in UI si filtre locale in marketplace
- watchlist cu `Watch / Unwatch`, `watchers count` si pagina `My Watchlist`
- zona `My Area` cu pagini `My Auctions`, `My Bids` si `My Watchlist`
- profil utilizator cu date de contact, meniu de cont in header si schimbare parola
- reserve price optional cu `reservePrice` si `reserveMet` in backend, worker si UI
- buy now optional cu `buyNowPrice`, endpoint dedicat si inchidere imediata cu `closedReason = BUY_NOW`
- dashboard operational cu KPI-uri si breakdown-uri pe categorie pentru bids si watchlist
- fraud signals pentru `bidder pair dominance` si `seller-bidder concentration`, cu suspendare administrativa pentru licitatii `RUNNING`
- notificari in-app generate asincron din evenimente si afisate in UI cu unread badge
- toast global in coltul ecranului pentru notificari noi de tip `AUCTION_WON`
- email notifications livrate din worker prin SMTP configurabil (`MailHog` local sau provider real) pentru evenimentele importante
- status terminal `SUSPENDED` cu `suspendedAt`, `suspendedBy` si `suspensionReason`
- autentificare reala cu `USER` / `ADMIN`, JWT si eliminarea `X-User-Id`
- route guards in Angular si protectie explicita pentru endpoint-urile/paginile admin

Observatii importante despre starea curenta:

- taxonomy-ul exista deja functional in UI si in modelul `auctions` prin `categoryCode` si `subcategoryCode`
- nu exista inca o tabela normalizata `categories`
- imaginile nu sunt salvate in baza ca blob; se pastreaza ca fisiere locale
- imagini seed/demo sunt in `auction-api/src/main/resources/static/demo-images/`
- upload-urile runtime sunt in `uploads/`
- scriptul de generare pentru asset-urile demo este in `scripts/generate_thematic_demo_assets.mjs`

## Working Rules For Next Features

Pentru fiecare feature nou:

1. backend first
   - schema DB
   - entity/repository/service/controller
   - teste
2. apoi frontend
   - model
   - API service
   - pages/components
   - UX states
3. apoi documentatie
   - update in `README.md`
   - update status in acest fisier

## Completed

- Core auction lifecycle
- Bid flow
- Anti-sniping
- Winner/finalization logic
- Outbox + RabbitMQ + worker audit
- WebSocket live events
- Local image upload + gallery
- Draft edit flow with reusable form
- Category/subcategory taxonomy in create, details and list filters
- Watchlist / Favorite Auctions
- My Area
- User Profile / Account Settings
- Reserve Price
- Buy Now
- Analytics / Dashboard
- Fraud / Suspicious Activity Detection
- In-App Notifications
- Email Notifications
- Admin auction preview modal from fraud review
- Fraud review filtering by actionable vs inactive auctions
- Auth / JWT / Role-based access

### Final Stabilization

Status: `active`

- smoke test end-to-end pentru fluxurile principale dupa integrarea auth
- verificare manuala pentru:
  - create -> start -> bid -> auto/manual close
  - reserve price
  - buy now
  - watchlist
  - notifications + email delivery (MailHog sau SMTP real)
  - fraud review + suspendare administrativa
- verificare documentatie si screenshots finale

## Later

### Reviews / Rating

Status: `planned`

- scos din scope-ul MVP curent
- poate fi reluat ulterior pe baza licitatiilor inchise cu winner valid

### Advanced Bid Rules

Status: `planned`

- scos din scope-ul MVP curent
- increment fix vs step rules
- minimum next bid mai realist pe intervale de pret

## Optional Architecture Improvement

### Normalized Categories In DB

Status: `optional-later`

Observatie:

- taxonomy-ul exista deja functional prin coduri hardcodate si filtre in UI
- daca vrem model mai matur, putem introduce:
  - tabela `categories`
  - optional tabela `subcategories`

Nu este urgent pentru MVP-ul actual, pentru ca functionalitatea de categorie exista deja.

## Next Execution Order

Ordinea recomandata de implementare de acum:

1. Final stabilization
2. Demo preparation
3. Optional post-MVP backlog review

## Definition Of Done

Un feature este considerat terminat doar daca are:

- schema + backend business logic
- endpoint-uri sau consumers relevante
- UI functional
- validari si stari de eroare
- test minim relevant
- update in `README.md`
- update in acest roadmap


Ultima directie convenita:
feature-urile principale sunt suficiente pentru MVP, iar focusul se muta pe documentatie, stabilizare si pregatirea demo-ului final.
