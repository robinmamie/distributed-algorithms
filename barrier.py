#!/usr/bin/env python3

import argparse
import socket
import time
import struct
from collections import OrderedDict

class Barrier:
    def __init__(self, host, port, waitFor, printer=None):
        self.host = host
        self.port = port
        self.waitFor = waitFor
        self.printer = printer
        self.startTimes = dict()

    def listen(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind((self.host, self.port))
        self.sock.listen(128)

    def wait(self):
        connections = []
        addresses = []

        while True:
            conn, addr = self.sock.accept()

            idInBytes = []
            while len(idInBytes) < 8:
                data = conn.recv(8 - len(idInBytes))
                if not data:
                    raise Exception("Could not recv the LogicalPID")
                idInBytes += data

            pid = struct.unpack('!Q', bytes(idInBytes))[0]
            connections.append((pid, conn))
            addresses.append(addr)

            if self.printer:
                self.printer("Connection from {}, corresponds to PID {}".format(addr, pid))

            if len(connections) == self.waitFor:
                break

        for pid, conn in connections:
            self.startTimes[pid] = int(time.time() * 1000)
            conn.close()

        return None

    def startTimesFuture(self):
        return self.startTimes

if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--host",
        default="0.0.0.0",
        dest="host",
        help="IP address where the barrier listens to (default: any)",
    )

    parser.add_argument(
        "--port",
        default=10000,
        type=int,
        dest="port",
        help="TCP port where the barrier listens to (default: 10000)",
    )

    parser.add_argument(
        "-p",
        "--processes",
        required=True,
        type=int,
        dest="processes",
        help="Number of processes the barrier waits for",
    )

    results = parser.parse_args()

    barrier = Barrier(results.host, results.port, results.processes, print)
    barrier.listen()
    print("Barrier listens on {}:{} and waits for {} processes".format(results.host, results.port, results.processes))

    barrier.wait()

    for pid, startTs in OrderedDict(sorted(barrier.startTimesFuture().items())).items():
        print("Process {} started broadcasting at time {} ms from Unix epoch ".format(pid, startTs))