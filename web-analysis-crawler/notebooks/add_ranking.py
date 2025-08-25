import json
import glob
import re
import csv
import socket
from tld import get_fld
from urllib.parse import urlparse
from functools import lru_cache

from concurrent.futures import ProcessPoolExecutor, as_completed, TimeoutError as concurrentTimeoutError
from tqdm import tqdm

rank_file_path = "202502.csv"
outputSize = 100000

# results_folder = "results/ranked_"
results_folder = "_new_results/ranked_"

# location = "frankfurt"
location = "new_york"

# version = "_100k"
# version = "_desktop"
# version = "_recrawl"
# version = "_ios"
version = "_post"

# input_folder = "new_york_data/data\\"
# input_folder = "frankfurt_data/data\\"
# input_file = "results/requests_output_" + location + version + ".csv"
# output = "requests_output"
# input_file = "results/webRTC_output_" + location + version + ".csv"
# output = "webRTC_output"

input_file = "_new_results/requests_output_" + location + version + ".csv"
output = "requests_output"
# input_file = "_new_results/webRTC_output_" + location + version + ".csv"
# output = "webRTC_output"

TIMEOUT_SECONDS = 30
ranking = {}

@lru_cache(maxsize=100_000)
def get_rank(tested_url):
    if tested_url.endswith("/"):
        tested_url = tested_url[:-1]
    return ranking.get(tested_url, "?")

def analyze_dict(row_dict : dict):
    tested_url = row_dict.get("tested_url", "")
    if tested_url == "":
        print("Error")

    row_dict["rank"] = get_rank(tested_url)

    return row_dict

def main():
    crawl_count = 0

    with open(rank_file_path) as file:
        reader = csv.reader(file)
        count = 0

        for row in reader:
            ranking[row[0]] = row[1]
            count += 1
            if count > outputSize:
                break

    with open(input_file, mode='r', newline='', encoding='utf-8') as file:
        reader = csv.DictReader(file)
        data = [row for row in reader]

    with open(results_folder + output + "_" + location + version + ".csv", "w", newline="") as f1:
        WRITER_REQS = None

        for row in data:

            crawl_count += 1
            if not crawl_count % 100:
                print(crawl_count)

            result = analyze_dict(row)

            if result:
                if WRITER_REQS is None:
                    WRITER_REQS = csv.DictWriter(f1, fieldnames=result.keys())
                    WRITER_REQS.writeheader()
                WRITER_REQS.writerow(result)

if __name__ == "__main__":
    main()