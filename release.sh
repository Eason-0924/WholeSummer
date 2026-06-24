#!/bin/bash

set -e

CURRENT_BRANCH=$(git branch --show-current)

if [ "$CURRENT_BRANCH" != "main" ]; then
  echo "錯誤：目前分支是 $CURRENT_BRANCH，不是 main"
  exit 1
fi

REPUBLISH=false

read_project_version() {
  ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout
}

refresh_version_state() {
  VERSION=$(read_project_version)

  if [ -z "$VERSION" ]; then
    echo "錯誤：無法從 pom.xml 讀取 project.version"
    exit 1
  fi

  TAG="v$VERSION"
  LOCAL_TAG_EXISTS=false
  REMOTE_TAG_EXISTS=false

  if git rev-parse "$TAG" >/dev/null 2>&1; then
    LOCAL_TAG_EXISTS=true
  fi

  if git ls-remote --tags origin "$TAG" | grep -q "refs/tags/$TAG"; then
    REMOTE_TAG_EXISTS=true
  fi
}

update_project_version() {
  NEW_VERSION="$1"
  ./mvnw -q versions:set -DnewVersion="$NEW_VERSION" -DgenerateBackupPoms=false
}

refresh_version_state

while [ "$LOCAL_TAG_EXISTS" = true ] || [ "$REMOTE_TAG_EXISTS" = true ]; do
  echo "警告：版本 $TAG 已經存在。"
  echo

  if [ "$LOCAL_TAG_EXISTS" = true ]; then
    echo "- 本機已存在 tag：$TAG"
  fi

  if [ "$REMOTE_TAG_EXISTS" = true ]; then
    echo "- 遠端已存在 tag：$TAG"
  fi

  echo
  echo "請選擇處理方式："
  echo "1. 覆蓋並重新發布 $TAG"
  echo "2. 變更版本號"
  echo "3. 取消發布"
  read -p "請輸入 1、2 或 3： " VERSION_ACTION

  case "$VERSION_ACTION" in
    1)
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
      LOCAL_TAG_EXISTS=false
      REMOTE_TAG_EXISTS=false
      ;;
    2)
      while true; do
        read -p "請輸入新版本號（格式 x.y.z，例如 1.0.2）： " NEW_VERSION

        if [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
          break
        fi

        echo "版本格式不正確，請使用 x.y.z 格式。"
      done

      if [ "$NEW_VERSION" = "$VERSION" ]; then
        echo "新版本號與目前版本相同，請重新選擇。"
        echo
        continue
      fi

      echo "更新 pom.xml 版本：$VERSION -> $NEW_VERSION"
      update_project_version "$NEW_VERSION"
      REPUBLISH=false
      refresh_version_state

      if [ "$LOCAL_TAG_EXISTS" = false ] && [ "$REMOTE_TAG_EXISTS" = false ]; then
        echo "版本已變更為 $VERSION。"
      else
        echo "版本 $TAG 也已存在，請再次選擇處理方式。"
      fi
      echo
      ;;
    3)
      echo "已取消發布。"
      exit 0
      ;;
    *)
      echo "選項無效，請輸入 1、2 或 3。"
      echo
      ;;
  esac
done

echo "目前 pom.xml 版本：$VERSION"
echo "即將發布：$TAG"
echo

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
