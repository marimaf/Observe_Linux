#!/bin/bash
javac -encoding UTF-8 -cp \* *.java
for (( ; ; ))
do
   java -cp .:\* Main
   sleep 1
done
