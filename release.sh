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

LOCAL_TAG_EXISTS=false
REMOTE_TAG_EXISTS=false
REPUBLISH=false

if git rev-parse "$TAG" >/dev/null 2>&1; then
  LOCAL_TAG_EXISTS=true
fi

if git ls-remote --tags origin "$TAG" | grep -q "refs/tags/$TAG"; then
  REMOTE_TAG_EXISTS=true
fi

if [ "$LOCAL_TAG_EXISTS" = true ] || [ "$REMOTE_TAG_EXISTS" = true ]; then
  echo "警告：版本 $TAG 已經存在。"
  echo

  if [ "$LOCAL_TAG_EXISTS" = true ]; then
    echo "- 本機已存在 tag：$TAG"
  fi

  if [ "$REMOTE_TAG_EXISTS" = true ]; then
    echo "- 遠端已存在 tag：$TAG"
  fi

  echo
  echo "是否要重新發布 $TAG？"
  echo "這會刪除本機與遠端的同名 tag，然後重新建立 tag。"
  read -p "輸入 y 表示重新發布，其他則取消： " REPUBLISH_ANSWER

  if [ "$REPUBLISH_ANSWER" = "y" ] || [ "$REPUBLISH_ANSWER" = "Y" ]; then
    REPUBLISH=true

    echo
    echo "準備重新發布：$TAG"

    if [ "$LOCAL_TAG_EXISTS" = true ]; then
      echo "刪除本機 tag：$TAG"
      git tag -d "$TAG"
    fi

    if [ "$REMOTE_TAG_EXISTS" = true ]; then
      echo "刪除遠端 tag：$TAG"
      git push origin ":refs/tags/$TAG"
    fi
  else
    echo "已取消發布。"
    exit 0
  fi
fi

echo
echo "開始 Maven 打包測試..."
./mvnw clean package -DskipTests

echo
echo "Maven 打包成功，準備提交。"
echo

git status --short
echo

git add .

COMMIT_MESSAGE="Release $TAG"
RELEASE_NOTES=""

if git diff --cached --quiet; then
  echo "沒有新的檔案變更，略過 commit。"
else
  echo "是否要描述本次更新內容？"
  read -p "輸入 y 表示要描述，其他則略過： " ADD_DESCRIPTION

  if [ "$ADD_DESCRIPTION" = "y" ] || [ "$ADD_DESCRIPTION" = "Y" ]; then
    echo
    echo "請輸入本次更新內容。"
    echo "每輸入一行按 Enter。"
    echo "輸入空白行代表結束。"
    echo

    while true; do
      read -p "> " LINE

      if [ -z "$LINE" ]; then
        break
      fi

      RELEASE_NOTES="$RELEASE_NOTES
- $LINE"
    done

    if [ -n "$RELEASE_NOTES" ]; then
      git commit -m "$COMMIT_MESSAGE" -m "$RELEASE_NOTES"
    else
      git commit -m "$COMMIT_MESSAGE"
    fi
  else
    git commit -m "$COMMIT_MESSAGE"
  fi
fi

git push origin main

echo
echo "建立 Git tag：$TAG"

if [ -n "$RELEASE_NOTES" ]; then
  git tag -a "$TAG" -m "WholeSummer $TAG" -m "$RELEASE_NOTES"
else
  git tag -a "$TAG" -m "WholeSummer $TAG"
fi

git push origin "$TAG"

echo
echo "發布 tag 完成：$TAG"

if [ "$REPUBLISH" = true ]; then
  echo "這是重新發布版本：$TAG"
  echo "GitHub Actions 會重新打包 exe，並更新 GitHub Release 中的安裝檔。"
else
  echo "GitHub Actions 會自動建立 Windows exe 並上傳到 GitHub Releases。"
fi