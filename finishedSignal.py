#!/usr/bin/env python3

import argparse
import socket
import time
import struct
import selectors
from collections import OrderedDict

class FinishedSignal:
    def __init__(self, host, port, waitFor, printer=None):
        self.host = host
        self.port = port
        self.waitFor = waitFor
        self.printer = printer
        self.connections = dict()
        self.endTimes = dict()
        self.sel = selectors.DefaultSelector()

    def listen(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind((self.host, self.port))
        self.sock.listen(128)
        self.sock.setblocking(False)
        self.sel.register(self.sock, selectors.EVENT_READ, self.accept)

    def wait(self):
        while self.waitFor > 0:
            events = self.sel.select()
            for key, mask in events:
                callback = key.data
                callback(key.fileobj, mask)

        for key, pid_timestamp in self.connections.items():
            pid = pid_timestamp[0]
            ts = pid_timestamp[1]
            self.endTimes[pid] = ts

    def endTimestamps(self):
        return self.endTimes

    def accept(self, sock, mask):
        conn, addr = sock.accept()
        conn.setblocking(False)
        self.sel.register(conn, selectors.EVENT_READ, self.read)

    def read(self, conn, mask):
        data = conn.recv(8)
        if data:
            pid = struct.unpack('!Q', bytes(data))[0]
            self.connections[conn] = [pid]

            if self.printer:
                host, port = conn.getpeername()
                addr = '{}:{}'.format(host, port)
                self.printer("Connection from {}, corresponds to PID {}".format(addr, pid))
        else:
            self.connections[conn].append(int(time.time() * 1000))
            self.sel.unregister(conn)
            conn.close()
            self.waitFor -= 1



if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--host",
        default="0.0.0.0",
        dest="host",
        help="IP address where the finish signal listens to (default: any)",
    )

    parser.add_argument(
        "--port",
        default=11000,
        type=int,
        dest="port",
        help="TCP port where the finish signal listens to (default: 11000)",
    )

    parser.add_argument(
        "-p",
        "--processes",
        required=True,
        type=int,
        dest="processes",
        help="Number of processes the finish signal waits for",
    )

    results = parser.parse_args()

    signal = FinishedSignal(results.host, results.port, results.processes, print)
    signal.listen()
    print("Finish signal listens on {}:{} and waits for {} processes".format(results.host, results.port, results.processes))

    signal.wait()

    for pid, endTs in OrderedDict(sorted(signal.endTimestamps().items())).items():
        print("Process {} finished broadcasting at time {} ms from Unix epoch ".format(pid, endTs))