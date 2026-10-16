# Java Structure Bar

Plugin per IntelliJ IDEA che aggiunge in cima a ogni editor Java una barra di navigazione in stile Visual Studio, con due menu a tendina:

- **Classi** — tutte le classi del file, comprese quelle annidate (es. `Outer.Inner`).
- **Membri** — campi, costruttori, metodi e componenti dei record della classe selezionata, con tipo e firma in grigio (es. `find(String, int) : User`).

Entrambi i menu seguono il cursore: spostandoti nel codice si aggiornano sulla classe e sul membro in cui ti trovi. Scegliendo una voce l'editor salta alla sua dichiarazione.

## Caratteristiche

- Voci ordinate alfabeticamente, con le icone e la visibilità standard di IntelliJ.
- Aggiornamento automatico mentre scrivi (con un breve ritardo per non rallentare l'editor).
- Vengono ignorati i membri non scritti nel sorgente, come i metodi impliciti di enum e record.
- Funziona anche durante l'indicizzazione.

## Requisiti

- IntelliJ IDEA 2026.2 o successivo (build `262+`)
- Plugin Java incluso nell'IDE

## Build

Serve un JDK 25 (Gradle lo usa come toolchain). Il wrapper scarica automaticamente Gradle 9.8.

```bash
./gradlew buildPlugin      # crea lo ZIP del plugin in build/distributions/
./gradlew runIde           # avvia un IDE di prova con il plugin installato
./gradlew test             # esegue i test
./gradlew verifyPlugin     # verifica la compatibilità con IntelliJ IDEA 2026.2.3
```

Su Windows usa `gradlew.bat` al posto di `./gradlew`.

## Installazione

1. Esegui `./gradlew buildPlugin`.
2. In IntelliJ IDEA apri **Settings → Plugins → ⚙ → Install Plugin from Disk…**
3. Seleziona lo ZIP in `build/distributions/` e riavvia l'IDE.

## Struttura del progetto

| File | Ruolo |
|------|-------|
| `StructureBarProvider.kt` | Aggancia la barra agli editor dei file `.java` |
| `StructureBar.kt` | Interfaccia: i due menu, la sincronizzazione con il cursore e la navigazione |
| `StructureBuilder.kt` | Legge il file Java (PSI) e costruisce l'elenco di classi e membri |
| `StructureBuilderTest.kt` | Test della costruzione della struttura |
