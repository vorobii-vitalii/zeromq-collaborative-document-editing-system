#!/bin/bash

echo 'Building image...'
docker build -t document-editor/document-server .
echo 'Image built'