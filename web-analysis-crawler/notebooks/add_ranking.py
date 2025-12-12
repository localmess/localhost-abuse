import json
import glob
import re
import csv
import socket
from tld import get_fld
from urllib.parse import urlparse
from functools import lru_cache
import argparse

from concurrent.futures import ProcessPoolExecutor, as_completed, TimeoutError as concurrentTimeoutError
from tqdm import tqdm

# rank_file_path = "202502.csv"
outputSize = 100000

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

def parse_args():
    parser = argparse.ArgumentParser(description="Process JSON crawl files.")

    parser.add_argument(
        "--input-file",
        required=True,
        help="CSV file to add ranking to"
    )

    parser.add_argument(
        "--rank-file",
        default="202502.csv",
        help="CSV file containing ranking information, defaults to 202502.csv in the same folder as this script"
    )

    parser.add_argument(
        "--version",
        default="ranked",
        help="Version label used in naming output files"
    )

    return parser.parse_args()

def main():
    args = parse_args()

    input_file = args.input_file
    rank_file_path = args.rank_file
    version = args.version

    output_file = input_file[:-4] + "_" + version + ".csv"

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

    with open(output_file, "w", newline="") as f1:
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