UPDATE recipe r
SET video_url = m.url
FROM media m
WHERE r.video_id = m.id
  AND r.video_url IS NULL;
