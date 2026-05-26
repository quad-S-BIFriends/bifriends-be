docker cp src/main/resources/db/math_seed.sql bifriends-db:/tmp/
docker cp src/main/resources/db/korean_seed.sql bifriends-db:/tmp/
docker exec bifriends-db psql -U bifriends -d bifriends -f /tmp/math_seed.sql
docker exec bifriends-db psql -U bifriends -d bifriends -f /tmp/korean_seed.sql

위 명령어로 시드 실행