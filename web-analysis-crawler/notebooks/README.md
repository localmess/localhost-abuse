## Analysis scripts

- [`202502.csv`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/202502.csv): CrUX ranking from February 2025, used to generate the 100k list of sites.
- [`_convert_csv_list_to_txt.ipynb`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/_convert_csv_list_to_txt.ipynb): Script to convert `202502.csv` into a .txt file with one site per line.
- [`add_ranking.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/add_ranking.py): Script that takes the output from `process_output.py` and adds the correct site ranking.
- [`process_output.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/process_output.py): Main processing script that takes the output from the crawls and checks each site for any localhost requests beeing made.
- [`summarize_results.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/summarize_results.py): Takes the output from `process_output.py` and outputs a summarry to .md files of the main ports used and domains sending localhost requests on the most webpages.
