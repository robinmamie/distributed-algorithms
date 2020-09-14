#pragma once
#include "parser.hpp"

void waitOnBarrier(Parser::Host const &barrier);

void waitOnBarrier(Parser::Host const &barrier) {
  struct sockaddr_in server;
  std::memset(&server, 0, sizeof(server));

  int fd = socket(AF_INET, SOCK_STREAM, 0);
  if (fd < 0) {
    throw std::runtime_error("Could not create the barrier socket: " +
                             std::string(std::strerror(errno)));
  }

  server.sin_family = AF_INET;
  server.sin_addr.s_addr = barrier.ip;
  server.sin_port = barrier.port;
  if (connect(fd, reinterpret_cast<struct sockaddr *>(&server),
              sizeof(server)) < 0) {
    throw std::runtime_error("Could not connect to the barrier: " +
                             std::string(std::strerror(errno)));
  }

  char dummy;
  if (recv(fd, &dummy, sizeof(dummy), 0) < 0) {
    throw std::runtime_error("Could not read from the barrier socket: " +
                             std::string(std::strerror(errno)));
  }

  close(fd);
}
