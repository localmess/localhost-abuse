# 📊 Crawl Data and Analysis Scripts

Helper scripts to process crawl output, add rankings, and summarize localhost-request findings, as well as the obtained summaries from the original crawl data.

---

## ⚙️ Requirements

- python packages:
  - pandas
  - tld
  - tqdm

---

## 📁 Folder Structure

```
.
├── crawl_result_csvs.tar.gz      # Summaries from the original crawl data
├── process_output.py             # Main processor: checks crawl output for localhost requests
├── add_ranking.py                # Adds site rankings to process_output.py output
├── summarize_results.py          # Summarizes top ports/domains to .md files
├── 202502.csv                    # CrUX ranking (Feb 2025) used for the 100k list
├── convert_csv_list_to_txt.ipynb # Converts 202502.csv to a one-site-per-line .txt list
└── README.md
```

References:
- [`202502.csv`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/202502.csv) (CrUX Feb 2025; source: [`crux-top-lists`](https://github.com/zakird/crux-top-lists/blob/main/data/global/202502.csv.gz))


---

## 🛠️ Workflow

1. Run `python process_output.py` with the following arguments:
  - `--input-folder`: Folder containing input JSON files. (crawler output)
  - `--output-folder`: Folder where output CSV files will be written.
  - `--location"` and `--version"`: Location and version label used in naming output files. (Optional)
2. Run `python add_ranking.py` with the following arguments:
  - `--input-file`: CSV file to add ranking to (output from `process_output.py`).
  - `--rank-file`: CSV file containing ranking information, defaults to 202502.csv in the same folder as this script. (Optional)
  - `--version"`: Version label used in naming output files, defaults to "ranked". (Optional)
3. Run `python summarize_results.py` with the following arguments:
  - `--input-webrtc`: CSV file with WebRTC output from `process_output.py`.
  - `--input-requests`: CSV file with requests output from `process_output.py`.
  - `--output-folder`: Folder where output CSV files will be written.
  - `--location"` and `--version"`: Location and version label used in naming output files. (Optional)


---

## 🗄️ Crawl Data

The table below lists the crawls performed for the paper.

Crawl name | Location* | Configuration | Consent mode | Date | Num. of websites | URL list**
-- | -- | -- | -- | -- | -- | --
frankfurt_android_data.tar.gz | EU | Android | Accept | April 2025 | 100,000 | list_100k.txt
new_york_android_data.tar.gz | US | Android | Accept | April 2025 | 100,000 | list_100k.txt
frankfurt_desktop_data.tar.gz | EU | Desktop (Windows) | Accept | May 2025 | 100,000 | list_100k.txt
frankfurt_android_recrawl_data.tar.gz | EU | Android | Reject | May 2025 | 16,831*** | detected_urls_frankfurt.txt
new_york_desktop_data.tar.gz | US | Desktop (Windows) | Accept | May 2025 | 100,000 | list_100k.txt
new_york_android_recrawl_data.tar.gz | US | Android | Reject | May 2025 | 18,431*** | detected_urls_new_york.txt
frankfurt_ios_data.tar.gz | EU | iOS | Accept | June 2025 | 100,000 | list_100k.txt
frankfurt_android_post_data.tar.gz | EU | Android | Accept | June 2025 | 100,000 | list_100k.txt
new_york_android_post_data.tar.gz | US | Android | Accept | June 2025 | 100,000 | list_100k.txt
new_york_ios_data.tar.gz | US | iOS | Accept | June 2025 | 100,000 | list_100k.txt


*: EU: Frankfurt; US: New York

**: URL lists used in the crawls can be found in the `web-analysis-crawler/input/` folder

***: Only targeted websites where we observed localhost communications