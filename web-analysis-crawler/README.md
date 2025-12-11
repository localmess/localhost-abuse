# web-analysis-crawler

Based on DuckDuckGo Tracker Radar Collector: https://github.com/duckduckgo/tracker-radar-collector/tree/main
Main additions are the collection of WebSocket frames as well as intercepting WebRTC related function calls.
 
## How to use

1. Clone this project locally
2. Install all dependencies (`npm i`)
3. Run the command line tool to test:

```sh
npm run crawl -- -u "https://example.com" -o ./data/ -v
```

Commands used for the data collection are found in [`commands.txt`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/commands.txt).
- Android crawl with consent:
    - Use the default provided [`crawler.js`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/crawler.js) file.
    - Run `npm run crawl -- -i ./input/list_100k.txt -o ./data/ -f -m -l ./temp_data/ --reporters cli,file`
- Android crawl without consent:
    - Rename [`crawlerNoConsent.js`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/crawlerNoConsent.js) to `crawler.js`
    - Run `npm run crawl -- -i ./input/list_100k.txt -o ./data/ -f -m -l ./temp_data/ --reporters cli,file`
- Desktop crawl with consent:
    - Use the default provided [`crawler.js`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/crawler.js) file.
    - Run `npm run crawl -- -i ./input/list_100k.txt -o ./data/ -f -l ./temp_data/ --reporters cli,file`
- iOS crawl with consent:
    - Rename [`crawlerIOS.js`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/crawlerIOS.js) to `crawler.js`
    - Run `npm run crawl -- -i ./input/list_100k.txt -o ./data/ -f -m -l ./temp_data/ --reporters cli,file`

Available options:

- `-o, --output <path>` - (required) output folder where output files will be created
- `-u, --url <url>` - single URL to crawl
- `-i, --input-list <path>` - path to a text file with list of URLs to crawl (each in a separate line)
- `-c, --crawlers <number>` - override the default number of concurrent crawlers (default number is picked based on the number of CPU cores)
- `--reporters <list>` - comma separated list (e.g. `--reporters 'cli,file,html'`) of reporters to be used ('cli' by default)
- `-v, --verbose` - instructs reporters to log additional information (e.g. for "cli" reporter progress bar will not be shown when verbose logging is enabled)
- `-l, --log-path <path>` - instructs reporters where all logs should be written to
- `-f, --force-overwrite` - overwrite existing output files (by default entries with existing output files are skipped)
- `-m, --mobile` - emulate a mobile device when crawling

## Additional Content

- [`input`](https://github.com/localmess/localhost-abuse/tree/main/web-analysis-crawler/input) folder contains the input files used for the crawls. [`list_100k`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/input/list_100k.txt) is the list of 100k sites used for the crawls across EU and US regions.
- [`notebooks`](https://github.com/localmess/localhost-abuse/tree/main/web-analysis-crawler/notebooks) folder contains the scripts used to process the crawl output and the CrUX ranking.