# ATELIER — Virtual Closet

A complete Java web application for a real-life problem: choosing outfits from clothes you already own. Organize a wardrobe, mix individual pieces, shuffle a look, upload your own garment photos, and keep favourite combinations. Its neutral editorial grid and outfit mixer are inspired by the supplied fashion UI references. The original HTML and console Java proposal informed the feature set; this project replaces them with one integrated application.

## Start here

**Fastest: Java 17 or newer is all you need to run the included application.** Java 21 LTS is recommended. Extract the ZIP, open a terminal in the `atelier` directory, then:

```sh
java -jar dist/atelier-1.0.0.jar
```

Open **http://localhost:8080** in your browser. Keep the terminal open. Stop with Ctrl+C. Windows users can double-click `start-windows.cmd` after installing Java and adding it to PATH.

There are no API keys, database servers, frontend build tools, accounts, or paid services required. The first start creates 12 local sample garments and one sample look. All interface files and garment images are bundled locally; after downloading the app, it runs without internet.

## Build and test from source

Prerequisites: JDK 17+ (check `java -version`), Apache Maven 3.6.3+ (check `mvn -version`), internet for the initial dependency download. Spring Boot version is pinned in `pom.xml`. Official compatibility: https://docs.spring.io/spring-boot/3.5/system-requirements.html

From the project directory:

```sh
mvn clean package
java -jar target/atelier-1.0.0.jar
```

Or run during development:

```sh
mvn spring-boot:run
```

Run the automated tests alone with `mvn test`. The prebuilt JAR is a convenience copy; after changing source, build and run the JAR under `target`.

## How to use it

1. In **Wardrobe**, filter by category, season, occasion, or search by name. `All season` garments match every selected wardrobe season. Occasion matching is exact.
2. Click a garment or **Style this piece** to place it in the studio.
3. In **Outfit studio**, use arrows or dropdowns for Top/Dress, Bottom, Outerwear, Shoes, Bag, Accessory, and Headwear. Each position is optional. A dress replaces the top and bottom. Clear individual pieces or the whole selection.
4. **Shuffle a look** asks the Java backend to randomly select one compatible piece per available position using current season and occasion filters. An empty result asks you to change filters.
5. Name a composition, choose its season and occasion, then save. Open or delete it in **Saved looks**. Saved look metadata is matched exactly by the lookbook filters.
6. **Add a piece** accepts actual PNG or JPEG files, up to 8 MB and 20 megapixels. File contents are checked on the server, not just the filename. Enter name, category, season, and occasion.
7. **Remove** works for both samples and uploads. A confirmation explains that it also removes the piece from saved outfits. An outfit whose last piece is deleted is removed automatically. Deleted samples do not reappear on restart.

The collection is shared by all visitors to this one application instance. It is intentionally a classroom/single-closet demo, with no sign-in or separate user accounts. Public visitors can change its collection; use private access for a personal closet or add authentication before a broad public launch.

## Persistence and backup

The default `data/` directory is relative to the directory where you launch Java. It contains `closet.json` and `uploads/`. Each successful mutation writes a new JSON snapshot through a temporary file and atomic replacement where supported. Methods are synchronized to coordinate simultaneous requests in one Java process.

Keep a copy of the entire `data` directory for backup. Stop the app before backing up or restoring it. Use one instance per data directory. For a fresh demo, stop the server and move the existing `data` folder to a backup location; a new folder is seeded on next launch. Startup deliberately fails on corrupt data rather than silently overwriting your wardrobe.

To set an explicit persistent location, use `ATELIER_DATA`:

Windows PowerShell:

```powershell
$env:ATELIER_DATA = 'C:\AtelierData'
$env:PORT = '8080'
java -jar dist/atelier-1.0.0.jar
```

macOS/Linux:

```sh
ATELIER_DATA="$HOME/atelier-data" PORT=8080 java -jar dist/atelier-1.0.0.jar
```

## Deployment

This app needs a Java server or Docker host; GitHub Pages and other static-only hosting cannot run the backend. The ZIP is deployable source, not an already published URL.

**Docker (persistent classroom deployment):** install Docker, then run from this directory:

```sh
docker build -t atelier .
docker volume create atelier-data
docker run -d --name atelier -p 8080:8080 -v atelier-data:/app/data atelier
```

Open `http://localhost:8080`. For a hosted Docker service, choose this repository's Dockerfile, expose the `PORT` supplied by the host (default 8080), set `ATELIER_DATA=/app/data`, and attach a persistent disk to `/app/data`. Deploy a single instance. Data on an ephemeral filesystem will be lost when the container is replaced. Do not include `data/` in an image.

**Java hosting / VPS:** build with `mvn -B clean package`; start with `java -jar target/atelier-1.0.0.jar`. Configure `PORT` and a writable persistent `ATELIER_DATA` directory. Put HTTPS and access controls in the host/reverse proxy configuration. A permanent public link comes from the hosting provider after deployment. The executable JAR can also be copied directly to a machine with Java 17+.

## Assignment: meaningful Java conditionals, switch, and loops

The application logic is in `src/main/java/com/atelier/Closet.java`, called by `Api.java`. These are live features used by the website, not a separate console demonstration.

| Requirement | Exact Java location | What it does in the app |
| --- | --- | --- |
| Conditional `if` | `upload()` | Rejects empty, oversized, invalid, or overly large images before storing them. |
| Conditional `if` | `shuffle()` and `save()` | Omits bottoms when a dress is chosen; rejects a dress/bottom conflict and duplicate outfit positions. |
| Switch statement/expression | `slot()` | Routes tops/dresses to the base position, bottoms to bottom, shoes to footwear, and other categories to their meaningful mixer positions. Invalid categories are rejected. |
| `for` loop | `filter()` | Examines each garment and applies category, season, and occasion filters. |
| `for` loops | `shuffle()` | Groups filtered pieces into positions, then visits each position to create a random compatible outfit. |
| `for` loop | `deleteItem()` | Updates saved outfits when a garment is deleted and discards empty outfits. |
| `for` loop | `save()` | Validates every selected piece and enforces one piece per position. |

The website also includes a short expandable Java explanation below the wardrobe. Suggested classroom demo: filter Tops, upload a PNG, shuffle several times, choose a dress to show the conditional behaviour, save a look, restart Java to show persistence, then remove the uploaded garment and show the updated lookbook.

## Project structure

```text
atelier/
  pom.xml                         Maven dependencies and executable JAR build
  Dockerfile                      Two-stage Java container build
  start-windows.cmd               Convenience launcher
  dist/atelier-1.0.0.jar           Prebuilt, tested application
  src/main/java/com/atelier/
    Application.java              Spring Boot entry point
    Api.java                      HTTP routes and friendly error responses
    Closet.java                   Java rules, upload validation, persistence
  src/main/resources/
    application.properties        Port, storage, upload limits
    static/index.html             Browser UI and accessible forms
    static/style.css              Responsive editorial styling
    static/app.js                 Browser interactions with Java API
    static/assets/                Original local garment illustrations
  src/test/java/com/atelier/       Persistence, compatibility, upload/API tests
  data/                           Created at runtime; do not commit
```

## Images and background removal

Samples are original SVG garment illustrations created for this project, not third-party fashion product photos or unreliable remote URLs. They are fully replaceable and deletable. Uploads display using `object-fit: contain`, preserving the garment's proportions and entire photo.

**Automatic background removal is not included.** CSS cannot remove a photo background. Transparent PNG cutouts are supported and recommended. A future enhancement could run a local segmentation model in a separate service, with explicit model installation, resource limits, and a fallback to the original photo. Adding model downloads and an additional runtime would make this classroom project less dependable, so the core app has no such dependency.

## Troubleshooting

- `java` not recognized: install a JDK/JRE 17+ and add its `bin` directory to PATH; reopen the terminal.
- Address already in use: change `PORT` to 8081 and open `http://localhost:8081`.
- Could not read/save data: check write permission and free disk space for `ATELIER_DATA`.
- Image rejected: use a valid PNG/JPEG under 8 MB and 20 megapixels; export or resize your image first.
- Changes disappear on hosting: configure a persistent disk and one server instance.
- Build cannot fetch dependencies: check internet/proxy configuration; the bundled JAR still runs without Maven.
