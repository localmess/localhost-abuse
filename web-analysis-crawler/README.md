# web-analysis-crawler

Based on DuckDuckGo Tracker Radar Collector: https://github.com/duckduckgo/tracker-radar-collector/tree/main
 
## How do I use it?

### Use it from the command line

1. Clone this project locally
2. Install all dependencies (`npm i`)
3. Run the command line tool:

```sh
npm run crawl -- -u "https://example.com" -o ./data/ -v
```

Available options:

- `-o, --output <path>` - (required) output folder where output files will be created
- `-u, --url <url>` - single URL to crawl
- `-i, --input-list <path>` - path to a text file with list of URLs to crawl (each in a separate line)
- `-d, --data-collectors <list>` - comma separated list (e.g `-d 'requests,cookies'`) of data collectors that should be used (all by default)
- `-c, --crawlers <number>` - override the default number of concurrent crawlers (default number is picked based on the number of CPU cores)
- `--reporters <list>` - comma separated list (e.g. `--reporters 'cli,file,html'`) of reporters to be used ('cli' by default)
- `-v, --verbose` - instructs reporters to log additional information (e.g. for "cli" reporter progress bar will not be shown when verbose logging is enabled)
- `-l, --log-path <path>` - instructs reporters where all logs should be written to
- `-f, --force-overwrite` - overwrite existing output files (by default entries with existing output files are skipped)
- `-3, --only-3p` - don't save any first-party data (e.g. requests, API calls for the same eTLD+1 as the main document)
- `-m, --mobile` - emulate a mobile device when crawling
- `-p, --proxy-config <host>` - optional SOCKS proxy host
- `-r, --region-code <region>` - optional 2 letter region code. For metadata only
- `-a, --disable-anti-bot` - disable simple build-in anti bot detection script injected to every frame
- `--chromium-version <version_number>` - use custom version of Chromium (e.g. "843427") instead of using the default
- `--selenium-hub <url>` - If provided, browsers will be requested from selenium hub instead of spawning local processes (e.g. `--selenium-hub http://my-selenium-hub-host:4444`).
- `--config <path>` - path to a config file that allows to set all the above settings (and more). Note that CLI flags have a higher priority than settings passed via config. You can find a sample config file in `tests/cli/sampleConfig.json`.
- `--autoconsent-action <action>` - automatic autoconsent action (requires the `cookiepopups` collector). Possible values: optIn, optOut

Example commands used for the data collection are found in [`commands.txt`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/commands.txt).

## Additional Content

- [`input`](https://github.com/localmess/localhost-abuse/tree/main/web-analysis-crawler/input) folder contains the input files used for the crawls. [`list_100k`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/input/list_100k.txt) is the list of 100k sites used for the crawls across EU and US regions.
- [`notebooks`](https://github.com/localmess/localhost-abuse/tree/main/web-analysis-crawler/notebooks) folder contains the scripts used to process the crawl output and the CrUX ranking.