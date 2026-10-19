#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/src/androidTest/assets
ffmpeg -hide_banner -loglevel error -y -f lavfi -i 'sine=frequency=440:duration=2' \
  -i app/src/main/res/drawable-nodpi/ami_logo.png -map 0:a -map 1:v -c:a libmp3lame -c:v copy \
  -id3v2_version 3 -metadata title='Phone Test Song' -metadata artist='Phone Test Artist' \
  -metadata album='Phone Test Album' -metadata album_artist='Various Test Artists' \
  -metadata date='2024' -metadata genre='Electronic' -metadata track='3/10' \
  -metadata:s:v title='Album cover' -metadata:s:v comment='Cover (front)' \
  app/src/androidTest/assets/tagged.mp3
