#!/bin/sh -e

BASE=/home/tools/jira

if [ $# -ne 2 ]
then
 echo usage install.sh jira-version genshi-version
 exit 1
fi

mkdir -p $BASE/share/jira-$1/bin
mkdir -p $BASE/share/jira-$1/lib
export PYTHONPATH=$BASE/share/jira-$1/lib

cd genshi-$2
python setup.py install --prefix=$BASE/share/jira-$1 --install-lib=$BASE/share/jira-$1/lib
