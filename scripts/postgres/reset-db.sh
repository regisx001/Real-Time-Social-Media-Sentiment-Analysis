#!/bin/bash

# Script to reset the database (DROP all tables) to allow clean Flyway migration
# WARNING: This deletes all data!

echo "Resetting database 'realtime_social_media_sentiments'..."
echo "Dropping all tables..."
echo "---------------------------------------------------"

# Execute SQL inside the container
sudo docker exec -i social_media_sentiments psql -U admin -d realtime_social_media_sentiments <<EOF
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO admin;
GRANT ALL ON SCHEMA public TO public;
EOF

echo "---------------------------------------------------"
echo "Database reset complete. Creating clean state."