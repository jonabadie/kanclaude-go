// Package store is the Postgres persistence layer. It speaks the GraphQL
// model types directly — with a single consumer, a separate domain layer
// would only add mapping noise.
package store

import (
	"context"
	_ "embed"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/jonabadie/kanclaude-go/graph/model"
)

//go:embed schema.sql
var schemaSQL string

var ErrNotFound = errors.New("no such task")

// Enums are stored lowercase ('todo', 'high', 'claude') to stay compatible
// with the original board.json data; GraphQL exposes them uppercase.
func toDB[E ~string](v E) string   { return strings.ToLower(string(v)) }
func fromDB[E ~string](s string) E { return E(strings.ToUpper(s)) }

const taskColumns = "id, title, descr, project, priority, ord, col, created_at"

// Tasks sort like the original UI: priority band first, then order key.
const boardOrder = `ORDER BY CASE priority WHEN 'high' THEN 0 WHEN 'normal' THEN 1 ELSE 2 END, ord, created_at`

type Store struct {
	pool *pgxpool.Pool
}

// New connects, waits for the database to accept connections (compose
// starts both containers at once), and applies schema.sql.
func New(ctx context.Context, dsn string) (*Store, error) {
	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		return nil, fmt.Errorf("connect: %w", err)
	}
	for {
		if err = pool.Ping(ctx); err == nil {
			break
		}
		select {
		case <-ctx.Done():
			pool.Close()
			return nil, fmt.Errorf("ping: %w", err)
		case <-time.After(500 * time.Millisecond):
		}
	}
	if _, err := pool.Exec(ctx, schemaSQL); err != nil {
		pool.Close()
		return nil, fmt.Errorf("apply schema: %w", err)
	}
	return &Store{pool: pool}, nil
}

func (s *Store) Close() { s.pool.Close() }

func scanTask(row pgx.CollectableRow) (*model.Task, error) {
	var t model.Task
	var priority, col string
	err := row.Scan(&t.ID, &t.Title, &t.Desc, &t.Project, &priority, &t.Order, &col, &t.CreatedAt)
	t.Priority = fromDB[model.Priority](priority)
	t.Column = fromDB[model.Column](col)
	t.Log = []*model.LogEntry{}
	return &t, err
}

// attachLogs loads the logs for all given tasks in one query.
func (s *Store) attachLogs(ctx context.Context, tasks []*model.Task) error {
	if len(tasks) == 0 {
		return nil
	}
	byID := make(map[string]*model.Task, len(tasks))
	ids := make([]string, len(tasks))
	for i, t := range tasks {
		ids[i] = t.ID
		byID[t.ID] = t
	}
	rows, err := s.pool.Query(ctx,
		`SELECT task_id, who, text, at FROM task_logs WHERE task_id = ANY($1) ORDER BY id`, ids)
	if err != nil {
		return err
	}
	defer rows.Close()
	for rows.Next() {
		var taskID, who string
		e := &model.LogEntry{}
		if err := rows.Scan(&taskID, &who, &e.Text, &e.At); err != nil {
			return err
		}
		e.Who = fromDB[model.Author](who)
		t := byID[taskID]
		t.Log = append(t.Log, e)
	}
	return rows.Err()
}

func (s *Store) queryTasks(ctx context.Context, sql string, args ...any) ([]*model.Task, error) {
	rows, err := s.pool.Query(ctx, sql, args...)
	if err != nil {
		return nil, err
	}
	tasks, err := pgx.CollectRows(rows, scanTask)
	if err != nil {
		return nil, err
	}
	if err := s.attachLogs(ctx, tasks); err != nil {
		return nil, err
	}
	return tasks, nil
}

func (s *Store) List(ctx context.Context, column *model.Column) ([]*model.Task, error) {
	if column != nil {
		return s.queryTasks(ctx,
			`SELECT `+taskColumns+` FROM tasks WHERE col = $1 `+boardOrder, toDB(*column))
	}
	return s.queryTasks(ctx, `SELECT `+taskColumns+` FROM tasks `+boardOrder)
}

func (s *Store) Get(ctx context.Context, id string) (*model.Task, error) {
	tasks, err := s.queryTasks(ctx, `SELECT `+taskColumns+` FROM tasks WHERE id = $1`, id)
	if err != nil {
		return nil, err
	}
	if len(tasks) == 0 {
		return nil, ErrNotFound
	}
	return tasks[0], nil
}

func (s *Store) Create(ctx context.Context, input model.NewTask) (*model.Task, error) {
	desc := strDefault(input.Desc, "")
	project := strDefault(input.Project, "")
	priority := model.PriorityNormal
	if input.Priority != nil {
		priority = *input.Priority
	}
	// New tasks go on top: below the current minimum order (as in the original).
	var id string
	err := s.pool.QueryRow(ctx, `
		INSERT INTO tasks (title, descr, project, priority, ord)
		VALUES ($1, $2, $3, $4, (SELECT COALESCE(LEAST(MIN(ord), 0), 0) - 1 FROM tasks))
		RETURNING id`,
		input.Title, desc, project, toDB(priority)).Scan(&id)
	if err != nil {
		return nil, err
	}
	return s.Get(ctx, id)
}

func (s *Store) Update(ctx context.Context, id string, patch model.TaskPatch) (*model.Task, error) {
	sets := []string{}
	args := []any{id}
	add := func(col string, v any) {
		args = append(args, v)
		sets = append(sets, fmt.Sprintf("%s = $%d", col, len(args)))
	}
	if patch.Title != nil {
		add("title", *patch.Title)
	}
	if patch.Desc != nil {
		add("descr", *patch.Desc)
	}
	if patch.Project != nil {
		add("project", *patch.Project)
	}
	if patch.Priority != nil {
		add("priority", toDB(*patch.Priority))
	}
	if patch.Order != nil {
		add("ord", *patch.Order)
	}
	if len(sets) > 0 {
		tag, err := s.pool.Exec(ctx,
			`UPDATE tasks SET `+strings.Join(sets, ", ")+` WHERE id = $1`, args...)
		if err != nil {
			return nil, err
		}
		if tag.RowsAffected() == 0 {
			return nil, ErrNotFound
		}
	}
	return s.Get(ctx, id)
}

func (s *Store) Move(ctx context.Context, id string, column model.Column, order *float64) (*model.Task, error) {
	tag, err := s.pool.Exec(ctx,
		`UPDATE tasks SET col = $2, ord = COALESCE($3, ord) WHERE id = $1`,
		id, toDB(column), order)
	if err != nil {
		return nil, err
	}
	if tag.RowsAffected() == 0 {
		return nil, ErrNotFound
	}
	return s.Get(ctx, id)
}

func (s *Store) AddLog(ctx context.Context, taskID string, who model.Author, text string) (*model.Task, error) {
	tag, err := s.pool.Exec(ctx,
		`INSERT INTO task_logs (task_id, who, text) SELECT id, $2, $3 FROM tasks WHERE id = $1`,
		taskID, toDB(who), text)
	if err != nil {
		return nil, err
	}
	if tag.RowsAffected() == 0 {
		return nil, ErrNotFound
	}
	return s.Get(ctx, taskID)
}

func (s *Store) Delete(ctx context.Context, id string) (bool, error) {
	tag, err := s.pool.Exec(ctx, `DELETE FROM tasks WHERE id = $1`, id)
	if err != nil {
		return false, err
	}
	return tag.RowsAffected() > 0, nil
}

func strDefault(v *string, def string) string {
	if v == nil {
		return def
	}
	return *v
}
