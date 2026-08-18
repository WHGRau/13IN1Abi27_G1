-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Generation Time: Aug 18, 2026 at 09:01 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `Bibliothek`
--

-- --------------------------------------------------------

--
-- Table structure for table `ausleihen`
--

CREATE TABLE `ausleihen` (
  `schueler_id` int(11) NOT NULL,
  `isbn` varchar(20) NOT NULL,
  `ausleihdatum` date NOT NULL,
  `geplante_rueckgabe` date NOT NULL,
  `ruckgabe_datum` date DEFAULT NULL,
  `id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `benutzer`
--

CREATE TABLE `benutzer` (
  `id` int(11) NOT NULL,
  `vorname` varchar(100) NOT NULL,
  `nachname` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `passwort` varchar(255) NOT NULL,
  `rolle` enum('schueler','lehrer') NOT NULL,
  `freigeschaltet` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `buecher`
--

CREATE TABLE `buecher` (
  `isbn` varchar(20) NOT NULL,
  `titel` varchar(255) NOT NULL,
  `autor` varchar(255) NOT NULL,
  `erscheinungsjahr` year(4) NOT NULL,
  `beschreibung` text NOT NULL,
  `status` enum('verfuegbar','verliehen','reserviert','entfernt') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `reservierungen`
--

CREATE TABLE `reservierungen` (
  `id` int(11) NOT NULL,
  `isbn` varchar(20) NOT NULL,
  `schueler_id` int(11) NOT NULL,
  `status` enum('wartend','bereit','abgeschlossen','abgesagt') NOT NULL,
  `reservierung_beginn` date NOT NULL,
  `reservierung_ende` date NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `ausleihen`
--
ALTER TABLE `ausleihen`
  ADD PRIMARY KEY (`id`),
  ADD KEY `isbn` (`isbn`),
  ADD KEY `schueler_id` (`schueler_id`);

--
-- Indexes for table `benutzer`
--
ALTER TABLE `benutzer`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `buecher`
--
ALTER TABLE `buecher`
  ADD PRIMARY KEY (`isbn`);

--
-- Indexes for table `reservierungen`
--
ALTER TABLE `reservierungen`
  ADD PRIMARY KEY (`id`),
  ADD KEY `isbn` (`isbn`),
  ADD KEY `schueler_id` (`schueler_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `ausleihen`
--
ALTER TABLE `ausleihen`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=0;

--
-- AUTO_INCREMENT for table `benutzer`
--
ALTER TABLE `benutzer`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=0;

--
-- AUTO_INCREMENT for table `reservierungen`
--
ALTER TABLE `reservierungen`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=0;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `ausleihen`
--
ALTER TABLE `ausleihen`
  ADD CONSTRAINT `ausleihen_ibfk_3` FOREIGN KEY (`schueler_id`) REFERENCES `benutzer` (`id`),
  ADD CONSTRAINT `ausleihen_ibfk_4` FOREIGN KEY (`isbn`) REFERENCES `buecher` (`isbn`);

--
-- Constraints for table `reservierungen`
--
ALTER TABLE `reservierungen`
  ADD CONSTRAINT `reservierungen_ibfk_2` FOREIGN KEY (`schueler_id`) REFERENCES `benutzer` (`id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `reservierungen_ibfk_3` FOREIGN KEY (`isbn`) REFERENCES `buecher` (`isbn`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
