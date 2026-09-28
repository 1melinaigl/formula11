-- Carga inicial de jugadores para el catálogo (Entrega 1)
-- No incluye cotizaciones, precios ni tokens: eso es de una entrega futura.

INSERT INTO jugadores (nombre, equipo, liga, posicion) VALUES
-- Premier League
('Alisson Becker',        'Liverpool',        'PREMIER_LEAGUE', 'POR'),
('Virgil van Dijk',       'Liverpool',        'PREMIER_LEAGUE', 'DEF'),
('Rodri',                 'Manchester City',  'PREMIER_LEAGUE', 'MED'),
('Erling Haaland',        'Manchester City',  'PREMIER_LEAGUE', 'DEL'),

-- Bundesliga
('Manuel Neuer',          'Bayern Munich',    'BUNDESLIGA',      'POR'),
('Dayot Upamecano',       'Bayern Munich',    'BUNDESLIGA',      'DEF'),
('Jamal Musiala',         'Bayern Munich',    'BUNDESLIGA',      'MED'),
('Harry Kane',            'Bayern Munich',    'BUNDESLIGA',      'DEL'),

-- La Liga
('Thibaut Courtois',      'Real Madrid',      'LA_LIGA',         'POR'),
('Antonio Rüdiger',       'Real Madrid',      'LA_LIGA',         'DEF'),
('Jude Bellingham',       'Real Madrid',      'LA_LIGA',         'MED'),
('Kylian Mbappé',         'Real Madrid',      'LA_LIGA',         'DEL'),
('Robert Lewandowski',    'Barcelona',        'LA_LIGA',         'DEL'),

-- Serie A
('Mike Maignan',          'AC Milan',         'SERIE_A',         'POR'),
('Alessandro Bastoni',    'Inter de Milán',   'SERIE_A',         'DEF'),
('Nicolò Barella',        'Inter de Milán',   'SERIE_A',         'MED'),
('Lautaro Martínez',      'Inter de Milán',   'SERIE_A',         'DEL'),

-- Ligue 1
('Gianluigi Donnarumma',  'Paris Saint-Germain', 'LIGUE_1',      'POR'),
('Marquinhos',            'Paris Saint-Germain', 'LIGUE_1',      'DEF'),
('Vitinha',               'Paris Saint-Germain', 'LIGUE_1',      'MED'),
('Ludovic Ajorque',       'Racing Strasbourg',   'LIGUE_1',      'DEL');