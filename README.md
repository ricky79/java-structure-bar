# Java Structure Bar

An IntelliJ IDEA plugin that adds a Visual Studio style navigation bar at the top of every Java editor, with two dropdowns:

- **Classes**: every class in the file, nested ones included (e.g. `Outer.Inner`).
- **Members**: fields, constructors, methods and record components of the selected class, with their type and signature in grey (e.g. `find(String, int) : User`).

Both dropdowns follow the caret: as you move through the code they update to the class and member you are in. Choosing an entry jumps the editor to its declaration.

## Features

- Entries sorted alphabetically, with IntelliJ's standard icons and visibility markers.
- Updates automatically as you type (with a short delay so the editor stays responsive).
- Members that are not written in the source, such as implicit enum and record methods, are left out.
- Works while the IDE is indexing.

## Requirements

- IntelliJ IDEA 2026.2 or later (build `262+`)
- The bundled Java plugin

## Build

You need JDK 25 (Gradle uses it as the toolchain). The wrapper downloads Gradle 9.8 automatically.

```bash
./gradlew buildPlugin      # builds the plugin ZIP in build/distributions/
./gradlew runIde           # starts a sandbox IDE with the plugin installed
./gradlew test             # runs the tests
./gradlew verifyPlugin     # checks compatibility with IntelliJ IDEA 2026.2.3
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Installation

1. Run `./gradlew buildPlugin`.
2. In IntelliJ IDEA open **Settings → Plugins → ⚙ → Install Plugin from Disk…**
3. Select the ZIP in `build/distributions/` and restart the IDE.

## Project structure

| File | Role |
|------|------|
| `StructureBarProvider.kt` | Attaches the bar to editors of `.java` files |
| `StructureBar.kt` | UI: the two dropdowns, caret syncing and navigation |
| `StructureBuilder.kt` | Reads the Java file (PSI) and builds the list of classes and members |
| `StructureBuilderTest.kt` | Tests for building the structure |

## License

Copyright (C) 2026 Riccardo Oliosi

This program is free software: you can redistribute it and/or modify it under the terms of the [GNU General Public License](LICENSE) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
