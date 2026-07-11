# kanclaude-go

A Go + GraphQL + Postgres backend for [KanClaude], my kanban board shared
between a human and Claude. The original backend is a zero-dependency Node
server persisting to a JSON file; this is the same board rebuilt on a
production-shaped stack — and my first Go project, built to learn the
language properly.

**Stack:** Go, [gqlgen] (schema-first GraphQL), [pgx] (Postgres), Docker.

## Run

```sh
docker compose up          # API on :8080, Postgres on :5433
```

or, with only Postgres in Docker:

```sh
docker compose up -d postgres
go run .                   # uses the compose DB by default (DATABASE_URL to override)
```

GraphQL playground at <http://localhost:8080/>, API at `POST /query`.

## The model

Tasks flow `TODO → DOING → VERIFY → DONE` (or `BLOCKED`), sorted by priority
band then a float `order` key. Each task carries an append-only activity log
written by both `USER` and `CLAUDE` — that log is how Claude reports its work
back to the human. See [graph/schema.graphqls](graph/schema.graphqls).

```graphql
mutation {
  createTask(input: { title: "Ship it", priority: HIGH }) { id }
}

mutation {
  addLog(taskId: "…", who: CLAUDE, text: "started: shipping") { id }
  moveTask(id: "…", column: DOING) { column }
}

query {
  board(column: DOING) { title priority log { who text at } }
}
```

## Tests

Integration tests run against a real Postgres and skip when none is
configured:

```sh
docker compose up -d postgres
TEST_DATABASE_URL=postgres://kanclaude:kanclaude@localhost:5433/kanclaude go test ./...
```

## Layout

- `graph/schema.graphqls` — the schema; `go tool gqlgen generate` regenerates
  the executable schema and models from it
- `graph/schema.resolvers.go` — resolvers, thin delegation to the store
- `internal/store` — pgx-backed persistence; `schema.sql` applied
  idempotently at startup

[KanClaude]: https://github.com/jonabadie
[gqlgen]: https://gqlgen.com
[pgx]: https://github.com/jackc/pgx
