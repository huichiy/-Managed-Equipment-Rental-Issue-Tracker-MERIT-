CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    password TEXT NOT NULL,
    type TEXT NOT NULL,
    final_year INTEGER DEFAULT 0
);