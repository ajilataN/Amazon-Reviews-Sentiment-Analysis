import os
import matplotlib.pyplot as plt
import numpy as np

def read_file(file_path):
    with open(file_path, 'r') as file:
        lines = file.readlines()
        reviews_per_second = [int(line.split(":")[1].strip()) for line in lines]
        return reviews_per_second

def compute_statistics(data):
    average = np.mean(data)
    median = np.median(data)
    minimum = np.min(data)
    maximum = np.max(data)
    std_dev = np.std(data)
    return average, median, minimum, maximum, std_dev

def plot_results(sequential_data, parallel_data, distributed_data):
    plt.figure(figsize=(10, 6))
    plt.plot(sequential_data, label='Sequential', marker='o')
    plt.plot(parallel_data, label='Parallel', marker='o')
    plt.plot(distributed_data, label='Distributed', marker='o')
    plt.xlabel('Time (seconds)')
    plt.ylabel('Reviews processed per second')
    plt.title('Reviews Processed per Second Over Time')
    plt.legend()
    plt.grid(True)
    plt.show()

if __name__ == "__main__":
    
    script_dir = os.path.dirname(os.path.abspath(__file__))
    sequential_file = os.path.join(script_dir, "sequential_review_counts.txt")
    parallel_file = os.path.join(script_dir, "parallel_review_counts.txt")
    distributed_file = os.path.join(script_dir, "distributed_review_counts.txt")

    sequential_data = read_file(sequential_file)
    parallel_data = read_file(parallel_file)
    distributed_data = read_file(distributed_file)

    # Compute statistics
    seq_avg, seq_median, seq_min, seq_max, seq_std = compute_statistics(sequential_data)
    par_avg, par_median, par_min, par_max, par_std = compute_statistics(parallel_data)
    dist_avg, dist_median, dist_min, dist_max, dist_std = compute_statistics(distributed_data)

    # Print statistics
    print("Sequential Data Statistics:")
    print(f"Average: {seq_avg}, Median: {seq_median}, Min: {seq_min}, Max: {seq_max}, Std Dev: {seq_std}")
    
    print("\nParallel Data Statistics:")
    print(f"Average: {par_avg}, Median: {par_median}, Min: {par_min}, Max: {par_max}, Std Dev: {par_std}")

    print("\nDistributed Data Statistics:")
    print(f"Average: {dist_avg}, Median: {dist_median}, Min: {dist_min}, Max: {dist_max}, Std Dev: {dist_std}")

    plot_results(sequential_data, parallel_data, distributed_data)
