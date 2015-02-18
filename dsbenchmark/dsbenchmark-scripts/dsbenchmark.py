#!/usr/bin/python
__author__ = "Jan Medved"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "New-style BSD"
__email__ = "jmedved@cisco.com"

import requests
import json
import csv


def send_clear_request():
    """
    Sends a clear request to the dsbenchmark app. A clear will clear the test-exec data store
    and clear the 'test-executing' flag.
    :return: None
    """
    url = "http://localhost:8181/restconf/operations/dsbenchmark:cleanup-store"

    r = requests.post(url, stream=False, auth=('admin', 'admin'))
    print r.status_code


def send_test_request(operation, data_fmt, outer_elem, inner_elem, ops_per_tx):
    """
    Sends a request to the dsbenchmark app to start a data store benchmark test run.
    The dsbenchmark app will perform the requested benchmark test and return measured
    transaction times
    :param operation: PUT, MERGE or DELETE
    :param data_fmt: BINDING-AWARE or BINDING-INDEPENDENT
    :param outer_elem: Number of elements in the outer list
    :param inner_elem: Number of elements in the inner list
    :param ops_per_tx: Number of operations (PUTs, MERGEs or DELETEs) on each transaction
    :return:
    """
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
    data = test_request_template % (operation, data_fmt, outer_elem, inner_elem, ops_per_tx)
    r = requests.post(url, data, headers=postheaders, stream=False, auth=('admin', 'admin'))
    result = {u'http-status': r.status_code}
    if r.status_code == 200:
        result = dict(result.items() + json.loads(r.content)['output'].items())
    else:
        print 'Error %s, %s' % (r.status_code, r.content)
    return result


def print_results(run_type, idx, res):
    """
    Prints results from a dsbenchmakr test run to console
    :param run_type: String parameter that can be used to identify the type of the
                     test run (e.g. WARMUP or TEST)
    :param idx: Index of the test run
    :param res: Parsed json (disctionary) that was returned from a dsbenchmark
                test run
    :return: None
    """
    print '%s #%d: status: %s, listBuildTime %d, testExecTime %d, txOk %d, txError %d' % \
          (run_type, idx, res[u'status'], res[u'listBuildTime'], res[u'execTime'], res[u'txOk'], res[u'txError'])


def run_test(warmup_runs, test_runs, operation, data_fmt, outer_elem, inner_elem, ops_per_tx):
    """
    Execute a benchmark test. Performs the JVM 'wamrup' before the test, runs
    the specified number of dsbenchmark test runs and computes the average time
    for building the test data (a list of lists) and the average time for the
    execution of the test.
    :param warmup_runs: # of warmup runs
    :param test_runs: # of test runs
    :param operation: PUT, MERGE or DELETE
    :param data_fmt: BINDING-AWARE or BINDING-INDEPENDENT
    :param outer_elem: Number of elements in the outer list
    :param inner_elem: Number of elements in the inner list
    :param ops_per_tx: Number of operations (PUTs, MERGEs or DELETEs) on each transaction
    :return: average build time AND average test execution time
    """
    total_build_time = 0.0
    total_exec_time = 0.0

    print 'Operation: {0:s}, Data Format: {1:s}, Outer/Inner Elements: {2:d}/{3:d}, PutsPerTx {4:d}' \
        .format(operation, data_fmt, outer_elem, inner_elem, ops_per_tx)
    for idx in range(warmup_runs):
        res = send_test_request(operation, data_fmt, outer_elem, inner_elem, ops_per_tx)
        print_results('WARMUP', idx, res)

    for idx in range(test_runs):
        res = send_test_request(operation, data_fmt, outer_elem, inner_elem, ops_per_tx)
        print_results('TEST', idx, res)
        total_build_time += res['listBuildTime']
        total_exec_time += res['execTime']

    return total_build_time / test_runs, total_exec_time / test_runs


if __name__ == "__main__":

    # Test Parameters
    TOTAL_ELEMENTS = 100000
    INNER_ELEMENTS = [1, 10, 100, 1000, 10000, 100000]
    OPS_PER_TX = [1, 10, 100, 1000, 10000, 100000]
    OPERATIONS = ["PUT", "MERGE", "DELETE"]
    DATA_FORMATS = ["BINDING-AWARE", "BINDING-INDEPENDENT"]

    # Iterations
    WARMUP_RUNS = 10
    TEST_RUNS = 10

    # Clean up any data that may be present in the data store
    send_clear_request()

    headers = []
    build_times = []
    exec_times = []
    total_times = []

    # Run the benchmark tests and collect data in a csv file for import into a graphing software
    f = open('test.csv', 'wt')
    try:
        writer = csv.writer(f)

        # Iterate over all data formats, operation types, and different list-of-lists layouts; always
        # use a single operation in each transaction
        for fmt in DATA_FORMATS:
            print '***************************************'
            print 'Data format: %s' % fmt
            print '***************************************'
            writer.writerow((('%s:' % fmt), ''))

            for oper in OPERATIONS:
                print 'Operation: %s' % oper
                writer.writerow(('', '%s:' % oper))

                for elem in INNER_ELEMENTS:
                    avg_build_time, avg_exec_time = \
                        run_test(WARMUP_RUNS, TEST_RUNS, oper, fmt, TOTAL_ELEMENTS / elem, elem, 1)
                    writer.writerow(('', '', elem, avg_build_time, avg_exec_time, (avg_build_time + avg_exec_time)))

        # Iterate over all data formats, operation types, and operations-per-transaction; always
        # use a list of lists where the inner list has one parameter
        for fmt in DATA_FORMATS:
            print '***************************************'
            print 'Data format: %s' % fmt
            print '***************************************'
            writer.writerow((('%s:' % fmt), ''))

            for oper in OPERATIONS:
                print 'Operation: %s' % oper
                writer.writerow(('', '%s:' % oper))

                for wtx in OPS_PER_TX:
                    avg_build_time, avg_exec_time = \
                        run_test(WARMUP_RUNS, TEST_RUNS, oper, fmt, TOTAL_ELEMENTS, 1, wtx)
                    writer.writerow(('', '', wtx, avg_build_time, avg_exec_time, (avg_build_time + avg_exec_time)))

    finally:
        f.close()
