## Analysis scripts

- [`process_output.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/process_output.py): Main processing script that takes the output from the crawls and checks each site for any localhost requests beeing made.
- [`add_ranking.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/add_ranking.py): Script that takes the output from `process_output.py` and adds the correct site ranking.
- [`summarize_results.py`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/summarize_results.py): Takes the output from `process_output.py` and outputs a summarry to .md files of the main ports used and domains sending localhost requests on the most webpages.
- [`202502.csv`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/202502.csv): CrUX ranking from February 2025, used to generate the 100k list of sites. Obtained from the [`crux-top-lists`](https://github.com/zakird/crux-top-lists/blob/main/data/global/202502.csv.gz) Github repository by Zakir Durumeric and David Adrian.
- [`convert_csv_list_to_txt.ipynb`](https://github.com/localmess/localhost-abuse/blob/main/web-analysis-crawler/notebooks/convert_csv_list_to_txt.ipynb): Script to convert `202502.csv` into a .txt file with one site per line.

## How to use

1. Change the `Input and output params` in the `process_output.py` file to point to the output data of the specific crawl that needs to be processed.</li>
2. Run `python process_output.py`</li>
3. Change the `Input and output params` in both `add_ranking.py` and `summarize_results.py` to point to the output data of `process_output.py`</li>
4. Run `python add_ranking.py` and `python summarize_results.py`</li>