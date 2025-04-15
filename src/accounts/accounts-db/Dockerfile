FROM postgres:16.6-alpine@sha256:56e87f5517713be839c1ce724ec275cb46579fb05edbb0887f26f60faebe7f0d

COPY initdb/0-accounts-schema.sql /docker-entrypoint-initdb.d/
COPY initdb/1-load-testdata.sql /docker-entrypoint-initdb.d/
