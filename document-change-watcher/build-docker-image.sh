#!/bin/bash

echo 'Building image...'
docker build -t document-editor/document-change-watcher .
echo 'Image built!'