-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Erstellungszeit: 29. Aug 2026 um 17:20
-- Server-Version: 10.4.32-MariaDB
-- PHP-Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Datenbank: `bibliothek`
--

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `ausleihen`
--

CREATE TABLE `ausleihen` (
  `schueler_id` int(11) NOT NULL,
  `isbn` varchar(20) NOT NULL,
  `ausleihdatum` date NOT NULL,
  `geplante_rueckgabe` date NOT NULL,
  `ruckgabe_datum` date DEFAULT NULL,
  `id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Daten für Tabelle `ausleihen`
--

INSERT INTO `ausleihen` (`schueler_id`, `isbn`, `ausleihdatum`, `geplante_rueckgabe`, `ruckgabe_datum`, `id`) VALUES
(8, '978-3125739291', '2026-08-17', '2026-09-20', '2026-08-29', 16),
(8, '978-3608126013', '2026-08-17', '2026-09-20', NULL, 17),
(13, '978-3551321022', '2026-08-20', '2026-09-09', NULL, 18);

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `benutzer`
--

CREATE TABLE `benutzer` (
  `id` int(11) NOT NULL,
  `vorname` varchar(100) NOT NULL,
  `nachname` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `passwort` varchar(255) NOT NULL,
  `rolle` enum('schueler','lehrer') NOT NULL,
  `freigeschaltet` tinyint(1) NOT NULL,
  `gesperrt_von` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Daten für Tabelle `benutzer`
--

INSERT INTO `benutzer` (`id`, `vorname`, `nachname`, `email`, `passwort`, `rolle`, `freigeschaltet`, `gesperrt_von`) VALUES
(7, 'Tom', 'Meier', 'tom@email.com', '$argon2id$v=19$m=60000,t=10,p=1$A3QCmr7Z0kteBA3zb/sk1g$x3nKYQ4S4Ex7dP/5onZkryk7cuzVxje+CGL4xXLxpFc', 'schueler', 0, NULL),
(8, 'Marie', 'Schmidt', 'marie@email.com', '$argon2id$v=19$m=60000,t=10,p=1$K1HMAc4IiMliq71JEhaVug$17EBQyMvBMxenU+BcP6E91f9PXIae1v5kum8rCJuq5k', 'schueler', 1, NULL),
(9, 'Tina', 'Meier', 'Meier@email.com', '$argon2id$v=19$m=60000,t=10,p=1$qvrW+yHDp+3MYzTP2z7CmQ$g+SuP+/s8t5QrFH1gdNYnboOs+7d+aYbd6NOMVo+aKM', 'lehrer', 1, NULL),
(10, 'Mike', 'Reck', 'Reck@email.de', '$argon2id$v=19$m=60000,t=10,p=1$Cu04+qPppBA5ZR7cszpqqQ$I/4WuS5NvZlLyW7d6KljN/iJxqOcaS+lvvYftt7gNBU', 'lehrer', 1, NULL),
(11, 'Elisabeth', 'Johnson', 'elisabeth@email.com', '$argon2id$v=19$m=60000,t=10,p=1$PI4FVuSJeQ94W4by8sdXCw$X6KKmJdmNwwjyJlhV8EVXuXBAQhi6pvnn8iT3WSCxFc', 'schueler', 1, NULL),
(12, 'Mattias', 'Neuer', 'mattias@email.com', '$argon2id$v=19$m=60000,t=10,p=1$X6enJ6Ks8uVfpA1gvvc0Nw$RUhVD2o8sGrp69GPqKRKqMFSA3xvTkjVYBB8SB0xlAI', 'schueler', 1, NULL),
(13, 'Annalena', 'Langenstein', 'annalena@email.com', '$argon2id$v=19$m=60000,t=10,p=1$JihbQVPnXK1VIaOlmFDF3w$xwhWtp3Uq4gwaHqpOdLtQL8hxD+326FefnfXorJn/0s', 'schueler', 1, NULL);

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `buecher`
--

CREATE TABLE `buecher` (
  `isbn` varchar(20) NOT NULL,
  `titel` varchar(255) NOT NULL,
  `autor` varchar(255) NOT NULL,
  `erscheinungsjahr` year(4) NOT NULL,
  `beschreibung` text NOT NULL,
  `status` enum('verfuegbar','verliehen','reserviert','entfernt') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Daten für Tabelle `buecher`
--

INSERT INTO `buecher` (`isbn`, `titel`, `autor`, `erscheinungsjahr`, `beschreibung`, `status`) VALUES
('978-0261102217', 'The Hobbit', 'J. R. R. Tolkien', '2011', 'Bilbo Baggins is a hobbit who enjoys a comfortable, unambitious life, rarely travelling further than the pantry of his hobbit-hole in Bag End. But his contentment is disturbed when the wizard, Gandalf, and a company of thirteen dwarves arrive on his doorstep one day to whisk him away on an unexpected journey ‘there and back again’. They have a plot to raid the treasure hoard of Smaug the Magnificent, a large and very dangerous dragon…', 'verfuegbar'),
('978-1464221378', 'The Teacher', 'Freida McFadden', '2024', 'Eve has a good life. She gets up each day, gets a kiss from her husband Nate, and heads off to teach math at the local high school. All is as it should be. Except…\r\n\r\nLast year, Caseham High was rocked by a scandal, with one student, Addie, at its center. And this year, Eve is dismayed to find the girl in her class.\r\n\r\nAddie can\'t be trusted. She lies. She hurts people. She destroys lives. At least, that\'s what everyone says.\r\n\r\nBut nobody knows the real Addie. Nobody knows the secrets that could destroy her. And Addie will do anything to keep it quiet.', 'verfuegbar'),
('978-1642750331', 'I want to eat your pancreas', 'Yoru Sumino', '2018', 'A high school boy finds the diary of his classmate—only to discover that she’s dying. Yamauchi Sakura has been silently suffering from a pancreatic disease, and now exactly one person outside her family knows. He swears to her that he won’t tell anyone what he learned, and the shared secret brings them closer together in this deeply moving, first-person story that traces their developing relationship in Sakura’s final months of life.', 'verfuegbar'),
('978-1645052975', 'At Night, I Become a Monster', 'Yoru Sumino', '2020', 'Every night, Adachi transforms into a nightmarish creature―and every morning, he reverts to human form. When he encounters his ostracized classmate Yano Satsuki in his monstrous state, the two develop a peculiar bond. But daylight brings its own form of terrors. Which is worse, the monster at night, or the cruel realities of the classroom by day?', 'entfernt'),
('978-3125739291', 'Nineteen Eighty-Four', 'George Orwell', '2021', 'Winston Smith lives in 1984 in a London ruled by a fearsome totalitarian regime, headed by the ever-present Big Brother, and watched closely by the hugely feared Thought Police. But, he rebels…', 'reserviert'),
('978-3551317148', 'Im Zeichen der Zauberkugel 1', 'Stefan Gemmel', '2019', 'Beim Stöbern auf dem Dachboden seiner Großeltern entdeckt Alex eine verborgene Tür, die er noch nie gesehen hat. Eine Tür, die laut seiner Oma strengstens verboten ist. Natürlich öffnet Alex sie trotzdem. Dahinter liegt das geheime Arbeitszimmer seines Großvaters – ein Professor, der vor Jahren spurlos verschwand. Und zwischen all den rätselhaften Büchern und seltsamen Gegenständen findet Alex eine leuchtende Kugel. Darin steckt Sahli, ein echter Dschinn! Alex kann sein Glück kaum fassen. Doch das ändert sich schnell. Denn mit der Befreiung des Kugelgeists hat Alex nicht nur drei Wünsche frei – sondern auch Argus gegen sich aufgebracht. Den mächtigsten und gefährlichsten Dschinn aller Zeiten!', 'reserviert'),
('978-3551321022', 'Im Zeichen der Zauberkugel 2: Der Fluch des Skorpions', 'Stefan Gemmel', '2022', 'Alex hat einen neuen Freund: Sahli, den Jungen aus der Zauberkugel. Doch sie werden von dem geheimnisvollen Dschinn Argus verfolgt! Mächtiger und böser denn je, ist er wild entschlossen, den Fluch des Skorpions gegen die Freunde einzusetzen. Gemeinsam mit der magischen Katze Kadabra und den Zwillingen Liv und Sally müssen die beiden Jungs sich wehren - und werden dabei in eine aufregende Suche verwickelt, die bis tief in die Steinzeit führt …', 'verliehen'),
('978-3608126013', 'Der Herr der Ringe. Bd. 1 - Die Gefährten', 'J. R. R. Tolkien', '2026', 'Ein ungewöhnlicher Held. Eine Reise voller Gefahren. Das größte Abenteuer aller Zeiten.\r\n\r\nIn einem ruhigen Dorf im Auenland bekommt der junge Frodo ein Geschenk, das sein Leben für immer verändern wird – den Einen Ring, der seit Jahrhunderten als verschollen galt. Ein mächtiges und furchterregendes Ding, mit dem der Dunkle Herrscher einst Mittelerde versklavte.\r\n\r\nNun erhebt sich die Dunkelheit erneut, und Frodo muss tief in das Reich des Dunklen Herrschers vordringen, bis zu dem einzigen Ort, an dem der Ring zerstört werden kann: dem Schicksalsberg. Die Reise wird Frodos Mut, seine Freundschaften und sein Herz auf die Probe stellen. Denn der Ring korrumpiert alle, die ihn tragen. Kann Frodo den Ring vernichten, bevor der Ring ihn vernichtet? ', 'verliehen'),
('978-3641306113', 'Die Ehefrau – Was hat sie zu verbergen?', 'Freida McFadden', '2026', 'Sylvia Robinson wird im Haus der Barnetts als private Pflegekraft eingestellt. Nach einem Unfall benötigt Victoria Barnett rund um die Uhr Betreuung. Sie kann weder gehen noch sprechen und ist an ihr Bett im obersten Stockwerk des Hauses gefesselt. Daher hat ihr Mann Sylvia als Unterstützung hinzugeholt. Doch schon bald hat Sylvia das Gefühl, dass Victoria nicht so hilflos ist, wie sie scheint. Dann entdeckt sie Victorias Tagebuch versteckt in einer Kommode. Und was sie darin liest, zieht ihr den Boden unter den Füßen weg.', 'verfuegbar'),
('979-8285838210', '50 Groundbreaking Roller Coasters: The Most Important Scream Machines Ever Built', 'Nick Weisenberger', '2025', '50 Groundbreaking Roller Coasters is a comprehensive list of the most influential scream machines that drove the evolution of the modern roller coaster. Its a new and interesting look at roller coaster history. What makes a majority of the roller coasters listed in this book even more impressive is the fact that they were designed using pencil and paper rather than computers.\r\n\r\nPerfect for coaster fans, theme park travelers, and thrill ride historians alike, this book dives into the technology, stories, and bold ideas behind each groundbreaking coaster. Learn how engineers and visionaries shattered records, changed coaster design forever, and sparked new trends that continue to thrill millions today.\r\n\r\nThe groundbreaking scream machines that shaped the evolution of the roller coaster made this list because they were the first of their kind, crossed a threshold that had never been broken before, or have some other historical or cultural significance, such as:\r\nThe first floorless coaster.\r\nThe first to use lap bar restraints.\r\nThe first to use Linear Induction Motors.\r\nThe first to have two hills over 100 feet.\r\nThe first modern wooden coaster built in China.\r\nThe first steel inverting coaster.\r\nThe first to break 100mph.\r\nThe biggest wooden coaster ever built.\r\nAnd much more!\r\nTrace the evolution of white knuckle rides through these 50 Groundbreaking Roller Coasters. Whether you’re a casual parkgoer or a hardcore coaster enthusiast, this book is your front-row seat to the greatest achievements in roller coaster history.\r\nGet ready to ride — the most groundbreaking coasters of all time are waiting for you!', 'entfernt');

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `einstellungen`
--

CREATE TABLE `einstellungen` (
  `schluessel` varchar(50) NOT NULL,
  `wert` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Daten für Tabelle `einstellungen`
--

INSERT INTO `einstellungen` (`schluessel`, `wert`) VALUES
('buechersuche_api_key', 'AIzaSyA59yFeATSjQo9pIgPAzamkbUWUzZ6zLtI'),
('buechersuche_datenbank', 'Google Books'),
('email_adresse', 'euleinc@gmail.com'),
('email_passwort', 'ngbz pith mvjj sggv'),
('reservierungen_aktiv', '1'),
('reservierung_dauer_tage', '14'),
('reservierung_sperre_tage', '7'),
('smtp_port', '587'),
('smtp_server', 'smtp.gmail.com'),
('sperren_aktiv', '0'),
('sperren_verspaetung_tage', ''),
('sperren_zuruecksetzen_monate', '');

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `reservierungen`
--

CREATE TABLE `reservierungen` (
  `id` int(11) NOT NULL,
  `isbn` varchar(20) NOT NULL,
  `schueler_id` int(11) NOT NULL,
  `status` enum('wartend','bereit','abgeschlossen','abgesagt') NOT NULL,
  `reservierung_beginn` date DEFAULT NULL,
  `reservierung_ende` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Daten für Tabelle `reservierungen`
--

INSERT INTO `reservierungen` (`id`, `isbn`, `schueler_id`, `status`, `reservierung_beginn`, `reservierung_ende`) VALUES
(7, '978-3551317148', 11, 'bereit', '2026-08-20', '2026-09-03'),
(8, '978-0261102217', 7, 'abgesagt', '2026-08-29', '2026-09-12'),
(9, '978-0261102217', 7, 'abgesagt', '2026-08-29', '2026-09-12');

--
-- Indizes der exportierten Tabellen
--

--
-- Indizes für die Tabelle `ausleihen`
--
ALTER TABLE `ausleihen`
  ADD PRIMARY KEY (`id`),
  ADD KEY `isbn` (`isbn`),
  ADD KEY `schueler_id` (`schueler_id`);

--
-- Indizes für die Tabelle `benutzer`
--
ALTER TABLE `benutzer`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indizes für die Tabelle `buecher`
--
ALTER TABLE `buecher`
  ADD PRIMARY KEY (`isbn`);

--
-- Indizes für die Tabelle `einstellungen`
--
ALTER TABLE `einstellungen`
  ADD PRIMARY KEY (`schluessel`);

--
-- Indizes für die Tabelle `reservierungen`
--
ALTER TABLE `reservierungen`
  ADD PRIMARY KEY (`id`),
  ADD KEY `isbn` (`isbn`),
  ADD KEY `schueler_id` (`schueler_id`);

--
-- AUTO_INCREMENT für exportierte Tabellen
--

--
-- AUTO_INCREMENT für Tabelle `ausleihen`
--
ALTER TABLE `ausleihen`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT für Tabelle `benutzer`
--
ALTER TABLE `benutzer`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT für Tabelle `reservierungen`
--
ALTER TABLE `reservierungen`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Constraints der exportierten Tabellen
--

--
-- Constraints der Tabelle `ausleihen`
--
ALTER TABLE `ausleihen`
  ADD CONSTRAINT `ausleihen_ibfk_3` FOREIGN KEY (`schueler_id`) REFERENCES `benutzer` (`id`),
  ADD CONSTRAINT `ausleihen_ibfk_4` FOREIGN KEY (`isbn`) REFERENCES `buecher` (`isbn`);

--
-- Constraints der Tabelle `reservierungen`
--
ALTER TABLE `reservierungen`
  ADD CONSTRAINT `reservierungen_ibfk_2` FOREIGN KEY (`schueler_id`) REFERENCES `benutzer` (`id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `reservierungen_ibfk_3` FOREIGN KEY (`isbn`) REFERENCES `buecher` (`isbn`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
