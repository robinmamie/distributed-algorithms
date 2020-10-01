./../barrier.py -p 2
./../finishedSignal.py -p 2
./run.sh --id 1 --hosts hosts --barrier localhost:10000 --signal localhost:11000 --output proc1.txt config
./run.sh --id 2 --hosts hosts --barrier localhost:10000 --signal localhost:11000 --output proc1.txt config
