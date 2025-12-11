# 📊 Analysis Scripts

---

## 📁 Files

- [`process_output.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/process_output.py): Main processor; reads crawl output and checks each site for localhost requests.
- [`add_ranking.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/add_ranking.py): Adds site rankings to the processed output from `process_output.py`.
- [`summarize_results.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/summarize_results.py): Produces `.md` summaries of top ports and domains sending localhost requests.
- [`202502.csv`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/202502.csv): CrUX ranking (Feb 2025) used to generate the 100k list of sites; sourced from [`crux-top-lists`](https://github.com/zakird/crux-top-lists/blob/main/data/global/202502.csv.gz).
- [`convert_csv_list_to_txt.ipynb`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/convert_csv_list_to_txt.ipynb): Converts `202502.csv` into a `.txt` file (one site per line).

---

## 🛠️ Workflow

1. Set the **Input and output params** in [`process_output.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/process_output.py) to your crawl output.
2. Run `python process_output.py`.
3. Update **Input and output params** in [`add_ranking.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/add_ranking.py) and [`summarize_results.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/summarize_results.py) to point to the `process_output.py` output.
4. Run `python add_ranking.py` and `python summarize_results.py`.
