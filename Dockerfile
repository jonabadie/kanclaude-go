FROM golang:1.26-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /kanclaude-go .

FROM alpine:3.21
COPY --from=build /kanclaude-go /usr/local/bin/kanclaude-go
EXPOSE 8080
CMD ["kanclaude-go"]
