import pandas as pd
import json

# results_input__folder = "results/"
results_input__folder = "_new_results/"

# location = "frankfurt"
location = "new_york"

# version = "_100k"
# version = "_desktop"
# version = "_recrawl"
# version = "_ios"
version = "_post"

# output_folder = "results/"
output_folder = "_new_results/"

webrtc_csv_path = results_input__folder + 'webRTC_output_' + location + version + '.csv'
requests_csv_path = results_input__folder + 'requests_output_' + location + version + '.csv'
# resolved_csv_path = results_input__folder + 'resolved_requests_output_' + location + version + '.csv'
output_json_path = output_folder + 'summary_output_' + location + version + '.json'
markdown_output_path = output_folder + 'summary_report_' + location + version + '.md'

min_urls_threshold = 5

# Load CSVs
webrtc_df = pd.read_csv(webrtc_csv_path)
requests_df = pd.read_csv(requests_csv_path)
# resolved_df = pd.read_csv(resolved_csv_path)

# Filter requests by protocol
http_df = requests_df[requests_df['protocol'].str.lower() == 'http']
websocket_df = requests_df[requests_df['protocol'].str.lower() == 'websocket']

# Helper function
def get_summary(df, label):
    unique_final_urls = df['final_url'].nunique()

    domain_final_url_counts = df.groupby('script_domain')['final_url'].nunique()
    domain_port_counts = df.groupby('script_domain')['port_num'].nunique()

    # Filter by threshold for unique final URLs per domain
    filtered_domains = domain_final_url_counts[domain_final_url_counts >= min_urls_threshold].index
    filtered_port_counts = domain_port_counts.loc[filtered_domains].sort_values(ascending=False).head(10)

    # Top script domain per port based on unique final_urls
    grouped = df.groupby(['port_num', 'script_domain'])['final_url'].nunique().reset_index(name='unique_final_urls_count')

    # No filter applied anymore
    top_script_domain_per_port = (
        grouped.sort_values('unique_final_urls_count', ascending=False)
        .groupby('port_num')
        .first()
        .sort_index()
    )

    # Convert the DataFrame to a serializable format
    top_script_domain_per_port_dict = top_script_domain_per_port.reset_index().to_dict(orient='records')

    summary = {
        "unique_final_url_count": unique_final_urls,
        "top_script_domains_by_unique_ports": domain_port_counts.sort_values(ascending=False).head(10),
        "top_script_domains_by_unique_ports_min_urls": filtered_port_counts,
        "top_script_domains_by_unique_final_urls": domain_final_url_counts.sort_values(ascending=False).head(10),
        "top_ports_by_unique_final_urls": df.groupby('port_num')['final_url'].nunique().sort_values(ascending=False),
        "top_script_domain_per_port": top_script_domain_per_port_dict  # Use the list of dictionaries
    }

    # Markdown
    markdown = f"## {label} Summary\n"
    markdown += f"**Total Unique Final URLs:** {unique_final_urls}\n\n"

    for key, series in summary.items():
        if key == "top_script_domain_per_port":
            markdown += f"### Top Script Domain Per Port (Based on Unique Final URLs)\n"
            markdown += "| Port | Script Domain | Unique Final URLs Count |\n|------|----------------|-------------------------|\n"
            for row in series:
                markdown += f"| {row['port_num']} | {row['script_domain']} | {row['unique_final_urls_count']} |\n"
            markdown += "\n"
            continue

        if isinstance(series, pd.Series):
            title = key.replace("_", " ").title()
            markdown += f"### {title}\n"
            markdown += "| Key | Count |\n|-----|-------|\n"
            for idx, val in series.items():
                markdown += f"| {idx} | {val} |\n"
            markdown += "\n"

    return summary, markdown

# Summaries
webrtc_summary, webrtc_md = get_summary(webrtc_df, "WebRTC")
http_summary, http_md = get_summary(http_df, "HTTP Requests")
ws_summary, ws_md = get_summary(websocket_df, "WebSocket Requests")
# resolved_summary, resolved_md = get_summary(resolved_df, "Resolved Requests")

# JSON output
summary_dict = {
    "webrtc": {k: v.to_dict() if isinstance(v, pd.Series) else v for k, v in webrtc_summary.items()},
    "http_requests": {k: v.to_dict() if isinstance(v, pd.Series) else v for k, v in http_summary.items()},
    "websocket_requests": {k: v.to_dict() if isinstance(v, pd.Series) else v for k, v in ws_summary.items()},
    # "resolved_requests:": {k: v.to_dict() if isinstance(v, pd.Series) else v for k, v in resolved_summary.items()}
}

with open(output_json_path, 'w') as f:
    json.dump(summary_dict, f, indent=4)

# Markdown output
with open(markdown_output_path, 'w') as f:
    f.write("# Summary Report\n\n")
    f.write(webrtc_md)
    f.write(http_md)
    f.write(ws_md)
    # f.write(resolved_md)

print(f"Summary written to:\n- {output_json_path}\n- {markdown_output_path}")
