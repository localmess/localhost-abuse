import pandas as pd

# version = "_frankfurt_100k"
# version = "_new_york_100k"

# version = "_frankfurt_recrawl"
version = "_new_york_recrawl"

results_folder = "results/"

webRTC_file = "results/Facebook_sites" + version + ".csv"
requests_file = "results/Yandex_sites" + version + ".csv"
# resolved_file = "results/ranked_resolved_requests_output" + version + ".csv"

# version_consent = "_frankfurt_100k"
version_consent = "_new_york_100k"
webRTC_file_consent = "results/Facebook_sites" + version_consent + ".csv"
requests_file_consent = "results/Yandex_sites" + version_consent + ".csv"

# version_result = "_frankfurt"
version_result = "_new_york"

def main():
    webRTC_df = pd.read_csv(webRTC_file)
    requests_df = pd.read_csv(requests_file)
    webRTC_consent_df = pd.read_csv(webRTC_file_consent)
    requests_consent_df = pd.read_csv(requests_file_consent)

    webRTC_consent_df['sends_before_consent'] = webRTC_consent_df['final_url'].isin(webRTC_df['final_url'])
    requests_consent_df['sends_before_consent'] = requests_consent_df['final_url'].isin(requests_df['final_url'])

    webRTC_consent_df.to_csv("results/Facebook_sites" + version_result + ".csv", index=False)
    requests_consent_df.to_csv("results/Yandex_sites" + version_result + ".csv", index=False)

if __name__ == "__main__":
    main()