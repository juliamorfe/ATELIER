# ATELIER — Virtual Closet

A complete Java web application for a real-life problem: choosing outfits from clothes you already own. Organize a wardrobe, mix individual pieces, shuffle a look, upload your own garment photos, and keep favourite combinations. Its neutral editorial grid and outfit mixer are inspired by the supplied fashion UI references. The original HTML and console Java proposal informed the feature set; this project replaces them with one integrated application.

## Start here

ATELIER now has email/password registration, login/logout, and private wardrobes for each user. Accounts, saved looks, and uploaded images are stored in a database. The existing wardrobe filters, outfit studio, shuffle, and upload validation are preserved.

See [SETUP.md](SETUP.md) for local startup, database design, migration notes, and the Railway PostgreSQL deployment checklist. Railway resources have not been provisioned by this code change.

```sh
mvn clean package
java -jar target/atelier-1.0.0.jar
```

Open http://localhost:8080 and create an account. Local development uses persistent H2 storage; the production profile uses PostgreSQL. Every new account receives its own sample collection.

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
  SETUP.md                        Local and Railway deployment guide
  railway.json                    Railway build and health check
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
  data/                           Local H2 database; do not commit
```

## Images and background removal

Samples are original SVG garment illustrations created for this project, not third-party fashion product photos or unreliable remote URLs. They are fully replaceable and deletable. Uploads display using `object-fit: contain`, preserving the garment's proportions and entire photo.

**Automatic background removal is not included.** CSS cannot remove a photo background. Transparent PNG cutouts are supported and recommended. A future enhancement could run a local segmentation model in a separate service, with explicit model installation, resource limits, and a fallback to the original photo. Adding model downloads and an additional runtime would make this classroom project less dependable, so the core app has no such dependency.

## Troubleshooting

See [SETUP.md](SETUP.md) for setup and storage details. Check Java/Maven availability and database connection variables if startup fails. Use valid PNG/JPEG images under 8 MB and 20 megapixels.
