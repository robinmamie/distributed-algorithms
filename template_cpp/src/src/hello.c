#include "hello.h"

void hello() {
    printf("Source file that end in .c will be compiled automatically with gcc,\n"
           "while files that end in .cpp will be compiled automatically with g++.\n"
           "For those that prefer C, they can either use the provided C++\n"
           "argument parser and write everything else in C, or delete the\n"
           "provided main.cpp and start from scratch.\n"
           "Make sure to have compatible ABI when calling C functions from cpp:\n"
           "See `hello.h` on how to do this.\n");
}
