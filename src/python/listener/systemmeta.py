# This script is a listener of a trigger on the systemmetadata table.
# It prints out the payload from the tigger and send the information to RabbitMQ.
# You need to install psycopg2 first: pip install psycopg2-binary
import psycopg2
import select
import json

# Database connection parameters
DB_CONFIG = {
    'dbname': 'metacat',
    'user': 'tao',
    'password': 'metacat',
    'host': 'localhost',
    'port': 5432
}

def listen():
    conn = psycopg2.connect(**DB_CONFIG)
    conn.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)

    cur = conn.cursor()
    cur.execute("LISTEN core_db_event;")
    print("Listening for notifications on channel 'core_db_event'...")

    try:
        while True:
            # Wait for activity
            if select.select([conn], [], [], 5) == ([], [], []):
                continue  # Timeout with no activity
            conn.poll()
            while conn.notifies:
                notify = conn.notifies.pop(0)
                try:
                    payload = json.loads(notify.payload)
                    print("Received Notification:")
                    print(json.dumps(payload, indent=2))
                except json.JSONDecodeError:
                    print("Received non-JSON payload:")
                    print(notify.payload)

    except KeyboardInterrupt:
        print("Listener stopped.")
    finally:
        cur.close()
        conn.close()

if __name__ == "__main__":
    listen()