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


def send_test_request(operation, data_fmt, outer_elem, inner_elem, puts_per_tx):
    url = "http://localhost:8181/restconf/operations/dsbenchmark:start-test"
    postheaders = {'content-type': 'application/json', 'Accept': 'application/json'}

    test_request_template = '''{
        "input": {
            "operation": "%s",
            "data-format": "%s",
            "outerElements": %d,
            "innerElements": %d,
            "putsPerTx": %d
        }
    }'''
    data = test_request_template % (operation, data_fmt, outer_elem, inner_elem, puts_per_tx)
    r = requests.post(url, data, headers=postheaders, stream=False, auth=('admin', 'admin'))
    result = {u'http-status': r.status_code}
    if r.status_code == 200:
        result = dict(result.items() + json.loads(r.content)['output'].items())
    else:
        print 'Error %s, %s' % (r.status_code, r.content)
    return result


def print_results(run_type, idx, res):
    print '%s #%d: status: %s, listBuildTime %d, testExecTime %d, txOk %d, txError %d' % \
          (run_type, idx, res[u'status'], res[u'listBuildTime'], res[u'execTime'], res[u'txOk'], res[u'txError'])


def run_test(warmup_runs, test_runs, operation, data_fmt, outer_elem, inner_elem, puts_per_tx):
    total_build_time = 0.0
    total_exec_time = 0.0

    print 'Operation: {0:s}, Data Format: {1:s}, Outer/Inner Elements: {2:d}/{3:d}, PutsPerTx {4:d}' \
        .format(operation, data_fmt, outer_elem, inner_elem, puts_per_tx)
    for idx in range(warmup_runs):
        res = send_test_request(operation, data_fmt, outer_elem, inner_elem, puts_per_tx)
        print_results('WARMUP', idx, res)

    for idx in range(test_runs):
        res = send_test_request(operation, data_fmt, outer_elem, inner_elem, puts_per_tx)
        print_results('TEST', idx, res)
        total_build_time += res['listBuildTime']
        total_exec_time += res['execTime']

    return total_build_time / test_runs, total_exec_time / test_runs


if __name__ == "__main__":

    # Test Parameters
    TOTAL_ELEMENTS = 100000
    INNER_ELEMENTS = [1, 10, 100, 1000, 10000, 100000]
    WRITES_PER_TX = [1, 10, 100, 1000, 10000, 100000]
    OPERATIONS = ["PUT", "MERGE", "DELETE"]
    DATA_FORMATS = ["BINDING-AWARE", "BINDING-INDEPENDENT"]

    # Iterations
    WARMUP_RUNS = 10
    TEST_RUNS = 10

    send_clear_request()

    headers = []
    build_times = []
    exec_times = []
    total_times = []

    print send_test_request("PUT", "BINDING-AWARE", 10, 10, 1)

    f = open('test.csv', 'wt')
    try:
        writer = csv.writer(f)

        for fmt in DATA_FORMATS:
            print '***************************************'
            print 'Data format: %s' % fmt
            print '***************************************'
            writer.writerow((('%s:' % fmt),''))

            for oper in OPERATIONS:
                print 'Operation: %s' % oper
                writer.writerow(('', '%s:' % oper))

                for elem in INNER_ELEMENTS:
                    avg_build_time, avg_exec_time = \
                        run_test(WARMUP_RUNS, TEST_RUNS, oper, fmt, TOTAL_ELEMENTS / elem, elem, 1)
                    writer.writerow(('', '', elem, avg_build_time, avg_exec_time, (avg_build_time + avg_exec_time)))

        for fmt in DATA_FORMATS:
            print '***************************************'
            print 'Data format: %s' % fmt
            print '***************************************'
            writer.writerow((('%s:' % fmt),''))

            for oper in OPERATIONS:
                print 'Operation: %s' % oper
                writer.writerow(('', '%s:' % oper))

                for wtx in WRITES_PER_TX:
                    avg_build_time, avg_exec_time = \
                        run_test(WARMUP_RUNS, TEST_RUNS, oper, fmt, TOTAL_ELEMENTS, 1, wtx)
                    writer.writerow(('', '', wtx, avg_build_time, avg_exec_time, (avg_build_time + avg_exec_time)))

    finally:
        f.close()
