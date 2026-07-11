package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/99designs/gqlgen/graphql/handler"
	"github.com/99designs/gqlgen/graphql/handler/extension"
	"github.com/99designs/gqlgen/graphql/handler/lru"
	"github.com/99designs/gqlgen/graphql/handler/transport"
	"github.com/99designs/gqlgen/graphql/playground"
	"github.com/vektah/gqlparser/v2/ast"

	"github.com/jonabadie/kanclaude-go/graph"
	"github.com/jonabadie/kanclaude-go/internal/store"
)

const (
	defaultPort = "8080"
	defaultDSN  = "postgres://kanclaude:kanclaude@localhost:5433/kanclaude"
)

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = defaultPort
	}
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		dsn = defaultDSN
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	st, err := store.New(ctx, dsn)
	cancel()
	if err != nil {
		log.Fatalf("store: %v", err)
	}
	defer st.Close()

	srv := handler.New(graph.NewExecutableSchema(graph.Config{Resolvers: &graph.Resolver{Store: st}}))

	srv.AddTransport(transport.Options{})
	srv.AddTransport(transport.GET{})
	srv.AddTransport(transport.POST{})

	srv.SetQueryCache(lru.New[*ast.QueryDocument](1000))

	srv.Use(extension.Introspection{})
	srv.Use(extension.AutomaticPersistedQuery{
		Cache: lru.New[string](100),
	})

	http.Handle("/", playground.Handler("KanClaude GraphQL", "/query"))
	http.Handle("/query", srv)

	log.Printf("KanClaude Go backend — playground at http://localhost:%s/", port)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}
