-- Applied idempotently on startup. "desc", "order" and "column" are reserved
-- or awkward in SQL, hence descr/ord/col.
CREATE TABLE IF NOT EXISTS tasks (
  id         TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
  title      TEXT NOT NULL,
  descr      TEXT NOT NULL DEFAULT '',
  project    TEXT NOT NULL DEFAULT '',
  priority   TEXT NOT NULL DEFAULT 'normal' CHECK (priority IN ('high', 'normal', 'low')),
  ord        DOUBLE PRECISION NOT NULL DEFAULT 0,
  col        TEXT NOT NULL DEFAULT 'todo' CHECK (col IN ('todo', 'doing', 'verify', 'done', 'blocked')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS task_logs (
  id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  task_id TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  who     TEXT NOT NULL CHECK (who IN ('user', 'claude')),
  text    TEXT NOT NULL,
  at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS tasks_board_idx ON tasks (col, priority, ord);
CREATE INDEX IF NOT EXISTS task_logs_task_idx ON task_logs (task_id, id);
