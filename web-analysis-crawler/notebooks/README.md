# 📊 Analysis Scripts

Helper scripts to process crawl output, add rankings, and summarize localhost-request findings.

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
