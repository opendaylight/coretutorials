#!/usr/bin/python
__author__ = "Jan Medved"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "New-style BSD"
__email__ = "jmedved@cisco.com"

import requests
import json
import csv


def send_clear_request():
    url = "http://localhost:8181/restconf/operations/dsbenchmark:cleanup-store"

    r = requests.post(url, stream=False, auth=('admin', 'admin'))
    print r.status_code


def send_test_request(operation, data_fmt, outer_elem, inner_elem, puts_per_tx, thread_count):
    url = "http://localhost:8181/restconf/operations/dsbenchmark:start-test"
    postheaders = {'content-type': 'application/json', 'Accept': 'application/json'}

    test_request_template = '''{
        "input": {
            "operation": "%s",
            "data-format": "%s",
            "outerElements": %d,
            "innerElements": %d,
            "putsPerTx": %d,
            "threadCount" : %d
        }
    }'''
    data = test_request_template % (operation, data_fmt, outer_elem, inner_elem, puts_per_tx, thread_count)
    r = requests.post(url, data, headers=postheaders, stream=False, auth=('admin', 'admin'))
    result = {u'http-status': r.status_code}
    if r.status_code == 200:
        result = dict(result.items() + json.loads(r.content)['output'].items())
    else:
        print 'Error %s, %s' % (r.status_code, r.content)
    return result


def print_results(run_type, idx, res):
    print '%s #%d: status: %s, listBuildTime %d, testExecTime %d, txOk %d' % \
          (run_type, idx, res[u'status'], res[u'listBuildTime'], res[u'execTime'], res[u'txOk'])


def run_test(warmup_runs, test_runs, operation, data_fmt, outer_elem, inner_elem, puts_per_tx, thread_count):
    total_build_time = 0.0
    total_exec_time = 0.0

    print 'Operation: {0:s}, Data Format: {1:s}, Outer/Inner Elements: {2:d}/{3:d}, PutsPerTx' \
        .format(operation, data_fmt, outer_elem, inner_elem, puts_per_tx)
    for idx in range(warmup_runs):
        res = send_test_request(operation, data_fmt, outer_elem, inner_elem, puts_per_tx, thread_count)
        print_results('WARMUP', idx, res)

    for idx in range(test_runs):
        res = send_test_request(operation, data_fmt, outer_elem, inner_elem, puts_per_tx, thread_count)
        print_results('TEST', idx, res)
        total_build_time += res['listBuildTime']
        total_exec_time += res['execTime']

    return total_build_time / test_runs, total_exec_time / test_runs


if __name__ == "__main__":

    # Test Parameters
    TOTAL_ELEMENTS = 100000
    INNER_ELEMENTS = [1, 10, 100, 1000, 10000, 100000]
    PUTS_PER_TX = [1, 10, 100, 1000, 10000, 1000000]
    OPERATIONS = ["PUT", "MERGE", "DELETE"]
    DATA_FORMATS = ["BINDING-AWARE", "BINDING-INDEPENDENT"]

    # Iterations
    WARMUP_RUNS = 10
    TEST_RUNS = 10
    THREAD_COUNT = 5

    send_clear_request()

    headers = []
    build_times = []
    exec_times = []
    total_times = []

#    print send_test_request("PUT", "BINDING-AWARE", 10, 10, 1, THREAD_COUNT)

    for fmt in DATA_FORMATS:
        for oper in OPERATIONS:
            for elem in INNER_ELEMENTS:
                avg_build_time, avg_exec_time = \
                    run_test(WARMUP_RUNS, TEST_RUNS, oper, fmt, TOTAL_ELEMENTS / elem, elem, 1, THREAD_COUNT)
                headers.append(elem)
                build_times.append(avg_build_time)
                exec_times.append(avg_exec_time)
                total_times.append(avg_build_time + avg_exec_time)

    print build_times
    print exec_times
    print total_times

    f = open('test.csv', 'wt')
    try:
        writer = csv.writer(f)
        for i in range(len(build_times)):
            writer.writerow((headers[i], build_times[i], exec_times[i], build_times[i] + exec_times[i]))
    finally:
        f.close()

