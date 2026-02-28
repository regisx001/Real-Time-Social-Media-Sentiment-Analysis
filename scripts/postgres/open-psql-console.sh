#!/bin/bash

# Script to open the PostgreSQL console
# This connects to the 'sentiments' container running TimescaleDB

echo "Opening PSQL Console for database 'realtime_social_media_sentiments'..."
echo "Use \q to exit."
echo "---------------------------------------------------"

# Run psql inside the 'sentiments' container
sudo docker exec -it social_media_sentiments psql -U admin -d realtime_social_media_sentiments

echo ""
echo "PSQL session closed."