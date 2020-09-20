#!/usr/bin/env python3

import argparse
import socket

class Barrier:
    def __init__(self, host, port, wait_for):
        self.host = host
        self.port = port
        self.wait_for = wait_for

    def listen(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind((self.host, self.port))
        self.sock.listen()

    def waitSingle(self):
        connections = []
        addresses = []

        while True:
            conn, addr = self.sock.accept()
            connections.append(conn)
            addresses.append(addr)
            yield addr

            if len(connections) == self.wait_for:
                break

        for conn in connections:
            conn.close()

        return None

    def wait(self):
        g = self.waitSingle()

        conn = []
        while True:
            try:
                conn.append(next(g))
            except StopIteration:
                break

        return conn


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
        default=11000,
        type=int,
        dest="port",
        help="TCP port where the barrier listens to (default: 11000)",
    )

    parser.add_argument(
        "--processes",
        required=True,
        type=int,
        dest="processes",
        help="Number of processes the barrier waits for",
    )

    results = parser.parse_args()

    barrier = Barrier(results.host, results.port, results.processes)
    barrier.listen()
    print("Barrier listens on {}:{} and waits for {} processes".format(results.host, results.port, results.processes))

    # connectedAddr = barrier.wait()
    connectedAddrGen = barrier.waitSingle()

    while True:
        try:
            connectedAddr = next(connectedAddrGen)
            print("Connection from {}".format(connectedAddr))
        except StopIteration:
            break