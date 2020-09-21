#pragma once
#include "parser.hpp"

#include <endian.h>
#include <algorithm>

class Coordinator {
public:
  Coordinator(unsigned long id, Parser::Host barrier, Parser::Host signal)
  : id_{id}, barrier_{barrier}, signal_{signal}
  {
    signalFd_ = connectToHost(signal_, "signal");
  }

  void waitOnBarrier() {
    int fd = connectToHost(barrier_, "barrier");

    char dummy;
    if (recv(fd, &dummy, sizeof(dummy), 0) < 0) {
      throw std::runtime_error("Could not read from the barrier socket: " +
                              std::string(std::strerror(errno)));
    }

    close(fd);
  }

  void finishedBroadcasting() {
    close(signalFd_);
  }

private:
  int connectToHost(Parser::Host const &host, std::string const &reason) {
    struct sockaddr_in server;
    std::memset(&server, 0, sizeof(server));
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0) {
      throw std::runtime_error("Could not create the " + reason + " socket: " +
                              std::string(std::strerror(errno)));
    }

    server.sin_family = AF_INET;
    server.sin_addr.s_addr = host.ip;
    server.sin_port = host.port;
    if (connect(fd, reinterpret_cast<struct sockaddr *>(&server),
                sizeof(server)) < 0) {
      throw std::runtime_error("Could not connect to the " + reason + ": " +
                              std::string(std::strerror(errno)));
    }

    auto id = htonT(static_cast<uint64_t>(id_));
    if (writeWithRetry(fd, reinterpret_cast<uint8_t *>(&id), sizeof(id))) {
      throw std::runtime_error("Could not send my LogicalPID to the " + reason + ": " +
                              std::string(std::strerror(errno)));
    }

    return fd;
  }

  // From https://stackoverflow.com/questions/32683086/handling-incomplete-write-calls
  ssize_t writeWithRetry (int fd, uint8_t const* buf, size_t size) {
      ssize_t ret;
      while (size > 0) {
          do
          {
              ret = write(fd, buf, size);
          } while ((ret < 0) && (errno == EINTR || errno == EAGAIN || errno == EWOULDBLOCK));
          if (ret < 0)
              return ret;
          size -= ret;
          buf += ret;
      }
      return 0;
  }

  template <typename T>
  constexpr T htonT (T value) noexcept
  {
  #if __BYTE_ORDER == __LITTLE_ENDIAN
    char* ptr = reinterpret_cast<char*>(&value);
    std::reverse(ptr, ptr + sizeof(T));
  #endif
    return value;
  }

private:
  unsigned long id_;
  Parser::Host barrier_;
  Parser::Host signal_;
  int signalFd_;
};
