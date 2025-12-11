# 📊 Analysis Scripts

Helper scripts to process crawl output, add rankings, and summarize localhost-request findings.

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

1. Set the **Input and output params** in [`process_output.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/process_output.py) to your crawl output.
2. Run `python process_output.py`.
3. Update **Input and output params** in [`add_ranking.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/add_ranking.py) and [`summarize_results.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/summarize_results.py) to point to the `process_output.py` output.
4. Run `python add_ranking.py` and `python summarize_results.py`.
