#!/usr/bin/env python3

import argparse
import socket

def barrier(host, port, wait_for):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((host, port))
        s.listen()

        connections = []
        addresses = []

        while True:
            conn, addr = s.accept()
            connections.append(conn)
            addresses.append(addr)
            print("Connection from {}".format(addr))

            if len(connections) == wait_for:
                break

        for conn in connections:
            conn.close()

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

    print("Barrier listens on {}:{} and waits for {} processes".format(results.host, results.port, results.processes))
    barrier(results.host, results.port, results.processes)