#! /bin/sh

BRANCH=$1
RELEASE_VERSION=$2
NEXT_VERSION=$3

git checkout $BRANCH
git checkout -b release_$RELEASE_VERSION
echo -n $RELEASE_VERSION > version.txt
git add . && git commit -m "Version bump to $RELEASE_VERSION" && git push --set-upstream origin release_$RELEASE_VERSION

git checkout $BRANCH
echo -n $NEXT_VERSION > version.txt
git add . && git commit -m "Version bump to $NEXT_VERSION" && git push
