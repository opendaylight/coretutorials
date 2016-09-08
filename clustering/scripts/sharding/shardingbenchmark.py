#!/usr/bin/python

import argparse
import requests
import json
import csv
import time

__author__ = "Kun Chen"
__copyright__ = "Copyright(c) 2016, Cisco Systems, Inc."
__license__ = "New-style BSD"
__email__ = "kunch@cisco.com"

parser = argparse.ArgumentParser(description='Sharding Benchmark')

# Host Config
parser.add_argument("--host", default="localhost", help="the IP of the target host to initiate benchmark testing on.")
parser.add_argument("--port", type=int, default=8181, help="the port number of target host.")

# Test Parameters
parser.add_argument("--type", choices=["ROUND-ROBIN", "MULTI-THREADED"], nargs='+',
                    default=["ROUND-ROBIN", "MULTI-THREADED"],
                    help='Type of test to run. Default=["ROUND-ROBIN", "MULTI-THREADED"]')
parser.add_argument("--datastore", choices=["CONFIG", "OPERATIONAL"],
                    nargs='+', default=["CONFIG", "OPERATIONAL"], help="Data store type.")
parser.add_argument("--shards", type=int, default=[1, 2, 4, 8], nargs="+",
                    help="Number of shards.")
parser.add_argument("--dataitems", type=int, default=100000,
                    help="Number of total-shard data items.")
parser.add_argument("--putspertx", type=int, default=[1, 10, 100, 1000, 10000], nargs='+',
                    help="Number of write operations(PUT, MERGE, or DELETE) before a transaction submit() is issued.")
parser.add_argument("--listeners", type=int, default=[0, 1, 10, 100], nargs='+',
                    help="Number of data tree change listeners listening for changes on the test exec tree.")
parser.add_argument("--predata", choices=["TRUE", "FALSE"], nargs="+", default=["FALSE", "TRUE"],
                    help="Specifies whether test data should be pre-created before pushing it into the data store."
                         'Default=["FALSE"]')
parser.add_argument("--validate", choices=["TRUE", "FALSE"], nargs="+", default=["FALSE"],
                    help="Specifies whether the written data should be validated."
                         'Default=["FALSE"]')
parser.add_argument("--warmup", type=int, default=10,
                    help="Number of warmup runs before official test runs. Default 10")
parser.add_argument("--runs", type=int, default=10,
                    help="Number of official test runs. Default 10. "
                         "Note: Reported results are based on official test runs.")
parser.add_argument("--delay", type=int, default=10,
                    help="Delay between test runs. Default 10")

args = parser.parse_args()

BASE_URL = "http://%s:%d/restconf/" % (args.host, args.port)


def send_test_request(test_type, datastore, shards, data_items, puts_per_tx, listeners, precreate_data, validate_data):
    """
    Sends a request to the shardingsimple app to start a sharding data store benchmark test run.
    The shardingsimple app will perform the requested benchmark test and return measured
    transaction times
    :param test_type: Type of test to run, "ROUND-ROBIN" or "MULTI-THREADED"
    :param datastore: "CONFIG" or "OPERATIONAL"
    :param shards: Number of shards
    :param data_items: Number of per-shard data items
    :param puts_per_tx: Number of write operations(PUT, MERGE, or DELETE) before a transaction submit() is issued
    :param listeners: Number of data tree change listeners listening for changes on the test exec tree
    :param precreate_data: Specifies whether test data should be pre-created before pushing it into the data store,
    "True" or "False"
    :param validate_data: Specifies whether the written data should be validated, "True" or "False"
    :return: Result from the RESTCONF RPC
    """
    url = BASE_URL + "operations/shardingsimple:shard-test"
    postheaders = {'content-type': 'application/json', 'Accept': 'application/json'}

    test_request_template = '''{
            "input": {
                "test-type": "%s",
                "data-store": "%s",
                "shards": "%d",
                "dataItems": "%d",
                "putsPerTx": "%d",
                "listeners": "%d",
                "precreate-data": "%s",
                "validate-data": "%s"
            }
        }'''
    data = test_request_template % (test_type, datastore, shards, data_items,
                                    puts_per_tx, listeners, precreate_data, validate_data)
    r = requests.post(url, data, headers=postheaders, stream=False, auth=('admin', 'admin'))
    result = {u'http-status': r.status_code}
    if r.status_code == 200:
        result = dict(result.items() + json.loads(r.content)['output'].items())
    else:
        print 'Error %s, %s' % (r.status_code, r.content)
    return result


def print_results(run_type, idx, res):
    """
    Prints results from a shardingsimple test run to console
    :param run_type: String parameter that can be used to identify the type of the
                     test run (e.g. WARMUP or TEST)
    :param idx: Index of the test run
    :param res: Parsed json (dictionary) that was returned from a shardingsimple test run
    :return: None
    """
    print '{0:s} #{1:d}: status: {2:s}, totalExecTime {3:d}, ' \
          'listenerEventsOk {4:d} txOk {5:d}, txError {6:d}' \
        .format(run_type, idx, res[u'status'], res[u'totalExecTime'], res[u'listenerEventsOk'], res[u'txOk'],
                res[u'txError'])


def run_test(warmup_runs, test_runs, test_type, datastore, shards, data_items, puts_per_tx,
             listeners, precreate_data, validate_data):
    """
    Execute a shardingsimple test.
    The shardingsimple app will perform the requested benchmark test and return measured
    transaction times
    :param warmup_runs:
    :param test_runs: Number of test runs
    :param test_type: Type of test to run, "ROUND-ROBIN" or "MULTI-THREADED"
    :param datastore: "CONFIG" or "OPERATIONAL"
    :param shards: Number of shards
    :param data_items: Number of per-shard data items
    :param puts_per_tx: Number of write operations(PUT, MERGE, or DELETE) before a transaction submit() is issued
    :param listeners: Number of data tree change listeners listening for changes on the test exec tree
    :param precreate_data: Specifies whether test data should be pre-created before pushing it into the data store,
    "True" or "False"
    :param validate_data: Specifies whether the written data should be validated, "True" or "False"
    :return: Average test execution time
    """
    total_exec_time = 0.0

    print 'Test Type: {0:s}, Datastore: {1:s}, Shards: {2:d}, Data Items: {3:d}, ' \
          'Puts per Tx: {4:d}, Listeners: {5:d}, Precreate Data: {6:s}, Validate Data: {7:s}' \
        .format(test_type, datastore, shards, data_items, puts_per_tx, listeners, precreate_data, validate_data)

    for idx in range(warmup_runs):
        res = send_test_request(test_type, datastore, shards, data_items,
                                puts_per_tx, listeners, precreate_data, validate_data)
        print_results('WARMUP', idx, res)

    for idx in range(test_runs):
        res = send_test_request(test_type, datastore, shards, data_items,
                                puts_per_tx, listeners, precreate_data, validate_data)
        print_results('TEST', idx, res)
        total_exec_time += res[u'totalExecTime']

    return total_exec_time / test_runs


if __name__ == "__main__":
    # Test Parameters
    TEST_TYPE = args.type
    DATASTORE = args.datastore
    SHARDS = args.shards
    DATA_ITEMS = args.dataitems
    PUTS_PER_TX = args.putspertx
    LISTENERS = args.listeners
    PRECREATE_DATA = args.predata
    VALIDATE_DATA = args.validate
    DELAY = args.delay
    USEC_PER_SEC = 1000000

    # Iterations
    WARMUP_RUNS = args.warmup
    TEST_RUNS = args.runs

    # Run the sharding tests and collect data in a csv file for import into a graphing software
    filename = "result " + time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(time.time())) + ".csv"
    f = open(filename, 'wt')
    try:
        start_time = time.time()
        print "Start time: %f" % start_time

        writer = csv.writer(f)

        # Iterate over all test type, datastore, number of shards, per-shard data items, puts per transaction
        # listeners and if it is precreate data and validate data
        writer.writerow(('Test Type', 'Datastore', 'Validate Data', 'Precreate Data', 'Listeners', 'Puts per Tx',
                         'Shards', 'Data Items', 'Exec Time'))

        for ttype in TEST_TYPE:
            print '\n***************************************'
            print 'Test Type: %s' % ttype
            print '***************************************'
            writer.writerow((('%s' % ttype), '', ''))

            for dtst in DATASTORE:
                print
                '---------------------------------------'
                print 'Datastore: %s' % dtst
                print '---------------------------------------'
                writer.writerow(('', ('%s' % dtst), ''))

                for vld in VALIDATE_DATA:
                    print 'Validate Data: %s' % vld
                    writer.writerow(('', '', '%s' % vld))

                    for pcd in PRECREATE_DATA:
                        print 'Precreate Data: %s' % pcd
                        writer.writerow(('', '', '', '%s' % pcd))

                        for lsts in LISTENERS:
                            print 'Listeners: %d' % lsts
                            writer.writerow(('', '', '', '', '%d' % lsts))

                            for ppt in PUTS_PER_TX:
                                print 'Puts per Tx: %d' % ppt
                                writer.writerow(('', '', '', '', '', '%d' % ppt))

                                for sd in SHARDS:
                                    print 'Shards: %d' % sd

                                    di = DATA_ITEMS / sd
                                    print 'Data Items: %d' % di

                                    avg_exec_time = \
                                        run_test(WARMUP_RUNS, TEST_RUNS, ttype, dtst, sd, di, ppt, lsts, pcd, vld)

                                    writer.writerow(('', '', '', '', '', '', '%d' % sd, '%d' % di, avg_exec_time))
                                    print '     avg_exec_time: %d' % avg_exec_time
                                    tx_rate = DATA_ITEMS / ppt * USEC_PER_SEC / avg_exec_time
                                    upd_rate = DATA_ITEMS * USEC_PER_SEC / avg_exec_time
                                    print '     tx_rate: %d, upd_rate: %d' % (tx_rate, upd_rate)

                                    time.sleep(DELAY)

    finally:
        f.close()
