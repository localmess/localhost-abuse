# 🕸️ Web Analysis Crawler

Our web crawler, built on [duckduckgo/tracker-radar-collector](https://github.com/duckduckgo/tracker-radar-collector/tree/main), adds WebSocket frame capture and WebRTC function-call interception.

---

## ⚙️ Requirements

- Node.js and npm installed locally

---

## 🙌 Our Additions

- Capture of WebSocket frames during crawls
- Interception of WebRTC-related function calls

---

## 📁 Folder Structure

```
.
├── crawler.js              # Default crawler (Android/desktop with consent)
├── crawlerNoConsent.js     # Android variant without consent (rename to crawler.js to use)
├── crawlerIOS.js           # iOS variant with consent (rename to crawler.js to use)
├── commands.txt            # Example crawl commands
├── input/                  # URL lists (e.g., list_100k.txt)
├── notebooks/              # Post-processing and CrUX ranking scripts
└── README.md
```

---

## 🛠️ Setup

1. Clone this project locally.
2. Install dependencies:

   ```sh
   npm i
   ```

---

## 🚀 Usage

Quick test run:

```sh
npm run crawl -- -u "https://example.com" -o ./data/ -v
```

Reference crawl commands: [`commands.txt`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/commands.txt).

For full crawls:

1. Choose the crawler file:
   - **Android (with consent)**: use [`crawler.js`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/crawler.js) as-is.
   - **Android (without consent)**: copy or rename [`crawlerNoConsent.js`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/crawlerNoConsent.js) to `crawler.js`, replacing the default.
   - **Desktop (with consent)**: use [`crawler.js`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/crawler.js) as-is.
   - **iOS (with consent)**: copy or rename [`crawlerIOS.js`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/crawlerIOS.js) to `crawler.js`, replacing the default.

2. Run a crawl:
   - Android/iOS (with/without consent):
     ```sh
     npm run crawl -- -i ./input/list_100k.txt -o ./data/ -f -m -l ./temp_data/ --reporters cli,file
     ```
   - Desktop (with consent):
     ```sh
     npm run crawl -- -i ./input/list_100k.txt -o ./data/ -f -l ./temp_data/ --reporters cli,file
     ```

---

## 📌 CLI Options

- `-o, --output <path>`: (required) output folder where output files will be created
- `-u, --url <url>`: single URL to crawl
- `-i, --input-list <path>`: path to a text file with list of URLs to crawl (each in a separate line)
- `-c, --crawlers <number>`: override the default number of concurrent crawlers (default picked by CPU cores)
- `--reporters <list>`: comma-separated list (e.g., `--reporters 'cli,file,html'`) of reporters to be used (`cli` by default)
- `-v, --verbose`: reporters log more info (for `cli`, progress bar is hidden when verbose)
- `-l, --log-path <path>`: location for all logs
- `-f, --force-overwrite`: overwrite existing output files (otherwise skips existing)
- `-m, --mobile`: emulate a mobile device when crawling

---

## 📂 Additional Content

- [`input/`](https://github.com/localmess/localhost-abuse/tree/main/web-analysis-crawler/input) contains the crawl input files. [`input/list_100k.txt`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/input/list_100k.txt) is the 100k-site list used across EU and US regions.
- [`notebooks/`](https://github.com/localmess/localhost-abuse/tree/main/web-analysis-crawler/notebooks) contains scripts to process crawl output and the CrUX ranking.
