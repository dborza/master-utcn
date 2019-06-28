#!/usr/bin/env bash

THREADS="10"

while getopts ":t:" opt; do
  case $opt in
    t) THREADS="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

echo "Threads:"${THREADS}