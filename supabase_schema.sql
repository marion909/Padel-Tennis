-- Supabase SQL Schema für Padel Tennis App
-- Bitte in der Supabase Console unter SQL Editor ausführen

-- Tabelle 1: matches (Spielergebnisse)
CREATE TABLE IF NOT EXISTS matches (
  id BIGSERIAL PRIMARY KEY,
  timestamp BIGINT NOT NULL,
  team_a_name TEXT NOT NULL,
  team_b_name TEXT NOT NULL,
  winner_team_index INTEGER NOT NULL,
  sets_team_a INTEGER NOT NULL,
  sets_team_b INTEGER NOT NULL,
  games_data JSONB DEFAULT '[]',
  duration_ms BIGINT NOT NULL,
  golden_point_used BOOLEAN NOT NULL,
  num_sets INTEGER NOT NULL,
  total_points INTEGER NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tabelle 2: players (Spielerstatistiken)
CREATE TABLE IF NOT EXISTS players (
  uuid TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  matches_played INTEGER DEFAULT 0,
  matches_won INTEGER DEFAULT 0,
  total_points INTEGER DEFAULT 0,
  total_sets_won INTEGER DEFAULT 0,
  total_games_won INTEGER DEFAULT 0,
  last_played BIGINT DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tabelle 3: match_players (Verknüpfung Match <-> Spieler)
CREATE TABLE IF NOT EXISTS match_players (
  match_id BIGINT REFERENCES matches(id) ON DELETE CASCADE,
  player_uuid TEXT REFERENCES players(uuid) ON DELETE CASCADE,
  team_index INTEGER NOT NULL,
  points_scored INTEGER NOT NULL,
  was_winner BOOLEAN NOT NULL,
  PRIMARY KEY (match_id, player_uuid)
);

-- Indizes für bessere Performance
CREATE INDEX IF NOT EXISTS idx_matches_timestamp ON matches(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_players_matches_played ON players(matches_played DESC);
CREATE INDEX IF NOT EXISTS idx_players_matches_won ON players(matches_won DESC);
CREATE INDEX IF NOT EXISTS idx_match_players_match_id ON match_players(match_id);
CREATE INDEX IF NOT EXISTS idx_match_players_player_uuid ON match_players(player_uuid);

-- Optional: Row Level Security (RLS) aktivieren für öffentlichen Lesezugriff
ALTER TABLE matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE players ENABLE ROW LEVEL SECURITY;
ALTER TABLE match_players ENABLE ROW LEVEL SECURITY;

-- Öffentlicher Lesezugriff (Anon Key kann lesen)
CREATE POLICY "Öffentlicher Lesezugriff auf matches" ON matches
  FOR SELECT USING (true);

CREATE POLICY "Öffentlicher Lesezugriff auf players" ON players
  FOR SELECT USING (true);

CREATE POLICY "Öffentlicher Lesezugriff auf match_players" ON match_players
  FOR SELECT USING (true);

-- Öffentlicher Schreibzugriff (Anon Key kann schreiben)
CREATE POLICY "Öffentlicher Schreibzugriff auf matches" ON matches
  FOR INSERT WITH CHECK (true);

CREATE POLICY "Öffentlicher Schreibzugriff auf players" ON players
  FOR ALL USING (true);

CREATE POLICY "Öffentlicher Schreibzugriff auf match_players" ON match_players
  FOR INSERT WITH CHECK (true);
