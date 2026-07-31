package main

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"syscall"
	"time"
)

type Config struct {
	Identity   string         `json:"identity"`
	Listen     string         `json:"listen"`
	StaticDir  string         `json:"static_dir"`
	StateDir   string         `json:"state_dir"`
	DoorsFile  string         `json:"doors_file"`
	Provider   ProviderConfig `json:"provider"`
}

type ProviderConfig struct {
	Endpoint  string `json:"endpoint"`
	Model     string `json:"model"`
	TokenFile string `json:"token_file"`
	Timeout   int    `json:"timeout_seconds"`
}

type DoorRegistry struct {
	Doors map[string]Door `json:"doors"`
}

type Door struct {
	Label   string `json:"label"`
	Purpose string `json:"purpose"`
	Command string `json:"command"`
}

type Snapshot struct {
	Hostname       string   `json:"hostname"`
	BootID         string   `json:"boot_id"`
	UptimeSeconds  float64  `json:"uptime_seconds"`
	LoadAverage    string   `json:"load_average"`
	MemoryTotalKB  uint64   `json:"memory_total_kb"`
	MemoryAvailKB  uint64   `json:"memory_available_kb"`
	DiskTotalBytes uint64   `json:"disk_total_bytes"`
	DiskFreeBytes  uint64   `json:"disk_free_bytes"`
	Interfaces     []string `json:"interfaces"`
	GoVersion      string   `json:"runtime"`
	ObservedAt     string   `json:"observed_at"`
}

type IntentRequest struct {
	Text string `json:"text"`
}

type RememberRequest struct {
	Text string `json:"text"`
}

type Plan struct {
	Thought             string  `json:"thought"`
	Door                string  `json:"door,omitempty"`
	LaunchURL           string  `json:"launch_url,omitempty"`
	Confidence          float64 `json:"confidence"`
	NeedsConfirmation   bool    `json:"needs_confirmation"`
	Provider            string  `json:"provider"`
	Evidence            []string `json:"evidence,omitempty"`
}

type Event struct {
	ID       string      `json:"id"`
	Time     string      `json:"time"`
	Kind     string      `json:"kind"`
	Input    string      `json:"input,omitempty"`
	Plan     *Plan       `json:"plan,omitempty"`
	Snapshot *Snapshot   `json:"snapshot,omitempty"`
}

type Core struct {
	cfg      Config
	registry DoorRegistry
	mu       sync.RWMutex
	client   *http.Client
}

func main() {
	if filepath.Base(os.Args[0]) == "felixctl" {
		if err := runCLI(os.Args[1:]); err != nil {
			fmt.Fprintln(os.Stderr, "felixctl:", err)
			os.Exit(1)
		}
		return
	}

	cfgPath := "/etc/felix/core.json"
	if len(os.Args) == 3 && os.Args[1] == "-config" {
		cfgPath = os.Args[2]
	}
	cfg, err := loadConfig(cfgPath)
	if err != nil {
		log.Fatal(err)
	}
	if err := os.MkdirAll(cfg.StateDir, 0750); err != nil {
		log.Fatal(err)
	}
	registry, err := loadDoors(cfg.DoorsFile)
	if err != nil {
		log.Fatal(err)
	}
	timeout := time.Duration(cfg.Provider.Timeout) * time.Second
	if timeout <= 0 {
		timeout = 20 * time.Second
	}
	core := &Core{cfg: cfg, registry: registry, client: &http.Client{Timeout: timeout}}
	if err := core.record(Event{ID: newID(), Time: now(), Kind: "boot", Snapshot: ptr(snapshot())}); err != nil {
		log.Printf("boot event: %v", err)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/api/health", core.health)
	mux.HandleFunc("/api/status", core.status)
	mux.HandleFunc("/api/doors", core.doors)
	mux.HandleFunc("/api/intent", core.intent)
	mux.HandleFunc("/api/remember", core.remember)
	mux.HandleFunc("/api/events", core.events)
	mux.Handle("/", http.FileServer(http.Dir(cfg.StaticDir)))

	server := &http.Server{
		Addr:              cfg.Listen,
		Handler:           securityHeaders(mux),
		ReadHeaderTimeout: 5 * time.Second,
		IdleTimeout:       60 * time.Second,
	}
	log.Printf("%s online at http://%s", cfg.Identity, cfg.Listen)
	if err := server.ListenAndServe(); !errors.Is(err, http.ErrServerClosed) {
		log.Fatal(err)
	}
}

func loadConfig(path string) (Config, error) {
	cfg := Config{
		Identity:  "FELIX",
		Listen:    "127.0.0.1:8080",
		StaticDir: "/opt/felix-hallway",
		StateDir:  "/var/lib/felix",
		DoorsFile: "/etc/felix/doors.json",
	}
	b, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return cfg, nil
		}
		return cfg, err
	}
	if err := json.Unmarshal(b, &cfg); err != nil {
		return cfg, fmt.Errorf("decode %s: %w", path, err)
	}
	return cfg, nil
}

func loadDoors(path string) (DoorRegistry, error) {
	var r DoorRegistry
	b, err := os.ReadFile(path)
	if err != nil {
		return r, err
	}
	if err := json.Unmarshal(b, &r); err != nil {
		return r, err
	}
	if len(r.Doors) == 0 {
		return r, errors.New("door registry is empty")
	}
	return r, nil
}

func (c *Core) health(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]any{"identity": c.cfg.Identity, "state": "online", "time": now()})
}

func (c *Core) status(w http.ResponseWriter, _ *http.Request) {
	s := snapshot()
	_ = c.record(Event{ID: newID(), Time: now(), Kind: "observe", Snapshot: &s})
	writeJSON(w, http.StatusOK, s)
}

func (c *Core) doors(w http.ResponseWriter, _ *http.Request) {
	c.mu.RLock()
	defer c.mu.RUnlock()
	writeJSON(w, http.StatusOK, c.registry)
}

func (c *Core) intent(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeError(w, http.StatusMethodNotAllowed, "POST required")
		return
	}
	var req IntentRequest
	if err := decodeJSON(r.Body, &req); err != nil || strings.TrimSpace(req.Text) == "" {
		writeError(w, http.StatusBadRequest, "intent text required")
		return
	}
	req.Text = strings.TrimSpace(req.Text)
	s := snapshot()
	plan, err := c.providerPlan(r.Context(), req.Text, s)
	if err != nil {
		log.Printf("provider fallback: %v", err)
		plan = c.nativePlan(req.Text, s)
	}
	if plan.Door != "" {
		if _, ok := c.registry.Doors[plan.Door]; !ok {
			plan.Evidence = append(plan.Evidence, "Provider suggested a door outside the allowlist; action removed.")
			plan.Door = ""
			plan.LaunchURL = ""
			plan.NeedsConfirmation = true
		}
	}
	if plan.Door != "" {
		plan.LaunchURL = "felix://open/" + plan.Door
	}
	e := Event{ID: newID(), Time: now(), Kind: "intent", Input: req.Text, Plan: &plan, Snapshot: &s}
	if err := c.record(e); err != nil {
		log.Printf("record intent: %v", err)
	}
	writeJSON(w, http.StatusOK, map[string]any{"event_id": e.ID, "plan": plan, "snapshot": s})
}

func (c *Core) remember(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeError(w, http.StatusMethodNotAllowed, "POST required")
		return
	}
	var req RememberRequest
	if err := decodeJSON(r.Body, &req); err != nil || strings.TrimSpace(req.Text) == "" {
		writeError(w, http.StatusBadRequest, "memory text required")
		return
	}
	req.Text = strings.TrimSpace(req.Text)
	e := Event{ID: newID(), Time: now(), Kind: "memory", Input: req.Text}
	if err := c.record(e); err != nil {
		writeError(w, http.StatusInternalServerError, err.Error())
		return
	}
	writeJSON(w, http.StatusCreated, e)
}

func (c *Core) events(w http.ResponseWriter, r *http.Request) {
	limit := 20
	if raw := r.URL.Query().Get("limit"); raw != "" {
		if n, err := strconv.Atoi(raw); err == nil && n > 0 && n <= 200 {
			limit = n
		}
	}
	events, err := c.readEvents(limit)
	if err != nil {
		writeError(w, http.StatusInternalServerError, err.Error())
		return
	}
	writeJSON(w, http.StatusOK, events)
}

func (c *Core) nativePlan(text string, s Snapshot) Plan {
	q := strings.ToLower(text)
	plan := Plan{
		Thought:    "I read the operator intent against current machine state and the registered capability doors.",
		Confidence: 0.58,
		Provider:   "native-cortex",
		Evidence: []string{
			fmt.Sprintf("host=%s", s.Hostname),
			fmt.Sprintf("load=%s", s.LoadAverage),
		},
	}
	match := func(words ...string) bool {
		for _, word := range words {
			if strings.Contains(q, word) {
				return true
			}
		}
		return false
	}
	switch {
	case match("file", "folder", "project", "drive", "photo"):
		plan.Door, plan.Confidence = "files", 0.84
		plan.Thought = "This intent is about stored material, so the Files door is the shortest bounded route."
	case match("terminal", "command", "shell", "build", "compile", "git"):
		plan.Door, plan.Confidence = "terminal", 0.86
		plan.Thought = "This intent requires direct operator tooling, so I am routing to the Terminal door without inventing a background loop."
	case match("internet", "web", "search", "browser", "site"):
		plan.Door, plan.Confidence = "browser", 0.82
		plan.Thought = "This intent needs an external information surface, so the Browser door is the correct route."
	case match("wifi", "network", "vpn", "ethernet", "connection"):
		plan.Door, plan.Confidence = "network", 0.9
		plan.Thought = "This is a connectivity intent. The Network door owns that capability."
	case match("setting", "display", "screen", "keyboard", "mouse", "sound"):
		plan.Door, plan.Confidence = "settings", 0.87
		plan.Thought = "This is a device or desktop configuration intent, so Settings is the bounded owner."
	case match("log", "error", "crash", "service", "journal", "why"):
		plan.Door, plan.Confidence = "logs", 0.79
		plan.Thought = "This intent asks for evidence about system behavior. Logs is the evidence-first route."
	case match("shutdown", "power off", "reboot", "erase", "delete everything", "format"):
		plan.NeedsConfirmation = true
		plan.Confidence = 0.95
		plan.Thought = "This intent can alter availability or destroy state. I will not execute it through an application door without explicit confirmation and a dedicated policy action."
	default:
		plan.Door = "hallway"
		plan.Thought = "The request is open-ended. I am keeping it in the Hallway so the operator can refine the goal while preserving context."
	}
	return plan
}

func (c *Core) providerPlan(ctx context.Context, text string, s Snapshot) (Plan, error) {
	endpoint := strings.TrimSpace(c.cfg.Provider.Endpoint)
	if endpoint == "" {
		endpoint = strings.TrimSpace(os.Getenv("FELIX_PROVIDER_URL"))
	}
	if endpoint == "" {
		return Plan{}, errors.New("no model provider configured")
	}
	model := c.cfg.Provider.Model
	if model == "" {
		model = os.Getenv("FELIX_PROVIDER_MODEL")
	}
	if model == "" {
		model = "local"
	}
	token := strings.TrimSpace(os.Getenv("FELIX_PROVIDER_TOKEN"))
	if token == "" && c.cfg.Provider.TokenFile != "" {
		if b, err := os.ReadFile(c.cfg.Provider.TokenFile); err == nil {
			token = strings.TrimSpace(string(b))
		}
	}
	memories, _ := c.readEvents(12)
	prompt := map[string]any{
		"model": model,
		"temperature": 0.2,
		"messages": []map[string]string{
			{"role": "system", "content": "You are FELIX, the operating system control plane. You do not invent commands or bypass policy. Read machine state, memory, and the allowlisted door registry. Return ONLY JSON with thought, door, confidence, needs_confirmation, and evidence. door must be one registered door or empty."},
			{"role": "user", "content": mustJSON(map[string]any{"intent": text, "machine": s, "doors": c.registry.Doors, "recent_memory": memories})},
		},
	}
	body, _ := json.Marshal(prompt)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, bytes.NewReader(body))
	if err != nil {
		return Plan{}, err
	}
	req.Header.Set("Content-Type", "application/json")
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}
	resp, err := c.client.Do(req)
	if err != nil {
		return Plan{}, err
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		b, _ := io.ReadAll(io.LimitReader(resp.Body, 2048))
		return Plan{}, fmt.Errorf("provider status %d: %s", resp.StatusCode, strings.TrimSpace(string(b)))
	}
	var result struct {
		Choices []struct {
			Message struct {
				Content string `json:"content"`
			} `json:"message"`
		} `json:"choices"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return Plan{}, err
	}
	if len(result.Choices) == 0 {
		return Plan{}, errors.New("provider returned no choices")
	}
	content := strings.TrimSpace(result.Choices[0].Message.Content)
	content = strings.TrimPrefix(content, "```json")
	content = strings.TrimPrefix(content, "```")
	content = strings.TrimSuffix(content, "```")
	var plan Plan
	if err := json.Unmarshal([]byte(strings.TrimSpace(content)), &plan); err != nil {
		return Plan{}, fmt.Errorf("provider response was not plan JSON: %w", err)
	}
	plan.Provider = "model:" + model
	if plan.Confidence < 0 || plan.Confidence > 1 {
		plan.Confidence = 0.5
	}
	return plan, nil
}

func (c *Core) record(e Event) error {
	c.mu.Lock()
	defer c.mu.Unlock()
	path := filepath.Join(c.cfg.StateDir, "events.jsonl")
	f, err := os.OpenFile(path, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0640)
	if err != nil {
		return err
	}
	defer f.Close()
	return json.NewEncoder(f).Encode(e)
}

func (c *Core) readEvents(limit int) ([]Event, error) {
	c.mu.RLock()
	defer c.mu.RUnlock()
	path := filepath.Join(c.cfg.StateDir, "events.jsonl")
	f, err := os.Open(path)
	if err != nil {
		if os.IsNotExist(err) {
			return []Event{}, nil
		}
		return nil, err
	}
	defer f.Close()
	var all []Event
	scanner := bufio.NewScanner(f)
	scanner.Buffer(make([]byte, 64*1024), 2*1024*1024)
	for scanner.Scan() {
		var e Event
		if json.Unmarshal(scanner.Bytes(), &e) == nil {
			all = append(all, e)
		}
	}
	if err := scanner.Err(); err != nil {
		return nil, err
	}
	if len(all) > limit {
		all = all[len(all)-limit:]
	}
	for i, j := 0, len(all)-1; i < j; i, j = i+1, j-1 {
		all[i], all[j] = all[j], all[i]
	}
	return all, nil
}

func snapshot() Snapshot {
	host, _ := os.Hostname()
	bootID := strings.TrimSpace(readText("/proc/sys/kernel/random/boot_id"))
	load := strings.TrimSpace(readText("/proc/loadavg"))
	if fields := strings.Fields(load); len(fields) >= 3 {
		load = strings.Join(fields[:3], " ")
	}
	var uptime float64
	if fields := strings.Fields(readText("/proc/uptime")); len(fields) > 0 {
		uptime, _ = strconv.ParseFloat(fields[0], 64)
	}
	mem := map[string]uint64{}
	for _, line := range strings.Split(readText("/proc/meminfo"), "\n") {
		fields := strings.Fields(line)
		if len(fields) >= 2 {
			v, _ := strconv.ParseUint(fields[1], 10, 64)
			mem[strings.TrimSuffix(fields[0], ":")] = v
		}
	}
	var stat syscall.Statfs_t
	_ = syscall.Statfs("/", &stat)
	var ifaces []string
	if list, err := net.Interfaces(); err == nil {
		for _, iface := range list {
			if iface.Flags&net.FlagUp != 0 && iface.Name != "lo" {
				ifaces = append(ifaces, iface.Name)
			}
		}
	}
	return Snapshot{
		Hostname: host, BootID: bootID, UptimeSeconds: uptime, LoadAverage: load,
		MemoryTotalKB: mem["MemTotal"], MemoryAvailKB: mem["MemAvailable"],
		DiskTotalBytes: stat.Blocks * uint64(stat.Bsize), DiskFreeBytes: stat.Bavail * uint64(stat.Bsize),
		Interfaces: ifaces, GoVersion: runtime.Version(), ObservedAt: now(),
	}
}

func securityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("Referrer-Policy", "no-referrer")
		w.Header().Set("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; connect-src 'self'; img-src 'self' data:")
		next.ServeHTTP(w, r)
	})
}

func decodeJSON(r io.Reader, dst any) error {
	dec := json.NewDecoder(io.LimitReader(r, 1<<20))
	dec.DisallowUnknownFields()
	return dec.Decode(dst)
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, status int, msg string) {
	writeJSON(w, status, map[string]string{"error": msg})
}

func readText(path string) string {
	b, _ := os.ReadFile(path)
	return string(b)
}

func mustJSON(v any) string {
	b, _ := json.Marshal(v)
	return string(b)
}

func ptr[T any](v T) *T { return &v }
func now() string { return time.Now().UTC().Format(time.RFC3339Nano) }
func newID() string { return fmt.Sprintf("%d-%d", time.Now().UnixNano(), os.Getpid()) }

func runCLI(args []string) error {
	base := "http://127.0.0.1:8080"
	if len(args) == 0 {
		return errors.New("usage: felixctl status | think <intent> | remember <text> | events")
	}
	client := &http.Client{Timeout: 25 * time.Second}
	var method, path string
	var payload any
	switch args[0] {
	case "status":
		method, path = http.MethodGet, "/api/status"
	case "events":
		method, path = http.MethodGet, "/api/events?limit=25"
	case "think":
		if len(args) < 2 { return errors.New("think requires an intent") }
		method, path, payload = http.MethodPost, "/api/intent", IntentRequest{Text: strings.Join(args[1:], " ")}
	case "remember":
		if len(args) < 2 { return errors.New("remember requires text") }
		method, path, payload = http.MethodPost, "/api/remember", RememberRequest{Text: strings.Join(args[1:], " ")}
	default:
		return fmt.Errorf("unknown command %q", args[0])
	}
	var body io.Reader
	if payload != nil {
		b, _ := json.Marshal(payload)
		body = bytes.NewReader(b)
	}
	req, _ := http.NewRequest(method, base+path, body)
	if payload != nil { req.Header.Set("Content-Type", "application/json") }
	resp, err := client.Do(req)
	if err != nil { return err }
	defer resp.Body.Close()
	b, _ := io.ReadAll(resp.Body)
	if resp.StatusCode >= 300 { return fmt.Errorf("status %d: %s", resp.StatusCode, strings.TrimSpace(string(b))) }
	var pretty bytes.Buffer
	if json.Indent(&pretty, b, "", "  ") == nil {
		fmt.Println(pretty.String())
	} else {
		fmt.Print(string(b))
	}
	return nil
}
