package store

import (
	"context"
	"os"
	"testing"

	"github.com/jonabadie/kanclaude-go/graph/model"
)

// Integration test — runs against a real Postgres when TEST_DATABASE_URL is
// set (e.g. the docker-compose one:
// postgres://kanclaude:kanclaude@localhost:5433/kanclaude), skips otherwise.
func TestTaskLifecycle(t *testing.T) {
	dsn := os.Getenv("TEST_DATABASE_URL")
	if dsn == "" {
		t.Skip("TEST_DATABASE_URL not set")
	}
	ctx := context.Background()
	s, err := New(ctx, dsn)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	defer s.Close()

	high := model.PriorityHigh
	task, err := s.Create(ctx, model.NewTask{Title: "test: ship the Go backend", Priority: &high})
	if err != nil {
		t.Fatalf("create: %v", err)
	}
	defer s.Delete(ctx, task.ID)

	if task.Column != model.ColumnTodo || task.Priority != model.PriorityHigh {
		t.Errorf("new task: got column %s priority %s, want TODO/HIGH", task.Column, task.Priority)
	}

	task, err = s.AddLog(ctx, task.ID, model.AuthorClaude, "started: writing tests")
	if err != nil {
		t.Fatalf("add log: %v", err)
	}
	if len(task.Log) != 1 || task.Log[0].Who != model.AuthorClaude {
		t.Errorf("log: got %+v, want one claude entry", task.Log)
	}

	ord := 2.5
	task, err = s.Move(ctx, task.ID, model.ColumnVerify, &ord)
	if err != nil {
		t.Fatalf("move: %v", err)
	}
	if task.Column != model.ColumnVerify || task.Order != 2.5 {
		t.Errorf("move: got %s/%v, want VERIFY/2.5", task.Column, task.Order)
	}

	newTitle := "test: shipped"
	task, err = s.Update(ctx, task.ID, model.TaskPatch{Title: &newTitle})
	if err != nil {
		t.Fatalf("update: %v", err)
	}
	if task.Title != newTitle {
		t.Errorf("update: title %q, want %q", task.Title, newTitle)
	}

	board, err := s.List(ctx, nil)
	if err != nil {
		t.Fatalf("list: %v", err)
	}
	found := false
	for _, bt := range board {
		if bt.ID == task.ID {
			found = true
		}
	}
	if !found {
		t.Error("list: created task not on board")
	}

	ok, err := s.Delete(ctx, task.ID)
	if err != nil || !ok {
		t.Fatalf("delete: ok=%v err=%v", ok, err)
	}
	if _, err := s.Get(ctx, task.ID); err != ErrNotFound {
		t.Errorf("get after delete: err=%v, want ErrNotFound", err)
	}
}
