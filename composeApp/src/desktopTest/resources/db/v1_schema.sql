-- Fixture: a real schema-v1 MechForge database (v2 minus engineering_references)
-- with data rows, used by the v1 -> v2 migration test (README v2 §7.1 criterion 3).
CREATE TABLE calculators (
  id TEXT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL,
  category TEXT NOT NULL,
  description TEXT NOT NULL,
  formula TEXT NOT NULL,
  reference TEXT NOT NULL,
  version INTEGER NOT NULL,
  active INTEGER NOT NULL
);

CREATE TABLE calculation_history (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  calculator_id TEXT NOT NULL,
  title TEXT NOT NULL,
  timestamp INTEGER NOT NULL,
  inputs_json TEXT NOT NULL,
  results_json TEXT NOT NULL
);

CREATE TABLE saved_calculations (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  calculator_id TEXT NOT NULL,
  name TEXT NOT NULL,
  timestamp INTEGER NOT NULL,
  inputs_json TEXT NOT NULL,
  results_json TEXT NOT NULL,
  project_id INTEGER REFERENCES projects(id) ON DELETE SET NULL
);

CREATE TABLE favorites (
  calculator_id TEXT NOT NULL PRIMARY KEY,
  timestamp INTEGER NOT NULL
);

CREATE TABLE user_settings (
  key TEXT NOT NULL PRIMARY KEY,
  value TEXT NOT NULL
);

CREATE TABLE projects (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  description TEXT NOT NULL,
  created_at INTEGER NOT NULL
);

INSERT INTO calculators VALUES ('pump-power', 'Pump Hydraulic Power & Shaft Power', 'HYDRAULICS', 'd', 'f', 'r', 1, 1);
INSERT INTO calculation_history(calculator_id, title, timestamp, inputs_json, results_json)
  VALUES ('pump-power', 'Pump Power run', 1726200000000, '{"q":100}', '{"results":[]}');
INSERT INTO favorites VALUES ('pump-power', 1726200000000);
INSERT INTO user_settings VALUES ('theme', 'system');
INSERT INTO projects(name, description, created_at) VALUES ('Plant Upgrade', 'Main pump station', 1726100000000);
