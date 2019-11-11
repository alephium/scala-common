#!/usr/bin/env python3
import argparse, os

parser = argparse.ArgumentParser(description='Alephium Make')

parser.add_argument('goal', type=str)

args = parser.parse_args()

def run(cmd):
    print(cmd)
    os.system(cmd)

if args.goal == 'build':
    run('sbt clean compile')

elif args.goal == 'test':
    run('sbt clean scalafmtSbt scalafmt test:scalafmt scalastyle test:scalastyle coverage test coverageReport doc')

elif args.goal == 'package':
    run('sbt clean publishLocal')

