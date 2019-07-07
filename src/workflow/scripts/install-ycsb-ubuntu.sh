#!/usr/bin/env bash
curl -O --location https://github.com/brianfrankcooper/YCSB/releases/download/0.14.0/ycsb-0.14.0.tar.gz
tar xfvz ycsb-0.14.0.tar.gz
cd ycsb-0.14.0
echo "PATH=\"\$PATH:`pwd`/bin\"" >> ~/.profile
source ~/.profile
