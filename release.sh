#!/bin/bash

set -e

VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)

if [ -z "$VERSION" ]; then
  echo "錯誤：無法從 pom.xml 讀取 project.version"
  exit 1
fi

TAG="v$VERSION"

echo "目前 pom.xml 版本：$VERSION"
echo "即將發布：$TAG"
echo

CURRENT_BRANCH=$(git branch --show-current)

if [ "$CURRENT_BRANCH" != "main" ]; then
  echo "錯誤：目前分支是 $CURRENT_BRANCH，不是 main"
  exit 1
fi

if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "錯誤：本機已存在 tag：$TAG"
  exit 1
fi

if git ls-remote --tags origin "$TAG" | grep -q "$TAG"; then
  echo "錯誤：遠端已存在 tag：$TAG"
  exit 1
fi

echo "開始 Maven 打包測試..."
./mvnw clean package -DskipTests

echo
echo "Maven 打包成功，準備提交。"
echo

git status --short
echo

git add .

if git diff --cached --quiet; then
  echo "沒有新的檔案變更，略過 commit。"
else
  git commit -m "Release $TAG"
fi

git push origin main

git tag "$TAG"
git push origin "$TAG"

echo
echo "發布完成：$TAG"