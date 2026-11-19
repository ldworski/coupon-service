# coupon-service

Aplikacja Spring Boot budowana Gradle.

## Wymagania

- JDK 25 (Gradle może pobrać go automatycznie przez toolchain)

Gradle nie trzeba instalować — projekt używa Gradle Wrappera (`./gradlew`, na Windows `gradlew.bat`).

## Budowanie

Pełne budowanie: kompilacja, testy i sprawdzenie formatowania:

```bash
./gradlew build
```

Same testy:

```bash
./gradlew test
```

Wykonywalny plik JAR trafia do `build/libs/`.

## Uruchamianie

Uruchomienie aplikacji z kodu źródłowego:

```bash
./gradlew bootRun
```

Uruchomienie zbudowanego pliku JAR:

```bash
./gradlew bootJar
java -jar build/libs/coupon-service-0.0.1-SNAPSHOT.jar
```

## Formatowanie kodu

Formatowanie jest pilnowane przez [Spotless](https://github.com/diffplug/spotless):

- kod Java — [google-java-format](https://github.com/google/google-java-format) (Google Java Style),
- skrypty `*.gradle.kts` — [ktlint](https://github.com/pinterest/ktlint).

Sprawdzenie formatowania (uruchamiane też w ramach `./gradlew build` i w CI):

```bash
./gradlew spotlessCheck
```

Automatyczne poprawienie formatowania:

```bash
./gradlew spotlessApply
```

W IntelliJ IDEA warto zainstalować plugin **google-java-format** (i włączyć go w *Settings → google-java-format Settings*), żeby formatowanie w IDE było zgodne z CI.

## CI

Workflow GitHub Actions (`.github/workflows/build.yml`) uruchamia `./gradlew build` przy każdym pushu na `main` i przy
pull requestach do `main`.
