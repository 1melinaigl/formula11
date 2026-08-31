INSERT INTO players (name, league, team, position, base_value) VALUES
('Lionel Messi', 'LaLiga', 'Inter Miami', 'Forward', 100000000.00),
('Kylian Mbappé', 'LaLiga', 'Real Madrid', 'Forward', 95000000.00),
('Jude Bellingham', 'Premier League', 'Real Madrid', 'Midfielder', 90000000.00),
('Virgil van Dijk', 'Premier League', 'Liverpool', 'Defender', 70000000.00),
('Ederson', 'Premier League', 'Manchester City', 'Goalkeeper', 55000000.00)
ON CONFLICT DO NOTHING;