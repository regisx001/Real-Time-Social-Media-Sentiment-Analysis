import psycopg2
import json
import random
from datetime import datetime, timedelta

# Database connection parameters
DB_HOST = "localhost"
DB_PORT = "5435"
# Assuming default from generic setups or we should check docker-compose.yml
DB_NAME = "sentiments"
DB_USER = "postgres"
DB_PASS = "postgres"


def get_db_info():
    return {
        'user': 'admin',
        'password': 'adminpassword',
        'db': 'realtime_social_media_sentiments'
    }


def generate_tweet_data(index, date):
    sentiments = ["POSITIVE", "NEGATIVE", "NEUTRAL"]
    confidence = round(random.uniform(0.5, 0.99), 2)
    sentiment = random.choice(sentiments)

    raw_data = {
        "id": f"tweet_{index}",
        "text": f"This is synthetic tweet number {index} with {sentiment} sentiment.",
        "user": f"user_{random.randint(1, 1000)}",
        "lang": "en"
    }

    processed_data = {
        "sentiment": sentiment,
        "confidence": confidence,
        "tokens": ["synthetic", "tweet", "number", str(index)],
        "processed_text": f"synthetic tweet number {index} sentiment"
    }

    return json.dumps(raw_data), json.dumps(processed_data)


def seed_database():
    print("Starting database seeding...")

    db_info = get_db_info()

    try:
        conn = psycopg2.connect(
            host="localhost",
            port="5435",
            database=db_info['db'],
            user=db_info['user'],
            password=db_info['password']
        )
        cur = conn.cursor()

        # Check if table exists
        cur.execute("SELECT to_regclass('public.raw_tweets');")
        if not cur.fetchone()[0]:
            print("Creating raw_tweets table...")
            cur.execute("""
            CREATE TABLE IF NOT EXISTS raw_tweets (
                id BIGSERIAL PRIMARY KEY,
                raw_data JSONB,
                processed_data JSONB,
                ingested_at TIMESTAMP NOT NULL,
                processed_at TIMESTAMP
            )
            """)

        print("Generating and inserting 400 records...")

        # We want to ensure charts have data at all time ranges:
        # 10m, 30m, 1h, 12h, 24h, 7d, 30d
        now = datetime.now()
        buckets = [
            (now - timedelta(minutes=10), now, 50),
            (now - timedelta(minutes=30), now - timedelta(minutes=10), 50),
            (now - timedelta(hours=1), now - timedelta(minutes=30), 50),
            (now - timedelta(hours=12), now - timedelta(hours=1), 50),
            (now - timedelta(hours=24), now - timedelta(hours=12), 50),
            (now - timedelta(days=7), now - timedelta(hours=24), 50),
            (now - timedelta(days=30), now - timedelta(days=7), 100)
        ]

        inserted_count = 0
        for start_time, end_time, count in buckets:
            for _ in range(count):
                random_seconds = random.randint(
                    0, max(1, int((end_time - start_time).total_seconds())))
                ingested_at = start_time + timedelta(seconds=random_seconds)
                # Processed a few seconds/minutes later
                processed_at = ingested_at + \
                    timedelta(seconds=random.randint(1, 120))

                raw_data, processed_data = generate_tweet_data(
                    inserted_count, ingested_at)

                cur.execute(
                    """
                    INSERT INTO raw_tweets (raw_data, processed_data, ingested_at, processed_at)
                    VALUES (%s, %s, %s, %s)
                    """,
                    (raw_data, processed_data, ingested_at, processed_at)
                )
                inserted_count += 1

        conn.commit()
        cur.close()
        conn.close()

        print(f"Successfully inserted {inserted_count} rows!")

    except Exception as e:
        print(f"Error seeding database: {e}")
        print("Make sure psycopg2 is installed: pip install psycopg2-binary pyyaml")


if __name__ == "__main__":
    seed_database()
