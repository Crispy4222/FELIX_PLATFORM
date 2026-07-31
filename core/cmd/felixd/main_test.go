package main

import "testing"

func testCore() *Core {
	return &Core{registry: DoorRegistry{Doors: map[string]Door{
		"hallway":  {Label: "Hallway"},
		"files":    {Label: "Files"},
		"terminal": {Label: "Terminal"},
		"browser":  {Label: "Browser"},
		"network":  {Label: "Network"},
		"settings": {Label: "Settings"},
		"logs":     {Label: "Logs"},
	}}}
}

func TestNativePlanRoutesIntentToCapabilityOwner(t *testing.T) {
	core := testCore()
	plan := core.nativePlan("Find my project files and photos", Snapshot{Hostname: "test", LoadAverage: "0 0 0"})
	if plan.Door != "files" {
		t.Fatalf("expected files door, got %q", plan.Door)
	}
	if plan.NeedsConfirmation {
		t.Fatal("ordinary file browsing should not require destructive confirmation")
	}
}

func TestNativePlanHoldsDestructiveIntent(t *testing.T) {
	core := testCore()
	plan := core.nativePlan("format the computer and delete everything", Snapshot{Hostname: "test", LoadAverage: "0 0 0"})
	if !plan.NeedsConfirmation {
		t.Fatal("destructive intent must require confirmation")
	}
	if plan.Door != "" {
		t.Fatalf("destructive intent must not receive an application door, got %q", plan.Door)
	}
}

func TestOpenEndedIntentRemainsInHallway(t *testing.T) {
	core := testCore()
	plan := core.nativePlan("help me make sense of this", Snapshot{Hostname: "test", LoadAverage: "0 0 0"})
	if plan.Door != "hallway" {
		t.Fatalf("expected hallway for unresolved intent, got %q", plan.Door)
	}
}
