# Distributed Algorithms 2020/21 - EPFL


# Overview
The goal of this practical project is to implement certain building blocks necessary for a decentralized system. To this end, some underlying abstractions will be used:

  - Perfect Links,
  - Uniform Reliable Broadcast,
  - FIFO Broadcast (submission #1),
  - Localized Causal Broadcast (submission #2)

Various applications (e.g., a payment system) can be built upon these lower-level abstractions. We will check your submissions (see [Submissions](#submissions)) for correctness and performance as part of the final evaluation.

The implementation must take into account that **messages** exchanged between processes **may be dropped, delayed or reordered by the network**. The execution of processes may be paused for an arbitrary amount of time and resumed later. Processes may also fail by crashing at arbitrary points of their execution.

# Project Requirements

## Basics
In order to have a fair comparison among implementations, as well as provide support to students, the project must be developed using the following tools:

*Allowed programming language*:
  - C11 and/or C++17
  - Java

*Build system*:
  - CMake for C/C++
  - Maven for Java

Note that we provide you a template for both C/C++ and Java. It is mandatory to use the template in your project.

Allowed 3rd party libraries: **None**. You are not allowed to use any third party libraries in your project. C++17 and Java 11 come with an extensive standard library that can easily satisfy all your needs.

## Messages
Inter-process point-to-point messages (at the low level) must be carried exclusively by UDP packets in their most basic form, not utilizing any additional features (e.g., any form of feedback about packet delivery) provided by the network stack, the operating system or external libraries. Everything must be implemented on top of these low-level point to point messages.

The application messages (i.e., those broadcast by processes) are numbered sequentially at each process, starting from `1`. Thus, each process broadcasts messages `1` to `m`. By default, the payload carried by an application message is only the sequence number of that message.

## Template structure
We provide you a template for both C/C++ and Java, which you should use in your project. The template has a certain structure that is explained below:

### For C/C++:
```bash
.
├── bin
│   ├── deploy
│   │   └── README
│   ├── logs
│   │   └── README
│   └── README
├── build.sh
├── cleanup.sh
├── CMakeLists.txt
├── run.sh
└── src
    ├── CMakeLists.txt
    └── ...
```
You can run:
  - `build.sh` to compile your project
  - `run.sh <arguments>` to run your project
  - `cleanup.sh` to delete the build artifacts. We recommend running this command when submitting your project for evaluation.

You should place your source code under the `src` directory. You are not allowed to edit any files outside the `src` directory. Furthermore, you are not allowed to edit sections of `src/CMakeLists.txt` that are marked as "DO NOT EDIT". Apart from these restrictions, you are completely free on how to structure the source code inside `src`.

The template already includes some source code under `src`, that will help you with parsing the arguments provided to the executable (see below).

Finally, **your executable should not create/use directories named "deploy" and/or "logs"** in the current working directory. These directories are reserved for evaluation!

### For Java:
```sh
.
├── bin
│   ├── deploy
│   │   └── README
│   ├── logs
│   │   └── README
│   └── README
├── build.sh
├── cleanup.sh
├── pom.xml
├── run.sh
└── src
    └── main
        └── java
            └── cs451
                └── ...
```
The restrictions for the C/C++ template also apply here. The difference is that you are only allowed to place your source code under `src/main/java/cs451`.

## Interface
The templates provided come with a command line interface (CLI) that you should use in your deliverables. The implementation for the CLI is given to you for convenience. You are allowed to make any modifications to it, as long as it complies to the specification.

The supported arguments are:
```sh
./run.sh --id ID --hosts HOSTS --barrier NAME:PORT --signal NAME:PORT --output OUTPUT [config]
```

Where:
  - `ID` specifies the unique identifier of the process. In a system of `n` processes, the identifiers are `1`...`n`.
  - `HOSTS` specifies the path to a file that contains the information about every process in the system, i.e., it describes the system membership. The file contains as many lines as processes in the system. A process identity consists of a numerical process identifier, the IP address or name of the process and the port number on which the process is listening for incoming messages. The entries of each process identity are separated by white space character. The following is an example of the contents of a `HOSTS` file for a system of 5 processes:
  ```
2 localhost 11002
5 127.0.0.1 11005
3 10.0.0.1 11002
1 192.168.0.1 11001
4 my.domain.com 11002
  ```
  **Note**: The processes should listen for incoming messages in the port range `11000` to `11999` inclusive.

  - `NAME:PORT` for `--barrier` specifies the IP/Name and port of the barrier, which ensures that all processes have been intialized before the broacasting starts. The barrier is implemented using TCP and it is one of the two places (the other is for the `--signal` argument) in the source code where TCP is allowed. You can run the barrier as:
```sh
./barrier.py [-h] [--host HOST] [--port PORT] --processes PROCESSES
```
E.g. to wait for 3 processes, run `./barrier.py --processes 3`. When 3 connections are established to the barrier, the barrier closes all the connections, signaling the processes to start. The barrier cannot be used twice, meaning that you need to restart it every time you want to run you application. Also, it must be started before any other process.
 - `NAME:PORT` for `--signal` specifies the IP/Name and port of the service (notification handler) that handles the notifications sent by processes when they finish broadcasting. This notification is used to measure the time processes spend broadcasting messages: it is the time period between the release of the barrier up until this notification is sent.  The notification mechanism is implemented using TCP and it is one of the two places (the other is for the `--barrier` argument) in the source code where TCP is allowed. You can run the notification handler as:
```sh
./finishedSignal.py [-h] [--host HOST] [--port PORT] --processes PROCESSES
```
Note: Start both `finishedSignal.py` and `barrier.py` before you start any other process and provide the same number of `--processes` for both.
  - `OUTPUT` specifies the path to a text file where a process stores its output. The text file contains a log of events.  Each event is represented by one line of the output file, terminated by a Unix-style line break `\n`. There are two types of events to be logged:
    -  broadcast of application message, using the format `b`*`seq_nr`*,
  where `seq_nr` is the sequence number of the message.
    - delivery of application message, using the format `d`*`sender`* *`seq_nr`*, where *`sender`* is the number of the process that broadcast the message and *`seq_nr`* is the sequence number of the message (as numbered by the broadcasting process).

An example of the content of an output file:
```
b 1
b 2
b 3
d 2 1
d 4 2
b 4
```
A process that receives a `SIGTERM` or `SIGINT` signal must immediately stop its execution with the exception of writing to an output log file (see below). In particular, it must not send or handle any received network packets. This is used to simulate process crashes. You can assume that at most a minority (e.g., 1 out of 3; 2 out of 5; 4 out of 10, ...) processes may crash in one execution.

**Note:** The most straight-forward way of logging the output is to append a line to the output file on every broadcast or delivery event. However, this may harm the performance of the implementation. You might consider more sophisticated logging approaches, such as storing all logs in memory and write them to a file only when the `SIGINT` or `SIGTERM` signal is received. Also note that even a crashed process needs to output the sequence of events that occurred before the crash. You can assume that a process crash will be simulated only by the `SIGINT` or `SIGTERM` signals. Remember that writing to files is the only action we allow a process to do after receiving a `SIGINT` or `SIGTERM` signal.

  - `config`  specifies the path to a file that contains specific information required from the deliverable (e.g. processes that broadcast).

## Compilation
All submitted implementations will be tested using Ubuntu 18.04 running on a 64-bit architecture. 
These are the specific versions of toolchains where you project will be tested upon:
  - gcc (Ubuntu 7.5.0-3ubuntu1~18.04) 7.5.0
  - g++ (Ubuntu 7.5.0-3ubuntu1~18.04) 7.5.0
  - cmake version 3.10.2
  - OpenJDK Runtime Environment (build 11.0.8+10-post-Ubuntu-0ubuntu118.04.1)
  - Apache Maven 3.6.3 (cecedd343002696d0abb50b32b541b8a6ba2883f)

All submitted files are to be placed in one zip file, in the same structure as the provided templates. Make sure that the top-level of the zip file is not a directory that contains the template (along with your source code inside `src`), but the template itself.

You are **strongly encouraged** to test the compilation of your code in the virtualbox VM provided to you. Submissions that fail to compile will not be considered for grading.

**Detailed instructions for submitting your project will be released soon.**

## Cooperation
This project is meant to be completed individually. Copying from others is prohibited. You are free (and encouraged) to discuss the projects with others, but the submitted source code must be the exclusive work yours. Multiple copies of the same code will be disregarded without investigating which is the "original" and which is the "copy". Furthermore, please give appropriate credit to pieces of code you found online (e.g. in stackoverflow).

*Note*: code similarity tools will be used to check copying.

## Submissions
This project accounts for 30% of the final grade and comprises two submissions:
  - A runnable application implementing FIFO Broadcast, and
  - A runnable application implementing Localized Causal Broadcast.

Note that these submissions are *incremental*. This means that your work towards the first will help you in your work towards the second.

We evaluate your submissions based on two criteria: correctness and performance. We prioritize correctness, therefore a correct - yet slow - implementation will receive (at least) a score of 4-out-of-6. The rest 2-out-of-6 is given based on the perfomance of your implementation compared to the perfomance of the implemantions submitted by your colleagues. The fastest correct implementation will receive a perfect score (6). Incorrect implementations receive a score below 4, depending on the number of tests they fail to pass.

For your submissions, we are only interested in the FIFO and Localized Causal broadcast algorithms. We define several details for each algorithms below.

### FIFO Broadcast application
  - You must implement this on top of uniform reliable broadcast (URB).
  - The `config` command-line argument for this algorithm consists of a file that contains an integer `m` in its first line. `m` defines how many messages each process should broadcast.

### Localized Causal Broadcast
  - The `config` command-line argument for this algorithm consists of a file that contains an integer `m` in its first line. `m` defines how many messages each process should broadcast.
  - For a system of `n` processes, there are `n` more lines in the `config` file. Each line `i` corresponds to process `i`, and such a line indicates the identities of other processes which can affect process `i`. See the example below.
  - The FIFO property still needs to be maintained by localized causal broadcast. That is, messages broadcast by the same process must not be delivered in a different order then they were broadcast.
  - The output format for localized causal broadcast remains the same as before, i.e., adhering to the description in Section.
  Example of `config` file for a system of `5` processes, where each one broadcasts `m` messages:
```
m
1 4 5
2 1
4
5 3 4
3 1 2
```
*Note*: Lines should end in `\n`, and numbers are separated by white-space characters.

In this example we specify that process `1` is affected by messages broadcast by processes `4` and `5`. Similarly, we specify that process `2` is only affected by process `1`. Process `4` is not affected by any other processes. Process `5` is affected by processes `3` and `4`.

We say that a process `x` is affected by a process `z` if all the messages which process `z` broadcasts and which process `x` delivers become dependencies for all future messages broadcast by process `x`. We call these dependencies *localized*. If a process is not affected by any other process, messages it broadcasts only depend on its previously broadcast messages (due to the FIFO property).

*Note*:  In the default causal broadcast (this algorithm will be discussed in one of the lectures) each process affects `all` processes. In this algorithm we can selectively define which process affects some other process.
