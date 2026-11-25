# coupon-service

Serwis do zarządzania kuponami rabatowymi. Spring Boot, PostgreSQL, budowany Gradle.

## Wymagania

- JDK 25 (Gradle może pobrać go automatycznie przez toolchain)
- Docker: lokalna baza danych (`compose.yaml`) i testy integracyjne (Testcontainers)

Gradle nie trzeba instalować — projekt używa Gradle Wrappera (`./gradlew`, na Windows `gradlew.bat`).

## Uruchamianie lokalnie

1. Uruchom bazę PostgreSQL:

   ```bash
   docker compose up -d
   ```

2. Uruchom aplikację z profilem `local` (zawiera dane dostępowe do bazy z `compose.yaml`):

   ```bash
   SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
   ```

Schemat bazy tworzy Flyway przy starcie aplikacji (`src/main/resources/db/migration`).

Aplikacja nasłuchuje na `http://localhost:8080`.

Bazę z danymi można usunąć i postawić od nowa (np. po zmianie istniejącej migracji):

```bash
docker compose down -v && docker compose up -d
```

### Uruchamianie bez profilu `local`

Bez profilu `local` dane dostępowe do bazy muszą być podane w zmiennych środowiskowych:

| Zmienna       | Przykład                                  |
|---------------|-------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/coupon` |
| `DB_USERNAME` | `coupon`                                  |
| `DB_PASSWORD` | `coupon`                                  |

Uruchomienie zbudowanego pliku JAR:

```bash
./gradlew bootJar
DB_URL=jdbc:postgresql://localhost:5432/coupon DB_USERNAME=coupon DB_PASSWORD=coupon \
  java -jar build/libs/coupon-service-0.0.1-SNAPSHOT.jar
```

### Docker

Aplikację razem z bazą można uruchomić przez Docker Compose. Serwis `app` ma profil `app`, więc samo
`docker compose up -d` dalej uruchamia tylko bazę (do pracy z `bootRun`):

```bash
docker compose --profile app up -d --build    # baza + aplikacja na http://localhost:8080
docker compose --profile app down             # zatrzymanie (dane bazy zostają w wolumenie)
```

Kontener aplikacji w Compose działa z systemem plików tylko do odczytu (zapisywalny tylko `/tmp`), bez dodatkowych
uprawnień jądra (`cap_drop: ALL`) i z blokadą podnoszenia uprawnień (`no-new-privileges`).

Budowanie samego obrazu (testy nie są uruchamiane w trakcie budowania obrazu — uruchamia je CI):

```bash
docker build -t coupon-service .
```

Uruchomienie z bazą z `compose.yaml` (uruchomioną na hoście):

```bash
docker run --rm --network host \
  -e DB_URL=jdbc:postgresql://localhost:5432/coupon -e DB_USERNAME=coupon -e DB_PASSWORD=coupon \
  coupon-service
```

Konfigurację podaje się zmiennymi środowiskowymi (patrz [Konfiguracja](#konfiguracja)). Ustawienia JVM można nadpisać
zmienną `JAVA_TOOL_OPTIONS` (domyślnie `-XX:MaxRAMPercentage=75.0`, czyli sterta do 75% limitu pamięci kontenera).

Budowa obrazu (`Dockerfile`):

- dwa etapy: budowanie na JDK (Gradle), uruchamianie na samym JRE — narzędzia budowania nie trafiają do obrazu,
- obrazy bazowe Eclipse Temurin 25 przypięte do wersji i digestu: JDK na Ubuntu 24.04 do budowania, JRE na Alpine Linux
  do uruchamiania (mniejszy obraz, mniej pakietów systemowych),
- warstwa z Gradle i zależnościami budowana tylko przy zmianie plików Gradle, nie przy każdej zmianie kodu,
- JAR rozpakowany na [warstwy Spring Boot](https://docs.spring.io/spring-boot/reference/packaging/container-images/efficient-images.html)
  (zależności osobno od kodu aplikacji) — po zmianie kodu zmienia się tylko warstwa aplikacji (ok. 100 kB),
- aplikacja działa jako użytkownik bez uprawnień roota (`app`, UID 10001), a jej pliki należą do roota (tylko do odczytu),
- `.dockerignore` przepuszcza do kontekstu budowania tylko pliki potrzebne do zbudowania aplikacji.

## Konfiguracja

Ustawienia aplikacji są w `src/main/resources/application.yaml`. Każdą wartość można nadpisać zmienną środowiskową
(myślniki się pomija, kropki zastępuje `_`, np. `COUPONSERVICE_FREEIPAPI_READTIMEOUT=3s`) lub argumentem
`--klucz=wartość`. Wszystkie klucze
`coupon-service.*` są wymagane — brak któregokolwiek zatrzymuje start aplikacji.

### Free IP API (`coupon-service.free-ip-api`)

Kraj użytkownika jest ustalany na podstawie adresu IP przez [Free IP API](https://freeipapi.com/). Darmowy plan
pozwala na ok. 60 zapytań na minutę.

| Klucz               | Domyślnie                    | Opis                                                  |
|---------------------|------------------------------|-------------------------------------------------------|
| `base-url`          | `https://free.freeipapi.com` | adres API                                             |
| `connect-timeout`   | `5s`                         | timeout nawiązania połączenia                         |
| `read-timeout`      | `5s`                         | timeout odczytu odpowiedzi                            |
| `retry.max-retries` | `3`                          | liczba ponowień po błędzie 5xx lub błędzie połączenia |
| `retry.delay`       | `200ms`                      | opóźnienie przed pierwszym ponowieniem                |
| `retry.multiplier`  | `2`                          | mnożnik opóźnienia dla kolejnych ponowień             |
| `retry.jitter`      | `100ms`                      | losowe odchylenie opóźnienia                          |
| `retry.max-delay`   | `2s`                         | maksymalne opóźnienie między ponowieniami             |

Błędy 4xx (np. 429 po przekroczeniu limitu) nie są ponawiane. Gdy API pozostaje niedostępne, żądanie realizacji kuponu
kończy się odpowiedzią `503` z nagłówkiem `Retry-After`.

### Cache (`coupon-service.cache`)

| Cache               | `maximum-size` | `expire-after-write` | Zawartość                                                        |
|---------------------|----------------|----------------------|------------------------------------------------------------------|
| `ip-countries`      | `50000`        | `12h`                | kraj ustalony dla adresu IP — ogranicza zapytania do Free IP API |
| `exhausted-coupons` | `10000`        | `30d`                | wyczerpane kupony — odrzucane bez zapytania do bazy              |

Cache jest lokalny dla każdej instancji aplikacji (Caffeine).

### Baza danych

| Klucz                                                    | Domyślnie | Opis                                                                                       |
|----------------------------------------------------------|-----------|--------------------------------------------------------------------------------------------|
| `spring.jpa.properties.jakarta.persistence.lock.timeout` | `2000`    | maksymalny czas (ms) oczekiwania na blokadę kuponu przy realizacji; po przekroczeniu `503` |

Dane dostępowe do bazy — patrz [Uruchamianie bez profilu `local`](#uruchamianie-bez-profilu-local).

### Tracing

Śledzenie żądań zapewnia Micrometer Tracing z OpenTelemetry (`spring-boot-starter-opentelemetry`):

- każde żądanie HTTP dostaje `traceId` (standard [W3C Trace Context](https://www.w3.org/TR/trace-context/)); jeśli
  żądanie zawiera nagłówek `traceparent`, aplikacja kontynuuje przekazany ślad,
- `traceId` i `spanId` są dopisywane do logów (`[traceId-spanId]`) i zwracane w odpowiedziach błędów jako
  `correlationId` (patrz [API](#api)),
- spany nie są nigdzie wysyłane, dopóki nie zostanie ustawiony adres kolektora OpenTelemetry
  (`management.opentelemetry.tracing.export.otlp.endpoint`, np. `http://localhost:4318/v1/traces`); domyślnie
  wysyłane jest 10% śladów (`management.tracing.sampling.probability`),
- eksport metryk OTLP jest wyłączony (`management.otlp.metrics.export.enabled: false`), bo nie ma jeszcze kolektora.

Wywołania Free IP API nie są śledzone (zewnętrzne API, poza organizacją).

### Health checks

Spring Boot Actuator udostępnia endpointy stanu aplikacji (np. dla probe'ów Kubernetesa):

- liveness: http://localhost:8080/actuator/health/liveness — tylko stan samej aplikacji (awaria bazy nie powoduje
  restartu),
- readiness: http://localhost:8080/actuator/health/readiness — stan aplikacji i połączenie z bazą (bez bazy
  instancja nie przyjmuje ruchu).

Healthcheck serwisu `app` w `compose.yaml` korzysta z endpointu readiness.

## API

Dokumentacja OpenAPI (przy uruchomionej aplikacji):

- Swagger UI: http://localhost:8080/swagger-ui.html
- specyfikacja JSON: http://localhost:8080/v3/api-docs

Przykładowe żądania dla klienta HTTP z IntelliJ IDEA są w katalogu `http/` (środowisko `local`).

Błędy są zwracane w formacie [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`), z
dodatkowymi polami `timestamp` (czas wystąpienia błędu) i `correlationId` (`traceId` żądania — pozwala znaleźć
powiązane wpisy w logach):

```json
{
  "title": "Coupon not found",
  "status": 404,
  "detail": "Coupon with code WINTER2026 not found",
  "timestamp": "2026-11-23T11:15:30.123Z",
  "correlationId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

## Testy

| Polecenie                    | Zakres                                                                                                                                    |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `./gradlew test`             | testy jednostkowe (bez klas `*IT`), nie wymagają Dockera                                                                                  |
| `./gradlew integrationTest`  | testy integracyjne (klasy `*IT`), PostgreSQL uruchamiany w Testcontainers, Free IP API zastąpione przez [WireMock](https://wiremock.org/) |
| `./gradlew check`            | testy jednostkowe, integracyjne, sprawdzenie formatowania i progu pokrycia kodu                                                           |
| `./gradlew jacocoTestReport` | testy jednostkowe i integracyjne + raport pokrycia kodu ([JaCoCo](https://www.jacoco.org/jacoco/))                                        |

Testy są pisane w Groovy ([Spock](https://spockframework.org/)).

Testy integracyjne dziedziczą po `IntegrationTestSpecification`, która włącza profil `test`. Ustawienia tylko dla testów
(np. krótsze opóźnienia ponowień wywołań Free IP API) są w `src/test/resources/application-test.yaml`.

Raport pokrycia (łączny dla obu rodzajów testów) trafia do `build/reports/jacoco/test/html/index.html` (wersja XML:
`build/reports/jacoco/test/jacocoTestReport.xml`).

Minimalne pokrycie linii i gałęzi to 80% — sprawdza je `./gradlew jacocoTestCoverageVerification` (uruchamiane też
w ramach `./gradlew check`/`build` i w CI); poniżej progu build kończy się błędem.

## Budowanie

Pełne budowanie: kompilacja, wszystkie testy (również integracyjne — wymaga Dockera), sprawdzenie formatowania i progu
pokrycia kodu:

```bash
./gradlew build
```

Wykonywalny plik JAR trafia do `build/libs/`.

Wersje wtyczek Gradle, bibliotek spoza zarządzania Spring Boot oraz narzędzi (JaCoCo, palantir-java-format) są
zdefiniowane w `gradle.properties`.

## Formatowanie kodu

Formatowanie jest pilnowane przez [Spotless](https://github.com/diffplug/spotless):

- kod Java — [palantir-java-format](https://github.com/palantir/palantir-java-format),
- skrypty `*.gradle` — formatter Groovy z Eclipse (greclipse), wcięcia 4 spacjami,
- testy w Groovy (Spock) — tylko kolejność importów (najpierw statyczne), usuwanie średników, białe znaki na końcu
  linii, wcięcia spacjami i znak nowej linii na końcu pliku. Pełny formatter (greclipse) nie jest używany, bo psuje
  wyrównanie tabel `where:`. Układ kodu testów formatuje się w IntelliJ IDEA (*Reformat Code* wyrównuje tabele
  Spocka); ustawienia importów i wcięć dla IDE są w `.editorconfig`.

Sprawdzenie formatowania (uruchamiane też w ramach `./gradlew check`/`build` i w CI):

```bash
./gradlew spotlessCheck
```

Automatyczne poprawienie formatowania:

```bash
./gradlew spotlessApply
```

W IntelliJ IDEA warto zainstalować plugin **palantir-java-format**, żeby formatowanie w IDE było zgodne z CI.

## CI

Workflow GitHub Actions (`.github/workflows/build.yml`) uruchamia się przy każdym pushu na `main` i przy pull requestach
do `main`. Kolejne kroki:

1. sprawdzenie formatowania (`./gradlew spotlessCheck`),
2. testy jednostkowe (`./gradlew test`),
3. testy integracyjne (`./gradlew integrationTest`),
4. raport pokrycia kodu i sprawdzenie progu 80% (`./gradlew jacocoTestReport jacocoTestCoverageVerification`),
5. budowanie artefaktu (`./gradlew assemble`).

Raport pokrycia jest zawsze dostępny jako artefakt `coverage-report`. Jeśli któryś krok się nie powiedzie, raporty z
testów są dostępne jako artefakt `test-reports`.
