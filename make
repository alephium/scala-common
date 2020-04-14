#!/usr/bin/env python3
import argparse, os, sys

def run(cmd):
    print(cmd)
    os.system(cmd)

class AlephiumMake(object):
    def __init__(self):
        parser = argparse.ArgumentParser(
            usage='''make <command> [<args>]

   build     Build the project
   test      Run the test suite
   publish   Publish locally the project deliverable
''')
        parser.add_argument('command', help='Subcommand to run')
        args = parser.parse_args(sys.argv[1:2])
        if not hasattr(self, args.command):
            print('Unrecognized command')
            parser.print_help()
            exit(1)
        getattr(self, args.command)()

    def build(self):
        run('sbt compile')

    def test(self):
        run('sbt scalafmtSbt scalafmt test:scalafmt scalastyle test:scalastyle coverage test coverageReport')
        
    def publish(self):
        run('sbt publishLocal')


if __name__ == '__main__':
    AlephiumMake()
