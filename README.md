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

| Zmienna       | Przykład                                   |
|---------------|--------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/coupon`  |
| `DB_USERNAME` | `coupon`                                   |
| `DB_PASSWORD` | `coupon`                                   |

Uruchomienie zbudowanego pliku JAR:

```bash
./gradlew bootJar
DB_URL=jdbc:postgresql://localhost:5432/coupon DB_USERNAME=coupon DB_PASSWORD=coupon \
  java -jar build/libs/coupon-service-0.0.1-SNAPSHOT.jar
```

## API

Dokumentacja OpenAPI (przy uruchomionej aplikacji):

- Swagger UI: http://localhost:8080/swagger-ui.html
- specyfikacja JSON: http://localhost:8080/v3/api-docs

Przykładowe żądania dla klienta HTTP z IntelliJ IDEA są w katalogu `http/` (środowisko `local`).

## Testy

| Polecenie                    | Zakres                                                                 |
|------------------------------|------------------------------------------------------------------------|
| `./gradlew test`             | testy jednostkowe (bez klas `*IT`), nie wymagają Dockera               |
| `./gradlew integrationTest`  | testy integracyjne (klasy `*IT`), PostgreSQL uruchamiany w Testcontainers, Free IP API zastąpione przez [WireMock](https://wiremock.org/) |
| `./gradlew check`            | testy jednostkowe, integracyjne i sprawdzenie formatowania             |
| `./gradlew jacocoTestReport` | testy jednostkowe i integracyjne + raport pokrycia kodu ([JaCoCo](https://www.jacoco.org/jacoco/)) |

Testy są pisane w Groovy ([Spock](https://spockframework.org/)).

Raport pokrycia (łączny dla obu rodzajów testów) trafia do `build/reports/jacoco/test/html/index.html` (wersja XML: `build/reports/jacoco/test/jacocoTestReport.xml`).

## Budowanie

Pełne budowanie: kompilacja, wszystkie testy (również integracyjne — wymaga Dockera) i sprawdzenie formatowania:

```bash
./gradlew build
```

Wykonywalny plik JAR trafia do `build/libs/`.

Wersje wtyczek Gradle, bibliotek spoza zarządzania Spring Boot oraz narzędzi (JaCoCo, palantir-java-format) są
zdefiniowane w `gradle.properties`.

## Formatowanie kodu

Formatowanie jest pilnowane przez [Spotless](https://github.com/diffplug/spotless):

- kod Java — [palantir-java-format](https://github.com/palantir/palantir-java-format),
- skrypty `*.gradle` — formatter Groovy z Eclipse (greclipse), wcięcia 4 spacjami.

Testy w Groovy nie są objęte formatowaniem.

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
4. budowanie artefaktu (`./gradlew assemble`).

Jeśli któryś krok się nie powiedzie, raporty z testów są dostępne jako artefakt `test-reports`.
